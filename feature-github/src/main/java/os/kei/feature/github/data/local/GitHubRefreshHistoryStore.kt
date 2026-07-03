package os.kei.feature.github.data.local

import com.tencent.mmkv.MMKV
import java.security.MessageDigest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import os.kei.core.json.encodeCompact
import os.kei.core.json.optArray
import os.kei.core.json.optInt
import os.kei.core.json.optLong
import os.kei.core.json.optObject
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.core.prefs.KeiMmkv
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.model.GitHubRefreshHistoryFailureSummary
import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import os.kei.feature.github.model.GitHubRefreshHistoryRecord

object GitHubRefreshHistoryStore {
    private const val KV_ID = "github_refresh_history"
    private const val KEY_INDEX = "entry_index"
    internal const val MAX_RECORDS = 240

    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }

    private fun kv(): MMKV = store

    fun recordRefresh(record: GitHubRefreshHistoryRecord) {
        val normalized = record.normalizedForStorage()
        if (normalized.startedAtMillis <= 0L || normalized.finishedAtMillis <= 0L) return
        val kv = kv()
        val id = normalized.id.ifBlank { recordId(normalized) }
        val stored = normalized.copy(id = id)
        kv.encode(entryStoreKey(id), encodeRecord(stored).encodeCompact())
        val index = loadIndex(kv)
        index.remove(id)
        index.add(id)
        trimIndex(index, kv)
        saveIndex(index, kv)
    }

    fun load(): List<GitHubRefreshHistoryRecord> {
        val kv = kv()
        return loadIndex(kv)
            .mapNotNull { id ->
                decodeRecord(kv.decodeString(entryStoreKey(id)).orEmpty())
            }
            .sortedByDescending { record ->
                record.finishedAtMillis.takeIf { it > 0L } ?: record.startedAtMillis
            }
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

    internal fun encodeRecord(record: GitHubRefreshHistoryRecord): JsonObject {
        return buildJsonObject {
            put("id", record.id)
            put("sessionId", record.sessionId)
            put("scope", record.scope.name)
            put("source", record.source.name)
            put("outcome", record.outcome.name)
            put("totalTrackedCount", record.totalTrackedCount)
            put("targetCount", record.targetCount)
            put("completedCount", record.completedCount)
            put("updatableCount", record.updatableCount)
            put("preReleaseUpdateCount", record.preReleaseUpdateCount)
            put("failedCount", record.failedCount)
            put("startedAtMillis", record.startedAtMillis)
            put("finishedAtMillis", record.finishedAtMillis)
            put("elapsedMs", record.elapsedMs)
            put("p50ItemMs", record.p50ItemMs)
            put("p95ItemMs", record.p95ItemMs)
            put("maxItemMs", record.maxItemMs)
            put("note", record.note)
            put(
                "failureSummaries",
                buildJsonArray {
                    record.failureSummaries.forEach { failure ->
                        add(encodeFailureSummary(failure))
                    }
                },
            )
        }
    }

    internal fun decodeRecord(raw: String): GitHubRefreshHistoryRecord? {
        if (raw.isBlank()) return null
        return runCatching {
            val obj = raw.parseJsonObjectOrNull() ?: return@runCatching null
            val sessionId = obj.optLong("sessionId", 0L)
            val startedAtMillis = obj.optLong("startedAtMillis", 0L)
            val finishedAtMillis = obj.optLong("finishedAtMillis", 0L)
            if (startedAtMillis <= 0L || finishedAtMillis <= 0L) return@runCatching null
            GitHubRefreshHistoryRecord(
                id = obj.optString("id").trim(),
                sessionId = sessionId,
                scope = enumValueOrDefault(obj.optString("scope"), GitHubRefreshScope.AllTracked),
                source = enumValueOrDefault(obj.optString("source"), GitHubRefreshSource.Page),
                outcome = enumValueOrDefault(obj.optString("outcome"), GitHubRefreshHistoryOutcome.Completed),
                totalTrackedCount = obj.optInt("totalTrackedCount", 0).coerceAtLeast(0),
                targetCount = obj.optInt("targetCount", 0).coerceAtLeast(0),
                completedCount = obj.optInt("completedCount", 0).coerceAtLeast(0),
                updatableCount = obj.optInt("updatableCount", 0).coerceAtLeast(0),
                preReleaseUpdateCount = obj.optInt("preReleaseUpdateCount", 0).coerceAtLeast(0),
                failedCount = obj.optInt("failedCount", 0).coerceAtLeast(0),
                startedAtMillis = startedAtMillis,
                finishedAtMillis = finishedAtMillis,
                elapsedMs = obj.optLong("elapsedMs", 0L).coerceAtLeast(0L),
                p50ItemMs = obj.optLong("p50ItemMs", 0L).coerceAtLeast(0L),
                p95ItemMs = obj.optLong("p95ItemMs", 0L).coerceAtLeast(0L),
                maxItemMs = obj.optLong("maxItemMs", 0L).coerceAtLeast(0L),
                failureSummaries =
                    obj.optArray("failureSummaries")
                        ?.mapNotNull { element -> (element as? JsonObject)?.let(::decodeFailureSummary) }
                        .orEmpty(),
                note = obj.optString("note").trim(),
            )
        }.getOrNull()
    }

    internal fun GitHubRefreshHistoryRecord.normalizedForStorage(): GitHubRefreshHistoryRecord {
        val normalizedStarted = startedAtMillis.coerceAtLeast(0L)
        val normalizedFinished =
            finishedAtMillis
                .takeIf { it > 0L }
                ?.coerceAtLeast(normalizedStarted)
                ?: normalizedStarted
        return copy(
            id = id.trim(),
            totalTrackedCount = totalTrackedCount.coerceAtLeast(0),
            targetCount = targetCount.coerceAtLeast(0),
            completedCount = completedCount.coerceAtLeast(0),
            updatableCount = updatableCount.coerceAtLeast(0),
            preReleaseUpdateCount = preReleaseUpdateCount.coerceAtLeast(0),
            failedCount = failedCount.coerceAtLeast(0),
            startedAtMillis = normalizedStarted,
            finishedAtMillis = normalizedFinished,
            elapsedMs = elapsedMs.takeIf { it > 0L } ?: (normalizedFinished - normalizedStarted).coerceAtLeast(0L),
            p50ItemMs = p50ItemMs.coerceAtLeast(0L),
            p95ItemMs = p95ItemMs.coerceAtLeast(0L),
            maxItemMs = maxItemMs.coerceAtLeast(0L),
            failureSummaries =
                failureSummaries
                    .take(12)
                    .map { failure ->
                        failure.copy(
                            trackId = failure.trackId.trim(),
                            owner = failure.owner.trim(),
                            repo = failure.repo.trim(),
                            packageName = failure.packageName.trim(),
                            appLabel = failure.appLabel.trim(),
                            sourceMode = failure.sourceMode.trim(),
                            message = compactHistoryText(failure.message, 320),
                            elapsedMs = failure.elapsedMs.coerceAtLeast(0L),
                        )
                    },
            note = compactHistoryText(note, 320),
        )
    }

    internal fun recordId(record: GitHubRefreshHistoryRecord): String {
        val sessionPart = if (record.sessionId > 0L) record.sessionId.toString() else ""
        return sha1(
            listOf(
                sessionPart,
                record.scope.name,
                record.source.name,
                record.startedAtMillis.toString(),
                record.finishedAtMillis.toString(),
            ).joinToString("|"),
        )
    }

    internal fun shouldPruneBefore(
        record: GitHubRefreshHistoryRecord,
        cutoffMillis: Long,
    ): Boolean {
        val finishedAt = record.finishedAtMillis.takeIf { it > 0L } ?: record.startedAtMillis
        return cutoffMillis > 0L && finishedAt in 1 until cutoffMillis
    }

    private fun encodeFailureSummary(failure: GitHubRefreshHistoryFailureSummary): JsonObject {
        return buildJsonObject {
            put("trackId", failure.trackId)
            put("owner", failure.owner)
            put("repo", failure.repo)
            put("packageName", failure.packageName)
            put("appLabel", failure.appLabel)
            put("sourceMode", failure.sourceMode)
            put("message", failure.message)
            put("elapsedMs", failure.elapsedMs)
        }
    }

    private fun decodeFailureSummary(obj: JsonObject): GitHubRefreshHistoryFailureSummary =
        GitHubRefreshHistoryFailureSummary(
            trackId = obj.optString("trackId").trim(),
            owner = obj.optString("owner").trim(),
            repo = obj.optString("repo").trim(),
            packageName = obj.optString("packageName").trim(),
            appLabel = obj.optString("appLabel").trim(),
            sourceMode = obj.optString("sourceMode").trim(),
            message = obj.optString("message").trim(),
            elapsedMs = obj.optLong("elapsedMs", 0L).coerceAtLeast(0L),
        )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        raw: String,
        defaultValue: T,
    ): T =
        runCatching { enumValueOf<T>(raw.trim()) }.getOrDefault(defaultValue)

    private fun trimIndex(index: MutableSet<String>, kv: MMKV) {
        if (index.size <= MAX_RECORDS) return
        val sorted =
            index
                .mapNotNull { id ->
                    val record = decodeRecord(kv.decodeString(entryStoreKey(id)).orEmpty())
                    record?.let { id to (it.finishedAtMillis.takeIf { time -> time > 0L } ?: it.startedAtMillis) }
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
