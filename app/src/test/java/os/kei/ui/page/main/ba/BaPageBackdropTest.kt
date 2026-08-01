package os.kei.ui.page.main.ba

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.LayerBackdrop
import java.io.File
import kotlin.test.assertEquals
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
    application = BaPageBackdropTestApp::class,
    sdk = [35],
)
class BaPageBackdropTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun baPageBackdropIdentitiesStayIndependentAndStableAfterEntry() {
        lateinit var recompositionSignal: MutableIntState
        var observedBackdrops: Triple<LayerBackdrop, LayerBackdrop, LayerBackdrop>? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val signal = remember { mutableIntStateOf(0) }
                recompositionSignal = signal
                val backdrops = rememberBaPageBackdropSet(pageBackdropEffectsEnabled = true)
                val revision = signal.intValue

                SideEffect {
                    observedBackdrops = Triple(backdrops.topBar, backdrops.content, backdrops.sheet)
                    check(revision >= 0)
                }
                Box(modifier = Modifier.size(1.dp))
            }
        }

        composeRule.waitForIdle()
        lateinit var settledBackdrops: Triple<LayerBackdrop, LayerBackdrop, LayerBackdrop>
        composeRule.runOnIdle {
            settledBackdrops = requireNotNull(observedBackdrops)
            assertNotSame(settledBackdrops.first, settledBackdrops.second)
            assertNotSame(settledBackdrops.first, settledBackdrops.third)
            assertNotSame(settledBackdrops.second, settledBackdrops.third)
            recompositionSignal.intValue += 1
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            val recomposedBackdrops = requireNotNull(observedBackdrops)
            assertSame(settledBackdrops.first, recomposedBackdrops.first)
            assertSame(settledBackdrops.second, recomposedBackdrops.second)
            assertSame(settledBackdrops.third, recomposedBackdrops.third)
        }
    }

    @Test
    fun contentProducerPrecedesBaConsumersAndTopBarKeepsItsOwnProducer() {
        val pageSource = sourceFile(BA_PAGE_SOURCE)
        val contentSource = sourceFile(BA_PAGE_CONTENT_SOURCE)
        val sceneIndex = pageSource.indexOf("MainPageContentBackdropScene(")
        val sceneBackdropIndex =
            pageSource.indexOf("contentBackdrop = backdrops.content", startIndex = sceneIndex.coerceAtLeast(0))
        val scaffoldIndex = pageSource.indexOf("AppScaffold(", startIndex = sceneBackdropIndex.coerceAtLeast(0))
        val contentConsumerIndex =
            pageSource.indexOf("backdrop = backdrops.content", startIndex = scaffoldIndex.coerceAtLeast(0))
        val dockConsumerIndex =
            pageSource.indexOf("BaPageFloatingDock(", startIndex = contentConsumerIndex.coerceAtLeast(0))
        val topBarProducerIndex = contentSource.indexOf(".layerBackdrop(topBarBackdrop)")

        assertTrue(sceneIndex >= 0, "BA page must host one content Backdrop scene")
        assertTrue(sceneBackdropIndex > sceneIndex, "BA scene must produce the content identity")
        assertTrue(scaffoldIndex > sceneBackdropIndex, "Content producer must precede the Scaffold consumer tree")
        assertTrue(contentConsumerIndex > scaffoldIndex, "BA cards must consume the produced content identity")
        assertTrue(dockConsumerIndex > contentConsumerIndex, "Floating dock must be composed after page consumers")
        assertTrue(
            pageSource.indexOf("backdrop = backdrops.topBar", startIndex = dockConsumerIndex) > dockConsumerIndex,
            "Floating dock must sample the scrolling-content identity",
        )
        assertTrue(topBarProducerIndex >= 0, "BA scrolling content must keep the dedicated top-bar producer")
        assertEquals(1, pageSource.occurrencesOf("MainPageContentBackdropScene("))
        assertEquals(1, contentSource.occurrencesOf(".layerBackdrop(topBarBackdrop)"))
        assertEquals(1, pageSource.occurrencesOf("contentBackdrop = backdrops.content"))
        assertEquals(0, pageSource.occurrencesOf(".layerBackdrop(backdrops.content)"))
        assertEquals(0, contentSource.occurrencesOf(".layerBackdrop(backdrop)"))
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

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private const val BA_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BAPage.kt"
private const val BA_PAGE_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaPageContent.kt"

class BaPageBackdropTestApp : Application()
