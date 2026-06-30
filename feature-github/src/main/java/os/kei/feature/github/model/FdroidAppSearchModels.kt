package os.kei.feature.github.model

enum class FdroidAppSearchSource {
    OfficialSearchApi,
    RepositoryIndex
}

data class FdroidAppSearchRequest(
    val query: String = "",
    val packageName: String = "",
    val repoUrls: List<String>,
    val limit: Int = 12,
    val includeOfficialSearchApi: Boolean = true,
    val forceRefresh: Boolean = false
)

data class FdroidAppSearchCandidate(
    val repoUrl: String,
    val repoDisplayName: String,
    val repoPresetId: String = "",
    val packageName: String,
    val appName: String,
    val summary: String,
    val iconUrl: String = "",
    val packagePageUrl: String = "",
    val latestVersionName: String = "",
    val latestVersionCode: Long = -1L,
    val versionCount: Int = 0,
    val categories: List<String> = emptyList(),
    val antiFeatures: List<String> = emptyList(),
    val score: Int = 0,
    val source: FdroidAppSearchSource
) {
    val displayName: String
        get() = appName.ifBlank { packageName }
}

data class FdroidAppSearchFailure(
    val repoUrl: String,
    val message: String
)

data class FdroidAppSearchRepoReport(
    val repoUrl: String,
    val candidateCount: Int,
    val failureCount: Int,
    val elapsedMillis: Long
)

data class FdroidAppSearchResult(
    val query: String,
    val packageName: String,
    val searchedRepoUrls: List<String>,
    val candidates: List<FdroidAppSearchCandidate>,
    val failures: List<FdroidAppSearchFailure> = emptyList(),
    val repoReports: List<FdroidAppSearchRepoReport> = emptyList()
)
