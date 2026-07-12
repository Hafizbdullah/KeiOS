@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorModel
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppLiquidBadgedIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    badgeLabel: String? = null,
    badgeColor: Color? = null,
    badgeContentColor: Color? = null,
) {
    val label = badgeLabel?.takeIf { it.isNotBlank() }
    if (label == null) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        return
    }

    Box(
        modifier =
            Modifier
                .defaultMinSize(
                    minWidth = AppLiquidBadgedIconAnchorSize,
                    minHeight = AppLiquidBadgedIconAnchorSize,
                ).semantics { stateDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        AppLiquidIconBadge(
            label = label,
            color = badgeColor,
            contentColor = badgeContentColor,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .clearAndSetSemantics {},
        )
    }
}

@Composable
internal fun AppLiquidIconBadge(
    label: String?,
    modifier: Modifier = Modifier,
    color: Color? = null,
    contentColor: Color? = null,
) {
    val resolvedLabel = label?.takeIf { it.isNotBlank() } ?: return
    val colors =
        resolveLiquidBadgeColors(
            containerColor = color,
            contentColor = contentColor,
            defaultContainerColor = MiuixTheme.colorScheme.error,
            defaultContentColor = MiuixTheme.colorScheme.onError,
            surfaceColor = MiuixTheme.colorScheme.surfaceContainer,
        )
    Box(
        modifier =
            modifier
                .defaultMinSize(
                    minWidth = AppLiquidIconBadgeMinSize,
                    minHeight = AppLiquidIconBadgeMinSize,
                ).appSquircleBackground(
                    color = colors.containerColor,
                    cornerRadius = AppLiquidIconBadgeMinSize / 2,
                ).padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = resolvedLabel,
            color = colors.contentColor,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

internal data class LiquidBadgeColors(
    val containerColor: Color,
    val contentColor: Color,
)

internal fun resolveLiquidBadgeColors(
    containerColor: Color?,
    contentColor: Color?,
    defaultContainerColor: Color,
    defaultContentColor: Color,
    surfaceColor: Color,
): LiquidBadgeColors {
    val validDefaultContainer = defaultContainerColor.finiteRgbOrNull()
    val requestedContainer = containerColor.finiteRgbOrNull()
    val resolvedContainer = requestedContainer ?: validDefaultContainer ?: Color.Black

    contentColor.finiteRgbOrNull()?.let { explicitContent ->
        return LiquidBadgeColors(
            containerColor = resolvedContainer,
            contentColor = explicitContent,
        )
    }

    val validDefaultContent = defaultContentColor.finiteRgbOrNull()
    val usesDefaultContainer = requestedContainer == null || requestedContainer == validDefaultContainer
    if (usesDefaultContainer && validDefaultContainer != null && validDefaultContent != null) {
        return LiquidBadgeColors(
            containerColor = resolvedContainer,
            contentColor = validDefaultContent,
        )
    }

    val effectiveSurface = surfaceColor.finiteRgbOrNull()?.copy(alpha = 1f) ?: Color.White
    val effectiveContainer = resolvedContainer.compositeOver(effectiveSurface)
    val blackContrast = glassContrastRatio(Color.Black, effectiveContainer)
    val whiteContrast = glassContrastRatio(Color.White, effectiveContainer)
    return LiquidBadgeColors(
        containerColor = resolvedContainer,
        contentColor = if (blackContrast >= whiteContrast) Color.Black else Color.White,
    )
}

private fun Color?.finiteRgbOrNull(): Color? {
    val candidate = this ?: return null
    if (!candidate.isSpecified || candidate.colorSpace.model != ColorModel.Rgb) return null
    return candidate.takeIf {
        candidate.red.isFinite() &&
            candidate.green.isFinite() &&
            candidate.blue.isFinite() &&
            candidate.alpha.isFinite()
    }
}

private val AppLiquidBadgedIconAnchorSize = 34.dp
private val AppLiquidIconBadgeMinSize = 18.dp
