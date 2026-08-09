@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import os.kei.ui.page.main.widget.isAppInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.animation.InteractiveHighlight
import os.kei.ui.page.main.widget.chrome.snapChromeTranslationPx
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import os.kei.ui.page.main.widget.shape.appSquircleBorder
import os.kei.ui.page.main.widget.shape.appSquircleClip
import os.kei.ui.page.main.widget.shape.appSquircleSurface
import os.kei.ui.page.main.widget.shape.drawAppSquircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidSurface(
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    shape: Shape = ContinuousCapsule,
    enabled: Boolean = true,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    blurRadius: Dp = UiPerformanceBudget.backdropBlur,
    lensRadius: Dp = UiPerformanceBudget.backdropLens,
    effectVariant: GlassVariant? = null,
    chromaticAberration: Boolean = false,
    depthEffect: Boolean = false,
    highlightAlpha: Float? = null,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 0.dp,
    shadow: Boolean = true,
    shadowAlpha: Float = 0.10f,
    exportedBackdrop: LayerBackdrop? = null,
    /**
     * This card's place in a host page's card pile, when it is a page card on a stacking page.
     *
     * Threaded in rather than read from the composition local here because only the page-card
     * wrappers should stack — a pill or a button inside a card must not — and because the transform
     * has to end up inside [drawBackdrop]'s `layerBlock` so the sampled backdrop is
     * inverse-corrected for it. Defaults to inert, which is byte-identical to not having the
     * parameter.
     */
    edgeStack: AppEdgeStackSlot = AppEdgeStackSlot.Inert,
    interactionSource: MutableInteractionSource? = null,
    role: Role = Role.Button,
    selected: Boolean? = null,
    toggleableState: ToggleableState? = null,
    consumeDragChanges: Boolean = false,
    clipContent: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val isDark = isAppInDarkTheme()
    val animationScope = rememberCoroutineScope()
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val interactiveHighlight =
        remember(animationScope, consumeDragChanges, transitionAnimationsEnabled) {
            InteractiveHighlight(
                animationScope = animationScope,
                consumeDragChanges = consumeDragChanges,
                animationsEnabled = transitionAnimationsEnabled,
            )
        }
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val clickableModifier =
        if (onClick != null) {
            Modifier.clickable(
                interactionSource = resolvedInteractionSource,
                indication = if (isInteractive) null else LocalIndication.current,
                enabled = enabled,
                role = role,
                onClick = onClick,
            )
        } else {
            Modifier
        }
    val stateSemanticsModifier =
        if (selected != null || toggleableState != null) {
            Modifier.semantics {
                selected?.let { this.selected = it }
                toggleableState?.let { this.toggleableState = it }
            }
        } else {
            Modifier
        }

    val stackCard = edgeStack.card
    val interactiveTransformEnabled = isInteractive && enabled
    // Both transforms compose into one layer block, and the pile's contribution is applied on top of
    // the press deformation rather than replacing it, so a card at the front of the pile still
    // responds to touch.
    val interactiveLayerBlock: (GraphicsLayerScope.() -> Unit)? =
        remember(interactiveTransformEnabled, interactiveHighlight, stackCard) {
            when {
                interactiveTransformEnabled && stackCard != null -> {
                    {
                        applyLiquidSurfaceInteractiveTransform(interactiveHighlight)
                        applyAppEdgeStackTransform(stackCard)
                    }
                }

                interactiveTransformEnabled -> {
                    { applyLiquidSurfaceInteractiveTransform(interactiveHighlight) }
                }

                stackCard != null -> {
                    { applyAppEdgeStackTransform(stackCard) }
                }

                else -> null
            }
        }
    val activeBackdrop = activeGlassBackdrop(backdrop)
    val effectiveBlurRadius =
        effectVariant?.let { resolvedGlassBlurDp(blurRadius, it) } ?: blurRadius
    val effectiveLensRadius =
        effectVariant?.let { resolvedGlassLensDp(lensRadius, it) } ?: lensRadius
    val optimizedCornerRadius = appLiquidOptimizedCornerRadius(shape)
    val fallbackSurfaceColor =
        when {
            surfaceColor.isSpecified && surfaceColor.alpha > 0f -> surfaceColor
            else -> MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.84f)
        }
    val interactionModifier =
        if (isInteractive && enabled) {
            Modifier
                .then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier)
        } else {
            Modifier
        }
    val borderModifier =
        if (borderWidth > 0.dp && borderColor.isSpecified && borderColor.alpha > 0f) {
            Modifier.appLiquidOptimizedBorder(
                shape = shape,
                optimizedCornerRadius = optimizedCornerRadius,
                color = borderColor,
                width = borderWidth,
            )
        } else {
            Modifier
        }
    val contentAlphaModifier = liquidSurfaceContentAlphaModifier(enabled)
    // The shadow is sized to the surface rather than to a constant. `Shadow.Default` is a fixed 24dp
    // blur with a 4dp drop, which is right for a card and badly wrong for a checkbox: once the blur
    // radius dwarfs the corner radius, the blurred silhouette collapses towards the bounding box and
    // what shows under a rounded corner is a right angle. That is the square-cornered shadow, and it
    // is geometry rather than clipping — every small glass control in the app was drawing a card's
    // shadow.
    val surfaceMinDimensionDp = remember { mutableFloatStateOf(0f) }
    val outerShadow: (() -> Shadow?)? =
        liquidSurfaceOuterShadowOrNull(
            enabled = shadow,
            alpha = shadowAlpha,
            minDimensionDp = { surfaceMinDimensionDp.floatValue },
        )
    val density = LocalDensity.current
    // Only surfaces that actually cast a shadow pay for the probe. Written during layout and read in
    // the shadow lambda during draw, which is the safe direction and lands in the same frame.
    val shadowSizeProbeModifier =
        if (outerShadow != null) {
            remember(density, surfaceMinDimensionDp) {
                Modifier.onSizeChanged { size ->
                    val resolved = with(density) { minOf(size.width, size.height).toDp().value }
                    if (surfaceMinDimensionDp.floatValue != resolved) {
                        surfaceMinDimensionDp.floatValue = resolved
                    }
                }
            }
        } else {
            Modifier
        }
    val innerShadow: (() -> InnerShadow?)? =
        if (liquidSurfaceNeedsInteractiveInnerShadow(isInteractive = isInteractive, enabled = enabled)) {
            {
                val progress = interactiveHighlight.pressProgress
                InnerShadow(radius = 6.dp * progress, alpha = 0.55f * progress)
            }
        } else {
            null
        }
    // Depth is drawn as a scrim over the card's own surface, shaped to the card, and it has to sit
    // inside the pile's transform layer so it scales and travels with the card. Chained after the
    // surface modifier, it lands after the backdrop and before the content, which is where a
    // recession belongs. Shape.createOutline rather than the optimized corner radius, because that
    // helper only resolves circles and capsules — every page card is a RoundedRectangle and would
    // have fallen through to null.
    // Slides the card's content out of focus as it recedes, leaving the surface crisp. Sits at the end
    // of the chain, where the existing disabled-content alpha already proves a layer there affects the
    // content without touching the surface the earlier modifiers drew.
    val stackContentFadeModifier =
        if (stackCard != null) {
            Modifier.graphicsLayer {
                applyAppEdgeStackContentRecession(stackCard)
                clip = false
            }
        } else {
            Modifier
        }
    val stackRecessionModifier =
        if (stackCard != null) {
            Modifier.drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                onDrawBehind {
                    val dimAlpha = appEdgeStackDimAlpha(stackCard, isDark)
                    if (dimAlpha > 0f) {
                        drawOutline(outline, color = AppEdgeStackDimColor.copy(alpha = dimAlpha))
                    }
                }
            }
        } else {
            Modifier
        }
    val surfaceModifier =
        if (activeBackdrop != null) {
            Modifier.drawBackdrop(
                backdrop = activeBackdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(effectiveBlurRadius.toPx())
                    safeLiquidLens(
                        effectiveLensRadius.toPx(),
                        effectiveLensRadius.toPx(),
                        chromaticAberration = chromaticAberration,
                        depthEffect = depthEffect,
                    )
                    // Radial refraction from touch point for interactive surfaces
                    if (isInteractive && enabled && interactiveHighlight.pressProgress > 0f) {
                        radialRefraction(
                            centerX = interactiveHighlight.touchPosition.x,
                            centerY = interactiveHighlight.touchPosition.y,
                            radius = effectiveLensRadius.toPx() * 2f,
                            strength = 6f * interactiveHighlight.pressProgress,
                        )
                    }
                },
                highlight = {
                    Highlight.Default.copy(
                        alpha =
                            liquidSurfaceHighlightAlpha(
                                isDark = isDark,
                                interactive = isInteractive,
                                enabled = enabled,
                                pressProgress = interactiveHighlight.pressProgress,
                                overrideAlpha = highlightAlpha,
                            ),
                    )
                },
                shadow = outerShadow,
                innerShadow = innerShadow,
                layerBlock = interactiveLayerBlock,
                exportedBackdrop = exportedBackdrop,
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = tint.alpha * 0.70f))
                    }
                    if (surfaceColor.isSpecified && surfaceColor.alpha > 0f) {
                        drawRect(surfaceColor)
                    }
                },
            )
        } else {
            // No backdrop to hand the transform to, so the pile needs its own layer here. Only the
            // stack's contribution — the press deformation has never been applied on this path and
            // adding it would change every fallback surface in the app.
            val fallbackStackLayer =
                if (stackCard != null) {
                    Modifier.graphicsLayer { applyAppEdgeStackTransform(stackCard) }
                } else {
                    Modifier
                }
            fallbackStackLayer.appLiquidOptimizedSurface(
                shape = shape,
                optimizedCornerRadius = optimizedCornerRadius,
                color = fallbackSurfaceColor,
            )
        }
    if (clipContent) {
        Box(
            modifier =
                modifier
                    .then(shadowSizeProbeModifier)
                    .then(surfaceModifier)
                    .then(stackRecessionModifier)
                    .then(borderModifier)
                    .then(clickableModifier)
                    .then(stateSemanticsModifier)
                    .then(interactionModifier)
                    .then(contentAlphaModifier)
                    .then(stackContentFadeModifier),
            contentAlignment = contentAlignment,
            content = content,
        )
    } else {
        Box(
            modifier =
                modifier
                    .then(clickableModifier)
                    .then(stateSemanticsModifier)
                    .then(interactionModifier)
                    .then(contentAlphaModifier),
            contentAlignment = contentAlignment,
        ) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .then(shadowSizeProbeModifier)
                        .then(surfaceModifier)
                        .then(stackRecessionModifier)
                        .then(borderModifier)
                        .graphicsLayer { clip = false },
            )
            Box(
                modifier =
                    Modifier.graphicsLayer {
                        interactiveLayerBlock?.invoke(this)
                        stackCard?.let { applyAppEdgeStackContentRecession(it) }
                        clip = false
                    },
                contentAlignment = contentAlignment,
                content = content,
            )
        }
    }
}

