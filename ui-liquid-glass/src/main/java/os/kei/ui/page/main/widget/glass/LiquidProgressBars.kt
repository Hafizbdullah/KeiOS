@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.shape.appSquircleClip
import os.kei.ui.page.main.widget.shape.drawAppSquircleBackground

@Composable
fun LiquidLinearProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color = liquidProgressDefaultActiveColor(),
    inactiveColor: Color = liquidProgressDefaultInactiveColor(),
    height: Dp = 4.dp,
    contentDescription: String? = null,
    backdrop: Backdrop? = null,
) {
    LiquidLinearProgressBar(
        progress = progress,
        modifier = modifier,
        valueRange = valueRange,
        activeColor = { activeColor },
        inactiveColor = { inactiveColor },
        height = height,
        contentDescription = contentDescription,
        backdrop = backdrop,
    )
}

@Composable
fun LiquidLinearProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: () -> Color,
    inactiveColor: () -> Color,
    height: Dp = 4.dp,
    contentDescription: String? = null,
    backdrop: Backdrop? = null,
) {
    val liquidControlsEnabled = appGlassRuntimeEffectsEnabled()
    val parentBackdrop = backdrop ?: LocalLiquidParentBackdrop.current
    val glassRuntime = glassEffectRuntime()
    val contentDescriptionState = remember(contentDescription) { contentDescription }
    val progressProvider = progress
    val activeColorProvider = activeColor
    val inactiveColorProvider = inactiveColor
    val safeValueRange =
        remember(valueRange.start, valueRange.endInclusive) {
            liquidFiniteRange(valueRange)
        }
    val progressResolver = remember { LiquidFiniteValueResolver() }
    val safeHeight = liquidSafeProgressDimension(height)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(safeHeight)
                .semantics {
                    contentDescriptionState?.let { this.contentDescription = it }
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(
                            progressResolver.resolve(progressProvider(), safeValueRange),
                            safeValueRange,
                            steps = 0,
                        )
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackBackdrop =
            if (liquidControlsEnabled) {
                rememberLayerBackdrop()
            } else {
                null
            }
        val activeBackdrop =
            when {
                parentBackdrop != null && trackBackdrop != null -> {
                    rememberCombinedBackdrop(parentBackdrop, trackBackdrop)
                }

                trackBackdrop != null -> {
                    trackBackdrop
                }

                else -> {
                    null
                }
            }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .then(
                        if (trackBackdrop != null) {
                            Modifier.layerBackdrop(trackBackdrop)
                        } else {
                            Modifier
                        },
                    ).drawAppSquircleBackground(999.dp) {
                        inactiveColorProvider()
                    },
        )
        Box(
            modifier =
                Modifier
                    .height(safeHeight)
                    .layout { measurable, constraints ->
                        val safeFraction =
                            liquidProgressFraction(
                                value = progressResolver.resolve(progressProvider(), safeValueRange),
                                valueRange = safeValueRange,
                            )
                        val width =
                            (constraints.maxWidth * safeFraction)
                                .fastRoundToInt()
                                .coerceIn(0, constraints.maxWidth)
                        val placeable =
                            measurable.measure(
                                constraints.copy(
                                    minWidth = width,
                                    maxWidth = width,
                                ),
                            )
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }.appSquircleClip(999.dp)
                    .then(
                        if (activeBackdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = activeBackdrop,
                                shape = { ContinuousCapsule },
                                effects = {
                                    vibrancy()
                                    blur((4.dp * glassRuntime.blurScaleFor(GlassVariant.Compact)).toPx())
                                    val lensScale = glassRuntime.lensScaleFor(GlassVariant.Compact)
                                    safeLiquidLens(
                                        12.dp.toPx() * lensScale,
                                        20.dp.toPx() * lensScale,
                                        depthEffect = true,
                                    )
                                },
                                highlight = {
                                    Highlight.Ambient.copy(alpha = 0.52f)
                                },
                                shadow = {
                                    Shadow(radius = 3.dp, color = Color.Black.copy(alpha = 0.06f))
                                },
                                innerShadow = {
                                    InnerShadow(radius = 3.dp, alpha = 0.18f)
                                },
                                onDrawSurface = {
                                    drawRect(activeColorProvider())
                                    drawRect(Color.White.copy(alpha = 0.10f))
                                },
                            )
                        } else {
                            Modifier.drawAppSquircleBackground(999.dp) {
                                activeColorProvider()
                            }
                        },
                    ),
        )
    }
}

@Composable
fun LiquidMusicProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color = liquidProgressDefaultActiveColor(),
    inactiveColor: Color = liquidProgressDefaultInactiveColor(),
    contentDescription: String? = null,
    backdrop: Backdrop? = null,
) {
    LiquidLinearProgressBar(
        progress = progress,
        modifier = modifier,
        valueRange = valueRange,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        height = 3.dp,
        contentDescription = contentDescription,
        backdrop = backdrop,
    )
}

@Composable
fun LiquidMusicProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: () -> Color,
    inactiveColor: () -> Color,
    contentDescription: String? = null,
    backdrop: Backdrop? = null,
) {
    LiquidLinearProgressBar(
        progress = progress,
        modifier = modifier,
        valueRange = valueRange,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        height = 3.dp,
        contentDescription = contentDescription,
        backdrop = backdrop,
    )
}

@Composable
fun LiquidCircularProgressBar(
    progress: (() -> Float)? = null,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color = liquidProgressDefaultActiveColor(),
    inactiveColor: Color = liquidProgressDefaultInactiveColor(),
    size: Dp = 18.dp,
    strokeWidth: Dp = 2.dp,
    contentDescription: String? = null,
) {
    LiquidCircularProgressBar(
        progress = progress,
        modifier = modifier,
        valueRange = valueRange,
        activeColor = { activeColor },
        inactiveColor = { inactiveColor },
        size = size,
        strokeWidth = strokeWidth,
        contentDescription = contentDescription,
    )
}

