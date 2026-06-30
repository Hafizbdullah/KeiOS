package os.kei.ui.page.main.student

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import os.kei.core.json.KeiJson
import os.kei.core.json.decodeFromStringOrNull
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

private const val PAYLOAD_ROOT_DIR_NAME = "ba_student_guide_payload_cache"
private const val PAYLOAD_VERSION_DIR_NAME = "v1"
private const val PAYLOAD_INDEX_FILE_NAME = "index.json"
private const val PAYLOAD_ENABLED_FILE_NAME = ".enabled"

@Serializable
private data class BaStudentGuidePayloadFileIndex(
    val sourceUrls: List<String> = emptyList(),
)

internal data class BaStudentGuidePayloadMigrationThresholds(
    val minMmkvPayloadBytes: Long = 2L * 1024L * 1024L,
    val minMmkvEntryCount: Int = 96,
)

internal data class BaStudentGuidePayloadStorageStats(
    val mmkvEntryCount: Int = 0,
    val fileEntryCount: Int = 0,
    val totalEntryCount: Int = 0,
    val mmkvPayloadBytes: Long = 0L,
    val filePayloadBytes: Long = 0L,
    val filePayloadEnabled: Boolean = false,
) {
    fun shouldMigrateToFiles(
        thresholds: BaStudentGuidePayloadMigrationThresholds = BaStudentGuidePayloadMigrationThresholds(),
    ): Boolean {
        if (filePayloadEnabled) return false
        if (mmkvEntryCount <= 0 || mmkvPayloadBytes <= 0L) return false
        return mmkvPayloadBytes >= thresholds.minMmkvPayloadBytes ||
            mmkvEntryCount >= thresholds.minMmkvEntryCount
    }
}

internal data class BaStudentGuidePayloadMigrationResult(
    val attempted: Boolean,
    val migratedEntryCount: Int,
    val skippedEntryCount: Int,
    val before: BaStudentGuidePayloadStorageStats,
    val after: BaStudentGuidePayloadStorageStats,
)

