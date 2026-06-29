package os.kei.feature.github.domain.fdroid

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidRepositoryIndexClient
import os.kei.feature.github.data.remote.fdroid.FdroidRepositorySnapshot
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.buildFdroidRepositoryTrackIdentity
import os.kei.feature.github.model.isFdroidRepositoryTrack
import java.util.Locale

private const val DEFAULT_FDROID_REPOSITORY_BATCH_SIZE = 4

fun interface FdroidRepositorySnapshotProvider {
    suspend fun loadRepositorySnapshot(
        repoUrl: String,
        forceRefresh: Boolean
    ): Result<FdroidRepositorySnapshot>
}

class FdroidRepositoryIndexSnapshotProvider(
    private val client: FdroidRepositoryIndexClient = FdroidRepositoryIndexClient()
) : FdroidRepositorySnapshotProvider {
    override suspend fun loadRepositorySnapshot(
        repoUrl: String,
        forceRefresh: Boolean
    ): Result<FdroidRepositorySnapshot> {
        return client.fetchIndexV2(repoUrl)
    }
}

class FdroidBatchPackageSnapshotProvider(
    trackedItems: List<GitHubTrackedApp>,
    private val packageProvider: FdroidPackageSnapshotProvider = FdroidPackageApiSnapshotProvider(),
    private val repositoryProvider: FdroidRepositorySnapshotProvider = FdroidRepositoryIndexSnapshotProvider(),
    private val minRepositoryBatchSize: Int = DEFAULT_FDROID_REPOSITORY_BATCH_SIZE
) : FdroidPackageSnapshotProvider, FdroidPackageLookupSnapshotProvider {
    private val repositoryModeUrls: Set<String> =
        trackedItems
            .asSequence()
            .filter { item -> item.isFdroidRepositoryTrack() }
            .mapNotNull { item -> item.fdroidIdentityOrNull()?.normalizedRepoUrl }
            .groupingBy { repoUrl -> repoUrl }
            .eachCount()
            .filterValues { count -> count >= minRepositoryBatchSize.coerceAtLeast(2) }
            .keys

    private val mutex = Mutex()
    private val repositoryCache = mutableMapOf<String, Result<FdroidRepositorySnapshot>>()
    private val packageCache = mutableMapOf<String, Result<FdroidPackageSnapshot>>()
    private val repositoryInFlight = mutableMapOf<String, CompletableDeferred<Result<FdroidRepositorySnapshot>>>()
    private val packageInFlight = mutableMapOf<String, CompletableDeferred<Result<FdroidPackageSnapshot>>>()

    override suspend fun loadPackageSnapshot(
        item: GitHubTrackedApp,
        forceRefresh: Boolean
    ): Result<FdroidPackageSnapshot> {
        return loadPackageLookupSnapshot(item, forceRefresh)
            .map { lookup -> lookup.packageSnapshot }
    }

    override suspend fun loadPackageLookupSnapshot(
        item: GitHubTrackedApp,
        forceRefresh: Boolean
    ): Result<FdroidPackageLookupSnapshot> {
        return loadLookupSnapshot(item, forceRefresh)
    }

    private suspend fun loadLookupSnapshot(
        item: GitHubTrackedApp,
        forceRefresh: Boolean
    ): Result<FdroidPackageLookupSnapshot> {
        val identity = item.fdroidIdentityOrNull()
            ?: return Result.failure(IllegalArgumentException("invalid F-Droid repository URL or package"))
        return if (identity.normalizedRepoUrl in repositoryModeUrls) {
            loadLookupFromRepository(
                repoUrl = identity.normalizedRepoUrl,
                packageName = identity.packageName,
                forceRefresh = forceRefresh
            )
        } else {
            loadLookupFromApi(
                item = item,
                repoUrl = identity.normalizedRepoUrl,
                packageName = identity.packageName,
                forceRefresh = forceRefresh
            )
        }
    }

    private suspend fun loadLookupFromRepository(
        repoUrl: String,
        packageName: String,
        forceRefresh: Boolean
    ): Result<FdroidPackageLookupSnapshot> {
        return loadRepository(repoUrl, forceRefresh).mapCatching { repository ->
            val packageSnapshot = repository.packageSnapshot(packageName)
                ?: error("F-Droid package $packageName was not found in $repoUrl")
            FdroidPackageLookupSnapshot(
                packageSnapshot = packageSnapshot,
                repositorySnapshot = repository
            )
        }
    }

    private suspend fun loadRepository(
        repoUrl: String,
        forceRefresh: Boolean
    ): Result<FdroidRepositorySnapshot> {
        val key = repoUrl.cacheKey()
        val inFlight = mutex.withLock {
            repositoryCache[key]?.let { cached -> return cached }
            repositoryInFlight[key]?.let { existing ->
                return@withLock InFlight(existing, owner = false)
            }
            val created = CompletableDeferred<Result<FdroidRepositorySnapshot>>()
            repositoryInFlight[key] = created
            InFlight(created, owner = true)
        }
        if (!inFlight.owner) {
            return inFlight.deferred.await()
        }
        val result =
            try {
                repositoryProvider.loadRepositorySnapshot(repoUrl, forceRefresh)
            } catch (error: CancellationException) {
                mutex.withLock { repositoryInFlight.remove(key) }
                inFlight.deferred.completeExceptionally(error)
                throw error
            } catch (error: Throwable) {
                Result.failure(error)
            }
        mutex.withLock {
            repositoryCache[key] = result
            repositoryInFlight.remove(key)
        }
        inFlight.deferred.complete(result)
        return result
    }

    private suspend fun loadLookupFromApi(
        item: GitHubTrackedApp,
        repoUrl: String,
        packageName: String,
        forceRefresh: Boolean
    ): Result<FdroidPackageLookupSnapshot> {
        val apiResult = loadPackageFromApi(
            item = item,
            repoUrl = repoUrl,
            packageName = packageName,
            forceRefresh = forceRefresh
        )
        if (apiResult.isSuccess) {
            return apiResult.map { snapshot ->
                FdroidPackageLookupSnapshot(packageSnapshot = snapshot)
            }
        }
        val repositoryResult = loadLookupFromRepository(
            repoUrl = repoUrl,
            packageName = packageName,
            forceRefresh = forceRefresh
        )
        if (repositoryResult.isSuccess) return repositoryResult
        val apiError = apiResult.exceptionOrNull()
        val repositoryError = repositoryResult.exceptionOrNull()
        return Result.failure(
            IllegalStateException(
                "F-Droid package API failed: ${apiError?.message ?: "unknown"}; " +
                    "repository index fallback failed: ${repositoryError?.message ?: "unknown"}",
                repositoryError ?: apiError
            )
        )
    }

    private suspend fun loadPackageFromApi(
        item: GitHubTrackedApp,
        repoUrl: String,
        packageName: String,
        forceRefresh: Boolean
    ): Result<FdroidPackageSnapshot> {
        val key = "$repoUrl|$packageName".cacheKey()
        val inFlight = mutex.withLock {
            packageCache[key]?.let { cached -> return cached }
            packageInFlight[key]?.let { existing ->
                return@withLock InFlight(existing, owner = false)
            }
            val created = CompletableDeferred<Result<FdroidPackageSnapshot>>()
            packageInFlight[key] = created
            InFlight(created, owner = true)
        }
        if (!inFlight.owner) {
            return inFlight.deferred.await()
        }
        val result =
            try {
                packageProvider.loadPackageSnapshot(item, forceRefresh)
            } catch (error: CancellationException) {
                mutex.withLock { packageInFlight.remove(key) }
                inFlight.deferred.completeExceptionally(error)
                throw error
            } catch (error: Throwable) {
                Result.failure(error)
            }
        mutex.withLock {
            packageCache[key] = result
            packageInFlight.remove(key)
        }
        inFlight.deferred.complete(result)
        return result
    }

    private fun GitHubTrackedApp.fdroidIdentityOrNull() =
        buildFdroidRepositoryTrackIdentity(repoUrl, packageName)

    private fun String.cacheKey(): String = trim().lowercase(Locale.ROOT)

    private data class InFlight<T>(
        val deferred: CompletableDeferred<Result<T>>,
        val owner: Boolean
    )
}
