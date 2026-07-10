package os.kei.ui.page.main.ba

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.BaApReminderKind
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BaApNotificationSyncCoordinatorTest {
    @Test
    fun `foreground persistent read suppresses regenerated AP`() {
        val plan =
            planBaApNotificationSync(
                request =
                    request(
                        currentDisplay = 121,
                        lastNotifiedLevel = 120,
                        keepReadUntilBelowThreshold = true,
                        suppressionAnchorAtMs = NOW_MS - 1_000L,
                    ),
                nowMs = NOW_MS,
            )

        assertFalse(plan.shouldSendThresholdNotification)
        assertFalse(plan.shouldRefreshActiveNotification)
    }

    @Test
    fun `foreground expired hourly read sends and advances anchor`() {
        val plan =
            planBaApNotificationSync(
                request =
                    request(
                        currentDisplay = 120,
                        lastNotifiedLevel = 120,
                        keepReadUntilBelowThreshold = false,
                        suppressionAnchorAtMs = NOW_MS - BA_AP_READ_REPEAT_INTERVAL_MS,
                    ),
                nowMs = NOW_MS,
            )

        assertTrue(plan.shouldSendThresholdNotification)
        assertTrue(plan.advanceSuppressionAnchorAfterDelivery)
    }

    @Test
    fun `foreground below threshold clears local read state`() {
        val plan =
            planBaApNotificationSync(
                request = request(currentDisplay = 119, suppressionAnchorAtMs = NOW_MS),
                nowMs = NOW_MS,
            )

        assertEquals(0L, plan.nextSuppressionAnchorAtMs)
        assertEquals(-1, plan.nextLastNotifiedLevel)
    }

    @Test
    fun `successful hourly delivery commits anchor before replacement state can run`() = runTest {
        val accountId = BaAccountId("cn-main")
        val office =
            BaOfficeController(
                BaPageSnapshot(
                    apLastNotifiedLevel = 120,
                    apSuppressionAnchorAtMs = NOW_MS - BA_AP_READ_REPEAT_INTERVAL_MS,
                ),
            )
        val request =
            request(
                keepReadUntilBelowThreshold = false,
                suppressionAnchorAtMs = office.apSuppressionAnchorAtMs,
            ).copy(accountId = accountId)
        val events = mutableListOf<String>()
        val anchorWriter = RecordingAnchorWriter(events)

        val result =
            BaApNotificationSyncCoordinator.sync(
                request = request,
                nowMs = NOW_MS,
                delivery = FixedDelivery(sent = true),
            )
        persistBaForegroundApSyncResult(
            request = request,
            result = result,
            office = office,
            anchorWriter = anchorWriter,
        ) { level ->
            events += "level"
            assertEquals(NOW_MS, office.apSuppressionAnchorAtMs)
            office.applyApLastNotifiedLevel(level)
            val replacement =
                planBaApNotificationSync(
                    request.copy(
                        lastNotifiedLevel = office.apLastNotifiedLevel,
                        suppressionAnchorAtMs = office.apSuppressionAnchorAtMs,
                    ),
                    nowMs = NOW_MS,
                )
            assertFalse(replacement.shouldSendThresholdNotification)
        }

        assertEquals(listOf("anchor", "level"), events)
        assertEquals(listOf(Triple(accountId, BaApReminderKind.Ap, NOW_MS)), anchorWriter.writes)
        assertEquals(NOW_MS, office.apSuppressionAnchorAtMs)
    }

    @Test
    fun `failed or timed out foreground delivery preserves anchor`() = runTest {
        val initialAnchor = NOW_MS - BA_AP_READ_REPEAT_INTERVAL_MS
        val request =
            request(
                keepReadUntilBelowThreshold = false,
                suppressionAnchorAtMs = initialAnchor,
            ).copy(accountId = BaAccountId("cn-main"))

        listOf(
            FixedDelivery(sent = false),
            DelayedDelivery,
        ).forEach { delivery ->
            val office = BaOfficeController(BaPageSnapshot(apSuppressionAnchorAtMs = initialAnchor))
            val anchorWriter = RecordingAnchorWriter(mutableListOf())
            val result =
                BaApNotificationSyncCoordinator.sync(
                    request = request,
                    nowMs = NOW_MS,
                    delivery = delivery,
                    timeoutMs = 1L,
                )

            assertNull(result.suppressionAnchorAtMs)
            persistBaForegroundApSyncResult(
                request = request,
                result = result,
                office = office,
                anchorWriter = anchorWriter,
            ) {}
            assertTrue(anchorWriter.writes.isEmpty())
            assertEquals(initialAnchor, office.apSuppressionAnchorAtMs)
        }
    }

    @Test
    fun `cancellation after delivery still commits anchor and syncs office state`() = runTest {
        val accountId = BaAccountId("cn-main")
        val office = BaOfficeController(BaPageSnapshot(apSuppressionAnchorAtMs = 1L))
        val writer = BlockingAnchorWriter()
        val request =
            request(
                keepReadUntilBelowThreshold = false,
                suppressionAnchorAtMs = NOW_MS - BA_AP_READ_REPEAT_INTERVAL_MS,
            ).copy(accountId = accountId)
        val result =
            BaApNotificationSyncCoordinator.sync(
                request = request,
                nowMs = NOW_MS,
                delivery = FixedDelivery(sent = true),
            )
        val levelWrites = mutableListOf<Int>()

        val job =
            launch {
                persistBaForegroundApSyncResult(
                    request = request,
                    result = result,
                    office = office,
                    anchorWriter = writer,
                ) { levelWrites += it }
            }
        writer.started.await()
        job.cancel()
        writer.release.complete(Unit)
        job.join()

        assertEquals(listOf(Triple(accountId, BaApReminderKind.Ap, NOW_MS)), writer.writes)
        assertEquals(NOW_MS, office.apSuppressionAnchorAtMs)
        assertEquals(listOf(120), levelWrites)
        assertFalse(
            planBaApNotificationSync(
                request.copy(
                    keepReadUntilBelowThreshold = true,
                    suppressionAnchorAtMs = office.apSuppressionAnchorAtMs,
                ),
                nowMs = NOW_MS,
            ).shouldSendThresholdNotification,
        )
    }

    private fun request(
        currentDisplay: Int = 120,
        lastNotifiedLevel: Int = 120,
        keepReadUntilBelowThreshold: Boolean = true,
        suppressionAnchorAtMs: Long = 0L,
    ): BaApNotificationSyncRequest =
        BaApNotificationSyncRequest(
            currentDisplay = currentDisplay,
            limitDisplay = 240,
            thresholdDisplay = 120,
            notifyEnabled = true,
            lastNotifiedLevel = lastNotifiedLevel,
            keepReadUntilBelowThreshold = keepReadUntilBelowThreshold,
            suppressionAnchorAtMs = suppressionAnchorAtMs,
        )

    private companion object {
        const val NOW_MS = 10_000_000L
    }
}

