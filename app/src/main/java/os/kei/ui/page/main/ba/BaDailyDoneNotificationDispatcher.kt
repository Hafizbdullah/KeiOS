package os.kei.ui.page.main.ba

import android.content.Context
import android.content.pm.PackageManager
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.ui.page.main.ba.support.BaAccountId

/**
 * Reports the result of a daily-done run that was triggered from a quick-settings tile or shortcut.
 *
 * A notification rather than a toast, because neither of the cheaper channels actually works from a tile:
 * Android 12 and up suppress toasts posted by a background app, and a tile click does not count as
 * foreground; and a tile added at the icon-only size renders neither its label nor its subtitle, so
 * writing the outcome there would be invisible too.
 *
 * One fixed id, deliberately not per-account: the message summarises the whole run, so a second run
 * should replace the first rather than stack.
 *
 * ## Why the Focus fields are spelled out here
 *
 * The three `miFocus*` values are what turn this into a Super Island card rather than a generic one.
 * Before they were supplied this call passed only `overrideContent`, so the island fell through to the
 * shared pipeline's server-status shaping and announced a finished daily-done run with MCP's
 * "server online" string. `readme/MI_FOCUS_NOTIFICATION_TEMPLATES.md` describes the split this follows:
 * `miFocusTitle` short, `miFocusSpecialTitle` the status tag, `miFocusContent` the body with the tag's
 * wording removed so the two do not read as a stutter, and `overrideTitle`/`overrideContent` left full
 * for the plain notification and Android Live Updates.
 *
 * The island's own terminal shaping — short word, completion green, no progress bar, not ongoing — lives
 * with the other per-feature presentations in `MiIslandNotificationBuilder`, keyed off
 * [LiveNotificationPayload.isBaDailyDoneServerName].
 */
internal object BaDailyDoneNotificationDispatcher {
    private const val TAG = "BaDailyNotify"

    /** Well clear of the per-account (43k..443k) and craft (500k..1.1M) id ranges. */
    private const val NOTIFICATION_ID = 1_200_000

    /**
     * Stable across runs, matching the single fixed [NOTIFICATION_ID].
     *
     * HyperOS keys an island by its order id: reusing one means a second run rewrites the card that is
     * already showing, which is the behaviour a "latest result" summary wants. A per-run id would leave
     * a queue of stale daily-done islands behind instead.
     */
    private const val MI_FOCUS_ORDER_ID = "ba-daily-done"

    fun send(
        context: Context,
        changedAccounts: Int,
        craftSlotsStarted: Int,
        targetAccountId: BaAccountId? = null,
    ): Boolean {
        val granted =
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) {
            AppLogger.w(TAG) { "skip daily-done notification: permission missing" }
            return false
        }
        // Zero values are omitted rather than printed as "0 craft slots": the doc's summary rule is
        // "范围 · 进度 · 非零结果", and a run that started no craft slots has nothing to say about them.
        val detail =
            if (craftSlotsStarted > 0) {
                context.getString(
                    R.string.ba_daily_done_toast_applied_format,
                    changedAccounts,
                    craftSlotsStarted,
                )
            } else {
                context.getString(
                    R.string.ba_daily_done_notification_content_accounts_only,
                    changedAccounts,
                )
            }
        val title = context.getString(R.string.ba_daily_done_notification_title)
        return runCatching {
            McpNotificationHelper.notifyStandaloneEvent(
                context = context,
                notificationId = NOTIFICATION_ID,
                serverName = LiveNotificationPayload.BA_DAILY_DONE_SERVER_NAME,
                // `running = true` marks this a real event rather than a stopped service; `ongoing = false`
                // keeps it dismissible, since the work it reports is already over.
                running = true,
                port = 0,
                path = detail,
                clients = 0,
                ongoing = false,
                overrideTitle = title,
                overrideContent = detail,
                overrideShortText = context.getString(R.string.ba_daily_done_notification_island_text),
                miFocusTitle = title,
                miFocusSpecialTitle =
                    context.getString(R.string.ba_daily_done_notification_special_title),
                miFocusContent = detail,
                miFocusOrderId = MI_FOCUS_ORDER_ID,
                // Only when the run touched exactly one account, so tapping the card lands on the account
                // it is talking about. An all-accounts run has no single destination and must not guess.
                targetBaAccountId = targetAccountId?.value,
            )
        }.onFailure { throwable ->
            AppLogger.e(TAG, "daily-done notification failed", throwable)
        }.getOrDefault(false)
    }
}
