package os.kei.feature.notification

import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.ui.page.main.ba.BaAccountNotificationKind
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.BaApReminderKind

internal data class BaApMarkReadTarget(
    val accountId: BaAccountId,
    val kind: BaApReminderKind,
)

internal fun resolveBaApMarkReadTarget(
    notificationId: Int,
    serverName: String?,
    rawAccountId: String?,
    knownAccountIds: Set<BaAccountId>,
): BaApMarkReadTarget? {
    val accountId = BaAccountId(rawAccountId?.trim().orEmpty())
    if (accountId.value.isBlank() || accountId !in knownAccountIds) return null
    val kind =
        when (serverName?.trim()) {
            LiveNotificationPayload.BA_AP_SERVER_NAME -> BaApReminderKind.Ap
            LiveNotificationPayload.BA_CAFE_AP_SERVER_NAME -> BaApReminderKind.CafeAp
            else -> return null
        }
    val notificationKind =
        when (kind) {
            BaApReminderKind.Ap -> BaAccountNotificationKind.Ap
            BaApReminderKind.CafeAp -> BaAccountNotificationKind.CafeAp
        }
    if (notificationKind.notificationId(accountId) != notificationId) return null
    return BaApMarkReadTarget(accountId, kind)
}
