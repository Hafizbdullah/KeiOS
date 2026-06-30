package os.kei.feature.github.data.remote.fdroid

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.io.SharedHttpClient
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

private const val FDROID_INDEX_STREAM_BUFFER_SIZE = 64 * 1024

class FdroidRepositoryIndexClient(
    private val client: OkHttpClient = defaultClient,
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork
) {
    suspend fun searchIndexV2(
        repoBaseUrl: String,
        query: String,
        packageName: String,
        limit: Int
    ): Result<FdroidRepositorySnapshot> = withContext(ioDispatcher) {
        fdroidRepositoryIndexResult {
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
                    "F-Droid index-v2 search failed (HTTP ${response.code})"
                }
                response.body.charStream().buffered(FDROID_INDEX_STREAM_BUFFER_SIZE).use { reader ->
                    FdroidIndexV2StreamParser
                        .searchIndex(
                            repoUrl = normalizedRepoUrl,
                            reader = reader,
                            query = query,
                            packageName = packageName,
                            limit = limit
                        )
                        .getOrThrow()
                }
            }
        }
    }

    suspend fun fetchIndexV2Packages(
        repoBaseUrl: String,
        packageNames: Set<String>
    ): Result<FdroidRepositorySnapshot> = withContext(ioDispatcher) {
        fdroidRepositoryIndexResult {
            val normalizedRepoUrl = repoBaseUrl.trim().trimEnd('/')
            require(normalizedRepoUrl.isNotBlank()) { "F-Droid repository URL is blank" }
            require(packageNames.any { name -> name.trim().isNotBlank() }) {
                "F-Droid package names are blank"
            }
            val request = Request.Builder()
                .url("$normalizedRepoUrl/index-v2.json")
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json,*/*")
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) {
                    "F-Droid index-v2 package fetch failed (HTTP ${response.code})"
                }
                response.body.charStream().buffered(FDROID_INDEX_STREAM_BUFFER_SIZE).use { reader ->
                    FdroidIndexV2StreamParser
                        .loadPackages(
                            repoUrl = normalizedRepoUrl,
                            reader = reader,
                            packageNames = packageNames
                        )
                        .getOrThrow()
                }
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

private inline fun <T> fdroidRepositoryIndexResult(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
