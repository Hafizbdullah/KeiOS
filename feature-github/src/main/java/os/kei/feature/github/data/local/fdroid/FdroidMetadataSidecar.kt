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
import os.kei.feature.github.model.FdroidTrustPolicy
import java.security.MessageDigest

fun interface FdroidMetadataSidecarWriter {
    fun save(sidecar: FdroidMetadataSidecar)
}

data class FdroidMetadataSidecar(
    val trackId: String,
    val sourceConfigSignature: String,
    val fetchedAtMillis: Long,
    val repo: FdroidRepoMetadataSummary,
    val packageInfo: FdroidPackageMetadataSummary,
    val selectedVersion: FdroidVersionMetadataSummary?,
    val candidateVersions: List<FdroidVersionMetadataSummary>,
    val trust: FdroidTrustSummary,
    val antiFeatures: List<FdroidAntiFeatureSnapshot>
) {
    fun isFreshFor(
        activeSourceConfigSignature: String,
        nowMillis: Long,
        ttlMillis: Long
    ): Boolean {
        if (sourceConfigSignature != activeSourceConfigSignature) return false
        if (ttlMillis <= 0L || fetchedAtMillis <= 0L) return false
        return nowMillis - fetchedAtMillis <= ttlMillis
    }
}

data class FdroidRepoMetadataSummary(
    val repoUrl: String,
    val repoName: String,
    val repoDescription: String,
    val format: FdroidIndexFormat,
    val timestampMillis: Long?,
    val packageCount: Int,
    val mirrors: List<String>
)

data class FdroidPackageMetadataSummary(
    val packageName: String,
    val appName: String,
    val summary: String,
    val description: String,
    val license: String,
    val sourceCodeUrl: String,
    val webSiteUrl: String,
    val issueTrackerUrl: String,
    val changelogUrl: String,
    val categories: List<String>
)

data class FdroidVersionMetadataSummary(
    val versionName: String,
    val versionCode: Long,
    val apkName: String,
    val apkPath: String,
    val apkSha256: String,
    val apkSizeBytes: Long,
    val addedAtMillis: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val nativeAbis: List<String>,
    val signerSha256: List<String>,
    val releaseChannels: List<String>,
    val whatsNew: String,
    val antiFeatures: List<FdroidAntiFeatureSnapshot>
)

data class FdroidTrustSummary(
    val trustPolicy: FdroidTrustPolicy,
    val repoFingerprint: String,
    val apkSha256: String,
    val signerSha256: List<String>,
    val hashAvailable: Boolean,
    val signerAvailable: Boolean
)

fun buildFdroidMetadataSidecar(
    trackId: String,
    sourceConfigSignature: String,
    fetchedAtMillis: Long,
    repositorySnapshot: FdroidRepositorySnapshot?,
    packageSnapshot: FdroidPackageSnapshot,
    selectedVersion: FdroidVersionSnapshot?,
    trustPolicy: FdroidTrustPolicy = FdroidTrustPolicy.TrackOnlyWarn,
    repoFingerprint: String = ""
): FdroidMetadataSidecar {
    val repo = repositorySnapshot?.let {
        FdroidRepoMetadataSummary(
            repoUrl = it.repoUrl,
            repoName = it.repoName,
            repoDescription = it.repoDescription,
            format = it.format,
            timestampMillis = it.timestampMillis,
            packageCount = it.packageCount,
            mirrors = it.mirrors
        )
    } ?: FdroidRepoMetadataSummary(
        repoUrl = packageSnapshot.repoUrl,
        repoName = "",
        repoDescription = "",
        format = FdroidIndexFormat.Unknown,
        timestampMillis = null,
        packageCount = 0,
        mirrors = emptyList()
    )
    val selectedSummary = selectedVersion?.toMetadataSummary()
    val antiFeatures = (packageSnapshot.antiFeatures + selectedVersion?.antiFeatures.orEmpty())
        .distinctBy { it.id }
    return FdroidMetadataSidecar(
        trackId = trackId,
        sourceConfigSignature = sourceConfigSignature,
        fetchedAtMillis = fetchedAtMillis,
        repo = repo,
        packageInfo = FdroidPackageMetadataSummary(
            packageName = packageSnapshot.packageName,
            appName = packageSnapshot.appName,
            summary = packageSnapshot.summary,
            description = packageSnapshot.description,
            license = packageSnapshot.license,
            sourceCodeUrl = packageSnapshot.sourceCodeUrl,
            webSiteUrl = packageSnapshot.webSiteUrl,
            issueTrackerUrl = packageSnapshot.issueTrackerUrl,
            changelogUrl = packageSnapshot.changelogUrl,
            categories = packageSnapshot.categories
        ),
        selectedVersion = selectedSummary,
        candidateVersions = packageSnapshot.versions
            .sortedByDescending { it.versionCode }
            .take(8)
            .map { it.toMetadataSummary() },
        trust = FdroidTrustSummary(
            trustPolicy = trustPolicy,
            repoFingerprint = repoFingerprint,
            apkSha256 = selectedVersion?.apkSha256.orEmpty(),
            signerSha256 = selectedVersion?.signerSha256.orEmpty(),
            hashAvailable = !selectedVersion?.apkSha256.isNullOrBlank(),
            signerAvailable = !selectedVersion?.signerSha256.isNullOrEmpty()
        ),
        antiFeatures = antiFeatures
    )
}

