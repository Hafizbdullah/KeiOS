package os.kei.ui.page.main.widget.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import os.kei.ui.animation.DampedDragAnimation

internal fun Modifier.liquidActionBarSelectionAura(
    enabled: Boolean,
    animation: DampedDragAnimation,
    tabWidthPx: Float,
    panelOffsetPx: () -> Float,
    isLtr: Boolean,
    glowColor: Color,
    coreColor: Color,
    interactionProgress: () -> Float,
): Modifier {
    if (!enabled || tabWidthPx <= 0f) return this
    return drawWithContent {
        val activeProgress = interactionProgress().fastCoerceIn(0f, 1f)
        if (activeProgress <= 0.001f) {
            drawContent()
            return@drawWithContent
        }
        val centerX =
            if (isLtr) {
                (animation.value + 0.5f) * tabWidthPx + panelOffsetPx()
            } else {
                size.width - (animation.value + 0.5f) * tabWidthPx + panelOffsetPx()
            }.fastCoerceIn(0f, size.width)
        val center = Offset(centerX, size.height / 2f)
        val pressProgress = animation.pressProgress.fastCoerceIn(0f, 1f)
        val glowAlpha = (0.04f + pressProgress * 0.16f) * activeProgress
        val coreAlpha = (0.03f + pressProgress * 0.18f) * activeProgress
        drawCircle(
            color = glowColor.copy(alpha = glowAlpha.fastCoerceIn(0f, 0.24f)),
            radius = size.height * (0.82f + pressProgress * 0.14f),
            center = center,
        )
        drawCircle(
            color = coreColor.copy(alpha = coreAlpha.fastCoerceIn(0f, 0.22f)),
            radius = size.height * (0.38f + pressProgress * 0.06f),
            center = center,
        )
        drawContent()
    }
}

@Composable
internal fun rememberLiquidActionBarPalette(
    material: LiquidActionBarMaterial,
    isBlurEnabled: Boolean,
    isInLightTheme: Boolean,
    primary: Color,
    onSurface: Color,
    surfaceContainer: Color,
): LiquidActionBarPalette =
    remember(
        material,
        isBlurEnabled,
        isInLightTheme,
        primary,
        onSurface,
        surfaceContainer,
    ) {
        if (isInLightTheme) {
            return@remember LiquidActionBarPalette(
                baseFillColor = if (isBlurEnabled) surfaceContainer.copy(alpha = material.surfaceAlpha) else surfaceContainer,
                inactiveContentColor = onSurface.copy(alpha = if (isBlurEnabled) 0.92f else 0.90f),
                activeContentColor = primary,
                selectionGlowColor = Color.White,
                selectionCoreColor = Color.White,
                outlineColor = Color.White.copy(alpha = if (isBlurEnabled) 0.18f else 0.16f),
            )
        }

        LiquidActionBarPalette(
            baseFillColor = if (isBlurEnabled) surfaceContainer.copy(alpha = material.surfaceAlpha) else surfaceContainer,
            inactiveContentColor = onSurface.copy(alpha = 0.94f),
            activeContentColor = primary.copy(alpha = 0.98f),
            selectionGlowColor = Color.White.copy(alpha = 0.26f),
            selectionCoreColor = Color.White.copy(alpha = 0.18f),
            outlineColor = Color.White.copy(alpha = if (isBlurEnabled) 0.12f else 0.10f),
        )
    }

internal fun liquidActionBarBaseHighlight(
    material: LiquidActionBarMaterial,
    isBlurEnabled: Boolean,
): Highlight =
    Highlight.Default.copy(
        alpha = if (isBlurEnabled) material.highlightAlpha else 0f,
    )

internal fun liquidActionBarBaseShadow(isInLightTheme: Boolean): Shadow =
    Shadow.Default.copy(
        color = Color.Black.copy(alpha = if (isInLightTheme) 0.10f else 0.20f),
    )

/**
 * The full-material action bar always takes the strong interaction highlight. The reduced tier these
 * used to switch to went away with the preference that selected it.
 */
internal const val LiquidActionBarInteractionHighlightStrength = 1f

internal const val LiquidActionBarInteractionHighlightRadiusScale = 1.2f

/**
 * The one material both action bars wear — the top toolbar and the floating bottom bar.
 *
 * The bottom bar used to carry its own near-twin of this (`LiquidBottomBarMaterial`, same four fields,
 * different numbers: 0.40/0.18 surface against 0.30/0.22, and a light-mode highlight of a full 1.0
 * against 0.66). Two definitions of one visual role, on the two surfaces a teacher sees at the same
 * time, and the bottom bar's predated the rebuild. Folding it in here is the whole point: a bar is a bar.
 */
internal fun liquidActionBarMaterial(isLight: Boolean): LiquidActionBarMaterial =
    if (isLight) {
        LiquidActionBarMaterial(
            blur = 4.dp,
            lensHeight = 16.dp,
            lensAmount = 32.dp,
            surfaceAlpha = 0.30f,
            highlightAlpha = 0.66f,
        )
    } else {
        LiquidActionBarMaterial(
            blur = 4.dp,
            lensHeight = 16.dp,
            lensAmount = 28.dp,
            surfaceAlpha = 0.22f,
            highlightAlpha = 0.46f,
        )
    }

/**
 * The press lens, which only the bottom bar has: its selection indicator refracts under the finger
 * while the toolbar's actions do not.
 *
 * Named here rather than left as literals at the call site, and kept out of [LiquidActionBarMaterial]
 * because they are interaction-scoped rather than part of the resting material — putting them in the
 * data class would hand the toolbar two fields it has no use for.
 */
internal val LiquidBarPressLensHeight = 10.dp

internal val LiquidBarPressLensAmount = 14.dp

/** Strength of the radial refraction at full press, paired with [LiquidBarPressLensAmount]. */
internal const val LiquidBarPressRefractionStrength = 6f

internal fun liquidChromeSelectionIndicatorColor(
    isLight: Boolean,
    accentColor: Color,
): Color =
    if (isLight) {
        lerp(Color.White, accentColor, 0.16f).copy(alpha = 0.26f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }

internal data class LiquidActionBarMaterial(
    val blur: Dp,
    val lensHeight: Dp,
    val lensAmount: Dp,
    val surfaceAlpha: Float,
    val highlightAlpha: Float,
)

@Stable
internal class LiquidActionBarPalette(
    val baseFillColor: Color,
    val inactiveContentColor: Color,
    val activeContentColor: Color,
    val selectionGlowColor: Color,
    val selectionCoreColor: Color,
    val outlineColor: Color,
)