private val LiquidSurfaceDisabledContentAlphaModifier =
    Modifier.graphicsLayer {
        alpha = AppInteractiveTokens.disabledContentAlpha
        clip = false
    }

internal fun liquidSurfaceContentAlphaModifier(enabled: Boolean): Modifier =
    if (enabled) Modifier else LiquidSurfaceDisabledContentAlphaModifier

internal fun liquidSurfaceOuterShadowOrNull(
    enabled: Boolean,
    alpha: Float,
    minDimensionDp: () -> Float = { 0f },
): (() -> Shadow?)? {
    val resolvedAlpha = alpha.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f
    if (!enabled || resolvedAlpha <= 0f) return null
    return {
        val radius = liquidSurfaceShadowRadius(minDimensionDp())
        Shadow(
            radius = radius,
            offset = DpOffset(0.dp, radius / LIQUID_SHADOW_OFFSET_DIVISOR),
            color = Color.Black.copy(alpha = resolvedAlpha),
        )
    }
}

/**
 * Blur radius for a surface's drop shadow, scaled to the surface's shorter side.
 *
 * A shadow only reads as a shadow while its blur stays small relative to the corner it is tracing.
 * `Shadow.Default`'s fixed 24dp blur spreads `radius * 2` in every direction, so on a 22dp checkbox it
 * is drawing a silhouette four times the size of the control — and a silhouette that large has no
 * visible corner rounding left, which is why it reads as a right-angled smudge behind a rounded shape.
 * Half the shorter side keeps the blur proportionate; the ceiling is the previous constant, so cards
 * and sheets are unchanged.
 *
 * Falls back to the ceiling before the first measurement, so a surface never flashes a hard shadow on
 * its first frame.
 */
