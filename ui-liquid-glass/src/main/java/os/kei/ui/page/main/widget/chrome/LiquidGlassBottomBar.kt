@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.chrome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import os.kei.ui.animation.DampedDragAnimation
import os.kei.ui.animation.InteractiveHighlight
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.glass.appGlassRuntimeEffectsEnabled
import os.kei.ui.page.main.widget.glass.glassEffectRuntime
import os.kei.ui.page.main.widget.glass.radialRefraction
import os.kei.ui.page.main.widget.glass.safeLiquidLens
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import os.kei.ui.page.main.widget.shape.appSquircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

val LocalLiquidGlassBottomBarTabScale = staticCompositionLocalOf { { 1f } }
private val LocalLiquidGlassBottomBarSelectionProgress = staticCompositionLocalOf<(Int) -> Float> { { 0f } }
private val LocalLiquidGlassBottomBarContentColor = staticCompositionLocalOf<(Int) -> Color> { { Color.Unspecified } }
private val LocalLiquidGlassBottomBarItemInteractive = staticCompositionLocalOf { true }
private val LocalLiquidGlassBottomBarItemPressHandler =
    staticCompositionLocalOf<(Int, Boolean) -> Unit> {
        { _, _ -> }
    }

@Immutable
data class LiquidGlassBottomBarSelectionOptics(
    val overlayColor: Color,
    val rimColor: Color,
    val rimWidth: Dp = 1.dp,
)

@Composable
fun liquidGlassBottomBarItemSelectionProgress(tabIndex: Int): Float = LocalLiquidGlassBottomBarSelectionProgress.current(tabIndex)

@Composable
fun liquidGlassBottomBarItemContentColor(tabIndex: Int): Color = LocalLiquidGlassBottomBarContentColor.current(tabIndex)

