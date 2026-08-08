@file:Suppress("FunctionName", "PropertyName")

package os.kei.ui.page.main.widget.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigationevent.findViewTreeNavigationEventDispatcherOwner
import kotlinx.coroutines.launch
import os.kei.ui.page.main.widget.glass.LiquidBackdropWindowPopup
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.window.PopupPositionProvider as ComposePopupPositionProvider

enum class SnapshotPopupPlacement {
    Dropdown,
    ButtonEnd,
    ActionBarCenter,
}

internal data class SnapshotPopupLayoutInfo(
    val offset: IntOffset,
    val showBelow: Boolean,
    val showAbove: Boolean,
    val transformOrigin: TransformOrigin,
)

internal data class SnapshotPopupSafeInsets(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

/**
 * Liquid glass popup expansion animation.
 *
 * Spring-based for a natural, elastic feel — slightly underdamped so the popup settles smoothly
 * without an obvious overshoot. Replaces the linear-feeling Miuix default fraction spec.
 */
private val SnapshotPopupFractionAnimationSpec =
    spring<Float>(
        dampingRatio = 0.82f,
        stiffness = 420f,
        visibilityThreshold = 0.001f,
    )

/**
 * Liquid glass popup collapse animation — quicker and more linear, so dismissal feels decisive.
 */
private val SnapshotPopupFractionExitAnimationSpec =
    tween<Float>(
        durationMillis = 180,
        easing = FastOutLinearInEasing,
    )

/** Alpha fades in slightly slower than the geometric reveal so content remains crisp. */
private val SnapshotPopupAlphaEnterAnimationSpec =
    tween<Float>(
        durationMillis = 220,
        easing = LinearOutSlowInEasing,
    )

/** Alpha fades out faster than the geometric collapse to avoid lingering ghost frames. */
private val SnapshotPopupAlphaExitAnimationSpec =
    tween<Float>(
        durationMillis = 140,
        easing = FastOutLinearInEasing,
    )

/** Initial scale of the popup before expansion — subtle so it grows into place rather than popping. */
private const val SnapshotPopupInitialScale = 0.88f

/** Vertical offset (in dp) the popup translates from before settling — adds directional cue. */
private const val SnapshotPopupTranslationDp = 8f

@Composable
fun SnapshotWindowListPopup(
    show: Boolean,
    modifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.Start,
    anchorBounds: IntRect? = null,
    placement: SnapshotPopupPlacement = SnapshotPopupPlacement.Dropdown,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    maxHeight: Dp? = null,
    minWidth: Dp = 0.dp,
    maxWidth: Dp? = 280.dp,
    matchAnchorWidth: Boolean = false,
    content: @Composable () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val explicitAnchorBounds = anchorBounds
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val displayCutout = WindowInsets.displayCutout
    val statusBars = WindowInsets.statusBars
    val navigationBars = WindowInsets.navigationBars
    val captionBar = WindowInsets.captionBar
    val safeInsets =
        with(density) {
            SnapshotPopupSafeInsets(
                left =
                    maxOf(
                        displayCutout.getLeft(this, layoutDirection),
                        navigationBars.getLeft(this, layoutDirection),
                        captionBar.getLeft(this, layoutDirection),
                    ),
                top =
                    maxOf(
                        displayCutout.getTop(this),
                        statusBars.getTop(this),
                        captionBar.getTop(this),
                    ),
                right =
                    maxOf(
                        displayCutout.getRight(this, layoutDirection),
                        navigationBars.getRight(this, layoutDirection),
                        captionBar.getRight(this, layoutDirection),
                    ),
                bottom =
                    maxOf(
                        displayCutout.getBottom(this),
                        navigationBars.getBottom(this),
                        captionBar.getBottom(this),
                    ),
            )
        }
    val anchorWidthDp =
        remember(explicitAnchorBounds, density) {
            explicitAnchorBounds?.let { with(density) { it.width.toDp() } } ?: 0.dp
        }
    val resolvedMinWidth =
        if (matchAnchorWidth) {
            maxOf(minWidth, anchorWidthDp)
        } else {
            minWidth
        }
    val popupMinWidth = maxWidth?.let { resolvedMinWidth.coerceAtMost(it) } ?: resolvedMinWidth
    var popupLayoutInfo by remember { mutableStateOf<SnapshotPopupLayoutInfo?>(null) }
    val popupPlacementReady = popupLayoutInfo != null
    val fractionProgress = remember { Animatable(0f) }
    val alphaProgress = remember { Animatable(0f) }
    val fractionProgressProvider = remember(fractionProgress) { { fractionProgress.value.coerceIn(0f, 1f) } }
    val alphaProgressProvider = remember(alphaProgress) { { alphaProgress.value.coerceIn(0f, 1f) } }
    val coroutineScope = rememberCoroutineScope()
    var wasVisible by remember { mutableStateOf(show) }
    var popupRender by remember { mutableStateOf(show) }
    val currentOnDismissRequest = rememberUpdatedState(onDismissRequest)
    val currentOnDismissFinished = rememberUpdatedState(onDismissFinished)
    // The focused popup window has no back dispatcher of its own; the system falls back to
    // the host window, so the popup's back handler must bind to the host window's view-tree
    // owner. The composition local can't be used here: miuix-nav explicitly provides an
    // entry-scoped owner that never receives the host window's fallback events.
    val hostView = LocalView.current
    val navigationEventDispatcherOwner =
        remember(hostView) { hostView.findViewTreeNavigationEventDispatcherOwner() }
    val composePopupPositionProvider =
        remember(
            density,
            popupPositionProvider,
            alignment,
            placement,
            explicitAnchorBounds,
            safeInsets,
        ) {
            object : ComposePopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    val effectiveAnchorBounds = explicitAnchorBounds ?: anchorBounds
                    val popupMargin = popupPositionProvider.getMargins().toIntRect(density, layoutDirection)
                    val windowBounds = calculateSnapshotPopupWindowBounds(windowSize, safeInsets)
                    val providerOffset =
                        popupPositionProvider.calculatePosition(
                            anchorBounds = effectiveAnchorBounds,
                            windowBounds = windowBounds,
                            layoutDirection = layoutDirection,
                            popupContentSize = popupContentSize,
                            popupMargin = popupMargin,
                            alignment = alignment,
                        )
                    val resolvedLayout =
                        calculateSnapshotPopupLayout(
                            anchorBounds = effectiveAnchorBounds,
                            windowBounds = windowBounds,
                            layoutDirection = layoutDirection,
                            popupContentSize = popupContentSize,
                            popupMargin = popupMargin,
                            alignment = alignment,
                            placement = placement,
                            providerOffset = providerOffset,
                        )
                    if (
                        popupContentSize.width > 0 &&
                        popupContentSize.height > 0 &&
                        popupLayoutInfo != resolvedLayout
                    ) {
                        popupLayoutInfo = resolvedLayout
                    }
                    return resolvedLayout.offset
                }
            }
        }

    LaunchedEffect(show, popupPlacementReady, transitionAnimationsEnabled) {
        if (show) {
            wasVisible = true
            popupRender = true
            if (!popupPlacementReady) return@LaunchedEffect
            if (transitionAnimationsEnabled) {
                launch {
                    fractionProgress.animateTo(1f, SnapshotPopupFractionAnimationSpec)
                }
                alphaProgress.animateTo(1f, SnapshotPopupAlphaEnterAnimationSpec)
            } else {
                fractionProgress.snapTo(1f)
                alphaProgress.snapTo(1f)
            }
        } else {
            if (!popupRender && !wasVisible) return@LaunchedEffect
            if (transitionAnimationsEnabled) {
                // Run both animations in parallel and wait for BOTH to complete before
                // removing the Popup. Previously fractionProgress.stop() was called after
                // alpha finished, which killed the fraction animation mid-flight and caused
                // the dropdown to "flash disappear" without a visible collapse.
                val fractionJob =
                    launch {
                        fractionProgress.animateTo(0f, SnapshotPopupFractionExitAnimationSpec)
                    }
                val alphaJob =
                    launch {
                        alphaProgress.animateTo(0f, SnapshotPopupAlphaExitAnimationSpec)
                    }
                fractionJob.join()
                alphaJob.join()
            } else {
                fractionProgress.snapTo(0f)
                alphaProgress.snapTo(0f)
            }
            popupRender = false
            popupLayoutInfo = null
            if (wasVisible) {
                wasVisible = false
                currentOnDismissFinished.value?.invoke()
            }
        }
    }

    if (popupRender) {
        LiquidBackdropWindowPopup(
            popupPositionProvider = composePopupPositionProvider,
            onDismissRequest = { currentOnDismissRequest.value?.invoke() },
            properties =
                PopupProperties(
                    focusable = true,
                    // A focusable popup window owns back dispatch and has no navigation-event
                    // dispatcher, so the platform popup must handle back itself; the dismiss
                    // request runs the normal collapse animation before removal. (Upstream
                    // miuix hosts list popups in a Dialog window instead, which is the path
                    // to gesture-progress predictive collapse if ever needed.)
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    clippingEnabled = false,
                ),
        ) {
            val popupContent: @Composable () -> Unit = {
                SnapshotPopupBackHandler(
                    show = show,
                    popupRender = popupRender,
                    fractionProgress = fractionProgress,
                    alphaProgress = alphaProgress,
                    onDismissRequest = { currentOnDismissRequest.value?.invoke() },
                )
                val translationOffsetPx = with(density) { SnapshotPopupTranslationDp.dp.toPx() }
                val resolvedLayout = popupLayoutInfo
                Box(
                    modifier =
                        modifier
                            .defaultMinSize(minWidth = popupMinWidth)
                            .then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
                            .then(if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier)
                            .snapshotPopupReveal(
                                fractionProgress = fractionProgressProvider,
                                alphaProgress = alphaProgressProvider,
                                transformOrigin =
                                    resolvedLayout?.transformOrigin
                                        ?: TransformOrigin(0.5f, 0.5f),
                                showBelow = resolvedLayout?.showBelow == true,
                                showAbove = resolvedLayout?.showAbove == true,
                                translationOffsetPx = translationOffsetPx,
                            ),
                ) {
                    content()
                }
            }
            if (navigationEventDispatcherOwner != null) {
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner) {
                    popupContent()
                }
            } else {
                popupContent()
            }
        }
    }
}

