package os.kei.ui.page.main.widget.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberLiquidBottomBarPalette(
    isLiquidEffectEnabled: Boolean,
    isInLightTheme: Boolean,
    primary: Color,
    onSurface: Color,
    surfaceContainer: Color,
): LiquidBottomBarPalette =
    remember(
        isLiquidEffectEnabled,
        isInLightTheme,
        primary,
        onSurface,
        surfaceContainer,
    ) {
        if (!isLiquidEffectEnabled) {
            return@remember LiquidBottomBarPalette(
                baseFillColor = surfaceContainer,
                inactiveContentColor = onSurface,
                activeContentColor = primary,
            )
        }

        if (isInLightTheme) {
            return@remember LiquidBottomBarPalette(
                baseFillColor = surfaceContainer.copy(alpha = liquidBottomBarMaterial(isLight = true).surfaceAlpha),
                inactiveContentColor = onSurface.copy(alpha = 0.88f),
                activeContentColor = primary,
            )
        }

        return@remember LiquidBottomBarPalette(
            baseFillColor = surfaceContainer.copy(alpha = liquidBottomBarMaterial(isLight = false).surfaceAlpha),
            inactiveContentColor = onSurface.copy(alpha = 0.84f),
            activeContentColor = primary.copy(alpha = 0.98f),
        )
    }

internal fun liquidBottomBarMaterial(isLight: Boolean): LiquidBottomBarMaterial =
    if (isLight) {
        LiquidBottomBarMaterial(
            surfaceAlpha = 0.40f,
            highlightAlpha = 1f,
            lensHeight = 24.dp,
            lensAmount = 24.dp,
        )
    } else {
        LiquidBottomBarMaterial(
            surfaceAlpha = 0.18f,
            highlightAlpha = 0.48f,
            lensHeight = 16.dp,
            lensAmount = 28.dp,
        )
    }

internal fun liquidBottomBarSelectionIndicatorColor(isLight: Boolean): Color =
    if (isLight) {
        Color.Black.copy(alpha = 0.10f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }

internal data class LiquidBottomBarMaterial(
    val surfaceAlpha: Float,
    val highlightAlpha: Float,
    val lensHeight: Dp,
    val lensAmount: Dp,
)

@Stable
internal class LiquidBottomBarPalette(
    val baseFillColor: Color,
    val inactiveContentColor: Color,
    val activeContentColor: Color,
)
