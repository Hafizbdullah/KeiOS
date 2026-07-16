package os.kei.ui.page.main.widget.chrome

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class LiquidGlassBottomBarSelectionOpticsContractTest {
    @Test
    fun selectionOpticsRemainOptIn() {
        val source = sourceFile(LIQUID_BOTTOM_BAR_SOURCE)

        assertTrue(
            "selectionOptics: LiquidGlassBottomBarSelectionOptics? = null," in source,
            "Shared bottom bars must retain their existing appearance unless a caller opts in",
        )
        assertEquals(1, source.occurrencesOf("selectionOptics?.let { optics ->"))
    }

    @Test
    fun staticOverlayAndRimStayAboveBackdropAtRestAndDuringPress() {
        val source = sourceFile(LIQUID_BOTTOM_BAR_SOURCE)
        val indicator = source.substring(
            startIndex = source.indexOf("            if (tabWidthPx > 0f) {").requireFound(),
            endIndex = source.indexOf("internal fun liquidBottomBarFinitePosition"),
        )
        val optics = indicator.bracedBlock("selectionOptics?.let { optics ->")
        val backdropIndex = indicator.indexOf("Modifier.drawBackdrop(").requireFound()
        val opticsIndex = indicator.indexOf("selectionOptics?.let { optics ->").requireFound()

        assertTrue(backdropIndex < opticsIndex)
        assertTrue(".appSquircleBackground(optics.overlayColor, 999.dp)" in optics)
        assertTrue("width = optics.rimWidth," in optics)
        assertTrue("color = optics.rimColor," in optics)
        assertEquals(2, optics.occurrencesOf(".matchParentSize()"))
        assertFalse(
            "combinedPressProgressProvider" in optics,
            "Reference overlay and rim remain stable while the backdrop press layers animate",
        )
    }
}

private fun String.bracedBlock(marker: String): String {
    val markerIndex = indexOf(marker).requireFound()
    val openingBrace = indexOf('{', markerIndex).requireFound()
    var depth = 0
    for (index in openingBrace until length) {
        when (this[index]) {
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return substring(markerIndex, index + 1)
            }
        }
    }
    error("Unclosed block for $marker")
}

private fun Int.requireFound(): Int {
    require(this >= 0) { "Expected source marker was not found" }
    return this
}

private fun String.occurrencesOf(value: String): Int = windowed(value.length).count { it == value }

private fun sourceFile(relativePath: String): String {
    val candidates =
        generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
    val sourceFile =
        candidates
            .map { File(it, relativePath) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $relativePath from ${System.getProperty("user.dir")}"
    }.readText()
}

private const val LIQUID_BOTTOM_BAR_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/chrome/LiquidGlassBottomBar.kt"
