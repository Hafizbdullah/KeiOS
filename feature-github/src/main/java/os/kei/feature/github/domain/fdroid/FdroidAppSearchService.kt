package os.kei.feature.github.domain.fdroid

import kotlinx.coroutines.CoroutineDispatcher
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.github.GitHubExecution
import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidRepositorySnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidSearchApiApp
import os.kei.feature.github.data.remote.fdroid.FdroidSearchApiClient
import os.kei.feature.github.model.FdroidAppSearchCandidate
import os.kei.feature.github.model.FdroidAppSearchFailure
import os.kei.feature.github.model.FdroidAppSearchRequest
import os.kei.feature.github.model.FdroidAppSearchResult
import os.kei.feature.github.model.FdroidAppSearchSource
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.buildFdroidRepositoryTrackIdentity
import os.kei.feature.github.model.normalizedFdroidRepoUrlKey
import java.util.Locale

class FdroidAppSearchService(
    private val searchApiClient: FdroidSearchApiClient = FdroidSearchApiClient(),
    private val repositoryProvider: FdroidRepositorySnapshotProvider = FdroidRepositoryIndexSnapshotProvider(),
    private val dispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork
) {
    suspend fun search(request: FdroidAppSearchRequest): Result<FdroidAppSearchResult> =
        runCatching {
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
                    searchRepo(
                        repoUrl = repoUrl,
                        query = normalizedQuery,
                        packageName = normalizedPackageName,
                        limit = request.limit,
                        includeOfficialSearchApi = request.includeOfficialSearchApi
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
                failures = repoResults.flatMap { result -> result.failures }
            )
        }

    private suspend fun searchRepo(
        repoUrl: String,
        query: String,
        packageName: String,
        limit: Int,
        includeOfficialSearchApi: Boolean
    ): RepoSearchResult {
        if (includeOfficialSearchApi && repoUrl.isFdroidMainRepo()) {
            val apiResult = searchOfficialRepo(repoUrl, query, packageName, limit)
            if (apiResult.candidates.isNotEmpty()) {
                return apiResult
            }
            val indexResult = searchRepositoryIndex(repoUrl, query, packageName, limit)
            if (indexResult.candidates.isNotEmpty() || apiResult.failures.isNotEmpty()) {
                return indexResult
            }
            return apiResult
        }
        return searchRepositoryIndex(repoUrl, query, packageName, limit)
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

    private suspend fun searchRepositoryIndex(
        repoUrl: String,
        query: String,
        packageName: String,
        limit: Int
    ): RepoSearchResult {
        val result = repositoryProvider.loadRepositorySnapshot(
            repoUrl = repoUrl,
            forceRefresh = false
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
        val candidates: List<FdroidAppSearchCandidate> = emptyList(),
        val failures: List<FdroidAppSearchFailure> = emptyList()
    )
}
