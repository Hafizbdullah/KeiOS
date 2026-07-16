@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.core.AppStatusPrimitives
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidSurface
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.activeGlassBackdrop
import os.kei.ui.page.main.widget.glass.resolvedGlassBlurDp
import os.kei.ui.page.main.widget.glass.resolvedGlassLensDp
import os.kei.ui.page.main.widget.isAppInDarkTheme
import top.yukonga.miuix.kmp.basic.Icon

/** Defaults for the compact passive status-icon atom. */
object StatusIconPillDefaults {
    val Width = 28.dp
    val Height = 22.dp
    val IconSize = 13.dp
}

/**
 * Compact icon-only status treatment.
 *
 * [label] is exposed as the single passive semantic description. This atom intentionally owns no
 * click, disabled, or button semantics. It consumes [backdrop], then the nearest
 * [LocalLiquidParentBackdrop], and falls back to the same light optical stack as [StatusPill].
 */
@Composable
fun StatusIconPill(
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    width: Dp = StatusIconPillDefaults.Width,
    height: Dp = StatusIconPillDefaults.Height,
    iconSize: Dp = StatusIconPillDefaults.IconSize,
) {
    val resolvedWidth = width.positiveOr(StatusIconPillDefaults.Width)
    val resolvedHeight = height.positiveOr(StatusIconPillDefaults.Height)
    val maximumIconSize = minOf(resolvedWidth, resolvedHeight)
    val resolvedIconSize =
        iconSize
            .positiveOr(StatusIconPillDefaults.IconSize)
            .coerceAtMost(maximumIconSize)
    val isDark = isAppInDarkTheme()
    val backgroundAlpha = statusPillBackgroundAlpha(isDark)
    val borderAlpha = statusPillBorderAlpha(isDark)
    val parentBackdrop = LocalLiquidParentBackdrop.current
    val activeBackdrop = activeGlassBackdrop(backdrop ?: parentBackdrop)
    val fallbackOptics =
        statusPillFallbackOptics(
            isDark = isDark,
            accent = color,
            backgroundAlpha = backgroundAlpha,
            borderAlpha = borderAlpha,
        )
    val pillModifier =
        Modifier
            .size(width = resolvedWidth, height = resolvedHeight)
            .then(modifier)
            .statusPillMaterial(
                activeBackdrop = activeBackdrop,
                cornerRadius = StatusPillCornerRadius,
                color = { color },
                borderAlpha = borderAlpha,
                fallbackOptics = fallbackOptics,
            ).semantics(mergeDescendants = true) {
                contentDescription = label
            }
    val content: @Composable BoxScope.() -> Unit = {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = statusPillContentColor(isDark = isDark, accent = color),
            modifier = Modifier.size(resolvedIconSize),
        )
    }

    if (activeBackdrop != null) {
        LiquidSurface(
            backdrop = activeBackdrop,
            modifier = pillModifier,
            shape = AppStatusPrimitives.pillShape,
            isInteractive = false,
            surfaceColor = color.copy(alpha = backgroundAlpha),
            blurRadius =
                resolvedGlassBlurDp(
                    UiPerformanceBudget.backdropBlur,
                    GlassVariant.Compact,
                ),
            lensRadius =
                resolvedGlassLensDp(
                    UiPerformanceBudget.backdropLens,
                    GlassVariant.Compact,
                ),
            depthEffect = true,
            shadow = false,
            contentAlignment = Alignment.Center,
            content = content,
        )
    } else {
        Box(
            modifier = pillModifier,
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

private fun Dp.positiveOr(fallback: Dp): Dp =
    takeIf { candidate -> candidate.value.isFinite() && candidate.value > 0f } ?: fallback
