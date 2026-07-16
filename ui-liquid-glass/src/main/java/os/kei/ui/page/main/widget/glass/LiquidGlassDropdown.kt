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
        blurRadius = 4.dp,
        lensStart = 16.dp,
        lensEnd = 28.dp,
        shadowElevation = 14.dp,
        innerShadowRadius = 8.dp,
        vibrancy = true,
        chromaticAberration = false,
        depthEffect = false,
        lightHighlightAlpha = 0.64f,
        darkHighlightAlpha = 0.46f,
        lightOuterShadowAlpha = 0.09f,
        darkOuterShadowAlpha = 0.18f,
        lightSpotShadowAlpha = 0.07f,
        darkSpotShadowAlpha = 0.14f,
        lightShadowAlpha = 0.10f,
        darkShadowAlpha = 0.18f,
        lightInnerShadowAlpha = 0.08f,
        darkInnerShadowAlpha = 0.14f,
    )

private val LiquidGlassDropdownMetricsActionMenu =
    LiquidGlassDropdownMetrics(
        containerRadius = 30.dp,
        contentPadding = 5.dp,
        blurRadius = 6.dp,
        lensStart = 16.dp,
        lensEnd = 32.dp,
        shadowElevation = 18.dp,
        innerShadowRadius = 10.dp,
        vibrancy = true,
        chromaticAberration = false,
        depthEffect = false,
        lightHighlightAlpha = 0.62f,
        darkHighlightAlpha = 0.44f,
        lightOuterShadowAlpha = 0.10f,
        darkOuterShadowAlpha = 0.18f,
        lightSpotShadowAlpha = 0.08f,
        darkSpotShadowAlpha = 0.14f,
        lightShadowAlpha = 0.10f,
        darkShadowAlpha = 0.18f,
        lightInnerShadowAlpha = 0.08f,
        darkInnerShadowAlpha = 0.14f,
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
                                drawRect(colors.topSheen)
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
    val topSheen: Color,
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
                    surfaceColor = surfaceContainer.copy(alpha = 0.38f),
                    topSheen = Color.White.copy(alpha = 0.05f),
                    borderColor = Color.White.copy(alpha = 0.14f),
                    fallbackBaseColor = Color(0xFF101113).copy(alpha = 0.76f),
                    fallbackMiddleBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.06f),
                                    Color.White.copy(alpha = 0.03f),
                                ),
                            start = Offset.Zero,
                            end = Offset(360f, 460f),
                        ),
                    fallbackSheenBrush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent,
                                ),
                            center = Offset(112f, 20f),
                            radius = 280f,
                        ),
                )
            } else {
                LiquidGlassDropdownContainerColors(
                    surfaceColor = Color.White.copy(alpha = 0.52f),
                    topSheen = Color.White.copy(alpha = 0.08f),
                    borderColor = Color.White.copy(alpha = 0.58f),
                    fallbackBaseColor = Color.White.copy(alpha = 0.68f),
                    fallbackMiddleBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.34f),
                                    Color(0xFFF4F8FF).copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.10f),
                                ),
                            start = Offset.Zero,
                            end = Offset(360f, 460f),
                        ),
                    fallbackSheenBrush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.28f),
                                    Color(0xFFEAF4FF).copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                            center = Offset(112f, 20f),
                            radius = 300f,
                        ),
                )
            }
        }

        LiquidGlassDropdownMaterial.Default -> {
            if (isDark) {
                LiquidGlassDropdownContainerColors(
                    surfaceColor = surfaceContainer.copy(alpha = 0.34f),
                    topSheen = Color.White.copy(alpha = 0.05f),
                    borderColor = Color.White.copy(alpha = 0.20f),
                    fallbackBaseColor = surfaceContainer.copy(alpha = 0.78f),
                    fallbackMiddleBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.05f),
                                ),
                            start = Offset.Zero,
                            end = Offset(320f, 420f),
                        ),
                    fallbackSheenBrush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.04f),
                                    Color.Transparent,
                                ),
                            center = Offset(96f, 24f),
                            radius = 260f,
                        ),
                )
            } else {
                LiquidGlassDropdownContainerColors(
                    surfaceColor = Color.White.copy(alpha = 0.46f),
                    topSheen = Color.White.copy(alpha = 0.08f),
                    borderColor = Color.White.copy(alpha = 0.54f),
                    fallbackBaseColor = Color.White.copy(alpha = 0.72f),
                    fallbackMiddleBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.38f),
                                    Color(0xFFEAF3FF).copy(alpha = 0.24f),
                                    Color.White.copy(alpha = 0.12f),
                                ),
                            start = Offset.Zero,
                            end = Offset(320f, 420f),
                        ),
                    fallbackSheenBrush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.30f),
                                    Color(0xFFE1EFFF).copy(alpha = 0.12f),
                                    Color.Transparent,
                                ),
                            center = Offset(96f, 24f),
                            radius = 260f,
                        ),
                )
            }
        }
    }

internal fun liquidGlassDropdownSelectedSurfaceColor(
    isDark: Boolean,
    material: LiquidGlassDropdownMaterial = LiquidGlassDropdownMaterial.Default,
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
            if (isDark) {
                Color.White.copy(alpha = 0.14f)
            } else {
                Color(0xFFEFF4FB).copy(alpha = 0.34f)
            }
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
            if (isDark) {
                Color.White.copy(alpha = 0.14f)
            } else {
                Color.White.copy(alpha = 0.44f)
            }
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
