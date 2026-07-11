package os.kei.feature.github.model

data class GitHubLocalVersionInfo(
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean = false,
)
