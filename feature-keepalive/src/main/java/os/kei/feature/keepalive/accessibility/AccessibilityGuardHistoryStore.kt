package os.kei.feature.keepalive.accessibility

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
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
private const val HISTORY_EXPORT_SCHEMA_VERSION = 2
private const val DEFAULT_MAX_ENTRIES = 500
private const val DEFAULT_MAX_BYTES = 1L * 1024L * 1024L
private const val MAX_TRIGGER_ACTION_LENGTH = 80
private const val MAX_SHIZUKU_STATUS_LENGTH = 160
private const val MAX_FAILURE_REASON_LENGTH = 512
private const val STARTUP_HEALTHY_DEDUP_WINDOW_MS = 3_000L

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
            val records =
                (readRecordsLocked() + entry.normalizedForHistory())
                    .deduplicateStartupHealthyBursts()
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

        internal fun encodeEntry(entry: AccessibilityGuardHistoryEntry) =
            buildJsonObject {
                val normalized = entry.normalizedForHistory()
                put("id", normalized.id)
                put("timestampMs", normalized.timestampMs)
                put("reason", normalized.reason.name)
                put("status", normalized.status.name)
                put("triggerAction", normalized.triggerAction)
                put("checkCount", normalized.checkCount)
                put("healthyCount", normalized.healthyCount)
                put("warningCount", normalized.warningCount)
                put("elapsedMs", normalized.elapsedMs)
                put("shizukuStatus", normalized.shizukuStatus)
                put("failureReason", normalized.failureReason)
            }

        internal fun decodeEntry(raw: String): AccessibilityGuardHistoryEntry? {
            if (raw.isBlank()) return null
            return runCatching {
                val obj = raw.parseJsonObjectOrNull() ?: return@runCatching null
                val timestampMs = obj.optLong("timestampMs", 0L)
                if (timestampMs <= 0L) return@runCatching null
                val status = obj.optString("status").toCheckStatus()
                val legacySelectedCount = obj.optInt("selectedCount", 0)
                val legacyRestoredCount = obj.optInt("restoredCount", 0)
                val legacySkippedCount = obj.optInt("skippedCount", 0)
                val legacyServiceCount = obj.optArray("serviceIds")?.size ?: 0
                val fallbackCheckCount =
                    maxOf(legacySelectedCount, legacyRestoredCount + legacySkippedCount, legacyServiceCount)
                AccessibilityGuardHistoryEntry(
                    id = obj.optString("id").trim().ifBlank { UUID.randomUUID().toString() },
                    timestampMs = timestampMs,
                    reason = obj.optString("reason").toCheckReason(),
                    status = status,
                    triggerAction = obj.optString("triggerAction").trim(),
                    checkCount = obj.optInt("checkCount", fallbackCheckCount),
                    healthyCount =
                        obj.optInt(
                            "healthyCount",
                            legacyHealthyCount(
                                status = status,
                                restoredCount = legacyRestoredCount,
                                checkCount = fallbackCheckCount,
                            ),
                        ),
                    warningCount =
                        obj.optInt(
                            "warningCount",
                            legacyWarningCount(
                                status = status,
                                skippedCount = legacySkippedCount,
                                checkCount = fallbackCheckCount,
                            ),
                        ),
                    elapsedMs = obj.optLong("elapsedMs", 0L),
                    shizukuStatus = obj.optString("shizukuStatus").trim(),
                    failureReason = obj.optString("failureReason").trim(),
                ).normalizedForHistory()
            }.getOrNull()
        }
    }
}

