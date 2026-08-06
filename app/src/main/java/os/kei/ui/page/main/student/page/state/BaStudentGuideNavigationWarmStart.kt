package os.kei.ui.page.main.student.page.state

import os.kei.ui.page.main.student.BaStudentGuideInfo
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal data class BaStudentGuideNavigationWarmStart(
    val sourceUrl: String,
    val info: BaStudentGuideInfo? = null,
    val isNpcSatelliteGuide: Boolean = false,
    val contentPresentationState: BaStudentGuideContentPresentationState =
        BaStudentGuideContentPresentationState(),
)

/**
 * One-shot process-local handoff for a Nav 3 entry.
 *
 * The persisted source URL remains the recovery source after process death. This handoff only
 * keeps an already-decoded cache snapshot ready for the first frame of the destination.
 */
internal object BaStudentGuideNavigationWarmStartStore {
    private data class Entry(
        val id: Long,
        val snapshot: BaStudentGuideNavigationWarmStart,
    )

    private val nextId = AtomicLong(1L)
    private val latest = AtomicReference<Entry?>(null)

    fun publish(snapshot: BaStudentGuideNavigationWarmStart): Long {
        val id = nextId.getAndIncrement()
        latest.set(Entry(id = id, snapshot = snapshot))
        return id
    }

    /**
     * Claim an id before the snapshot exists, so navigation can start immediately and the snapshot
     * can land afterwards via [fulfil].
     *
     * Waiting for the snapshot before navigating costs 34-46 ms on the first navigation of a
     * process (measured on 5eea1f50 with a real full cache), which breaks the plan's rule that
     * click to animation start must not grow by more than one 120 Hz frame. A timeout cannot fix
     * that: the underlying cache read is a blocking call with no cancellation points, so
     * `withTimeoutOrNull` does not actually cap the wall time.
     */
    fun reserve(): Long = nextId.getAndIncrement()

    /**
     * Attach a snapshot to a reserved id. Ignored if a newer navigation has since claimed the slot,
     * so a slow read from an abandoned navigation can never overwrite a newer one.
     */
    fun fulfil(
        id: Long,
        snapshot: BaStudentGuideNavigationWarmStart,
    ) {
        if (id <= 0L) return
        val current = latest.get()
        if (current != null && current.id > id) return
        latest.set(Entry(id = id, snapshot = snapshot))
    }

    fun consume(id: Long): BaStudentGuideNavigationWarmStart? {
        if (id <= 0L) return null
        while (true) {
            val entry = latest.get() ?: return null
            if (entry.id != id) return null
            if (latest.compareAndSet(entry, null)) return entry.snapshot
        }
    }

    internal fun clearForTest() {
        latest.set(null)
    }
}