internal fun liquidSurfaceShadowRadius(minDimensionDp: Float): Dp {
    if (!minDimensionDp.isFinite() || minDimensionDp <= 0f) return LiquidShadowRadiusMax
    return (minDimensionDp * LIQUID_SHADOW_RADIUS_RATIO).dp
        .coerceIn(LiquidShadowRadiusMin, LiquidShadowRadiusMax)
}

/**
 * A drop shadow proportioned to a control whose shorter side is [minDimension].
 *
 * For the surfaces that call `drawBackdrop` directly instead of going through [LiquidSurface] and so
 * cannot use the measured probe. Pass the control's own height — never `Shadow.Default`, whose 24dp
 * blur is a card's shadow and turns a small rounded control's shadow into a right-angled smudge.
 */
internal fun liquidGlassShadow(
    minDimension: Dp,
    color: Color,
): Shadow {
    val radius = liquidSurfaceShadowRadius(minDimension.value)
    return Shadow(
        radius = radius,
        offset = DpOffset(0.dp, radius / LIQUID_SHADOW_OFFSET_DIVISOR),
        color = color,
    )
}

private const val LIQUID_SHADOW_RADIUS_RATIO = 0.5f

/** Matches the proportion `Shadow.Default` uses between its own radius and drop. */
private const val LIQUID_SHADOW_OFFSET_DIVISOR = 6f

