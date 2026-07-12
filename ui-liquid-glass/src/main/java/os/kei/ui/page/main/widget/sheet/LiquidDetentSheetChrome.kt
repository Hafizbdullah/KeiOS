@file:Suppress("FunctionName")

// Adapted from compose-miuix-ui WindowBottomSheet / BottomSheetContentLayout.
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package os.kei.ui.page.main.widget.sheet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val LIQUID_SHEET_SETTLE_DAMPING = 0.85f
private const val LIQUID_SHEET_SETTLE_RESPONSE = 0.40f
private const val LIQUID_SHEET_HANDLE_REST_WIDTH = 45f
private const val LIQUID_SHEET_HANDLE_PRESSED_WIDTH = 55f
private const val LIQUID_SHEET_HANDLE_PRESSED_SCALE = 1.15f
private const val LIQUID_SHEET_HANDLE_PRESS_DURATION_MS = 100
private const val LIQUID_SHEET_HANDLE_RELEASE_DURATION_MS = 150

@Composable
internal fun LiquidDetentTopChrome(
    title: String?,
    startAction: (@Composable () -> Unit)?,
    endAction: (@Composable () -> Unit)?,
    dragHandleColor: Color,
    coroutineScope: CoroutineScope,
    canExpand: Boolean,
    canCollapse: Boolean,
    canDismiss: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
    onDragStarted: () -> Unit,
    onDrag: (Float) -> Unit,
    onSettle: (velocity: Float) -> Unit,
) {
    val isPressing = remember { mutableFloatStateOf(0f) }
    val pressScale = remember { Animatable(1f) }
    val pressWidth = remember { Animatable(LIQUID_SHEET_HANDLE_REST_WIDTH) }
    val handleShape = remember { RoundedCornerShape(2.dp) }
    val density = LocalDensity.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    if (canExpand) {
                        expand {
                            onExpand()
                            true
                        }
                    }
                    if (canCollapse) {
                        collapse {
                            onCollapse()
                            true
                        }
                    }
                    if (canDismiss) {
                        dismiss {
                            onDismiss()
                            true
                        }
                    }
                }.pointerHoverIcon(PointerIcon.Hand)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressing.floatValue = 1f
                            coroutineScope.animateHandlePressDown(pressScale, pressWidth)
                            val released = tryAwaitRelease()
                            if (released) {
                                isPressing.floatValue = 0f
                                coroutineScope.animateHandlePressRelease(pressScale, pressWidth)
                            }
                        },
                    )
                }.draggable(
                    orientation = Orientation.Vertical,
                    state =
                        rememberDraggableState { dragAmount ->
                            onDrag(dragAmount)
                        },
                    onDragStarted = {
                        onDragStarted()
                        isPressing.floatValue = 1f
                        coroutineScope.animateHandlePressDown(pressScale, pressWidth)
                    },
                    onDragStopped = { velocity ->
                        isPressing.floatValue = 0f
                        coroutineScope.animateHandlePressRelease(pressScale, pressWidth)
                        onSettle(velocity)
                    },
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .liquidSheetWidthPx {
                            with(density) { pressWidth.value.dp.roundToPx() }
                        }.height(4.dp)
                        .graphicsLayer {
                            scaleY = pressScale.value
                        }.clip(handleShape)
                        .drawBehind {
                            val handleAlpha = lerp(0.2f, 0.35f, isPressing.floatValue)
                            drawRect(dragHandleColor.copy(alpha = handleAlpha))
                        },
            )
        }
        LiquidDetentTitleAndActionsRow(
            title = title,
            startAction = startAction,
            endAction = endAction,
        )
    }
}

@Composable
private fun LiquidDetentTitleAndActionsRow(
    title: String?,
    startAction: (@Composable () -> Unit)?,
    endAction: (@Composable () -> Unit)?,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 12.dp),
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            startAction?.invoke()
        }
        title?.let {
            Text(
                text = it,
                modifier = Modifier.align(Alignment.Center),
                fontSize = MiuixTheme.textStyles.title4.fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            endAction?.invoke()
        }
    }
}

