@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppFloatingLiquidActionButton(
    backdrop: Backdrop?,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = AppChromeTokens.floatingBottomBarOuterHeight,
    iconSize: Dp = 27.dp,
    iconTint: Color = MiuixTheme.colorScheme.primary,
    enabled: Boolean = true,
    iconModifier: Modifier = Modifier,
    tooltipText: String? = contentDescription.takeIf { it.isNotBlank() },
    badgeLabel: String? = null,
    badgeColor: Color? = null,
    badgeContentColor: Color? = null,
) {
    val metrics = resolveFloatingActionMetrics(size)
    AppLiquidIconButton(
        backdrop = backdrop,
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier =
            Modifier
                .defaultMinSize(
                    minWidth = metrics.touchSize,
                    minHeight = metrics.touchSize,
                ).then(modifier),
        width = metrics.visualSize,
        height = metrics.visualSize,
        variant = GlassVariant.Bar,
        iconTint = iconTint,
        iconModifier = iconModifier.then(Modifier.size(iconSize)),
        enabled = enabled,
        tooltipText = tooltipText,
        badgeLabel = badgeLabel,
        badgeColor = badgeColor,
        badgeContentColor = badgeContentColor,
        // The collapsed dock is glass chrome, so its badge gets the material too — otherwise the badge
        // would be the only flat, pasted-on element in the dock.
        badgeBackdrop = backdrop,
    )
}

internal data class FloatingActionMetrics(
    val visualSize: Dp,
    val touchSize: Dp,
)

internal fun resolveFloatingActionMetrics(requestedSize: Dp): FloatingActionMetrics {
    val visualSize =
        requestedSize.takeIf { size ->
            size != Dp.Unspecified && size.value.isFinite() && size > 0.dp
        } ?: AppChromeTokens.floatingBottomBarOuterHeight
    return FloatingActionMetrics(
        visualSize = visualSize,
        touchSize = maxOf(visualSize, AppFloatingActionMinimumTouchSize),
    )
}

private val AppFloatingActionMinimumTouchSize = 48.dp
