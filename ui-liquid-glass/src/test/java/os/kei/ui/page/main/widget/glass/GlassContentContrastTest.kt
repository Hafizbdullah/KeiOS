package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Test
import kotlin.test.assertTrue

class GlassContentContrastTest {
    @Test
    fun commonAccentsKeepNormalTextContrastOnLightGlass() {
        val accents =
            mapOf(
                "blue" to Color(0xFF3B82F6),
                "green" to Color(0xFF22C55E),
                "red" to Color(0xFFEF4444),
                "amber" to Color(0xFFF59E0B),
            )

        accents.forEach { (name, accent) ->
            listOf(0.18f, 0.24f).forEach { backgroundAlpha ->
                val foreground =
                    resolveLightGlassContentColor(
                        accent = accent,
                        backgroundAlpha = backgroundAlpha,
                    )
                val background =
                    accent
                        .copy(alpha = backgroundAlpha)
                        .compositeOver(Color.White)
                val contrast = glassContrastRatio(foreground, background)

                assertTrue(
                    contrast >= 4.5f,
                    "Expected $name glass contrast at alpha $backgroundAlpha >= 4.5:1, got $contrast",
                )
            }
        }
    }
}
