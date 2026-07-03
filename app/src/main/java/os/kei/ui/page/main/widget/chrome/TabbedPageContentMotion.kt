// Copyright 2026, KeiOS contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.chrome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration

@Immutable
internal data class TabbedPageContentSwitchState(
    val selectedPage: Int,
    val direction: Int,
)

@Composable
internal fun rememberTabbedPageContentSwitchState(
    selectedPage: Int,
): TabbedPageContentSwitchState {
    var previousPage by remember { mutableIntStateOf(selectedPage) }
    val direction = selectedPage.compareTo(previousPage).coerceIn(-1, 1)
    LaunchedEffect(selectedPage) {
        previousPage = selectedPage
    }
    return remember(selectedPage, direction) {
        TabbedPageContentSwitchState(
            selectedPage = selectedPage,
            direction = direction,
        )
    }
}

@Composable
internal fun LazyItemScope.tabbedPageContentItemModifier(
    switchState: TabbedPageContentSwitchState,
    itemIndex: Int = 0,
    label: String = "tabbed_page_content_item",
): Modifier {
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    val fadeOutSpec =
        if (animationsEnabled) {
            tween<Float>(
                durationMillis = resolvedMotionDuration(TabbedPageContentFadeOutMs, animationsEnabled),
                easing = FastOutSlowInEasing,
            )
        } else {
            null
        }
    val placementSpec =
        if (animationsEnabled) {
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold,
            )
        } else {
            null
        }
    return Modifier
        .animateItem(
            fadeInSpec = null,
            placementSpec = placementSpec,
            fadeOutSpec = fadeOutSpec,
        )
        .tabbedPageContentSwitchMotion(
            switchState = switchState,
            itemIndex = itemIndex,
            label = label,
        )
}

@Composable
private fun Modifier.tabbedPageContentSwitchMotion(
    switchState: TabbedPageContentSwitchState,
    itemIndex: Int,
    label: String,
): Modifier {
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    val density = LocalDensity.current
    val hasSwitchMotion = animationsEnabled && switchState.direction != 0
    val initialAlpha = if (hasSwitchMotion) TabbedPageContentStartAlpha else 1f
    val initialTranslationX =
        if (hasSwitchMotion) {
            with(density) {
                TabbedPageContentSlideDistance.toPx() * switchState.direction
            }
        } else {
            0f
        }
    val alpha =
        remember(switchState.selectedPage, label) {
            Animatable(initialAlpha)
        }
    val translationX =
        remember(switchState.selectedPage, label) {
            Animatable(initialTranslationX)
        }
    LaunchedEffect(switchState.selectedPage, animationsEnabled) {
        val direction = switchState.direction
        if (!animationsEnabled || direction == 0) {
            alpha.snapTo(1f)
            translationX.snapTo(0f)
            return@LaunchedEffect
        }

        val delayMs =
            (
                itemIndex.coerceIn(0, TabbedPageContentMaxStaggerItems) *
                    TabbedPageContentStaggerMs
            ).toLong()
        if (delayMs > 0L) {
            delay(delayMs)
        }
        coroutineScope {
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        tween(
                            durationMillis =
                                resolvedMotionDuration(
                                    TabbedPageContentFadeInMs,
                                    animationsEnabled,
                                ),
                            easing = FastOutSlowInEasing,
                        ),
                )
            }
            launch {
                translationX.animateTo(
                    targetValue = 0f,
                    animationSpec =
                        tween(
                            durationMillis =
                                resolvedMotionDuration(
                                    TabbedPageContentSlideInMs,
                                    animationsEnabled,
                                ),
                            easing = FastOutSlowInEasing,
                        ),
                )
            }
        }
    }
    return graphicsLayer {
        this.alpha = alpha.value
        this.translationX = translationX.value
    }
}

private val TabbedPageContentSlideDistance = 18.dp
private const val TabbedPageContentStartAlpha = 0.08f
private const val TabbedPageContentFadeInMs = 150
private const val TabbedPageContentSlideInMs = 220
private const val TabbedPageContentFadeOutMs = 110
private const val TabbedPageContentStaggerMs = 14
private const val TabbedPageContentMaxStaggerItems = 5