private class RecordingAnchorWriter(
    private val events: MutableList<String>,
) : BaApSuppressionAnchorWriter {
    val writes = mutableListOf<Triple<BaAccountId, BaApReminderKind, Long>>()

    override suspend fun save(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        anchorAtMs: Long,
    ) {
        events += "anchor"
        writes += Triple(accountId, kind, anchorAtMs)
    }
}

private class FixedDelivery(
    private val sent: Boolean,
) : BaApNotificationDelivery {
    override suspend fun sendThreshold(request: BaApNotificationSyncRequest): Boolean = sent

    override suspend fun refreshActive(request: BaApNotificationSyncRequest): Boolean = true
}

private object DelayedDelivery : BaApNotificationDelivery {
    override suspend fun sendThreshold(request: BaApNotificationSyncRequest): Boolean {
        delay(Long.MAX_VALUE)
        return true
    }

    override suspend fun refreshActive(request: BaApNotificationSyncRequest): Boolean = true
}

private class BlockingAnchorWriter : BaApSuppressionAnchorWriter {
    val started = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    val writes = mutableListOf<Triple<BaAccountId, BaApReminderKind, Long>>()

    override suspend fun save(
        accountId: BaAccountId,
        kind: BaApReminderKind,
        anchorAtMs: Long,
    ) {
        started.complete(Unit)
        release.await()
        writes += Triple(accountId, kind, anchorAtMs)
    }
}
