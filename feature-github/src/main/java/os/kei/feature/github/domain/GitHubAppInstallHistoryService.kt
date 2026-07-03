package os.kei.feature.github.domain

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.github.data.local.GitHubAppInstallHistoryStore
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.data.remote.GitHubVersionUtils
import os.kei.feature.github.model.GitHubAppInstallHistoryAction
import os.kei.feature.github.model.GitHubAppInstallHistoryRecord
import os.kei.feature.github.model.GitHubAppInstallHistorySource
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedAppInstallSnapshot
import java.util.Locale

class GitHubAppInstallHistoryService(
    private val localDispatcher: CoroutineDispatcher = AppDispatchers.githubLocal,
) {
    suspend fun loadHistory(): List<GitHubAppInstallHistoryRecord> =
        withContext(localDispatcher) {
            GitHubAppInstallHistoryStore.load()
        }

    suspend fun recordPackageChanged(
        context: Context,
        packageName: String,
        action: String,
        replacing: Boolean,
        changedAtMillis: Long = System.currentTimeMillis(),
    ) {
        withContext(localDispatcher) {
            recordPackageChangedBlocking(
                context = context.applicationContext,
                packageName = packageName,
                action = action,
                replacing = replacing,
                changedAtMillis = changedAtMillis,
            )
        }
    }

    fun recordPackageChangedBlocking(
        context: Context,
        packageName: String,
        action: String,
        replacing: Boolean,
        changedAtMillis: Long = System.currentTimeMillis(),
    ) {
        val normalizedPackage = packageName.normalizedPackageName()
        if (normalizedPackage.isBlank() || changedAtMillis <= 0L) return
        val trackedItems =
            GitHubTrackStore.load()
                .filter { item -> item.packageName.normalizedPackageName() == normalizedPackage }
        if (trackedItems.isEmpty()) return

        val previousSnapshot = GitHubAppInstallHistoryStore.loadSnapshot(normalizedPackage)
        val currentSnapshot =
            if (action.isPackageRemovalAction() && !replacing) {
                null
            } else {
                querySnapshot(
                    context = context,
                    packageName = normalizedPackage,
                    appLabel = trackedItems.firstOrNull()?.appLabel.orEmpty(),
                    observedAtMillis = changedAtMillis,
                )
            }
        val result =
            buildPackageChangeResult(
                trackedItems = trackedItems,
                previousSnapshot = previousSnapshot,
                currentSnapshot = currentSnapshot,
                packageName = normalizedPackage,
                action = action,
                replacing = replacing,
                changedAtMillis = changedAtMillis,
            )
        GitHubAppInstallHistoryStore.recordEvents(result.records)
        when {
            result.removeSnapshot -> GitHubAppInstallHistoryStore.removeSnapshot(normalizedPackage)
            result.nextSnapshot != null -> GitHubAppInstallHistoryStore.saveSnapshot(result.nextSnapshot)
        }
    }

    suspend fun refreshTrackedInstallSnapshots(context: Context) {
        withContext(localDispatcher) {
            refreshTrackedInstallSnapshotsBlocking(context.applicationContext)
        }
    }

    fun refreshTrackedInstallSnapshotsBlocking(context: Context) {
        val now = System.currentTimeMillis()
        val snapshots =
            GitHubTrackStore.load()
                .asSequence()
                .filter { item -> item.packageName.trim().isNotBlank() }
                .distinctBy { item -> item.packageName.normalizedPackageName() }
                .mapNotNull { item ->
                    querySnapshot(
                        context = context,
                        packageName = item.packageName,
                        appLabel = item.appLabel,
                        observedAtMillis = now,
                    )
                }
                .toList()
        GitHubAppInstallHistoryStore.replaceSnapshots(snapshots)
    }

    suspend fun pruneBefore(cutoffMillis: Long): Int =
        withContext(localDispatcher) {
            GitHubAppInstallHistoryStore.pruneBefore(cutoffMillis)
        }

    private fun querySnapshot(
        context: Context,
        packageName: String,
        appLabel: String,
        observedAtMillis: Long,
    ): GitHubTrackedAppInstallSnapshot? {
        val normalizedPackage = packageName.normalizedPackageName()
        if (normalizedPackage.isBlank()) return null
        val info =
            runCatching {
                GitHubVersionUtils.localVersionInfoOrNull(context, normalizedPackage)
            }.getOrNull() ?: return null
        return GitHubTrackedAppInstallSnapshot(
            packageName = normalizedPackage,
            versionName = info.versionName,
            versionCode = info.versionCode,
            isSystemApp = info.isSystemApp,
            appLabel = appLabel.trim(),
            observedAtMillis = observedAtMillis,
        )
    }

    companion object {
        internal data class PackageChangeResult(
            val records: List<GitHubAppInstallHistoryRecord>,
            val nextSnapshot: GitHubTrackedAppInstallSnapshot?,
            val removeSnapshot: Boolean,
        )

        internal fun buildPackageChangeResult(
            trackedItems: List<GitHubTrackedApp>,
            previousSnapshot: GitHubTrackedAppInstallSnapshot?,
            currentSnapshot: GitHubTrackedAppInstallSnapshot?,
            packageName: String,
            action: String,
            replacing: Boolean,
            changedAtMillis: Long,
        ): PackageChangeResult {
            val normalizedPackage = packageName.normalizedPackageName()
            if (trackedItems.isEmpty() || normalizedPackage.isBlank() || changedAtMillis <= 0L) {
                return PackageChangeResult(emptyList(), nextSnapshot = null, removeSnapshot = false)
            }
            if (action == Intent.ACTION_PACKAGE_REMOVED && replacing) {
                return PackageChangeResult(emptyList(), nextSnapshot = previousSnapshot, removeSnapshot = false)
            }
            if (action.isPackageRemovalAction()) {
                val effectivePrevious =
                    previousSnapshot
                        ?: GitHubTrackedAppInstallSnapshot(
                            packageName = normalizedPackage,
                            versionName = "",
                            versionCode = -1L,
                            appLabel = trackedItems.firstOrNull()?.appLabel.orEmpty(),
                            observedAtMillis = changedAtMillis,
                        )
                val records =
                    trackedItems.map { item ->
                        item.toAppHistoryRecord(
                            action = GitHubAppInstallHistoryAction.Uninstalled,
                            changedAtMillis = changedAtMillis,
                            previousSnapshot = effectivePrevious,
                            currentSnapshot = null,
                            broadcastAction = action,
                            replacing = replacing,
                        )
                    }
                return PackageChangeResult(
                    records = records,
                    nextSnapshot = null,
                    removeSnapshot = true,
                )
            }
            if (action == Intent.ACTION_PACKAGE_CHANGED) {
                return PackageChangeResult(
                    records = emptyList(),
                    nextSnapshot = currentSnapshot,
                    removeSnapshot = currentSnapshot == null,
                )
            }
            if (currentSnapshot == null) {
                return PackageChangeResult(emptyList(), nextSnapshot = null, removeSnapshot = false)
            }
            val historyAction =
                when {
                    previousSnapshot == null -> GitHubAppInstallHistoryAction.Installed
                    currentSnapshot.versionCode > previousSnapshot.versionCode ->
                        GitHubAppInstallHistoryAction.Updated
                    currentSnapshot.versionCode < previousSnapshot.versionCode ->
                        GitHubAppInstallHistoryAction.Downgraded
                    currentSnapshot.versionName.trim() != previousSnapshot.versionName.trim() ->
                        GitHubAppInstallHistoryAction.Updated
                    else -> null
                }
            val records =
                historyAction?.let { appAction ->
                    trackedItems.map { item ->
                        item.toAppHistoryRecord(
                            action = appAction,
                            changedAtMillis = changedAtMillis,
                            previousSnapshot = previousSnapshot,
                            currentSnapshot = currentSnapshot,
                            broadcastAction = action,
                            replacing = replacing,
                        )
                    }
                }.orEmpty()
            return PackageChangeResult(
                records = records,
                nextSnapshot = currentSnapshot,
                removeSnapshot = false,
            )
        }

        private fun GitHubTrackedApp.toAppHistoryRecord(
            action: GitHubAppInstallHistoryAction,
            changedAtMillis: Long,
            previousSnapshot: GitHubTrackedAppInstallSnapshot?,
            currentSnapshot: GitHubTrackedAppInstallSnapshot?,
            broadcastAction: String,
            replacing: Boolean,
        ): GitHubAppInstallHistoryRecord =
            GitHubAppInstallHistoryRecord(
                id = "",
                trackId = id,
                action = action,
                source = GitHubAppInstallHistorySource.PackageBroadcast,
                changedAtMillis = changedAtMillis,
                owner = owner,
                repo = repo,
                repoUrl = repoUrl,
                packageName =
                    currentSnapshot?.packageName
                        ?: previousSnapshot?.packageName
                        ?: packageName,
                appLabel =
                    appLabel
                        .ifBlank { currentSnapshot?.appLabel.orEmpty() }
                        .ifBlank { previousSnapshot?.appLabel.orEmpty() },
                sourceMode = sourceMode,
                previousVersionName = previousSnapshot?.versionName.orEmpty(),
                previousVersionCode = previousSnapshot?.versionCode ?: -1L,
                currentVersionName = currentSnapshot?.versionName.orEmpty(),
                currentVersionCode = currentSnapshot?.versionCode ?: -1L,
                broadcastAction = broadcastAction,
                replacing = replacing,
            )

        private fun String.isPackageRemovalAction(): Boolean =
            this == Intent.ACTION_PACKAGE_REMOVED ||
                this == Intent.ACTION_PACKAGE_FULLY_REMOVED

        private fun String.normalizedPackageName(): String =
            trim().lowercase(Locale.ROOT)
    }
}
