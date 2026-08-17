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

/**
 * The panel's geometry. Everything else it used to carry — blur, lens start and end, four shadow alphas
 * per theme, highlight and inner-shadow alphas, vibrancy, depth and chromatic-aberration flags — was
 * dead configuration: the menu rendered in a Popup window, so `activeGlassBackdrop` always resolved to
 * null and the panel always took its opaque fallback. None of those twenty values ever reached a shader.
 *
 * The material now comes from [rememberLiquidMenuSurface], which shares
 * [presentationGlassBlur]/[presentationGlassLens] with the sheet, alert and toast.
 */
internal data class LiquidGlassDropdownMetrics(
    val containerRadius: Dp,
    val contentPadding: Dp,
)

internal fun liquidGlassDropdownMetrics(material: LiquidGlassDropdownMaterial): LiquidGlassDropdownMetrics =
    when (material) {
        LiquidGlassDropdownMaterial.Default -> LiquidGlassDropdownMetricsDefault
        LiquidGlassDropdownMaterial.ActionMenu -> LiquidGlassDropdownMetricsActionMenu
    }

// Hoisted to top-level constants: the metrics are pure values (no composition state), so allocating a
// fresh data class on every recomposition was pure overhead.
private val LiquidGlassDropdownMetricsDefault =
    LiquidGlassDropdownMetrics(
        containerRadius = LiquidGlassDropdownContainerRadius,
        contentPadding = LiquidGlassDropdownContentPadding,
    )

private val LiquidGlassDropdownMetricsActionMenu =
    LiquidGlassDropdownMetrics(
        containerRadius = 30.dp,
        contentPadding = 5.dp,
    )

/**
 * The lift a row takes under the finger. Lives beside the metrics because it is the same kind of thing:
 * pure per-material values with no composition state.
 *
 * Was four nested `if (material == ActionMenu) … if (isDark) …` ternaries inline in the row's
 * `graphicsLayer`, which is where a shadow triple is least readable and most easily half-edited. The
 * action menu sits on a smaller, tighter panel, so it lifts less.
 */
internal data class LiquidGlassDropdownPressShadow(
    val elevation: Dp,
    val ambientAlpha: Float,
    val spotAlpha: Float,
)

internal fun liquidGlassDropdownPressShadow(
    material: LiquidGlassDropdownMaterial,
    isDark: Boolean,
): LiquidGlassDropdownPressShadow =
    when (material) {
        LiquidGlassDropdownMaterial.ActionMenu ->
            if (isDark) {
                LiquidGlassDropdownPressShadowActionMenuDark
            } else {
                LiquidGlassDropdownPressShadowActionMenuLight
            }

        LiquidGlassDropdownMaterial.Default ->
            if (isDark) LiquidGlassDropdownPressShadowDefaultDark else LiquidGlassDropdownPressShadowDefaultLight
    }

private val LiquidGlassDropdownPressShadowActionMenuDark =
    LiquidGlassDropdownPressShadow(elevation = 6.dp, ambientAlpha = 0.10f, spotAlpha = 0.08f)

private val LiquidGlassDropdownPressShadowActionMenuLight =
    LiquidGlassDropdownPressShadow(elevation = 6.dp, ambientAlpha = 0.05f, spotAlpha = 0.04f)

private val LiquidGlassDropdownPressShadowDefaultDark =
    LiquidGlassDropdownPressShadow(elevation = 10.dp, ambientAlpha = 0.18f, spotAlpha = 0.16f)

private val LiquidGlassDropdownPressShadowDefaultLight =
    LiquidGlassDropdownPressShadow(elevation = 10.dp, ambientAlpha = 0.10f, spotAlpha = 0.08f)

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
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val activeBackdrop = activeGlassBackdrop(backdrop)
    val surface =
        rememberLiquidMenuSurface(
            cornerRadius = metrics.containerRadius,
            isDark = isDark,
            explicitBackdrop = activeBackdrop,
            material = material,
        )
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
                // The surface owns the reveal, so it has to come before the clip: `drawBackdrop` applies
                // its `layerBlock` as this element's own graphics layer, and a clip installed above that
                // layer would crop the panel at its un-scaled bounds while it grows.
                .then(surface.modifier)
                .appSquircleClip(metrics.containerRadius),
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
