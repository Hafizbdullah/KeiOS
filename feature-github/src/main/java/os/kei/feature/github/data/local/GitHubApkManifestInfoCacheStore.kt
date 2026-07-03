package os.kei.feature.github.data.local

import com.tencent.mmkv.MMKV
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import os.kei.core.json.encodeCompact
import os.kei.core.json.jsonPrimitiveOrNull
import os.kei.core.json.optArray
import os.kei.core.json.optLong
import os.kei.core.json.optObject
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.core.prefs.KeiMmkv
import os.kei.feature.github.data.remote.GitHubApkManifestInfoCache
import os.kei.feature.github.model.GitHubApkManifestInfo
import os.kei.feature.github.model.GitHubApkManifestMetadata
import os.kei.feature.github.model.GitHubApkManifestNode
import os.kei.feature.github.model.GitHubApkSignatureInfo
import java.security.MessageDigest

object GitHubApkManifestInfoCacheStore : GitHubApkManifestInfoCache {
    private const val KV_ID = "github_apk_manifest_info_cache"
    private const val KEY_INDEX = "entry_index"
    private const val MIN_CACHE_AGE_HOURS = 24L
    private const val MAX_CACHE_AGE_HOURS = 24L * 7L
    private const val MAX_ENTRY_COUNT = 512

    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }
    private val indexLock = Any()
    private var cachedIndex: MutableSet<String>? = null

    override fun load(
        cacheKey: String,
        refreshIntervalHours: Int
    ): GitHubApkManifestInfo? =
        runCatching {
            loadOrThrow(cacheKey = cacheKey, refreshIntervalHours = refreshIntervalHours)
        }.getOrNull()

    override fun save(
        cacheKey: String,
        info: GitHubApkManifestInfo
    ) {
        runCatching {
            saveOrThrow(cacheKey = cacheKey, info = info)
        }
    }

    override fun remove(cacheKey: String) {
        runCatching {
            val normalizedKey = cacheKey.trim()
            if (normalizedKey.isBlank()) return@runCatching
            val id = keyId(normalizedKey)
            val kv = kv()
            kv.removeValueForKey(entryStoreKey(id))
            removeIndex(id, kv)
        }
    }

    fun clearAll() {
        runCatching {
            val kv = kv()
            loadIndex(kv).forEach { id ->
                kv.removeValueForKey(entryStoreKey(id))
            }
            kv.removeValueForKey(KEY_INDEX)
            synchronized(indexLock) {
                cachedIndex = mutableSetOf()
            }
            kv.trim()
        }
    }

    fun cachedEntryCount(): Int =
        runCatching {
            loadIndex().size
        }.getOrDefault(0)

    private fun loadOrThrow(
        cacheKey: String,
        refreshIntervalHours: Int
    ): GitHubApkManifestInfo? {
        val normalizedKey = cacheKey.trim()
        if (normalizedKey.isBlank()) return null
        val id = keyId(normalizedKey)
        val raw = kv().decodeString(entryStoreKey(id), "").orEmpty()
        if (raw.isBlank()) return null
        val root = raw.parseJsonObjectOrNull() ?: return clearAndNull(id)
        if (root.optString("cacheKey").trim() != normalizedKey) return clearAndNull(id)
        val syncedAtMs = root.optLong("syncedAtMs", 0L).coerceAtLeast(0L)
        val intervalMs = cacheMaxAgeMs(refreshIntervalHours)
        val expired = syncedAtMs <= 0L ||
            (System.currentTimeMillis() - syncedAtMs).coerceAtLeast(0L) >= intervalMs
        if (expired) return clearAndNull(id)
        return decodeInfo(root.optObject("manifest")) ?: clearAndNull(id)
    }

    private fun saveOrThrow(
        cacheKey: String,
        info: GitHubApkManifestInfo
    ) {
        val normalizedKey = cacheKey.trim()
        if (normalizedKey.isBlank()) return
        val id = keyId(normalizedKey)
        val payload = buildJsonObject {
            put("cacheKey", normalizedKey)
            put("syncedAtMs", System.currentTimeMillis())
            put("manifest", encodeInfo(info))
        }.encodeCompact()
        val kv = kv()
        kv.encode(entryStoreKey(id), payload)
        addIndex(id, kv)
    }

    private fun clearAndNull(id: String): GitHubApkManifestInfo? {
        val kv = kv()
        kv.removeValueForKey(entryStoreKey(id))
        removeIndex(id, kv)
        return null
    }

    private fun cacheMaxAgeMs(refreshIntervalHours: Int): Long {
        val hours = refreshIntervalHours
            .toLong()
            .coerceAtLeast(MIN_CACHE_AGE_HOURS)
            .coerceAtMost(MAX_CACHE_AGE_HOURS)
        return hours * 60L * 60L * 1000L
    }

    private fun kv(): MMKV = store

    private fun entryStoreKey(id: String): String = "entry_$id"

    private fun loadIndex(kv: MMKV = store): MutableSet<String> =
        synchronized(indexLock) {
            cachedIndex?.toMutableSet() ?: readIndex(kv).also { cachedIndex = it.toMutableSet() }
        }

    private fun readIndex(kv: MMKV): MutableSet<String> {
        val raw = kv.decodeString(KEY_INDEX, "").orEmpty()
        if (raw.isBlank()) return mutableSetOf()
        return raw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
    }

    private fun addIndex(id: String, kv: MMKV = store) {
        synchronized(indexLock) {
            val index = cachedIndex ?: readIndex(kv).also { cachedIndex = it.toMutableSet() }
            if (index.add(id)) {
                trimOverflowLocked(index, kv)
                saveIndexLocked(index, kv)
            }
        }
    }

    private fun removeIndex(id: String, kv: MMKV = store) {
        synchronized(indexLock) {
            val index = cachedIndex ?: readIndex(kv).also { cachedIndex = it.toMutableSet() }
            if (index.remove(id)) {
                saveIndexLocked(index, kv)
            }
        }
    }

    private fun saveIndexLocked(index: Set<String>, kv: MMKV = store) {
        val normalized = index.filter { it.isNotBlank() }.toSortedSet()
        kv.encode(KEY_INDEX, normalized.joinToString(","))
        cachedIndex = normalized.toMutableSet()
    }

    private fun trimOverflowLocked(index: MutableSet<String>, kv: MMKV) {
        if (index.size <= MAX_ENTRY_COUNT) return
        val overflow = index.size - MAX_ENTRY_COUNT
        index.sortedBy { id ->
            kv.decodeString(entryStoreKey(id), "")
                .orEmpty()
                .parseJsonObjectOrNull()
                ?.optLong("syncedAtMs", 0L)
                ?: 0L
        }.take(overflow).forEach { id ->
            kv.removeValueForKey(entryStoreKey(id))
            index.remove(id)
        }
    }

    private fun encodeInfo(info: GitHubApkManifestInfo): JsonObject =
        buildJsonObject {
            put("assetName", info.assetName)
            put("fetchSource", info.fetchSource)
            put("appLabel", info.appLabel)
            put("packageName", info.packageName)
            put("versionName", info.versionName)
            put("versionCode", info.versionCode)
            put("minSdk", info.minSdk)
            put("targetSdk", info.targetSdk)
            put("releaseNotes", info.releaseNotes)
            putStringArray("nativeAbis", info.nativeAbis)
            putStringArray("permissions", info.permissions)
            putStringArray("features", info.features)
            put(
                "metadata",
                buildJsonArray {
                    info.metadata.forEach { item ->
                        add(
                            buildJsonObject {
                                put("name", item.name)
                                put("value", item.value)
                            },
                        )
                    }
                },
            )
            put(
                "manifestNodes",
                buildJsonArray {
                    info.manifestNodes.forEach { node ->
                        add(
                            buildJsonObject {
                                put("tagName", node.tagName)
                                put("displayName", node.displayName)
                                put(
                                    "attributes",
                                    buildJsonObject {
                                        node.attributes.forEach { (key, value) ->
                                            put(key, value)
                                        }
                                    },
                                )
                            },
                        )
                    }
                },
            )
            info.signatureInfo?.let { signature ->
                put("signatureInfo", encodeSignatureInfo(signature))
            }
        }

    private fun JsonObject.decodeStringList(key: String): List<String> =
        optArray(key)
            ?.mapNotNull { element ->
                element.jsonPrimitiveOrNull()
                    ?.contentOrNull
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
            .orEmpty()

    private fun decodeInfo(obj: JsonObject?): GitHubApkManifestInfo? {
        obj ?: return null
        val assetName = obj.optString("assetName").trim()
        val packageName = obj.optString("packageName").trim()
        val versionName = obj.optString("versionName").trim()
        val versionCode = obj.optString("versionCode").trim()
        if (assetName.isBlank() && packageName.isBlank() && versionName.isBlank() && versionCode.isBlank()) {
            return null
        }
        return GitHubApkManifestInfo(
            assetName = assetName,
            fetchSource = obj.optString("fetchSource").trim(),
            appLabel = obj.optString("appLabel").trim(),
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            minSdk = obj.optString("minSdk").trim(),
            targetSdk = obj.optString("targetSdk").trim(),
            nativeAbis = obj.decodeStringList("nativeAbis"),
            permissions = obj.decodeStringList("permissions"),
            features = obj.decodeStringList("features"),
            metadata = decodeMetadata(obj.optArray("metadata").orEmpty()),
            manifestNodes = decodeManifestNodes(obj.optArray("manifestNodes").orEmpty()),
            signatureInfo = decodeSignatureInfo(obj.optObject("signatureInfo")),
            releaseNotes = obj.optString("releaseNotes")
        )
    }

    private fun decodeMetadata(elements: List<kotlinx.serialization.json.JsonElement>): List<GitHubApkManifestMetadata> =
        elements.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val name = obj.optString("name").trim()
            if (name.isBlank()) return@mapNotNull null
            GitHubApkManifestMetadata(
                name = name,
                value = obj.optString("value").trim()
            )
        }

    private fun decodeManifestNodes(elements: List<kotlinx.serialization.json.JsonElement>): List<GitHubApkManifestNode> =
        elements.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val tagName = obj.optString("tagName").trim()
            val displayName = obj.optString("displayName").trim()
            if (tagName.isBlank() && displayName.isBlank()) return@mapNotNull null
            GitHubApkManifestNode(
                tagName = tagName,
                displayName = displayName,
                attributes = decodeAttributes(obj.optObject("attributes"))
            )
        }

    private fun decodeAttributes(obj: JsonObject?): Map<String, String> {
        obj ?: return emptyMap()
        return obj.mapNotNull { (key, value) ->
            val normalizedKey = key.trim()
            val normalizedValue = value.jsonPrimitiveOrNull()
                ?.contentOrNull
                ?.trim()
                .orEmpty()
            if (normalizedKey.isBlank() || normalizedValue.isBlank()) {
                null
            } else {
                normalizedKey to normalizedValue
            }
        }.toMap()
    }

    private fun encodeSignatureInfo(signature: GitHubApkSignatureInfo): JsonObject =
        buildJsonObject {
            put("entryName", signature.entryName)
            put("subject", signature.subject)
            put("issuer", signature.issuer)
            put("serialNumber", signature.serialNumber)
            put("algorithm", signature.algorithm)
            put("notBeforeMillis", signature.notBeforeMillis)
            put("notAfterMillis", signature.notAfterMillis)
            put("sha256", signature.sha256)
        }

    private fun decodeSignatureInfo(obj: JsonObject?): GitHubApkSignatureInfo? {
        obj ?: return null
        val entryName = obj.optString("entryName").trim()
        val sha256 = obj.optString("sha256").trim()
        if (entryName.isBlank() && sha256.isBlank()) return null
        return GitHubApkSignatureInfo(
            entryName = entryName,
            subject = obj.optString("subject").trim(),
            issuer = obj.optString("issuer").trim(),
            serialNumber = obj.optString("serialNumber").trim(),
            algorithm = obj.optString("algorithm").trim(),
            notBeforeMillis = obj.optLong("notBeforeMillis", -1L),
            notAfterMillis = obj.optLong("notAfterMillis", -1L),
            sha256 = sha256
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putStringArray(
        key: String,
        values: List<String>
    ) {
        put(
            key,
            buildJsonArray {
                values.forEach { value ->
                    val normalized = value.trim()
                    if (normalized.isNotBlank()) add(JsonPrimitive(normalized))
                }
            },
        )
    }

    private fun keyId(cacheKey: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(cacheKey.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
}
