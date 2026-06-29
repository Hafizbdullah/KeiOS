package os.kei.feature.github.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.feature.github.domain.fdroid.FdroidReleaseCheckEvaluator
import os.kei.feature.github.model.GITHUB_FDROID_STRATEGY_ID
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedReleaseCheck
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import os.kei.feature.github.model.GitHubTrackedSourceMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class GitHubReleaseCheckServiceFdroidTest {
    @Test
    fun `evaluateTrackedApp dispatches fdroid source to fdroid evaluator`() = runBlocking {
        val item = GitHubTrackedApp(
            repoUrl = "https://f-droid.org/repo",
            owner = "f-droid.org",
            repo = "repo",
            packageName = "org.fdroid.fdroid",
            appLabel = "F-Droid",
            sourceMode = GitHubTrackedSourceMode.FdroidRepository
        )
        var observedItem: GitHubTrackedApp? = null
        val evaluator = FdroidReleaseCheckEvaluator { trackedItem, _, localVersion, localVersionCode, forceRefresh ->
            observedItem = trackedItem
            GitHubTrackedReleaseCheck(
                strategyId = GITHUB_FDROID_STRATEGY_ID,
                localVersion = localVersion,
                localVersionCode = localVersionCode,
                sourceConfigSignature = trackedItem.fdroidConfig.selectionMode.storageId,
                status = if (forceRefresh) {
                    GitHubTrackedReleaseStatus.UpToDate
                } else {
                    GitHubTrackedReleaseStatus.ComparisonUncertain
                }
            )
        }

        val result = GitHubReleaseCheckService.evaluateTrackedAppForTest(
            context = ApplicationProvider.getApplicationContext<Context>(),
            item = item,
            fdroidReleaseCheckSource = evaluator,
            lookupConfigOverride = GitHubLookupConfig(),
            forceRefresh = true
        )

        assertEquals(item.id, observedItem?.id)
        assertEquals(GITHUB_FDROID_STRATEGY_ID, result.strategyId)
        assertEquals(GitHubTrackedReleaseStatus.UpToDate, result.status)
    }
}