@Composable
fun RowScope.LiquidGlassBottomBarItem(
    selected: Boolean,
    tabIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val selectedScale = LocalLiquidGlassBottomBarTabScale.current
    val selectionProgress = liquidGlassBottomBarItemSelectionProgress(tabIndex)
    val interactive = LocalLiquidGlassBottomBarItemInteractive.current
    val onItemPressed = LocalLiquidGlassBottomBarItemPressHandler.current
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnItemPressed by rememberUpdatedState(onItemPressed)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale =
        when {
            selected || selectionProgress > 0f -> lerp(1f, selectedScale(), selectionProgress)
            else -> 1f
        }
    val scaleState =
        appMotionFloatState(
            targetValue = targetScale,
            durationMillis = 160,
            label = "liquid_bottom_bar_item_scale",
        )
    val scaleProvider = remember(scaleState) { { scaleState.value } }
    LaunchedEffect(interactive, enabled, isPressed, tabIndex) {
        if (interactive) {
            currentOnItemPressed(tabIndex, enabled && isPressed)
        }
    }
    DisposableEffect(interactive, tabIndex) {
        onDispose {
            if (interactive) {
                currentOnItemPressed(tabIndex, false)
            }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .weight(1f)
                .then(
                    if (interactive) {
                        Modifier.selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.Tab,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { currentOnClick() },
                        )
                    } else {
                        Modifier.clearAndSetSemantics {}
                    },
                ).graphicsLayer {
                    val scale = scaleProvider()
                    scaleX = scale
                    scaleY = scale
                    alpha = if (enabled) 1f else 0.38f
                },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier =
                if (interactive && label != null) {
                    Modifier.clearAndSetSemantics {
                        contentDescription = label
                    }
                } else {
                    Modifier
                },
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
fun LiquidGlassBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    selectedPosition: Float? = null,
    selectedPositionProvider: (() -> Float?)? = null,
    onSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    isTabEnabled: (Int) -> Boolean = { true },
    interactionEnabled: Boolean = true,
    isLiquidEffectEnabled: Boolean = true,
    expandToMaxWidth: Boolean = false,
    selectionOptics: LiquidGlassBottomBarSelectionOptics? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isInLightTheme = !isAppInDarkTheme()
    val transitionAnimationsEnabled = LocalTransitionAnimationsEnabled.current
    val animationScope = rememberCoroutineScope()
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val effectiveLiquidEffectEnabled = isLiquidEffectEnabled && appGlassRuntimeEffectsEnabled()
    val accentColor = MiuixTheme.colorScheme.primary

    val safeTabsCount = tabsCount.coerceAtLeast(1)
    val horizontalPadding = AppChromeTokens.floatingBottomBarHorizontalPadding
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
    val pressLiftPx = with(density) { 1.25.dp.toPx() }
    val panelMaxOffsetPx = with(density) { 4.dp.toPx() }

    val palette =
        rememberLiquidBottomBarPalette(
            isLiquidEffectEnabled = effectiveLiquidEffectEnabled,
            isInLightTheme = isInLightTheme,
            primary = accentColor,
            onSurface = MiuixTheme.colorScheme.onSurface,
            surfaceContainer = MiuixTheme.colorScheme.surfaceContainer,
        )

    val tabsBackdrop = rememberLayerBackdrop()

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val panelOffsetProvider =
        remember(panelMaxOffsetPx, offsetAnimation) {
            {
                liquidBottomBarPanelOffset(
                    rawOffsetPx = offsetAnimation.value,
                    totalWidthPx = totalWidthPx,
                    maxOffsetPx = panelMaxOffsetPx,
                )
            }
        }

    var currentIndex by remember(safeTabsCount) {
        mutableIntStateOf(selectedIndex.fastCoerceIn(0, safeTabsCount - 1))
    }
    var pressedTabIndex by remember(safeTabsCount) { mutableIntStateOf(-1) }
    var localDragSettledIndex by remember(safeTabsCount) { mutableIntStateOf(-1) }
    var selectionGestureDragged by remember(safeTabsCount) { mutableStateOf(false) }
    val currentOnSelected by rememberUpdatedState(onSelected)
    val currentIsTabEnabled = rememberUpdatedState(isTabEnabled)
    val currentInteractionEnabled = rememberUpdatedState(interactionEnabled)

    class DampedDragAnimationHolder {
        var instance: DampedDragAnimation? = null
    }

    val holder = remember { DampedDragAnimationHolder() }

    val dampedDragAnimation =
        remember(
            animationScope,
            safeTabsCount,
            density,
            isLtr,
            touchSlop,
            transitionAnimationsEnabled,
        ) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = currentIndex.toFloat(),
                valueRange = 0f..(safeTabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                animationsEnabled = transitionAnimationsEnabled,
                gestureKey = safeTabsCount to isLtr,
                dragOrientation = Orientation.Horizontal,
                dragTouchSlop = touchSlop,
                canDrag = { offset ->
                    if (!currentInteractionEnabled.value) return@DampedDragAnimation false
                    val animation = holder.instance ?: return@DampedDragAnimation true
                    if (tabWidthPx <= 0f || totalWidthPx <= 0f) return@DampedDragAnimation false
                    val paddingPx = with(density) { horizontalPadding.toPx() }
                    val indicatorX = animation.value * tabWidthPx
                    val globalTouchX =
                        if (isLtr) {
                            paddingPx + indicatorX + offset.x
                        } else {
                            totalWidthPx - paddingPx - tabWidthPx - indicatorX + offset.x
                        }
                    globalTouchX in 0f..totalWidthPx
                },
                onDragStarted = {
                    selectionGestureDragged = false
                },
                onDragStopped = {
                    val gestureWasDragged = selectionGestureDragged
                    selectionGestureDragged = false
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, safeTabsCount - 1)
                    val resolvedIndex =
                        liquidBottomBarResolvedEnabledIndex(
                            targetIndex = targetIndex,
                            currentIndex = currentIndex,
                            tabsCount = safeTabsCount,
                            isTabEnabled = currentIsTabEnabled.value,
                        )
                    localDragSettledIndex = resolvedIndex
                    val previousIndex = currentIndex
                    currentIndex = resolvedIndex
                    if (transitionAnimationsEnabled) {
                        animateToValue(resolvedIndex.toFloat())
                    } else {
                        snapToValue(resolvedIndex.toFloat())
                    }
                    if (resolvedIndex != previousIndex || !gestureWasDragged) {
                        currentOnSelected(resolvedIndex)
                    }
                    animationScope.launch {
                        if (transitionAnimationsEnabled) {
                            offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                        } else {
                            offsetAnimation.snapTo(0f)
                        }
                    }
                },
                onDragCancelled = {
                    selectionGestureDragged = false
                    localDragSettledIndex = -1
                    if (transitionAnimationsEnabled) {
                        animateToValue(currentIndex.toFloat())
                    } else {
                        snapToValue(currentIndex.toFloat())
                    }
                    animationScope.launch {
                        if (transitionAnimationsEnabled) {
                            offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                        } else {
                            offsetAnimation.snapTo(0f)
                        }
                    }
                },
                onDrag = { _, dragAmount ->
                    if (safeTabsCount > 1 && tabWidthPx > 0f) {
                        if (dragAmount != Offset.Zero) {
                            selectionGestureDragged = true
                        }
                        val progressDelta = dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f
                        snapToValue(
                            (value + progressDelta).fastCoerceIn(0f, (safeTabsCount - 1).toFloat()),
                        )
                        animationScope.launch {
                            offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                        }
                    }
                },
            ).also { holder.instance = it }
        }
    val externalSelectionPosition =
        liquidBottomBarFinitePosition(
            position = selectedPosition,
            tabsCount = safeTabsCount,
        )
    val currentExternalSelectionPosition = rememberUpdatedState(externalSelectionPosition)
    val currentSelectedPositionProvider = rememberUpdatedState(selectedPositionProvider)
    val currentLocalDragSettledIndex = rememberUpdatedState(localDragSettledIndex)
    val displaySelectionValueProvider =
        remember(safeTabsCount, dampedDragAnimation) {
            selectionProvider@{
                val localSettledIndex = currentLocalDragSettledIndex.value
                if (localSettledIndex >= 0) {
                    val target = localSettledIndex.fastCoerceIn(0, safeTabsCount - 1).toFloat()
                    val localStillSettling =
                        dampedDragAnimation.pressProgress > 0.001f ||
                            abs(dampedDragAnimation.value - target) > 0.001f
                    return@selectionProvider if (localStillSettling) {
                        dampedDragAnimation.value
                    } else {
                        target
                    }
                }
                val providedPosition =
                    liquidBottomBarFinitePosition(
                        position = currentSelectedPositionProvider.value?.invoke(),
                        tabsCount = safeTabsCount,
                    )
                val pagerDrivenPosition = providedPosition ?: currentExternalSelectionPosition.value
                if (
                    pagerDrivenPosition != null &&
                    dampedDragAnimation.pressProgress <= 0.001f
                ) {
                    pagerDrivenPosition
                } else {
                    dampedDragAnimation.value
                }
            }
        }

    LaunchedEffect(externalSelectionPosition, safeTabsCount) {
        val pagerDrivenPosition = externalSelectionPosition ?: return@LaunchedEffect
        if (localDragSettledIndex >= 0) return@LaunchedEffect
        dampedDragAnimation.snapToValue(
            value = pagerDrivenPosition,
            updateVelocity = false,
        )
    }
    LaunchedEffect(selectedIndex, safeTabsCount) {
        val index = selectedIndex.fastCoerceIn(0, safeTabsCount - 1)
        if (localDragSettledIndex >= 0 && localDragSettledIndex != index) {
            localDragSettledIndex = -1
        }
        currentIndex = index
        if (selectedPositionProvider != null && localDragSettledIndex != index) {
            dampedDragAnimation.snapToValue(
                value = index.toFloat(),
                updateVelocity = false,
            )
        }
    }
    LaunchedEffect(localDragSettledIndex, safeTabsCount, selectedPositionProvider, externalSelectionPosition) {
        val index = localDragSettledIndex
        if (index < 0) return@LaunchedEffect
        val target = index.fastCoerceIn(0, safeTabsCount - 1).toFloat()
        val hasExternalPosition = selectedPositionProvider != null || externalSelectionPosition != null
        if (!hasExternalPosition) {
            localDragSettledIndex = -1
            return@LaunchedEffect
        }
        snapshotFlow {
            liquidBottomBarFinitePosition(
                position = currentSelectedPositionProvider.value?.invoke(),
                tabsCount = safeTabsCount,
            )
                ?: currentExternalSelectionPosition.value
        }.filter { position ->
            position != null && abs(position - target) <= 0.01f
        }.first()
        if (localDragSettledIndex == index) {
            localDragSettledIndex = -1
        }
    }

    LaunchedEffect(dampedDragAnimation, transitionAnimationsEnabled, safeTabsCount) {
        snapshotFlow { currentIndex }
            .drop(1)
            .collectLatest { index ->
                val target = index.fastCoerceIn(0, safeTabsCount - 1).toFloat()
                val localSettlingTarget = localDragSettledIndex
                if (localSettlingTarget == index) {
                    return@collectLatest
                }
                if (
                    abs(dampedDragAnimation.value - target) <= 0.001f &&
                    abs(dampedDragAnimation.targetValue - target) <= 0.001f
                ) {
                    return@collectLatest
                }
                if (transitionAnimationsEnabled) {
                    dampedDragAnimation.animateToValue(target)
                } else {
                    dampedDragAnimation.snapToValue(target)
                }
            }
    }
    LaunchedEffect(interactionEnabled, dampedDragAnimation) {
        if (!interactionEnabled) {
            pressedTabIndex = -1
            localDragSettledIndex = -1
            selectionGestureDragged = false
            dampedDragAnimation.snapToValue(currentIndex.toFloat())
            offsetAnimation.snapTo(0f)
        }
    }

    val pressProgressProvider =
        remember(dampedDragAnimation, effectiveLiquidEffectEnabled, interactionEnabled) {
            {
                if (effectiveLiquidEffectEnabled && interactionEnabled) {
                    dampedDragAnimation.pressProgress
                } else {
                    0f
                }
            }
        }
    val itemPressProgressState =
        appMotionFloatState(
            targetValue =
                if (interactionEnabled && pressedTabIndex >= 0 && effectiveLiquidEffectEnabled) {
                    1f
                } else {
                    0f
                },
            durationMillis = 120,
            label = "liquid_bottom_bar_item_press",
        )
    val itemPressProgressProvider = remember(itemPressProgressState) { { itemPressProgressState.value } }
    val itemDeformationProgressProvider =
        remember(
            itemPressProgressProvider,
            transitionAnimationsEnabled,
            effectiveLiquidEffectEnabled,
            interactionEnabled,
        ) {
            {
                if (
                    transitionAnimationsEnabled &&
                    effectiveLiquidEffectEnabled &&
                    interactionEnabled
                ) {
                    itemPressProgressProvider()
                } else {
                    0f
                }
            }
        }
    val combinedPressProgressProvider =
        remember(pressProgressProvider, itemPressProgressProvider) {
            { max(pressProgressProvider(), itemPressProgressProvider()) }
        }
    val combinedDeformationProgressProvider =
        remember(
            dampedDragAnimation,
            itemDeformationProgressProvider,
            transitionAnimationsEnabled,
            effectiveLiquidEffectEnabled,
            interactionEnabled,
        ) {
            {
                if (
                    transitionAnimationsEnabled &&
                    effectiveLiquidEffectEnabled &&
                    interactionEnabled
                ) {
                    max(dampedDragAnimation.deformationProgress, itemDeformationProgressProvider())
                } else {
                    0f
                }
            }
        }
    val interactionLensScale = glassEffectRuntime().interactionLensScale
    val material = liquidBottomBarMaterial(isInLightTheme)
    val effectBlurDp = UiPerformanceBudget.backdropBlur
    val useLightweightBackdrop = !effectiveLiquidEffectEnabled
    val selectionIndicatorColor = liquidBottomBarSelectionIndicatorColor(isLight = isInLightTheme)

    val selectionProgressValue =
        if (selectedPositionProvider != null || externalSelectionPosition != null) {
            selectedIndex.fastCoerceIn(0, safeTabsCount - 1).toFloat()
        } else {
            currentIndex.toFloat()
        }
    val selectionProgressProvider: (Int) -> Float =
        remember(selectionProgressValue) {
            { tabIndex ->
                (1f - abs(selectionProgressValue - tabIndex)).fastCoerceIn(0f, 1f)
            }
        }

    val interactiveHighlight =
        if (
            effectiveLiquidEffectEnabled
        ) {
            remember(
                animationScope,
                tabWidthPx,
                isLtr,
                displaySelectionValueProvider,
                panelOffsetProvider,
                transitionAnimationsEnabled,
            ) {
                InteractiveHighlight(
                    animationScope = animationScope,
                    animationsEnabled = transitionAnimationsEnabled,
                    position = { size, _ ->
                        val displayValue = displaySelectionValueProvider()
                        val x =
                            if (isLtr) {
                                (displayValue + 0.5f) * tabWidthPx + panelOffsetProvider()
                            } else {
                                size.width - (displayValue + 0.5f) * tabWidthPx + panelOffsetProvider()
                            }
                        Offset(
                            x,
                            size.height / 2f,
                        )
                    },
                    highlightColor = Color.White,
                    highlightStrength = if (isInLightTheme) 0.60f else 0.90f,
                    highlightRadiusScale = if (isInLightTheme) 0.90f else 1.08f,
                )
            }
        } else {
            null
        }
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    CompositionLocalProvider(
        LocalLiquidGlassBottomBarTabScale provides {
            if (effectiveLiquidEffectEnabled) {
                lerp(1f, 1.2f, combinedDeformationProgressProvider())
            } else {
                1f
            }
        },
        LocalLiquidGlassBottomBarSelectionProgress provides selectionProgressProvider,
        LocalLiquidGlassBottomBarContentColor provides { palette.inactiveContentColor },
        LocalLiquidGlassBottomBarItemInteractive provides interactionEnabled,
        LocalLiquidGlassBottomBarItemPressHandler provides { index, isPressed ->
            if (interactionEnabled) {
                when {
                    isPressed -> pressedTabIndex = index
                    pressedTabIndex == index -> pressedTabIndex = -1
                }
            }
        },
    ) {
        Box(
            modifier =
                modifier
                    .then(
                        if (expandToMaxWidth) Modifier.fillMaxWidth() else Modifier.width(IntrinsicSize.Min),
                    ).then(
                        if (interactionEnabled) {
                            Modifier.selectableGroup()
                        } else {
                            Modifier.clearAndSetSemantics {}
                        },
                    ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier =
                    Modifier
                        .onGloballyPositioned { coords ->
                            val measuredTotalWidthPx = coords.size.width.toFloat()
                            if (abs(totalWidthPx - measuredTotalWidthPx) > 0.5f) {
                                totalWidthPx = measuredTotalWidthPx
                            }
                            val contentWidthPx =
                                measuredTotalWidthPx -
                                    horizontalPaddingPx * 2f
                            val measuredTabWidthPx = (contentWidthPx / safeTabsCount).coerceAtLeast(0f)
                            if (abs(tabWidthPx - measuredTabWidthPx) > 0.5f) {
                                tabWidthPx = measuredTabWidthPx
                            }
                        }.graphicsLayer {
                            translationX = panelOffsetProvider()
                            val deformationProgress = combinedDeformationProgressProvider()
                            translationY = snapChromeTranslationPx(-pressLiftPx * deformationProgress)
                        }.then(
                            if (useLightweightBackdrop) {
                                Modifier
                                    .appSquircleBackground(palette.baseFillColor, 999.dp)
                            } else {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { ContinuousCapsule },
                                    effects = {
                                        if (effectiveLiquidEffectEnabled) {
                                            vibrancy()
                                            blur(effectBlurDp.toPx())
                                            safeLiquidLens(
                                                material.lensHeight.toPx(),
                                                material.lensAmount.toPx(),
                                            )
                                        }
                                    },
                                    highlight = {
                                        Highlight.Default.copy(
                                            alpha =
                                                if (effectiveLiquidEffectEnabled) {
                                                    material.highlightAlpha
                                                } else {
                                                    0f
                                                },
                                        )
                                    },
                                    shadow = {
                                        Shadow.Default.copy(
                                            color = Color.Black.copy(if (isInLightTheme) 0.10f else 0.20f),
                                        )
                                    },
                                    layerBlock = {
                                        val deformationProgress = combinedDeformationProgressProvider()
                                        scaleX = lerp(1f, 1.006f, deformationProgress)
                                        scaleY = lerp(1f, 0.996f, deformationProgress)
                                    },
                                    onDrawSurface = { drawRect(palette.baseFillColor) },
                                )
                            },
                        ).then(if (interactiveHighlight != null) interactiveHighlight.modifier else Modifier)
                        .height(AppChromeTokens.floatingBottomBarOuterHeight)
                        .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )

            CompositionLocalProvider(
                LocalLiquidGlassBottomBarContentColor provides { palette.activeContentColor },
                LocalLiquidGlassBottomBarItemInteractive provides false,
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .then(
                            if (useLightweightBackdrop) {
                                Modifier
                            } else {
                                Modifier.layerBackdrop(
                                    tabsBackdrop,
                                )
                            },
                        ).graphicsLayer {
                            val combinedPressProgress = combinedDeformationProgressProvider()
                            translationX = panelOffsetProvider()
                            translationY = snapChromeTranslationPx(-pressLiftPx * combinedPressProgress)
                            scaleX = lerp(1f, 1.006f, combinedPressProgress)
                            scaleY = lerp(1f, 0.996f, combinedPressProgress)
                        }.then(
                            if (useLightweightBackdrop) {
                                Modifier
                            } else {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { ContinuousCapsule },
                                    effects = {
                                        if (effectiveLiquidEffectEnabled) {
                                            val progress = combinedPressProgressProvider()
                                            vibrancy()
                                            blur(effectBlurDp.toPx())
                                            safeLiquidLens(
                                                material.lensHeight.toPx() * progress,
                                                material.lensAmount.toPx() * progress,
                                            )
                                        }
                                    },
                                    highlight = {
                                        Highlight.Default.copy(
                                            alpha =
                                                if (effectiveLiquidEffectEnabled) {
                                                    material.highlightAlpha * combinedPressProgressProvider()
                                                } else {
                                                    0f
                                                },
                                        )
                                    },
                                    onDrawSurface = { drawRect(palette.baseFillColor) },
                                )
                            },
                        ).height(AppChromeTokens.floatingBottomBarInnerHeight)
                        .padding(horizontal = horizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }

            if (tabWidthPx > 0f) {
                Box(
                    Modifier
                        .padding(horizontal = horizontalPadding)
                        .graphicsLayer {
                            val combinedPressProgress = combinedDeformationProgressProvider()
                            val contentWidth =
                                totalWidthPx -
                                    horizontalPaddingPx * 2f
                            val singleTabWidth = contentWidth / safeTabsCount
                            val progressOffset = displaySelectionValueProvider() * singleTabWidth
                            val panelOffset = panelOffsetProvider()
                            translationX =
                                snapChromeTranslationPx(
                                    if (isLtr) {
                                        progressOffset + panelOffset
                                    } else {
                                        -progressOffset + panelOffset
                                    },
                                )
                            translationY = snapChromeTranslationPx(-pressLiftPx * combinedPressProgress)
                            scaleX = lerp(1f, 1.006f, combinedPressProgress)
                            scaleY = lerp(1f, 0.996f, combinedPressProgress)
                        }.then(
                            if (interactionEnabled && interactiveHighlight != null) {
                                interactiveHighlight.gestureModifier
                            } else {
                                Modifier
                            },
                        ).then(if (interactionEnabled) dampedDragAnimation.modifier else Modifier)
                        .then(
                            if (useLightweightBackdrop) {
                                Modifier.appSquircleBackground(
                                    color = selectionIndicatorColor,
                                    cornerRadius = 999.dp,
                                )
                            } else {
                                Modifier.drawBackdrop(
                                    backdrop = combinedBackdrop,
                                    shape = { ContinuousCapsule },
                                    effects = {
                                        val progress =
                                            if (effectiveLiquidEffectEnabled) {
                                                combinedPressProgressProvider()
                                            } else {
                                                0f
                                            }
                                        if (progress > 0f) {
                                            safeLiquidLens(
                                                10f.dp.toPx() * progress * interactionLensScale,
                                                14f.dp.toPx() * progress * interactionLensScale,
                                                true,
                                            )
                                            // Radial refraction from center of indicator
                                            radialRefraction(
                                                centerX = size.width / 2f,
                                                centerY = size.height / 2f,
                                                radius = 14f.dp.toPx() * progress * interactionLensScale,
                                                strength = 6f * progress,
                                            )
                                        }
                                    },
                                    highlight = {
                                        Highlight.Default.copy(
                                            alpha = if (effectiveLiquidEffectEnabled) combinedPressProgressProvider() else 0f,
                                        )
                                    },
                                    shadow = {
                                        Shadow(alpha = if (effectiveLiquidEffectEnabled) combinedPressProgressProvider() else 0f)
                                    },
                                    innerShadow = {
                                        val progress = if (effectiveLiquidEffectEnabled) combinedPressProgressProvider() else 0f
                                        InnerShadow(
                                            radius = 8f.dp * progress,
                                            alpha = progress,
                                        )
                                    },
                                    layerBlock = {
                                        if (effectiveLiquidEffectEnabled) {
                                            val clickScale =
                                                lerp(
                                                    1f,
                                                    1.045f,
                                                    itemDeformationProgressProvider(),
                                                )
                                            scaleX = dampedDragAnimation.scaleX * clickScale
                                            scaleY = dampedDragAnimation.scaleY * clickScale
                                            val velocity = dampedDragAnimation.velocity / 10f
                                            scaleX /= 1f -
                                                (velocity * 0.75f).fastCoerceIn(
                                                    -0.2f,
                                                    0.2f,
                                                )
                                            scaleY *= 1f -
                                                (velocity * 0.25f).fastCoerceIn(
                                                    -0.2f,
                                                    0.2f,
                                                )
                                        }
                                    },
                                    onDrawSurface = {
                                        val progress =
                                            if (effectiveLiquidEffectEnabled) combinedPressProgressProvider() else 0f
                                        drawRect(
                                            color = selectionIndicatorColor,
                                            alpha = 1f - progress,
                                        )
                                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                                    },
                                )
                            },
                        ).clearAndSetSemantics {}
                        .height(AppChromeTokens.floatingBottomBarInnerHeight)
                        .width(
                            with(density) {
                                ((totalWidthPx - (horizontalPadding * 2).toPx()) / safeTabsCount).toDp()
                            },
                        ),
                ) {
                    selectionOptics?.let { optics ->
                        Box(
                            Modifier
                                .matchParentSize()
                                .appSquircleBackground(optics.overlayColor, 999.dp),
                        )
                        Box(
                            Modifier
                                .matchParentSize()
                                .appSquircleBorder(
                                    width = optics.rimWidth,
                                    color = optics.rimColor,
                                    cornerRadius = 999.dp,
                                ),
                        )
                    }
                }
            }
        }
    }
}

