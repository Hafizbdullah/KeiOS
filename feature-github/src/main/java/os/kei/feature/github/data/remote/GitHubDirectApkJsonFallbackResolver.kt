package os.kei.feature.github.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import os.kei.core.io.SharedHttpClient
import os.kei.core.io.cancellableResult
import os.kei.core.io.executeCancellable
import os.kei.core.versioning.VersionCandidate
import os.kei.core.versioning.VersionConfidence
import os.kei.core.versioning.VersioningEngine
import os.kei.core.json.jsonPrimitiveOrNull
import os.kei.core.json.optArray
import os.kei.core.json.optObject
import os.kei.core.json.parseJsonArrayOrNull
import os.kei.core.json.parseJsonObjectOrNull
import java.net.URI
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

data class GitHubDirectApkJsonFallback(
    val sourceUrl: String,
    val fileUrl: String,
    val versionName: String,
    val versionCode: String,
    val changelog: String
) {
    fun toAsset(): GitHubReleaseAssetFile {
        return GitHubReleaseAssetFile(
            name = directApkFileNameFromUrl(fileUrl).ifBlank { "remote.apk" },
            downloadUrl = fileUrl,
            sizeBytes = 0L,
            downloadCount = 0,
            contentType = "application/vnd.android.package-archive"
        )
    }
}

