package os.kei.ui.page.main.student

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.student.section.GuideSkillMetadataPill
import os.kei.ui.page.main.student.section.GuideSkillMetadataPillKind
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = StudentGuideSkillMetadataPillTestApp::class,
    sdk = [35],
    qualifiers = "w360dp-h800dp-xxhdpi",
)
class StudentGuideSkillMetadataPillTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun metadataPillsPreserveCompactGeometryAndGrowNaturallyAtLargeFont() {
        var density = 1f

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val baseDensity = LocalDensity.current
                density = baseDensity.density
                Column {
                    CompositionLocalProvider(
                        LocalDensity provides Density(baseDensity.density, fontScale = 1f),
                    ) {
                        GuideSkillMetadataPill(
                            label = PRIMARY_ONE_X_LABEL,
                            backdrop = null,
                            kind = GuideSkillMetadataPillKind.Primary,
                            modifier = Modifier.testTag(PRIMARY_ONE_X_TAG),
                        )
                        GuideSkillMetadataPill(
                            label = STATE_ONE_X_LABEL,
                            backdrop = null,
                            kind = GuideSkillMetadataPillKind.State,
                            modifier = Modifier.testTag(STATE_ONE_X_TAG),
                        )
                    }
                    CompositionLocalProvider(
                        LocalDensity provides Density(baseDensity.density, fontScale = 1.5f),
                    ) {
                        GuideSkillMetadataPill(
                            label = PRIMARY_LARGE_FONT_LABEL,
                            backdrop = null,
                            kind = GuideSkillMetadataPillKind.Primary,
                            modifier = Modifier.testTag(PRIMARY_LARGE_FONT_TAG),
                        )
                        GuideSkillMetadataPill(
                            label = STATE_LARGE_FONT_LABEL,
                            backdrop = null,
                            kind = GuideSkillMetadataPillKind.State,
                            modifier = Modifier.testTag(STATE_LARGE_FONT_TAG),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag(PRIMARY_ONE_X_TAG).assertHeightIsEqualTo(32.dp)
        composeRule.onNodeWithTag(STATE_ONE_X_TAG).assertHeightIsEqualTo(30.dp)

        val primaryOneXBounds =
            composeRule.onNodeWithTag(PRIMARY_ONE_X_TAG).fetchSemanticsNode().boundsInRoot
        val stateOneXBounds =
            composeRule.onNodeWithTag(STATE_ONE_X_TAG).fetchSemanticsNode().boundsInRoot
        val primaryLargeFontBounds =
            composeRule.onNodeWithTag(PRIMARY_LARGE_FONT_TAG).fetchSemanticsNode().boundsInRoot
        val stateLargeFontBounds =
            composeRule.onNodeWithTag(STATE_LARGE_FONT_TAG).fetchSemanticsNode().boundsInRoot
        composeRule.runOnIdle {
            assertTrue(primaryLargeFontBounds.height > primaryOneXBounds.height)
            assertTrue(stateLargeFontBounds.height > stateOneXBounds.height)
            assertTrue(primaryLargeFontBounds.height in 37f * density..43f * density)
            assertTrue(stateLargeFontBounds.height in 35f * density..41f * density)
            assertTrue(primaryLargeFontBounds.height < 48f * density)
            assertTrue(stateLargeFontBounds.height < 48f * density)
        }

        listOf(
            PRIMARY_ONE_X_LABEL,
            STATE_ONE_X_LABEL,
            PRIMARY_LARGE_FONT_LABEL,
            STATE_LARGE_FONT_LABEL,
        ).forEach { label ->
            composeRule
                .onNodeWithText(label)
                .assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Role))
                .assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
                .assert(!SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
        }
    }

    @Test
    fun levelSelectorRetainsItsInteractiveTouchTarget() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppDropdownSelector(
                    selectedText = "1",
                    options = listOf("1", "2"),
                    selectedIndex = 0,
                    expanded = false,
                    anchorBounds = null,
                    onExpandedChange = {},
                    onSelectedIndexChange = {},
                    onAnchorBoundsChange = {},
                    modifier = Modifier.testTag(LEVEL_SELECTOR_TAG),
                    backdrop = null,
                    minHeight = 30.dp,
                    horizontalPadding = 10.dp,
                    verticalPadding = 6.dp,
                )
            }
        }

        composeRule.onNodeWithTag(LEVEL_SELECTOR_TAG).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun productionSkillMetadataConsumesTheExistingBackdropWithoutButtonSemantics() {
        val source = sourceFile(GUIDE_SECTION_SKILL_SOURCE)
        val metadataPillSource =
            source
                .substringAfter("internal fun GuideSkillMetadataPill(")
                .substringBefore("internal fun GuideSkillVariantBadge(")

        assertFalse("rememberLayerBackdrop" in source)
        assertFalse(".layerBackdrop(" in source)
        assertTrue("StatusPill(" in metadataPillSource)
        assertTrue("size = AppStatusPillSize.Compact" in metadataPillSource)
        assertTrue("backdrop = backdrop" in metadataPillSource)
        assertTrue("maxLines = 1" in metadataPillSource)
        assertTrue("overflow = TextOverflow.Ellipsis" in metadataPillSource)
        assertTrue("typographyOverride = GuideSkillMetadataPillTypography" in metadataPillSource)
        assertFalse("enabled =" in metadataPillSource)
        assertFalse("onClick =" in metadataPillSource)

        val disabledMetadataButtons =
            source.callBlocks("AppLiquidTextButton").count { call -> "enabled = false" in call }
        assertEquals(0, disabledMetadataButtons)
        assertEquals(4, source.occurrencesOf("GuideSkillMetadataPill("))
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

private fun String.callBlocks(callName: String): List<String> {
    val marker = "$callName("
    val calls = mutableListOf<String>()
    var searchStart = 0
    while (searchStart < length) {
        val callStart = indexOf(marker, startIndex = searchStart)
        if (callStart < 0) break
        var cursor = callStart + callName.length
        var depth = 0
        while (cursor < length) {
            when (this[cursor]) {
                '(' -> depth += 1
                ')' -> {
                    depth -= 1
                    if (depth == 0) {
                        calls += substring(callStart, cursor + 1)
                        searchStart = cursor + 1
                        break
                    }
                }
            }
            cursor += 1
        }
        if (cursor >= length) break
    }
    return calls
}

private fun String.occurrencesOf(needle: String): Int =
    windowed(needle.length).count { candidate -> candidate == needle }

class StudentGuideSkillMetadataPillTestApp : Application()

private const val PRIMARY_ONE_X_LABEL = "EX skill"
private const val STATE_ONE_X_LABEL = "Normal"
private const val PRIMARY_LARGE_FONT_LABEL = "COST:10"
private const val STATE_LARGE_FONT_LABEL = "Enhanced"
private const val PRIMARY_ONE_X_TAG = "skill-metadata-primary-one-x"
private const val STATE_ONE_X_TAG = "skill-metadata-state-one-x"
private const val PRIMARY_LARGE_FONT_TAG = "skill-metadata-primary-large-font"
private const val STATE_LARGE_FONT_TAG = "skill-metadata-state-large-font"
private const val LEVEL_SELECTOR_TAG = "skill-level-selector"
private const val GUIDE_SECTION_SKILL_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/section/GuideSectionSkill.kt"