fun FdroidMetadataSidecar.toCacheJson(): JsonObject {
    return buildJsonObject {
        put("trackId", trackId)
        put("sourceConfigSignature", sourceConfigSignature)
        put("fetchedAtMillis", fetchedAtMillis)
        put("repo", repo.toCacheJson())
        put("packageInfo", packageInfo.toCacheJson())
        selectedVersion?.let { put("selectedVersion", it.toCacheJson()) }
        put(
            "candidateVersions",
            buildJsonArray {
                candidateVersions.forEach { add(it.toCacheJson()) }
            }
        )
        put("trust", trust.toCacheJson())
        put("antiFeatures", antiFeatures.toCacheJson())
    }
}

fun parseFdroidMetadataSidecar(obj: JsonObject?): FdroidMetadataSidecar? {
    obj ?: return null
    val trackId = obj.optString("trackId").trim()
    val signature = obj.optString("sourceConfigSignature").trim()
    val fetchedAtMillis = obj.optLong("fetchedAtMillis", -1L)
    if (trackId.isBlank() || signature.isBlank() || fetchedAtMillis <= 0L) return null
    return FdroidMetadataSidecar(
        trackId = trackId,
        sourceConfigSignature = signature,
        fetchedAtMillis = fetchedAtMillis,
        repo = parseRepoSummary(obj.optObject("repo")) ?: return null,
        packageInfo = parsePackageSummary(obj.optObject("packageInfo")) ?: return null,
        selectedVersion = parseVersionSummary(obj.optObject("selectedVersion")),
        candidateVersions = obj.optArray("candidateVersions")
            ?.mapNotNull { parseVersionSummary(it.jsonObjectOrNull()) }
            .orEmpty(),
        trust = parseTrustSummary(obj.optObject("trust")),
        antiFeatures = parseAntiFeatures(obj.optArray("antiFeatures"))
    )
}

