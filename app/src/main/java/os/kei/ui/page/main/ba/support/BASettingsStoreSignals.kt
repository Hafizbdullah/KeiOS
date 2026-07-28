package os.kei.ui.page.main.ba.support

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal object BASettingsStoreSignals {
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    private val _homeOverviewVersion = MutableStateFlow(0L)
    val homeOverviewVersion: StateFlow<Long> = _homeOverviewVersion.asStateFlow()

    fun notifyChanged(
        atMillis: Long = System.currentTimeMillis(),
        notifyHomeOverview: Boolean = true,
    ) {
        _version.update { previous ->
            atMillis.coerceAtLeast(previous + 1L)
        }
        if (notifyHomeOverview) {
            _homeOverviewVersion.update { previous ->
                atMillis.coerceAtLeast(previous + 1L)
            }
        }
    }
}
