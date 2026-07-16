package os.kei.ui.page.main.student.catalog.component

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    application = BaGuideMemoryLobbyVariantSelectorTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class BaGuideMemoryLobbyVariantSelectorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sourceDelegatesTheCompleteVariantPickerContractToAppDropdownSelector() {
        val source = sourceFile(MEMORY_LOBBY_CARDS_SOURCE)
        val dropdownSource = sourceFile(APP_DROPDOWN_CONTROLS_SOURCE)
        val selectorSource =
            source
                .substringAfter("internal fun BaGuideMemoryLobbyVariantSelector(")
                .substringBefore("private fun BaGuideMemoryLobbyImagePreviewGroup(")
        val sharedSelectorSource =
            dropdownSource
                .substringAfter("fun AppDropdownSelector(")
                .substringBefore("private fun DropdownSelectorChoiceList(")

        assertTrue("AppDropdownSelector(" in selectorSource)
        listOf(
            "AppDropdownAnchorButton(",
            "capturePopupAnchor",
            "SnapshotWindowListPopup(",
            "LiquidGlassDropdownColumn(",
            "LiquidGlassDropdownSingleChoiceItem(",
        ).forEach { manualPrimitive ->
            assertFalse(manualPrimitive in selectorSource, "Found manual dropdown primitive: $manualPrimitive")
        }
        assertSourceContains(
            source = selectorSource,
            "modifier = modifier.widthIn(min = 54.dp, max = 96.dp)",
            "variant = GlassVariant.Compact",
            "textColor = Color(0xFF3B82F6)",
            "minHeight = 36.dp",
            "horizontalPadding = 8.dp",
            "verticalPadding = 6.dp",
            "anchorTextMaxLines = 1",
            "anchorTextOverflow = TextOverflow.Ellipsis",
            "anchorTextSoftWrap = false",
            "anchorTextSize = AppTypographyTokens.Supporting.fontSize",
            "anchorTextLineHeight = AppTypographyTokens.Supporting.lineHeight",
            "dropdownItemTextMaxLines = 1",
            "popupMinWidth = MemoryLobbyVariantMenuMinWidth",
            "popupMaxWidth = MemoryLobbyVariantMenuMaxWidth",
            "popupMaxHeight = MemoryLobbyVariantMenuMaxHeight",
            "dropdownItemVariant = GlassVariant.SheetAction",
            "anchorAlignment = Alignment.Center",
            "alignment = PopupPositionProvider.Align.BottomEnd",
            "placement = SnapshotPopupPlacement.ButtonEnd",
        )
        assertTrue("private val MemoryLobbyVariantMenuMinWidth = 136.dp" in source)
        assertTrue("private val MemoryLobbyVariantMenuMaxWidth = 196.dp" in source)
        assertTrue("private val MemoryLobbyVariantMenuMaxHeight = 220.dp" in source)
        assertSourceContains(
            source = sharedSelectorSource,
            "popupMinWidth: Dp = DropdownSelectorMinWidth",
            "dropdownItemVariant: GlassVariant = variant",
            "val resolvedPopupMinWidth =",
            "variant = dropdownItemVariant",
        )
    }

    @Test
    fun anchorKeepsLegacyWidthAndDensityWithButtonSemantics() {
        composeRule.setContent {
            MemoryLobbyDropdownTestTheme {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BaGuideMemoryLobbyVariantSelector(
                        optionLabels = listOf("A", "B"),
                        selectedIndex = 0,
                        onSelectedIndexChange = {},
                        modifier = Modifier.testTag("short-variant-anchor"),
                    )
                    BaGuideMemoryLobbyVariantSelector(
                        optionLabels = listOf(LONG_VARIANT_LABEL, "B"),
                        selectedIndex = 0,
                        onSelectedIndexChange = {},
                        modifier = Modifier.testTag("long-variant-anchor"),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("short-variant-anchor")
            .assertWidthIsEqualTo(54.dp)
            .assertHeightIsEqualTo(52.dp)
        composeRule
            .onNodeWithTag("long-variant-anchor")
            .assertWidthIsEqualTo(96.dp)
            .assertHeightIsEqualTo(52.dp)
        composeRule
            .onNode(hasText("A") and buttonRoleMatcher and hasClickAction())
            .assertHasClickAction()
            .assertHeightIsEqualTo(48.dp)
        composeRule
            .onNode(hasText(LONG_VARIANT_LABEL) and buttonRoleMatcher and hasClickAction())
            .assertHasClickAction()
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun shortPopupKeepsLegacyMinimumWidthAndRadioSemantics() {
        composeRule.setContent {
            MemoryLobbyDropdownTestTheme {
                BaGuideMemoryLobbyVariantSelector(
                    optionLabels = listOf("A", "B"),
                    selectedIndex = 0,
                    onSelectedIndexChange = {},
                )
            }
        }

        composeRule
            .onNode(hasText("A") and buttonRoleMatcher)
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(radioRoleMatcher).fetchSemanticsNodes().size == 2
        }

        composeRule.onAllNodes(selectedRadioMatcher).assertCountEquals(1)
        composeRule
            .onNode(hasText("A") and radioRoleMatcher)
            .assertIsSelected()
        val rootWidths =
            composeRule
                .onAllNodes(isRoot())
                .fetchSemanticsNodes()
                .map { root ->
                    with(composeRule.density) { root.boundsInRoot.width.toDp() }
                }.filter { width -> width > 0.dp }

        assertTrue(rootWidths.size >= 2, "Expected activity and popup roots, widths=$rootWidths")
        assertEquals(136.dp, rootWidths.maxOrNull())
    }

    @Test
    fun popupScrollsToSelectedRadioCapsGeometryAndClosesAfterChoice() {
        val options =
            List(12) { index ->
                if (index == 10) LONG_VARIANT_LABEL else "Variant ${index + 1}"
            }
        val selectedIndices = mutableListOf<Int>()

        composeRule.setContent {
            MemoryLobbyDropdownTestTheme {
                BaGuideMemoryLobbyVariantSelector(
                    optionLabels = options,
                    selectedIndex = 10,
                    onSelectedIndexChange = selectedIndices::add,
                )
            }
        }

        composeRule
            .onNode(hasText(LONG_VARIANT_LABEL) and buttonRoleMatcher)
            .assertHasClickAction()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodes(radioRoleMatcher)
                .fetchSemanticsNodes()
                .size == options.size
        }

        composeRule.onAllNodes(selectedRadioMatcher).assertCountEquals(1)
        composeRule
            .onNode(hasText(LONG_VARIANT_LABEL) and radioRoleMatcher)
            .assertIsSelected()
            .assertIsDisplayed()

        val rootGeometries =
            composeRule
                .onAllNodes(isRoot())
                .fetchSemanticsNodes()
                .map { root ->
                    with(composeRule.density) {
                        root.boundsInRoot.width.toDp() to root.boundsInRoot.height.toDp()
                    }
                }.filter { (width, height) -> width > 0.dp && height > 0.dp }
        assertTrue(rootGeometries.size >= 2, "Expected activity and popup roots, roots=$rootGeometries")
        val popupGeometry =
            requireNotNull(rootGeometries.maxByOrNull { (_, height) -> height }) {
                "Expected non-empty popup geometry, roots=$rootGeometries"
            }
        assertTrue(
            popupGeometry.first <= 196.dp,
            "Expected popup width <= 196.dp, roots=$rootGeometries",
        )
        assertTrue(
            popupGeometry.second <= 220.dp,
            "Expected popup height <= 220.dp, roots=$rootGeometries",
        )

        composeRule
            .onNode(hasText("Variant 10") and radioRoleMatcher)
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(radioRoleMatcher).fetchSemanticsNodes().isEmpty()
        }
        composeRule.runOnIdle {
            assertEquals(listOf(9), selectedIndices)
        }
    }

    private companion object {
        const val LONG_VARIANT_LABEL =
            "A very long memorial-lobby video variant label that must stay on one line"
        const val MEMORY_LOBBY_CARDS_SOURCE =
            "app/src/main/java/os/kei/ui/page/main/student/catalog/component/BaGuideMemoryLobbyCards.kt"
        const val APP_DROPDOWN_CONTROLS_SOURCE =
            "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/AppDropdownControls.kt"

        val buttonRoleMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        val radioRoleMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        val selectedRadioMatcher =
            radioRoleMatcher and SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
    }
}

private fun sourceFile(relativePath: String): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val sourceFile =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, relativePath) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $relativePath from $workingDirectory"
    }.readText()
}

private fun assertSourceContains(
    source: String,
    vararg expectedFragments: String,
) {
    expectedFragments.forEach { fragment ->
        assertTrue(fragment in source, "Expected source fragment: $fragment")
    }
}

@androidx.compose.runtime.Composable
private fun MemoryLobbyDropdownTestTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Light), content = content)
}

class BaGuideMemoryLobbyVariantSelectorTestApp : Application()
