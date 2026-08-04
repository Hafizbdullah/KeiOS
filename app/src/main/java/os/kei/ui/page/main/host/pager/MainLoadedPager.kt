package os.kei.ui.page.main.host.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun MainLoadedPager(
    state: MainLoadedPagerState,
    userScrollEnabled: Boolean,
    animationsEnabled: Boolean,
    additionalMotionInProgress: Boolean = false,
    modifier: Modifier = Modifier,
    pageContent: @Composable (pageIndex: Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val highFrameRateMotionActive =
        shouldPreferHighFrameRateForPagerMotion(
            pagerScrollInProgress = state.isScrollInProgress,
            additionalMotionInProgress = additionalMotionInProgress,
        )
    BoxWithConstraints(
        modifier =
            modifier
                .preferHighFrameRateForPagerMotion(highFrameRateMotionActive)
                .clipToBounds(),
    ) {
        val pageWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val visualPairVeilColor = MiuixTheme.colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    state = rememberDraggableState { deltaPx ->
                        state.dragBy(-deltaPx / pageWidthPx)
                    },
                    orientation = Orientation.Horizontal,
                    enabled = userScrollEnabled && state.pageCount > 1,
                    startDragImmediately = state.isScrollInProgress,
                    onDragStarted = { state.startUserScroll() },
                    onDragStopped = { velocityPxPerSecond ->
                        coroutineScope.launch {
                            state.settleAfterDrag(
                                velocityPagesPerSecond = -velocityPxPerSecond / pageWidthPx,
                                animationsEnabled = animationsEnabled
                            )
                        }
                    }
                )
        ) {
            repeat(state.pageCount) { pageIndex ->
                val flattenVisualPairPage = state.isVisualPairPage(pageIndex)
                val semanticsModifier = if (pageIndex == state.accessibilityPage) {
                    Modifier
                } else {
                    Modifier.clearAndSetSemantics { }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .loadedPagerPageMotion(
                            pageIndex = pageIndex,
                            state = state,
                            pageWidthPx = pageWidthPx,
                            flattenVisualPairPage = flattenVisualPairPage,
                            visualPairVeilColor = visualPairVeilColor,
                        )
                        .then(
                            if (flattenVisualPairPage) {
                                Modifier
                            } else {
                                Modifier.drawLoadedPagerPage(pageIndex, state)
                            },
                        )
                        .then(semanticsModifier)
                ) {
                    pageContent(pageIndex)
                }
            }
        }
    }
}

private fun Modifier.loadedPagerPageMotion(
    pageIndex: Int,
    state: MainLoadedPagerState,
    pageWidthPx: Float,
    flattenVisualPairPage: Boolean,
    visualPairVeilColor: androidx.compose.ui.graphics.Color,
): Modifier =
    if (flattenVisualPairPage) {
        drawWithContent {
            if (state.shouldDrawVisualPairPage(pageIndex)) {
                translate(
                    left = state.relativePositionFor(pageIndex) * pageWidthPx,
                ) {
                    this@drawWithContent.drawContent()
                }
                drawRect(
                    color = visualPairVeilColor,
                    alpha = state.visualPairVeilAlphaFor(pageIndex),
                )
            }
        }
    } else {
        offset {
            IntOffset(
                x = (state.relativePositionFor(pageIndex) * pageWidthPx).roundToInt(),
                y = 0,
            )
        }
    }

private fun Modifier.drawLoadedPagerPage(
    pageIndex: Int,
    state: MainLoadedPagerState
): Modifier = drawWithContent {
    val drawDistance = if (state.isScrollInProgress) {
        MainLoadedPagerActiveDrawDistance
    } else {
        MainLoadedPagerSettledDrawDistance
    }
    val relativePosition = state.relativePositionFor(pageIndex)
    if (abs(relativePosition) <= drawDistance) {
        val pageOffsetPx = relativePosition * size.width
        val clipLeft = max(0f, -pageOffsetPx)
        val clipRight = min(size.width, size.width - pageOffsetPx)
        if (clipRight > clipLeft) {
            clipRect(
                left = clipLeft,
                top = 0f,
                right = clipRight,
                bottom = size.height
            ) {
                this@drawWithContent.drawContent()
            }
        }
    }
}

private const val MainLoadedPagerSettledDrawDistance = 0.05f
private const val MainLoadedPagerActiveDrawDistance = 1.05f
