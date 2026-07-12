package os.kei.ui.page.main.widget.sheet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class LiquidSheetDismissCoordinator {
    private var generation = 0
    private var job: Job? = null

    var isInProgress by mutableStateOf(false)
        private set

    fun launch(
        coroutineScope: CoroutineScope,
        block: suspend () -> Unit,
    ): Boolean {
        if (isInProgress || job?.isActive == true) return false
        val currentGeneration = generation + 1
        generation = currentGeneration
        isInProgress = true
        job =
            coroutineScope.launch {
                try {
                    block()
                } finally {
                    if (generation == currentGeneration) {
                        job = null
                        isInProgress = false
                    }
                }
            }
        return true
    }

    fun cancel() {
        generation += 1
        job?.cancel()
        job = null
        isInProgress = false
    }
}
