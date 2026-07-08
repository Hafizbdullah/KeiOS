package os.kei.feature.keepalive.accessibility

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import os.kei.core.json.KeiJson
import os.kei.core.json.encodeCompact
import os.kei.core.json.optArray
import os.kei.core.json.optInt
import os.kei.core.json.optLong
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull

private const val HISTORY_EXPORT_FORMAT = "keios.keepalive.accessibility-guard-history"
private const val HISTORY_EXPORT_SCHEMA_VERSION = 1
private const val DEFAULT_MAX_ENTRIES = 500
private const val DEFAULT_MAX_BYTES = 1L * 1024L * 1024L
private const val MAX_TRIGGER_ACTION_LENGTH = 80
private const val MAX_SHIZUKU_STATUS_LENGTH = 160
private const val MAX_FAILURE_REASON_LENGTH = 512
private const val MAX_SERVICE_IDS_PER_ENTRY = 64

data class AccessibilityGuardHistoryExportResult(
    val exportedCount: Int,
    val byteCount: Int,
)

class AccessibilityGuardHistoryStore(
    private val historyFile: File,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {
    suspend fun append(entry: AccessibilityGuardHistoryEntry) {
        synchronized(fileLock) {
            val records = readRecordsLocked() + entry.normalizedForHistory()
            writeRecordsLocked(trimRecords(records))
        }
    }

    suspend fun latest(limit: Int): List<AccessibilityGuardHistoryEntry> =
        synchronized(fileLock) {
            readRecordsLocked()
                .sortedByDescending { entry -> entry.timestampMs }
                .take(limit.coerceIn(1, maxEntries.coerceAtLeast(1)))
        }

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
    ): Result<AccessibilityGuardHistoryExportResult> =
        runCatching {
            val records =
                synchronized(fileLock) {
                    readRecordsLocked().sortedByDescending { entry -> entry.timestampMs }
                }
            val content = buildExportJson(records = records, exportedAtMillis = clockMs())
            val bytes = content.toByteArray(Charsets.UTF_8)
            val output =
                context.contentResolver.openOutputStream(uri, "w")
                    ?: error("Unable to open output stream for $uri")
            output.use { stream -> stream.write(bytes) }
            AccessibilityGuardHistoryExportResult(
                exportedCount = records.size,
                byteCount = bytes.size,
            )
        }

    private fun readRecordsLocked(): List<AccessibilityGuardHistoryEntry> {
        if (!historyFile.isFile) return emptyList()
        return historyFile
            .readLines(Charsets.UTF_8)
            .mapNotNull(::decodeEntry)
    }

    private fun writeRecordsLocked(records: List<AccessibilityGuardHistoryEntry>) {
        historyFile.parentFile?.mkdirs()
        val ordered = records.sortedBy { entry -> entry.timestampMs }
        val content =
            ordered.joinToString(separator = "\n", postfix = if (ordered.isEmpty()) "" else "\n") { entry ->
                encodeEntry(entry.normalizedForHistory()).encodeCompact()
            }
        val temp = File(historyFile.parentFile ?: historyFile.absoluteFile.parentFile, "${historyFile.name}.tmp")
        temp.writeText(content, Charsets.UTF_8)
        if (!temp.renameTo(historyFile)) {
            temp.copyTo(historyFile, overwrite = true)
            temp.delete()
        }
    }

    private fun trimRecords(records: List<AccessibilityGuardHistoryEntry>): List<AccessibilityGuardHistoryEntry> {
        val newestFirst =
            records
                .map { entry -> entry.normalizedForHistory() }
                .sortedByDescending { entry -> entry.timestampMs }
                .take(maxEntries.coerceAtLeast(1))
                .toMutableList()
        while (newestFirst.size > 1 && encodedJsonlByteCount(newestFirst) > maxBytes.coerceAtLeast(1L)) {
            newestFirst.removeLast()
        }
        return newestFirst.sortedBy { entry -> entry.timestampMs }
    }

    private fun encodedJsonlByteCount(records: List<AccessibilityGuardHistoryEntry>): Long =
        records.sumOf { entry ->
            encodeEntry(entry).encodeCompact().toByteArray(Charsets.UTF_8).size.toLong() + 1L
        }

    companion object {
        private val fileLock = Any()

        fun forContext(context: Context): AccessibilityGuardHistoryStore =
            AccessibilityGuardHistoryStore(
                historyFile = File(context.filesDir, "keepalive/accessibility-guard-history.jsonl"),
            )

        internal fun buildExportJson(
            records: List<AccessibilityGuardHistoryEntry>,
            exportedAtMillis: Long,
        ): String {
            val normalizedRecords =
                records
                    .map { entry -> entry.normalizedForHistory() }
                    .sortedByDescending { entry -> entry.timestampMs }
            val summary = normalizedRecords.toSummary()
            return buildJsonObject {
                put("format", HISTORY_EXPORT_FORMAT)
                put("schemaVersion", HISTORY_EXPORT_SCHEMA_VERSION)
                put("syncScope", "local_only")
                put("exportedAtMillis", exportedAtMillis.coerceAtLeast(0L))
                put("summary", summary.toJson())
                put(
                    "records",
                    buildJsonArray {
                        normalizedRecords.forEach { entry -> add(encodeEntry(entry)) }
                    },
                )
            }.encodeCompact(KeiJson.pretty)
        }

        internal fun encodeEntry(entry: AccessibilityGuardHistoryEntry): JsonObject =
            buildJsonObject {
                val normalized = entry.normalizedForHistory()
                put("id", normalized.id)
                put("timestampMs", normalized.timestampMs)
                put("reason", normalized.reason.name)
                put("status", normalized.status.name)
                put("triggerAction", normalized.triggerAction)
                put("selectedCount", normalized.selectedCount)
                put("restoredCount", normalized.restoredCount)
                put("skippedCount", normalized.skippedCount)
                put("elapsedMs", normalized.elapsedMs)
                put("shizukuStatus", normalized.shizukuStatus)
                put("failureReason", normalized.failureReason)
                put(
                    "serviceIds",
                    buildJsonArray {
                        normalized.serviceIds.forEach { id -> add(id.toJson()) }
                    },
                )
            }

        internal fun decodeEntry(raw: String): AccessibilityGuardHistoryEntry? {
            if (raw.isBlank()) return null
            return runCatching {
                val obj = raw.parseJsonObjectOrNull() ?: return@runCatching null
                val timestampMs = obj.optLong("timestampMs", 0L)
                if (timestampMs <= 0L) return@runCatching null
                AccessibilityGuardHistoryEntry(
                    id = obj.optString("id").trim().ifBlank { UUID.randomUUID().toString() },
                    timestampMs = timestampMs,
                    reason = enumValueOrDefault(obj.optString("reason"), AccessibilityGuardRestoreReason.Manual),
                    status = enumValueOrDefault(obj.optString("status"), AccessibilityGuardRestoreStatus.Failed),
                    triggerAction = obj.optString("triggerAction").trim(),
                    selectedCount = obj.optInt("selectedCount", 0),
                    restoredCount = obj.optInt("restoredCount", 0),
                    skippedCount = obj.optInt("skippedCount", 0),
                    elapsedMs = obj.optLong("elapsedMs", 0L),
                    shizukuStatus = obj.optString("shizukuStatus").trim(),
                    failureReason = obj.optString("failureReason").trim(),
                    serviceIds = obj.optArray("serviceIds").toServiceIds(),
                ).normalizedForHistory()
            }.getOrNull()
        }
    }
}

