package os.kei.ui.page.main.student.catalog.component.bgm

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.R
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = BaGuideBgmChromeMiniPlayerTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class BaGuideBgmChromeMiniPlayerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun expandedPlayerExposesFortyEightDpSeekAndTransportTargets() {
        var previousClicks = 0
        var playClicks = 0
        var nextClicks = 0
        setMiniPlayer(
            expanded = 1f,
            onPreviousClick = { previousClicks++ },
            onPlayPauseClick = { playClicks++ },
            onNextClick = { nextClicks++ },
        )
        val context = ApplicationProvider.getApplicationContext<Application>()

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_seekbar))
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_action_previous))
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_action_play))
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_action_next))
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, previousClicks)
        assertEquals(1, playClicks)
        assertEquals(1, nextClicks)
    }

    @Test
    fun expandedPlayerKeepsProgressTrackBelowTitleInsideSurface() {
        setMiniPlayer(expanded = 1f)
        val context = ApplicationProvider.getApplicationContext<Application>()
        val titleBounds = composeRule.onNodeWithText(TEST_TRACK_TITLE).fetchSemanticsNode().boundsInRoot
        val seekBounds =
            composeRule
                .onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_seekbar))
                .fetchSemanticsNode()
                .boundsInRoot
        val surfaceBounds =
            composeRule
                .onNodeWithTag(MINI_PLAYER_SURFACE_TEST_TAG)
                .fetchSemanticsNode()
                .boundsInRoot
        val minimumTrackGapPx = 8.dp.value * context.resources.displayMetrics.density

        assertTrue(
            seekBounds.center.y >= titleBounds.bottom + minimumTrackGapPx,
            "Progress track center ${seekBounds.center.y} must stay below title bottom ${titleBounds.bottom}",
        )
        assertTrue(
            seekBounds.bottom <= surfaceBounds.bottom,
            "Seek target bottom ${seekBounds.bottom} must stay inside surface bottom ${surfaceBounds.bottom}",
        )
    }

    @Test
    fun compactPlayerRemovesSeekAndSideTransportSemantics() {
        setMiniPlayer(expanded = 0f)
        val context = ApplicationProvider.getApplicationContext<Application>()

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_seekbar))
            .assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_action_previous))
            .assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_action_next))
            .assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.ba_catalog_bgm_action_play))
            .assertHeightIsAtLeast(48.dp)
    }

    private fun setMiniPlayer(
        expanded: Float,
        onPreviousClick: () -> Unit = {},
        onPlayPauseClick: () -> Unit = {},
        onNextClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .testTag(MINI_PLAYER_SURFACE_TEST_TAG),
                ) {
                    BaGuideBgmChromeMiniPlayer(
                        accent = Color(0xFF3B82F6),
                        currentTrackTitle = TEST_TRACK_TITLE,
                        artworkImageUrl = "",
                        isPlaying = false,
                        playbackProgress = { 0.25f },
                        onPlaybackProgressChange = {},
                        onPlaybackProgressChangeFinished = {},
                        onPlaybackSliderInteractionChanged = {},
                        expandedProgress = { expanded },
                        compactProgress = { 1f - expanded },
                        onPlayPauseClick = onPlayPauseClick,
                        onPreviousClick = onPreviousClick,
                        onNextClick = onNextClick,
                        backdrop = emptyBackdrop(),
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
    }
}

private const val TEST_TRACK_TITLE = "学生 · A long track title"

private const val MINI_PLAYER_SURFACE_TEST_TAG = "ba_bgm_mini_player_surface"

class BaGuideBgmChromeMiniPlayerTestApp : Application()
