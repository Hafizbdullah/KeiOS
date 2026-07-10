package os.kei.mcp.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import os.kei.core.log.AppLogger

private const val DELIVERY_COMMIT_ATTEMPT_TIMEOUT_MS = 2_000L

data class McpStandaloneEventRequest(
    val serverName: String,
    val running: Boolean,
    val port: Int,
    val path: String,
    val clients: Int,
    val ongoing: Boolean = running,
    val onlyAlertOnce: Boolean = false,
    val primaryActionLabel: String? = null,
    val secondaryActionLabel: String? = null,
    val showSecondaryActionWhenStopped: Boolean = false,
    val outerGlow: Boolean = true,
    val overrideTitle: String? = null,
    val overrideContent: String? = null,
    val overrideOnlineText: String? = null,
    val overrideShortText: String? = null,
    val overrideProgressPercent: Int? = null,
    val overrideAccentColor: String? = null,
    val deadlineAtMs: Long? = null,
    val miFocusOrderId: String? = null,
    val targetBaAccountId: String? = null,
    val targetRoute: String? = null,
)

class McpNotificationDeliveryCommitException(
    cause: Throwable,
) : IllegalStateException("Notification delivery commit failed", cause)

internal data class PreparedStandaloneEvent(
    val notification: Notification,
    val snapshot: McpNotificationSnapshot,
    val useXiaomiMagic: Boolean,
    val alreadyActive: Boolean,
)

internal suspend fun dispatchStandaloneEventAwaitingDelivery(
    context: Context,
    notificationId: Int,
    notification: Notification,
    useXiaomiMagic: Boolean,
    onDelivered: suspend () -> Unit = {},
): Boolean {
    val commitDelivery: suspend () -> Unit = {
        runStandaloneEventDeliveryCommit {
            McpNotificationActiveStateCache.markActive(notificationId, active = true)
            onDelivered()
        }
    }
    val dispatched =
        if (useXiaomiMagic) {
            McpXiaomiMagicDispatcher.notify(
                context = context,
                notificationId = notificationId,
                notification = notification,
                onDelivered = commitDelivery,
            )
        } else {
            val notificationManager = NotificationManagerCompat.from(context)
            if (McpXiaomiMagicDispatcher.canUseCommand()) {
                McpNotificationHelper.restoreXiaomiNetworkIfNeeded(context)
            }
            currentCoroutineContext().ensureActive()
            withContext(NonCancellable) {
                val delivered =
                    McpNotificationHelper.notifySafely(
                        context,
                        notificationManager,
                        notificationId,
                        notification,
                    )
                if (delivered) {
                    commitDelivery()
                }
                delivered
            }
        }
    AppLogger.i("McpStandaloneEvent") {
        "awaited dispatch result=$dispatched id=$notificationId xiaomiMagic=$useXiaomiMagic"
    }
    return dispatched
}

internal suspend fun runStandaloneEventDeliveryCommit(block: suspend () -> Unit) {
    var lastFailure: Throwable? = null
    repeat(2) {
        try {
            withTimeout(DELIVERY_COMMIT_ATTEMPT_TIMEOUT_MS) { block() }
            return
        } catch (throwable: Throwable) {
            if (throwable is CancellationException && throwable !is TimeoutCancellationException) {
                throw throwable
            }
            lastFailure =
                if (throwable is McpNotificationDeliveryCommitException) {
                    throwable.cause ?: throwable
                } else {
                    throwable
                }
        }
    }
    throw McpNotificationDeliveryCommitException(requireNotNull(lastFailure))
}
