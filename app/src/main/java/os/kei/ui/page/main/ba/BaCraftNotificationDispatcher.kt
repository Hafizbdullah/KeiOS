package os.kei.ui.page.main.ba

import android.content.Context
import android.content.pm.PackageManager
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.BaCraftCompletion

/**
 * Posts one notification per completed Craft Chamber slot.
 *
 * One notification per slot rather than a single rolled-up one, because the six slots finish at
 * unrelated times and each id is stable ([BaCraftNotificationIds]) — so a slot that is reloaded and
 * finishes again replaces its own notification instead of stacking a second copy.
 */
internal object BaCraftNotificationDispatcher {
    private const val TAG = "BaCraftNotify"

    fun send(
        context: Context,
        completion: BaCraftCompletion,
        accountDisplayName: String = "",
        accountId: BaAccountId? = null,
    ): Boolean {
        val notificationsGranted =
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        val notificationId =
            BaCraftNotificationIds.notificationId(
                accountId = accountId ?: BaAccountId(""),
                function = completion.function,
                slotIndex = completion.index,
            )
        if (!notificationsGranted) {
            AppLogger.w(TAG) {
                "skip craft notification: permission missing id=$notificationId " +
                    "function=${completion.function} slot=${completion.index}"
            }
            return false
        }

        val detailLine = buildCraftDetailLine(context = context, completion = completion)

        return runCatching {
            McpNotificationHelper.notifyStandaloneEvent(
                context = context,
                notificationId = notificationId,
                serverName = LiveNotificationPayload.BA_CRAFT_SERVER_NAME,
                running = true,
                port = 0,
                path = detailLine,
                clients = 0,
                ongoing = false,
                overrideContent =
                    baAccountNotificationContent(
                        context = context,
                        accountDisplayName = accountDisplayName,
                        content = detailLine,
                    ),
                targetBaAccountId = accountId?.value,
            )
        }.onSuccess { sent ->
            AppLogger.i(TAG) {
                "send result=$sent id=$notificationId function=${completion.function} " +
                    "slot=${completion.index} end=${completion.endAtMs} " +
                    "account=${accountId?.value.orEmpty()}"
            }
        }.onFailure { throwable ->
            AppLogger.e(TAG, "send failed id=$notificationId slot=${completion.index}", throwable)
        }.getOrDefault(false)
    }

    private fun buildCraftDetailLine(
        context: Context,
        completion: BaCraftCompletion,
    ): String {
        val functionLabel = context.getString(baCraftFunctionLabelRes(completion.function))
        // Slots are one-based in the game's own UI, so do not leak the zero-based index.
        val slotNumber = completion.index + 1
        val label = completion.slot.label.trim()
        return if (label.isBlank()) {
            context.getString(
                R.string.ba_craft_notification_content_detail,
                functionLabel,
                slotNumber,
            )
        } else {
            context.getString(
                R.string.ba_craft_notification_content_detail_labelled,
                functionLabel,
                slotNumber,
                label,
            )
        }
    }
}
