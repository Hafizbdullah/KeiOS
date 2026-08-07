@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.dialog

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LocalGlassEffectRuntime
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LocalLiquidOverlayHost
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.clampGlassBlur
import os.kei.ui.page.main.widget.glass.safeLiquidLens
import os.kei.ui.page.main.widget.sheet.LocalSceneBackdrop

private const val LIQUID_MODAL_LENS_SCALE = 0.30f
private const val LIQUID_MODAL_REFRACTION_AMOUNT_SCALE = 1.25f

internal class LiquidModalSurface(
    val modifier: Modifier,
    val exportedBackdrop: Backdrop?,
    val glassEnabled: Boolean,
)

/**
 * A glass card for alerts and action sheets.
 *
 * The fills are **theme-aware and deliberately high**. Two reasons:
 *
 * - The dialog this replaces used `Color.White.copy(alpha = 0.5f)` in both themes, so in dark mode it
 *   was a pale card on a dark app rather than dark glass.
 * - An alert is small and sits over arbitrary content, so it has less blurred area to hide behind than
 *   a sheet does. Apple's guidance is that alerts carry critical information — legibility wins over
 *   translucency here.
 *
 * [scaleProvider] goes into `layerBlock` rather than a `graphicsLayer`, because `drawBackdrop` applies
 * the block as the element's own layer *and* inverse-transforms its sample by it. That keeps the
 * refraction anchored while the card scales in; a plain `graphicsLayer` would magnify the sampled
 * backdrop along with the card and the glass would read as a zooming decal.
 */
@Composable
internal fun rememberLiquidModalSurface(
    cornerRadius: Dp,
    isDark: Boolean,
    explicitBackgroundColor: Color? = null,
    scaleProvider: () -> Float,
): LiquidModalSurface {
    val liquidControlsEnabled = LocalLiquidControlsEnabled.current
    val inOverlay = LocalLiquidOverlayHost.current != null
    val sceneBackdrop = LocalSceneBackdrop.current
    val glassRuntime = LocalGlassEffectRuntime.current
    val cardBackdrop = rememberLayerBackdrop()
    val glassEnabled = liquidControlsEnabled && inOverlay && explicitBackgroundColor == null
    val shape = remember(cornerRadius) { RoundedRectangle(cornerRadius) }

    if (!glassEnabled) {
        return LiquidModalSurface(
            modifier = Modifier
                .graphicsLayer {
                    val scale = scaleProvider()
                    scaleX = scale
                    scaleY = scale
                }.background(
                    color = explicitBackgroundColor ?: liquidModalFallbackFill(isDark),
                    shape = shape,
                ),
            exportedBackdrop = null,
            glassEnabled = false,
        )
    }

    val blurRadius =
        (UiPerformanceBudget.maxGlassBlur * glassRuntime.blurScaleFor(GlassVariant.Floating))
            .clampGlassBlur()
    val lensRadius =
        UiPerformanceBudget.backdropLens *
            LIQUID_MODAL_LENS_SCALE *
            glassRuntime.lensScaleFor(GlassVariant.Floating)
    val fill = liquidModalGlassFill(isDark)

    return LiquidModalSurface(
        modifier = Modifier.drawBackdrop(
            backdrop = sceneBackdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx())
                safeLiquidLens(
                    lensRadius.toPx(),
                    (lensRadius * LIQUID_MODAL_REFRACTION_AMOUNT_SCALE).toPx(),
                    chromaticAberration = false,
                    depthEffect = true,
                )
            },
            layerBlock = {
                val scale = scaleProvider()
                scaleX = scale
                scaleY = scale
            },
            exportedBackdrop = cardBackdrop,
            highlight = { Highlight.Default.copy(alpha = if (isDark) 0.72f else 0.90f) },
            shadow = {
                Shadow.Default.copy(
                    color = Color.Black.copy(alpha = if (isDark) 0.30f else 0.18f),
                )
            },
            innerShadow = { InnerShadow(radius = 10.dp, alpha = if (isDark) 0.20f else 0.12f) },
            onDrawSurface = { drawRect(fill) },
        ),
        exportedBackdrop = cardBackdrop,
        glassEnabled = true,
    )
}

/** The tint painted over the blurred sample. */
internal fun liquidModalGlassFill(isDark: Boolean): Color =
    if (isDark) {
        Color(0xFF171725).copy(alpha = 0.82f)
    } else {
        Color(0xFFF9FAFD).copy(alpha = 0.80f)
    }

/** Used when there is no usable backdrop, so it has to be opaque enough to stand on its own. */
internal fun liquidModalFallbackFill(isDark: Boolean): Color =
    if (isDark) {
        Color(0xFF171725).copy(alpha = 0.98f)
    } else {
        Color(0xFFF9FAFD).copy(alpha = 0.98f)
    }