internal val LiquidShadowRadiusMin = 5.dp

/** The old fixed radius, kept as the ceiling so anything card-sized looks exactly as it did. */
internal val LiquidShadowRadiusMax = 24.dp

internal fun liquidSurfaceNeedsInteractiveInnerShadow(
    isInteractive: Boolean,
    enabled: Boolean,
): Boolean = isInteractive && enabled

internal fun liquidSurfaceHighlightAlpha(
    isDark: Boolean,
    interactive: Boolean,
    enabled: Boolean,
    pressProgress: Float,
    overrideAlpha: Float? = null,
): Float {
    overrideAlpha
        ?.takeIf(Float::isFinite)
        ?.let { return it.coerceIn(0f, 1f) }

    val baseAlpha = if (isDark) 0.42f else 0.62f
    val interactionBoost =
        if (interactive && enabled) {
            val safeProgress = pressProgress.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f
            0.10f * safeProgress
        } else {
            0f
        }
    return baseAlpha + interactionBoost
}

private fun GraphicsLayerScope.applyLiquidSurfaceInteractiveTransform(interactiveHighlight: InteractiveHighlight) {
    if (size.width <= 0f || size.height <= 0f) return
    val progress = interactiveHighlight.deformationProgress
    val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, progress)
    val maxOffset = size.minDimension
    val offset = interactiveHighlight.offset
    val initialDerivative = 0.05f
    translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
    translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

    val maxDragScale = 4.dp.toPx() / size.height
    val offsetAngle = atan2(offset.y, offset.x)
    scaleX = scale +
        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
        (size.width / size.height).fastCoerceAtMost(1f)
    scaleY = scale +
        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
        (size.height / size.width).fastCoerceAtMost(1f)
}

