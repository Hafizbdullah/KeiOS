package os.kei.feature.notification

import org.junit.Test
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.ui.page.main.ba.BaAccountNotificationKind
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.BaApReminderKind
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BaApMarkReadTargetTest {
    @Test
    fun `known account and matching ordinary AP notification resolve to AP target`() {
        val accountId = BaAccountId("cn-main")

        val target =
            resolveBaApMarkReadTarget(
                notificationId = BaAccountNotificationKind.Ap.notificationId(accountId),
                serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
                rawAccountId = accountId.value,
                knownAccountIds = setOf(accountId),
            )

        assertEquals(accountId, target?.accountId)
        assertEquals(BaApReminderKind.Ap, target?.kind)
    }

    @Test
    fun `another account id and unsupported server name do not resolve`() {
        val accountId = BaAccountId("cn-main")
        val otherAccountId = BaAccountId("cn-alt")
        val notificationId = BaAccountNotificationKind.Ap.notificationId(accountId)

        assertNull(
            resolveBaApMarkReadTarget(
                notificationId = notificationId,
                serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
                rawAccountId = otherAccountId.value,
                knownAccountIds = setOf(accountId, otherAccountId),
            ),
        )
        assertNull(
            resolveBaApMarkReadTarget(
                notificationId = notificationId,
                serverName = "BlueArchive Arena Refresh",
                rawAccountId = accountId.value,
                knownAccountIds = setOf(accountId),
            ),
        )
    }
}
