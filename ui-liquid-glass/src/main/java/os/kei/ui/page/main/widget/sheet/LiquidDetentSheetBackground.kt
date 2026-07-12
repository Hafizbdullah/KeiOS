@file:Suppress("FunctionName")

// Adapted from compose-miuix-ui WindowBottomSheet / BottomSheetContentLayout.
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package os.kei.ui.page.main.widget.sheet

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private const val LIQUID_SHEET_BACKGROUND_LOW_TINT_DEPTH = 0.28f
private const val LIQUID_SHEET_BACKGROUND_BLUR_START_DEPTH = 0.30f

@Composable
internal fun LiquidDetentBackgroundDimLayer(
    dimAlpha: MutableFloatState,
    depthProgress: () -> Float,
    blurLayerHeightPx: () -> Float,
    blurLayerAlpha: () -> Float,
    blurRadius: Dp,
) {
    val baseColor = MiuixTheme.colorScheme.windowDimming
    val sceneBackdrop = LocalSceneBackdrop.current
    val isDark = isSystemInDarkTheme()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val depth = liquidSheetSmoothStep(depthProgress())
                    val dimDepth = liquidSheetBackgroundDimDepth(depth)
                    drawRect(
                        baseColor.copy(
                            alpha = baseColor.alpha * dimAlpha.floatValue * dimDepth,
                        ),
                    )
                },
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .liquidSheetHeightPx {
                        blurLayerHeightPx().roundToInt()
                    }.graphicsLayer {
                        alpha = blurLayerAlpha()
                    }.drawBackdrop(
                        backdrop = sceneBackdrop,
                        shape = { RectangleShape },
                        effects = {
                            blur(blurRadius.toPx())
                        },
                        highlight = null,
                        shadow = null,
                        innerShadow = null,
                        onDrawSurface = {
                            val depth = liquidSheetSmoothStep(depthProgress())
                            val depthTint =
                                if (isDark) {
                                    Color.Black.copy(alpha = 0.065f * depth)
                                } else {
                                    Color.White.copy(alpha = 0.038f * depth)
                                }
                            drawRect(depthTint)
                        },
                    ),
        )
    }
}

internal fun Modifier.liquidSheetHeightPx(heightPx: () -> Int): Modifier =
    layout { measurable, constraints ->
        val resolvedHeight = heightPx().coerceIn(0, constraints.maxHeight)
        val placeable =
            measurable.measure(
                constraints.copy(
                    minHeight = resolvedHeight,
                    maxHeight = resolvedHeight,
                ),
            )
        layout(placeable.width, resolvedHeight) {
            placeable.place(0, 0)
        }
    }

internal fun Modifier.liquidSheetOptionalHeightPx(heightPx: () -> Int): Modifier =
    layout { measurable, constraints ->
        val requestedHeight = heightPx()
        if (requestedHeight <= 0) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        } else {
            val resolvedHeight = requestedHeight.coerceIn(0, constraints.maxHeight)
            val placeable =
                measurable.measure(
                    constraints.copy(
                        minHeight = resolvedHeight,
                        maxHeight = resolvedHeight,
                    ),
                )
            layout(placeable.width, resolvedHeight) {
                placeable.place(0, 0)
            }
        }
    }

internal fun liquidSheetSmoothStep(value: Float): Float {
    val x = value.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

internal fun Modifier.liquidSheetWidthPx(widthPx: () -> Int): Modifier =
    layout { measurable, constraints ->
        val resolvedWidth = widthPx().coerceIn(0, constraints.maxWidth)
        val placeable =
            measurable.measure(
                constraints.copy(
                    minWidth = resolvedWidth,
                    maxWidth = resolvedWidth,
                ),
            )
        layout(resolvedWidth, placeable.height) {
            placeable.place(0, 0)
        }
    }

internal fun Modifier.liquidSheetFloatingPlacement(offsetY: () -> Float): Modifier =
    this.then(
        Modifier.offset { IntOffset(x = 0, y = offsetY().roundToInt()) },
    )

internal fun liquidSheetMaxVisibleHeightPx(
    windowHeightPx: Float,
    topInsetPx: Float,
): Float = (windowHeightPx - topInsetPx).coerceAtLeast(0f)

internal fun liquidSheetBackgroundBlurLayerHeightPx(
    sheetTopOffsetPx: Float,
    cornerRadiusPx: Float,
    windowHeightPx: Float,
): Float =
    (sheetTopOffsetPx + cornerRadiusPx.coerceAtLeast(0f))
        .coerceIn(0f, windowHeightPx.coerceAtLeast(0f))

internal fun liquidSheetVisibleHeightFraction(
    visibleHeightPx: Float,
    maxVisibleHeightPx: Float,
): Float =
    if (visibleHeightPx > 0f && maxVisibleHeightPx > 0f) {
        (visibleHeightPx / maxVisibleHeightPx).coerceIn(0f, 1f)
    } else {
        0f
    }

internal fun liquidSheetBackgroundDimDepth(depth: Float): Float =
    lerp(
        start = LIQUID_SHEET_BACKGROUND_LOW_TINT_DEPTH,
        stop = 1f,
        fraction = depth.coerceIn(0f, 1f),
    )

internal fun liquidSheetBackgroundBlurLayerAlpha(depth: Float): Float {
    val linear =
        (
            (depth - LIQUID_SHEET_BACKGROUND_BLUR_START_DEPTH) /
                (1f - LIQUID_SHEET_BACKGROUND_BLUR_START_DEPTH)
        ).coerceIn(0f, 1f)
    return liquidSheetSmoothStep(linear)
}

internal fun Modifier.pointerInputDismissLayer(
    allowDismiss: Boolean,
    onDismissRequest: () -> Unit,
    onBlockedDismissRequest: (() -> Unit)?,
): Modifier =
    pointerInput(allowDismiss, onDismissRequest, onBlockedDismissRequest) {
        detectTapGestures(
            onTap = {
                if (allowDismiss) {
                    onDismissRequest()
                } else {
                    onBlockedDismissRequest?.invoke()
                }
            },
        )
    }
