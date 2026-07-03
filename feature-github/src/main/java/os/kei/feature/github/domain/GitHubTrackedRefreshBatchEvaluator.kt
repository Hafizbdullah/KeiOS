package os.kei.feature.github.domain

import android.content.Context
import os.kei.feature.github.data.remote.GitHubReleaseLookupStrategy
import os.kei.feature.github.data.remote.GitHubReleaseStrategyRegistry
import os.kei.feature.github.domain.fdroid.FdroidBatchPackageSnapshotProvider
import os.kei.feature.github.domain.fdroid.FdroidReleaseCheckEvaluator
import os.kei.feature.github.domain.fdroid.FdroidReleaseCheckSource
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubRepositoryProfilePurpose
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedReleaseCheck

class GitHubTrackedRefreshBatchEvaluator(
    trackedItems: List<GitHubTrackedApp>,
    private val lookupConfigProvider: () -> GitHubLookupConfig =
        GitHubReleaseStrategyRegistry::loadLookupConfig,
    private val fdroidReleaseCheckSource: FdroidReleaseCheckEvaluator =
        FdroidReleaseCheckSource(
            snapshotProvider = FdroidBatchPackageSnapshotProvider(trackedItems)
        )
) {
    private val batchLookupConfig: GitHubLookupConfig by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        lookupConfigProvider()
    }

    suspend fun evaluateTrackedApp(
        context: Context,
        item: GitHubTrackedApp,
        strategy: GitHubReleaseLookupStrategy? = null,
        profilePurposeOverride: GitHubRepositoryProfilePurpose? = null,
        forceRefresh: Boolean = false
    ): GitHubTrackedReleaseCheck {
        return GitHubReleaseCheckService.evaluateTrackedApp(
            context = context,
            item = item,
            strategy = strategy,
            lookupConfigOverride = batchLookupConfig,
            profilePurposeOverride = profilePurposeOverride,
            forceRefresh = forceRefresh,
            fdroidReleaseCheckSource = fdroidReleaseCheckSource
        )
    }
}
