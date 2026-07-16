@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppCompactIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MiuixTheme.colorScheme.primary,
    visualSize: Dp = AppCompactIconActionDefaultVisualSize,
) {
    val resolvedVisualSize = resolveCompactIconActionVisualSize(visualSize)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState =
        appMotionFloatState(
            targetValue = if (enabled && isPressed) AppInteractiveTokens.pressedScale else 1f,
            durationMillis = 110,
            label = "app_compact_icon_action_scale",
        )
    val scaleProvider = remember(scaleState) { { scaleState.value } }
    Box(
        modifier =
            Modifier
                .defaultMinSize(
                    minWidth = AppCompactIconActionMinimumTouchSize,
                    minHeight = AppCompactIconActionMinimumTouchSize,
                ).then(modifier)
                .semantics {
                    this.contentDescription = contentDescription
                }.clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(resolvedVisualSize)
                    .graphicsLayer {
                        val scale = scaleProvider()
                        scaleX = scale
                        scaleY = scale
                        alpha = if (enabled) 1f else AppInteractiveTokens.disabledContentAlpha
                    },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
            )
        }
    }
}

@Composable
fun AppCompactIconIndicator(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.primary,
    visualSize: Dp = AppCompactIconActionDefaultVisualSize,
) {
    val resolvedVisualSize = resolveCompactIconActionVisualSize(visualSize)
    Box(
        modifier =
            Modifier
                .defaultMinSize(
                    minWidth = AppCompactIconActionMinimumTouchSize,
                    minHeight = AppCompactIconActionMinimumTouchSize,
                ).then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(resolvedVisualSize),
            tint = tint,
        )
    }
}

internal fun resolveCompactIconActionVisualSize(requestedSize: Dp): Dp {
    if (
        requestedSize == Dp.Unspecified ||
        !requestedSize.value.isFinite() ||
        requestedSize <= 0.dp
    ) {
        return AppCompactIconActionDefaultVisualSize
    }
    return requestedSize.coerceIn(
        minimumValue = AppCompactIconActionMinimumVisualSize,
        maximumValue = AppCompactIconActionMaximumVisualSize,
    )
}

internal val AppCompactIconActionDefaultVisualSize = 30.dp

private val AppCompactIconActionMinimumVisualSize = 30.dp
private val AppCompactIconActionMaximumVisualSize = 42.dp
private val AppCompactIconActionMinimumTouchSize = 48.dp
