@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur

/** Which end of the scrolling area the effect sits at, and therefore which way its ramp runs. */
enum class AppScrollEdgeSide {
    Top,
    Bottom,
}

/**
 * Apple's scroll edge effect: blur and fade the content that slides under floating chrome.
 *
 * The mechanism matters. Apple's Materials guidance describes it as "blurring and reducing the opacity of
 * background content" — a *treatment of what is behind*, not a plate drawn in front. A colour curtain
 * looks similar over a flat page and is completely wrong over a wallpaper: it hides it in a band.
 *
 * That is what this replaces. The catalog page painted its two edges as a gradient of the page's panel
 * colour at alpha 0.96/0.98, and that colour is `Color.Transparent` while a managed background paints —
 * so `Color.Transparent.copy(alpha = 0.96f)` resolved to 96% *black*. Measured on the AVD: rgb(1,2,3) at
 * the top of the screen and rgb(5,5,5) at the bottom, with the wallpaper only reappearing between them.
 *
 * [tint] is capped at [maxTintAlpha], defaulting to the 35% Apple names for a dimming layer, so the edge
 * can still buy legibility for chrome text without becoming the curtain again.
 */
@Composable
fun AppScrollEdgeEffect(
    backdrop: Backdrop,
    side: AppScrollEdgeSide,
    height: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    blurRadius: Dp = AppScrollEdgeBlurRadius,
    maxTintAlpha: Float = APPLE_DIMMING_LAYER_ALPHA,
) {
    if (height <= 0.dp) return
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                // The mask has to land on this element alone, and `drawPlainBackdrop` draws into a layer
                // of its own that an outer `drawWithCache` cannot reach — hence an explicit one here.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .appScrollEdgeFade(side)
                .drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = { blur(blurRadius.toPx()) },
                    onDrawSurface = {
                        val alpha = maxTintAlpha.coerceIn(0f, 1f)
                        if (alpha > 0f) {
                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            when (side) {
                                                AppScrollEdgeSide.Top ->
                                                    listOf(tint.copy(alpha = alpha), tint.copy(alpha = 0f))
                                                AppScrollEdgeSide.Bottom ->
                                                    listOf(tint.copy(alpha = 0f), tint.copy(alpha = alpha))
                                            },
                                    ),
                            )
                        }
                    },
                ),
    )
}

/**
 * Ramps the whole edge — blur and tint together — out toward the content.
 *
 * `DstIn` against a gradient rather than a `graphicsLayer` alpha, because the fade has to be positional:
 * a flat alpha would leave a visible seam where the band ends.
 *
 * The stops are deliberately not linear. A linear ramp reads as a band with an edge, because the blur
 * stays legible far longer than the alpha suggests; holding full strength briefly and then falling away
 * is what makes it read as the page simply going soft.
 */
private fun Modifier.appScrollEdgeFade(side: AppScrollEdgeSide): Modifier =
    drawWithCache {
        val stops =
            when (side) {
                AppScrollEdgeSide.Top ->
                    arrayOf(
                        0f to Color.Black,
                        0.35f to Color.Black.copy(alpha = 0.82f),
                        0.72f to Color.Black.copy(alpha = 0.28f),
                        1f to Color.Transparent,
                    )

                // The bottom ramp rises much sooner than a symmetric one would. A bottom edge exists to
                // separate floating chrome that sits *inside* the band — the catalog's playback bar starts
                // about 14% down it — so a ramp that is still near zero there buys nothing where it is
                // needed. Measured: the bar's own title collided with a list row until this moved.
                AppScrollEdgeSide.Bottom ->
                    arrayOf(
                        0f to Color.Transparent,
                        0.08f to Color.Black.copy(alpha = 0.35f),
                        0.28f to Color.Black.copy(alpha = 0.78f),
                        0.60f to Color.Black.copy(alpha = 0.96f),
                        1f to Color.Black,
                    )
            }
        val mask = Brush.verticalGradient(colorStops = stops)
        onDrawWithContent {
            drawContent()
            drawRect(brush = mask, blendMode = BlendMode.DstIn)
        }
    }

/** Apple, Materials: "consider adding a dark dimming layer of 35% opacity". */
private const val APPLE_DIMMING_LAYER_ALPHA = 0.35f

private val AppScrollEdgeBlurRadius = 16.dp
