package os.kei.ui.page.main.host.pager

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlin.test.assertNotSame
import kotlin.test.assertSame
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
    application = MainPageBackdropSetTestApp::class,
    sdk = [35],
)
class MainPageBackdropSetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentSceneKeepsLayerProducersBeforeConsumerSlot() {
        val source = sourceFile(MAIN_PAGE_BACKDROP_SET_SOURCE)
        val contentProducerIndex = source.indexOf(".layerBackdrop(contentLayerBackdrop)")
        val sheetProducerIndex = source.indexOf(".layerBackdrop(sheetLayerBackdrop)")
        val consumerIndex = source.indexOf("content()", startIndex = sheetProducerIndex + 1)

        assertTrue(contentProducerIndex >= 0, "Expected a content Backdrop producer")
        assertTrue(sheetProducerIndex > contentProducerIndex, "Sheet producer must follow the content producer")
        assertTrue(consumerIndex > sheetProducerIndex, "Content consumers must follow both producer siblings")
        assertTrue(".matchParentSize()" in source, "The producer must cover the complete page scene")
        assertTrue(
            "contentBackdrop as? LayerBackdrop" in source && "sheetBackdrop as? LayerBackdrop" in source,
            "Canvas materials must bypass page-sized LayerBackdrop producers",
        )
        assertTrue(
            "Box(modifier = modifier)" in source,
            "The scene modifier must remain on the neutral parent rather than the Backdrop producer",
        )
    }

    @Test
    fun solidContentMaterialSharesOnlyTheTopBarLayerIdentity() {
        var backdrops: MainPageBackdropSet? = null

        composeRule.setContent {
            TestTheme {
                backdrops =
                    rememberMainPageBackdropSet(
                        keyPrefix = "solid",
                        distinctLayers = true,
                        useSolidSurfaceBackdrops = true,
                    )
            }
        }

        composeRule.runOnIdle {
            val result = requireNotNull(backdrops)
            assertSame(result.topBar, result.content)
            assertNotSame(result.topBar, result.sheet)
            assertNotSame(result.topBar, result.contentMaterial)
        }
    }

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
    fun collapsedLayersKeepTopBarIndependentByDefault() {
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
            distinctLayers.value = true
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            val expandedAgain = requireNotNull(backdrops)
            assertSame(expanded.topBar, expandedAgain.topBar)
            assertSame(expanded.content, expandedAgain.content)
            assertNotSame(expandedAgain.content, expandedAgain.sheet)
        }
    }

    @Composable
    private fun TestTheme(content: @Composable () -> Unit) {
        MiuixTheme(controller = ThemeController(ColorSchemeMode.Light), content = content)
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

private const val MAIN_PAGE_BACKDROP_SET_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/host/pager/MainPageBackdropSet.kt"

class MainPageBackdropSetTestApp : Application()
