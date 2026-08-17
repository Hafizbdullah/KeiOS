package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.shape.drawAppSquircleBorder

/**
 * The film a search surface draws over its glass: a vertical sheen, an edge vignette, a centre
 * specular, a bottom bounce, and one inner rim.
 *
 * ## It used to carry a second copy of the border
 *
 * There was an `edge` colour here too, drawn as a 1.1dp squircle ring. That was the *same ring*
 * `glassStyle(variant = GlassVariant.SearchField)` already defines — same 1.1dp width, and in light
 * mode the same literal `Color(0xFF86C3FF).copy(alpha = 0.32f)`. Both surfaces that use this film draw
 * `glass.borderColor` at `glass.borderWidth` themselves, so the ring was being stroked **twice on the
 * same path**: an effective light-mode alpha of `0.32 + 0.32 * 0.68 ≈ 0.54`, a two-thirds-over-strength
 * border that nothing asked for.
 *
 * Measured, not reasoned: on the light search-shell screenshot the ring pixel over the `(243,244,246)`
 * page read `(194,224,253)` before and `(212,233,252)` after. Compositing the accent `(134,195,255)`
 * once at 0.32 predicts 208; twice at an effective 0.5376 predicts 184. The before/after straddles those
 * two predictions, which is what makes this a double-draw rather than a taste call.
 *
 * Deleted rather than reconciled, because the variant style is where a border for this variant belongs.
 * What is left here is the part `glassStyle` does not describe.
 *
 * ## Why the rim below is not the same mistake
 *
 * `innerRim` survives because it is a different treatment, not another definition of one: it is white
 * in both themes where the border is accent-tinted in light, and it sits a tenth of a dp further in. And
 * neither of them duplicates the `Highlight` the surface passes to `drawBackdrop` — that is a *shader*
 * rim whose brightness follows a 45° light direction around the perimeter, so it cannot express a flat
 * ring of a chosen hue, and a flat ring cannot express it.
 */
data class AppLiquidSearchMaterialColors(
    val overlayTop: Color,
    val overlayBottom: Color,
    val centerGlow: Color,
    val bottomGlow: Color,
    val sideRim: Color,
    val innerRim: Color,
)

fun appLiquidSearchMaterialColors(
    isDark: Boolean,
    compactMaterial: Boolean = false,
): AppLiquidSearchMaterialColors =
    AppLiquidSearchMaterialColors(
        overlayTop =
            if (isDark) {
                Color.White.copy(alpha = if (compactMaterial) 0.020f else 0.045f)
            } else {
                Color.White.copy(alpha = 0.080f)
            },
        overlayBottom =
            if (isDark) {
                Color(0xFF82B8FF).copy(alpha = if (compactMaterial) 0.016f else 0.035f)
            } else {
                Color(0xFFEDF6FF).copy(alpha = 0.050f)
            },
        centerGlow =
            if (isDark) {
                Color(0xFFBBD9FF).copy(alpha = if (compactMaterial) 0.000f else 0.025f)
            } else {
                Color.White.copy(alpha = 0.100f)
            },
        bottomGlow =
            if (isDark) {
                Color(0xFF73AFFF).copy(alpha = if (compactMaterial) 0.012f else 0.025f)
            } else {
                Color(0xFFC6E0FF).copy(alpha = 0.060f)
            },
        sideRim = if (isDark) Color.White.copy(alpha = if (compactMaterial) 0.028f else 0.050f) else Color.White.copy(alpha = 0.16f),
        innerRim = if (isDark) Color.White.copy(alpha = if (compactMaterial) 0.045f else 0.080f) else Color.White.copy(alpha = 0.26f),
    )

fun appLiquidSearchHighlightAlpha(
    baseAlpha: Float,
    materialProgress: Float,
    isDark: Boolean,
    darkMaxAlpha: Float = 0.34f,
): Float {
    val targetAlpha = baseAlpha + 0.05f * materialProgress
    return targetAlpha.coerceAtMost(if (isDark) darkMaxAlpha else 0.68f)
}

fun appLiquidSearchPlaceholderColor(
    contentColor: Color,
    isDark: Boolean,
): Color =
    if (isDark) {
        contentColor.copy(alpha = 0.84f)
    } else {
        contentColor.copy(alpha = 0.62f)
    }

