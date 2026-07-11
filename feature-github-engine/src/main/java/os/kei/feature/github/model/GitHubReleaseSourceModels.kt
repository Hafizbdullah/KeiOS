package os.kei.feature.github.model

enum class GitHubLookupStrategyOption(
    val storageId: String,
    val label: String,
) {
    AtomFeed(
        storageId = "atom_feed",
        label = "Atom Feed",
    ),
    GitHubApiToken(
        storageId = "github_api_token",
        label = "GitHub API Token",
    );

    companion object {
        fun fromStorageId(value: String): GitHubLookupStrategyOption {
            return entries.firstOrNull { it.storageId == value } ?: AtomFeed
        }
    }
}

enum class GitHubApiAuthMode(val label: String) {
    Guest("Guest"),
    Token("Token"),
}

data class GitHubStrategyLoadTrace<T>(
    val result: Result<T>,
    val fromCache: Boolean,
    val elapsedMs: Long,
    val authMode: GitHubApiAuthMode? = null,
)

data class GitHubApiCredentialStatus(
    val authMode: GitHubApiAuthMode,
    val coreLimit: Int,
    val coreRemaining: Int,
    val coreUsed: Int,
    val resetAtMillis: Long? = null,
) {
    val summaryLabel: String
        get() = when (authMode) {
            GitHubApiAuthMode.Guest -> "Guest API available"
            GitHubApiAuthMode.Token -> "Token available"
        }
}

enum class GitRepositoryPlatform(val storageId: String) {
    GitHub("github"),
    Gitee("gitee"),
    GitLab("gitlab"),
    Gitea("gitea"),
    Generic("generic"),
}

data class GitRepositoryTrackIdentity(
    val url: String,
    val host: String,
    val namespace: String,
    val repo: String,
    val owner: String,
    val displayName: String,
    val platform: GitRepositoryPlatform,
)
