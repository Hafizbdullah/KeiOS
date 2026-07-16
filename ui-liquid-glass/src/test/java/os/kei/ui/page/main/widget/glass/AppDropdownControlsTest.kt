@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = AppDropdownControlsTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class AppDropdownControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyOptionsCollapseExpandedStateAndDisableAnchor() {
        val expandedChanges = mutableListOf<Boolean>()

        composeRule.setContent {
            DropdownTestTheme {
                var expanded by remember { mutableStateOf(true) }
                AppDropdownSelector(
                    selectedText = "No choices",
                    options = emptyList(),
                    selectedIndex = 7,
                    expanded = expanded,
                    anchorBounds = null,
                    onExpandedChange = { nextExpanded ->
                        expandedChanges += nextExpanded
                        expanded = nextExpanded
                    },
                    onSelectedIndexChange = {},
                    onAnchorBoundsChange = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No choices").assertIsNotEnabled()
        assertEquals(1, composeRule.onAllNodes(isRoot()).fetchSemanticsNodes().size)
        assertEquals(listOf(false), expandedChanges)
    }

    @Test
    fun outOfRangeSelectionKeepsChoicesUsableAndClosesAfterSelection() {
        val selections = mutableListOf<Int>()
        var expandedState = true

        composeRule.setContent {
            DropdownTestTheme {
                var expanded by remember { mutableStateOf(true) }
                expandedState = expanded
                AppDropdownSelector(
                    selectedText = "Unknown",
                    options = listOf("First", "Second"),
                    selectedIndex = Int.MAX_VALUE,
                    expanded = expanded,
                    anchorBounds = null,
                    onExpandedChange = { expanded = it },
                    onSelectedIndexChange = { selections += it },
                    onAnchorBoundsChange = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Second") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule
            .onNode(hasText("First") and hasClickAction())
            .assertIsNotSelected()
        composeRule
            .onNode(hasText("Second") and hasClickAction())
            .assertIsNotSelected()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("First") and hasClickAction()).fetchSemanticsNodes().isEmpty()
        }

        assertEquals(listOf(1), selections)
        assertEquals(false, expandedState)
    }

    @Test
    fun popupMaxHeightCapsTallChoiceListGeometry() {
        val popupMaxHeight = 112.dp

        composeRule.setContent {
            DropdownTestTheme {
                AppDropdownSelector(
                    selectedText = "Selected",
                    options = List(10) { index -> "Option ${index + 1}" },
                    selectedIndex = 0,
                    expanded = true,
                    anchorBounds = null,
                    onExpandedChange = {},
                    onSelectedIndexChange = {},
                    onAnchorBoundsChange = {},
                    popupMaxHeight = popupMaxHeight,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Option 1") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        val rootHeights =
            composeRule
                .onAllNodes(isRoot())
                .fetchSemanticsNodes()
                .map { root ->
                    with(composeRule.density) { root.boundsInRoot.height.toDp() }
                }.filter { height -> height > 0.dp }
        val popupHeight = rootHeights.maxOrNull()

        assertTrue(rootHeights.size >= 2, "Expected activity and popup roots, heights=$rootHeights")
        assertTrue(
            popupHeight != null && popupHeight <= popupMaxHeight,
            "Expected popup height <= $popupMaxHeight, heights=$rootHeights",
        )
    }

    @Test
    fun customPopupMinWidthKeepsCompactSelectorGeometry() {
        val popupMinWidth = 136.dp

        composeRule.setContent {
            DropdownTestTheme {
                AppDropdownSelector(
                    selectedText = "A",
                    options = listOf("A", "B"),
                    selectedIndex = 0,
                    expanded = true,
                    anchorBounds = null,
                    onExpandedChange = {},
                    onSelectedIndexChange = {},
                    onAnchorBoundsChange = {},
                    popupMinWidth = popupMinWidth,
                    popupMaxWidth = 196.dp,
                    dropdownItemVariant = GlassVariant.SheetAction,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("B") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        val rootWidths =
            composeRule
                .onAllNodes(isRoot())
                .fetchSemanticsNodes()
                .map { root ->
                    with(composeRule.density) { root.boundsInRoot.width.toDp() }
                }.filter { width -> width > 0.dp }

        assertTrue(rootWidths.size >= 2, "Expected activity and popup roots, widths=$rootWidths")
        assertEquals(popupMinWidth, rootWidths.maxOrNull())
    }
}

@Composable
private fun DropdownTestTheme(content: @Composable () -> Unit) {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Light), content = content)
}

class AppDropdownControlsTestApp : Application()
