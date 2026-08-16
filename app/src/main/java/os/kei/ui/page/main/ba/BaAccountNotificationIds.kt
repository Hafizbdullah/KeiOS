package os.kei.ui.page.main.ba

import os.kei.mcp.notification.McpNotificationHelper
import os.kei.ui.page.main.ba.support.BA_CRAFT_SLOT_COUNT
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.BaCraftFunction

internal enum class BaAccountNotificationKind(
    val legacyId: Int,
    val offset: Int,
) {
    Ap(
        legacyId = McpNotificationHelper.BA_AP_NOTIFICATION_ID,
        offset = 0,
    ),
    CafeAp(
        legacyId = McpNotificationHelper.BA_CAFE_AP_NOTIFICATION_ID,
        offset = 1,
    ),
    CafeVisit(
        legacyId = McpNotificationHelper.BA_CAFE_VISIT_NOTIFICATION_ID,
        offset = 2,
    ),
    ArenaRefresh(
        legacyId = McpNotificationHelper.BA_ARENA_REFRESH_NOTIFICATION_ID,
        offset = 3,
    );

    fun notificationId(accountId: BaAccountId): Int =
        BaAccountNotificationIds.notificationId(
            accountId = accountId,
            kind = this,
        )
}

internal object BaAccountNotificationIds {
    private const val ACCOUNT_NOTIFICATION_BASE_ID = 43_000
    private const val ACCOUNT_NOTIFICATION_BUCKET_COUNT = 100_000
    private const val KIND_STRIDE = 4

    fun notificationId(
        accountId: BaAccountId,
        kind: BaAccountNotificationKind,
    ): Int {
        val bucket =
            Math.floorMod(
                accountId.value.hashCode(),
                ACCOUNT_NOTIFICATION_BUCKET_COUNT,
            )
        return ACCOUNT_NOTIFICATION_BASE_ID + bucket * KIND_STRIDE + kind.offset
    }
}

/**
 * Notification ids for the six Craft Chamber slots of one account.
 *
 * Deliberately a separate allocator rather than six more [BaAccountNotificationKind] entries. Ids there
 * are `base + bucket * KIND_STRIDE + offset`, so raising `KIND_STRIDE` from 4 would move **every**
 * already-posted notification of every account, orphaning whatever is currently in the shade.
 *
 * The base also has to clear that range: with a bucket count of 100_000 and a stride of 4, account ids
 * span 43_000 .. 43_000 + 99_999 * 4 + 3 = 442_999, so anything below ~443k would collide. Starting at
 * 500_000 with a stride of 6 puts craft ids at 500_000 .. 1_099_999 — well inside Int.
 */
internal object BaCraftNotificationIds {
    private const val CRAFT_NOTIFICATION_BASE_ID = 500_000
    private const val CRAFT_NOTIFICATION_BUCKET_COUNT = 100_000
    private const val CRAFT_STRIDE = 2 * BA_CRAFT_SLOT_COUNT

    fun notificationId(
        accountId: BaAccountId,
        function: BaCraftFunction,
        slotIndex: Int,
    ): Int {
        val bucket =
            Math.floorMod(
                accountId.value.hashCode(),
                CRAFT_NOTIFICATION_BUCKET_COUNT,
            )
        val offset =
            function.ordinal * BA_CRAFT_SLOT_COUNT + slotIndex.coerceIn(0, BA_CRAFT_SLOT_COUNT - 1)
        return CRAFT_NOTIFICATION_BASE_ID + bucket * CRAFT_STRIDE + offset
    }
}
