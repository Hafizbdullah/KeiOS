package os.kei.feature.github.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.github.data.local.GitHubTrackChangeHistoryStore
import os.kei.feature.github.model.FdroidTrackedAppConfig
import os.kei.feature.github.model.GitHubTrackChangeField
import os.kei.feature.github.model.GitHubTrackChangeHistoryAction
import os.kei.feature.github.model.GitHubTrackChangeHistoryRecord
import os.kei.feature.github.model.GitHubTrackChangeHistorySource
import os.kei.feature.github.model.GitHubTrackedApp

data class GitHubTrackChangeSemanticUpdate(
    val previous: GitHubTrackedApp,
    val next: GitHubTrackedApp,
)

class GitHubTrackChangeHistoryService(
    private val localDispatcher: CoroutineDispatcher = AppDispatchers.githubLocal,
) {
    suspend fun loadHistory(): List<GitHubTrackChangeHistoryRecord> =
        withContext(localDispatcher) {
            GitHubTrackChangeHistoryStore.load()
        }

    suspend fun recordChanges(
        previousItems: List<GitHubTrackedApp>,
        nextItems: List<GitHubTrackedApp>,
        source: GitHubTrackChangeHistorySource,
        changedAtMillis: Long = System.currentTimeMillis(),
        semanticUpdates: List<GitHubTrackChangeSemanticUpdate> = emptyList(),
    ) {
        withContext(localDispatcher) {
            recordChangesBlocking(
                previousItems = previousItems,
                nextItems = nextItems,
                source = source,
                changedAtMillis = changedAtMillis,
                semanticUpdates = semanticUpdates,
            )
        }
    }

    fun recordChangesBlocking(
        previousItems: List<GitHubTrackedApp>,
        nextItems: List<GitHubTrackedApp>,
        source: GitHubTrackChangeHistorySource,
        changedAtMillis: Long = System.currentTimeMillis(),
        semanticUpdates: List<GitHubTrackChangeSemanticUpdate> = emptyList(),
    ) {
        val records =
            buildChangeRecords(
                previousItems = previousItems,
                nextItems = nextItems,
                source = source,
                changedAtMillis = changedAtMillis,
                semanticUpdates = semanticUpdates,
            )
        GitHubTrackChangeHistoryStore.recordChanges(records)
    }

    suspend fun pruneBefore(cutoffMillis: Long): Int =
        withContext(localDispatcher) {
            GitHubTrackChangeHistoryStore.pruneBefore(cutoffMillis)
        }

    companion object {
        internal fun buildChangeRecords(
            previousItems: List<GitHubTrackedApp>,
            nextItems: List<GitHubTrackedApp>,
            source: GitHubTrackChangeHistorySource,
            changedAtMillis: Long,
            semanticUpdates: List<GitHubTrackChangeSemanticUpdate> = emptyList(),
        ): List<GitHubTrackChangeHistoryRecord> {
            if (changedAtMillis <= 0L) return emptyList()
            val previousById = previousItems.associateBy { it.id }
            val nextById = nextItems.associateBy { it.id }
            val semanticPreviousIds = mutableSetOf<String>()
            val semanticNextIds = mutableSetOf<String>()
            val records = mutableListOf<GitHubTrackChangeHistoryRecord>()

            semanticUpdates.forEach { update ->
                semanticPreviousIds += update.previous.id
                semanticNextIds += update.next.id
                val changedFields = update.previous.significantChangeFields(update.next)
                if (changedFields.isEmpty()) return@forEach
                records +=
                    update.next.toHistoryRecord(
                        action = GitHubTrackChangeHistoryAction.Updated,
                        source = source,
                        changedAtMillis = changedAtMillis,
                        changedFields = changedFields,
                        previousTrackId =
                            update.previous.id
                                .takeIf { previousId -> previousId != update.next.id }
                                .orEmpty(),
                    )
            }

            nextItems.forEach { next ->
                if (next.id in semanticNextIds) return@forEach
                val previous = previousById[next.id]
                if (previous == null) {
                    records +=
                        next.toHistoryRecord(
                            action = GitHubTrackChangeHistoryAction.Added,
                            source = source,
                            changedAtMillis = changedAtMillis,
                            changedFields = emptyList(),
                        )
                } else {
                    val changedFields = previous.significantChangeFields(next)
                    if (changedFields.isNotEmpty()) {
                        records +=
                            next.toHistoryRecord(
                                action = GitHubTrackChangeHistoryAction.Updated,
                                source = source,
                                changedAtMillis = changedAtMillis,
                                changedFields = changedFields,
                            )
                    }
                }
            }

            previousItems.forEach { previous ->
                if (previous.id in semanticPreviousIds) return@forEach
                if (previous.id !in nextById) {
                    records +=
                        previous.toHistoryRecord(
                            action = GitHubTrackChangeHistoryAction.Deleted,
                            source = source,
                            changedAtMillis = changedAtMillis,
                            changedFields = emptyList(),
                        )
                }
            }

            return records
        }

        internal fun GitHubTrackedApp.significantChangeFields(
            next: GitHubTrackedApp,
        ): List<GitHubTrackChangeField> =
            buildList {
                if (
                    repoUrl.trim() != next.repoUrl.trim() ||
                    owner.trim() != next.owner.trim() ||
                    repo.trim() != next.repo.trim()
                ) {
                    add(GitHubTrackChangeField.Repository)
                }
                if (!packageName.trim().equals(next.packageName.trim(), ignoreCase = true)) {
                    add(GitHubTrackChangeField.PackageName)
                }
                if (appLabel.trim() != next.appLabel.trim()) {
                    add(GitHubTrackChangeField.AppLabel)
                }
                if (sourceMode != next.sourceMode) {
                    add(GitHubTrackChangeField.SourceMode)
                }
                if (preferPreRelease != next.preferPreRelease) {
                    add(GitHubTrackChangeField.PreferPreRelease)
                }
                if (alwaysShowLatestReleaseDownloadButton != next.alwaysShowLatestReleaseDownloadButton) {
                    add(GitHubTrackChangeField.LatestReleaseDownloadButton)
                }
                if (checkActionsUpdates != next.checkActionsUpdates) {
                    add(GitHubTrackChangeField.ActionsUpdates)
                }
                if (updateIntervalMode != next.updateIntervalMode) {
                    add(GitHubTrackChangeField.UpdateInterval)
                }
                if (actionsUpdateIntervalMode != next.actionsUpdateIntervalMode) {
                    add(GitHubTrackChangeField.ActionsUpdateInterval)
                }
                if (preciseApkVersionMode != next.preciseApkVersionMode) {
                    add(GitHubTrackChangeField.PreciseApkVersion)
                }
                if (ignoreMode != next.ignoreMode) {
                    add(GitHubTrackChangeField.IgnoreMode)
                }
                if (ignoredStableReleaseKey.trim() != next.ignoredStableReleaseKey.trim()) {
                    add(GitHubTrackChangeField.IgnoredStableRelease)
                }
                if (ignoredPreReleaseKey.trim() != next.ignoredPreReleaseKey.trim()) {
                    add(GitHubTrackChangeField.IgnoredPreRelease)
                }
                if (fdroidConfig.historyRelevantCopy() != next.fdroidConfig.historyRelevantCopy()) {
                    add(GitHubTrackChangeField.FdroidConfig)
                }
            }

        private fun FdroidTrackedAppConfig.historyRelevantCopy(): FdroidTrackedAppConfig =
            copy(
                packagePageUrl = packagePageUrl.trim(),
                repoPresetId = repoPresetId.trim(),
                versionNameRegex = versionNameRegex.trim(),
                apkNameRegex = apkNameRegex.trim(),
            )

        private fun GitHubTrackedApp.toHistoryRecord(
            action: GitHubTrackChangeHistoryAction,
            source: GitHubTrackChangeHistorySource,
            changedAtMillis: Long,
            changedFields: List<GitHubTrackChangeField>,
            previousTrackId: String = "",
        ): GitHubTrackChangeHistoryRecord =
            GitHubTrackChangeHistoryRecord(
                id = "",
                trackId = id,
                previousTrackId = previousTrackId,
                action = action,
                source = source,
                changedAtMillis = changedAtMillis,
                owner = owner,
                repo = repo,
                repoUrl = repoUrl,
                packageName = packageName,
                appLabel = appLabel,
                sourceMode = sourceMode,
                changedFields = changedFields,
            )
    }
}