@Composable
private fun SnapshotPopupBackHandler(
    show: Boolean,
    popupRender: Boolean,
    fractionProgress: Animatable<Float, AnimationVector1D>,
    alphaProgress: Animatable<Float, AnimationVector1D>,
    onDismissRequest: (() -> Unit)?,
) {
    val coroutineScope = rememberCoroutineScope()
    var dismissRequestDispatched by remember(show) { mutableStateOf(false) }
    if (LocalNavigationEventDispatcherOwner.current != null) {
        val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationEventState,
            isBackEnabled = popupRender,
            onBackCancelled = {
                if (show) {
                    coroutineScope.launch {
                        launch { fractionProgress.animateTo(1f, SnapshotPopupFractionAnimationSpec) }
                        alphaProgress.animateTo(1f, SnapshotPopupAlphaEnterAnimationSpec)
                    }
                }
            },
            onBackCompleted = {
                if (show && !dismissRequestDispatched) {
                    dismissRequestDispatched = true
                    onDismissRequest?.invoke()
                }
            },
        )
        LaunchedEffect(navigationEventState) {
            snapshotFlow { navigationEventState.transitionState }
                .collect { transitionState ->
                    if (
                        show &&
                        transitionState is NavigationEventTransitionState.InProgress &&
                        transitionState.direction == NavigationEventTransitionState.TRANSITIONING_BACK
                    ) {
                        val progress = 1f - transitionState.latestEvent.progress.coerceIn(0f, 1f)
                        fractionProgress.snapTo(progress)
                        alphaProgress.snapTo(progress)
                    }
                }
        }
    } else {
        BackHandler(enabled = popupRender) {
            if (show && !dismissRequestDispatched) {
                dismissRequestDispatched = true
                onDismissRequest?.invoke()
            }
        }
    }
}

