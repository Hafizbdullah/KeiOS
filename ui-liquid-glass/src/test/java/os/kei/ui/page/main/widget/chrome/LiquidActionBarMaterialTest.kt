package os.kei.ui.page.main.widget.chrome

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One material for both action bars.
 *
 * Replaces `LiquidGlassBottomBarMaterialTest`, which pinned the bottom bar's private near-twin of this
 * material — the very numbers the consolidation removed. Pinning them again would have frozen the
 * duplication in place, so these assert the *shared* values and that the private copy stays gone.
 */
class LiquidActionBarMaterialTest {
    @Test
    fun lightMaterialKeepsTheRebuiltReferenceRefraction() {
        val material = liquidActionBarMaterial(isLight = true)

        assertEquals(4.dp, material.blur)
        assertEquals(16.dp, material.lensHeight)
        assertEquals(32.dp, material.lensAmount)
        assertEquals(0.30f, material.surfaceAlpha)
        assertEquals(0.66f, material.highlightAlpha)
    }

    @Test
    fun darkMaterialKeepsTheRebuiltReferenceRefraction() {
        val material = liquidActionBarMaterial(isLight = false)

        assertEquals(4.dp, material.blur)
        assertEquals(16.dp, material.lensHeight)
        assertEquals(28.dp, material.lensAmount)
        assertEquals(0.22f, material.surfaceAlpha)
        assertEquals(0.46f, material.highlightAlpha)
    }

    @Test
    fun bothBarsReadTheSameMaterial() {
        // The regression this guards is a *reintroduced* private material, not a wrong number: the two
        // bars are visible at once, and the bottom bar spent a month at 0.40/1.00 against the toolbar's
        // 0.30/0.66 without anything failing.
        val bottomBar = sourceFile(LIQUID_BOTTOM_BAR_SOURCE).readText()

        assertTrue(
            "The bottom bar must take its material from liquidActionBarMaterial",
            "liquidActionBarMaterial(" in bottomBar,
        )
        assertTrue(
            "The bottom bar must take its palette from rememberLiquidActionBarPalette",
            "rememberLiquidActionBarPalette(" in bottomBar,
        )
        assertFalse(
            "liquidBottomBarMaterial is gone; do not reintroduce a bar-private material",
            "liquidBottomBarMaterial" in bottomBar,
        )
    }

    @Test
    fun theSelectionIndicatorFollowsTheAccent() {
        // Was a flat 10% black/white film that ignored the theme's primary. The dark side still lands on
        // a neutral white film by design; only light mode mixes the accent in.
        val accent = Color(0xFF3B82F6)

        val light = liquidChromeSelectionIndicatorColor(isLight = true, accentColor = accent)
        val dark = liquidChromeSelectionIndicatorColor(isLight = false, accentColor = accent)

        // Delta, not equality: Color quantizes alpha to 8 bits, so 0.26f stores as 66/255 = 0.2588.
        assertEquals(0.26f, light.alpha, 0.01f)
        assertTrue("Light indicator should carry accent, not pure white", light.blue > light.red)
        assertEquals(Color.White.copy(alpha = 0.10f), dark)
    }

    @Test
    fun thePressLensIsNamedRatherThanInlined() {
        assertEquals(10.dp, LiquidBarPressLensHeight)
        assertEquals(14.dp, LiquidBarPressLensAmount)
        assertEquals(6f, LiquidBarPressRefractionStrength)

        val bottomBar = sourceFile(LIQUID_BOTTOM_BAR_SOURCE).readText()
        assertFalse(
            "The press lens should read its numbers from the shared style, not from literals",
            "10f.dp.toPx() * progress" in bottomBar,
        )
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
