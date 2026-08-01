package os.kei.ui.perf

import android.os.Trace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.metrics.performance.PerformanceMetricsState

@Composable
fun ReportPagerPerformanceState(
    scope: String,
    currentPage: String,
    targetPage: String,
    scrolling: Boolean,
    currentPageIndex: Int = -1,
    targetPageIndex: Int = -1,
    settledPageIndex: Int = -1,
    programmaticNavigation: Boolean = false,
    navigationActive: Boolean = false,
    pageBackdropProducerCount: Int = -1,
    pageFullBackdropEffectCount: Int = -1,
) {
    val view = LocalView.current
    val holder = remember(view) { PerformanceMetricsState.getHolderForHierarchy(view) }
    val metricsState = holder.state
    val currentKey = "$scope.current"
    val targetKey = "$scope.target"
    val scrollingKey = "$scope.scrolling"

    LaunchedEffect(
        metricsState,
        currentPage,
        targetPage,
        scrolling,
        currentPageIndex,
        targetPageIndex,
        settledPageIndex,
        programmaticNavigation,
        navigationActive,
        pageBackdropProducerCount,
        pageFullBackdropEffectCount,
    ) {
        metricsState?.let { state ->
            state.putState(currentKey, currentPage)
            state.putState(targetKey, targetPage)
            state.putState(scrollingKey, if (scrolling) "1" else "0")
        }
        if (Trace.isEnabled() && currentPageIndex >= 0) {
            Trace.setCounter("keios.$scope.current_page", currentPageIndex.toLong())
            Trace.setCounter("keios.$scope.target_page", targetPageIndex.toLong())
            Trace.setCounter("keios.$scope.settled_page", settledPageIndex.toLong())
            Trace.setCounter("keios.$scope.scrolling", scrolling.asTraceCounter())
            Trace.setCounter("keios.$scope.programmatic_navigation", programmaticNavigation.asTraceCounter())
            Trace.setCounter("keios.$scope.navigation_active", navigationActive.asTraceCounter())
            if (pageBackdropProducerCount >= 0) {
                Trace.setCounter("keios.$scope.page_backdrop_producers", pageBackdropProducerCount.toLong())
            }
            if (pageFullBackdropEffectCount >= 0) {
                Trace.setCounter("keios.$scope.page_full_backdrop_effects", pageFullBackdropEffectCount.toLong())
            }
        }
    }

    DisposableEffect(metricsState, currentKey, targetKey, scrollingKey) {
        onDispose {
            metricsState?.removeState(currentKey)
            metricsState?.removeState(targetKey)
            metricsState?.removeState(scrollingKey)
        }
    }
}

private fun Boolean.asTraceCounter(): Long = if (this) 1L else 0L