fun AccessibilityGuardHistoryEntry.Companion.fromResult(
    result: AccessibilityGuardCheckResult,
    id: String = UUID.randomUUID().toString(),
    triggerAction: String = result.reason.name,
): AccessibilityGuardHistoryEntry =
    AccessibilityGuardHistoryEntry(
        id = id,
        timestampMs = result.finishedAtMs.takeIf { it > 0L } ?: result.startedAtMs,
        reason = result.reason,
        status = result.status,
        triggerAction = triggerAction,
        checkCount = result.checkCount,
        healthyCount = result.healthyCount,
        warningCount = result.warningCount,
        elapsedMs = result.elapsedMs,
        shizukuStatus = result.shizukuStatus,
        failureReason = result.failureReason,
    ).normalizedForHistory()

private data class AccessibilityGuardHistorySummary(
    val storedCount: Int,
    val healthyRecordCount: Int,
    val checkedRecordCount: Int,
    val warningRecordCount: Int,
    val failedRecordCount: Int,
    val timedOutRecordCount: Int,
    val checkedItemCount: Int,
    val healthyItemCount: Int,
    val warningItemCount: Int,
    val latestTimestampMs: Long,
)

private fun List<AccessibilityGuardHistoryEntry>.toSummary(): AccessibilityGuardHistorySummary =
    AccessibilityGuardHistorySummary(
        storedCount = size,
        healthyRecordCount = count { entry -> entry.status == AccessibilityGuardCheckStatus.Healthy },
        checkedRecordCount = count { entry -> entry.status == AccessibilityGuardCheckStatus.Checked },
        warningRecordCount = count { entry -> entry.status == AccessibilityGuardCheckStatus.MissingPrivilege },
        failedRecordCount = count { entry -> entry.status == AccessibilityGuardCheckStatus.Failed },
        timedOutRecordCount = count { entry -> entry.status == AccessibilityGuardCheckStatus.TimedOut },
        checkedItemCount = sumOf { entry -> entry.checkCount },
        healthyItemCount = sumOf { entry -> entry.healthyCount },
        warningItemCount = sumOf { entry -> entry.warningCount },
        latestTimestampMs = maxOfOrNull { entry -> entry.timestampMs } ?: 0L,
    )

private fun AccessibilityGuardHistorySummary.toJson() =
    buildJsonObject {
        put("storedCount", storedCount)
        put("healthyRecordCount", healthyRecordCount)
        put("checkedRecordCount", checkedRecordCount)
        put("warningRecordCount", warningRecordCount)
        put("failedRecordCount", failedRecordCount)
        put("timedOutRecordCount", timedOutRecordCount)
        put("checkedItemCount", checkedItemCount)
        put("healthyItemCount", healthyItemCount)
        put("warningItemCount", warningItemCount)
        put("latestTimestampMs", latestTimestampMs)
    }

private fun AccessibilityGuardHistoryEntry.normalizedForHistory(): AccessibilityGuardHistoryEntry {
    val normalizedCheckCount = checkCount.coerceAtLeast(0)
    return copy(
        id = id.trim().ifBlank { UUID.randomUUID().toString() },
        timestampMs = timestampMs.coerceAtLeast(0L),
        triggerAction = triggerAction.compactHistoryText(MAX_TRIGGER_ACTION_LENGTH),
        checkCount = normalizedCheckCount,
        healthyCount = healthyCount.coerceIn(0, normalizedCheckCount),
        warningCount = warningCount.coerceAtLeast(0),
        elapsedMs = elapsedMs.coerceAtLeast(0L),
        shizukuStatus = shizukuStatus.compactHistoryText(MAX_SHIZUKU_STATUS_LENGTH),
        failureReason = failureReason.compactHistoryText(MAX_FAILURE_REASON_LENGTH),
    )
}

private fun List<AccessibilityGuardHistoryEntry>.deduplicateStartupHealthyBursts():
    List<AccessibilityGuardHistoryEntry> {
    if (size < 2) return this
    val merged = mutableListOf<AccessibilityGuardHistoryEntry>()
    sortedBy { entry -> entry.timestampMs }.forEach { entry ->
        val previous = merged.lastOrNull()
        if (previous != null && previous.isSameStartupHealthyBurst(entry)) {
            merged[merged.lastIndex] = entry
        } else {
            merged += entry
        }
    }
    return merged
}

