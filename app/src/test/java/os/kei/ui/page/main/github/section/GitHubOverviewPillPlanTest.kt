package os.kei.ui.page.main.github.section

import kotlin.test.assertEquals
import org.junit.Test

class GitHubOverviewPillPlanTest {
    @Test
    fun defaultEntriesMergeCompatibleMetricPills() {
        val plan = buildGitHubOverviewExpandedPillPlan(defaultGitHubOverviewEntries())

        assertEquals(
            listOf(
                GitHubOverviewExpandedPillKind.Lookup,
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
                GitHubOverviewExpandedPillKind.Lookup,
                GitHubOverviewExpandedPillKind.StableUpdate,
                GitHubOverviewExpandedPillKind.PreReleaseTracked,
                GitHubOverviewExpandedPillKind.CheckFailed,
            ),
            plan.map { it.kind },
        )
    }

    @Test
    fun apiOnlyEntryStillUsesLookupPill() {
        val plan =
            buildGitHubOverviewExpandedPillPlan(
                setOf(
                    GitHubOverviewEntry.Api,
                    GitHubOverviewEntry.Tracked,
                ),
            )

        assertEquals(
            listOf(
                GitHubOverviewExpandedPillKind.Lookup,
            ),
            plan.map { it.kind },
        )
    }
}
