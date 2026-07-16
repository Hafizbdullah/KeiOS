package os.kei.ui.page.main.about.page

import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import os.kei.ui.page.main.widget.chrome.TabbedPageBottomChromeSearchDockVisibleAlpha
import os.kei.ui.page.main.widget.chrome.tabbedPageCategoryDockExpanded
import os.kei.ui.page.main.widget.chrome.tabbedPageChromeVisible
import os.kei.ui.page.main.widget.chrome.tabbedPageCollapsedDockWidth
import os.kei.ui.page.main.widget.chrome.tabbedPageExpandedSearchWidth
import os.kei.ui.page.main.widget.chrome.tabbedPageSearchDockRegionOffset
import os.kei.ui.page.main.widget.chrome.tabbedPageUsesCompactHeightDock

class AboutBottomChromeLayoutTest {
    @Test
    fun expandedSearchUsesRemainingWidthAfterCompactDock() {
        assertEquals(
            300.dp,
            tabbedPageExpandedSearchWidth(
                availableWidth = 370.dp,
                compactDockWidth = 62.dp,
                gap = 8.dp,
            ),
        )
    }

    @Test
    fun expandedSearchKeepsMinimumWidthOnNarrowSurfaces() {
        assertEquals(
            196.dp,
            tabbedPageExpandedSearchWidth(
                availableWidth = 220.dp,
                compactDockWidth = 62.dp,
                gap = 8.dp,
            ),
        )
    }

    @Test
    fun collapsedDockUsesSpaceBeforeSearchButton() {
        assertEquals(
            300.dp,
            tabbedPageCollapsedDockWidth(
                availableWidth = 370.dp,
                searchDockWidth = 62.dp,
                gap = 8.dp,
            ),
        )
    }

    @Test
    fun collapsedChromeKeepsSearchDockVisible() {
        assertEquals(1f, TabbedPageBottomChromeSearchDockVisibleAlpha)
    }

    @Test
    fun searchDockRegionStartsAfterCompactCategoryButton() {
        assertEquals(
            70.dp,
            tabbedPageSearchDockRegionOffset(
                size = 62.dp,
                gap = 8.dp,
            ),
        )
    }

    @Test
    fun categoryDockUsesMainCompactAnimationState() {
        assertTrue(
            tabbedPageCategoryDockExpanded(
                visible = true,
                searchExpanded = false,
            ),
        )
        assertFalse(
            tabbedPageCategoryDockExpanded(
                visible = false,
                searchExpanded = false,
            ),
        )
        assertFalse(
            tabbedPageCategoryDockExpanded(
                visible = true,
                searchExpanded = true,
            ),
        )
    }

    @Test
    fun compactLandscapeStartsWithTheSelectedCategoryDock() {
        assertTrue(
            tabbedPageUsesCompactHeightDock(
                availableWidth = 952.dp,
                availableHeight = 426.dp,
            ),
        )
        assertFalse(
            tabbedPageChromeVisible(
                visible = true,
                compactHeightPresentation = true,
                compactHeightDockExpanded = false,
            ),
        )
        assertTrue(
            tabbedPageChromeVisible(
                visible = true,
                compactHeightPresentation = true,
                compactHeightDockExpanded = true,
            ),
        )
    }

    @Test
    fun portraitAndTallWindowsKeepTheExpandedCategoryDock() {
        assertFalse(
            tabbedPageUsesCompactHeightDock(
                availableWidth = 411.dp,
                availableHeight = 891.dp,
            ),
        )
        assertFalse(
            tabbedPageUsesCompactHeightDock(
                availableWidth = 952.dp,
                availableHeight = 600.dp,
            ),
        )
        assertTrue(
            tabbedPageChromeVisible(
                visible = true,
                compactHeightPresentation = false,
                compactHeightDockExpanded = false,
            ),
        )
    }
}