internal class BaStudentGuidePayloadFileCacheStore(
    private val rootDir: File,
) {
    private val lock = Any()

    fun isEnabled(): Boolean =
        synchronized(lock) {
            File(cacheRootDir(), PAYLOAD_ENABLED_FILE_NAME).isFile
        }

    fun setEnabled(enabled: Boolean) {
        synchronized(lock) {
            val marker = File(cacheRootDir().apply { mkdirs() }, PAYLOAD_ENABLED_FILE_NAME)
            if (enabled) {
                writeTextAtomic(marker, "1")
            } else {
                marker.delete()
            }
        }
    }

    fun savePayload(
        sourceUrl: String,
        payload: Map<String, String>,
    ): Boolean =
        synchronized(lock) {
            val source = normalizeStudentGuideSourceUrl(sourceUrl)
            if (source.isBlank()) return@synchronized false
            if (!BA_GUIDE_CACHE_REQUIRED_SUFFIXES.all { suffix -> payload[suffix].orEmpty().isNotBlank() }) {
                return@synchronized false
            }
            val dir = entryDir(source)
            runCatching { dir.deleteRecursively() }
            dir.mkdirs()
            BA_GUIDE_CACHE_REQUIRED_SUFFIXES.forEach { suffix ->
                writeTextAtomic(
                    target = File(dir, payloadFileName(suffix)),
                    text = payload[suffix].orEmpty(),
                )
            }
            val index = readIndexLocked().toMutableSet()
            index += source
            writeIndexLocked(index)
            true
        }

    fun loadPayload(sourceUrl: String): Map<String, String>? =
        synchronized(lock) {
            val source = normalizeStudentGuideSourceUrl(sourceUrl)
            if (source.isBlank()) return@synchronized null
            val dir = entryDir(source)
            if (!dir.isDirectory) return@synchronized null
            val payload =
                BA_GUIDE_CACHE_REQUIRED_SUFFIXES.associateWith { suffix ->
                    File(dir, payloadFileName(suffix)).readTextOrNull().orEmpty()
                }
            payload.takeIf { values -> values.values.all { it.isNotBlank() } }
        }

    fun contains(sourceUrl: String): Boolean =
        synchronized(lock) {
            val source = normalizeStudentGuideSourceUrl(sourceUrl)
            source.isNotBlank() && source in readIndexLocked()
        }

    fun remove(sourceUrl: String) {
        synchronized(lock) {
            val source = normalizeStudentGuideSourceUrl(sourceUrl)
            if (source.isBlank()) return
            runCatching { entryDir(source).deleteRecursively() }
            val index = readIndexLocked().toMutableSet()
            if (index.remove(source)) {
                writeIndexLocked(index)
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            runCatching { cacheRootDir().deleteRecursively() }
        }
    }

    fun sourceUrls(): Set<String> =
        synchronized(lock) {
            readIndexLocked()
        }

    fun entryCount(): Int = sourceUrls().size

    fun payloadBytes(): Long =
        synchronized(lock) {
            readIndexLocked().sumOf { source ->
                BA_GUIDE_CACHE_REQUIRED_SUFFIXES.sumOf { suffix ->
                    File(entryDir(source), payloadFileName(suffix)).length().coerceAtLeast(0L)
                }
            }
        }

    fun storageBytes(): Long =
        synchronized(lock) {
            cacheRootDir().totalFileBytes()
        }

    fun latestSyncedAtMs(): Long =
        synchronized(lock) {
            readIndexLocked().maxOfOrNull { source ->
                val metaRaw = File(entryDir(source), payloadFileName(CACHE_SUFFIX_META)).readTextOrNull().orEmpty()
                runCatching { JSONObject(metaRaw).optLong("syncedAtMs", 0L) }.getOrDefault(0L)
            } ?: 0L
        }

    private fun readIndexLocked(): Set<String> {
        val raw = File(cacheRootDir(), PAYLOAD_INDEX_FILE_NAME).readTextOrNull().orEmpty()
        return KeiJson.lenient
            .decodeFromStringOrNull<BaStudentGuidePayloadFileIndex>(raw)
            ?.sourceUrls
            .orEmpty()
            .asSequence()
            .map(::normalizeStudentGuideSourceUrl)
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun writeIndexLocked(index: Set<String>) {
        val root = cacheRootDir().apply { mkdirs() }
        val normalized =
            index
                .asSequence()
                .map(::normalizeStudentGuideSourceUrl)
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .toList()
        if (normalized.isEmpty()) {
            File(root, PAYLOAD_INDEX_FILE_NAME).delete()
            return
        }
        writeTextAtomic(
            target = File(root, PAYLOAD_INDEX_FILE_NAME),
            text = KeiJson.lenient.encodeToString(BaStudentGuidePayloadFileIndex(normalized)),
        )
    }

    private fun cacheRootDir(): File = File(rootDir, "$PAYLOAD_ROOT_DIR_NAME/$PAYLOAD_VERSION_DIR_NAME")

    private fun entryDir(sourceUrl: String): File = File(cacheRootDir(), payloadCacheId(sourceUrl))
}

internal fun baStudentGuidePayloadFileCacheStore(context: Context): BaStudentGuidePayloadFileCacheStore =
    BaStudentGuidePayloadFileCacheStore(context.filesDir)

private fun payloadFileName(suffix: String): String =
    suffix
        .lowercase(Locale.ROOT)
        .replace(Regex("""[^a-z0-9_]+"""), "_")
        .ifBlank { "payload" } + ".json"

private fun payloadCacheId(sourceUrl: String): String {
    val digest = MessageDigest.getInstance("SHA-1")
    return digest.digest(sourceUrl.toByteArray(StandardCharsets.UTF_8)).joinToString(separator = "") { byte ->
        String.format(Locale.ROOT, "%02x", byte.toInt() and 0xff)
    }
}

private fun File.readTextOrNull(): String? =
    runCatching {
        if (isFile) readText(Charsets.UTF_8) else null
    }.getOrNull()

private fun File.totalFileBytes(): Long =
    runCatching {
        if (!exists()) {
            0L
        } else {
            walkTopDown()
                .filter { file -> file.isFile }
                .sumOf { file -> file.length().coerceAtLeast(0L) }
        }
    }.getOrDefault(0L)

private fun writeTextAtomic(
    target: File,
    text: String,
) {
    val parent = target.parentFile ?: return
    parent.mkdirs()
    val temp = File(parent, "${target.name}.tmp")
    temp.writeText(text, Charsets.UTF_8)
    if (!temp.renameTo(target)) {
        temp.copyTo(target, overwrite = true)
        temp.delete()
    }
}
