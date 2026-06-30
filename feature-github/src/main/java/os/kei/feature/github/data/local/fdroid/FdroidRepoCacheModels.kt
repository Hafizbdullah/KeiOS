package os.kei.feature.github.data.local.fdroid

import com.tencent.mmkv.MMKV
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import os.kei.core.json.encodeCompact
import os.kei.core.json.jsonObjectOrNull
import os.kei.core.json.jsonPrimitiveOrNull
import os.kei.core.json.optArray
import os.kei.core.json.optInt
import os.kei.core.json.optLong
import os.kei.core.json.optObject
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.core.prefs.KeiMmkv
import os.kei.feature.github.data.remote.fdroid.FdroidAntiFeatureSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidRepositorySnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidVersionSnapshot
import os.kei.feature.github.model.FdroidIndexFormat
import java.security.MessageDigest
import java.util.Locale

private const val FDROID_REPO_INDEX_CACHE_MAX_RECORDS = 96

data class FdroidRepoCacheKey(
    val repoUrl: String
) {
    companion object {
        fun from(repoUrl: String): FdroidRepoCacheKey {
            return FdroidRepoCacheKey(repoUrl.trim().trimEnd('/'))
        }
    }
}

data class FdroidRepoCacheRequestKey(
    val repoUrl: String,
    val kind: String,
    val discriminator: String
) {
    val stableId: String
        get() = sha1("$repoUrl|$kind|$discriminator")

    companion object {
        fun packages(
            repoUrl: String,
            packageNames: Set<String>
        ): FdroidRepoCacheRequestKey {
            return FdroidRepoCacheRequestKey(
                repoUrl = normalizeRepoUrl(repoUrl),
                kind = "packages",
                discriminator = packageNames
                    .map { name -> name.trim().lowercase(Locale.ROOT) }
                    .filter { name -> name.isNotBlank() }
                    .distinct()
                    .sorted()
                    .joinToString(",")
            )
        }

        fun search(
            repoUrl: String,
            query: String,
            packageName: String,
            limit: Int
        ): FdroidRepoCacheRequestKey {
            return FdroidRepoCacheRequestKey(
                repoUrl = normalizeRepoUrl(repoUrl),
                kind = "search",
                discriminator = listOf(
                    query.trim().lowercase(Locale.ROOT).replace(Regex("""\s+"""), " "),
                    packageName.trim().lowercase(Locale.ROOT),
                    limit.coerceIn(1, 50).toString()
                ).joinToString("|")
            )
        }

        private fun normalizeRepoUrl(repoUrl: String): String = repoUrl.trim().trimEnd('/')
    }
}

data class FdroidRepoCacheRecord(
    val repoUrl: String,
    val fetchedAtMillis: Long,
    val etag: String,
    val lastModified: String,
    val snapshot: FdroidRepositorySnapshot
) {
    fun isFresh(
        nowMillis: Long,
        maxAgeMillis: Long
    ): Boolean {
        if (maxAgeMillis <= 0L) return false
        if (fetchedAtMillis <= 0L) return false
        return nowMillis - fetchedAtMillis <= maxAgeMillis
    }
}

interface FdroidRepositoryIndexCacheStore {
    fun load(key: FdroidRepoCacheRequestKey): FdroidRepoCacheRecord?

    fun save(key: FdroidRepoCacheRequestKey, record: FdroidRepoCacheRecord)

    fun clear(repoUrl: String? = null)
}

object FdroidRepoIndexCacheStore : FdroidRepositoryIndexCacheStore {
    private const val KV_ID = "github_fdroid_repo_index_cache"
    private const val KEY_INDEX = "entry_index"

    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }

    override fun load(key: FdroidRepoCacheRequestKey): FdroidRepoCacheRecord? {
        return runCatching {
            val raw = store.decodeString(entryStoreKey(key.stableId), "").orEmpty()
            parseFdroidRepoCacheRecord(raw.parseJsonObjectOrNull())
        }.getOrNull()
    }

    override fun save(key: FdroidRepoCacheRequestKey, record: FdroidRepoCacheRecord) {
        runCatching {
            val id = key.stableId
            store.encode(entryStoreKey(id), record.toCacheJson().encodeCompact())
            val entry = indexEntry(id = id, repoUrl = key.repoUrl)
            saveIndex(trimIndex((loadIndex() - id) + entry))
        }
    }

    override fun clear(repoUrl: String?) {
        runCatching {
            val normalizedRepoUrl = repoUrl?.trim()?.trimEnd('/').orEmpty()
            val current = loadIndex()
            val removed = current.filter { entry ->
                normalizedRepoUrl.isBlank() || entry.substringAfter('|') == normalizedRepoUrl
            }
            removed.forEach { entry ->
                store.removeValueForKey(entryStoreKey(entry.substringBefore('|')))
            }
            saveIndex(current - removed.toSet())
            store.trim()
        }
    }

    private fun loadIndex(): Set<String> {
        return store.decodeString(KEY_INDEX, "").orEmpty()
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() && '|' in it }
            .toSet()
    }

    private fun saveIndex(index: Set<String>) {
        if (index.isEmpty()) {
            store.removeValueForKey(KEY_INDEX)
        } else {
            store.encode(KEY_INDEX, index.sorted().joinToString("\n"))
        }
    }

    private fun trimIndex(index: Set<String>): Set<String> {
        if (index.size <= FDROID_REPO_INDEX_CACHE_MAX_RECORDS) return index
        val sorted = index.sorted()
        val removed = sorted.take(index.size - FDROID_REPO_INDEX_CACHE_MAX_RECORDS)
        removed.forEach { entry -> store.removeValueForKey(entryStoreKey(entry.substringBefore('|'))) }
        return sorted.drop(removed.size).toSet()
    }

    private fun indexEntry(id: String, repoUrl: String): String = "$id|$repoUrl"

    private fun entryStoreKey(id: String): String = "entry_$id"
}

