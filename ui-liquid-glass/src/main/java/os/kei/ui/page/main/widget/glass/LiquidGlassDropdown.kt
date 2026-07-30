@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import os.kei.ui.page.main.widget.shape.appSquircleBorder
import os.kei.ui.page.main.widget.shape.appSquircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LiquidGlassDropdownContainerRadius = 26.dp
internal val LiquidGlassDropdownItemRadius = 18.dp
private val LiquidGlassDropdownMinWidth = 168.dp
private val LiquidGlassDropdownMaxWidth = 280.dp
private val LiquidGlassDropdownMaxHeight = 336.dp
private val LiquidGlassDropdownContentPadding = 8.dp
internal val LiquidGlassDropdownItemPressSafePadding =
    AppInteractiveTokens.compactLiquidPressSafePadding
internal val LiquidGlassDropdownRowMinHeight = 48.dp
internal val LiquidGlassDropdownIconSize = 18.dp
internal val LiquidGlassDropdownCheckSize = 18.dp
internal val LocalLiquidGlassDropdownSizingPass = staticCompositionLocalOf { false }
internal val LocalLiquidGlassDropdownBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal val LocalLiquidGlassDropdownMaterial =
    staticCompositionLocalOf { LiquidGlassDropdownMaterial.Default }

enum class LiquidGlassDropdownMaterial {
    Default,
    ActionMenu,
}

internal data class LiquidGlassDropdownMetrics(
    val containerRadius: Dp,
    val contentPadding: Dp,
    val blurRadius: Dp,
    val lensStart: Dp,
    val lensEnd: Dp,
    val shadowElevation: Dp,
    val innerShadowRadius: Dp,
    val vibrancy: Boolean,
    val chromaticAberration: Boolean,
    val depthEffect: Boolean,
    val lightHighlightAlpha: Float,
    val darkHighlightAlpha: Float,
    val lightOuterShadowAlpha: Float,
    val darkOuterShadowAlpha: Float,
    val lightSpotShadowAlpha: Float,
    val darkSpotShadowAlpha: Float,
    val lightShadowAlpha: Float,
    val darkShadowAlpha: Float,
    val lightInnerShadowAlpha: Float,
    val darkInnerShadowAlpha: Float,
)

internal fun liquidGlassDropdownMetrics(material: LiquidGlassDropdownMaterial): LiquidGlassDropdownMetrics =
    when (material) {
        LiquidGlassDropdownMaterial.Default -> LiquidGlassDropdownMetricsDefault
        LiquidGlassDropdownMaterial.ActionMenu -> LiquidGlassDropdownMetricsActionMenu
    }

private fun liquidGlassDropdownVariant(material: LiquidGlassDropdownMaterial): GlassVariant =
    when (material) {
        LiquidGlassDropdownMaterial.Default -> GlassVariant.Floating
        LiquidGlassDropdownMaterial.ActionMenu -> GlassVariant.SheetAction
    }

// Hoisted to top-level constants: the metrics are pure values (no composition state),
// so allocating a fresh data class on every recomposition was pure overhead. Reusing
// these instances also keeps structural equality intact for any downstream skip checks.
private val LiquidGlassDropdownMetricsDefault =
    LiquidGlassDropdownMetrics(
        containerRadius = LiquidGlassDropdownContainerRadius,
        contentPadding = LiquidGlassDropdownContentPadding,
        blurRadius = 12.dp,
        lensStart = 28.dp,
        lensEnd = 44.dp,
        shadowElevation = 24.dp,
        innerShadowRadius = 16.dp,
        vibrancy = true,
        chromaticAberration = false,
        depthEffect = true,
        lightHighlightAlpha = 0.66f,
        darkHighlightAlpha = 0.48f,
        lightOuterShadowAlpha = 0.18f,
        darkOuterShadowAlpha = 0.30f,
        lightSpotShadowAlpha = 0.16f,
        darkSpotShadowAlpha = 0.26f,
        lightShadowAlpha = 0.20f,
        darkShadowAlpha = 0.30f,
        lightInnerShadowAlpha = 0.10f,
        darkInnerShadowAlpha = 0.16f,
    )

