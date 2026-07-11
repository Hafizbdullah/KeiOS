package os.kei.ui.page.main.widget.core

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = AppOverviewPillLayoutTestApp::class,
    sdk = [35],
    qualifiers = "zh-rCN-w375dp-h817dp-520dpi",
)
class AppOverviewPillLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun osOverviewPillsStayOnOneRowAtPhysicalDeviceWidth() {
        val labels = listOf("Top 2/22", "参数 1/7", "活动 9/9", "Shell 4/4")

        setPills(labels)

        assertEquals(1, distinctRowCount(labels))
    }

    @Test
    fun longOverviewPillRemainsSingleLineHeight() {
        val label = "a-very-long-custom-service-name-that-must-be-ellipsized"

        setPills(listOf(label))

        val bounds = boundsFor(label)
        val maxHeightPx = with(composeRule.density) { 28.dp.toPx() }
        assertTrue(
            actual = bounds.height <= maxHeightPx,
            message = "Expected a single-line 28dp pill, bounds=$bounds",
        )
    }

    private fun setPills(labels: List<String>) {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AppOverviewPillFlow(
                        pills = labels.map { label ->
                            AppOverviewPill(label = label, color = Color(0xFF2563EB))
                        },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun distinctRowCount(labels: List<String>): Int {
        val tolerancePx = with(composeRule.density) { 2.dp.toPx() }
        return labels
            .map(::boundsFor)
            .map { bounds -> bounds.center.y }
            .fold(mutableListOf<Float>()) { rows, centerY ->
                if (rows.none { rowY -> abs(rowY - centerY) <= tolerancePx }) rows += centerY
                rows
            }.size
    }

    private fun boundsFor(text: String): Rect =
        composeRule
            .onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
}

class AppOverviewPillLayoutTestApp : Application()