object FdroidMetadataSidecarStore : FdroidMetadataSidecarWriter {
    private const val KV_ID = "github_fdroid_metadata_sidecar"
    private const val KEY_INDEX = "entry_index"

    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }

    override fun save(sidecar: FdroidMetadataSidecar) {
        val id = keyId(sidecar.trackId)
        store.encode(entryStoreKey(id), sidecar.toCacheJson().encodeCompact())
        saveIndex(loadIndex() + id)
    }

    fun load(trackId: String): FdroidMetadataSidecar? {
        val raw = store.decodeString(entryStoreKey(keyId(trackId)), "").orEmpty()
        return parseFdroidMetadataSidecar(raw.parseJsonObjectOrNull())
    }

    fun clear(trackId: String) {
        val id = keyId(trackId)
        store.removeValueForKey(entryStoreKey(id))
        saveIndex(loadIndex() - id)
    }

    fun clearAll() {
        loadIndex().forEach { id ->
            store.removeValueForKey(entryStoreKey(id))
        }
        store.removeValueForKey(KEY_INDEX)
        store.trim()
    }

    private fun loadIndex(): Set<String> {
        return store.decodeString(KEY_INDEX, "").orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun saveIndex(index: Set<String>) {
        store.encode(KEY_INDEX, index.filter { it.isNotBlank() }.sorted().joinToString(","))
    }

    private fun entryStoreKey(id: String): String = "entry_$id"

    private fun keyId(trackId: String): String = sha1(trackId.trim())

    private fun sha1(text: String): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(text.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}

private fun FdroidVersionSnapshot.toMetadataSummary(): FdroidVersionMetadataSummary {
    return FdroidVersionMetadataSummary(
        versionName = versionName,
        versionCode = versionCode,
        apkName = apkName,
        apkPath = apkPath,
        apkSha256 = apkSha256,
        apkSizeBytes = apkSizeBytes,
        addedAtMillis = addedAtMillis,
        minSdk = minSdk,
        targetSdk = targetSdk,
        nativeAbis = nativeAbis,
        signerSha256 = signerSha256,
        releaseChannels = releaseChannels,
        whatsNew = whatsNew,
        antiFeatures = antiFeatures
    )
}

private fun FdroidRepoMetadataSummary.toCacheJson(): JsonObject =
    buildJsonObject {
        put("repoUrl", repoUrl)
        put("repoName", repoName)
        put("repoDescription", repoDescription)
        put("format", format.storageId)
        put("timestampMillis", timestampMillis ?: 0L)
        put("packageCount", packageCount)
        put("mirrors", mirrors.toJsonArray())
    }

private fun FdroidPackageMetadataSummary.toCacheJson(): JsonObject =
    buildJsonObject {
        put("packageName", packageName)
        put("appName", appName)
        put("summary", summary)
        put("description", description)
        put("license", license)
        put("sourceCodeUrl", sourceCodeUrl)
        put("webSiteUrl", webSiteUrl)
        put("issueTrackerUrl", issueTrackerUrl)
        put("changelogUrl", changelogUrl)
        put("categories", categories.toJsonArray())
    }

private fun FdroidVersionMetadataSummary.toCacheJson(): JsonObject =
    buildJsonObject {
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

private fun FdroidTrustSummary.toCacheJson(): JsonObject =
    buildJsonObject {
        put("trustPolicy", trustPolicy.storageId)
        put("repoFingerprint", repoFingerprint)
        put("apkSha256", apkSha256)
        put("signerSha256", signerSha256.toJsonArray())
        put("hashAvailable", hashAvailable)
        put("signerAvailable", signerAvailable)
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

private fun List<String>.toJsonArray(): JsonArray =
    buildJsonArray {
        forEach { value -> add(JsonPrimitive(value)) }
    }

private fun parseRepoSummary(obj: JsonObject?): FdroidRepoMetadataSummary? {
    obj ?: return null
    val repoUrl = obj.optString("repoUrl").trim()
    if (repoUrl.isBlank()) return null
    return FdroidRepoMetadataSummary(
        repoUrl = repoUrl,
        repoName = obj.optString("repoName").trim(),
        repoDescription = obj.optString("repoDescription").trim(),
        format = FdroidIndexFormat.fromStorageId(obj.optString("format")),
        timestampMillis = obj.optLong("timestampMillis", 0L).takeIf { it > 0L },
        packageCount = obj.optInt("packageCount", 0),
        mirrors = obj.optArray("mirrors").toStringList()
    )
}

private fun parsePackageSummary(obj: JsonObject?): FdroidPackageMetadataSummary? {
    obj ?: return null
    val packageName = obj.optString("packageName").trim()
    if (packageName.isBlank()) return null
    return FdroidPackageMetadataSummary(
        packageName = packageName,
        appName = obj.optString("appName").trim(),
        summary = obj.optString("summary").trim(),
        description = obj.optString("description").trim(),
        license = obj.optString("license").trim(),
        sourceCodeUrl = obj.optString("sourceCodeUrl").trim(),
        webSiteUrl = obj.optString("webSiteUrl").trim(),
        issueTrackerUrl = obj.optString("issueTrackerUrl").trim(),
        changelogUrl = obj.optString("changelogUrl").trim(),
        categories = obj.optArray("categories").toStringList()
    )
}

private fun parseVersionSummary(obj: JsonObject?): FdroidVersionMetadataSummary? {
    obj ?: return null
    val versionCode = obj.optLong("versionCode", -1L)
    if (versionCode < 0L) return null
    return FdroidVersionMetadataSummary(
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

private fun parseTrustSummary(obj: JsonObject?): FdroidTrustSummary {
    obj ?: return FdroidTrustSummary(
        trustPolicy = FdroidTrustPolicy.TrackOnlyWarn,
        repoFingerprint = "",
        apkSha256 = "",
        signerSha256 = emptyList(),
        hashAvailable = false,
        signerAvailable = false
    )
    return FdroidTrustSummary(
        trustPolicy = FdroidTrustPolicy.fromStorageId(obj.optString("trustPolicy")),
        repoFingerprint = obj.optString("repoFingerprint").trim(),
        apkSha256 = obj.optString("apkSha256").trim(),
        signerSha256 = obj.optArray("signerSha256").toStringList(),
        hashAvailable = obj["hashAvailable"].jsonPrimitiveOrNull()?.contentOrNull?.toBooleanStrictOrNull()
            ?: obj.optString("apkSha256").isNotBlank(),
        signerAvailable = obj["signerAvailable"].jsonPrimitiveOrNull()?.contentOrNull?.toBooleanStrictOrNull()
            ?: !obj.optArray("signerSha256").toStringList().isEmpty()
    )
}

private fun parseAntiFeatures(array: JsonArray?): List<FdroidAntiFeatureSnapshot> {
    return array?.mapNotNull { element ->
        val obj = element.jsonObjectOrNull()
        when {
            obj != null -> {
                val id = obj.optString("id").trim()
                id.takeIf { it.isNotBlank() }?.let {
                    FdroidAntiFeatureSnapshot(
                        id = it,
                        label = obj.optString("label").trim(),
                        description = obj.optString("description").trim()
                    )
                }
            }

            else -> element.jsonPrimitiveOrNull()?.contentOrNull?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { FdroidAntiFeatureSnapshot(id = it) }
        }
    }.orEmpty()
}

private fun JsonArray?.toStringList(): List<String> {
    return this?.mapNotNull { element ->
        element.jsonPrimitiveOrNull()?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
    }.orEmpty()
}
