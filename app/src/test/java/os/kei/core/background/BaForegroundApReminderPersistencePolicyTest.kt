package os.kei.core.background

import org.junit.Test
import os.kei.ui.page.main.ba.support.BaApReminderKind
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaForegroundApReminderPersistencePolicyTest {
    @Test
    fun `disabled ordinary AP writes last notified reset and local anchor reset`() {
        val writes = BaForegroundApReminderPersistencePolicy.disabledWrites(BaApReminderKind.Ap)

        assertEquals(
            listOf(
                BaForegroundApReminderWrite(
                    kind = BaApReminderKind.Ap,
                    lastNotifiedLevel = -1,
                ),
                BaForegroundApReminderWrite(
                    kind = BaApReminderKind.Ap,
                    suppressionAnchorAtMs = 0L,
                ),
            ),
            writes,
        )
    }

    @Test
    fun `disabled cafe AP writes last notified reset and local anchor reset`() {
        val writes = BaForegroundApReminderPersistencePolicy.disabledWrites(BaApReminderKind.CafeAp)

        assertEquals(
            listOf(
                BaForegroundApReminderWrite(
                    kind = BaApReminderKind.CafeAp,
                    lastNotifiedLevel = -1,
                ),
                BaForegroundApReminderWrite(
                    kind = BaApReminderKind.CafeAp,
                    suppressionAnchorAtMs = 0L,
                ),
            ),
            writes,
        )
    }

    @Test
    fun `successful expired delivery advances anchor with last notified level`() {
        val writes =
            BaForegroundApReminderPersistencePolicy.deliveryWrites(
                kind = BaApReminderKind.Ap,
                sent = true,
                currentDisplay = 130,
                advanceSuppressionAnchorAfterDelivery = true,
                nowMs = NOW_MS,
            )

        assertEquals(
            listOf(
                BaForegroundApReminderWrite(
                    kind = BaApReminderKind.Ap,
                    lastNotifiedLevel = 130,
                ),
                BaForegroundApReminderWrite(
                    kind = BaApReminderKind.Ap,
                    suppressionAnchorAtMs = NOW_MS,
                ),
            ),
            writes,
        )
    }

    @Test
    fun `failed Xiaomi style ordinary and cafe delivery preserve anchors and last levels`() {
        listOf(BaApReminderKind.Ap, BaApReminderKind.CafeAp).forEach { kind ->
            var lastNotifiedLevel = 130
            var suppressionAnchorAtMs = NOW_MS - 3_600_000L
            val writes =
                BaForegroundApReminderPersistencePolicy.deliveryWrites(
                    kind = kind,
                    sent = false,
                    currentDisplay = 140,
                    advanceSuppressionAnchorAfterDelivery = true,
                    nowMs = NOW_MS,
                )

            writes.forEach { write ->
                write.lastNotifiedLevel?.let { lastNotifiedLevel = it }
                write.suppressionAnchorAtMs?.let { suppressionAnchorAtMs = it }
            }

            assertTrue(writes.isEmpty())
            assertEquals(130, lastNotifiedLevel)
            assertEquals(NOW_MS - 3_600_000L, suppressionAnchorAtMs)
        }
    }

    private companion object {
        private const val NOW_MS = 20_000_000L
    }
}
