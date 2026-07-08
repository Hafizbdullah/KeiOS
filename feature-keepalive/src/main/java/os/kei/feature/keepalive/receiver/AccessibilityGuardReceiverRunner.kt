package os.kei.feature.keepalive.receiver

import android.content.BroadcastReceiver
import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger

internal object AccessibilityGuardReceiverRunner {
    private val scope = CoroutineScope(SupervisorJob() + AppDispatchers.osOperations)

    fun launch(
        receiver: BroadcastReceiver,
        context: Context,
        timeoutMs: Long,
        onTimeout: suspend (Context) -> Unit = {},
        block: suspend (Context) -> Unit,
    ) {
        val pendingResult = receiver.goAsync()
        val finished = AtomicBoolean(false)
        val appContext = context.applicationContext

        fun finishOnce() {
            if (finished.compareAndSet(false, true)) {
                pendingResult.finish()
            }
        }

        val worker =
            scope.launch {
                try {
                    block(appContext)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    AppLogger.w(TAG, "accessibility guard receiver failed", error)
                } finally {
                    finishOnce()
                }
            }
        val watchdog =
            scope.launch {
                delay(timeoutMs.coerceAtLeast(1L))
                if (!worker.isActive) return@launch
                finishOnce()
                worker.cancel(CancellationException("Accessibility guard receiver timed out after ${timeoutMs}ms"))
                runCatching { onTimeout(appContext) }
                    .onFailure { error -> AppLogger.w(TAG, "accessibility guard timeout recording failed", error) }
            }
        worker.invokeOnCompletion {
            watchdog.cancel()
            finishOnce()
        }
    }

    private const val TAG = "AccessibilityGuardReceiver"
}
