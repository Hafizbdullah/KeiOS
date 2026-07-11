package os.kei.feature.github.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.github.data.local.GitHubAppInstallHistoryStore
import os.kei.feature.github.data.local.GitHubInstalledAppRepository
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.model.GitHubAppInstallHistoryAction
import os.kei.feature.github.model.GitHubAppInstallHistoryRecord
import os.kei.feature.github.model.GitHubAppInstallHistorySource
import os.kei.feature.github.model.GitHubAppInstallSourceInfo
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
        broadcastUid: Int = -1,
        broadcastDataRemoved: Boolean = false,
        broadcastUserInitiated: Boolean = false,
        broadcastArchival: Boolean = false,
        changedAtMillis: Long = System.currentTimeMillis(),
    ) {
        withContext(localDispatcher) {
            recordPackageChangedBlocking(
                context = context.applicationContext,
                packageName = packageName,
                action = action,
                replacing = replacing,
                broadcastUid = broadcastUid,
                broadcastDataRemoved = broadcastDataRemoved,
                broadcastUserInitiated = broadcastUserInitiated,
                broadcastArchival = broadcastArchival,
                changedAtMillis = changedAtMillis,
            )
        }
    }

    fun recordPackageChangedBlocking(
        context: Context,
        packageName: String,
        action: String,
        replacing: Boolean,
        broadcastUid: Int = -1,
        broadcastDataRemoved: Boolean = false,
        broadcastUserInitiated: Boolean = false,
        broadcastArchival: Boolean = false,
        changedAtMillis: Long = System.currentTimeMillis(),
    ) {
        val normalizedPackage = packageName.normalizedPackageName()
        if (normalizedPackage.isBlank() || changedAtMillis <= 0L) return
        val trackedItems =
            GitHubTrackStore.load()
                .filter { item -> item.packageName.normalizedPackageName() == normalizedPackage }
        if (trackedItems.isEmpty()) return

        val previousSnapshot = GitHubAppInstallHistoryStore.loadSnapshot(normalizedPackage)
        val installSourceLabelCache = mutableMapOf<String, String>()
        val currentSnapshot =
            if (action.isPackageRemovalAction() && !replacing) {
                null
            } else {
                querySnapshot(
                    context = context,
                    packageName = normalizedPackage,
                    appLabel = trackedItems.firstOrNull()?.appLabel.orEmpty(),
                    observedAtMillis = changedAtMillis,
                    installSourceLabelCache = installSourceLabelCache,
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
                broadcastUid = broadcastUid,
                broadcastDataRemoved = broadcastDataRemoved,
                broadcastUserInitiated = broadcastUserInitiated,
                broadcastArchival = broadcastArchival,
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
        val installSourceLabelCache = mutableMapOf<String, String>()
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
                        installSourceLabelCache = installSourceLabelCache,
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
        installSourceLabelCache: MutableMap<String, String> = mutableMapOf(),
    ): GitHubTrackedAppInstallSnapshot? {
        val normalizedPackage = packageName.normalizedPackageName()
        if (normalizedPackage.isBlank()) return null
        val info =
            runCatching {
                GitHubInstalledAppRepository.localVersionInfoOrNull(context, normalizedPackage)
            }.getOrNull() ?: return null
        return GitHubTrackedAppInstallSnapshot(
            packageName = normalizedPackage,
            versionName = info.versionName,
            versionCode = info.versionCode,
            isSystemApp = info.isSystemApp,
            appLabel = appLabel.trim(),
            observedAtMillis = observedAtMillis,
            installSourceInfo =
                context.packageManager.resolveInstallSourceInfo(
                    packageName = normalizedPackage,
                    labelCache = installSourceLabelCache,
                ),
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
            broadcastUid: Int = -1,
            broadcastDataRemoved: Boolean = false,
            broadcastUserInitiated: Boolean = false,
            broadcastArchival: Boolean = false,
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
                            broadcastUid = broadcastUid,
                            broadcastDataRemoved = broadcastDataRemoved,
                            broadcastUserInitiated = broadcastUserInitiated,
                            broadcastArchival = broadcastArchival,
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
                    currentSnapshot.installSourceInfo != previousSnapshot.installSourceInfo ->
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
                            broadcastUid = broadcastUid,
                            broadcastDataRemoved = broadcastDataRemoved,
                            broadcastUserInitiated = broadcastUserInitiated,
                            broadcastArchival = broadcastArchival,
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
            broadcastUid: Int,
            broadcastDataRemoved: Boolean,
            broadcastUserInitiated: Boolean,
            broadcastArchival: Boolean,
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
                broadcastUid = broadcastUid,
                broadcastDataRemoved = broadcastDataRemoved,
                broadcastUserInitiated = broadcastUserInitiated,
                broadcastArchival = broadcastArchival,
                replacing = replacing,
                previousInstallSourceInfo =
                    previousSnapshot?.installSourceInfo ?: GitHubAppInstallSourceInfo(),
                currentInstallSourceInfo =
                    currentSnapshot?.installSourceInfo ?: GitHubAppInstallSourceInfo(),
            )

        private fun String.isPackageRemovalAction(): Boolean =
            this == Intent.ACTION_PACKAGE_REMOVED ||
                this == Intent.ACTION_PACKAGE_FULLY_REMOVED

        private fun String.normalizedPackageName(): String =
            trim().lowercase(Locale.ROOT)

        private fun PackageManager.resolveInstallSourceInfo(
            packageName: String,
            labelCache: MutableMap<String, String>,
        ): GitHubAppInstallSourceInfo {
            val sourceInfo =
                runCatching {
                    getInstallSourceInfo(packageName)
                }.getOrNull() ?: return GitHubAppInstallSourceInfo()
            return GitHubAppInstallSourceInfo(
                installingPackageName = sourceInfo.installingPackageName.orEmpty(),
                installingPackageLabel = sourceLabel(sourceInfo.installingPackageName, labelCache),
                initiatingPackageName = sourceInfo.initiatingPackageName.orEmpty(),
                initiatingPackageLabel = sourceLabel(sourceInfo.initiatingPackageName, labelCache),
                originatingPackageName = sourceInfo.originatingPackageName.orEmpty(),
                originatingPackageLabel = sourceLabel(sourceInfo.originatingPackageName, labelCache),
                updateOwnerPackageName = sourceInfo.updateOwnerPackageName.orEmpty(),
                updateOwnerPackageLabel = sourceLabel(sourceInfo.updateOwnerPackageName, labelCache),
                packageSource = sourceInfo.packageSource.takeIf { it >= 0 } ?: PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED,
            )
        }

        private fun PackageManager.sourceLabel(
            packageName: String?,
            labelCache: MutableMap<String, String>,
        ): String {
            val sourcePackageName = packageName?.trim().orEmpty()
            if (sourcePackageName.isBlank()) return ""
            return labelCache.getOrPut(sourcePackageName) {
                resolvePackageLabel(sourcePackageName)
            }
        }

        private fun PackageManager.resolvePackageLabel(packageName: String): String =
            runCatching {
                val appInfo =
                    getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0),
                    )
                getApplicationLabel(appInfo).toString()
            }.getOrDefault(packageName)
                .trim()
                .ifBlank { packageName }
    }
}