class GitHubDirectApkJsonFallbackResolver(
    private val client: OkHttpClient = defaultClient
) {
    suspend fun resolve(directApkUrl: String): Result<GitHubDirectApkJsonFallback?> = cancellableResult {
        val jsonUrl = directApkUrl.jsonFeedUrl() ?: return@cancellableResult null
        val request = Request.Builder()
            .url(jsonUrl)
            .get()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json,text/plain,*/*")
            .header("Cache-Control", "no-store")
            .header("Pragma", "no-cache")
            .build()
        client.executeCancellable(request) { response ->
            check(response.isSuccessful) {
                "direct APK companion JSON failed (HTTP ${response.code})"
            }
            val contentLength = response.body.contentLength()
            check(contentLength < 0L || contentLength <= MAX_JSON_FEED_CHARS) {
                "direct APK companion JSON is too large"
            }
            val raw = response.body.string()
            check(raw.length <= MAX_JSON_FEED_CHARS) {
                "direct APK companion JSON is too large"
            }
            val candidate = parseJsonFeedCandidates(raw)
                .latestCandidate()
                ?: return@executeCancellable null
            GitHubDirectApkJsonFallback(
                sourceUrl = jsonUrl,
                fileUrl = candidate.fileUrl,
                versionName = candidate.versionName,
                versionCode = candidate.versionCode,
                changelog = candidate.changelog,
            )
        }
    }

    private fun String.jsonFeedUrl(): String? {
        val value = trim()
        if (value.isBlank()) return null
        if (value.endsWith(".json", ignoreCase = true)) return value
        return "$value.json"
    }

    private fun parseJsonFeedCandidates(raw: String): List<DirectApkJsonCandidate> {
        val trimmed = raw.trim()
        val objects = when {
            trimmed.startsWith("{") -> {
                val root = trimmed.parseJsonObjectOrNull() ?: return emptyList()
                buildList { root.collectFeedObjects(this, depth = 0) }
            }

            trimmed.startsWith("[") -> buildList {
                trimmed.parseJsonArrayOrNull()?.collectFeedObjects(this, depth = 0)
            }
            else -> emptyList()
        }
        return objects.mapNotNull { obj -> obj.toFeedCandidate() }
    }

    private fun JsonObject.collectFeedObjects(
        destination: MutableList<JsonObject>,
        depth: Int,
    ) {
        if (depth > MAX_JSON_NESTING_DEPTH) return
        if (firstNonBlankString(*fileUrlKeys).isNotBlank()) {
            destination += this
        }
        feedArrayKeys.forEach { key ->
            optArray(key)?.collectFeedObjects(destination, depth + 1)
        }
        feedObjectKeys.forEach { key ->
            optObject(key)?.collectFeedObjects(destination, depth + 1)
        }
    }

    private fun JsonArray.collectFeedObjects(
        destination: MutableList<JsonObject>,
        depth: Int,
    ) {
        if (depth > MAX_JSON_NESTING_DEPTH) return
        for (element in this) {
            val obj = element as? JsonObject ?: continue
            obj.collectFeedObjects(destination, depth + 1)
        }
    }

    private fun JsonObject.toFeedCandidate(): DirectApkJsonCandidate? {
        val fileUrl = firstNonBlankString(*fileUrlKeys)
        if (fileUrl.isBlank()) return null
        return DirectApkJsonCandidate(
            fileUrl = fileUrl,
            versionName = firstNonBlankString(*versionNameKeys),
            versionCode = firstNonBlankString(*versionCodeKeys),
            changelog = firstNonBlankString(*changelogKeys),
            publishedAtMillis = firstNonBlankString(*publishedAtKeys)
                .parseIsoInstantOrNull(),
        )
    }

    private fun List<DirectApkJsonCandidate>.latestCandidate(): DirectApkJsonCandidate? {
        var latest: DirectApkJsonCandidate? = null
        for (candidate in this) {
            val current = latest
            if (current == null || compareCandidates(current, candidate) < 0) {
                latest = candidate
            }
        }
        return latest
    }

    private fun compareCandidates(
        left: DirectApkJsonCandidate,
        right: DirectApkJsonCandidate,
    ): Int {
        val leftCode = left.versionCode.toLongOrNull()
        val rightCode = right.versionCode.toLongOrNull()
        if (leftCode != null && rightCode != null && leftCode != rightCode) {
            return leftCode.compareTo(rightCode)
        }
        val versionComparison = VersioningEngine.compareRemoteCandidateSets(
            leftCandidates = left.versionName.toVersionCandidates(),
            rightCandidates = right.versionName.toVersionCandidates(),
        )
        if (
            versionComparison != null &&
            versionComparison.confidence != VersionConfidence.Low &&
            versionComparison.order.legacyValue != 0
        ) {
            return versionComparison.order.legacyValue
        }
        return compareValues(
            left.publishedAtMillis ?: Long.MIN_VALUE,
            right.publishedAtMillis ?: Long.MIN_VALUE,
        )
    }

    private fun String.toVersionCandidates(): List<VersionCandidate> {
        if (isBlank()) return emptyList()
        return listOf(VersionCandidate(value = this, sourcePriority = 0))
    }

    private fun String.parseIsoInstantOrNull(): Long? {
        return runCatching { Instant.parse(trim()).toEpochMilli() }.getOrNull()
    }

    private fun JsonObject.firstNonBlankString(vararg keys: String): String {
        keys.forEach { key ->
            val element = this[key]?.takeUnless { it is JsonNull } ?: return@forEach
            val value = element.jsonPrimitiveOrNull()?.contentOrNull?.trim()
                ?: element.toString().trim()
            if (value.isNotBlank() && value != "null") return value
        }
        return ""
    }

    private companion object {
        const val USER_AGENT = "KeiOS-App/1.0 (Android)"
        const val MAX_JSON_FEED_CHARS = 512 * 1024
        const val MAX_JSON_NESTING_DEPTH = 4
        val fileUrlKeys = arrayOf("file_url", "download_url", "apk_url", "url")
        val versionNameKeys = arrayOf(
            "version_name",
            "versionName",
            "version",
            "tag_name",
            "tag",
        )
        val versionCodeKeys = arrayOf(
            "version_code",
            "versionCode",
            "build",
            "build_number",
        )
        val changelogKeys = arrayOf(
            "changelog",
            "release_notes",
            "releaseNotes",
            "notes",
            "body",
        )
        val publishedAtKeys = arrayOf(
            "published_at",
            "publishedAt",
            "updated_at",
            "updatedAt",
            "date",
        )
        val feedArrayKeys = arrayOf("releases", "items", "assets", "downloads", "versions")
        val feedObjectKeys = arrayOf("release", "latest", "android")
        val defaultClient: OkHttpClient = SharedHttpClient.base.newBuilder()
            .connectTimeout(12.seconds)
            .readTimeout(20.seconds)
            .callTimeout(28.seconds)
            .build()
    }

    private data class DirectApkJsonCandidate(
        val fileUrl: String,
        val versionName: String,
        val versionCode: String,
        val changelog: String,
        val publishedAtMillis: Long?,
    )
}

fun directApkFileNameFromUrl(url: String): String {
    return runCatching {
        URI(url).path.substringAfterLast('/').trim()
    }.getOrDefault("")
}
