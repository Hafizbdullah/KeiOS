package os.kei.feature.github.model

enum class GitHubTrackChangeHistoryAction {
    Added,
    Updated,
    Deleted,
}

enum class GitHubTrackChangeHistorySource {
    Page,
    Import,
    StarImport,
}

enum class GitHubTrackChangeField {
    Repository,
    PackageName,
    AppLabel,
    SourceMode,
    PreferPreRelease,
    LatestReleaseDownloadButton,
    ActionsUpdates,
    UpdateInterval,
    ActionsUpdateInterval,
    PreciseApkVersion,
    IgnoreMode,
    IgnoredStableRelease,
    IgnoredPreRelease,
    FdroidConfig,
}

data class GitHubTrackChangeHistoryRecord(
    val id: String,
    val trackId: String,
    val previousTrackId: String = "",
    val action: GitHubTrackChangeHistoryAction,
    val source: GitHubTrackChangeHistorySource,
    val changedAtMillis: Long,
    val owner: String,
    val repo: String,
    val repoUrl: String,
    val packageName: String,
    val appLabel: String,
    val sourceMode: GitHubTrackedSourceMode,
    val changedFields: List<GitHubTrackChangeField> = emptyList(),
    val note: String = "",
)