@Composable
fun AppLiquidFloatingSurface(
    modifier: Modifier,
    shape: Shape = ContinuousCapsule,
    backdrop: Backdrop? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    clipContent: Boolean = true,
    consumeTouches: Boolean = false,
    pressDurationMillis: Int = 130,
    pressLabel: String = "app_liquid_floating_surface_press",
    pressSafePadding: Dp = Dp.Unspecified,
    content: @Composable BoxScope.() -> Unit,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by resolvedInteractionSource.collectIsPressedAsState()
    val pressProgressState =
        appMotionFloatState(
            targetValue = if (pressed) 1f else 0f,
            durationMillis = pressDurationMillis,
            label = pressLabel,
        )
    val pressProgressProvider = remember(pressProgressState) { { pressProgressState.value } }
    val floatingMinDimensionDp = remember { mutableFloatStateOf(0f) }
    val floatingDensity = LocalDensity.current
    val floatingShadowProbe =
        remember(floatingDensity, floatingMinDimensionDp) {
            Modifier.onSizeChanged { size ->
                val resolved = with(floatingDensity) { minOf(size.width, size.height).toDp().value }
                if (floatingMinDimensionDp.floatValue != resolved) {
                    floatingMinDimensionDp.floatValue = resolved
                }
            }
        }
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val deformationProgressProvider =
        remember(pressProgressProvider, transitionAnimationsEnabled) {
            {
                if (transitionAnimationsEnabled) {
                    pressProgressProvider()
                } else {
                    0f
                }
            }
        }
    val density = LocalDensity.current
    val pressLiftPx = with(density) { 1.25.dp.toPx() }
    val isDark = isAppInDarkTheme()
    val surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.20f else 0.40f)
    val overlayColor =
        if (isDark) {
            Color.White.copy(alpha = 0.04f)
        } else {
            Color.White.copy(alpha = 0.08f)
        }
    val borderColor =
        if (isDark) {
            Color.White.copy(alpha = 0.18f)
        } else {
            Color.White.copy(alpha = 0.54f)
        }
    val resolvedPressSafePadding =
        if (pressSafePadding == Dp.Unspecified) {
            if (onClick != null || consumeTouches) {
                AppInteractiveTokens.denseLiquidPressSafePadding
            } else {
                0.dp
            }
        } else {
            pressSafePadding
        }
    val activeBackdrop = activeGlassBackdrop(backdrop)
    val effectBlurRadius = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Floating)
    val effectLensRadius = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Floating)
    val optimizedCornerRadius = appLiquidOptimizedCornerRadius(shape)
    val pressVisualTransform: GraphicsLayerScope.() -> Unit = {
        val pressProgress = deformationProgressProvider()
        translationY = snapChromeTranslationPx(-pressLiftPx * pressProgress)
        scaleX = lerp(1f, 1.010f, pressProgress)
        scaleY = lerp(1f, 0.992f, pressProgress)
        clip = false
    }

    Box(
        modifier =
            modifier
                .padding(resolvedPressSafePadding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(floatingShadowProbe)
                    .then(
                        if (activeBackdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = activeBackdrop,
                                shape = { shape },
                                effects = {
                                    vibrancy()
                                    blur(effectBlurRadius.toPx())
                                    val pressProgress = pressProgressProvider()
                                    safeLiquidLens(
                                        (
                                            effectLensRadius *
                                                (0.90f + 0.08f * pressProgress)
                                        ).toPx(),
                                        (
                                            effectLensRadius *
                                                (0.90f + 0.10f * pressProgress)
                                        ).toPx(),
                                    )
                                },
                                highlight = {
                                    val pressProgress = pressProgressProvider()
                                    Highlight.Default.copy(
                                        alpha = (if (isDark) 0.46f else 0.82f) + 0.06f * pressProgress,
                                    )
                                },
                                shadow = {
                                    val pressProgress = pressProgressProvider()
                                    val shadowAlpha = (if (isDark) 0.12f else 0.05f) * (1f - 0.35f * pressProgress)
                                    // Floating chrome runs from a small round button to a tall dock, so
                                    // the radius is measured rather than assumed.
                                    liquidGlassShadow(
                                        minDimension = floatingMinDimensionDp.floatValue.dp,
                                        color = Color.Black.copy(alpha = shadowAlpha),
                                    )
                                },
                                layerBlock = pressVisualTransform,
                                exportedBackdrop = exportedBackdrop,
                                onDrawSurface = { drawRect(surfaceColor) },
                            )
                        } else {
                            Modifier
                                .graphicsLayer(block = pressVisualTransform)
                                .appLiquidOptimizedSurface(
                                    shape = shape,
                                    optimizedCornerRadius = optimizedCornerRadius,
                                    color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
                                )
                        },
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(block = pressVisualTransform)
                    .appLiquidOptimizedBorder(
                        shape = shape,
                        optimizedCornerRadius = optimizedCornerRadius,
                        color = {
                            val pressProgress = pressProgressProvider()
                            borderColor.copy(alpha = borderColor.alpha * (1f - 0.72f * pressProgress))
                        },
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(block = pressVisualTransform)
                    .then(
                        if (clipContent) {
                            Modifier.appLiquidOptimizedClip(
                                shape = shape,
                                optimizedCornerRadius = optimizedCornerRadius,
                            )
                        } else {
                            Modifier
                        },
                    ),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (!clipContent) {
                                Modifier.appLiquidOptimizedClip(
                                    shape = shape,
                                    optimizedCornerRadius = optimizedCornerRadius,
                                )
                            } else {
                                Modifier
                            },
                        ).background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        overlayColor,
                                        overlayColor.copy(alpha = overlayColor.alpha * 0.52f),
                                    ),
                            ),
                        ),
            )
            if (consumeTouches && onClick == null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (!clipContent) {
                                    Modifier.appLiquidOptimizedClip(
                                        shape = shape,
                                        optimizedCornerRadius = optimizedCornerRadius,
                                    )
                                } else {
                                    Modifier
                                },
                            ).clickable(
                                interactionSource = resolvedInteractionSource,
                                indication = null,
                                onClick = {},
                            ),
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(
                            when {
                                onClick != null -> {
                                    Modifier.clickable(
                                        interactionSource = resolvedInteractionSource,
                                        indication = null,
                                        onClick = onClick,
                                    )
                                }

                                else -> {
                                    Modifier
                                }
                            },
                        ),
                contentAlignment = Alignment.Center,
                content = content,
            )
        }
    }
}

