@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS-notification-center style edge stacking for card-dense lazy pages.
 *
 * Cards leaving through the top of the viewport pin at the page's content start line,
 * tuck upward a few dp per depth level while scaling down, and fade out shortly before
 * the lazy layout would dispose them, so scrolling reads as cards collecting into a
 * shallow pile instead of sliding off screen.
 *
 * Wiring: a page provides [LocalAppEdgeStackCards] around its lazy column and tags the
 * column with [appEdgeStackContainer]; [AppSurfaceCard][os.kei.ui.page.main.widget.core.AppSurfaceCard]
 * picks the effect up automatically for every top-level card and blocks nested surfaces
 * from double-transforming. Pages without the provider render unchanged.
 */
@Stable
class AppEdgeStackState internal constructor(
    internal val stackLinePx: Float,
) {
    internal val containerTopInRootY = mutableFloatStateOf(Float.NaN)
}

val LocalAppEdgeStackCards = compositionLocalOf<AppEdgeStackState?> { null }

@Composable
fun rememberAppEdgeStackState(stackLine: Dp): AppEdgeStackState {
    val stackLinePx = with(LocalDensity.current) { stackLine.toPx() }
    return remember(stackLinePx) { AppEdgeStackState(stackLinePx = stackLinePx) }
}

/** Tags the lazy container whose top edge anchors the stack line. */
fun Modifier.appEdgeStackContainer(state: AppEdgeStackState): Modifier =
    onGloballyPositioned { coordinates ->
        val topInRootY = coordinates.positionInRoot().y
        if (state.containerTopInRootY.floatValue != topInRootY) {
            state.containerTopInRootY.floatValue = topInRootY
        }
    }

/**
 * Opts the wrapped cards out of edge stacking inside a providing page. Page overview
 * cards use this: they carry the page's status hub and must scroll away readable
 * instead of being buried under the pile.
 */
@Composable
fun AppEdgeStackExempt(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppEdgeStackCards provides null, content = content)
}

/**
 * Card-side transform. Layout position feeds the draw-phase layer lambda through
 * snapshot state, so scrolling only invalidates draw, never composition. Cards
 * resting below the stack line collapse to a sentinel: they take one snapshot write
 * when leaving the zone and then skip both writes and transform math entirely, so a
 * steadily scrolling list only pays for the few cards at the pile.
 */
@Composable
internal fun Modifier.appEdgeStackedCard(state: AppEdgeStackState): Modifier {
    val itemTopInContainerY = remember { mutableFloatStateOf(APP_EDGE_STACK_RESTING) }
    val itemHeightPx = remember { mutableIntStateOf(0) }
    val tuckRisePx = with(LocalDensity.current) { AppEdgeStackTuckRise.toPx() }
    return this
        .onGloballyPositioned { coordinates ->
            val containerTop = state.containerTopInRootY.floatValue
            val topInContainer =
                if (containerTop.isNaN()) {
                    APP_EDGE_STACK_RESTING
                } else {
                    val relative = coordinates.positionInRoot().y - containerTop
                    if (relative >= state.stackLinePx) APP_EDGE_STACK_RESTING else relative
                }
            if (itemTopInContainerY.floatValue != topInContainer) {
                itemTopInContainerY.floatValue = topInContainer
            }
            val heightPx = coordinates.size.height
            if (itemHeightPx.intValue != heightPx) {
                itemHeightPx.intValue = heightPx
            }
        }
        .graphicsLayer {
            val itemTop = itemTopInContainerY.floatValue
            if (itemTop == APP_EDGE_STACK_RESTING) {
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                alpha = 1f
                return@graphicsLayer
            }
            val transform = computeAppEdgeStackTransform(
                itemTopInContainer = itemTop,
                itemHeightPx = itemHeightPx.intValue.toFloat(),
                stackLinePx = state.stackLinePx,
                tuckRisePx = tuckRisePx,
            )
            translationY = transform.translationY
            scaleX = transform.scale
            scaleY = transform.scale
            alpha = transform.alpha
            transformOrigin = TransformOrigin(0.5f, 0f)
        }
}

data class AppEdgeStackTransform(
    val translationY: Float,
    val scale: Float,
    val alpha: Float,
) {
    companion object {
        val Identity = AppEdgeStackTransform(translationY = 0f, scale = 1f, alpha = 1f)
    }
}

/**
 * Depth progress runs 0..2: 0 at the stack line, 1 when one following card has fully
 * replaced this one, 2 exactly when the item's layout leaves the viewport and the lazy
 * layout disposes it - alpha must reach zero before that point so disposal never pops.
 */
fun computeAppEdgeStackTransform(
    itemTopInContainer: Float,
    itemHeightPx: Float,
    stackLinePx: Float,
    tuckRisePx: Float,
    minScale: Float = APP_EDGE_STACK_MIN_SCALE,
): AppEdgeStackTransform {
    val overshoot = stackLinePx - itemTopInContainer
    if (overshoot <= 0f || itemHeightPx <= 0f) return AppEdgeStackTransform.Identity
    val disposalOvershoot = stackLinePx + itemHeightPx
    val progress = (overshoot / (disposalOvershoot / 2f)).coerceIn(0f, 2f)
    val firstLevel = progress.coerceAtMost(1f)
    val secondLevel = (progress - 1f).coerceIn(0f, 1f)
    val rise = tuckRisePx * (FIRST_LEVEL_RISE_WEIGHT * firstLevel + SECOND_LEVEL_RISE_WEIGHT * secondLevel)
    val scale = 1f - (1f - minScale) * (0.5f * firstLevel + 0.5f * secondLevel)
    val alpha = when {
        progress <= APP_EDGE_STACK_FADE_START -> 1f
        progress >= APP_EDGE_STACK_FADE_END -> 0f
        else ->
            1f - (progress - APP_EDGE_STACK_FADE_START) /
                (APP_EDGE_STACK_FADE_END - APP_EDGE_STACK_FADE_START)
    }
    return AppEdgeStackTransform(
        translationY = overshoot - rise,
        scale = scale,
        alpha = alpha,
    )
}

private val AppEdgeStackTuckRise = 18.dp

/**
 * Top content inset for lists whose page pins an overview block above them: leaves the
 * tuck rise fully visible inside the lazy viewport plus a breathing gap, and doubles as
 * the stack line those lists pass to [rememberAppEdgeStackState].
 */
val AppEdgeStackListTopInset = 26.dp

const val APP_EDGE_STACK_MIN_SCALE = 0.90f
internal const val APP_EDGE_STACK_RESTING = Float.MAX_VALUE
internal const val FIRST_LEVEL_RISE_WEIGHT = 0.55f
internal const val SECOND_LEVEL_RISE_WEIGHT = 0.45f
internal const val APP_EDGE_STACK_FADE_START = 1.2f
internal const val APP_EDGE_STACK_FADE_END = 1.9f
