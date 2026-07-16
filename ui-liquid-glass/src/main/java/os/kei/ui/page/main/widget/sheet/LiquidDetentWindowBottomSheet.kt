@file:Suppress("FunctionName")

// Adapted from compose-miuix-ui WindowBottomSheet / BottomSheetContentLayout.
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package os.kei.ui.page.main.widget.sheet

import android.view.WindowInsets as AndroidWindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import os.kei.ui.page.main.widget.glass.LiquidBackdropWindowDialog
import os.kei.ui.page.main.widget.motion.LocalPredictiveBackAnimationsEnabled
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.utils.RemovePlatformDialogDefaultEffects
import top.yukonga.miuix.kmp.utils.platformDialogProperties
import kotlin.math.roundToInt

private const val LIQUID_SHEET_BACKGROUND_MIN_DEPTH = 0.06f
private const val LIQUID_SHEET_BLOCKED_DISMISS_RESISTANCE = 0.35f
private const val LIQUID_SHEET_OPEN_CLOSE_DAMPING = 0.90f
private const val LIQUID_SHEET_OPEN_CLOSE_RESPONSE = 0.38f
private const val LIQUID_SHEET_RESET_DURATION_MS = 150

@Composable
internal fun LiquidDetentWindowBottomSheet(
    show: Boolean,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    backgroundColor: Color,
    enableWindowDim: Boolean,
    cornerRadius: Dp,
    sheetMaxWidth: Dp,
    onDismissRequest: (() -> Unit)?,
    onDismissFinished: (() -> Unit)?,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    defaultWindowInsetsPadding: Boolean,
    dragHandleColor: Color,
    allowDismiss: Boolean,
    enableNestedScroll: Boolean,
    minimumFloatingHeight: Dp,
    dismissDragThreshold: Dp,
    onBlockedDismissRequest: (() -> Unit)?,
    contentCanScrollUp: () -> Boolean = { false },
    backgroundDepthBlurRadius: Dp,
    onVisibleHeightFractionChanged: (Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val captionBarPadding = WindowInsets.captionBar.asPaddingValues().calculateTopPadding()
    val displayCutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val safeTopInset =
        remember(statusBarsPadding, captionBarPadding, displayCutoutPadding) {
            maxOf(statusBarsPadding, captionBarPadding, displayCutoutPadding)
        }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val requestDismiss: () -> Unit = {
        currentOnDismissRequest?.invoke()
    }
    LiquidDetentBottomSheetContentLayout(
        show = show,
        backgroundColor = backgroundColor,
        cornerRadius = cornerRadius,
        sheetMaxWidth = sheetMaxWidth,
        outsideMargin = outsideMargin,
        insideMargin = insideMargin,
        dragHandleColor = dragHandleColor,
        popupHost = { visible, hostContent ->
            if (visible) {
                LiquidBackdropWindowDialog(
                    onDismissRequest = {
                        if (allowDismiss) {
                            requestDismiss()
                        } else {
                            onBlockedDismissRequest?.invoke()
                        }
                    },
                    properties = platformDialogProperties(),
                ) {
                    RemovePlatformDialogDefaultEffects()
                    // ComponentDialog installs its own navigation event owner on the dialog view
                    // tree. Keep that owner so both ordinary and predictive back are dispatched
                    // through the dialog window that currently has focus.
                    hostContent()
                }
            }
        },
        modifier = modifier,
        title = title,
        startAction = startAction,
        endAction = endAction,
        enableWindowDim = enableWindowDim,
        onDismissRequest = requestDismiss,
        onDismissFinished = onDismissFinished,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        surfaceModifier = surfaceModifier,
        topInset = safeTopInset,
        minimumFloatingHeight = minimumFloatingHeight,
        dismissDragThreshold = dismissDragThreshold,
        onBlockedDismissRequest = onBlockedDismissRequest,
        contentCanScrollUp = contentCanScrollUp,
        backgroundDepthBlurRadius = backgroundDepthBlurRadius,
        onVisibleHeightFractionChanged = onVisibleHeightFractionChanged,
        content = {
            CompositionLocalProvider(
                LocalDismissState provides requestDismiss,
            ) {
                content()
            }
        },
    )
}

@Composable
private fun LiquidDetentBottomSheetContentLayout(
    show: Boolean,
    backgroundColor: Color,
    cornerRadius: Dp,
    sheetMaxWidth: Dp,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    dragHandleColor: Color,
    popupHost: @Composable (visible: Boolean, content: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    defaultWindowInsetsPadding: Boolean = true,
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    surfaceModifier: Modifier,
    topInset: Dp,
    minimumFloatingHeight: Dp,
    dismissDragThreshold: Dp,
    onBlockedDismissRequest: (() -> Unit)?,
    contentCanScrollUp: () -> Boolean,
    backgroundDepthBlurRadius: Dp,
    onVisibleHeightFractionChanged: (Float) -> Unit,
    content: @Composable () -> Unit,
) {
    val animationProgress = remember { Animatable(0f, visibilityThreshold = 0.0001f) }
    val dismissOffsetY = remember { mutableFloatStateOf(0f) }
    val visibleSheetHeightPx = remember { mutableFloatStateOf(0f) }
    val userResizedSheet = remember { mutableStateOf(false) }
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    val currentAllowDismiss by rememberUpdatedState(allowDismiss)
    val currentOnBlockedDismissRequest by rememberUpdatedState(onBlockedDismissRequest)
    val internalVisible = remember { mutableStateOf(false) }
    val sheetInteracting = remember { mutableStateOf(false) }
    val dismissCoordinator = remember { LiquidSheetDismissCoordinator() }
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current

    LaunchedEffect(show, transitionAnimationsEnabled) {
        dismissCoordinator.cancel()
        if (show) {
            internalVisible.value = true
            dismissOffsetY.floatValue = 0f
            visibleSheetHeightPx.floatValue = 0f
            userResizedSheet.value = false
            if (transitionAnimationsEnabled) {
                animationProgress.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        folmeSpring(
                            damping = LIQUID_SHEET_OPEN_CLOSE_DAMPING,
                            response = LIQUID_SHEET_OPEN_CLOSE_RESPONSE,
                        ),
                )
            } else {
                animationProgress.snapTo(1f)
            }
        } else {
            if (!internalVisible.value) return@LaunchedEffect
            if (!transitionAnimationsEnabled || dismissOffsetY.floatValue > 0f) {
                animationProgress.snapTo(0f)
            } else {
                animationProgress.animateTo(
                    targetValue = 0f,
                    animationSpec =
                        folmeSpring(
                            damping = LIQUID_SHEET_OPEN_CLOSE_DAMPING,
                            response = LIQUID_SHEET_OPEN_CLOSE_RESPONSE,
                        ),
                )
            }
            internalVisible.value = false
            currentOnDismissFinished?.invoke()
        }
    }

    if (!show && !internalVisible.value) return

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val coroutineScope = rememberCoroutineScope()
    val sheetHeightPx = remember { mutableIntStateOf(0) }
    val dimAlpha = remember { mutableFloatStateOf(1f) }
    val minimumFloatingHeightPx = with(density) { minimumFloatingHeight.toPx() }
    val dismissDragThresholdPx = with(density) { dismissDragThreshold.toPx() }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val predictiveBackAnimationsEnabled = LocalPredictiveBackAnimationsEnabled.current
    val requestImmediateDismiss: () -> Unit = {
        if (!dismissCoordinator.isInProgress) {
            currentOnDismissRequest?.invoke()
        }
    }

    fun windowHeightPx(): Float = with(density) { windowInfo.containerDpSize.height.toPx() }

    fun maxVisibleHeightPx(): Float =
        liquidSheetMaxVisibleHeightPx(
            windowHeightPx = windowHeightPx(),
            topInsetPx = with(density) { topInset.toPx() },
        )

    fun visibleHeightPx(): Float {
        val naturalHeight = sheetHeightPx.intValue.toFloat()
        val requestedHeight =
            when {
                visibleSheetHeightPx.floatValue > 0f -> visibleSheetHeightPx.floatValue
                naturalHeight > 0f -> naturalHeight
                else -> maxVisibleHeightPx()
            }
        return requestedHeight.coerceIn(0f, maxVisibleHeightPx())
    }

    fun sheetPlacementOffsetPx(): Float {
        val currentHeight = visibleHeightPx()
        val fallbackHeight = windowHeightPx()
        val baseOffset = if (currentHeight > 0f) currentHeight else fallbackHeight
        return baseOffset * (1f - animationProgress.value) + dismissOffsetY.floatValue
    }

    fun sheetTopOffsetPx(): Float {
        val currentHeight = visibleHeightPx()
        val windowHeightPx = windowHeightPx()
        return if (currentHeight > 0f) {
            (windowHeightPx - currentHeight + sheetPlacementOffsetPx()).coerceIn(0f, windowHeightPx)
        } else {
            windowHeightPx
        }
    }

    fun backgroundDepthProgress(): Float {
        val currentHeight = visibleHeightPx()
        val windowHeightPx = windowHeightPx()
        val visibleHeightFraction =
            if (currentHeight > 0f && windowHeightPx > 0f) {
                (currentHeight / windowHeightPx).coerceIn(0f, 1f)
            } else {
                1f
            }
        val heightDepth =
            (
                (visibleHeightFraction - LIQUID_SHEET_BACKGROUND_MIN_DEPTH) /
                    (1f - LIQUID_SHEET_BACKGROUND_MIN_DEPTH)
            ).coerceIn(0f, 1f)
        val dismissProgress =
            (dismissOffsetY.floatValue / dismissDragThresholdPx.coerceAtLeast(1f))
                .coerceIn(0f, 1f)
        return animationProgress.value * heightDepth * (1f - dismissProgress)
    }

    fun backgroundBlurLayerHeightPx(): Float {
        if (sheetInteracting.value) return 0f
        val depth = liquidSheetSmoothStep(backgroundDepthProgress())
        return if (liquidSheetBackgroundBlurLayerAlpha(depth) > 0f) {
            liquidSheetBackgroundBlurLayerHeightPx(
                sheetTopOffsetPx = sheetTopOffsetPx(),
                cornerRadiusPx = with(density) { cornerRadius.toPx() },
                windowHeightPx = windowHeightPx(),
            )
        } else {
            0f
        }
    }

    fun backgroundBlurLayerAlpha(): Float {
        if (sheetInteracting.value) return 0f
        val depth = liquidSheetSmoothStep(backgroundDepthProgress())
        return liquidSheetBackgroundBlurLayerAlpha(depth)
    }

    val resetGesture: suspend () -> Unit = {
        if (transitionAnimationsEnabled) {
            animate(
                dismissOffsetY.floatValue,
                0f,
                animationSpec = tween(durationMillis = LIQUID_SHEET_RESET_DURATION_MS),
            ) { value, _ ->
                dismissOffsetY.floatValue = value
            }
            animate(
                dimAlpha.floatValue,
                1f,
                animationSpec = tween(durationMillis = LIQUID_SHEET_RESET_DURATION_MS),
            ) { value, _ ->
                dimAlpha.floatValue = value
            }
        } else {
            dismissOffsetY.floatValue = 0f
            dimAlpha.floatValue = 1f
        }
    }

    val requestAnimatedDismiss: (Float) -> Unit = requestAnimatedDismiss@{ velocity ->
        if (dismissCoordinator.isInProgress) return@requestAnimatedDismiss
        if (currentAllowDismiss) {
            dismissCoordinator.launch(coroutineScope) {
                sheetInteracting.value = true
                try {
                    if (transitionAnimationsEnabled) {
                        animateDismissOffScreen(
                            dismissOffsetY = dismissOffsetY,
                            visibleHeightPx = visibleHeightPx(),
                            windowHeightPx = windowHeightPx(),
                            dimAlpha = dimAlpha,
                            velocity = velocity,
                            onDismiss = {},
                        )
                    } else {
                        dismissOffsetY.floatValue = maxOf(visibleHeightPx(), windowHeightPx())
                        dimAlpha.floatValue = 0f
                    }
                    if (currentAllowDismiss) {
                        currentOnDismissRequest?.invoke()
                    } else {
                        currentOnBlockedDismissRequest?.invoke()
                        resetGesture()
                    }
                } finally {
                    sheetInteracting.value = false
                }
            }
        } else {
            currentOnBlockedDismissRequest?.invoke()
            coroutineScope.launch { resetGesture() }
        }
    }

    popupHost(internalVisible.value) {
        // The Dialog owns its own View, focus tree, and inset controller. Resolve these locals
        // inside the host content so an IME opened by a Sheet field is dismissed through the
        // Dialog window rather than the activity window behind it.
        val dialogDensity = LocalDensity.current
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val view = LocalView.current
        val composeImeBottomPx = WindowInsets.ime.getBottom(dialogDensity)
        val completeBack: () -> Unit = {
            if (
                liquidSheetImeVisible(
                    composeImeBottomPx = composeImeBottomPx,
                    platformImeVisible =
                        view.rootWindowInsets
                            ?.isVisible(AndroidWindowInsets.Type.ime()) == true,
                )
            ) {
                focusManager.clearFocus(force = true)
                view.windowInsetsController?.hide(AndroidWindowInsets.Type.ime())
                    ?: keyboardController?.hide()
                coroutineScope.launch { resetGesture() }
            } else {
                requestAnimatedDismiss(0f)
            }
        }
        val navigationEventOwnerAvailable =
            androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner.current != null
        if (navigationEventOwnerAvailable) {
            val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
            NavigationBackHandler(
                state = navigationEventState,
                isBackEnabled = show && predictiveBackAnimationsEnabled,
                onBackCancelled = {
                    coroutineScope.launch { resetGesture() }
                },
                onBackCompleted = completeBack,
            )

            LaunchedEffect(
                navigationEventState,
                predictiveBackAnimationsEnabled,
                allowDismiss,
            ) {
                if (!predictiveBackAnimationsEnabled) return@LaunchedEffect
                snapshotFlow { navigationEventState.transitionState }
                    .collect { transitionState ->
                        if (
                            transitionState is NavigationEventTransitionState.InProgress &&
                            transitionState.direction == NavigationEventTransitionState.TRANSITIONING_BACK
                        ) {
                            val progress = transitionState.latestEvent.progress.coerceIn(0f, 1f)
                            val baseOffset = visibleHeightPx().coerceAtLeast(500f)
                            dismissOffsetY.floatValue =
                                if (allowDismiss) {
                                    progress * baseOffset
                                } else {
                                    progress * baseOffset * LIQUID_SHEET_BLOCKED_DISMISS_RESISTANCE
                                }
                            dimAlpha.floatValue =
                                if (allowDismiss) {
                                    1f - progress
                                } else {
                                    1f
                                }
                        }
                    }
            }
        }
        BackHandler(
            enabled = show && (!navigationEventOwnerAvailable || !predictiveBackAnimationsEnabled),
            onBack = completeBack,
        )

        if (enableWindowDim) {
            LiquidDetentBackgroundDimLayer(
                dimAlpha = dimAlpha,
                depthProgress = ::backgroundDepthProgress,
                blurLayerHeightPx = ::backgroundBlurLayerHeightPx,
                blurLayerAlpha = ::backgroundBlurLayerAlpha,
                blurRadius = backgroundDepthBlurRadius,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInputDismissLayer(
                        allowDismiss = allowDismiss,
                        onDismissRequest = requestImmediateDismiss,
                        onBlockedDismissRequest = onBlockedDismissRequest,
                    ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val sheetModifier =
                modifier
                    .liquidSheetFloatingPlacement {
                        sheetPlacementOffsetPx()
                    }
            CompositionLocalProvider(
                LocalLiquidSheetVisibleHeightPx provides {
                    if (userResizedSheet.value) {
                        visibleHeightPx().roundToInt()
                    } else {
                        0
                    }
                },
            ) {
                LiquidDetentBottomSheetColumn(
                    title = title,
                    backgroundColor = backgroundColor,
                    cornerRadius = cornerRadius,
                    sheetMaxWidth = sheetMaxWidth,
                    outsideMargin = outsideMargin,
                    insideMargin = insideMargin,
                    defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                    dragHandleColor = dragHandleColor,
                    allowDismiss = allowDismiss,
                    sheetHeightPx = sheetHeightPx,
                    visibleSheetHeightPx = visibleSheetHeightPx,
                    dismissOffsetY = dismissOffsetY,
                    userResizedSheet = userResizedSheet,
                    dimAlpha = dimAlpha,
                    transitionAnimationsEnabled = transitionAnimationsEnabled,
                    dismissInProgress = dismissCoordinator.isInProgress,
                    onAnimatedDismissRequest = requestAnimatedDismiss,
                    modifier = sheetModifier,
                    surfaceModifier = surfaceModifier,
                    topInset = topInset,
                    enableNestedScroll = enableNestedScroll,
                    minimumFloatingHeight = minimumFloatingHeight,
                    onBlockedDismissRequest = onBlockedDismissRequest,
                    contentCanScrollUp = contentCanScrollUp,
                    dismissDragThresholdPx = dismissDragThresholdPx,
                    onVisibleHeightFractionChanged = { fraction ->
                        onVisibleHeightFractionChanged(fraction)
                    },
                    onInteractionStarted = {
                        sheetInteracting.value = true
                    },
                    onInteractionFinished = {
                        if (!dismissCoordinator.isInProgress) {
                            sheetInteracting.value = false
                        }
                    },
                    startAction =
                        startAction?.let { action ->
                            { CompositionLocalProvider(LocalDismissState provides requestImmediateDismiss) { action() } }
                        },
                    endAction =
                        endAction?.let { action ->
                            { CompositionLocalProvider(LocalDismissState provides requestImmediateDismiss) { action() } }
                        },
                    content = content,
                )
            }
        }
    }
}

internal fun liquidSheetImeVisible(
    composeImeBottomPx: Int,
    platformImeVisible: Boolean,
): Boolean = composeImeBottomPx > 0 || platformImeVisible
