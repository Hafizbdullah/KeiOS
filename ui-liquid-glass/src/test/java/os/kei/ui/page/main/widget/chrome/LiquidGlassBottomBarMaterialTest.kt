package os.kei.ui.page.main.widget.chrome

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassBottomBarMaterialTest {
    @Test
    fun lightMaterialPreservesReferenceRefraction() {
        val material = liquidBottomBarMaterial(isLight = true)

        assertEquals(0.40f, material.surfaceAlpha)
        assertEquals(1f, material.highlightAlpha)
        assertEquals(24.dp, material.lensHeight)
        assertEquals(24.dp, material.lensAmount)
    }

    @Test
    fun darkMaterialPreservesReferenceRefraction() {
        val material = liquidBottomBarMaterial(isLight = false)

        assertEquals(0.18f, material.surfaceAlpha)
        assertEquals(0.48f, material.highlightAlpha)
        assertEquals(16.dp, material.lensHeight)
        assertEquals(28.dp, material.lensAmount)
    }

    @Test
    fun lightSelectionIndicatorKeepsAVisibleNeutralFilm() {
        val indicator = liquidBottomBarSelectionIndicatorColor(isLight = true)

        assertEquals(Color.Black.copy(alpha = 0.10f), indicator)
    }

    @Test
    fun darkSelectionIndicatorKeepsAVisibleNeutralFilm() {
        val indicator = liquidBottomBarSelectionIndicatorColor(isLight = false)

        assertEquals(Color.White.copy(alpha = 0.10f), indicator)
    }

    @Test
    fun bottomBarRenderingSourceStaysBelowFileSizeBudget() {
        val lineCount = sourceFile(LIQUID_BOTTOM_BAR_SOURCE).useLines { it.count() }

        assertTrue(
            "LiquidGlassBottomBar.kt has $lineCount lines; keep rendering source below 1000 lines",
            lineCount < 1_000,
        )
    }
}

private fun sourceFile(relativePath: String): File {
    val candidates =
        generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
    return requireNotNull(
        candidates
            .map { File(it, relativePath) }
            .firstOrNull(File::isFile),
    ) {
        "Unable to locate $relativePath from ${System.getProperty("user.dir")}"
    }
}

private const val LIQUID_BOTTOM_BAR_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/chrome/LiquidGlassBottomBar.kt"
