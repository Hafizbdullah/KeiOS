package os.kei.feature.github.data.local

import com.tencent.mmkv.MMKV
import java.security.MessageDigest
import java.util.Locale
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import os.kei.core.json.encodeCompact
import os.kei.core.json.optBoolean
import os.kei.core.json.optInt
import os.kei.core.json.optLong
import os.kei.core.json.optObject
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.core.prefs.KeiMmkv
import os.kei.feature.github.model.GitHubAppInstallHistoryAction
import os.kei.feature.github.model.GitHubAppInstallHistoryRecord
import os.kei.feature.github.model.GitHubAppInstallHistorySource
import os.kei.feature.github.model.GitHubAppInstallSourceInfo
import os.kei.feature.github.model.GitHubTrackedAppInstallSnapshot
import os.kei.feature.github.model.GitHubTrackedSourceMode

object GitHubAppInstallHistoryStore {
    private const val KV_ID = "github_app_install_history"
    private const val KEY_INDEX = "entry_index"
    private const val KEY_SNAPSHOT_INDEX = "snapshot_index"
    internal const val MAX_RECORDS = 240

    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }

    private fun kv(): MMKV = store

    fun recordEvent(record: GitHubAppInstallHistoryRecord) {
        recordEvents(listOf(record))
    }

    fun recordEvents(records: List<GitHubAppInstallHistoryRecord>) {
        val normalizedRecords =
            records
                .map { record -> record.normalizedForStorage() }
                .filter { record ->
                    record.changedAtMillis > 0L &&
                        record.trackId.isNotBlank() &&
                        record.packageName.isNotBlank()
                }
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

    fun load(): List<GitHubAppInstallHistoryRecord> {
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
        loadSnapshotIndex(kv).forEach { packageName ->
            kv.removeValueForKey(snapshotStoreKey(packageName))
        }
        kv.removeValueForKey(KEY_INDEX)
        kv.removeValueForKey(KEY_SNAPSHOT_INDEX)
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

    fun loadSnapshot(packageName: String): GitHubTrackedAppInstallSnapshot? {
        val normalizedPackage = normalizePackageName(packageName)
        if (normalizedPackage.isBlank()) return null
        return decodeSnapshot(kv().decodeString(snapshotStoreKey(normalizedPackage)).orEmpty())
    }

    fun saveSnapshot(snapshot: GitHubTrackedAppInstallSnapshot) {
        val normalized = snapshot.normalizedForStorage()
        if (normalized.packageName.isBlank() || normalized.observedAtMillis <= 0L) return
        val kv = kv()
        val index = loadSnapshotIndex(kv)
        kv.encode(snapshotStoreKey(normalized.packageName), encodeSnapshot(normalized).encodeCompact())
        index += normalized.packageName
        saveSnapshotIndex(index, kv)
    }

    fun removeSnapshot(packageName: String) {
        val normalizedPackage = normalizePackageName(packageName)
        if (normalizedPackage.isBlank()) return
        val kv = kv()
        val index = loadSnapshotIndex(kv)
        kv.removeValueForKey(snapshotStoreKey(normalizedPackage))
        index.remove(normalizedPackage)
        saveSnapshotIndex(index, kv)
    }

    fun replaceSnapshots(snapshots: Collection<GitHubTrackedAppInstallSnapshot>) {
        val normalizedSnapshots =
            snapshots
                .map { snapshot -> snapshot.normalizedForStorage() }
                .filter { snapshot -> snapshot.packageName.isNotBlank() && snapshot.observedAtMillis > 0L }
                .associateBy { snapshot -> snapshot.packageName }
        val kv = kv()
        loadSnapshotIndex(kv)
            .filter { packageName -> packageName !in normalizedSnapshots }
            .forEach { packageName -> kv.removeValueForKey(snapshotStoreKey(packageName)) }
        normalizedSnapshots.values.forEach { snapshot ->
            kv.encode(snapshotStoreKey(snapshot.packageName), encodeSnapshot(snapshot).encodeCompact())
        }
        saveSnapshotIndex(normalizedSnapshots.keys, kv)
    }

    internal fun encodeRecord(record: GitHubAppInstallHistoryRecord): JsonObject {
        return buildJsonObject {
            put("id", record.id)
            put("trackId", record.trackId)
            put("action", record.action.name)
            put("source", record.source.name)
            put("changedAtMillis", record.changedAtMillis)
            put("owner", record.owner)
            put("repo", record.repo)
            put("repoUrl", record.repoUrl)
            put("packageName", record.packageName)
            put("appLabel", record.appLabel)
            put("sourceMode", record.sourceMode.name)
            put("previousVersionName", record.previousVersionName)
            put("previousVersionCode", record.previousVersionCode)
            put("currentVersionName", record.currentVersionName)
            put("currentVersionCode", record.currentVersionCode)
            put("broadcastAction", record.broadcastAction)
            put("broadcastUid", record.broadcastUid)
            put("broadcastDataRemoved", record.broadcastDataRemoved)
            put("broadcastUserInitiated", record.broadcastUserInitiated)
            put("broadcastArchival", record.broadcastArchival)
            put("replacing", record.replacing)
            put("previousInstallSourceInfo", encodeInstallSourceInfo(record.previousInstallSourceInfo))
            put("currentInstallSourceInfo", encodeInstallSourceInfo(record.currentInstallSourceInfo))
            put("note", record.note)
        }
    }

    internal fun decodeRecord(raw: String): GitHubAppInstallHistoryRecord? {
        if (raw.isBlank()) return null
        return runCatching {
            val obj = raw.parseJsonObjectOrNull() ?: return@runCatching null
            val trackId = obj.optString("trackId").trim()
            val packageName = normalizePackageName(obj.optString("packageName"))
            val changedAtMillis = obj.optLong("changedAtMillis", 0L)
            if (trackId.isBlank() || packageName.isBlank() || changedAtMillis <= 0L) {
                return@runCatching null
            }
            GitHubAppInstallHistoryRecord(
                id = obj.optString("id").trim(),
                trackId = trackId,
                action =
                    enumValueOrDefault(
                        raw = obj.optString("action"),
                        defaultValue = GitHubAppInstallHistoryAction.Updated,
                    ),
                source =
                    enumValueOrDefault(
                        raw = obj.optString("source"),
                        defaultValue = GitHubAppInstallHistorySource.PackageBroadcast,
                    ),
                changedAtMillis = changedAtMillis,
                owner = obj.optString("owner").trim(),
                repo = obj.optString("repo").trim(),
                repoUrl = obj.optString("repoUrl").trim(),
                packageName = packageName,
                appLabel = obj.optString("appLabel").trim(),
                sourceMode =
                    enumValueOrDefault(
                        raw = obj.optString("sourceMode"),
                        defaultValue = GitHubTrackedSourceMode.GitHubRepository,
                    ),
                previousVersionName = obj.optString("previousVersionName").trim(),
                previousVersionCode = obj.optLong("previousVersionCode", -1L),
                currentVersionName = obj.optString("currentVersionName").trim(),
                currentVersionCode = obj.optLong("currentVersionCode", -1L),
                broadcastAction = obj.optString("broadcastAction").trim(),
                broadcastUid = obj.optInt("broadcastUid", -1),
                broadcastDataRemoved = obj.optBoolean("broadcastDataRemoved", false),
                broadcastUserInitiated = obj.optBoolean("broadcastUserInitiated", false),
                broadcastArchival = obj.optBoolean("broadcastArchival", false),
                replacing = obj.optBoolean("replacing", false),
                previousInstallSourceInfo =
                    decodeInstallSourceInfo(obj.optObject("previousInstallSourceInfo")),
                currentInstallSourceInfo =
                    decodeInstallSourceInfo(obj.optObject("currentInstallSourceInfo")),
                note = obj.optString("note").trim(),
            )
        }.getOrNull()
    }

    internal fun encodeSnapshot(snapshot: GitHubTrackedAppInstallSnapshot): JsonObject {
        return buildJsonObject {
            put("packageName", snapshot.packageName)
            put("versionName", snapshot.versionName)
            put("versionCode", snapshot.versionCode)
            put("isSystemApp", snapshot.isSystemApp)
            put("appLabel", snapshot.appLabel)
            put("observedAtMillis", snapshot.observedAtMillis)
            put("installSourceInfo", encodeInstallSourceInfo(snapshot.installSourceInfo))
        }
    }

    internal fun decodeSnapshot(raw: String): GitHubTrackedAppInstallSnapshot? {
        if (raw.isBlank()) return null
        return runCatching {
            val obj = raw.parseJsonObjectOrNull() ?: return@runCatching null
            val packageName = normalizePackageName(obj.optString("packageName"))
            val observedAtMillis = obj.optLong("observedAtMillis", 0L)
            if (packageName.isBlank() || observedAtMillis <= 0L) return@runCatching null
            GitHubTrackedAppInstallSnapshot(
                packageName = packageName,
                versionName = obj.optString("versionName").trim(),
                versionCode = obj.optLong("versionCode", -1L),
                isSystemApp = obj.optBoolean("isSystemApp", false),
                appLabel = obj.optString("appLabel").trim(),
                observedAtMillis = observedAtMillis,
                installSourceInfo = decodeInstallSourceInfo(obj.optObject("installSourceInfo")),
            )
        }.getOrNull()
    }

    internal fun GitHubAppInstallHistoryRecord.normalizedForStorage(): GitHubAppInstallHistoryRecord =
        copy(
            id = id.trim(),
            trackId = trackId.trim(),
            changedAtMillis = changedAtMillis.coerceAtLeast(0L),
            owner = compactHistoryText(owner, 120),
            repo = compactHistoryText(repo, 160),
            repoUrl = compactHistoryText(repoUrl, 360),
            packageName = normalizePackageName(packageName),
            appLabel = compactHistoryText(appLabel, 160),
            previousVersionName = compactHistoryText(previousVersionName, 120),
            previousVersionCode = previousVersionCode.takeIf { it >= 0L } ?: -1L,
            currentVersionName = compactHistoryText(currentVersionName, 120),
            currentVersionCode = currentVersionCode.takeIf { it >= 0L } ?: -1L,
            broadcastAction = compactHistoryText(broadcastAction, 120),
            broadcastUid = broadcastUid.takeIf { it >= 0 } ?: -1,
            previousInstallSourceInfo = previousInstallSourceInfo.normalizedForStorage(),
            currentInstallSourceInfo = currentInstallSourceInfo.normalizedForStorage(),
            note = compactHistoryText(note, 320),
        )

    internal fun GitHubTrackedAppInstallSnapshot.normalizedForStorage(): GitHubTrackedAppInstallSnapshot =
        copy(
            packageName = normalizePackageName(packageName),
            versionName = compactHistoryText(versionName, 120),
            versionCode = versionCode.takeIf { it >= 0L } ?: -1L,
            appLabel = compactHistoryText(appLabel, 160),
            observedAtMillis = observedAtMillis.coerceAtLeast(0L),
            installSourceInfo = installSourceInfo.normalizedForStorage(),
        )

    internal fun GitHubAppInstallSourceInfo.normalizedForStorage(): GitHubAppInstallSourceInfo =
        copy(
            installingPackageName = normalizePackageName(installingPackageName),
            installingPackageLabel = compactHistoryText(installingPackageLabel, 160),
            initiatingPackageName = normalizePackageName(initiatingPackageName),
            initiatingPackageLabel = compactHistoryText(initiatingPackageLabel, 160),
            originatingPackageName = normalizePackageName(originatingPackageName),
            originatingPackageLabel = compactHistoryText(originatingPackageLabel, 160),
            updateOwnerPackageName = normalizePackageName(updateOwnerPackageName),
            updateOwnerPackageLabel = compactHistoryText(updateOwnerPackageLabel, 160),
            packageSource = packageSource.takeIf { it >= 0 } ?: -1,
        )

    internal fun recordId(record: GitHubAppInstallHistoryRecord): String =
        sha1(
            listOf(
                record.action.name,
                record.source.name,
                record.trackId,
                record.packageName,
                record.previousVersionCode.toString(),
                record.currentVersionCode.toString(),
                record.changedAtMillis.toString(),
                record.broadcastAction,
                record.broadcastUid.toString(),
                record.currentInstallSourceInfo.installingPackageName,
                record.currentInstallSourceInfo.initiatingPackageName,
                record.currentInstallSourceInfo.packageSource.toString(),
            ).joinToString("|"),
        )

    internal fun shouldPruneBefore(
        record: GitHubAppInstallHistoryRecord,
        cutoffMillis: Long,
    ): Boolean = cutoffMillis > 0L && record.changedAtMillis in 1 until cutoffMillis

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

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        raw: String,
        defaultValue: T,
    ): T =
        runCatching { enumValueOf<T>(raw.trim()) }.getOrNull() ?: defaultValue

    private fun entryStoreKey(id: String): String = "entry_$id"

    private fun snapshotStoreKey(packageName: String): String =
        "snapshot_${normalizePackageName(packageName)}"

    private fun loadIndex(kv: MMKV = store): MutableSet<String> =
        loadCommaSeparatedSet(kv.decodeString(KEY_INDEX).orEmpty())

    private fun saveIndex(index: Set<String>, kv: MMKV = store) {
        kv.encode(KEY_INDEX, index.filter { it.isNotBlank() }.joinToString(","))
    }

    private fun loadSnapshotIndex(kv: MMKV = store): MutableSet<String> =
        loadCommaSeparatedSet(kv.decodeString(KEY_SNAPSHOT_INDEX).orEmpty())

    private fun saveSnapshotIndex(index: Set<String>, kv: MMKV = store) {
        val normalized = index.map(::normalizePackageName).filter { it.isNotBlank() }.toSet()
        if (normalized.isEmpty()) {
            kv.removeValueForKey(KEY_SNAPSHOT_INDEX)
        } else {
            kv.encode(KEY_SNAPSHOT_INDEX, normalized.joinToString(","))
        }
    }

    private fun loadCommaSeparatedSet(raw: String): MutableSet<String> {
        if (raw.isBlank()) return mutableSetOf()
        return raw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
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

    private fun normalizePackageName(packageName: String): String =
        packageName.trim().lowercase(Locale.ROOT)

    private fun encodeInstallSourceInfo(info: GitHubAppInstallSourceInfo): JsonObject {
        val normalized = info.normalizedForStorage()
        return buildJsonObject {
            put("installingPackageName", normalized.installingPackageName)
            put("installingPackageLabel", normalized.installingPackageLabel)
            put("initiatingPackageName", normalized.initiatingPackageName)
            put("initiatingPackageLabel", normalized.initiatingPackageLabel)
            put("originatingPackageName", normalized.originatingPackageName)
            put("originatingPackageLabel", normalized.originatingPackageLabel)
            put("updateOwnerPackageName", normalized.updateOwnerPackageName)
            put("updateOwnerPackageLabel", normalized.updateOwnerPackageLabel)
            put("packageSource", normalized.packageSource)
        }
    }

    private fun decodeInstallSourceInfo(obj: JsonObject?): GitHubAppInstallSourceInfo {
        if (obj == null) return GitHubAppInstallSourceInfo()
        return GitHubAppInstallSourceInfo(
            installingPackageName = obj.optString("installingPackageName").trim(),
            installingPackageLabel = obj.optString("installingPackageLabel").trim(),
            initiatingPackageName = obj.optString("initiatingPackageName").trim(),
            initiatingPackageLabel = obj.optString("initiatingPackageLabel").trim(),
            originatingPackageName = obj.optString("originatingPackageName").trim(),
            originatingPackageLabel = obj.optString("originatingPackageLabel").trim(),
            updateOwnerPackageName = obj.optString("updateOwnerPackageName").trim(),
            updateOwnerPackageLabel = obj.optString("updateOwnerPackageLabel").trim(),
            packageSource = obj.optInt("packageSource", -1),
        ).normalizedForStorage()
    }
}
