@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val NESTED_SHEET_TAG = "nested-liquid-sheet"
private const val NESTED_CONTENT_TAG = "nested-liquid-sheet-content"

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = LiquidGlassBottomSheetTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class LiquidGlassBottomSheetNestedScrollTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentReachingTopHandsRemainingDownwardDragToSheetInSameGesture() {
        var dismissRequests = 0
        composeRule.setContent {
            NestedSheetTestTheme {
                LiquidGlassBottomSheet(
                    show = true,
                    modifier = Modifier.testTag(NESTED_SHEET_TAG),
                    title = "Sheet",
                    initialDetent = LiquidSheetInitialDetent.Full,
                    onDismissRequest = { dismissRequests++ },
                ) {
                    SheetContentColumn(
                        modifier = Modifier.testTag(NESTED_CONTENT_TAG),
                        verticalSpacing = 0.dp,
                    ) {
                        repeat(48) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .background(Color.Gray),
                            )
                        }
                    }
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        val rootHeight = rootHeight()

        composeRule.onNodeWithTag(NESTED_CONTENT_TAG).performTouchInput {
            val start = Offset(x = width / 2f, y = height * 0.76f)
            down(start)
            moveBy(Offset(x = 0f, y = -(rootHeight * 0.22f).toPx()))
            up()
        }

        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        val heightBefore = sheetHeight()

        composeRule.onNodeWithTag(NESTED_CONTENT_TAG).performTouchInput {
            val start = Offset(x = width / 2f, y = height * 0.30f)
            down(start)
            moveBy(Offset(x = 0f, y = (rootHeight * 0.52f).toPx()))
            up()
        }

        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()

        val heightAfter = sheetHeight()
        assertEquals(0, dismissRequests)
        assertTrue(
            heightAfter < heightBefore - rootHeight * 0.12f,
            "Expected the unconsumed drag remainder to resize the sheet, before=$heightBefore after=$heightAfter",
        )
    }

    private fun sheetHeight(): Dp {
        val heightPx =
            composeRule
                .onNodeWithTag(NESTED_SHEET_TAG)
                .fetchSemanticsNode()
                .boundsInRoot
                .height
        return with(composeRule.density) { heightPx.toDp() }
    }

    private fun rootHeight(): Dp {
        val heightPx =
            composeRule
                .onAllNodes(isRoot())
                .fetchSemanticsNodes()
                .maxOf { it.boundsInRoot.height }
        return with(composeRule.density) { heightPx.toDp() }
    }
}

@Composable
private fun NestedSheetTestTheme(content: @Composable () -> Unit) {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
        content()
    }
}
