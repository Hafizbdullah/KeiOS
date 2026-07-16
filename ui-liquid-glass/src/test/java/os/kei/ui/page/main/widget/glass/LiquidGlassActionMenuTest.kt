@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = LiquidGlassActionMenuTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class LiquidGlassActionMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingSubmenuChoiceCallsChoiceAndDismissesMenu() {
        var selectedSort = "update"
        var selectedInterval = "3h"
        var dismissCount = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 360.dp, height = 420.dp)
                            .background(Color(0xFFF3F4F6))
                            .padding(24.dp),
                ) {
                    LiquidGlassActionMenu(
                        items =
                            listOf(
                                LiquidGlassActionMenuSubmenuRow(
                                    id = "sort",
                                    text = "排序",
                                    subtitle = "更新优先",
                                    submenuItems =
                                        listOf(
                                            LiquidGlassActionMenuSingleChoiceRow(
                                                id = "update",
                                                text = "更新优先",
                                                selected = selectedSort == "update",
                                                onClick = { selectedSort = "update" },
                                            ),
                                            LiquidGlassActionMenuSingleChoiceRow(
                                                id = "name",
                                                text = "名称 A-Z",
                                                selected = selectedSort == "name",
                                                onClick = { selectedSort = "name" },
                                            ),
                                        ),
                                ),
                                LiquidGlassActionMenuSubmenuRow(
                                    id = "interval",
                                    text = "更新间隔",
                                    subtitle = "3 小时",
                                    submenuItems =
                                        listOf(
                                            LiquidGlassActionMenuSingleChoiceRow(
                                                id = "3h",
                                                text = "3 小时",
                                                selected = selectedInterval == "3h",
                                                onClick = { selectedInterval = "3h" },
                                            ),
                                            LiquidGlassActionMenuSingleChoiceRow(
                                                id = "6h",
                                                text = "6 小时",
                                                selected = selectedInterval == "6h",
                                                onClick = { selectedInterval = "6h" },
                                            ),
                                        ),
                                ),
                            ),
                        onDismissRequest = { dismissCount += 1 },
                    )
                }
            }
        }

        composeRule.onNode(hasText("排序") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("名称 A-Z").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule
            .onNode(hasText("名称 A-Z") and hasClickAction())
            .assertIsDisplayed()
            .performClick()

        assertEquals("name", selectedSort)
        assertEquals(1, dismissCount)

        composeRule.onNode(hasText("更新间隔") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("6 小时").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule
            .onNode(hasText("6 小时") and hasClickAction())
            .assertIsDisplayed()
            .performClick()

        assertEquals("6h", selectedInterval)
        assertEquals(2, dismissCount)
    }

    @Test
    fun multipleChoiceRowExposesCheckboxStateAndReportsTheNextValue() {
        var requestedValue: Boolean? = null
        var dismissCount = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ActionMenuTestSurface {
                    LiquidGlassActionMenu(
                        items =
                            listOf(
                                LiquidGlassActionMenuMultipleChoiceRow(
                                    id = "compact_rows",
                                    text = "紧凑行",
                                    checked = true,
                                    onCheckedChange = { checked -> requestedValue = checked },
                                ),
                            ),
                        onDismissRequest = { dismissCount += 1 },
                    )
                }
            }
        }

        val checkboxRole =
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        composeRule
            .onNode(hasText("紧凑行") and hasClickAction())
            .assert(checkboxRole)
            .assertIsOn()
            .performClick()

        assertEquals(false, requestedValue)
        assertEquals(1, dismissCount)
    }

    @Test
    fun replacingItemsWithSameSubmenuIdKeepsSubmenuExpanded() {
        var itemRevision by mutableIntStateOf(0)
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ActionMenuTestSurface {
                    val revision = itemRevision
                    LiquidGlassActionMenu(
                        items =
                            listOf(
                                LiquidGlassActionMenuSubmenuRow(
                                    id = "sort",
                                    text = "排序 ${revision + 1}",
                                    submenuItems =
                                        listOf(
                                            LiquidGlassActionMenuSingleChoiceRow(
                                                id = "name",
                                                text = "名称版本 ${revision + 1}",
                                                selected = false,
                                                onClick = { itemRevision = revision + 1 },
                                            ),
                                        ),
                                ),
                            ),
                    )
                }
            }
        }

        composeRule.onNode(hasText("排序 1") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("名称版本 1").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.runOnIdle { itemRevision = 1 }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("名称版本 2").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText("名称版本 2") and hasClickAction()).assertIsDisplayed()
        composeRule.onNode(hasText("排序 2") and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun backFromSubmenuReturnsToMainMenuBeforeOuterDismiss() {
        var dismissCount = 0
        lateinit var backDispatcher: OnBackPressedDispatcher
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val localBackDispatcher =
                    checkNotNull(LocalOnBackPressedDispatcherOwner.current)
                        .onBackPressedDispatcher
                SideEffect {
                    backDispatcher = localBackDispatcher
                }
                BackHandler { dismissCount += 1 }
                ActionMenuTestSurface {
                    LiquidGlassActionMenu(
                        items =
                            listOf(
                                LiquidGlassActionMenuSubmenuRow(
                                    id = "sort",
                                    text = "排序",
                                    submenuItems =
                                        listOf(
                                            LiquidGlassActionMenuSingleChoiceRow(
                                                id = "name",
                                                text = "名称 A-Z",
                                                selected = false,
                                                onClick = {},
                                            ),
                                        ),
                                ),
                                LiquidGlassActionMenuActionRow(
                                    id = "refresh",
                                    text = "刷新",
                                    onClick = {},
                                ),
                            ),
                        onDismissRequest = { dismissCount += 1 },
                    )
                }
            }
        }

        composeRule.onNode(hasText("排序") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("名称 A-Z").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.runOnIdle { backDispatcher.onBackPressed() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("刷新").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, dismissCount)
        composeRule.onNode(hasText("刷新") and hasClickAction()).assertIsDisplayed()

        composeRule.runOnIdle { backDispatcher.onBackPressed() }
        assertEquals(1, dismissCount)
    }

    @Test
    fun quickActionExposesOneButtonLabelAndDisabledState() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ActionMenuTestSurface {
                    LiquidGlassActionMenu(
                        quickActions =
                            listOf(
                                LiquidGlassActionMenuQuickAction(
                                    id = "info",
                                    icon = MiuixIcons.Regular.Info,
                                    label = "信息",
                                    contentDescription = "查看信息",
                                    enabled = false,
                                    testTag = "quick-info",
                                    onClick = {},
                                ),
                            ),
                        items = emptyList(),
                    )
                }
            }
        }

        val expectedQuickActionSemantics =
            hasContentDescription("查看信息") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and
                isNotEnabled()
        val quickActionNodes =
            composeRule
                .onAllNodes(
                    hasTestTag("quick-info"),
                    useUnmergedTree = true,
                ).assertAll(expectedQuickActionSemantics)
                .fetchSemanticsNodes()
        val labelledNodes =
            composeRule
                .onAllNodes(
                    hasContentDescription("查看信息"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
        assertTrue(quickActionNodes.isNotEmpty())
        assertEquals(quickActionNodes.size, labelledNodes.size)
        composeRule.onAllNodesWithText("信息", useUnmergedTree = true).assertCountEquals(0)
    }
}

@Composable
private fun ActionMenuTestSurface(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(width = 360.dp, height = 420.dp)
                .background(Color(0xFFF3F4F6))
                .padding(24.dp),
    ) {
        content()
    }
}

class LiquidGlassActionMenuTestApp : Application()
