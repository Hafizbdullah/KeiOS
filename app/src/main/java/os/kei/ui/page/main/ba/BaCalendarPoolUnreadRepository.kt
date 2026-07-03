package os.kei.ui.page.main.ba

import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.ui.page.main.ba.support.BaCalendarEntry
import os.kei.ui.page.main.ba.support.BaCalendarPoolUnreadStore
import os.kei.ui.page.main.ba.support.BaPoolEntry

internal data class BaCalendarPoolUnreadRecordResult(
    val changedCount: Int,
    val recorded: Boolean,
)

internal object BaCalendarPoolUnreadRepository {
    fun loadCounts(serverIndex: Int): BaCalendarPoolUnreadCounts =
        BaCalendarPoolUnreadCounter.buildCounts(
            events = BaCalendarPoolUnreadStore.loadEvents(),
            watermarks = BaCalendarPoolUnreadStore.loadWatermarks(),
            serverIndex = serverIndex,
        )

    suspend fun loadCountsAsync(serverIndex: Int): BaCalendarPoolUnreadCounts =
        withContext(AppDispatchers.baFetch) {
            loadCounts(serverIndex)
        }

    fun markRead(
        kind: BaCalendarPoolUnreadKind,
        serverIndex: Int,
    ): BaCalendarPoolUnreadCounts {
        val events = BaCalendarPoolUnreadStore.loadEvents()
        val latestSeenAtMillis =
            BaCalendarPoolUnreadCounter.latestSeenAt(
                events = events,
                serverIndex = serverIndex,
                kind = kind,
            )
        val watermarks =
            BaCalendarPoolUnreadStore.markRead(
                kind = kind,
                serverIndex = serverIndex,
                latestSeenAtMillis = latestSeenAtMillis,
            )
        return BaCalendarPoolUnreadCounter.buildCounts(
            events = events,
            watermarks = watermarks,
            serverIndex = serverIndex,
        )
    }

    suspend fun markReadAsync(
        kind: BaCalendarPoolUnreadKind,
        serverIndex: Int,
    ): BaCalendarPoolUnreadCounts =
        withContext(AppDispatchers.baFetch) {
            markRead(
                kind = kind,
                serverIndex = serverIndex,
            )
        }

    fun recordCalendarObservation(
        serverIndex: Int,
        previousEntries: List<BaCalendarEntry>,
        nextEntries: List<BaCalendarEntry>,
        nowMs: Long,
        hadCache: Boolean,
        dataDiff: BaCalendarPoolChangeDiff? = null,
    ): BaCalendarPoolUnreadRecordResult =
        recordObservation(
            kind = BaCalendarPoolUnreadKind.Calendar,
            serverIndex = serverIndex,
            nextEntries = nextEntries,
            nowMs = nowMs,
            hadCache = hadCache,
            dataDiff = dataDiff ?: BaCalendarPoolChangeDetector.calendarDataDiff(previousEntries, nextEntries),
            phaseSnapshotOf = { entries ->
                BaCalendarPoolChangeDetector.calendarPhaseSnapshot(
                    entries = entries,
                    nowMs = nowMs,
                )
            },
            statusTransitionIdsOf = { previousPhases, currentPhases ->
                BaCalendarPoolChangeDetector.statusTransitionIds(
                    previousPhases = previousPhases,
                    nextPhases = currentPhases,
                )
            },
            idOf = { entry -> entry.id },
            titleOf = { entry -> entry.title },
        )

    fun recordPoolObservation(
        serverIndex: Int,
        previousEntries: List<BaPoolEntry>,
        nextEntries: List<BaPoolEntry>,
        nowMs: Long,
        hadCache: Boolean,
        dataDiff: BaCalendarPoolChangeDiff? = null,
    ): BaCalendarPoolUnreadRecordResult =
        recordObservation(
            kind = BaCalendarPoolUnreadKind.Pool,
            serverIndex = serverIndex,
            nextEntries = nextEntries,
            nowMs = nowMs,
            hadCache = hadCache,
            dataDiff = dataDiff ?: BaCalendarPoolChangeDetector.poolDataDiff(previousEntries, nextEntries),
            phaseSnapshotOf = { entries ->
                BaCalendarPoolChangeDetector.poolPhaseSnapshot(
                    entries = entries,
                    nowMs = nowMs,
                )
            },
            statusTransitionIdsOf = { previousPhases, currentPhases ->
                BaCalendarPoolChangeDetector.statusTransitionIds(
                    previousPhases = previousPhases,
                    nextPhases = currentPhases,
                )
            },
            idOf = { entry -> entry.id },
            titleOf = { entry -> entry.name },
        )

