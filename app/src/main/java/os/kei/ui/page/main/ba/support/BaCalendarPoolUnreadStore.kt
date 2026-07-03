package os.kei.ui.page.main.ba.support

import com.tencent.mmkv.MMKV
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import os.kei.core.json.encodeCompact
import os.kei.core.json.optInt
import os.kei.core.json.optLong
import os.kei.core.json.optString
import os.kei.core.json.parseJsonArrayOrNull
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.core.prefs.KeiMmkv
import os.kei.ui.page.main.ba.BaCalendarPoolEntryPhase
import os.kei.ui.page.main.ba.BaCalendarPoolUnreadEvent
import os.kei.ui.page.main.ba.BaCalendarPoolUnreadKind
import os.kei.ui.page.main.ba.BaCalendarPoolUnreadWatermarks

internal object BaCalendarPoolUnreadStore {
    private const val KEY_EVENTS = "calendar_pool_unread_events_v1"
    private const val KEY_WATERMARKS = "calendar_pool_unread_watermarks_v1"
    private const val KEY_CALENDAR_PHASE_PREFIX = "calendar_pool_unread_calendar_phase_"
    private const val KEY_POOL_PHASE_PREFIX = "calendar_pool_unread_pool_phase_"
    private const val MAX_EVENTS = 160

    private val store: MMKV by lazy { KeiMmkv.byId(BA_SETTINGS_KV_ID) }

    private fun kv(): MMKV = store

