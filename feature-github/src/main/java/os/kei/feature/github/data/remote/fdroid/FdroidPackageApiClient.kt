package os.kei.feature.github.data.remote.fdroid

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.io.SharedHttpClient
import os.kei.core.io.executeCancellable
import os.kei.core.io.stringLimitedBlocking
import os.kei.core.json.jsonArrayOrNull
import os.kei.core.json.jsonObjectOrNull
import os.kei.core.json.jsonPrimitiveOrNull
import os.kei.core.json.optArray
import os.kei.core.json.optObject
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

class FdroidPackageApiClient(
    private val client: OkHttpClient = defaultClient,
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork
) {
    suspend fun fetchPackage(
        repoBaseUrl: String,
        packageName: String
    ): Result<FdroidPackageSnapshot> = withContext(ioDispatcher) {
        fdroidPackageApiResult {
            val normalizedRepoUrl = repoBaseUrl.trim().trimEnd('/')
            require(normalizedRepoUrl.isNotBlank()) { "F-Droid repository URL is blank" }
            val normalizedPackageName = packageName.trim()
            require(normalizedPackageName.isNotBlank()) { "F-Droid package name is blank" }
            val request = Request.Builder()
                .url(packageApiUrl(normalizedRepoUrl, normalizedPackageName))
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json,*/*")
                .build()
            client.executeCancellable(request) { response ->
                check(response.isSuccessful) {
                    "F-Droid package API failed (HTTP ${response.code})"
                }
                val root = response.body.stringLimitedBlocking(MAX_PACKAGE_RESPONSE_BYTES)
                    .parseJsonObjectOrNull()
                    ?: error("F-Droid package API returned invalid JSON")
                root.toPackageSnapshot(
                    fallbackRepoUrl = normalizedRepoUrl,
                    fallbackPackageName = normalizedPackageName
                )
            }
        }
    }

    private fun packageApiUrl(
        normalizedRepoUrl: String,
        packageName: String
    ): String {
        val encodedPackage = packageName.urlEncode()
        return when {
            normalizedRepoUrl.equals("https://f-droid.org/repo", ignoreCase = true) ||
                normalizedRepoUrl.equals("http://f-droid.org/repo", ignoreCase = true) ->
                "https://f-droid.org/api/v1/packages/$encodedPackage"

            normalizedRepoUrl.endsWith("/repo", ignoreCase = true) ->
                "${normalizedRepoUrl.removeSuffixIgnoreCase("/repo")}/api/v1/packages/$encodedPackage"

            else -> "$normalizedRepoUrl/api/v1/packages/$encodedPackage"
        }
    }

    private fun JsonObject.toPackageSnapshot(
        fallbackRepoUrl: String,
        fallbackPackageName: String
    ): FdroidPackageSnapshot {
        val packageName = optString("packageName").trim()
            .ifBlank { optString("package_name").trim() }
            .ifBlank { fallbackPackageName }
        val versions = optArray("packages")
            ?.mapNotNull { element -> element.jsonObjectOrNull()?.toVersionSnapshot() }
            .orEmpty()
            .sortedByDescending { it.versionCode }
        return FdroidPackageSnapshot(
            repoUrl = fallbackRepoUrl,
            packageName = packageName,
            suggestedVersionCode = longValue("suggestedVersionCode")
                ?: longValue("suggested_version_code"),
            versions = versions
        )
    }

    private fun JsonObject.toVersionSnapshot(): FdroidVersionSnapshot? {
        val versionCode = longValue("versionCode")
            ?: longValue("version_code")
            ?: return null
        val apkName = optString("apkName").trim()
            .ifBlank { optString("apk_name").trim() }
            .ifBlank { optString("name").trim() }
        val apkPath = optString("apkPath").trim()
            .ifBlank { optString("apk_path").trim() }
            .ifBlank { optString("apkFile").trim() }
            .ifBlank { apkName }
        return FdroidVersionSnapshot(
            versionName = optString("versionName").trim()
                .ifBlank { optString("version_name").trim() },
            versionCode = versionCode,
            apkName = apkName,
            apkPath = apkPath,
            apkSha256 = optString("hash").trim()
                .ifBlank { optString("sha256").trim() }
                .ifBlank { optString("apkSha256").trim() },
            apkSizeBytes = longValue("size")
                ?: longValue("apkSize")
                ?: longValue("apk_size")
                ?: 0L,
            addedAtMillis = longValue("added")
                ?: longValue("addedAt")
                ?: longValue("added_at"),
            minSdk = intValue("minSdkVersion") ?: intValue("minSdk") ?: intValue("min_sdk"),
            targetSdk = intValue("targetSdkVersion") ?: intValue("targetSdk") ?: intValue("target_sdk"),
            nativeAbis = stringListValue("nativecode") + stringListValue("nativeCode"),
            signerSha256 = stringListValue("signer")
                .ifEmpty { stringListValue("signerSha256") },
            releaseChannels = stringListValue("releaseChannels"),
            whatsNew = localizedStringValue("whatsNew"),
            antiFeatures = antiFeatureSnapshots()
        )
    }

    private fun JsonObject.antiFeatureSnapshots(): List<FdroidAntiFeatureSnapshot> {
        val arrayValues = optArray("antiFeatures")
            ?: optArray("anti_features")
        if (arrayValues != null) {
            return arrayValues.mapNotNull { element ->
                when {
                    element is JsonPrimitive -> {
                        element.contentOrNull?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?.let { FdroidAntiFeatureSnapshot(id = it) }
                    }

                    element is JsonObject -> {
                        val id = element.optString("id").trim()
                            .ifBlank { element.optString("name").trim() }
                        id.takeIf { it.isNotBlank() }?.let {
                            FdroidAntiFeatureSnapshot(
                                id = it,
                                label = element.optString("label").trim(),
                                description = element.optString("description").trim()
                            )
                        }
                    }

                    else -> null
                }
            }
        }
        val objectValues = optObject("antiFeatures")
            ?: optObject("anti_features")
        return objectValues?.keys
            ?.map { key -> FdroidAntiFeatureSnapshot(id = key) }
            .orEmpty()
    }

    private fun JsonObject.localizedStringValue(key: String): String {
        val direct = optString(key).trim()
        if (direct.isNotBlank()) return direct
        val obj = optObject(key) ?: return ""
        return obj.optString("en-US").trim()
            .ifBlank { obj.optString("en").trim() }
            .ifBlank { obj.values.firstNotNullOfOrNull { element ->
                element.jsonPrimitiveOrNull()?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
            }.orEmpty() }
    }

    private fun JsonObject.longValue(key: String): Long? {
        val element = this[key] ?: return null
        return element.jsonPrimitiveOrNull()?.longOrNull
            ?: element.jsonPrimitiveOrNull()?.contentOrNull?.trim()?.toLongOrNull()
    }

    private fun JsonObject.intValue(key: String): Int? {
        val element = this[key] ?: return null
        return element.jsonPrimitiveOrNull()?.intOrNull
            ?: element.jsonPrimitiveOrNull()?.contentOrNull?.trim()?.toIntOrNull()
    }

    private fun JsonObject.stringListValue(key: String): List<String> {
        val element = this[key] ?: return emptyList()
        val primitive = element.jsonPrimitiveOrNull()?.contentOrNull
        if (primitive != null) {
            return primitive.split(',', ';', ' ')
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
        return element.jsonArrayOrNull()
            ?.mapNotNull { item ->
                item.jsonPrimitiveOrNull()?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
            }
            .orEmpty()
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
    }

    private fun String.removeSuffixIgnoreCase(suffix: String): String {
        return if (lowercase(Locale.ROOT).endsWith(suffix.lowercase(Locale.ROOT))) {
            dropLast(suffix.length)
        } else {
            this
        }
    }

    private companion object {
        const val USER_AGENT = "KeiOS-App/1.0 (Android)"
        const val MAX_PACKAGE_RESPONSE_BYTES = 8L * 1024L * 1024L
        val defaultClient: OkHttpClient = SharedHttpClient.base.newBuilder()
            .connectTimeout(12.seconds)
            .readTimeout(20.seconds)
            .callTimeout(28.seconds)
            .build()
    }
}

private inline fun <T> fdroidPackageApiResult(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
