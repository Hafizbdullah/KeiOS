package os.kei.ui.page.main.widget.core

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@RunWith(AndroidJUnit4::class)
@Config(
    application = AppOverviewCardBackdropTestApp::class,
    sdk = [35],
)
class AppOverviewCardBackdropTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cardExportsStableChildBackdropAndProvidesContentColor() {
        lateinit var recompositionSignal: MutableIntState
        var parentBackdrop: Backdrop? = null
        val observedChildBackdrops = mutableListOf<Backdrop?>()
        val expectedContentColor = Color(0xFF2468AC)
        val observedContentColors = mutableListOf<Color>()

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                androidx.compose.runtime.CompositionLocalProvider(LocalLiquidControlsEnabled provides false) {
                    val signal = remember { mutableIntStateOf(0) }
                    recompositionSignal = signal
                    val parent = rememberLayerBackdrop()
                    val revision = signal.intValue
                    SideEffect { parentBackdrop = parent }

                    AppOverviewCard(
                        title = "Overview",
                        backdrop = parent,
                        contentColor = expectedContentColor,
                        modifier = Modifier.size(180.dp),
                    ) {
                        val child = LocalLiquidParentBackdrop.current
                        val contentColor = LocalContentColor.current
                        SideEffect {
                            check(revision >= 0)
                            observedChildBackdrops += child
                            observedContentColors += contentColor
                        }
                        Box(modifier = Modifier.size(1.dp))
                    }
                }
            }
        }

        composeRule.waitForIdle()
        lateinit var settledChildBackdrop: Backdrop
        composeRule.runOnIdle {
            settledChildBackdrop = assertNotNull(observedChildBackdrops.last())
            assertNotSame(parentBackdrop, settledChildBackdrop)
            assertEquals(expectedContentColor, observedContentColors.last())
            recompositionSignal.intValue += 1
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertSame(settledChildBackdrop, observedChildBackdrops.last())
            assertEquals(expectedContentColor, observedContentColors.last())
        }
    }

    @Test
    fun standaloneCardKeepsParentBackdropAbsentForSolidFallback() {
        var observed = false
        var childBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                androidx.compose.runtime.CompositionLocalProvider(LocalLiquidControlsEnabled provides false) {
                    AppOverviewCard(
                        title = "Standalone",
                        modifier = Modifier.size(180.dp),
                    ) {
                        val child = LocalLiquidParentBackdrop.current
                        SideEffect {
                            observed = true
                            childBackdrop = child
                        }
                        Box(modifier = Modifier.size(1.dp))
                    }
                }
            }
        }

        composeRule.runOnIdle {
            assertTrue(observed)
            assertNull(childBackdrop)
        }
    }

    @Test
    fun sourceExportsCardMaterialWithoutTransparentCaptureFallback() {
        val source = overviewCardSource()

        assertTrue("exportedBackdrop = contentBackdrop" in source)
        assertTrue("LocalLiquidParentBackdrop provides contentBackdrop" in source)
        assertTrue("LocalLiquidParentBackdropOverridesFallback provides true" in source)
        assertTrue("LocalContentColor provides contentColor" in source)
        assertTrue("borderColor = borderColor" in source)
        assertTrue("borderWidth = 1.dp" in source)
        assertEquals(0, source.occurrencesOf(".layerBackdrop("))
        assertEquals(0, source.occurrencesOf("captureBackdrop"))
    }

    @Test
    fun metricTileMaterialsFollowTheAppTheme() {
        val source = overviewCardSource()

        assertFalse("isSystemInDarkTheme" in source)
        assertEquals(2, source.occurrencesOf("isAppInDarkTheme()"))
    }
}

private fun overviewCardSource(): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val sourceFile =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, APP_OVERVIEW_CARD_SOURCE) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $APP_OVERVIEW_CARD_SOURCE from $workingDirectory"
    }.readText()
}

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private const val APP_OVERVIEW_CARD_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/widget/core/AppOverviewCards.kt"

class AppOverviewCardBackdropTestApp : Application()
