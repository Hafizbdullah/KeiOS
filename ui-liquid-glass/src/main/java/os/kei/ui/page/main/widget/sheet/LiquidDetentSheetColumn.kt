@file:Suppress("FunctionName")

// Adapted from compose-miuix-ui WindowBottomSheet / BottomSheetContentLayout.
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package os.kei.ui.page.main.widget.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.roundToInt

private const val LIQUID_SHEET_BLOCKED_DISMISS_RESISTANCE = 0.35f
private val LiquidSheetDismissVelocityThreshold = 800.dp

@Composable
internal fun LiquidDetentBottomSheetColumn(
    title: String?,
    backgroundColor: Color,
    cornerRadius: Dp,
    sheetMaxWidth: Dp,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    defaultWindowInsetsPadding: Boolean,
    dragHandleColor: Color,
    allowDismiss: Boolean,
    sheetHeightPx: MutableIntState,
    visibleSheetHeightPx: MutableFloatState,
    dismissOffsetY: MutableFloatState,
    userResizedSheet: MutableState<Boolean>,
    dimAlpha: MutableFloatState,
    transitionAnimationsEnabled: Boolean,
    dismissInProgress: Boolean,
    onAnimatedDismissRequest: (velocity: Float) -> Unit,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier,
    topInset: Dp,
    enableNestedScroll: Boolean,
    minimumFloatingHeight: Dp,
    onBlockedDismissRequest: (() -> Unit)?,
    contentCanScrollUp: () -> Boolean,
    dismissDragThresholdPx: Float,
    onVisibleHeightFractionChanged: (Float) -> Unit,
    onInteractionStarted: () -> Unit,
    onInteractionFinished: () -> Unit,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val windowHeight = windowInfo.containerDpSize.height
    val currentWindowHeight by rememberUpdatedState(windowHeight)
    val coroutineScope = rememberCoroutineScope()
    val settlingJob = remember { mutableStateOf<Job?>(null) }
    val isSettling = remember { mutableStateOf(false) }
    val settleGeneration = remember { mutableIntStateOf(0) }
    val currentContentCanScrollUp = rememberUpdatedState(contentCanScrollUp)
    val currentOnVisibleHeightFractionChanged = rememberUpdatedState(onVisibleHeightFractionChanged)
    val currentAllowDismiss by rememberUpdatedState(allowDismiss)
    val currentDismissInProgress by rememberUpdatedState(dismissInProgress)
    val currentOnAnimatedDismissRequest = rememberUpdatedState(onAnimatedDismissRequest)
    val currentOnBlockedDismissRequest by rememberUpdatedState(onBlockedDismissRequest)
    val currentOnInteractionStarted by rememberUpdatedState(onInteractionStarted)
    val currentOnInteractionFinished by rememberUpdatedState(onInteractionFinished)
    val minimumFloatingHeightPx = with(density) { minimumFloatingHeight.toPx() }

    fun maxVisibleHeightPx(): Float {
        val windowHeightPx = with(density) { currentWindowHeight.toPx() }
        val topInsetPx = with(density) { topInset.toPx() }
        return liquidSheetMaxVisibleHeightPx(
            windowHeightPx = windowHeightPx,
            topInsetPx = topInsetPx,
        )
    }

    fun naturalHeightPx(): Float = sheetHeightPx.intValue.toFloat().coerceAtLeast(0f)

    fun currentVisibleHeightPx(): Float {
        val requestedHeight =
            when {
                visibleSheetHeightPx.floatValue > 0f -> visibleSheetHeightPx.floatValue
                naturalHeightPx() > 0f -> naturalHeightPx()
                else -> maxVisibleHeightPx()
            }
        return requestedHeight.coerceIn(0f, maxVisibleHeightPx())
    }

    fun minimumVisibleHeightPx(): Float {
        val maxVisibleHeight = maxVisibleHeightPx()
        val minimumResizableHeight = minimumFloatingHeightPx.coerceAtMost(maxVisibleHeight)
        val naturalHeight = naturalHeightPx()
        return if (naturalHeight in 1f..minimumResizableHeight) {
            naturalHeight
        } else {
            minimumResizableHeight
        }
    }

    fun reportVisibleHeightFraction(heightPx: Float = currentVisibleHeightPx()) {
        currentOnVisibleHeightFractionChanged.value(
            liquidSheetVisibleHeightFraction(
                visibleHeightPx = heightPx,
                maxVisibleHeightPx = maxVisibleHeightPx(),
            ),
        )
    }

    fun updateDimAlpha(offset: Float) {
        dimAlpha.floatValue =
            if (allowDismiss && offset > 0f) {
                1f - (offset / dismissDragThresholdPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
            } else {
                1f
            }
    }

    fun cancelActiveSettle() {
        if (!isSettling.value) return
        settleGeneration.intValue += 1
        settlingJob.value?.cancel()
        settlingJob.value = null
        isSettling.value = false
        currentOnInteractionFinished()
    }

    LaunchedEffect(dismissInProgress) {
        if (dismissInProgress) {
            cancelActiveSettle()
        }
    }

    fun applyResizeDrag(delta: Float): Float {
        if (currentDismissInProgress) return 0f
        if (delta == 0f) return 0f
        userResizedSheet.value = true
        val beforeHeight = currentVisibleHeightPx()
        val beforeDismissOffset = dismissOffsetY.floatValue
        var remainingDelta = delta

        if (remainingDelta < 0f && dismissOffsetY.floatValue > 0f) {
            val restoredOffset = minOf(dismissOffsetY.floatValue, -remainingDelta)
            dismissOffsetY.floatValue -= restoredOffset
            remainingDelta += restoredOffset
        }

        if (remainingDelta != 0f) {
            val desiredHeight = currentVisibleHeightPx() - remainingDelta
            val minimumVisibleHeight = minimumVisibleHeightPx()
            val maximumVisibleHeight = maxVisibleHeightPx()
            when {
                desiredHeight >= maximumVisibleHeight -> {
                    visibleSheetHeightPx.floatValue = maximumVisibleHeight
                    dismissOffsetY.floatValue = 0f
                }

                desiredHeight >= minimumVisibleHeight -> {
                    visibleSheetHeightPx.floatValue = desiredHeight
                    dismissOffsetY.floatValue = 0f
                }

                else -> {
                    visibleSheetHeightPx.floatValue = minimumVisibleHeight
                    val extraOffset = minimumVisibleHeight - desiredHeight
                    dismissOffsetY.floatValue =
                        if (allowDismiss) {
                            extraOffset
                        } else {
                            extraOffset * LIQUID_SHEET_BLOCKED_DISMISS_RESISTANCE
                        }
                }
            }
        }

        updateDimAlpha(dismissOffsetY.floatValue)
        reportVisibleHeightFraction()
        return (beforeHeight - currentVisibleHeightPx()) +
            (dismissOffsetY.floatValue - beforeDismissOffset)
    }

    fun animateVisibleHeightTo(targetValue: Float) {
        if (currentDismissInProgress) return
        cancelActiveSettle()
        val resolvedTarget = targetValue.coerceIn(minimumVisibleHeightPx(), maxVisibleHeightPx())
        if (!transitionAnimationsEnabled) {
            userResizedSheet.value = true
            visibleSheetHeightPx.floatValue = resolvedTarget
            dismissOffsetY.floatValue = 0f
            dimAlpha.floatValue = 1f
            reportVisibleHeightFraction(resolvedTarget)
            return
        }
        val generation = settleGeneration.intValue + 1
        settleGeneration.intValue = generation
        userResizedSheet.value = true
        isSettling.value = true
        currentOnInteractionStarted()
        settlingJob.value =
            coroutineScope.launch {
                try {
                    animateSheetHeightTo(
                        visibleSheetHeightPx = visibleSheetHeightPx,
                        targetValue = resolvedTarget,
                        initialVelocity = 0f,
                        onHeightChanged = ::reportVisibleHeightFraction,
                    )
                    if (dismissOffsetY.floatValue != 0f) {
                        animateDismissOffsetTo(
                            dismissOffsetY = dismissOffsetY,
                            dimAlpha = dimAlpha,
                            targetValue = 0f,
                            initialVelocity = 0f,
                        )
                    }
                } finally {
                    if (settleGeneration.intValue == generation) {
                        settlingJob.value = null
                        isSettling.value = false
                        currentOnInteractionFinished()
                    }
                }
            }
    }

    val settle: (Float) -> Unit =
        remember(
            density,
            minimumFloatingHeight,
            transitionAnimationsEnabled,
        ) {
            settle@{ velocity ->
                if (currentDismissInProgress) return@settle
                cancelActiveSettle()
                val generation = settleGeneration.intValue + 1
                settleGeneration.intValue = generation
                isSettling.value = true
                currentOnInteractionStarted()
                settlingJob.value =
                    coroutineScope.launch {
                        val currentDismissOffset = dismissOffsetY.floatValue
                        val velocityThresholdPx =
                            with(density) { LiquidSheetDismissVelocityThreshold.toPx() }
                        val dismissAllowedAtStart = currentAllowDismiss
                        val thresholdDismissOffset =
                            if (dismissAllowedAtStart) {
                                currentDismissOffset
                            } else {
                                currentDismissOffset / LIQUID_SHEET_BLOCKED_DISMISS_RESISTANCE
                            }
                        val shouldRequestDismiss =
                            thresholdDismissOffset > 0f &&
                                (thresholdDismissOffset > dismissDragThresholdPx || velocity > velocityThresholdPx)
                        val targetHeight =
                            currentVisibleHeightPx()
                                .coerceIn(minimumVisibleHeightPx(), maxVisibleHeightPx())

                        try {
                            when {
                                shouldRequestDismiss && dismissAllowedAtStart -> {
                                    currentOnAnimatedDismissRequest.value(velocity)
                                }

                                shouldRequestDismiss -> {
                                    currentOnBlockedDismissRequest?.invoke()
                                    if (transitionAnimationsEnabled) {
                                        animateDismissOffsetTo(
                                            dismissOffsetY,
                                            dimAlpha,
                                            targetValue = 0f,
                                            initialVelocity = 0f,
                                        )
                                    } else {
                                        dismissOffsetY.floatValue = 0f
                                        dimAlpha.floatValue = 1f
                                    }
                                }

                                else -> {
                                    if (abs(visibleSheetHeightPx.floatValue - targetHeight) > 0.5f) {
                                        if (transitionAnimationsEnabled) {
                                            animateSheetHeightTo(
                                                visibleSheetHeightPx = visibleSheetHeightPx,
                                                targetValue = targetHeight,
                                                initialVelocity = -velocity,
                                                onHeightChanged = { heightPx ->
                                                    reportVisibleHeightFraction(heightPx)
                                                },
                                            )
                                        } else {
                                            visibleSheetHeightPx.floatValue = targetHeight
                                            reportVisibleHeightFraction(targetHeight)
                                        }
                                    }
                                    if (dismissOffsetY.floatValue != 0f) {
                                        if (transitionAnimationsEnabled) {
                                            animateDismissOffsetTo(
                                                dismissOffsetY = dismissOffsetY,
                                                dimAlpha = dimAlpha,
                                                targetValue = 0f,
                                                initialVelocity = velocity,
                                            )
                                        } else {
                                            dismissOffsetY.floatValue = 0f
                                            dimAlpha.floatValue = 1f
                                        }
                                    } else {
                                        dimAlpha.floatValue = 1f
                                    }
                                }
                            }
                        } catch (_: CancellationException) {
                        } finally {
                            if (settleGeneration.intValue == generation) {
                                settlingJob.value = null
                                isSettling.value = false
                                currentOnInteractionFinished()
                            }
                        }
                    }
            }
        }

    val nestedScrollConnection =
        remember(
            enableNestedScroll,
            allowDismiss,
            density,
            minimumFloatingHeight,
            dismissDragThresholdPx,
            settle,
        ) {
            var sheetConsumedScroll = false
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (!enableNestedScroll || currentDismissInProgress) return Offset.Zero
                    cancelActiveSettle()

                    val delta = available.y
                    if (
                        delta < 0f &&
                        (
                            dismissOffsetY.floatValue > 0f ||
                                (!currentContentCanScrollUp.value() && currentVisibleHeightPx() < maxVisibleHeightPx())
                        )
                    ) {
                        val consumedY = applyResizeDrag(delta)
                        if (consumedY != 0f) {
                            currentOnInteractionStarted()
                            sheetConsumedScroll = true
                            return Offset(0f, consumedY)
                        }
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (!enableNestedScroll || currentDismissInProgress) return Offset.Zero
                    val delta = available.y
                    if (delta > 0f) {
                        cancelActiveSettle()
                        val consumedY = applyResizeDrag(delta)
                        if (consumedY != 0f) {
                            currentOnInteractionStarted()
                            sheetConsumedScroll = true
                            return Offset(0f, consumedY)
                        }
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (!enableNestedScroll || isSettling.value || currentDismissInProgress) return Velocity.Zero
                    if (available.y > 0f && currentContentCanScrollUp.value()) {
                        return Velocity.Zero
                    }
                    if (sheetConsumedScroll || dismissOffsetY.floatValue != 0f) {
                        settle(available.y)
                        sheetConsumedScroll = false
                        return available
                    }
                    return Velocity.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    if (!enableNestedScroll || isSettling.value || currentDismissInProgress) return Velocity.Zero
                    if (sheetConsumedScroll || dismissOffsetY.floatValue != 0f) {
                        settle(available.y)
                        sheetConsumedScroll = false
                        return available
                    }
                    return Velocity.Zero
                }
            }
        }

    val imeInsets = WindowInsets.ime
    val sheetCornerShape =
        remember(cornerRadius) {
            RoundedRectangle(cornerRadius)
        }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier =
                modifier
                    .pointerInput(Unit) { detectTapGestures { } }
                    .then(if (enableNestedScroll) Modifier.nestedScroll(nestedScrollConnection) else Modifier)
                    .widthIn(max = sheetMaxWidth)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = windowHeight - topInset)
                    .then(
                        if (userResizedSheet.value) {
                            Modifier.liquidSheetOptionalHeightPx {
                                currentVisibleHeightPx().roundToInt()
                            }
                        } else {
                            Modifier
                        },
                    ).onGloballyPositioned { coordinates ->
                        if (imeInsets.getBottom(density) == 0 && !userResizedSheet.value) {
                            val measuredHeight = coordinates.size.height.toFloat()
                            sheetHeightPx.intValue = coordinates.size.height
                            visibleSheetHeightPx.floatValue = measuredHeight
                            reportVisibleHeightFraction(measuredHeight)
                        }
                    }.padding(horizontal = outsideMargin.width)
                    .then(surfaceModifier)
                    .clip(sheetCornerShape)
                    .background(backgroundColor)
                    .then(if (defaultWindowInsetsPadding) Modifier.imePadding() else Modifier)
                    .padding(horizontal = insideMargin.width)
                    .padding(bottom = insideMargin.height),
        ) {
            LiquidDetentTopChrome(
                title = title,
                startAction = startAction,
                endAction = endAction,
                dragHandleColor = dragHandleColor,
                coroutineScope = coroutineScope,
                canExpand =
                    !currentDismissInProgress &&
                        currentVisibleHeightPx() < maxVisibleHeightPx() - 0.5f,
                canCollapse =
                    !currentDismissInProgress &&
                        currentVisibleHeightPx() > minimumVisibleHeightPx() + 0.5f,
                canDismiss = !currentDismissInProgress && currentAllowDismiss,
                onExpand = { animateVisibleHeightTo(maxVisibleHeightPx()) },
                onCollapse = { animateVisibleHeightTo(minimumVisibleHeightPx()) },
                onDismiss = { currentOnAnimatedDismissRequest.value(0f) },
                onDragStarted = {
                    if (!currentDismissInProgress) currentOnInteractionStarted()
                },
                onDrag = { dragAmount ->
                    if (currentDismissInProgress) return@LiquidDetentTopChrome
                    cancelActiveSettle()
                    applyResizeDrag(dragAmount)
                },
                onSettle = settle,
            )
            content()
        }
    }
}
