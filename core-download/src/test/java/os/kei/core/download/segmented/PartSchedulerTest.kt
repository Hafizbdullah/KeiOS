package os.kei.core.download.segmented

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

        val first = assertNotNull(scheduler.nextPart(workerId = 0))
        assertEquals(DownloadPart(start = 0, endInclusive = 9), first.part)
        scheduler.finish(workerId = 0, active = first)

        val second = assertNotNull(scheduler.nextPart(workerId = 1))
        assertEquals(DownloadPart(start = 190, endInclusive = 199), second.part)
        scheduler.finish(workerId = 1, active = second)

        val third = assertNotNull(scheduler.nextPart(workerId = 0))
        assertEquals(DownloadPart(start = 10, endInclusive = 19), third.part)
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

        val first = assertNotNull(scheduler.nextPart(workerId = 0))
        assertEquals(DownloadPart(start = 0, endInclusive = 4), first.part)
        scheduler.finish(workerId = 0, active = first)

        val second = assertNotNull(scheduler.nextPart(workerId = 1))
        assertEquals(DownloadPart(start = 16, endInclusive = 19), second.part)
        scheduler.finish(workerId = 1, active = second)

        val third = assertNotNull(scheduler.nextPart(workerId = 0))
        assertEquals(DownloadPart(start = 5, endInclusive = 7), third.part)
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
        assertEquals(DownloadPart(start = 0, endInclusive = 9), first.part)

        scheduler.finish(workerId = 0, active = first)
        scheduler.record(workerId = 0, bytes = first.part.length, elapsedMs = 100)

        assertEquals(
            DownloadPart(start = 600, endInclusive = 999),
            assertNotNull(scheduler.nextPart(workerId = 0)).part,
        )
    }

    @Test
    fun `idle worker waits while remaining bytes belong to active part`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 500,
            initialPartSizeBytes = 500,
            maxRetriesPerPart = 2,
            concurrency = 2,
            tuning = testTuning.copy(
                tailWindowInitialMultiplier = 0,
            ),
            nowMs = { 1_000L },
        )
        val active = assertNotNull(scheduler.nextPart(workerId = 0))
        active.advanceTo(100)

        assertNull(scheduler.nextPart(workerId = 1))
        assertEquals(499, active.currentEndInclusive())
        assertEquals(
            PartSchedulerStats(retryCount = 0, stealCount = 0, handoffCount = 0),
            scheduler.stats(),
        )
    }

    @Test
    fun `scheduler starts four workers and grows after a successful part`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 1_200,
            initialPartSizeBytes = 100,
            maxRetriesPerPart = 2,
            concurrency = 6,
            tuning = testTuning.copy(tailWindowInitialMultiplier = 0),
        )
        val active = (0 until 4).map { workerId ->
            assertNotNull(scheduler.nextPart(workerId))
        }

        assertNull(scheduler.nextPart(workerId = 4))

        val completed = active.first()
        scheduler.finish(workerId = 0, active = completed)
        scheduler.record(
            workerId = 0,
            bytes = completed.part.length,
            elapsedMs = 100,
        )

        assertNotNull(scheduler.nextPart(workerId = 4))
        Unit
    }

    private companion object {
        val testTuning = PartSchedulerTuning(
            minDynamicPartSizeBytes = 1,
            minTailPartSizeBytes = 1,
        )
    }
}