fun FdroidRepoCacheRecord.withFetchedAt(fetchedAtMillis: Long): FdroidRepoCacheRecord {
    return copy(fetchedAtMillis = fetchedAtMillis)
}

private fun FdroidRepoCacheRecord.toCacheJson(): JsonObject {
    return buildJsonObject {
        put("repoUrl", repoUrl)
        put("fetchedAtMillis", fetchedAtMillis)
        put("etag", etag)
        put("lastModified", lastModified)
        put("snapshot", snapshot.toCacheJson())
    }
}

private fun parseFdroidRepoCacheRecord(obj: JsonObject?): FdroidRepoCacheRecord? {
    obj ?: return null
    val repoUrl = obj.optString("repoUrl").trim().trimEnd('/')
    val fetchedAtMillis = obj.optLong("fetchedAtMillis", -1L)
    if (repoUrl.isBlank() || fetchedAtMillis <= 0L) return null
    return FdroidRepoCacheRecord(
        repoUrl = repoUrl,
        fetchedAtMillis = fetchedAtMillis,
        etag = obj.optString("etag").trim(),
        lastModified = obj.optString("lastModified").trim(),
        snapshot = parseRepositorySnapshot(obj.optObject("snapshot")) ?: return null
    )
}

private fun FdroidRepositorySnapshot.toCacheJson(): JsonObject {
    return buildJsonObject {
        put("repoUrl", repoUrl)
        put("format", format.storageId)
        put("repoName", repoName)
        put("repoDescription", repoDescription)
        put("timestampMillis", timestampMillis ?: 0L)
        put("mirrors", mirrors.toJsonArray())
        put("totalPackageCount", totalPackageCount ?: 0)
        put(
            "packages",
            buildJsonArray {
                packages.values.forEach { snapshot -> add(snapshot.toCacheJson()) }
            }
        )
    }
}

private fun parseRepositorySnapshot(obj: JsonObject?): FdroidRepositorySnapshot? {
    obj ?: return null
    val repoUrl = obj.optString("repoUrl").trim().trimEnd('/')
    if (repoUrl.isBlank()) return null
    val packages = obj.optArray("packages")
        ?.mapNotNull { element -> parsePackageSnapshot(element.jsonObjectOrNull()) }
        .orEmpty()
        .associateBy { snapshot -> snapshot.packageName }
    return FdroidRepositorySnapshot(
        repoUrl = repoUrl,
        format = FdroidIndexFormat.fromStorageId(obj.optString("format")),
        repoName = obj.optString("repoName").trim(),
        repoDescription = obj.optString("repoDescription").trim(),
        timestampMillis = obj.optLong("timestampMillis", 0L).takeIf { it > 0L },
        mirrors = obj.optArray("mirrors").toStringList(),
        packages = packages,
        totalPackageCount = obj.optInt("totalPackageCount", 0).takeIf { it > 0 }
    )
}

private fun FdroidPackageSnapshot.toCacheJson(): JsonObject {
    return buildJsonObject {
        put("repoUrl", repoUrl)
        put("packageName", packageName)
        put("suggestedVersionCode", suggestedVersionCode ?: -1L)
        put("appName", appName)
        put("summary", summary)
        put("description", description)
        put("license", license)
        put("sourceCodeUrl", sourceCodeUrl)
        put("webSiteUrl", webSiteUrl)
        put("issueTrackerUrl", issueTrackerUrl)
        put("changelogUrl", changelogUrl)
        put("categories", categories.toJsonArray())
        put("antiFeatures", antiFeatures.toCacheJson())
        put(
            "versions",
            buildJsonArray {
                versions.forEach { version -> add(version.toCacheJson()) }
            }
        )
    }
}