private fun Modifier.snapshotPopupReveal(
    fractionProgress: () -> Float,
    alphaProgress: () -> Float,
    transformOrigin: TransformOrigin,
    showBelow: Boolean,
    showAbove: Boolean,
    translationOffsetPx: Float,
): Modifier =
    graphicsLayer {
        val fraction = fractionProgress()
        val scale = SnapshotPopupInitialScale + (1f - SnapshotPopupInitialScale) * fraction
        val translationY =
            if (showBelow) {
                -translationOffsetPx * (1f - fraction)
            } else {
                translationOffsetPx * (1f - fraction)
            }
        scaleX = scale
        scaleY = scale
        this.translationY = translationY
        alpha = alphaProgress()
        this.transformOrigin = transformOrigin
    }.drawWithContent {
        val progress = fractionProgress()
        val showMiddle = !showBelow && !showAbove
        val clipStart =
            when {
                showAbove -> size.height * (1f - progress)
                showMiddle -> size.height * (0.5f - 0.5f * progress)
                else -> 0f
            }
        val clipBottom =
            when {
                showBelow -> size.height * progress
                showAbove -> size.height
                showMiddle -> size.height * (0.5f + 0.5f * progress)
                else -> size.height
            }
        if (clipBottom > clipStart) {
            clipRect(
                left = 0f,
                top = clipStart,
                right = size.width,
                bottom = clipBottom,
            ) {
                this@drawWithContent.drawContent()
            }
        }
    }

