package os.kei.feature.github.data.local

import com.tencent.mmkv.MMKV
import java.security.MessageDigest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import os.kei.core.json.encodeCompact
import os.kei.core.json.optArray
import os.kei.core.json.optLong
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.core.prefs.KeiMmkv
import os.kei.feature.github.model.GitHubTrackChangeField
import os.kei.feature.github.model.GitHubTrackChangeHistoryAction
import os.kei.feature.github.model.GitHubTrackChangeHistoryRecord
import os.kei.feature.github.model.GitHubTrackChangeHistorySource
import os.kei.feature.github.model.GitHubTrackedSourceMode

object GitHubTrackChangeHistoryStore {
    private const val KV_ID = "github_track_change_history"
    private const val KEY_INDEX = "entry_index"
    internal const val MAX_RECORDS = 240

    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }

    private fun kv(): MMKV = store

    fun recordChange(record: GitHubTrackChangeHistoryRecord) {
        recordChanges(listOf(record))
    }

    fun recordChanges(records: List<GitHubTrackChangeHistoryRecord>) {
        val normalizedRecords =
            records
                .map { record -> record.normalizedForStorage() }
                .filter { record -> record.changedAtMillis > 0L && record.trackId.isNotBlank() }
        if (normalizedRecords.isEmpty()) return
        val kv = kv()
        val index = loadIndex(kv)
        normalizedRecords.forEach { normalized ->
            val id = normalized.id.ifBlank { recordId(normalized) }
            val stored = normalized.copy(id = id)
            kv.encode(entryStoreKey(id), encodeRecord(stored).encodeCompact())
            index.remove(id)
            index.add(id)
        }
        trimIndex(index, kv)
        saveIndex(index, kv)
    }

    fun load(): List<GitHubTrackChangeHistoryRecord> {
        val kv = kv()
        return loadIndex(kv)
            .mapNotNull { id ->
                decodeRecord(kv.decodeString(entryStoreKey(id)).orEmpty())
            }
            .sortedByDescending { record -> record.changedAtMillis }
    }

    fun clear() {
        val kv = kv()
        loadIndex(kv).forEach { id -> kv.removeValueForKey(entryStoreKey(id)) }
        kv.removeValueForKey(KEY_INDEX)
        kv.trim()
    }

    fun pruneBefore(cutoffMillis: Long): Int {
        if (cutoffMillis <= 0L) return 0
        val kv = kv()
        val remaining = mutableSetOf<String>()
        var removedCount = 0
        loadIndex(kv).forEach { id ->
            val record = decodeRecord(kv.decodeString(entryStoreKey(id)).orEmpty())
            if (record != null && shouldPruneBefore(record, cutoffMillis)) {
                kv.removeValueForKey(entryStoreKey(id))
                removedCount += 1
            } else {
                remaining += id
            }
        }
        saveIndex(remaining, kv)
        if (removedCount > 0) kv.trim()
        return removedCount
    }

    fun cachedRecordCount(): Int = loadIndex().size

    internal fun encodeRecord(record: GitHubTrackChangeHistoryRecord): JsonObject {
        return buildJsonObject {
            put("id", record.id)
            put("trackId", record.trackId)
            put("previousTrackId", record.previousTrackId)
            put("action", record.action.name)
            put("source", record.source.name)
            put("changedAtMillis", record.changedAtMillis)
            put("owner", record.owner)
            put("repo", record.repo)
            put("repoUrl", record.repoUrl)
            put("packageName", record.packageName)
            put("appLabel", record.appLabel)
            put("sourceMode", record.sourceMode.name)
            put(
                "changedFields",
                buildJsonArray {
                    record.changedFields.forEach { field -> add(JsonPrimitive(field.name)) }
                },
            )
            put("note", record.note)
        }
    }

    internal fun decodeRecord(raw: String): GitHubTrackChangeHistoryRecord? {
        if (raw.isBlank()) return null
        return runCatching {
            val obj = raw.parseJsonObjectOrNull() ?: return@runCatching null
            val trackId = obj.optString("trackId").trim()
            val changedAtMillis = obj.optLong("changedAtMillis", 0L)
            if (trackId.isBlank() || changedAtMillis <= 0L) return@runCatching null
            GitHubTrackChangeHistoryRecord(
                id = obj.optString("id").trim(),
                trackId = trackId,
                previousTrackId = obj.optString("previousTrackId").trim(),
                action = enumValueOrDefault(
                    raw = obj.optString("action"),
                    defaultValue = GitHubTrackChangeHistoryAction.Updated,
                ),
                source = enumValueOrDefault(
                    raw = obj.optString("source"),
                    defaultValue = GitHubTrackChangeHistorySource.Page,
                ),
                changedAtMillis = changedAtMillis,
                owner = obj.optString("owner").trim(),
                repo = obj.optString("repo").trim(),
                repoUrl = obj.optString("repoUrl").trim(),
                packageName = obj.optString("packageName").trim(),
                appLabel = obj.optString("appLabel").trim(),
                sourceMode = enumValueOrDefault(
                    raw = obj.optString("sourceMode"),
                    defaultValue = GitHubTrackedSourceMode.GitHubRepository,
                ),
                changedFields =
                    obj.optArray("changedFields")
                        ?.mapNotNull { element ->
                            enumValueOrNull<GitHubTrackChangeField>(
                                element.jsonPrimitive.content,
                            )
                        }
                        .orEmpty(),
                note = obj.optString("note").trim(),
            )
        }.getOrNull()
    }

    internal fun GitHubTrackChangeHistoryRecord.normalizedForStorage(): GitHubTrackChangeHistoryRecord =
        copy(
            id = id.trim(),
            trackId = trackId.trim(),
            previousTrackId = previousTrackId.trim(),
            changedAtMillis = changedAtMillis.coerceAtLeast(0L),
            owner = compactHistoryText(owner, 120),
            repo = compactHistoryText(repo, 160),
            repoUrl = compactHistoryText(repoUrl, 360),
            packageName = compactHistoryText(packageName, 160),
            appLabel = compactHistoryText(appLabel, 160),
            changedFields = changedFields.distinct().take(16),
            note = compactHistoryText(note, 320),
        )

    internal fun recordId(record: GitHubTrackChangeHistoryRecord): String =
        sha1(
            listOf(
                record.action.name,
                record.source.name,
                record.trackId,
                record.previousTrackId,
                record.changedAtMillis.toString(),
                record.changedFields.joinToString(",") { it.name },
            ).joinToString("|"),
        )

    internal fun shouldPruneBefore(
        record: GitHubTrackChangeHistoryRecord,
        cutoffMillis: Long,
    ): Boolean = cutoffMillis > 0L && record.changedAtMillis in 1 until cutoffMillis

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        raw: String,
        defaultValue: T,
    ): T =
        enumValueOrNull<T>(raw) ?: defaultValue

    private inline fun <reified T : Enum<T>> enumValueOrNull(raw: String): T? =
        runCatching { enumValueOf<T>(raw.trim()) }.getOrNull()

    private fun trimIndex(index: MutableSet<String>, kv: MMKV) {
        if (index.size <= MAX_RECORDS) return
        val sorted =
            index
                .mapNotNull { id ->
                    val record = decodeRecord(kv.decodeString(entryStoreKey(id)).orEmpty())
                    record?.let { id to it.changedAtMillis }
                }
                .sortedByDescending { it.second }
        val keep = sorted.take(MAX_RECORDS).map { it.first }.toSet()
        index.filter { it !in keep }.forEach { id ->
            kv.removeValueForKey(entryStoreKey(id))
        }
        index.retainAll(keep)
    }

    private fun entryStoreKey(id: String): String = "entry_$id"

    private fun loadIndex(kv: MMKV = store): MutableSet<String> {
        val raw = kv.decodeString(KEY_INDEX).orEmpty()
        if (raw.isBlank()) return mutableSetOf()
        return raw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
    }

    private fun saveIndex(index: Set<String>, kv: MMKV = store) {
        kv.encode(KEY_INDEX, index.filter { it.isNotBlank() }.joinToString(","))
    }

    private fun sha1(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        return digest.digest(text.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun compactHistoryText(
        raw: String,
        maxLength: Int,
    ): String =
        raw
            .lineSequence()
            .joinToString(" ") { it.trim() }
            .trim()
            .take(maxLength)
}