private fun parsePackageSnapshot(obj: JsonObject?): FdroidPackageSnapshot? {
    obj ?: return null
    val repoUrl = obj.optString("repoUrl").trim().trimEnd('/')
    val packageName = obj.optString("packageName").trim()
    if (repoUrl.isBlank() || packageName.isBlank()) return null
    return FdroidPackageSnapshot(
        repoUrl = repoUrl,
        packageName = packageName,
        suggestedVersionCode = obj.optLong("suggestedVersionCode", -1L).takeIf { it >= 0L },
        versions = obj.optArray("versions")
            ?.mapNotNull { element -> parseVersionSnapshot(element.jsonObjectOrNull()) }
            .orEmpty(),
        appName = obj.optString("appName").trim(),
        summary = obj.optString("summary").trim(),
        description = obj.optString("description").trim(),
        license = obj.optString("license").trim(),
        sourceCodeUrl = obj.optString("sourceCodeUrl").trim(),
        webSiteUrl = obj.optString("webSiteUrl").trim(),
        issueTrackerUrl = obj.optString("issueTrackerUrl").trim(),
        changelogUrl = obj.optString("changelogUrl").trim(),
        categories = obj.optArray("categories").toStringList(),
        antiFeatures = parseAntiFeatures(obj.optArray("antiFeatures"))
    )
}

private fun FdroidVersionSnapshot.toCacheJson(): JsonObject {
    return buildJsonObject {
        put("versionName", versionName)
        put("versionCode", versionCode)
        put("apkName", apkName)
        put("apkPath", apkPath)
        put("apkSha256", apkSha256)
        put("apkSizeBytes", apkSizeBytes)
        put("addedAtMillis", addedAtMillis ?: 0L)
        put("minSdk", minSdk ?: 0)
        put("targetSdk", targetSdk ?: 0)
        put("nativeAbis", nativeAbis.toJsonArray())
        put("signerSha256", signerSha256.toJsonArray())
        put("releaseChannels", releaseChannels.toJsonArray())
        put("whatsNew", whatsNew)
        put("antiFeatures", antiFeatures.toCacheJson())
    }
}

private fun parseVersionSnapshot(obj: JsonObject?): FdroidVersionSnapshot? {
    obj ?: return null
    val versionCode = obj.optLong("versionCode", -1L)
    if (versionCode < 0L) return null
    return FdroidVersionSnapshot(
        versionName = obj.optString("versionName").trim(),
        versionCode = versionCode,
        apkName = obj.optString("apkName").trim(),
        apkPath = obj.optString("apkPath").trim(),
        apkSha256 = obj.optString("apkSha256").trim(),
        apkSizeBytes = obj.optLong("apkSizeBytes", 0L),
        addedAtMillis = obj.optLong("addedAtMillis", 0L).takeIf { it > 0L },
        minSdk = obj.optInt("minSdk", 0).takeIf { it > 0 },
        targetSdk = obj.optInt("targetSdk", 0).takeIf { it > 0 },
        nativeAbis = obj.optArray("nativeAbis").toStringList(),
        signerSha256 = obj.optArray("signerSha256").toStringList(),
        releaseChannels = obj.optArray("releaseChannels").toStringList(),
        whatsNew = obj.optString("whatsNew").trim(),
        antiFeatures = parseAntiFeatures(obj.optArray("antiFeatures"))
    )
}

private fun List<FdroidAntiFeatureSnapshot>.toCacheJson(): JsonArray =
    buildJsonArray {
        forEach { feature ->
            add(
                buildJsonObject {
                    put("id", feature.id)
                    put("label", feature.label)
                    put("description", feature.description)
                }
            )
        }
    }

private fun parseAntiFeatures(array: JsonArray?): List<FdroidAntiFeatureSnapshot> {
    return array?.mapNotNull { element ->
        val obj = element.jsonObjectOrNull()
        if (obj != null) {
            val id = obj.optString("id").trim()
            id.takeIf { it.isNotBlank() }?.let {
                FdroidAntiFeatureSnapshot(
                    id = it,
                    label = obj.optString("label").trim(),
                    description = obj.optString("description").trim()
                )
            }
        } else {
            element.jsonPrimitiveOrNull()?.contentOrNull?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { FdroidAntiFeatureSnapshot(id = it) }
        }
    }.orEmpty()
}

private fun List<String>.toJsonArray(): JsonArray =
    buildJsonArray {
        forEach { value -> add(JsonPrimitive(value)) }
    }

private fun JsonArray?.toStringList(): List<String> {
    return this?.mapNotNull { element ->
        element.jsonPrimitiveOrNull()?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
    }.orEmpty()
}

private fun sha1(text: String): String {
    return MessageDigest.getInstance("SHA-1")
        .digest(text.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
}
