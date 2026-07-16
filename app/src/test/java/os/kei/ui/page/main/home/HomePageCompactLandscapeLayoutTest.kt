package os.kei.ui.page.main.home

import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class HomePageCompactLandscapeLayoutTest {
    @Test
    fun compactLandscapeUsesTheReducedHeroBudget() {
        assertTrue(
            homePageUsesCompactLandscapeLayout(
                availableWidth = 952.dp,
                availableHeight = 426.dp,
            ),
        )
        assertEquals(12.dp, homePageHeroTopPadding(compactHeightPresentation = true))
        assertEquals(12.dp, homePageHeroSpacerTrailingClearance(compactHeightPresentation = true))
        assertFalse(homePageHeroShowsSupportingDetails(compactHeightPresentation = true))
        assertEquals(
            96.dp,
            homePageLogoTopPadding(
                scaffoldTopPadding = 96.dp,
                contentTopPadding = 48.dp,
                compactHeightPresentation = true,
            ),
        )
        assertEquals(
            18.dp,
            homePageHeroSpacerHeight(
                heroContentHeight = 98.dp,
                logoTopPadding = 96.dp,
                listTopPadding = 200.dp,
                topPadding = 12.dp,
                trailingClearance = 12.dp,
                homeHeaderSinkOffset = 0.dp,
            ),
        )
    }

    @Test
    fun portraitAndTallLandscapeKeepTheFullHeroBudget() {
        assertFalse(
            homePageUsesCompactLandscapeLayout(
                availableWidth = 411.dp,
                availableHeight = 891.dp,
            ),
        )
        assertFalse(
            homePageUsesCompactLandscapeLayout(
                availableWidth = 952.dp,
                availableHeight = 600.dp,
            ),
        )
        assertEquals(36.dp, homePageHeroTopPadding(compactHeightPresentation = false))
        assertEquals(90.dp, homePageHeroSpacerTrailingClearance(compactHeightPresentation = false))
        assertTrue(homePageHeroShowsSupportingDetails(compactHeightPresentation = false))
        assertEquals(
            168.dp,
            homePageLogoTopPadding(
                scaffoldTopPadding = 96.dp,
                contentTopPadding = 48.dp,
                compactHeightPresentation = false,
            ),
        )
        assertEquals(
            0.dp,
            homePageHeroSpacerHeight(
                heroContentHeight = 40.dp,
                logoTopPadding = 0.dp,
                listTopPadding = 100.dp,
                topPadding = 12.dp,
                trailingClearance = 12.dp,
                homeHeaderSinkOffset = 0.dp,
            ),
        )
    }
}