fun AccessibilityGuardHistoryEntry.Companion.fromResult(
    result: AccessibilityGuardRestoreResult,
    id: String = UUID.randomUUID().toString(),
    triggerAction: String = result.reason.name,
): AccessibilityGuardHistoryEntry =
    AccessibilityGuardHistoryEntry(
        id = id,
        timestampMs = result.finishedAtMs.takeIf { it > 0L } ?: result.startedAtMs,
        reason = result.reason,
        status = result.status,
        triggerAction = triggerAction,
        selectedCount = result.selectedIds.size,
        restoredCount = result.restoredIds.size,
        skippedCount = result.skippedIds.size,
        elapsedMs = result.elapsedMs,
        shizukuStatus = result.shizukuStatus,
        failureReason = result.failureReason,
        serviceIds = result.selectedIds.sortedServiceIds(),
    ).normalizedForHistory()

private data class AccessibilityGuardHistorySummary(
    val storedCount: Int,
    val restoredRecordCount: Int,
    val skippedRecordCount: Int,
    val failedRecordCount: Int,
    val timedOutRecordCount: Int,
    val restoredServiceCount: Int,
    val skippedServiceCount: Int,
    val latestTimestampMs: Long,
)

private fun List<AccessibilityGuardHistoryEntry>.toSummary(): AccessibilityGuardHistorySummary =
    AccessibilityGuardHistorySummary(
        storedCount = size,
        restoredRecordCount = count { entry -> entry.status == AccessibilityGuardRestoreStatus.Restored },
        skippedRecordCount = count { entry -> entry.status.isSkippedStatus() },
        failedRecordCount = count { entry -> entry.status == AccessibilityGuardRestoreStatus.Failed },
        timedOutRecordCount = count { entry -> entry.status == AccessibilityGuardRestoreStatus.TimedOut },
        restoredServiceCount = sumOf { entry -> entry.restoredCount },
        skippedServiceCount = sumOf { entry -> entry.skippedCount },
        latestTimestampMs = maxOfOrNull { entry -> entry.timestampMs } ?: 0L,
    )

