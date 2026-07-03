package os.kei.ui.page.main.ba

import android.content.Context
import os.kei.ui.page.main.ba.support.BaCalendarEntry
import os.kei.ui.page.main.ba.support.BaPoolEntry

internal object BaCalendarPoolSyncNotifier {
    fun recordCalendarUnreadObservation(
        serverIndex: Int,
        previousEntries: List<BaCalendarEntry>,
        nextEntries: List<BaCalendarEntry>,
        nowMs: Long,
        hadCache: Boolean,
    ): BaCalendarPoolUnreadRecordResult =
        BaCalendarPoolUnreadRepository.recordCalendarObservation(
            serverIndex = serverIndex,
            previousEntries = previousEntries,
            nextEntries = nextEntries,
            nowMs = nowMs,
            hadCache = hadCache,
        )

    fun dispatchCalendarSyncNotifications(
        context: Context,
        serverIndex: Int,
        previousEntries: List<BaCalendarEntry>,
        nextEntries: List<BaCalendarEntry>,
        nowMs: Long,
        hadCache: Boolean,
    ) {
        val runtime = BaSettingsPersistenceRepository.loadCalendarPoolNotificationRuntime()
        val settings = runtime.settings
        val notifiedKeys = runtime.notifiedKeys
        val dataDiff =
            if (hadCache) {
                BaCalendarPoolChangeDetector.calendarDataDiff(previousEntries, nextEntries)
            } else {
                BaCalendarPoolChangeDiff(
                    fingerprint = BaCalendarPoolChangeDetector.calendarDataDiff(emptyList(), nextEntries).fingerprint,
                )
            }
        BaCalendarPoolUnreadRepository.recordCalendarObservation(
            serverIndex = serverIndex,
            previousEntries = previousEntries,
            nextEntries = nextEntries,
            nowMs = nowMs,
            hadCache = hadCache,
            dataDiff = dataDiff,
        )

        if (settings.calendarUpcomingNotifyEnabled) {
            val groups =
                BaReminderCoordinator.calendarUpcomingGroups(
                    entries = nextEntries,
                    nowMs = nowMs,
                    serverIndex = serverIndex,
                    leadHours = settings.calendarPoolNotifyLeadHours,
                    notifiedKeys = notifiedKeys,
                )
            groups.forEach { group ->
                if (BaCalendarPoolNotificationDispatcher.sendCalendarUpcomingGroup(
                        context,
                        serverIndex,
                        group.entries,
                    )
                ) {
                    group.keys.forEach(BaSettingsPersistenceRepository::markCalendarPoolNotified)
                }
            }
        }

        if (settings.calendarEndingNotifyEnabled) {
            val groups =
                BaReminderCoordinator.calendarEndingGroups(
                    entries = nextEntries,
                    nowMs = nowMs,
                    serverIndex = serverIndex,
                    leadHours = settings.calendarPoolNotifyLeadHours,
                    notifiedKeys = notifiedKeys,
                )
            groups.forEach { group ->
                if (BaCalendarPoolNotificationDispatcher.sendCalendarEndingGroup(
                        context,
                        serverIndex,
                        group.entries,
                    )
                ) {
                    group.keys.forEach(BaSettingsPersistenceRepository::markCalendarPoolNotified)
                }
            }
        }

        if (hadCache && settings.calendarPoolChangeNotifyEnabled) {
            val changedCount = dataDiff.changedCount
            val changeKey =
                BaReminderCoordinator.changeKey(
                    serverIndex = serverIndex,
                    type = "calendar_change",
                    changedCount = changedCount,
                    fingerprint = dataDiff.fingerprint,
                )
            if (changedCount > 0 &&
                changeKey !in notifiedKeys &&
                BaCalendarPoolNotificationDispatcher.sendDataChanged(
                    context = context,
                    serverIndex = serverIndex,
                    calendarChangeCount = changedCount,
                    poolChangeCount = 0,
                    detail = dataDiff.firstTitle,
                )
            ) {
                BaSettingsPersistenceRepository.markCalendarPoolNotified(changeKey)
            }
        }
    }

