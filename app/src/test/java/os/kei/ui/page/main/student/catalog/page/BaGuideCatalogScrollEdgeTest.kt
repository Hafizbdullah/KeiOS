package os.kei.ui.page.main.student.catalog.page

import androidx.compose.ui.graphics.Color
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The catalog's top and bottom edges must treat what is behind them, not paint over it.
 *
 * They used to be gradients of the page's `panelBackground` at alpha 0.96/0.98 — fine while that colour
 * was an opaque panel, and black the moment it became `Color.Transparent` so a managed background could
 * show. Measured on the AVD before the fix: rgb(1,2,3) at the top of the screen and rgb(5,5,5) at the
 * bottom, the wallpaper surviving only in the strip between them. After: rgb(20,24,30) and rgb(80,81,88).
 */
class BaGuideCatalogScrollEdgeTest {
    @Test
    fun tintingWithAColourThatMayBeTransparentPaintsBlack() {
        // The whole bug in one line. `Color.Transparent` is transparent *black*, so re-alpha'ing it does
        // not produce "the panel at 96%" — it produces black at 96%.
        val reAlphaed = Color.Transparent.copy(alpha = 0.96f)

        assertEquals(0f, reAlphaed.red)
        assertEquals(0f, reAlphaed.green)
        assertEquals(0f, reAlphaed.blue)
        // 8-bit channel, so the alpha lands on the nearest 1/255.
        assertEquals(0.96f, reAlphaed.alpha, 1f / 255f)
    }

    @Test
    fun neitherEdgeTintsWithThePanelColour() {
        val source = sourceFile(BA_GUIDE_CATALOG_PAGE_CONTENT_SOURCE)

        assertFalse(
            "panelBackground.copy(alpha" in source,
            "The panel colour is transparent while a background paints, so re-alpha'ing it paints black",
        )
        assertTrue(
            "val scrollEdgeTint = appPageBackdropBaseColor()" in source,
            "The edges must tint with the page's real base colour",
        )
    }

    @Test
    fun bothEdgesUseTheSharedScrollEdgeEffect() {
        val source = sourceFile(BA_GUIDE_CATALOG_PAGE_CONTENT_SOURCE)
        val topIndex = source.indexOf("side = AppScrollEdgeSide.Top,")
        val bottomIndex = source.indexOf("side = AppScrollEdgeSide.Bottom,")

        assertTrue(topIndex >= 0, "The top edge must use the shared effect")
        assertTrue(bottomIndex > topIndex, "The bottom edge must use the shared effect")
        assertEquals(2, source.occurrencesOf("AppScrollEdgeEffect("))
    }

    @Test
    fun theEdgesSampleThePagerRatherThanTheLayerTheyAreDrawnInto() {
        val source = sourceFile(BA_GUIDE_CATALOG_PAGE_CONTENT_SOURCE)

        assertTrue(
            "rememberCombinedBackdrop(managedSceneBackdrop, pageChromeBackdrop)" in source,
            "The edges must blur the wallpaper composite together with the list sliding under them",
        )
        assertEquals(
            2,
            source.occurrencesOf("backdrop = scrollEdgeBackdrop,"),
            "Sampling bottomChromeBackdrop would feed the edges their own output",
        )
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

private const val BA_GUIDE_CATALOG_PAGE_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/page/BaGuideCatalogPageContent.kt"