private val LiquidGlassDropdownMetricsActionMenu =
    LiquidGlassDropdownMetrics(
        containerRadius = 30.dp,
        contentPadding = 5.dp,
        blurRadius = 16.dp,
        lensStart = 32.dp,
        lensEnd = 54.dp,
        shadowElevation = 30.dp,
        innerShadowRadius = 18.dp,
        vibrancy = true,
        chromaticAberration = false,
        depthEffect = true,
        lightHighlightAlpha = 0.70f,
        darkHighlightAlpha = 0.52f,
        lightOuterShadowAlpha = 0.22f,
        darkOuterShadowAlpha = 0.36f,
        lightSpotShadowAlpha = 0.20f,
        darkSpotShadowAlpha = 0.32f,
        lightShadowAlpha = 0.24f,
        darkShadowAlpha = 0.36f,
        lightInnerShadowAlpha = 0.12f,
        darkInnerShadowAlpha = 0.18f,
    )

@Composable
fun LiquidGlassDropdownColumn(
    modifier: Modifier = Modifier,
    minWidth: Dp = LiquidGlassDropdownMinWidth,
    maxWidth: Dp = LiquidGlassDropdownMaxWidth,
    maxHeight: Dp = LiquidGlassDropdownMaxHeight,
    initialScrollItemIndex: Int? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    backdrop: Backdrop? = null,
    material: LiquidGlassDropdownMaterial = LiquidGlassDropdownMaterial.Default,
    content: @Composable () -> Unit,
) {
    val isDark = isAppInDarkTheme()
    val metrics = liquidGlassDropdownMetrics(material)
    val effectVariant = liquidGlassDropdownVariant(material)
    val effectBlurRadius = resolvedGlassBlurDp(metrics.blurRadius, effectVariant)
    val effectLensStart = resolvedGlassLensDp(metrics.lensStart, effectVariant)
    val effectLensEnd = resolvedGlassLensDp(metrics.lensEnd, effectVariant)
    val containerShape = RoundedRectangle(metrics.containerRadius)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val colors =
        liquidGlassDropdownContainerColors(
            isDark = isDark,
            accentColor = accentColor,
            material = material,
        )
    val activeBackdrop = activeGlassBackdrop(backdrop)
    LaunchedEffect(initialScrollItemIndex, scrollState.maxValue, density) {
        val itemIndex = initialScrollItemIndex?.coerceAtLeast(0) ?: return@LaunchedEffect
        if (scrollState.maxValue <= 0) return@LaunchedEffect
        val targetOffset =
            with(density) {
                val rowOffset = LiquidGlassDropdownRowMinHeight.toPx() * itemIndex
                val contextInset = maxHeight.toPx() * 0.34f
                (rowOffset - contextInset).toInt()
            }.coerceIn(0, scrollState.maxValue)
        scrollState.scrollTo(targetOffset)
    }

    Box(
        modifier =
            modifier
                .widthIn(min = minWidth, max = maxWidth)
                .shadow(
                    elevation = metrics.shadowElevation,
                    shape = containerShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = if (isDark) metrics.darkOuterShadowAlpha else metrics.lightOuterShadowAlpha),
                    spotColor = Color.Black.copy(alpha = if (isDark) metrics.darkSpotShadowAlpha else metrics.lightSpotShadowAlpha),
                ).appSquircleClip(metrics.containerRadius)
                .appSquircleBorder(
                    width = 1.dp,
                    color = colors.borderColor,
                    cornerRadius = metrics.containerRadius,
                ).then(
                    if (activeBackdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = activeBackdrop,
                            shape = { containerShape },
                            effects = {
                                if (metrics.vibrancy) {
                                    vibrancy()
                                }
                                blur(effectBlurRadius.toPx())
                                safeLiquidLens(
                                    effectLensStart.toPx(),
                                    effectLensEnd.toPx(),
                                    chromaticAberration = metrics.chromaticAberration,
                                    depthEffect = metrics.depthEffect,
                                )
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = if (isDark) metrics.darkHighlightAlpha else metrics.lightHighlightAlpha)
                            },
                            shadow = {
                                Shadow.Default.copy(
                                    color = Color.Black.copy(alpha = if (isDark) metrics.darkShadowAlpha else metrics.lightShadowAlpha),
                                )
                            },
                            innerShadow = {
                                InnerShadow(
                                    radius = metrics.innerShadowRadius,
                                    alpha = if (isDark) metrics.darkInnerShadowAlpha else metrics.lightInnerShadowAlpha,
                                )
                            },
                            onDrawSurface = {
                                drawRect(colors.surfaceColor)
                                drawRect(colors.surfaceGradientBrush)
                                drawRect(colors.surfaceCausticBrush)
                            },
                        )
                    } else {
                        Modifier
                            .appSquircleBackground(colors.fallbackBaseColor, metrics.containerRadius)
                            .background(colors.fallbackMiddleBrush)
                            .background(colors.fallbackSheenBrush)
                    },
                ),
    ) {
        CompositionLocalProvider(
            LocalLiquidGlassDropdownBackdrop provides activeBackdrop,
            LocalLiquidGlassDropdownMaterial provides material,
        ) {
            SubcomposeLayout(
                modifier =
                    Modifier
                        .padding(metrics.contentPadding)
                        .heightIn(max = maxHeight)
                        .verticalScroll(scrollState),
            ) { constraints ->
                val minWidthPx = minWidth.roundToPx()
                val maxWidthPx = maxWidth.roundToPx().coerceAtLeast(minWidthPx)
                val contentInsetPx = metrics.contentPadding.roundToPx() * 2
                val minContentWidth = (minWidthPx - contentInsetPx).coerceAtLeast(0)
                val maxContentWidth = (maxWidthPx - contentInsetPx).coerceAtLeast(minContentWidth)
                val probeConstraints =
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = maxContentWidth,
                        minHeight = 0,
                    )
                val probePlaceables =
                    subcompose("probe") {
                        CompositionLocalProvider(
                            LocalLiquidGlassDropdownSizingPass provides true,
                        ) {
                            Box(
                                modifier = Modifier.clearAndSetSemantics {},
                                content = { content() },
                            )
                        }
                    }.map { measurable ->
                        measurable.measure(probeConstraints)
                    }
                val resolvedWidth =
                    probePlaceables
                        .maxOfOrNull { it.width }
                        ?.coerceIn(minContentWidth, maxContentWidth)
                        ?: minContentWidth
                val contentConstraints =
                    constraints.copy(
                        minWidth = resolvedWidth,
                        maxWidth = resolvedWidth,
                        minHeight = 0,
                    )
                val placeables =
                    subcompose("content", content).map { measurable ->
                        measurable.measure(contentConstraints)
                    }
                val contentHeight = placeables.sumOf { it.height }
                layout(resolvedWidth, contentHeight) {
                    var currentY = 0
                    placeables.forEach { placeable ->
                        placeable.placeRelative(0, currentY)
                        currentY += placeable.height
                    }
                }
            }
        }
    }
}