    fun recordPoolUnreadObservation(
        serverIndex: Int,
        previousEntries: List<BaPoolEntry>,
        nextEntries: List<BaPoolEntry>,
        nowMs: Long,
        hadCache: Boolean,
    ): BaCalendarPoolUnreadRecordResult =
        BaCalendarPoolUnreadRepository.recordPoolObservation(
            serverIndex = serverIndex,
            previousEntries = previousEntries,
            nextEntries = nextEntries,
            nowMs = nowMs,
            hadCache = hadCache,
        )

    fun dispatchPoolSyncNotifications(
        context: Context,
        serverIndex: Int,
        previousEntries: List<BaPoolEntry>,
        nextEntries: List<BaPoolEntry>,
        nowMs: Long,
        hadCache: Boolean,
    ) {
        val runtime = BaSettingsPersistenceRepository.loadCalendarPoolNotificationRuntime()
        val settings = runtime.settings
        val notifiedKeys = runtime.notifiedKeys
        val dataDiff =
            if (hadCache) {
                BaCalendarPoolChangeDetector.poolDataDiff(previousEntries, nextEntries)
            } else {
                BaCalendarPoolChangeDiff(
                    fingerprint = BaCalendarPoolChangeDetector.poolDataDiff(emptyList(), nextEntries).fingerprint,
                )
            }
        BaCalendarPoolUnreadRepository.recordPoolObservation(
            serverIndex = serverIndex,
            previousEntries = previousEntries,
            nextEntries = nextEntries,
            nowMs = nowMs,
            hadCache = hadCache,
            dataDiff = dataDiff,
        )

        if (settings.poolUpcomingNotifyEnabled) {
            val groups =
                BaReminderCoordinator.poolUpcomingGroups(
                    entries = nextEntries,
                    nowMs = nowMs,
                    serverIndex = serverIndex,
                    leadHours = settings.calendarPoolNotifyLeadHours,
                    notifiedKeys = notifiedKeys,
                )
            groups.forEach { group ->
                if (BaCalendarPoolNotificationDispatcher.sendPoolUpcomingGroup(
                        context,
                        serverIndex,
                        group.entries,
                    )
                ) {
                    group.keys.forEach(BaSettingsPersistenceRepository::markCalendarPoolNotified)
                }
            }
        }

        if (settings.poolEndingNotifyEnabled) {
            val groups =
                BaReminderCoordinator.poolEndingGroups(
                    entries = nextEntries,
                    nowMs = nowMs,
                    serverIndex = serverIndex,
                    leadHours = settings.calendarPoolNotifyLeadHours,
                    notifiedKeys = notifiedKeys,
                )
            groups.forEach { group ->
                if (BaCalendarPoolNotificationDispatcher.sendPoolEndingGroup(
                        context,
                        serverIndex,
                        group.entries,
                    )
                ) {
                    group.keys.forEach(BaSettingsPersistenceRepository::markCalendarPoolNotified)
                }
            }
        }

        if (hadCache && settings.calendarPoolChangeNotifyEnabled) {
            val changedCount = dataDiff.changedCount
            val changeKey =
                BaReminderCoordinator.changeKey(
                    serverIndex = serverIndex,
                    type = "pool_change",
                    changedCount = changedCount,
                    fingerprint = dataDiff.fingerprint,
                )
            if (changedCount > 0 &&
                changeKey !in notifiedKeys &&
                BaCalendarPoolNotificationDispatcher.sendDataChanged(
                    context = context,
                    serverIndex = serverIndex,
                    calendarChangeCount = 0,
                    poolChangeCount = changedCount,
                    detail = dataDiff.firstTitle,
                )
            ) {
                BaSettingsPersistenceRepository.markCalendarPoolNotified(changeKey)
            }
        }
    }
}