private fun appLiquidOptimizedCornerRadius(shape: Shape): Dp? =
    when (shape) {
        CircleShape, ContinuousCapsule -> 999.dp
        else -> null
    }

@Composable
private fun Modifier.appLiquidOptimizedClip(
    shape: Shape,
    optimizedCornerRadius: Dp?,
): Modifier =
    if (optimizedCornerRadius != null) {
        appSquircleClip(optimizedCornerRadius)
    } else {
        clip(shape)
    }

@Composable
private fun Modifier.appLiquidOptimizedSurface(
    shape: Shape,
    optimizedCornerRadius: Dp?,
    color: Color,
): Modifier =
    if (optimizedCornerRadius != null) {
        appSquircleSurface(color = color, cornerRadius = optimizedCornerRadius)
    } else {
        clip(shape).background(color)
    }

@Composable
private fun Modifier.appLiquidOptimizedBorder(
    shape: Shape,
    optimizedCornerRadius: Dp?,
    color: Color,
    width: Dp = 1.dp,
): Modifier =
    if (optimizedCornerRadius != null) {
        appSquircleBorder(width = width, color = color, cornerRadius = optimizedCornerRadius)
    } else {
        border(width, color, shape)
    }

@Composable
private fun Modifier.appLiquidOptimizedBorder(
    shape: Shape,
    optimizedCornerRadius: Dp?,
    color: () -> Color,
    width: Dp = 1.dp,
): Modifier =
    if (optimizedCornerRadius != null) {
        drawAppSquircleBorder(width = width, cornerRadius = optimizedCornerRadius, color = color)
    } else {
        appLiquidOptimizedBorder(
            shape = shape,
            optimizedCornerRadius = optimizedCornerRadius,
            color = color(),
            width = width,
        )
    }

@Composable
fun LiquidRoundedCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    exportBackdropToContent: Boolean = false,
    blurRadius: Dp = UiPerformanceBudget.backdropBlur,
    lensRadius: Dp = UiPerformanceBudget.backdropLens,
    effectVariant: GlassVariant? = GlassVariant.Content,
    chromaticAberration: Boolean = false,
    depthEffect: Boolean = true,
    shadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val activeBackdrop = activeGlassBackdrop(backdrop)
    val exportedContentBackdrop =
        if (exportBackdropToContent && activeBackdrop != null) {
            rememberLayerBackdrop()
        } else {
            null
        }
    LiquidSurface(
        backdrop = activeBackdrop,
        modifier = modifier,
        shape = RoundedRectangle(cornerRadius),
        tint = tint,
        surfaceColor = surfaceColor,
        blurRadius = blurRadius,
        lensRadius = lensRadius,
        effectVariant = effectVariant,
        chromaticAberration = chromaticAberration,
        depthEffect = depthEffect,
        shadow = shadow,
        exportedBackdrop = exportedContentBackdrop,
    ) {
        if (exportedContentBackdrop != null) {
            CompositionLocalProvider(
                LocalLiquidParentBackdrop provides exportedContentBackdrop,
                LocalLiquidParentBackdropOverridesFallback provides true,
            ) {
                Box(
                    modifier = Modifier.padding(contentPadding),
                    content = content,
                )
            }
        } else {
            Box(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    }
}
