package os.kei.ui.page.main.ba

import androidx.compose.runtime.Immutable
import os.kei.ui.page.main.ba.support.BaCalendarEntry
import os.kei.ui.page.main.ba.support.BaPoolEntry

internal enum class BaCalendarPoolUnreadKind {
    Calendar,
    Pool,
}

internal enum class BaCalendarPoolEntryPhase {
    Upcoming,
    Running,
    Ended,
}

@Immutable
internal data class BaCalendarPoolUnreadCounts(
    val calendarCount: Int = 0,
    val poolCount: Int = 0,
) {
    val totalCount: Int
        get() = calendarCount + poolCount
}

internal data class BaCalendarPoolUnreadWatermarks(
    val calendarReadAtByServer: Map<Int, Long> = emptyMap(),
    val poolReadAtByServer: Map<Int, Long> = emptyMap(),
) {
    fun readAt(
        kind: BaCalendarPoolUnreadKind,
        serverIndex: Int,
    ): Long {
        val normalizedServerIndex = serverIndex.coerceIn(0, 2)
        return when (kind) {
            BaCalendarPoolUnreadKind.Calendar -> calendarReadAtByServer[normalizedServerIndex]
            BaCalendarPoolUnreadKind.Pool -> poolReadAtByServer[normalizedServerIndex]
        }?.coerceAtLeast(0L) ?: 0L
    }

    fun markRead(
        kind: BaCalendarPoolUnreadKind,
        serverIndex: Int,
        latestSeenAtMillis: Long,
    ): BaCalendarPoolUnreadWatermarks {
        if (latestSeenAtMillis <= 0L) return this
        val normalizedServerIndex = serverIndex.coerceIn(0, 2)
        return when (kind) {
            BaCalendarPoolUnreadKind.Calendar ->
                copy(
                    calendarReadAtByServer =
                        calendarReadAtByServer + (
                            normalizedServerIndex to
                                maxOf(readAt(kind, normalizedServerIndex), latestSeenAtMillis)
                        ),
                )

            BaCalendarPoolUnreadKind.Pool ->
                copy(
                    poolReadAtByServer =
                        poolReadAtByServer + (
                            normalizedServerIndex to
                                maxOf(readAt(kind, normalizedServerIndex), latestSeenAtMillis)
                        ),
                )
        }
    }
}

internal data class BaCalendarPoolUnreadEvent(
    val id: String,
    val serverIndex: Int,
    val kind: BaCalendarPoolUnreadKind,
    val changedAtMillis: Long,
    val changeCount: Int,
    val fingerprint: Long,
    val detail: String = "",
)

internal data class BaCalendarPoolChangeDiff(
    val changedIds: Set<Int> = emptySet(),
    val firstTitle: String = "",
    val fingerprint: Long = 0L,
) {
    val changedCount: Int
        get() = changedIds.size
}

internal object BaCalendarPoolUnreadCounter {
    fun buildCounts(
        events: List<BaCalendarPoolUnreadEvent>,
        watermarks: BaCalendarPoolUnreadWatermarks,
        serverIndex: Int,
    ): BaCalendarPoolUnreadCounts =
        BaCalendarPoolUnreadCounts(
            calendarCount =
                countUnread(
                    events = events,
                    watermarks = watermarks,
                    serverIndex = serverIndex,
                    kind = BaCalendarPoolUnreadKind.Calendar,
                ),
            poolCount =
                countUnread(
                    events = events,
                    watermarks = watermarks,
                    serverIndex = serverIndex,
                    kind = BaCalendarPoolUnreadKind.Pool,
                ),
        )

    fun latestSeenAt(
        events: List<BaCalendarPoolUnreadEvent>,
        serverIndex: Int,
        kind: BaCalendarPoolUnreadKind,
    ): Long {
        val normalizedServerIndex = serverIndex.coerceIn(0, 2)
        return events
            .asSequence()
            .filter { event ->
                event.serverIndex.coerceIn(0, 2) == normalizedServerIndex &&
                    event.kind == kind
            }.maxOfOrNull { event -> event.changedAtMillis.coerceAtLeast(0L) }
            ?: 0L
    }

    private fun countUnread(
        events: List<BaCalendarPoolUnreadEvent>,
        watermarks: BaCalendarPoolUnreadWatermarks,
        serverIndex: Int,
        kind: BaCalendarPoolUnreadKind,
    ): Int {
        val normalizedServerIndex = serverIndex.coerceIn(0, 2)
        val readAtMillis = watermarks.readAt(kind, normalizedServerIndex)
        return events
            .asSequence()
            .filter { event ->
                event.serverIndex.coerceIn(0, 2) == normalizedServerIndex &&
                    event.kind == kind &&
                    event.changedAtMillis > readAtMillis
            }.sumOf { event -> event.changeCount.coerceAtLeast(1) }
    }
}

internal object BaCalendarPoolChangeDetector {
    fun calendarDataDiff(
        previousEntries: List<BaCalendarEntry>,
        nextEntries: List<BaCalendarEntry>,
    ): BaCalendarPoolChangeDiff =
        dataDiff(
            previousEntries = previousEntries,
            nextEntries = nextEntries,
            idOf = { entry -> entry.id },
            titleOf = { entry -> entry.title },
            signatureOf = { entry ->
                "${entry.title}|${entry.kindId}|${entry.beginAtMs}|${entry.endAtMs}|${entry.linkUrl}"
            },
            fingerprintLineOf = { entry ->
                "${entry.id}|${entry.title}|${entry.kindId}|${entry.beginAtMs}|${entry.endAtMs}|${entry.linkUrl}"
            },
        )

