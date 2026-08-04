package os.kei.ui.page.main.host.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        viewModelScope.launch {
            try {
                repository.saveCurrentUrlAsync(normalizedUrl)
                val warmStart = repository.prepareNavigationWarmStart(normalizedUrl)
                val warmStartId = BaStudentGuideNavigationWarmStartStore.publish(warmStart)
                mutableEvents.emit(MainScreenGuideNavigationEvent.OpenStudentGuide(warmStartId))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                val warmStartId =
                    BaStudentGuideNavigationWarmStartStore.publish(
                        BaStudentGuideNavigationWarmStart(sourceUrl = normalizedUrl),
                    )
                mutableEvents.emit(MainScreenGuideNavigationEvent.OpenStudentGuide(warmStartId))
            }
        }
    }
}
