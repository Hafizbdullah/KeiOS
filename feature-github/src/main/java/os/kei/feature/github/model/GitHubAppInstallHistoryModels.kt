package os.kei.feature.github.model

enum class GitHubAppInstallHistoryAction {
    Installed,
    Updated,
    Downgraded,
    Uninstalled,
}

enum class GitHubAppInstallHistorySource {
    PackageBroadcast,
}

data class GitHubTrackedAppInstallSnapshot(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean = false,
    val appLabel: String = "",
    val observedAtMillis: Long,
)

data class GitHubAppInstallHistoryRecord(
    val id: String,
    val trackId: String,
    val action: GitHubAppInstallHistoryAction,
    val source: GitHubAppInstallHistorySource,
    val changedAtMillis: Long,
    val owner: String,
    val repo: String,
    val repoUrl: String,
    val packageName: String,
    val appLabel: String,
    val sourceMode: GitHubTrackedSourceMode,
    val previousVersionName: String = "",
    val previousVersionCode: Long = -1L,
    val currentVersionName: String = "",
    val currentVersionCode: Long = -1L,
    val broadcastAction: String = "",
    val replacing: Boolean = false,
    val note: String = "",
)
