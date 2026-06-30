package os.kei.feature.github.domain.fdroid

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.github.GitHubExecution
import os.kei.feature.github.data.remote.fdroid.FdroidPackageApiClient
import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidRepositoryIndexClient
import os.kei.feature.github.data.remote.fdroid.FdroidRepositorySnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidSearchApiApp
import os.kei.feature.github.data.remote.fdroid.FdroidSearchApiClient
import os.kei.feature.github.model.FdroidAppSearchCandidate
import os.kei.feature.github.model.FdroidAppSearchFailure
import os.kei.feature.github.model.FdroidAppSearchRepoReport
import os.kei.feature.github.model.FdroidAppSearchRequest
import os.kei.feature.github.model.FdroidAppSearchResult
import os.kei.feature.github.model.FdroidAppSearchSource
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.buildFdroidRepositoryTrackIdentity
import os.kei.feature.github.model.normalizedFdroidRepoUrlKey
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class FdroidAppSearchService(
    private val searchApiClient: FdroidSearchApiClient = FdroidSearchApiClient(),
    private val packageApiProvider: FdroidPackageSearchProvider =
        FdroidPackageApiSearchProvider(),
    private val repositorySearchProvider: FdroidRepositorySearchProvider =
        FdroidRepositoryIndexSearchProvider(),
    private val dispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun search(request: FdroidAppSearchRequest): Result<FdroidAppSearchResult> =
        withContext(dispatcher) {
            fdroidAppSearchResult {
                val normalizedQuery = request.query.trim()
                val normalizedPackageName = request.packageName.trim()
                require(normalizedQuery.isNotBlank() || normalizedPackageName.isNotBlank()) {
                    "F-Droid search query or package name is required"
                }
                val repoUrls = request.repoUrls
                    .map { url -> url.trim().trimEnd('/') }
                    .filter { url -> url.isNotBlank() }
                    .distinctBy { url -> url.normalizedFdroidRepoUrlKey() }
                require(repoUrls.isNotEmpty()) { "F-Droid repository scope is blank" }

                val repoResults =
                    GitHubExecution.mapOrderedBounded(
                        items = repoUrls,
                        maxConcurrency = 2,
                        dispatcher = dispatcher
                    ) { repoUrl ->
                        timedSearchRepo(
                            repoUrl = repoUrl,
                            query = normalizedQuery,
                            packageName = normalizedPackageName,
                            limit = request.limit,
                            includeOfficialSearchApi = request.includeOfficialSearchApi,
                            forceRefresh = request.forceRefresh
                        )
                    }
                val candidates = repoResults
                    .flatMap { result -> result.candidates }
                    .distinctBy { candidate ->
                        candidate.repoUrl.normalizedFdroidRepoUrlKey() +
                            "|" +
                            candidate.packageName.lowercase(Locale.ROOT)
                    }
                    .sortedWith(
                        compareByDescending<FdroidAppSearchCandidate> { candidate -> candidate.score }
                            .thenBy { candidate -> candidate.repoDisplayName.lowercase(Locale.ROOT) }
                            .thenBy { candidate -> candidate.displayName.lowercase(Locale.ROOT) }
                    )
                    .take(request.limit.coerceIn(1, 50))
                FdroidAppSearchResult(
                    query = normalizedQuery,
                    packageName = normalizedPackageName,
                    searchedRepoUrls = repoUrls,
                    candidates = candidates,
                    failures = repoResults.flatMap { result -> result.failures },
                    repoReports = repoResults.map { result -> result.toReport() }
                )
            }
        }

    private suspend fun timedSearchRepo(
        repoUrl: String,
        query: String,
        packageName: String,
        limit: Int,
        includeOfficialSearchApi: Boolean,
        forceRefresh: Boolean
    ): RepoSearchResult {
        val startedAtMillis = clock()
        return searchRepo(
            repoUrl = repoUrl,
            query = query,
            packageName = packageName,
            limit = limit,
            includeOfficialSearchApi = includeOfficialSearchApi,
            forceRefresh = forceRefresh
        ).copy(
            repoUrl = repoUrl,
            elapsedMillis = (clock() - startedAtMillis).coerceAtLeast(0L)
        )
    }

    private suspend fun searchRepo(
        repoUrl: String,
        query: String,
        packageName: String,
        limit: Int,
        includeOfficialSearchApi: Boolean,
        forceRefresh: Boolean
    ): RepoSearchResult {
        if (includeOfficialSearchApi && repoUrl.isFdroidMainRepo()) {
            val apiResult = searchOfficialRepo(repoUrl, query, packageName, limit)
            val packageApiResult =
                if (packageName.isNotBlank()) {
                    searchOfficialPackageApi(repoUrl, query, packageName)
                } else {
                    RepoSearchResult()
                }
            val officialCandidates =
                mergeOfficialCandidates(apiResult.candidates, packageApiResult.candidates, limit)
            if (officialCandidates.isNotEmpty()) {
                return RepoSearchResult(candidates = officialCandidates)
            }
            if (apiResult.failures.isEmpty()) {
                return RepoSearchResult()
            }
            val indexResult = searchRepositoryIndex(repoUrl, query, packageName, limit, forceRefresh)
            if (indexResult.candidates.isNotEmpty()) {
                return indexResult
            }
            return RepoSearchResult(
                failures = apiResult.failures + packageApiResult.failures + indexResult.failures
            )
        }
        return searchRepositoryIndex(repoUrl, query, packageName, limit, forceRefresh)
    }

    private suspend fun searchOfficialRepo(
        repoUrl: String,
        query: String,
        packageName: String,
        limit: Int
    ): RepoSearchResult {
        val searchTexts =
            listOf(packageName, query)
                .map { value -> value.trim() }
                .filter { value -> value.isNotBlank() }
                .distinctBy { value -> value.searchKey() }
        val candidates = mutableListOf<FdroidAppSearchCandidate>()
        val failures = mutableListOf<FdroidAppSearchFailure>()
        for (searchText in searchTexts) {
            searchApiClient.searchApps(searchText, limit = limit).fold(
                onSuccess = { apps ->
                    candidates += apps
                        .asSequence()
                        .map { app ->
                            app.toCandidate(
                                repoUrl = repoUrl,
                                query = query,
                                packageName = packageName
                            )
                        }
                        .filter { candidate ->
                            packageName.isBlank() ||
                                query.isNotBlank() ||
                                candidate.packageName.equals(packageName, ignoreCase = true)
                        }
                        .toList()
                },
                onFailure = { error ->
                    failures += FdroidAppSearchFailure(
                        repoUrl = repoUrl,
                        message = error.message ?: error.javaClass.simpleName
                    )
                }
            )
        }
        val distinctCandidates = candidates
            .distinctBy { candidate -> candidate.packageName.lowercase(Locale.ROOT) }
            .sortedByDescending { candidate -> candidate.score }
            .take(limit.coerceIn(1, 50))
        return RepoSearchResult(
            candidates = distinctCandidates,
            failures = failures.takeIf { distinctCandidates.isEmpty() }.orEmpty()
        )
    }

    private suspend fun searchOfficialPackageApi(
        repoUrl: String,
        query: String,
        packageName: String
    ): RepoSearchResult {
        return packageApiProvider.fetchPackage(
            repoUrl = repoUrl,
            packageName = packageName
        ).fold(
            onSuccess = { snapshot ->
                RepoSearchResult(
                    candidates =
                        listOf(
                            snapshot.toCandidate(
                                repoUrl = repoUrl,
                                query = query
                            )
                        )
                )
            },
            onFailure = { error ->
                RepoSearchResult(
                    failures =
                        listOf(
                            FdroidAppSearchFailure(
                                repoUrl = repoUrl,
                                message = error.message ?: error.javaClass.simpleName
                            )
                        )
                )
            }
        )
    }

    private suspend fun searchRepositoryIndex(
        repoUrl: String,
        query: String,
        packageName: String,
        limit: Int,
        forceRefresh: Boolean
    ): RepoSearchResult {
        val result = repositorySearchProvider.searchRepository(
            repoUrl = repoUrl,
            query = query,
            packageName = packageName,
            limit = limit,
            forceRefresh = forceRefresh
        )
        return result.fold(
            onSuccess = { repository ->
                RepoSearchResult(
                    candidates = repository.searchPackages(
                        query = query,
                        packageName = packageName,
                        limit = limit
                    )
                )
            },
            onFailure = { error ->
                RepoSearchResult(
                    failures = listOf(
                        FdroidAppSearchFailure(
                            repoUrl = repoUrl,
                            message = error.message ?: error.javaClass.simpleName
                        )
                    )
                )
            }
        )
    }

    private fun FdroidRepositorySnapshot.searchPackages(
        query: String,
        packageName: String,
        limit: Int
    ): List<FdroidAppSearchCandidate> {
        val normalizedPackageName = packageName.trim()
        val normalizedQuery = query.trim()
        return packages.values
            .asSequence()
            .mapNotNull { snapshot ->
                val score = snapshot.matchScore(
                    query = normalizedQuery,
                    packageName = normalizedPackageName
                )
                if (score <= 0) return@mapNotNull null
                snapshot.toCandidate(
                    repository = this,
                    score = score,
                    source = FdroidAppSearchSource.RepositoryIndex
                )
            }
            .sortedWith(
                compareByDescending<FdroidAppSearchCandidate> { candidate -> candidate.score }
                    .thenBy { candidate -> candidate.displayName.lowercase(Locale.ROOT) }
            )
            .take(limit.coerceIn(1, 50))
            .toList()
    }

    private fun FdroidPackageSnapshot.matchScore(
        query: String,
        packageName: String
    ): Int {
        if (packageName.isNotBlank()) {
            if (this.packageName.equals(packageName, ignoreCase = true)) return 1000
            if (query.isBlank()) return 0
        }
        val normalizedQuery = query.searchKey()
        if (normalizedQuery.isBlank()) return 0
        val name = appName.searchKey()
        val pkg = this.packageName.searchKey()
        val summaryKey = summary.searchKey()
        val categoryKey = categories.joinToString(" ").searchKey()
        return when {
            name == normalizedQuery -> 900
            name.startsWith(normalizedQuery) -> 820
            pkg == normalizedQuery -> 780
            pkg.contains(normalizedQuery) -> 720
            name.contains(normalizedQuery) -> 680
            summaryKey.contains(normalizedQuery) -> 420
            categoryKey.contains(normalizedQuery) -> 260
            else -> 0
        }
    }

    private fun FdroidPackageSnapshot.toCandidate(
        repository: FdroidRepositorySnapshot,
        score: Int,
        source: FdroidAppSearchSource
    ): FdroidAppSearchCandidate {
        val latest = selectedSuggestedVersion ?: versions.maxByOrNull { version -> version.versionCode }
        return FdroidAppSearchCandidate(
            repoUrl = repository.repoUrl,
            repoDisplayName = repository.displayName(),
            repoPresetId = FdroidRepositoryPresets
                .presetForRepoUrl(repository.repoUrl)
                ?.id
                .orEmpty(),
            packageName = packageName,
            appName = appName,
            summary = summary,
            packagePageUrl = buildFdroidRepositoryTrackIdentity(
                rawUrl = repository.repoUrl,
                rawPackageName = packageName
            )?.packagePageUrl.orEmpty(),
            latestVersionName = latest?.versionName.orEmpty(),
            latestVersionCode = latest?.versionCode ?: -1L,
            versionCount = versions.size,
            categories = categories,
            antiFeatures = (antiFeatures.map { it.id } + latest?.antiFeatures.orEmpty().map { it.id })
                .distinct(),
            score = score,
            source = source
        )
    }

    private fun FdroidPackageSnapshot.toCandidate(
        repoUrl: String,
        query: String
    ): FdroidAppSearchCandidate {
        val latest = selectedSuggestedVersion ?: versions.maxByOrNull { version -> version.versionCode }
        return FdroidAppSearchCandidate(
            repoUrl = repoUrl,
            repoDisplayName = FdroidRepositoryPresets.Main.displayName,
            repoPresetId = FdroidRepositoryPresets.MAIN_ID,
            packageName = packageName,
            appName = appName.ifBlank { query.trim() }.ifBlank { packageName },
            summary = summary.ifBlank { description },
            packagePageUrl = buildFdroidRepositoryTrackIdentity(
                rawUrl = repoUrl,
                rawPackageName = packageName
            )?.packagePageUrl.orEmpty(),
            latestVersionName = latest?.versionName.orEmpty(),
            latestVersionCode = latest?.versionCode ?: -1L,
            versionCount = versions.size,
            categories = categories,
            antiFeatures = (antiFeatures.map { it.id } + latest?.antiFeatures.orEmpty().map { it.id })
                .distinct(),
            score = 1000,
            source = FdroidAppSearchSource.PackageApi
        )
    }

    private fun mergeOfficialCandidates(
        apiCandidates: List<FdroidAppSearchCandidate>,
        packageApiCandidates: List<FdroidAppSearchCandidate>,
        limit: Int
    ): List<FdroidAppSearchCandidate> {
        return (packageApiCandidates + apiCandidates)
            .distinctBy { candidate -> candidate.packageName.lowercase(Locale.ROOT) }
            .sortedWith(
                compareByDescending<FdroidAppSearchCandidate> { candidate -> candidate.score }
                    .thenBy { candidate -> candidate.displayName.lowercase(Locale.ROOT) }
            )
            .take(limit.coerceIn(1, 50))
    }

    private fun FdroidSearchApiApp.toCandidate(
        repoUrl: String,
        query: String,
        packageName: String
    ): FdroidAppSearchCandidate {
        val score = when {
            packageName.isNotBlank() && this.packageName.equals(packageName, ignoreCase = true) -> 1000
            name.searchKey() == query.searchKey() -> 900
            name.searchKey().startsWith(query.searchKey()) -> 820
            this.packageName.searchKey().contains(query.searchKey()) -> 720
            name.searchKey().contains(query.searchKey()) -> 680
            summary.searchKey().contains(query.searchKey()) -> 420
            else -> 200
        }
        return FdroidAppSearchCandidate(
            repoUrl = repoUrl,
            repoDisplayName = FdroidRepositoryPresets.Main.displayName,
            repoPresetId = FdroidRepositoryPresets.MAIN_ID,
            packageName = this.packageName,
            appName = name,
            summary = summary,
            iconUrl = iconUrl,
            packagePageUrl = packagePageUrl,
            score = score,
            source = FdroidAppSearchSource.OfficialSearchApi
        )
    }

    private fun FdroidRepositorySnapshot.displayName(): String {
        return FdroidRepositoryPresets.presetForRepoUrl(repoUrl)?.displayName
            ?: repoName.ifBlank {
                buildFdroidRepositoryTrackIdentity(repoUrl)?.repoDisplayName
                    ?: repoUrl
            }
    }

    private fun String.isFdroidMainRepo(): Boolean {
        return normalizedFdroidRepoUrlKey() ==
            FdroidRepositoryPresets.Main.repoUrl.normalizedFdroidRepoUrlKey()
    }

    private fun String.searchKey(): String =
        trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s+"""), " ")

    private data class RepoSearchResult(
        val repoUrl: String = "",
        val candidates: List<FdroidAppSearchCandidate> = emptyList(),
        val failures: List<FdroidAppSearchFailure> = emptyList(),
        val elapsedMillis: Long = 0L
    ) {
        fun toReport(): FdroidAppSearchRepoReport {
            return FdroidAppSearchRepoReport(
                repoUrl = repoUrl,
                candidateCount = candidates.size,
                failureCount = failures.size,
                elapsedMillis = elapsedMillis
            )
        }
    }

    private inline fun <T> fdroidAppSearchResult(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

fun interface FdroidRepositorySearchProvider {
    suspend fun searchRepository(
        repoUrl: String,
        query: String,
        packageName: String,
        limit: Int,
        forceRefresh: Boolean
    ): Result<FdroidRepositorySnapshot>
}

class FdroidRepositoryIndexSearchProvider(
    private val client: FdroidRepositoryIndexClient = FdroidRepositoryIndexClient()
) : FdroidRepositorySearchProvider {
    override suspend fun searchRepository(
        repoUrl: String,
        query: String,
        packageName: String,
        limit: Int,
        forceRefresh: Boolean
    ): Result<FdroidRepositorySnapshot> {
        return client.searchIndexV2(
            repoBaseUrl = repoUrl,
            query = query,
            packageName = packageName,
            limit = limit,
            forceRefresh = forceRefresh
        )
    }
}

fun interface FdroidPackageSearchProvider {
    suspend fun fetchPackage(
        repoUrl: String,
        packageName: String
    ): Result<FdroidPackageSnapshot>
}

class FdroidPackageApiSearchProvider(
    private val client: FdroidPackageApiClient = FdroidPackageApiClient()
) : FdroidPackageSearchProvider {
    override suspend fun fetchPackage(
        repoUrl: String,
        packageName: String
    ): Result<FdroidPackageSnapshot> {
        return client.fetchPackage(
            repoBaseUrl = repoUrl,
            packageName = packageName
        )
    }
}