@Composable
fun AppStandaloneLiquidGlassDropdownColumn(
    modifier: Modifier = Modifier,
    minWidth: Dp = LiquidGlassDropdownMinWidth,
    maxWidth: Dp = LiquidGlassDropdownMaxWidth,
    maxHeight: Dp = LiquidGlassDropdownMaxHeight,
    initialScrollItemIndex: Int? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    material: LiquidGlassDropdownMaterial = LiquidGlassDropdownMaterial.Default,
    content: @Composable () -> Unit,
) {
    AppStandaloneBackdropHost(
        modifier = modifier,
    ) { activeBackdrop ->
        LiquidGlassDropdownColumn(
            minWidth = minWidth,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            initialScrollItemIndex = initialScrollItemIndex,
            accentColor = accentColor,
            backdrop = activeBackdrop,
            material = material,
            content = content,
        )
    }
}

@Composable
fun AppLiquidGlassDropdownColumn(
    modifier: Modifier = Modifier,
    minWidth: Dp = LiquidGlassDropdownMinWidth,
    maxWidth: Dp = LiquidGlassDropdownMaxWidth,
    maxHeight: Dp = LiquidGlassDropdownMaxHeight,
    initialScrollItemIndex: Int? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    backdrop: Backdrop? = null,
    material: LiquidGlassDropdownMaterial = LiquidGlassDropdownMaterial.Default,
    content: @Composable () -> Unit,
) {
    if (backdrop != null) {
        LiquidGlassDropdownColumn(
            modifier = modifier,
            minWidth = minWidth,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            initialScrollItemIndex = initialScrollItemIndex,
            accentColor = accentColor,
            backdrop = backdrop,
            material = material,
            content = content,
        )
    } else {
        AppStandaloneLiquidGlassDropdownColumn(
            modifier = modifier,
            minWidth = minWidth,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            initialScrollItemIndex = initialScrollItemIndex,
            accentColor = accentColor,
            material = material,
            content = content,
        )
    }
}

internal data class LiquidGlassDropdownContainerColors(
    val surfaceColor: Color,
    val surfaceGradientBrush: Brush,
    val surfaceCausticBrush: Brush,
    val borderColor: Color,
    val fallbackBaseColor: Color,
    val fallbackMiddleBrush: Brush,
    val fallbackSheenBrush: Brush,
)

