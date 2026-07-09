package os.kei.core.download.segmented

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PartSchedulerTest {
    @Test
    fun `scheduler allocates head and tail parts on demand before tail window`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 200,
            initialPartSizeBytes = 10,
            maxRetriesPerPart = 2,
            concurrency = 2,
            tuning = testTuning,
        )

        assertEquals(DownloadPart(start = 0, endInclusive = 9), scheduler.nextPart(workerId = 0))
        assertEquals(DownloadPart(start = 190, endInclusive = 199), scheduler.nextPart(workerId = 1))
        assertEquals(DownloadPart(start = 10, endInclusive = 19), scheduler.nextPart(workerId = 0))
    }

    @Test
    fun `scheduler uses smaller parts inside tail window`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 20,
            initialPartSizeBytes = 10,
            maxRetriesPerPart = 2,
            concurrency = 2,
            tuning = testTuning.copy(tailPartsPerConnection = 2),
        )

        assertEquals(DownloadPart(start = 0, endInclusive = 4), scheduler.nextPart(workerId = 0))
        assertEquals(DownloadPart(start = 16, endInclusive = 19), scheduler.nextPart(workerId = 1))
        assertEquals(DownloadPart(start = 5, endInclusive = 7), scheduler.nextPart(workerId = 0))
    }

    @Test
    fun `scheduler grows next part size from worker throughput`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 1_000,
            initialPartSizeBytes = 10,
            maxRetriesPerPart = 2,
            concurrency = 1,
            tuning = testTuning.copy(
                maxDynamicPartSizeBytes = 400,
                partSizeTargetDurationMs = 10_000,
            ),
        )

        val first = assertNotNull(scheduler.nextPart(workerId = 0))
        assertEquals(DownloadPart(start = 0, endInclusive = 9), first)

        scheduler.record(workerId = 0, bytes = first.length, elapsedMs = 100)

        assertEquals(DownloadPart(start = 600, endInclusive = 999), scheduler.nextPart(workerId = 0))
    }

    @Test
    fun `idle worker splits remaining bytes from active part`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 500,
            initialPartSizeBytes = 500,
            maxRetriesPerPart = 2,
            concurrency = 2,
            tuning = testTuning.copy(
                minStealAgeMs = 0,
                minStealPartSizeBytes = 10,
                tailWindowInitialMultiplier = 0,
            ),
            nowMs = { 1_000L },
        )
        val part = assertNotNull(scheduler.nextPart(workerId = 0))
        val active = scheduler.activate(workerId = 0, part = part)
        active.advanceTo(100)

        assertEquals(DownloadPart(start = 300, endInclusive = 499, retryCount = 1), scheduler.nextPart(workerId = 1))
        assertEquals(299, active.currentEndInclusive())
    }

    @Test
    fun `idle worker cancels active attempt when handing off remaining bytes`() = runBlocking {
        val cancelled = AtomicBoolean(false)
        val scheduler = PartScheduler(
            totalBytes = 100,
            initialPartSizeBytes = 100,
            maxRetriesPerPart = 2,
            concurrency = 2,
            tuning = testTuning.copy(
                minStealAgeMs = 0,
                minStealPartSizeBytes = 64,
                tailWindowInitialMultiplier = 0,
            ),
            nowMs = { 1_000L },
        )
        val part = assertNotNull(scheduler.nextPart(workerId = 0))
        val active = scheduler.activate(workerId = 0, part = part)
        active.setCancelAttempt { cancelled.set(true) }
        active.advanceTo(10)

        assertEquals(DownloadPart(start = 10, endInclusive = 99, retryCount = 1), scheduler.nextPart(workerId = 1))
        assertEquals(9, active.currentEndInclusive())
        assertTrue(active.isComplete())
        assertTrue(cancelled.get())
    }

    private companion object {
        val testTuning = PartSchedulerTuning(
            minDynamicPartSizeBytes = 1,
            minTailPartSizeBytes = 1,
            minStealPartSizeBytes = 1,
            minStealAgeMs = 0,
        )
    }
}
