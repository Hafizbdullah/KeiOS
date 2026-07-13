package os.kei.ui.page.main.host.pager

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertNotSame
import kotlin.test.assertSame
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
    application = MainPageBackdropSetTestApp::class,
    sdk = [35],
)
class MainPageBackdropSetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun distinctLayersUseIndependentBackdropIdentities() {
        var backdrops: MainPageBackdropSet? = null

        composeRule.setContent {
            TestTheme {
                backdrops =
                    rememberMainPageBackdropSet(
                        keyPrefix = "distinct",
                        distinctLayers = true,
                    )
            }
        }

        composeRule.runOnIdle {
            val result = requireNotNull(backdrops)
            assertNotSame(result.topBar, result.content)
            assertNotSame(result.topBar, result.sheet)
            assertNotSame(result.content, result.sheet)
        }
    }

    @Test
    fun collapsedLayersShareOneBackdropByDefault() {
        var backdrops: MainPageBackdropSet? = null

        composeRule.setContent {
            TestTheme {
                backdrops =
                    rememberMainPageBackdropSet(
                        keyPrefix = "shared",
                        distinctLayers = false,
                    )
            }
        }

        composeRule.runOnIdle {
            val result = requireNotNull(backdrops)
            assertSame(result.topBar, result.content)
            assertSame(result.topBar, result.sheet)
        }
    }

    @Test
    fun collapsedLayersCanKeepTopBarDistinct() {
        var backdrops: MainPageBackdropSet? = null

        composeRule.setContent {
            TestTheme {
                backdrops =
                    rememberMainPageBackdropSet(
                        keyPrefix = "topbar-distinct",
                        distinctLayers = false,
                        keepTopBarDistinct = true,
                    )
            }
        }

        composeRule.runOnIdle {
            val result = requireNotNull(backdrops)
            assertNotSame(result.topBar, result.content)
            assertSame(result.content, result.sheet)
        }
    }

    @Test
    fun topBarAndContentIdentitiesStayStableWhenSheetCollapses() {
        val distinctLayers = mutableStateOf(true)
        var backdrops: MainPageBackdropSet? = null

        composeRule.setContent {
            TestTheme {
                backdrops =
                    rememberMainPageBackdropSet(
                        keyPrefix = "stable",
                        distinctLayers = distinctLayers.value,
                        keepTopBarDistinct = true,
                    )
            }
        }

        lateinit var expanded: MainPageBackdropSet
        composeRule.runOnIdle {
            expanded = requireNotNull(backdrops)
            distinctLayers.value = false
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            val collapsed = requireNotNull(backdrops)
            assertSame(expanded.topBar, collapsed.topBar)
            assertSame(expanded.content, collapsed.content)
            assertSame(collapsed.content, collapsed.sheet)
        }
    }

    @Composable
    private fun TestTheme(content: @Composable () -> Unit) {
        MiuixTheme(controller = ThemeController(ColorSchemeMode.Light), content = content)
    }
}

class MainPageBackdropSetTestApp : Application()
