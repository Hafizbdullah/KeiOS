package os.kei.ui.page.main.widget.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp

/**
 * The refraction numbers for a presentation surface: how deep the glass rim is, and how far it bends
 * what is behind it.
 *
 * Two values rather than one because [safeLiquidLens] takes them separately — the height is capped by
 * the shape's smallest corner radius, the amount by its shortest side, so a component that wants a
 * pronounced bend on a modest rim has to say so.
 */
internal class LiquidPresentationLens(
    val refractionHeight: Dp,
    val refractionAmount: Dp,
)

/**
 * The blur every full-material presentation uses — sheet, alert, action sheet, toast, and the scrims
 * behind them.
 *
 * This is the project's ceiling, not the [UiPerformanceBudget.backdropBlur] the floating chrome runs
 * at. The distinction is a legibility one and it was learned the hard way: a capsule in the toolbar
 * gets away with ~4dp because it is small and heavily tinted, but across a sheet-sized surface 4dp
 * merely softens the page and body text stays readable straight through the glass. Anything a user
 * reads *on* the surface needs the ceiling.
 *
 * Scaled by the reduced-effects runtime so lowering glass lowers this too, then clamped, because the
 * scale is a multiplier and the ceiling is a ceiling.
 *
 * Four files had this same expression inlined. Frame cost lives here, so it should be one line to
 * find and one line to change.
 */
@Composable
@ReadOnlyComposable
internal fun presentationGlassBlur(): Dp =
    (UiPerformanceBudget.maxGlassBlur * glassEffectRuntime().blurScaleFor(GlassVariant.Floating))
        .clampGlassBlur()

/**
 * The refraction rim for a presentation surface. This is what makes an edge read as a thick piece of
 * glass rather than a translucent rectangle.
 *
 * [lensScale] is the component's share of [UiPerformanceBudget.backdropLens] — a sheet's rim is a
 * larger fraction than an alert's because it has more edge to sell. [refractionScale] pushes the bend
 * past the rim depth, which is what keeps a shallow rim from looking flat.
 *
 * Unlike the blur there is no clamp: [safeLiquidLens] already bounds both values against the actual
 * shape at draw time, which is the only place the real corner radii are known.
 */
@Composable
@ReadOnlyComposable
internal fun presentationGlassLens(
    lensScale: Float,
    refractionScale: Float,
): LiquidPresentationLens {
    val height =
        UiPerformanceBudget.backdropLens *
            lensScale *
            glassEffectRuntime().lensScaleFor(GlassVariant.Floating)
    return LiquidPresentationLens(
        refractionHeight = height,
        refractionAmount = height * refractionScale,
    )
}