    fun poolDataDiff(
        previousEntries: List<BaPoolEntry>,
        nextEntries: List<BaPoolEntry>,
    ): BaCalendarPoolChangeDiff =
        dataDiff(
            previousEntries = previousEntries,
            nextEntries = nextEntries,
            idOf = { entry -> entry.id },
            titleOf = { entry -> entry.name },
            signatureOf = { entry ->
                "${entry.name}|${entry.tagId}|${entry.startAtMs}|${entry.endAtMs}|${entry.linkUrl}"
            },
            fingerprintLineOf = { entry ->
                "${entry.id}|${entry.name}|${entry.tagId}|${entry.startAtMs}|${entry.endAtMs}|${entry.linkUrl}"
            },
        )

    fun calendarPhaseSnapshot(
        entries: List<BaCalendarEntry>,
        nowMs: Long,
    ): Map<Int, BaCalendarPoolEntryPhase> =
        entries.associate { entry ->
            entry.id to
                phaseOf(
                    beginAtMs = entry.beginAtMs,
                    endAtMs = entry.endAtMs,
                    nowMs = nowMs,
                )
        }

    fun poolPhaseSnapshot(
        entries: List<BaPoolEntry>,
        nowMs: Long,
    ): Map<Int, BaCalendarPoolEntryPhase> =
        entries.associate { entry ->
            entry.id to
                phaseOf(
                    beginAtMs = entry.startAtMs,
                    endAtMs = entry.endAtMs,
                    nowMs = nowMs,
                )
        }

    fun calendarStatusTransitionIds(
        previousPhases: Map<Int, BaCalendarPoolEntryPhase>,
        nextEntries: List<BaCalendarEntry>,
        nowMs: Long,
    ): Set<Int> =
        statusTransitionIds(
            previousPhases = previousPhases,
            nextPhases = calendarPhaseSnapshot(nextEntries, nowMs),
        )

    fun poolStatusTransitionIds(
        previousPhases: Map<Int, BaCalendarPoolEntryPhase>,
        nextEntries: List<BaPoolEntry>,
        nowMs: Long,
    ): Set<Int> =
        statusTransitionIds(
            previousPhases = previousPhases,
            nextPhases = poolPhaseSnapshot(nextEntries, nowMs),
        )

    fun statusTransitionIds(
        previousPhases: Map<Int, BaCalendarPoolEntryPhase>,
        nextPhases: Map<Int, BaCalendarPoolEntryPhase>,
    ): Set<Int> =
        nextPhases
            .filter { (id, nextPhase) ->
                val previousPhase = previousPhases[id] ?: return@filter false
                previousPhase != nextPhase &&
                    (
                        nextPhase == BaCalendarPoolEntryPhase.Running ||
                            nextPhase == BaCalendarPoolEntryPhase.Ended
                    )
            }.keys

    fun phaseFingerprint(phases: Map<Int, BaCalendarPoolEntryPhase>): Long =
        stableFingerprint(
            phases
                .toSortedMap()
                .map { (id, phase) -> "$id|${phase.name}" },
        )

    fun combinedFingerprint(vararg values: Long): Long {
        var result = 1125899906842597L
        values.forEach { value ->
            result = result * 31L + value
        }
        return result.and(0xffffffffL)
    }

    private fun <T> dataDiff(
        previousEntries: List<T>,
        nextEntries: List<T>,
        idOf: (T) -> Int,
        titleOf: (T) -> String,
        signatureOf: (T) -> String,
        fingerprintLineOf: (T) -> String,
    ): BaCalendarPoolChangeDiff {
        if (previousEntries === nextEntries) return BaCalendarPoolChangeDiff()

        val previousById = previousEntries.associateBy(idOf)
        val nextById = nextEntries.associateBy(idOf)
        val changedIds =
            (previousById.keys + nextById.keys)
                .filter { id ->
                    previousById[id]?.let(signatureOf) != nextById[id]?.let(signatureOf)
                }.toSet()
        val firstTitle =
            changedIds
                .asSequence()
                .mapNotNull { id -> nextById[id] ?: previousById[id] }
                .map(titleOf)
                .map { title -> title.trim() }
                .firstOrNull { title -> title.isNotBlank() }
                .orEmpty()
        return BaCalendarPoolChangeDiff(
            changedIds = changedIds,
            firstTitle = firstTitle,
            fingerprint =
                stableFingerprint(
                    nextEntries
                        .sortedBy(idOf)
                        .map(fingerprintLineOf),
                ),
        )
    }

    private fun phaseOf(
        beginAtMs: Long,
        endAtMs: Long,
        nowMs: Long,
    ): BaCalendarPoolEntryPhase =
        when {
            nowMs < beginAtMs -> BaCalendarPoolEntryPhase.Upcoming
            nowMs >= endAtMs -> BaCalendarPoolEntryPhase.Ended
            else -> BaCalendarPoolEntryPhase.Running
        }

    private fun stableFingerprint(lines: List<String>): Long =
        lines
            .joinToString(separator = "\n")
            .hashCode()
            .toLong()
            .and(0xffffffffL)
}
