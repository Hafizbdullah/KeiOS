package os.kei.ui.page.main.host.main

import android.os.Trace
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import os.kei.ui.page.main.student.page.state.BaStudentGuideNavigationWarmStart
import os.kei.ui.page.main.student.page.state.BaStudentGuideNavigationWarmStartStore
import os.kei.ui.page.main.student.page.state.BaStudentGuideRepository

internal sealed interface MainScreenGuideNavigationEvent {
    data class OpenStudentGuide(
        val warmStartId: Long,
    ) : MainScreenGuideNavigationEvent
}

internal class MainScreenGuideNavigationViewModel : ViewModel() {
    private val repository = BaStudentGuideRepository()
    private val mutableEvents =
        MutableSharedFlow<MainScreenGuideNavigationEvent>(
            replay = 0,
            extraBufferCapacity = 1,
        )

    val events: SharedFlow<MainScreenGuideNavigationEvent> = mutableEvents.asSharedFlow()

    fun saveAndOpenCanonicalGuide(canonicalGuideUrl: String) {
        val normalizedUrl = canonicalGuideUrl.trim()
        if (normalizedUrl.isBlank()) return
        val cookie = nextTraceCookie.getAndIncrement()
        viewModelScope.launch {
            // Async sections, not begin/endSection: the warm-start snapshot hops to an IO
            // dispatcher, and synchronous trace sections must open and close on one thread.
            beginAsyncTrace(TRACE_CLICK_TO_OPEN, cookie)
            val warmStartId = BaStudentGuideNavigationWarmStartStore.reserve()
            // Prepare concurrently rather than ahead of navigation. Warm, the snapshot lands in
            // 1-2 ms, comfortably before the destination's first composition one frame (8.33 ms)
            // later, so the warm-start benefit survives. Cold it takes 34-46 ms and simply loses
            // the race, leaving the destination on its normal load path -- no worse than before
            // warm-start existed, instead of delaying navigation by 4-5.5 frame budgets.
            launch {
                beginAsyncTrace(TRACE_WARM_START_PREPARE, cookie)
                val warmStart =
                    try {
                        repository.prepareNavigationWarmStart(normalizedUrl)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Throwable) {
                        BaStudentGuideNavigationWarmStart(sourceUrl = normalizedUrl)
                    } finally {
                        endAsyncTrace(TRACE_WARM_START_PREPARE, cookie)
                    }
                BaStudentGuideNavigationWarmStartStore.fulfil(warmStartId, warmStart)
            }
            try {
                repository.saveCurrentUrlAsync(normalizedUrl)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // Persisting the URL is the recovery path, not the navigation path.
            }
            mutableEvents.emit(MainScreenGuideNavigationEvent.OpenStudentGuide(warmStartId))
            endAsyncTrace(TRACE_CLICK_TO_OPEN, cookie)
        }
    }

    private companion object {
        /**
         * Click to navigation event. The plan's retention rule for the warm-start work is that this
         * must not grow by more than one 120 Hz frame (~8.33 ms) versus the pre-warm-start build.
         */
        const val TRACE_CLICK_TO_OPEN = "keios.ba.guide_nav_click_to_open"

        /** The snapshot read that the warm-start commits inserted ahead of the navigation event. */
        const val TRACE_WARM_START_PREPARE = "keios.ba.guide_warm_start_prepare"


        val nextTraceCookie = AtomicInteger(1)
    }
}

private fun beginAsyncTrace(
    name: String,
    cookie: Int,
) {
    if (Trace.isEnabled()) Trace.beginAsyncSection(name, cookie)
}

private fun endAsyncTrace(
    name: String,
    cookie: Int,
) {
    if (Trace.isEnabled()) Trace.endAsyncSection(name, cookie)
}