@Composable
fun SnapshotWindowBottomSheet(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    backgroundColor: Color? = null,
    enableWindowDim: Boolean = true,
    cornerRadius: Dp = BottomSheetDefaults.cornerRadius,
    sheetMaxWidth: Dp = BottomSheetDefaults.maxWidth,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    outsideMargin: DpSize = BottomSheetDefaults.outsideMargin,
    insideMargin: DpSize = DpSize(BottomSheetDefaults.insideMargin.width, 14.dp),
    defaultWindowInsetsPadding: Boolean = true,
    dragHandleColor: Color? = null,
    allowDismiss: Boolean = true,
    onBlockedDismissRequest: (() -> Unit)? = null,
    enableNestedScroll: Boolean = true,
    initialDetent: LiquidSheetInitialDetent = LiquidSheetInitialDetent.ThreeQuarter,
    surfaceTone: LiquidSheetSurfaceTone = LiquidSheetSurfaceTone.Default,
    preferExportedBackdrop: Boolean = false,
    content: @Composable () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var wasShown by remember { mutableStateOf(false) }
    LaunchedEffect(show) {
        if (show) {
            wasShown = true
        } else if (wasShown) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    val currentOnBlockedDismissRequest by rememberUpdatedState(onBlockedDismissRequest)
    val blockedDismissRequestGate = remember(show) { BlockedDismissRequestGate() }
    val requestBlockedDismiss: () -> Unit = {
        blockedDismissRequestGate.dispatch {
            currentOnBlockedDismissRequest?.invoke()
        }
    }

    LiquidGlassBottomSheet(
        show = show,
        modifier = modifier,
        title = title,
        startAction = startAction,
        endAction = endAction,
        backgroundColor = backgroundColor,
        enableWindowDim = enableWindowDim,
        cornerRadius = cornerRadius,
        sheetMaxWidth = sheetMaxWidth,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        outsideMargin = outsideMargin,
        insideMargin = insideMargin,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        dragHandleColor = dragHandleColor,
        allowDismiss = allowDismiss,
        onBlockedDismissRequest =
            requestBlockedDismiss.takeIf {
                onBlockedDismissRequest != null
            },
        enableNestedScroll = enableNestedScroll,
        initialDetent = initialDetent,
        surfaceTone = surfaceTone,
        preferExportedBackdrop = preferExportedBackdrop,
        content = content,
    )
}

private const val BlockedSheetDismissDeduplicationWindowNanos = 200_000_000L

internal class BlockedDismissRequestGate(
    private val deduplicationWindowNanos: Long = BlockedSheetDismissDeduplicationWindowNanos,
) {
    private var lastDispatchNanos: Long? = null

    fun dispatch(
        nowNanos: Long = System.nanoTime(),
        onDispatch: () -> Unit,
    ): Boolean {
        val previous = lastDispatchNanos
        if (previous != null && nowNanos - previous < deduplicationWindowNanos) return false
        lastDispatchNanos = nowNanos
        onDispatch()
        return true
    }
}


private fun PaddingValues.toIntRect(
    density: androidx.compose.ui.unit.Density,
    layoutDirection: LayoutDirection,
): IntRect =
    with(density) {
        IntRect(
            left = calculateLeftPadding(layoutDirection).roundToPx(),
            top = calculateTopPadding().roundToPx(),
            right = calculateRightPadding(layoutDirection).roundToPx(),
            bottom = calculateBottomPadding().roundToPx(),
        )
    }

private fun PopupPositionProvider.Align.normalizeForDropdown(layoutDirection: LayoutDirection): PopupPositionProvider.Align =
    when (this) {
        PopupPositionProvider.Align.End,
        PopupPositionProvider.Align.TopEnd,
        PopupPositionProvider.Align.BottomEnd,
        -> {
            if (layoutDirection == LayoutDirection.Ltr) {
                PopupPositionProvider.Align.End
            } else {
                PopupPositionProvider.Align.Start
            }
        }

        PopupPositionProvider.Align.Start,
        PopupPositionProvider.Align.TopStart,
        PopupPositionProvider.Align.BottomStart,
        -> {
            if (layoutDirection == LayoutDirection.Ltr) {
                PopupPositionProvider.Align.Start
            } else {
                PopupPositionProvider.Align.End
            }
        }
    }

internal fun calculateSnapshotPopupWindowBounds(
    windowSize: IntSize,
    safeInsets: SnapshotPopupSafeInsets,
): IntRect {
    val width = windowSize.width.coerceAtLeast(0)
    val height = windowSize.height.coerceAtLeast(0)
    val left = safeInsets.left.coerceIn(0, width)
    val top = safeInsets.top.coerceIn(0, height)
    val right = (width - safeInsets.right.coerceAtLeast(0)).coerceIn(left, width)
    val bottom = (height - safeInsets.bottom.coerceAtLeast(0)).coerceIn(top, height)
    return IntRect(left = left, top = top, right = right, bottom = bottom)
}

internal fun calculateSnapshotPopupLayout(
    anchorBounds: IntRect,
    windowBounds: IntRect,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize,
    popupMargin: IntRect,
    alignment: PopupPositionProvider.Align,
    placement: SnapshotPopupPlacement,
    providerOffset: IntOffset,
): SnapshotPopupLayoutInfo {
    val popupWidth = popupContentSize.width.coerceAtLeast(0)
    val popupHeight = popupContentSize.height.coerceAtLeast(0)
    val normalizedAlignment = alignment.normalizeForDropdown(layoutDirection)
    val rawX =
        when (placement) {
            SnapshotPopupPlacement.Dropdown -> {
                providerOffset.x
            }

            SnapshotPopupPlacement.ButtonEnd -> {
                anchorBounds.right - popupWidth - popupMargin.right
            }

            SnapshotPopupPlacement.ActionBarCenter -> {
                anchorBounds.left + (anchorBounds.width - popupWidth) / 2
            }
        }
    val minX = windowBounds.left + popupMargin.left
    val maxX =
        (windowBounds.right - popupWidth - popupMargin.right)
            .coerceAtLeast(minX)
    val minY = windowBounds.top + popupMargin.top
    val maxY =
        (windowBounds.bottom - popupHeight - popupMargin.bottom)
            .coerceAtLeast(minY)
    val offset =
        IntOffset(
            x = rawX.coerceIn(minX, maxX),
            y = providerOffset.y.coerceIn(minY, maxY),
        )

    val popupCenterY = offset.y + popupHeight / 2f
    val anchorCenterY = anchorBounds.top + anchorBounds.height / 2f
    val showBelow = popupCenterY > anchorCenterY
    val showAbove = popupCenterY < anchorCenterY
    val pivotX =
        when (placement) {
            SnapshotPopupPlacement.ActionBarCenter -> {
                0.5f
            }

            SnapshotPopupPlacement.Dropdown,
            SnapshotPopupPlacement.ButtonEnd,
            -> {
                val attachToEnd =
                    placement == SnapshotPopupPlacement.ButtonEnd ||
                        normalizedAlignment == PopupPositionProvider.Align.End
                val attachmentX =
                    if (attachToEnd) {
                        anchorBounds.right - popupMargin.right
                    } else {
                        anchorBounds.left + popupMargin.left
                    }
                if (popupWidth > 0) {
                    ((attachmentX - offset.x) / popupWidth.toFloat()).coerceIn(0f, 1f)
                } else {
                    if (attachToEnd) 1f else 0f
                }
            }
        }
    val pivotY =
        when {
            showBelow -> 0f
            showAbove -> 1f
            else -> 0.5f
        }
    return SnapshotPopupLayoutInfo(
        offset = offset,
        showBelow = showBelow,
        showAbove = showAbove,
        transformOrigin = TransformOrigin(pivotX, pivotY),
    )
}

fun Modifier.capturePopupAnchor(onBoundsChange: (IntRect) -> Unit): Modifier {
    return this.onGloballyPositioned { coordinates ->
        if (!coordinates.isAttached) return@onGloballyPositioned
        val position = coordinates.positionInWindow()
        if (!position.x.isFinite() || !position.y.isFinite()) return@onGloballyPositioned
        val right = position.x + coordinates.size.width
        val bottom = position.y + coordinates.size.height
        if (!right.isFinite() || !bottom.isFinite()) return@onGloballyPositioned
        onBoundsChange(
            IntRect(
                left = position.x.roundToInt(),
                top = position.y.roundToInt(),
                right = right.roundToInt(),
                bottom = bottom.roundToInt(),
            ),
        )
    }
}