@Composable
private fun liquidGlassDropdownContainerColors(
    isDark: Boolean,
    accentColor: Color,
    material: LiquidGlassDropdownMaterial,
): LiquidGlassDropdownContainerColors {
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    // Memoize the entire container-colors bundle. Without `remember`, every recomposition
    // allocates 2 fresh Brush instances (linearGradient + radialGradient) plus the data
    // class wrapper — and Brush instances are non-trivial (each holds a colors list and
    // an offset list internally). Inputs cover everything the result depends on.
    return remember(isDark, accentColor, material, surfaceContainer) {
        buildLiquidGlassDropdownContainerColors(
            isDark = isDark,
            accentColor = accentColor,
            material = material,
            surfaceContainer = surfaceContainer,
        )
    }
}

internal fun buildLiquidGlassDropdownContainerColors(
    isDark: Boolean,
    accentColor: Color,
    material: LiquidGlassDropdownMaterial,
    surfaceContainer: Color,
): LiquidGlassDropdownContainerColors =
    when (material) {
        LiquidGlassDropdownMaterial.ActionMenu -> {
            if (isDark) {
                LiquidGlassDropdownContainerColors(
                    surfaceColor = Color(0xFF1D1E22).copy(alpha = 0.56f),
                    surfaceGradientBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.07f),
                                    accentColor.copy(alpha = 0.04f),
                                    Color.Black.copy(alpha = 0.03f),
                                ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    surfaceCausticBrush =
                        Brush.linearGradient(
                            0.00f to Color.Transparent,
                            0.18f to Color.White.copy(alpha = 0.18f),
                            0.36f to Color.Transparent,
                            0.72f to accentColor.copy(alpha = 0.05f),
                            1.00f to Color.Transparent,
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f),
                        ),
                    borderColor = Color.White.copy(alpha = 0.20f),
                    fallbackBaseColor = Color(0xFF14161A).copy(alpha = 0.94f),
                    fallbackMiddleBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    accentColor.copy(alpha = 0.06f),
                                    Color.White.copy(alpha = 0.04f),
                                ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    fallbackSheenBrush =
                        Brush.linearGradient(
                            0.00f to Color.Transparent,
                            0.20f to Color.White.copy(alpha = 0.12f),
                            0.40f to Color.Transparent,
                            1.00f to Color.Transparent,
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f),
                        ),
                )
            } else {
                LiquidGlassDropdownContainerColors(
                    surfaceColor = Color.White.copy(alpha = 0.64f),
                    surfaceGradientBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    accentColor.copy(alpha = 0.04f),
                                    Color(0xFFEEF6FF).copy(alpha = 0.03f),
                                ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    surfaceCausticBrush =
                        Brush.linearGradient(
                            0.00f to Color.Transparent,
                            0.18f to Color.White.copy(alpha = 0.26f),
                            0.36f to Color.Transparent,
                            0.72f to accentColor.copy(alpha = 0.05f),
                            1.00f to Color.Transparent,
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f),
                        ),
                    borderColor = Color.Black.copy(alpha = 0.13f),
                    fallbackBaseColor = Color(0xFFF8FAFD).copy(alpha = 0.96f),
                    fallbackMiddleBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.38f),
                                    accentColor.copy(alpha = 0.05f),
                                    Color.White.copy(alpha = 0.12f),
                                ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    fallbackSheenBrush =
                        Brush.linearGradient(
                            0.00f to Color.Transparent,
                            0.20f to Color.White.copy(alpha = 0.30f),
                            0.40f to Color.Transparent,
                            0.74f to accentColor.copy(alpha = 0.06f),
                            1.00f to Color.Transparent,
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f),
                        ),
                )
            }
        }

        LiquidGlassDropdownMaterial.Default -> {
            if (isDark) {
                LiquidGlassDropdownContainerColors(
                    surfaceColor = Color(0xFF1D1E22).copy(alpha = 0.52f),
                    surfaceGradientBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.07f),
                                    accentColor.copy(alpha = 0.04f),
                                    Color.Black.copy(alpha = 0.03f),
                                ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    surfaceCausticBrush =
                        Brush.linearGradient(
                            0.00f to Color.Transparent,
                            0.20f to Color.White.copy(alpha = 0.17f),
                            0.38f to Color.Transparent,
                            0.74f to accentColor.copy(alpha = 0.04f),
                            1.00f to Color.Transparent,
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f),
                        ),
                    borderColor = Color.White.copy(alpha = 0.19f),
                    fallbackBaseColor = surfaceContainer.copy(alpha = 0.92f),
                    fallbackMiddleBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.13f),
                                    accentColor.copy(alpha = 0.05f),
                                    Color.White.copy(alpha = 0.04f),
                                ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    fallbackSheenBrush =
                        Brush.linearGradient(
                            0.00f to Color.Transparent,
                            0.20f to Color.White.copy(alpha = 0.12f),
                            0.40f to Color.Transparent,
                            1.00f to Color.Transparent,
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f),
                        ),
                )
            } else {
                LiquidGlassDropdownContainerColors(
                    surfaceColor = Color.White.copy(alpha = 0.58f),
                    surfaceGradientBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    accentColor.copy(alpha = 0.04f),
                                    Color(0xFFF0F7FF).copy(alpha = 0.03f),
                                ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    surfaceCausticBrush =
                        Brush.linearGradient(
                            0.00f to Color.Transparent,
                            0.20f to Color.White.copy(alpha = 0.24f),
                            0.38f to Color.Transparent,
                            0.74f to accentColor.copy(alpha = 0.05f),
                            1.00f to Color.Transparent,
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f),
                        ),
                    borderColor = Color.Black.copy(alpha = 0.12f),
                    fallbackBaseColor = Color(0xFFFAFBFD).copy(alpha = 0.94f),
                    fallbackMiddleBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.34f),
                                    accentColor.copy(alpha = 0.05f),
                                    Color.White.copy(alpha = 0.10f),
                                ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    fallbackSheenBrush =
                        Brush.linearGradient(
                            0.00f to Color.Transparent,
                            0.20f to Color.White.copy(alpha = 0.28f),
                            0.40f to Color.Transparent,
                            0.74f to accentColor.copy(alpha = 0.06f),
                            1.00f to Color.Transparent,
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f),
                        ),
                )
            }
        }
    }