@Composable
fun LiquidCircularProgressBar(
    progress: (() -> Float)? = null,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: () -> Color,
    inactiveColor: () -> Color,
    size: Dp = 18.dp,
    strokeWidth: Dp = 2.dp,
    contentDescription: String? = null,
) {
    val contentDescriptionState = remember(contentDescription) { contentDescription }
    val progressProvider = progress
    val activeColorProvider = activeColor
    val inactiveColorProvider = inactiveColor
    val safeValueRange =
        remember(valueRange.start, valueRange.endInclusive) {
            liquidFiniteRange(valueRange)
        }
    val progressResolver = remember { LiquidFiniteValueResolver() }
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    val safeSize = liquidSafeProgressDimension(size)
    val indeterminateStates =
        rememberLiquidCircularIndeterminateStates(
            enabled = progressProvider == null && animationsEnabled,
        )
    Canvas(
        modifier =
            modifier
                .size(safeSize)
                .semantics {
                    contentDescriptionState?.let { this.contentDescription = it }
                    if (progressProvider == null) {
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    } else {
                        progressBarRangeInfo =
                            ProgressBarRangeInfo(
                                progressResolver.resolve(progressProvider(), safeValueRange),
                                safeValueRange,
                                steps = 0,
                            )
                    }
                },
    ) {
        val geometry =
            liquidCircularProgressGeometry(
                width = this.size.width,
                height = this.size.height,
                requestedStrokeWidth = strokeWidth.toPx(),
            )
        if (geometry.strokeWidth <= 0f || geometry.arcSize.minDimension <= 0f) return@Canvas
        val strokePx = geometry.strokeWidth
        val arcInset = strokePx / 2f
        val arcSize = geometry.arcSize
        drawArc(
            color = inactiveColorProvider(),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(arcInset, arcInset),
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
        val fraction =
            progressProvider?.let { provider ->
                liquidProgressFraction(
                    value = progressResolver.resolve(provider(), safeValueRange),
                    valueRange = safeValueRange,
                )
            }
        val startAngle =
            if (fraction == null) {
                (indeterminateStates?.rotation?.value ?: 0f) - 90f
            } else {
                -90f
            }
        val sweepAngle =
            if (fraction == null) {
                72f + 148f * (indeterminateStates?.pulse?.value ?: 0f)
            } else {
                (fraction * 360f).coerceIn(0f, 360f)
            }
        drawArc(
            color = activeColorProvider(),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(arcInset, arcInset),
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
        drawArc(
            color = Color.White.copy(alpha = 0.18f),
            startAngle = startAngle,
            sweepAngle = sweepAngle.coerceAtMost(120f),
            useCenter = false,
            topLeft = Offset(arcInset, arcInset),
            size = arcSize,
            style =
                Stroke(
                    width = (strokePx * 0.46f).coerceIn(0f, strokePx),
                    cap = StrokeCap.Round,
                ),
        )
    }
}

@Composable
private fun rememberLiquidCircularIndeterminateStates(enabled: Boolean): LiquidCircularIndeterminateStates? {
    if (!enabled) return null
    val infiniteTransition = rememberInfiniteTransition(label = "liquid-circular-progress")
    val rotation =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1100),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "liquid-circular-progress-rotation",
        )
    val pulse =
        infiniteTransition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.66f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 900),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "liquid-circular-progress-pulse",
        )
    return LiquidCircularIndeterminateStates(
        rotation = rotation,
        pulse = pulse,
    )
}

private data class LiquidCircularIndeterminateStates(
    val rotation: State<Float>,
    val pulse: State<Float>,
)

@Composable
private fun liquidProgressDefaultActiveColor(): Color = if (isSystemInDarkTheme()) Color(0xFF5DAEFF) else Color(0xFF0088FF)

@Composable
private fun liquidProgressDefaultInactiveColor(): Color =
    if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color(0xFF1D1D1F).copy(alpha = 0.15f)
    }

internal fun liquidProgressFraction(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
): Float {
    val safeRange = liquidFiniteRange(valueRange)
    val span = safeRange.endInclusive - safeRange.start
    if (!span.isFinite() || span <= 0f) return 0f
    val safeValue = liquidFiniteValue(value, safeRange)
    val fraction = (safeValue - safeRange.start) / span
    return fraction.takeIf(Float::isFinite)?.fastCoerceIn(0f, 1f) ?: 0f
}

internal fun liquidSafeProgressDimension(value: Dp): Dp =
    value.value
        .takeIf { it.isFinite() && it > 0f }
        ?.dp
        ?: 0.dp

internal fun liquidCircularProgressGeometry(
    width: Float,
    height: Float,
    requestedStrokeWidth: Float,
): LiquidCircularProgressGeometry {
    val safeWidth = width.takeIf { it.isFinite() && it > 0f } ?: 0f
    val safeHeight = height.takeIf { it.isFinite() && it > 0f } ?: 0f
    val maxStrokeWidth = minOf(safeWidth, safeHeight) / 2f
    val strokeWidth =
        requestedStrokeWidth
            .takeIf { it.isFinite() && it > 0f }
            ?.coerceAtMost(maxStrokeWidth)
            ?: 0f
    return LiquidCircularProgressGeometry(
        strokeWidth = strokeWidth,
        arcSize =
            Size(
                width = (safeWidth - strokeWidth).coerceAtLeast(0f),
                height = (safeHeight - strokeWidth).coerceAtLeast(0f),
            ),
    )
}

internal data class LiquidCircularProgressGeometry(
    val strokeWidth: Float,
    val arcSize: Size,
)
