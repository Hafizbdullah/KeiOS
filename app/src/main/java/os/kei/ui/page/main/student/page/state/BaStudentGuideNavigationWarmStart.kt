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