private fun CoroutineScope.animateHandlePressDown(
    pressScale: Animatable<Float, *>,
    pressWidth: Animatable<Float, *>,
) {
    launch {
        pressScale.animateTo(
            targetValue = LIQUID_SHEET_HANDLE_PRESSED_SCALE,
            animationSpec = tween(durationMillis = LIQUID_SHEET_HANDLE_PRESS_DURATION_MS),
        )
    }
    launch {
        pressWidth.animateTo(
            targetValue = LIQUID_SHEET_HANDLE_PRESSED_WIDTH,
            animationSpec = tween(durationMillis = LIQUID_SHEET_HANDLE_PRESS_DURATION_MS),
        )
    }
}

private fun CoroutineScope.animateHandlePressRelease(
    pressScale: Animatable<Float, *>,
    pressWidth: Animatable<Float, *>,
) {
    launch {
        pressScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = LIQUID_SHEET_HANDLE_RELEASE_DURATION_MS),
        )
    }
    launch {
        pressWidth.animateTo(
            targetValue = LIQUID_SHEET_HANDLE_REST_WIDTH,
            animationSpec = tween(durationMillis = LIQUID_SHEET_HANDLE_RELEASE_DURATION_MS),
        )
    }
}

internal suspend fun animateSheetHeightTo(
    visibleSheetHeightPx: MutableFloatState,
    targetValue: Float,
    initialVelocity: Float,
    onHeightChanged: (Float) -> Unit,
) {
    animate(
        initialValue = visibleSheetHeightPx.floatValue,
        targetValue = targetValue,
        animationSpec =
            folmeSpring(
                damping = LIQUID_SHEET_SETTLE_DAMPING,
                response = LIQUID_SHEET_SETTLE_RESPONSE,
            ),
        initialVelocity = initialVelocity,
    ) { value, _ ->
        visibleSheetHeightPx.floatValue = value
        onHeightChanged(value)
    }
    visibleSheetHeightPx.floatValue = targetValue
    onHeightChanged(targetValue)
}

internal suspend fun animateDismissOffsetTo(
    dismissOffsetY: MutableFloatState,
    dimAlpha: MutableFloatState,
    targetValue: Float,
    initialVelocity: Float,
) {
    animate(
        initialValue = dismissOffsetY.floatValue,
        targetValue = targetValue,
        animationSpec =
            folmeSpring(
                damping = LIQUID_SHEET_SETTLE_DAMPING,
                response = LIQUID_SHEET_SETTLE_RESPONSE,
            ),
        initialVelocity = initialVelocity,
    ) { value, _ ->
        dismissOffsetY.floatValue = value
        dimAlpha.floatValue = 1f
    }
    dismissOffsetY.floatValue = targetValue
    dimAlpha.floatValue = 1f
}

internal suspend fun animateDismissOffScreen(
    dismissOffsetY: MutableFloatState,
    visibleHeightPx: Float,
    windowHeightPx: Float,
    dimAlpha: MutableFloatState,
    velocity: Float = 0f,
    onDismiss: () -> Unit,
) {
    val sheetHeight = visibleHeightPx
    val thresholdPx = if (sheetHeight > 0f) sheetHeight else 500f
    val targetValue = maxOf(sheetHeight, windowHeightPx)
    animate(
        initialValue = dismissOffsetY.floatValue,
        targetValue = targetValue,
        animationSpec =
            folmeSpring(
                damping = LIQUID_SHEET_SETTLE_DAMPING,
                response = LIQUID_SHEET_SETTLE_RESPONSE,
            ),
        initialVelocity = velocity,
    ) { value, _ ->
        dismissOffsetY.floatValue = value
        dimAlpha.floatValue = 1f - (value / thresholdPx).coerceIn(0f, 1f)
    }
    onDismiss()
}
