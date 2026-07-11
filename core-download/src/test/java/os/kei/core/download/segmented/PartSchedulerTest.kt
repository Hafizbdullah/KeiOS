package os.kei.core.download.segmented

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartSchedulerTest {
    @Test
    fun `foreground boost admits eight workers at startup`() = runBlocking {
        val tuning = SegmentedDownloadSpeedProfile.ForegroundBoost.schedulerTuning()
        val scheduler = PartScheduler(
            totalBytes = 256L * 1024L * 1024L,
            initialPartSizeBytes = 4L * 1024L * 1024L,
            maxRetriesPerPart = 3,
            concurrency = 12,
            tuning = tuning,
        )

        val admitted = (0 until 8).map { workerId ->
            assertNotNull(scheduler.nextPart(workerId))
        }

        assertEquals(8, tuning.startupActiveConnections)
        assertNull(scheduler.nextPart(workerId = 8))
        admitted.forEachIndexed { workerId, active ->
            scheduler.finish(workerId, active)
        }
    }

    @Test
    fun `progressive growth waits for one successful wave`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 10_000,
            initialPartSizeBytes = 100,
            maxRetriesPerPart = 1,
            concurrency = 4,
            tuning = testTuning.copy(
                startupActiveConnections = 2,
                tailWindowInitialMultiplier = 0,
            ),
        )
        val first = assertNotNull(scheduler.nextPart(workerId = 0))
        val second = assertNotNull(scheduler.nextPart(workerId = 1))

        scheduler.finish(workerId = 0, active = first)
        scheduler.recordSuccess(workerId = 0, part = first.part, bytes = first.part.length, elapsedMs = 100)
        val replacement = assertNotNull(scheduler.nextPart(workerId = 2))
        assertNull(scheduler.nextPart(workerId = 3))

        scheduler.finish(workerId = 1, active = second)
        scheduler.recordSuccess(workerId = 1, part = second.part, bytes = second.part.length, elapsedMs = 100)
        assertNotNull(scheduler.nextPart(workerId = 3))
        assertNotNull(scheduler.nextPart(workerId = 0))
        assertNull(scheduler.nextPart(workerId = 1))
        assertEquals(3, scheduler.stats().peakActiveConnections)

        scheduler.finish(workerId = 2, active = replacement)
    }

    @Test
    fun `progressive growth can reach the configured hard limit`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 1_000_000,
            initialPartSizeBytes = 100,
            maxRetriesPerPart = 1,
            concurrency = 12,
            tuning = testTuning.copy(
                startupActiveConnections = 8,
                tailWindowInitialMultiplier = 0,
            ),
        )

        for (expectedActive in 8 until 12) {
            val wave = (0 until expectedActive).map { workerId ->
                assertNotNull(scheduler.nextPart(workerId))
            }
            assertNull(scheduler.nextPart(workerId = expectedActive))
            wave.forEachIndexed { workerId, active ->
                scheduler.finish(workerId, active)
                scheduler.recordSuccess(
                    workerId = workerId,
                    part = active.part,
                    bytes = active.part.length,
                    elapsedMs = 100,
                )
            }
        }

        val finalWave = (0 until 12).map { workerId ->
            assertNotNull(scheduler.nextPart(workerId))
        }
        assertNull(scheduler.nextPart(workerId = 0))
        assertEquals(12, scheduler.stats().peakActiveConnections)
        finalWave.forEachIndexed { workerId, active ->
            scheduler.finish(workerId, active)
        }
    }

    @Test
    fun `foreground boost keeps medium downloads inside the distributed tail window`() = runBlocking {
        val tuning = SegmentedDownloadSpeedProfile.ForegroundBoost.schedulerTuning()
        val partSizeBytes = 4L * 1024L * 1024L
        val scheduler = PartScheduler(
            totalBytes = 96L * 1024L * 1024L,
            initialPartSizeBytes = partSizeBytes,
            maxRetriesPerPart = 3,
            concurrency = 6,
            tuning = tuning,
        )

        val firstWave = (0 until 6).map { workerId ->
            assertNotNull(scheduler.nextPart(workerId))
        }
        firstWave.forEachIndexed { workerId, active ->
            scheduler.finish(workerId, active)
            scheduler.recordSuccess(
                workerId = workerId,
                part = active.part,
                bytes = active.part.length,
                elapsedMs = 2_000,
            )
        }

        val secondWave = (0 until 6).map { workerId ->
            assertNotNull(scheduler.nextPart(workerId))
        }

        assertEquals(32, tuning.tailWindowInitialMultiplier)
        assertTrue(secondWave.all { it.part.length <= partSizeBytes })
        secondWave.forEachIndexed { workerId, active ->
            scheduler.finish(workerId, active)
        }
    }

    @Test
    fun `balanced profile keeps common large APKs inside the distributed tail window`() = runBlocking {
        val tuning = SegmentedDownloadSpeedProfile.Balanced.schedulerTuning()
        val partSizeBytes = 8L * 1024L * 1024L
        val scheduler = PartScheduler(
            totalBytes = 120L * 1024L * 1024L,
            initialPartSizeBytes = partSizeBytes,
            maxRetriesPerPart = 3,
            concurrency = 4,
            tuning = tuning,
        )

        val firstWave = (0 until 4).map { workerId ->
            assertNotNull(scheduler.nextPart(workerId))
        }
        firstWave.forEachIndexed { workerId, active ->
            scheduler.finish(workerId, active)
            scheduler.recordSuccess(
                workerId = workerId,
                part = active.part,
                bytes = active.part.length,
                elapsedMs = 4_000,
            )
        }

        val secondWave = (0 until 4).map { workerId ->
            assertNotNull(scheduler.nextPart(workerId))
        }

        assertEquals(16, tuning.tailWindowInitialMultiplier)
        assertTrue(secondWave.all { it.part.length <= partSizeBytes })
        secondWave.forEachIndexed { workerId, active ->
            scheduler.finish(workerId, active)
        }
    }

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
        assertEquals(DownloadPart(start = 15, endInclusive = 19), second.part)
        scheduler.finish(workerId = 1, active = second)

        val third = assertNotNull(scheduler.nextPart(workerId = 0))
        assertEquals(DownloadPart(start = 5, endInclusive = 9), third.part)
    }

    @Test
    fun `tail scheduler keeps a bounded fresh part count`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 320,
            initialPartSizeBytes = 100,
            maxRetriesPerPart = 1,
            concurrency = 8,
            tuning = testTuning.copy(tailPartsPerConnection = 4),
        )
        val parts = buildList {
            var workerId = 0
            while (true) {
                val active = scheduler.nextPart(workerId) ?: break
                add(active.part)
                scheduler.finish(workerId, active)
                workerId = (workerId + 1) % 8
            }
        }

        assertEquals(32, parts.size)
        assertEquals(320, parts.sumOf(DownloadPart::length))
        assertTrue(parts.all { it.length == 10L })
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
        scheduler.recordSuccess(
            workerId = 0,
            part = first.part,
            bytes = first.part.length,
            elapsedMs = 100,
        )

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
            PartSchedulerStats(
                retryCount = 0,
                stealCount = 0,
                handoffCount = 0,
                peakActiveConnections = 2,
            ),
            scheduler.stats(),
        )
    }

    @Test
    fun `scheduler reuses a released startup slot before growth`() = runBlocking {
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
        scheduler.recordSuccess(
            workerId = 0,
            part = completed.part,
            bytes = completed.part.length,
            elapsedMs = 100,
        )

        assertNotNull(scheduler.nextPart(workerId = 4))
        Unit
    }

    @Test
    fun `retry budgets are independent for eof and rate limit failures`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 100,
            initialPartSizeBytes = 100,
            maxRetriesPerPart = 1,
            concurrency = 1,
            tuning = testTuning,
            retryBudgets = RangeRetryBudgets(
                partialEof = 1,
                timeout = 0,
                connectionReset = 0,
                rateLimited = 1,
                transient = 0,
            ),
        )
        val initial = assertNotNull(scheduler.nextPart(workerId = 0))
        scheduler.finish(workerId = 0, active = initial)

        assertTrue(
            scheduler.requeueFailed(
                part = initial.part,
                nextStart = 0,
                failureKind = RangeFailureKind.PartialEof,
            ),
        )
        val afterEof = assertNotNull(scheduler.nextPart(workerId = 0))
        scheduler.finish(workerId = 0, active = afterEof)

        assertTrue(
            scheduler.requeueFailed(
                part = afterEof.part,
                nextStart = 0,
                failureKind = RangeFailureKind.RateLimited,
            ),
        )
        val afterBoth = assertNotNull(scheduler.nextPart(workerId = 0))
        scheduler.finish(workerId = 0, active = afterBoth)

        assertFalse(
            scheduler.requeueFailed(
                part = afterBoth.part,
                nextStart = 0,
                failureKind = RangeFailureKind.PartialEof,
            ),
        )
        assertFalse(
            scheduler.requeueFailed(
                part = afterBoth.part,
                nextStart = 0,
                failureKind = RangeFailureKind.RateLimited,
            ),
        )
        assertEquals(2, scheduler.stats().retryCount)
    }

    @Test
    fun `rate limit strikes reduce concurrency and recover with one probe`() = runBlocking {
        var now = 0L
        val scheduler = PartScheduler(
            totalBytes = 10_000,
            initialPartSizeBytes = 100,
            maxRetriesPerPart = 1,
            concurrency = 6,
            tuning = testTuning.copy(
                startupActiveConnections = 6,
                tailWindowInitialMultiplier = 0,
                rateLimitMinActiveConnections = 2,
                rateLimitStrikeThreshold = 2,
                rateLimitWindowMs = 1_000,
                rateLimitCooldownMs = 100,
                rateLimitRecoveryMs = 100,
                rateLimitedMinPartSizeBytes = 100,
            ),
            nowMs = { now },
        )

        repeat(2) { workerId ->
            val limited = assertNotNull(scheduler.nextPart(workerId))
            scheduler.finish(workerId, limited)
            scheduler.recordRateLimit(part = limited.part, delayMs = 100)
        }

        val admitted = (0 until 5).map { workerId ->
            assertNotNull(scheduler.nextPart(workerId))
        }
        assertNull(scheduler.nextPart(workerId = 5))

        scheduler.finish(workerId = 0, active = admitted.first())
        now = 99
        assertNotNull(scheduler.nextPart(workerId = 0))
        now = 100
        val probe = assertNotNull(scheduler.nextPart(workerId = 5))

        assertTrue(probe.part.rateProbe)

        scheduler.recordRateLimit(part = probe.part, delayMs = 100)
        scheduler.finish(workerId = 5, active = probe)
        assertNull(scheduler.nextPart(workerId = 5))
    }

    @Test
    fun `rate limited scheduler increases part size floor`() = runBlocking {
        val scheduler = PartScheduler(
            totalBytes = 5_000,
            initialPartSizeBytes = 100,
            maxRetriesPerPart = 1,
            concurrency = 4,
            tuning = testTuning.copy(
                tailWindowInitialMultiplier = 0,
                tailWindowMinDynamicMultiplier = 0,
                rateLimitStrikeThreshold = 1,
                rateLimitedMinPartSizeBytes = 400,
                limitedTailPartsPerConnection = 1,
            ),
        )
        val limited = assertNotNull(scheduler.nextPart(workerId = 0))
        scheduler.finish(workerId = 0, active = limited)
        scheduler.recordRateLimit(part = limited.part, delayMs = 100)

        val next = assertNotNull(scheduler.nextPart(workerId = 1))

        assertEquals(400, next.part.length)
    }

    private companion object {
        val testTuning = PartSchedulerTuning(
            minDynamicPartSizeBytes = 1,
            minTailPartSizeBytes = 1,
        )
    }
}
