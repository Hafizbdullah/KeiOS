package os.kei.feature.github.data.remote

import os.kei.feature.github.GitHubSingleFlight
import os.kei.feature.github.data.local.GitHubApkManifestInfoCacheStore
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.model.GitHubApkManifestInfo
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.githubAssetSourceSignature
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

interface GitHubApkManifestInfoCache {
    fun load(
        cacheKey: String,
        refreshIntervalHours: Int
    ): GitHubApkManifestInfo?

    fun save(
        cacheKey: String,
        info: GitHubApkManifestInfo
    )

    fun remove(cacheKey: String)
}

class GitHubApkInfoRepository(
    private val manifestReader: GitHubApkManifestReader = GitHubApkManifestReader(),
    private val manifestCache: GitHubApkManifestInfoCache = GitHubApkManifestInfoCacheStore
) {
    suspend fun inspect(
        asset: GitHubReleaseAssetFile,
        lookupConfig: GitHubLookupConfig,
        forceRefresh: Boolean = false
    ): Result<GitHubApkManifestInfo> {
        val cacheKey = buildInspectCacheKey(asset, lookupConfig)
        if (forceRefresh) {
            completedInspectCache.remove(cacheKey)
            manifestCache.remove(cacheKey)
        }
        completedInspectCache[cacheKey]?.let { cached ->
            return Result.success(cached)
        }
        if (!forceRefresh) {
            manifestCache.load(
                cacheKey = cacheKey,
                refreshIntervalHours = refreshIntervalHoursForCache()
            )?.let { cached ->
                completedInspectCache[cacheKey] = cached
                return Result.success(cached)
            }
        }
        return inFlightInspectCache.run(cacheKey) {
            val result = runCatching {
                manifestReader.inspect(asset = asset, lookupConfig = lookupConfig)
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                Result.failure(error)
            }
            result.getOrNull()?.let { info ->
                if (completedInspectCache.size >= MAX_COMPLETED_INSPECT_CACHE_SIZE) {
                    completedInspectCache.clear()
                }
                completedInspectCache[cacheKey] = info
                manifestCache.save(cacheKey = cacheKey, info = info)
            }
            result
        }
    }

    private fun buildInspectCacheKey(
        asset: GitHubReleaseAssetFile,
        lookupConfig: GitHubLookupConfig
    ): String {
        return listOf(
            asset.name.trim(),
            asset.downloadUrl.trim(),
            asset.apiAssetUrl.trim(),
            asset.digest.trim(),
            asset.sizeBytes.toString(),
            (asset.updatedAtMillis ?: -1L).toString(),
            lookupConfig.githubAssetSourceSignature(),
            lookupConfig.selectedStrategy.storageId,
            lookupConfig.apiToken.trim()
                .takeIf { it.isNotBlank() }
                ?.let { token -> "token:${token.hashCode()}" }
                ?: "guest"
        ).joinToString("|")
    }

    private fun refreshIntervalHoursForCache(): Int =
        runCatching {
            GitHubTrackStore.loadRefreshIntervalHours()
        }.getOrDefault(DEFAULT_REFRESH_INTERVAL_HOURS)

    companion object {
        private const val MAX_COMPLETED_INSPECT_CACHE_SIZE = 128
        private const val DEFAULT_REFRESH_INTERVAL_HOURS = 24
        private val completedInspectCache = ConcurrentHashMap<String, GitHubApkManifestInfo>()
        private val inFlightInspectCache = GitHubSingleFlight<String, GitHubApkManifestInfo>()

        internal fun clearMemoryCachesForTest() {
            completedInspectCache.clear()
        }
    }
}
