package os.kei.ui.page.main.github.section

import kotlin.test.assertEquals
import org.junit.Test

class GitHubOverviewPillPlanTest {
    @Test
    fun defaultEntriesMergeCompatibleMetricPills() {
        val plan = buildGitHubOverviewExpandedPillPlan(defaultGitHubOverviewEntries())

        assertEquals(
            listOf(
                GitHubOverviewExpandedPillKind.Stable,
                GitHubOverviewExpandedPillKind.PreRelease,
                GitHubOverviewExpandedPillKind.CheckFailed,
            ),
            plan.map { it.kind },
        )
    }

    @Test
    fun partialEntriesKeepRemainingMetricsVisible() {
        val plan =
            buildGitHubOverviewExpandedPillPlan(
                setOf(
                    GitHubOverviewEntry.Strategy,
                    GitHubOverviewEntry.StableUpdate,
                    GitHubOverviewEntry.PreReleaseTracked,
                    GitHubOverviewEntry.CheckFailed,
                ),
            )

        assertEquals(
            listOf(
                GitHubOverviewExpandedPillKind.StableUpdate,
                GitHubOverviewExpandedPillKind.PreReleaseTracked,
                GitHubOverviewExpandedPillKind.CheckFailed,
            ),
            plan.map { it.kind },
        )
    }

    @Test
    fun apiOnlyEntryLeavesMetricRowEmpty() {
        val plan =
            buildGitHubOverviewExpandedPillPlan(
                setOf(
                    GitHubOverviewEntry.Api,
                    GitHubOverviewEntry.Tracked,
                ),
            )

        assertEquals(
            emptyList(),
            plan.map { it.kind },
        )
    }
}