internal fun liquidBottomBarFinitePosition(
    position: Float?,
    tabsCount: Int,
): Float? =
    position
        ?.takeIf(Float::isFinite)
        ?.fastCoerceIn(0f, (tabsCount.coerceAtLeast(1) - 1).toFloat())

private fun liquidBottomBarPanelOffset(
    rawOffsetPx: Float,
    totalWidthPx: Float,
    maxOffsetPx: Float,
): Float {
    if (totalWidthPx == 0f) return 0f
    val fraction = (rawOffsetPx / totalWidthPx).fastCoerceIn(-1f, 1f)
    return snapChromeTranslationPx(maxOffsetPx * fraction.sign * EaseOut.transform(abs(fraction)))
}

internal fun liquidBottomBarResolvedEnabledIndex(
    targetIndex: Int,
    currentIndex: Int,
    tabsCount: Int,
    isTabEnabled: (Int) -> Boolean,
): Int {
    val safeCount = tabsCount.coerceAtLeast(1)
    val target = targetIndex.fastCoerceIn(0, safeCount - 1)
    if (isTabEnabled(target)) return target

    val current = currentIndex.fastCoerceIn(0, safeCount - 1)
    val preferredDirection = if (target >= current) 1 else -1
    for (distance in 1 until safeCount) {
        val preferred = target + preferredDirection * distance
        if (preferred in 0 until safeCount && isTabEnabled(preferred)) {
            return preferred
        }
        val opposite = target - preferredDirection * distance
        if (opposite in 0 until safeCount && isTabEnabled(opposite)) {
            return opposite
        }
    }
    return current
}

@Composable
private fun rememberLiquidBottomBarPalette(
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
    val lensHeight: androidx.compose.ui.unit.Dp,
    val lensAmount: androidx.compose.ui.unit.Dp,
)

@Stable
private class LiquidBottomBarPalette(
    val baseFillColor: Color,
    val inactiveContentColor: Color,
    val activeContentColor: Color,
)