private fun AccessibilityGuardHistoryEntry.isSameStartupHealthyBurst(
    other: AccessibilityGuardHistoryEntry,
): Boolean {
    if (!isStartupHealthySnapshot() || !other.isStartupHealthySnapshot()) return false
    val gapMs = kotlin.math.abs(other.timestampMs - timestampMs)
    if (gapMs > STARTUP_HEALTHY_DEDUP_WINDOW_MS) return false
    return checkCount == other.checkCount &&
        healthyCount == other.healthyCount &&
        warningCount == other.warningCount &&
        shizukuStatus == other.shizukuStatus
}

private fun AccessibilityGuardHistoryEntry.isStartupHealthySnapshot(): Boolean =
    status == AccessibilityGuardCheckStatus.Healthy &&
        healthyCount == checkCount &&
        warningCount == 0 &&
        failureReason.isBlank() &&
        reason.isStartupRecoveryReason()

private fun AccessibilityGuardCheckReason.isStartupRecoveryReason(): Boolean =
    when (this) {
        AccessibilityGuardCheckReason.ForegroundServiceStart,
        AccessibilityGuardCheckReason.BootCompleted,
        AccessibilityGuardCheckReason.PackageReplaced,
        -> true
        AccessibilityGuardCheckReason.Manual,
        AccessibilityGuardCheckReason.SecureSettingChanged,
        AccessibilityGuardCheckReason.ScreenOn,
        AccessibilityGuardCheckReason.TimeoutRecovery,
        -> false
    }

private fun String.toCheckReason(): AccessibilityGuardCheckReason =
    enumValueOrDefault(this, AccessibilityGuardCheckReason.Manual)

private fun String.toCheckStatus(): AccessibilityGuardCheckStatus {
    val raw = trim()
    return enumValues<AccessibilityGuardCheckStatus>()
        .firstOrNull { value -> value.name.equals(raw, ignoreCase = true) }
        ?: legacyCheckStatus(raw)
}

private fun legacyCheckStatus(raw: String): AccessibilityGuardCheckStatus =
    when {
        raw.equals("Restored", ignoreCase = true) ||
            raw.equals("SkippedAlreadyEnabled", ignoreCase = true) -> AccessibilityGuardCheckStatus.Healthy
        raw.equals("SkippedMissingPrivilege", ignoreCase = true) -> AccessibilityGuardCheckStatus.MissingPrivilege
        raw.equals("Failed", ignoreCase = true) -> AccessibilityGuardCheckStatus.Failed
        raw.equals("TimedOut", ignoreCase = true) -> AccessibilityGuardCheckStatus.TimedOut
        else -> AccessibilityGuardCheckStatus.Checked
    }

private fun legacyHealthyCount(
    status: AccessibilityGuardCheckStatus,
    restoredCount: Int,
    checkCount: Int,
): Int =
    when (status) {
        AccessibilityGuardCheckStatus.Healthy -> maxOf(restoredCount, checkCount)
        AccessibilityGuardCheckStatus.Checked -> checkCount
        AccessibilityGuardCheckStatus.MissingPrivilege,
        AccessibilityGuardCheckStatus.Failed,
        AccessibilityGuardCheckStatus.TimedOut,
        -> restoredCount
    }

private fun legacyWarningCount(
    status: AccessibilityGuardCheckStatus,
    skippedCount: Int,
    checkCount: Int,
): Int =
    when (status) {
        AccessibilityGuardCheckStatus.MissingPrivilege,
        AccessibilityGuardCheckStatus.Failed,
        AccessibilityGuardCheckStatus.TimedOut,
        -> maxOf(skippedCount, checkCount)
        AccessibilityGuardCheckStatus.Healthy,
        AccessibilityGuardCheckStatus.Checked,
        -> skippedCount
    }

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
