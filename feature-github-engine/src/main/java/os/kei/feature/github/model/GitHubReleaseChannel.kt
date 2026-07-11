package os.kei.feature.github.model

enum class GitHubReleaseChannel(val isPreRelease: Boolean) {
    DEV(true),
    ALPHA(true),
    BETA(true),
    RC(true),
    PREVIEW(true),
    STABLE(false),
    UNKNOWN(false),
}
