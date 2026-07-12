package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

internal fun resolveLightGlassContentColor(
    accent: Color,
    backgroundAlpha: Float,
    minimumContrast: Float = 4.5f,
    baseBackground: Color = Color.White,
): Color {
    if (!accent.isSpecified) return Color.Black
    val opaqueAccent = accent.compositeOver(baseBackground)
    val background =
        accent
            .copy(alpha = backgroundAlpha.coerceIn(0f, 1f))
            .compositeOver(baseBackground)
    if (glassContrastRatio(opaqueAccent, background) >= minimumContrast) return opaqueAccent
    for (step in 1..16) {
        val candidate = lerp(opaqueAccent, Color.Black, step / 16f)
        if (glassContrastRatio(candidate, background) >= minimumContrast) return candidate
    }
    return Color.Black
}

internal fun glassContrastRatio(
    foreground: Color,
    background: Color,
): Float {
    val foregroundLuminance = foreground.luminance()
    val backgroundLuminance = background.luminance()
    val lighter = maxOf(foregroundLuminance, backgroundLuminance)
    val darker = minOf(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}
