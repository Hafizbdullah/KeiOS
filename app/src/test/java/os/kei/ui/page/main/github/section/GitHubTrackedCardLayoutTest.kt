package os.kei.ui.page.main.github.section

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubTrackedCardLayoutTest {
    @Test
    fun collapsedHeaderUsesCompactVerticalInsets() {
        assertEquals(4.dp, githubTrackedCardHeaderPadding.calculateTopPadding())
        assertEquals(4.dp, githubTrackedCardHeaderPadding.calculateBottomPadding())
        assertEquals(
            14.dp,
            githubTrackedCardHeaderPadding.calculateLeftPadding(LayoutDirection.Ltr),
        )
    }

    @Test
    fun trailingActionsDoNotSpendWidthOnDecorativeGaps() {
        assertEquals(4.dp, githubTrackedCardHeaderHorizontalSpacing)
        assertEquals(0.dp, githubTrackedCardHeaderActionSpacing)
    }
}
