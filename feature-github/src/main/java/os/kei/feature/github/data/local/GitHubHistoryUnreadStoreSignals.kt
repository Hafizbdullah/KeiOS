package os.kei.feature.github.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object GitHubHistoryUnreadStoreSignals {
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    fun notifyChanged(atMillis: Long = System.currentTimeMillis()) {
        _version.update { previous ->
            atMillis.coerceAtLeast(previous + 1L)
        }
    }
}
