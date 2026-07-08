package os.kei.feature.github.data.remote.fdroid

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.io.SharedHttpClient
import os.kei.core.io.executeCancellable
import os.kei.core.json.jsonObjectOrNull
import os.kei.core.json.jsonPrimitiveOrNull
import os.kei.core.json.optArray
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.feature.github.model.buildFdroidRepositoryTrackIdentity
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

data class FdroidSearchApiApp(
    val name: String,
    val summary: String,
    val iconUrl: String,
    val packagePageUrl: String,
    val packageName: String
)

open class FdroidSearchApiClient(
    private val client: OkHttpClient = defaultClient,
    private val searchEndpoint: String = DEFAULT_SEARCH_ENDPOINT,
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork
) {
    open suspend fun searchApps(
        query: String,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<FdroidSearchApiApp>> = withContext(ioDispatcher) {
        fdroidSearchApiResult {
            val normalizedQuery = query.trim()
            require(normalizedQuery.isNotBlank()) { "F-Droid search query is blank" }
            val url = searchEndpoint.toHttpUrlBuilder()
                .addQueryParameter("q", normalizedQuery)
                .build()
            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json,*/*")
                .build()
            client.executeCancellable(request) { response ->
                check(response.isSuccessful) {
                    "F-Droid search API failed (HTTP ${response.code})"
                }
                val root = response.body.string().parseJsonObjectOrNull()
                    ?: error("F-Droid search API returned invalid JSON")
                root.optArray("apps")
                    ?.mapNotNull { element -> element.jsonObjectOrNull()?.toSearchApiApp() }
                    .orEmpty()
                    .take(limit.coerceIn(1, 50))
            }
        }
    }

    private fun JsonObject.toSearchApiApp(): FdroidSearchApiApp? {
        val packagePageUrl = optString("url").trim()
        val packageName = buildFdroidRepositoryTrackIdentity(packagePageUrl)?.packageName.orEmpty()
        if (packageName.isBlank()) return null
        return FdroidSearchApiApp(
            name = optString("name").trim(),
            summary = optString("summary").trim(),
            iconUrl = optString("icon").trim()
                .ifBlank {
                    this["icon"]?.jsonPrimitiveOrNull()?.contentOrNull?.trim().orEmpty()
                },
            packagePageUrl = packagePageUrl,
            packageName = packageName
        )
    }

    private fun String.toHttpUrlBuilder(): HttpUrl.Builder {
        return toHttpUrlOrNull()?.newBuilder()
            ?: error("Invalid F-Droid search endpoint")
    }

    private companion object {
        const val DEFAULT_SEARCH_ENDPOINT = "https://search.f-droid.org/api/search_apps"
        const val USER_AGENT = "KeiOS-App/1.0 (Android)"
        const val DEFAULT_LIMIT = 12
        val defaultClient: OkHttpClient = SharedHttpClient.base.newBuilder()
            .connectTimeout(8.seconds)
            .readTimeout(12.seconds)
            .callTimeout(18.seconds)
            .build()
    }
}

private inline fun <T> fdroidSearchApiResult(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
