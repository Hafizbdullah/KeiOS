package os.kei.feature.github.data.remote.fdroid

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import os.kei.core.io.SharedHttpClient
import kotlin.time.Duration.Companion.seconds

class FdroidRepositoryIndexClient(
    private val client: OkHttpClient = defaultClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun fetchIndexV2(
        repoBaseUrl: String
    ): Result<FdroidRepositorySnapshot> = withContext(ioDispatcher) {
        runCatching {
            val normalizedRepoUrl = repoBaseUrl.trim().trimEnd('/')
            require(normalizedRepoUrl.isNotBlank()) { "F-Droid repository URL is blank" }
            val request = Request.Builder()
                .url("$normalizedRepoUrl/index-v2.json")
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json,*/*")
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) {
                    "F-Droid index-v2 fetch failed (HTTP ${response.code})"
                }
                val rawJson = response.body.string()
                FdroidIndexV2Parser
                    .parseIndex(repoUrl = normalizedRepoUrl, rawJson = rawJson)
                    .getOrThrow()
            }
        }
    }

    private companion object {
        const val USER_AGENT = "KeiOS-App/1.0 (Android)"
        val defaultClient: OkHttpClient = SharedHttpClient.base.newBuilder()
            .connectTimeout(12.seconds)
            .readTimeout(60.seconds)
            .callTimeout(90.seconds)
            .build()
    }
}