    private fun <T> recordObservation(
        kind: BaCalendarPoolUnreadKind,
        serverIndex: Int,
        nextEntries: List<T>,
        nowMs: Long,
        hadCache: Boolean,
        dataDiff: BaCalendarPoolChangeDiff,
        phaseSnapshotOf: (List<T>) -> Map<Int, BaCalendarPoolEntryPhase>,
        statusTransitionIdsOf: (
            previousPhases: Map<Int, BaCalendarPoolEntryPhase>,
            currentPhases: Map<Int, BaCalendarPoolEntryPhase>,
        ) -> Set<Int>,
        idOf: (T) -> Int,
        titleOf: (T) -> String,
    ): BaCalendarPoolUnreadRecordResult {
        val normalizedServerIndex = serverIndex.coerceIn(0, 2)
        val previousPhases =
            BaCalendarPoolUnreadStore.loadPhaseSnapshot(
                kind = kind,
                serverIndex = normalizedServerIndex,
            )
        val currentPhases = phaseSnapshotOf(nextEntries)
        val statusTransitionIds =
            if (previousPhases.isEmpty()) {
                emptySet()
            } else {
                statusTransitionIdsOf(previousPhases, currentPhases)
            }
        if (currentPhases != previousPhases) {
            BaCalendarPoolUnreadStore.savePhaseSnapshot(
                kind = kind,
                serverIndex = normalizedServerIndex,
                phases = currentPhases,
            )
        }
        if (!hadCache) {
            return BaCalendarPoolUnreadRecordResult(
                changedCount = 0,
                recorded = false,
            )
        }

        val statusOnlyIds = statusTransitionIds - dataDiff.changedIds
        val changedCount = dataDiff.changedCount + statusOnlyIds.size
        if (changedCount <= 0) {
            return BaCalendarPoolUnreadRecordResult(
                changedCount = 0,
                recorded = false,
            )
        }

        val phaseFingerprint = BaCalendarPoolChangeDetector.phaseFingerprint(currentPhases)
        val fingerprint =
            BaCalendarPoolChangeDetector.combinedFingerprint(
                dataDiff.fingerprint,
                phaseFingerprint,
                changedCount.toLong(),
            )
        val detail =
            dataDiff.firstTitle.ifBlank {
                firstTransitionTitle(
                    ids = statusOnlyIds,
                    entries = nextEntries,
                    idOf = idOf,
                    titleOf = titleOf,
                )
            }
        val recorded =
            BaCalendarPoolUnreadStore.recordEvent(
                BaCalendarPoolUnreadEvent(
                    id = "${normalizedServerIndex}|${kind.name}|$fingerprint",
                    serverIndex = normalizedServerIndex,
                    kind = kind,
                    changedAtMillis = nowMs.coerceAtLeast(1L),
                    changeCount = changedCount,
                    fingerprint = fingerprint,
                    detail = detail,
                ),
            )
        return BaCalendarPoolUnreadRecordResult(
            changedCount = changedCount,
            recorded = recorded,
        )
    }

    private fun <T> firstTransitionTitle(
        ids: Set<Int>,
        entries: List<T>,
        idOf: (T) -> Int,
        titleOf: (T) -> String,
    ): String {
        if (ids.isEmpty()) return ""
        return entries
            .asSequence()
            .filter { entry -> idOf(entry) in ids }
            .map(titleOf)
            .map { title -> title.trim() }
            .firstOrNull { title -> title.isNotBlank() }
            .orEmpty()
    }
}