/**
 * How much of a cached full-strength glow brush to draw at [materialProgress].
 *
 * The two glows ramp as `base + gain * progress`. Rebuilding a [Brush] per frame to express that was
 * the old shape and it allocated a gradient plus its shader on every draw of a surface that is on
 * screen permanently — while the other two brushes in the same [drawWithCache] were already hoisted.
 *
 * Caching at `base + gain` and scaling the paint instead is exact *in the alpha*: a paint alpha
 * multiplies every stop, and the far stop of both glows is [Color.Transparent], so scaling leaves it
 * transparent — `base + gain * p == (base + gain) * f` by construction.
 *
 * The rendered pixels are not bit-identical, because the gradient interpolates in 8 bits before the
 * paint alpha multiply, so the dither pattern lands differently. Measured on the light search-shell
 * screenshot: **one 8-bit step of deviation at most**, over the glow interior, on well under one pixel's
 * worth of area. Nothing else in the image moved.
 */
internal fun appLiquidSearchGlowAlphaFraction(
    baseAlpha: Float,
    gain: Float,
    materialProgress: Float,
): Float {
    val peak = baseAlpha + gain
    if (peak <= 0f) return 0f
    return ((baseAlpha + gain * materialProgress) / peak).coerceIn(0f, 1f)
}

/** Extra alpha the centre specular earns as the surface activates. */
internal const val APP_LIQUID_SEARCH_CENTER_GLOW_GAIN = 0.055f

/** Extra alpha the bottom bounce earns as the surface activates. */
internal const val APP_LIQUID_SEARCH_BOTTOM_GLOW_GAIN = 0.035f

fun Modifier.appLiquidSearchMaterialOverlay(
    cornerRadius: Dp,
    colors: AppLiquidSearchMaterialColors,
    focusProgress: Float,
    pressProgress: Float,
): Modifier =
    appLiquidSearchMaterialOverlay(
        cornerRadius = cornerRadius,
        colors = colors,
        focusProgress = { focusProgress },
        pressProgress = { pressProgress },
    )

fun Modifier.appLiquidSearchMaterialOverlay(
    cornerRadius: Dp,
    colors: AppLiquidSearchMaterialColors,
    focusProgress: () -> Float,
    pressProgress: () -> Float,
): Modifier =
    drawWithCache {
        val overlayBrush = Brush.verticalGradient(colors = listOf(colors.overlayTop, colors.overlayBottom))
        val sideBrush =
            Brush.horizontalGradient(
                colors =
                    listOf(
                        colors.sideRim,
                        Color.Transparent,
                        Color.Transparent,
                        colors.sideRim,
                    ),
            )
        val centerGlowBrush =
            Brush.radialGradient(
                colors =
                    listOf(
                        colors.centerGlow.copy(
                            alpha = (colors.centerGlow.alpha + APP_LIQUID_SEARCH_CENTER_GLOW_GAIN).coerceAtMost(1f),
                        ),
                        Color.Transparent,
                    ),
            )
        val bottomGlowBrush =
            Brush.verticalGradient(
                colorStops =
                    arrayOf(
                        0.00f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1.00f to
                            colors.bottomGlow.copy(
                                alpha = (colors.bottomGlow.alpha + APP_LIQUID_SEARCH_BOTTOM_GLOW_GAIN).coerceAtMost(1f),
                            ),
                    ),
            )
        onDrawBehind {
            val materialProgress = maxOf(focusProgress(), pressProgress())
            drawRect(overlayBrush)
            drawRect(sideBrush)
            drawRect(
                brush = centerGlowBrush,
                alpha =
                    appLiquidSearchGlowAlphaFraction(
                        baseAlpha = colors.centerGlow.alpha,
                        gain = APP_LIQUID_SEARCH_CENTER_GLOW_GAIN,
                        materialProgress = materialProgress,
                    ),
            )
            drawRect(
                brush = bottomGlowBrush,
                alpha =
                    appLiquidSearchGlowAlphaFraction(
                        baseAlpha = colors.bottomGlow.alpha,
                        gain = APP_LIQUID_SEARCH_BOTTOM_GLOW_GAIN,
                        materialProgress = materialProgress,
                    ),
            )
        }
    }.drawAppSquircleBorder(
        width = 1.dp,
        cornerRadius = cornerRadius,
    ) {
        val materialProgress = maxOf(focusProgress(), pressProgress())
        colors.innerRim.copy(
            alpha = (colors.innerRim.alpha + 0.08f * materialProgress).coerceAtMost(1f),
        )
    }
