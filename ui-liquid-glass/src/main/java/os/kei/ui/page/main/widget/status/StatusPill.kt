@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppStatusPrimitives
import os.kei.ui.page.main.widget.core.rememberAppStatusPillMetrics
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.activeGlassBackdrop
import os.kei.ui.page.main.widget.glass.resolvedGlassBlurDp
import os.kei.ui.page.main.widget.glass.resolvedGlassLensDp
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.shape.appSquircleClip
import os.kei.ui.page.main.widget.shape.drawAppSquircleBackground
import os.kei.ui.page.main.widget.shape.drawAppSquircleBorder
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: AppStatusPillSize = AppStatusPillSize.Default,
    contentPadding: PaddingValues? = null,
    backgroundAlphaOverride: Float? = null,
    borderAlphaOverride: Float? = null,
    backdrop: Backdrop? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    StatusPill(
        label = label,
        color = { color },
        modifier = modifier,
        size = size,
        contentPadding = contentPadding,
        backgroundAlphaOverride = backgroundAlphaOverride,
        borderAlphaOverride = borderAlphaOverride,
        backdrop = backdrop,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun StatusPill(
    label: String,
    color: () -> Color,
    modifier: Modifier = Modifier,
    size: AppStatusPillSize = AppStatusPillSize.Default,
    contentPadding: PaddingValues? = null,
    backgroundAlphaOverride: Float? = null,
    borderAlphaOverride: Float? = null,
    backdrop: Backdrop? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val isDark = isAppInDarkTheme()
    val colorProvider = color
    val resolvedColor = colorProvider()
    val metrics = rememberAppStatusPillMetrics(size)
    val resolvedPadding = contentPadding ?: metrics.contentPadding
    val backgroundAlpha = backgroundAlphaOverride ?: if (isDark) 0.18f else 0.24f
    val borderAlpha = borderAlphaOverride ?: if (isDark) 0.35f else 0.42f
    val textColor = statusPillContentColor(isDark = isDark, accent = resolvedColor)
    val shape = AppStatusPrimitives.pillShape
    val cornerRadius = 999.dp
    val parentBackdrop = LocalLiquidParentBackdrop.current
    val activeBackdrop = activeGlassBackdrop(backdrop ?: parentBackdrop)
    val fallbackOptics =
        statusPillFallbackOptics(
            isDark = isDark,
            accent = resolvedColor,
            backgroundAlpha = backgroundAlpha,
            borderAlpha = borderAlpha,
        )
    val pillModifier =
        Modifier
            .then(modifier)
            .then(
                if (activeBackdrop == null) {
                    Modifier
                        .drawAppSquircleBackground(cornerRadius) {
                            fallbackOptics.baseColor
                        }.appSquircleClip(cornerRadius)
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        fallbackOptics.veilTop,
                                        fallbackOptics.veilMiddle,
                                        fallbackOptics.innerShadeBottom,
                                    ),
                            ),
                        )
                } else {
                    Modifier
                },
            ).drawAppSquircleBorder(
                width = 0.8.dp,
                cornerRadius = cornerRadius,
            ) {
                if (activeBackdrop == null) {
                    fallbackOptics.rimColor
                } else {
                    colorProvider().copy(alpha = borderAlpha)
                }
            }
    val content: @Composable () -> Unit = {
        DisableSelection {
            Text(
                text = label,
                color = textColor,
                fontSize = metrics.typography.fontSize,
                lineHeight = metrics.typography.lineHeight,
                fontWeight = metrics.typography.fontWeight,
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                overflow = overflow,
            )
        }
    }
    if (activeBackdrop != null) {
        StatusPillLiquid(
            modifier = pillModifier,
            backdrop = activeBackdrop,
            shape = shape,
            surfaceColor = resolvedColor.copy(alpha = backgroundAlpha),
            resolvedPadding = resolvedPadding,
            content = content,
        )
    } else {
        StatusPillStatic(
            modifier = pillModifier,
            resolvedPadding = resolvedPadding,
            content = content,
        )
    }
}

@Composable
private fun StatusPillStatic(
    modifier: Modifier,
    resolvedPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.padding(resolvedPadding),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun StatusPillLiquid(
    modifier: Modifier,
    backdrop: Backdrop,
    shape: Shape,
    surfaceColor: Color,
    resolvedPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    LiquidSurface(
        backdrop = backdrop,
        modifier = modifier,
        shape = shape,
        isInteractive = false,
        surfaceColor = surfaceColor,
        blurRadius = resolvedGlassBlurDp(UiPerformanceBudget.backdropBlur, GlassVariant.Compact),
        lensRadius = resolvedGlassLensDp(UiPerformanceBudget.backdropLens, GlassVariant.Compact),
        depthEffect = true,
        shadow = false,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.padding(resolvedPadding),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

internal fun statusPillContentColor(
    isDark: Boolean,
    accent: Color,
): Color = if (isDark) accent else accent.copy(alpha = 0.96f)

@Immutable
internal data class StatusPillFallbackOptics(
    val baseColor: Color,
    val veilTop: Color,
    val veilMiddle: Color,
    val innerShadeBottom: Color,
    val rimColor: Color,
)

internal fun statusPillFallbackOptics(
    isDark: Boolean,
    accent: Color,
    backgroundAlpha: Float,
    borderAlpha: Float,
): StatusPillFallbackOptics {
    val resolvedBackgroundAlpha = backgroundAlpha.coerceIn(0f, 1f)
    val resolvedBorderAlpha = borderAlpha.coerceIn(0f, 1f)
    return StatusPillFallbackOptics(
        baseColor = accent.copy(alpha = resolvedBackgroundAlpha),
        veilTop =
            Color.White.copy(
                alpha = resolvedBackgroundAlpha * if (isDark) 0.42f else 0.62f,
            ),
        veilMiddle = Color.White.copy(alpha = resolvedBackgroundAlpha * 0.12f),
        innerShadeBottom =
            Color.Black.copy(
                alpha = resolvedBackgroundAlpha * if (isDark) 0.20f else 0.14f,
            ),
        rimColor =
            lerp(
                start = accent,
                stop = Color.White,
                fraction = if (isDark) 0.42f else 0.58f,
            ).copy(alpha = resolvedBorderAlpha),
    )
}