internal fun liquidGlassDropdownSelectedSurfaceColor(
    isDark: Boolean,
    material: LiquidGlassDropdownMaterial = LiquidGlassDropdownMaterial.Default,
    accentColor: Color = Color(0xFF3B82F6),
): Color =
    when (material) {
        LiquidGlassDropdownMaterial.ActionMenu -> {
            if (isDark) {
                Color.White.copy(alpha = 0.08f)
            } else {
                Color.White.copy(alpha = 0.26f)
            }
        }

        LiquidGlassDropdownMaterial.Default -> {
            accentColor.copy(alpha = if (isDark) 0.20f else 0.13f)
        }
    }

internal fun liquidGlassDropdownPressedSurfaceColor(
    isDark: Boolean,
    material: LiquidGlassDropdownMaterial = LiquidGlassDropdownMaterial.Default,
): Color =
    when (material) {
        LiquidGlassDropdownMaterial.ActionMenu -> {
            if (isDark) {
                Color.White.copy(alpha = 0.07f)
            } else {
                Color.White.copy(alpha = 0.22f)
            }
        }

        LiquidGlassDropdownMaterial.Default -> {
            if (isDark) {
                Color.White.copy(alpha = 0.10f)
            } else {
                Color.White.copy(alpha = 0.28f)
            }
        }
    }

internal fun liquidGlassDropdownSelectedBorderColor(
    isDark: Boolean,
    material: LiquidGlassDropdownMaterial = LiquidGlassDropdownMaterial.Default,
    accentColor: Color = Color(0xFF3B82F6),
): Color =
    when (material) {
        LiquidGlassDropdownMaterial.ActionMenu -> {
            if (isDark) {
                Color.White.copy(alpha = 0.08f)
            } else {
                Color.White.copy(alpha = 0.40f)
            }
        }

        LiquidGlassDropdownMaterial.Default -> {
            accentColor.copy(alpha = if (isDark) 0.34f else 0.24f)
        }
    }

fun liquidGlassDropdownItemAccent(
    isDark: Boolean,
    accentColor: Color,
    variant: GlassVariant,
): Color =
    when (variant) {
        GlassVariant.SheetDangerAction -> {
            Color(0xFFE25B6A)
        }

        else -> {
            if (accentColor == Color.Unspecified) {
                if (isDark) Color(0xFF71ADFF) else Color(0xFF3B82F6)
            } else {
                accentColor
            }
        }
    }
