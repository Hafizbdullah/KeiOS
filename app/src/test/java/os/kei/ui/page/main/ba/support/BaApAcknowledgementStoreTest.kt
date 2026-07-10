package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaApAcknowledgementStoreTest {
    private val backing = InMemoryBaAccountKeyValueStore()
    private val store = BaApAcknowledgementStore(backing)

    @Test
    fun `anchors are isolated by account and AP kind`() {
        val first = BaAccountId("cn-main")
        val second = BaAccountId("jp-main")

        store.setSuppressionAnchor(first, BaApReminderKind.Ap, 1_000L)
        store.setSuppressionAnchor(first, BaApReminderKind.CafeAp, 2_000L)
        store.setSuppressionAnchor(second, BaApReminderKind.Ap, 3_000L)

        assertEquals(1_000L, store.loadSuppressionAnchor(first, BaApReminderKind.Ap))
        assertEquals(2_000L, store.loadSuppressionAnchor(first, BaApReminderKind.CafeAp))
        assertEquals(3_000L, store.loadSuppressionAnchor(second, BaApReminderKind.Ap))
    }

    @Test
    fun `clear account removes both AP anchors`() {
        val accountId = BaAccountId("cn-main")
        store.setSuppressionAnchor(accountId, BaApReminderKind.Ap, 1_000L)
        store.setSuppressionAnchor(accountId, BaApReminderKind.CafeAp, 2_000L)

        assertTrue(store.clearAccount(accountId))
        assertEquals(0L, store.loadSuppressionAnchor(accountId, BaApReminderKind.Ap))
        assertEquals(0L, store.loadSuppressionAnchor(accountId, BaApReminderKind.CafeAp))
    }

    @Test
    fun `negative anchors normalize to zero`() {
        val accountId = BaAccountId("cn-main")

        store.setSuppressionAnchor(accountId, BaApReminderKind.Ap, -10L)

        assertEquals(0L, store.loadSuppressionAnchor(accountId, BaApReminderKind.Ap))
    }
}