    fun loadEvents(): List<BaCalendarPoolUnreadEvent> =
        kv()
            .decodeString(KEY_EVENTS, "")
            .orEmpty()
            .parseJsonArrayOrNull()
            ?.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                decodeEvent(obj)
            }?.sortedByDescending { event -> event.changedAtMillis }
            .orEmpty()

    fun recordEvent(event: BaCalendarPoolUnreadEvent): Boolean {
        val normalized = event.normalizedForStore() ?: return false
        val events =
            (loadEvents().filterNot { it.id == normalized.id } + normalized)
                .sortedWith(
                    compareBy<BaCalendarPoolUnreadEvent> { it.changedAtMillis }
                        .thenBy { it.id },
                ).takeLast(MAX_EVENTS)
        saveEvents(events)
        BASettingsStoreSignals.notifyChanged(notifyHomeOverview = false)
        return true
    }

    fun loadWatermarks(): BaCalendarPoolUnreadWatermarks {
        val obj =
            kv()
                .decodeString(KEY_WATERMARKS, "")
                .orEmpty()
                .parseJsonObjectOrNull()
                ?: return BaCalendarPoolUnreadWatermarks()
        return BaCalendarPoolUnreadWatermarks(
            calendarReadAtByServer = loadServerLongMap(obj, "calendar"),
            poolReadAtByServer = loadServerLongMap(obj, "pool"),
        )
    }

    fun markRead(
        kind: BaCalendarPoolUnreadKind,
        serverIndex: Int,
        latestSeenAtMillis: Long,
    ): BaCalendarPoolUnreadWatermarks {
        val previous = loadWatermarks()
        val next =
            previous.markRead(
                kind = kind,
                serverIndex = serverIndex,
                latestSeenAtMillis = latestSeenAtMillis,
            )
        if (next != previous) {
            saveWatermarks(next)
            BASettingsStoreSignals.notifyChanged(notifyHomeOverview = false)
        }
        return next
    }

    fun loadPhaseSnapshot(
        kind: BaCalendarPoolUnreadKind,
        serverIndex: Int,
    ): Map<Int, BaCalendarPoolEntryPhase> {
        val obj =
            kv()
                .decodeString(phaseKey(kind, serverIndex), "")
                .orEmpty()
                .parseJsonObjectOrNull()
                ?: return emptyMap()
        return obj.entries.mapNotNull { (idText, phaseElement) ->
            val id = idText.toIntOrNull() ?: return@mapNotNull null
            val phaseText = (phaseElement as? JsonPrimitive)?.content.orEmpty()
            val phase = enumValueOrNull<BaCalendarPoolEntryPhase>(phaseText) ?: return@mapNotNull null
            id to phase
        }.toMap()
    }

    fun savePhaseSnapshot(
        kind: BaCalendarPoolUnreadKind,
        serverIndex: Int,
        phases: Map<Int, BaCalendarPoolEntryPhase>,
    ) {
        val key = phaseKey(kind, serverIndex)
        val kv = kv()
        val normalized =
            phases
                .filterKeys { id -> id > 0 }
                .toSortedMap()
        if (normalized.isEmpty()) {
            kv.removeValueForKey(key)
            return
        }
        val obj =
            buildJsonObject {
                normalized.forEach { (id, phase) ->
                    put(id.toString(), phase.name)
                }
            }
        kv.encode(key, obj.encodeCompact())
    }

    private fun saveEvents(events: List<BaCalendarPoolUnreadEvent>) {
        val kv = kv()
        val normalized = events.mapNotNull { event -> event.normalizedForStore() }
        if (normalized.isEmpty()) {
            kv.removeValueForKey(KEY_EVENTS)
            kv.trim()
            return
        }
        val array =
            buildJsonArray {
                normalized.forEach { event ->
                    add(encodeEvent(event))
                }
            }
        kv.encode(KEY_EVENTS, array.encodeCompact())
    }

    private fun saveWatermarks(watermarks: BaCalendarPoolUnreadWatermarks) {
        val obj =
            buildJsonObject {
                putServerLongMap("calendar", watermarks.calendarReadAtByServer)
                putServerLongMap("pool", watermarks.poolReadAtByServer)
            }
        kv().encode(KEY_WATERMARKS, obj.encodeCompact())
    }

    private fun encodeEvent(event: BaCalendarPoolUnreadEvent): JsonObject =
        buildJsonObject {
            put("id", event.id)
            put("serverIndex", event.serverIndex.coerceIn(0, 2))
            put("kind", event.kind.name)
            put("changedAtMillis", event.changedAtMillis.coerceAtLeast(0L))
            put("changeCount", event.changeCount.coerceAtLeast(1))
            put("fingerprint", event.fingerprint.coerceAtLeast(0L))
            put("detail", event.detail.trim())
        }

    private fun decodeEvent(obj: JsonObject): BaCalendarPoolUnreadEvent? {
        val id = obj.optString("id").trim()
        val kind = enumValueOrNull<BaCalendarPoolUnreadKind>(obj.optString("kind")) ?: return null
        val changedAtMillis = obj.optLong("changedAtMillis", 0L)
        val changeCount = obj.optInt("changeCount", 0)
        if (id.isBlank() || changedAtMillis <= 0L || changeCount <= 0) return null
        return BaCalendarPoolUnreadEvent(
            id = id,
            serverIndex = obj.optInt("serverIndex", 0).coerceIn(0, 2),
            kind = kind,
            changedAtMillis = changedAtMillis,
            changeCount = changeCount.coerceAtLeast(1),
            fingerprint = obj.optLong("fingerprint", 0L).coerceAtLeast(0L),
            detail = obj.optString("detail").trim(),
        )
    }

    private fun BaCalendarPoolUnreadEvent.normalizedForStore(): BaCalendarPoolUnreadEvent? {
        val normalizedChangedAtMillis = changedAtMillis.coerceAtLeast(0L)
        val normalizedChangeCount = changeCount.coerceAtLeast(1)
        val normalizedFingerprint = fingerprint.coerceAtLeast(0L)
        val normalizedServerIndex = serverIndex.coerceIn(0, 2)
        val normalizedId =
            id.trim()
                .ifBlank {
                    "${normalizedServerIndex}|${kind.name}|$normalizedFingerprint"
                }
        if (normalizedChangedAtMillis <= 0L || normalizedChangeCount <= 0) return null
        return copy(
            id = normalizedId,
            serverIndex = normalizedServerIndex,
            changedAtMillis = normalizedChangedAtMillis,
            changeCount = normalizedChangeCount,
            fingerprint = normalizedFingerprint,
            detail = detail.trim(),
        )
    }

    private fun loadServerLongMap(
        obj: JsonObject,
        prefix: String,
    ): Map<Int, Long> =
        (0..2)
            .mapNotNull { serverIndex ->
                val value = obj.optLong("${prefix}_$serverIndex", 0L).coerceAtLeast(0L)
                if (value > 0L) serverIndex to value else null
            }.toMap()

    private fun kotlinx.serialization.json.JsonObjectBuilder.putServerLongMap(
        prefix: String,
        values: Map<Int, Long>,
    ) {
        (0..2).forEach { serverIndex ->
            val value = values[serverIndex]?.coerceAtLeast(0L) ?: 0L
            if (value > 0L) {
                put("${prefix}_$serverIndex", JsonPrimitive(value))
            }
        }
    }

    private fun phaseKey(
        kind: BaCalendarPoolUnreadKind,
        serverIndex: Int,
    ): String {
        val normalizedServerIndex = serverIndex.coerceIn(0, 2)
        return when (kind) {
            BaCalendarPoolUnreadKind.Calendar -> "$KEY_CALENDAR_PHASE_PREFIX$normalizedServerIndex"
            BaCalendarPoolUnreadKind.Pool -> "$KEY_POOL_PHASE_PREFIX$normalizedServerIndex"
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(raw: String): T? =
        enumValues<T>().firstOrNull { value -> value.name == raw.trim() }
}
