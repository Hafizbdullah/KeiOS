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

data class GitHubAppInstallSourceInfo(
    val installingPackageName: String = "",
    val installingPackageLabel: String = "",
    val initiatingPackageName: String = "",
    val initiatingPackageLabel: String = "",
    val originatingPackageName: String = "",
    val originatingPackageLabel: String = "",
    val updateOwnerPackageName: String = "",
    val updateOwnerPackageLabel: String = "",
    val packageSource: Int = -1,
)

data class GitHubTrackedAppInstallSnapshot(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean = false,
    val appLabel: String = "",
    val observedAtMillis: Long,
    val installSourceInfo: GitHubAppInstallSourceInfo = GitHubAppInstallSourceInfo(),
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
    val broadcastUid: Int = -1,
    val broadcastDataRemoved: Boolean = false,
    val broadcastUserInitiated: Boolean = false,
    val broadcastArchival: Boolean = false,
    val replacing: Boolean = false,
    val previousInstallSourceInfo: GitHubAppInstallSourceInfo = GitHubAppInstallSourceInfo(),
    val currentInstallSourceInfo: GitHubAppInstallSourceInfo = GitHubAppInstallSourceInfo(),
    val note: String = "",
)