private fun AccessibilityGuardHistorySummary.toJson(): JsonObject =
    buildJsonObject {
        put("storedCount", storedCount)
        put("restoredRecordCount", restoredRecordCount)
        put("skippedRecordCount", skippedRecordCount)
        put("failedRecordCount", failedRecordCount)
        put("timedOutRecordCount", timedOutRecordCount)
        put("restoredServiceCount", restoredServiceCount)
        put("skippedServiceCount", skippedServiceCount)
        put("latestTimestampMs", latestTimestampMs)
    }

private fun AccessibilityGuardRestoreStatus.isSkippedStatus(): Boolean =
    when (this) {
        AccessibilityGuardRestoreStatus.SkippedNoTargets,
        AccessibilityGuardRestoreStatus.SkippedMissingPrivilege,
        AccessibilityGuardRestoreStatus.SkippedAlreadyEnabled,
        AccessibilityGuardRestoreStatus.SkippedCooldown,
        -> true
        AccessibilityGuardRestoreStatus.Restored,
        AccessibilityGuardRestoreStatus.Failed,
        AccessibilityGuardRestoreStatus.TimedOut,
        -> false
    }

private fun AccessibilityGuardHistoryEntry.normalizedForHistory(): AccessibilityGuardHistoryEntry =
    copy(
        id = id.trim().ifBlank { UUID.randomUUID().toString() },
        timestampMs = timestampMs.coerceAtLeast(0L),
        triggerAction = triggerAction.compactHistoryText(MAX_TRIGGER_ACTION_LENGTH),
        selectedCount = selectedCount.coerceAtLeast(0),
        restoredCount = restoredCount.coerceAtLeast(0),
        skippedCount = skippedCount.coerceAtLeast(0),
        elapsedMs = elapsedMs.coerceAtLeast(0L),
        shizukuStatus = shizukuStatus.compactHistoryText(MAX_SHIZUKU_STATUS_LENGTH),
        failureReason = failureReason.compactHistoryText(MAX_FAILURE_REASON_LENGTH),
        serviceIds = serviceIds.sortedServiceIds().take(MAX_SERVICE_IDS_PER_ENTRY),
    )

private fun AccessibilityServiceId.toJson(): JsonObject =
    buildJsonObject {
        put("packageName", packageName.trim())
        put("serviceName", serviceName.trim())
        put("flattened", flatten())
    }

private fun JsonArray?.toServiceIds(): List<AccessibilityServiceId> =
    this
        ?.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val packageName = obj.optString("packageName").trim()
            val serviceName = obj.optString("serviceName").trim()
            if (packageName.isBlank() || serviceName.isBlank()) {
                obj.optString("flattened").toAccessibilityServiceIdOrNull()
            } else {
                AccessibilityServiceId(packageName = packageName, serviceName = serviceName)
            }
        }
        ?.sortedServiceIds()
        .orEmpty()

private fun List<AccessibilityServiceId>.sortedServiceIds(): List<AccessibilityServiceId> =
    distinct()
        .sortedWith(compareBy<AccessibilityServiceId> { it.packageName }.thenBy { it.serviceName })

private fun Set<AccessibilityServiceId>.sortedServiceIds(): List<AccessibilityServiceId> =
    toList().sortedServiceIds()

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    raw: String,
    defaultValue: T,
): T =
    enumValues<T>().firstOrNull { value -> value.name.equals(raw.trim(), ignoreCase = true) } ?: defaultValue

private fun String.compactHistoryText(limit: Int): String {
    val compact =
        replace('\n', ' ')
            .replace('\r', ' ')
            .replace('\t', ' ')
            .trim()
    if (compact.length <= limit) return compact
    return compact.take(limit).trimEnd() + "..."
}
