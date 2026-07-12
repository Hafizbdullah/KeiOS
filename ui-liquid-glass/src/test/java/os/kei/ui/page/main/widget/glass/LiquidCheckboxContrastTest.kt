package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Test
import kotlin.test.assertTrue

class LiquidCheckboxContrastTest {
    @Test
    fun lightCheckedSurfaceKeepsWhiteCheckmarkAboveThreeToOne() {
        val checkedSurface =
            liquidCheckboxCheckedSurfaceColor(isDark = false)
                .compositeOver(Color.White)
        val contrast = contrastRatio(liquidCheckboxCheckmarkColor(isDark = false), checkedSurface)

        assertTrue(contrast >= 3f, "Expected light checkbox contrast >= 3:1, got $contrast")
    }

    @Test
    fun darkCheckedSurfaceKeepsDarkCheckmarkAboveThreeToOne() {
        val checkedSurface =
            liquidCheckboxCheckedSurfaceColor(isDark = true)
                .compositeOver(Color(0xFF15181E))
        val contrast = contrastRatio(liquidCheckboxCheckmarkColor(isDark = true), checkedSurface)

        assertTrue(contrast >= 3f, "Expected dark checkbox contrast >= 3:1, got $contrast")
    }

    private fun contrastRatio(
        first: Color,
        second: Color,
    ): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
