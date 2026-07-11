package os.kei.feature.github.model

import java.util.Locale

enum class GitHubReleaseIgnoreChannel {
    Stable,
    PreRelease
}

fun GitHubTrackedApp.excludesAutomaticReleaseRefresh(): Boolean {
    return ignoreMode == GitHubTrackedIgnoreMode.Temporary ||
        ignoreMode == GitHubTrackedIgnoreMode.AllVersions
}

fun GitHubTrackedApp.withReleaseIgnoreMode(
    mode: GitHubTrackedIgnoreMode,
    stableReleaseKey: String = ignoredStableReleaseKey,
    preReleaseKey: String = ignoredPreReleaseKey
): GitHubTrackedApp {
    return when (mode) {
        GitHubTrackedIgnoreMode.None,
        GitHubTrackedIgnoreMode.Temporary,
        GitHubTrackedIgnoreMode.AllVersions -> copy(
            ignoreMode = mode,
            ignoredStableReleaseKey = "",
            ignoredPreReleaseKey = ""
        )

        GitHubTrackedIgnoreMode.CurrentStable -> copy(
            ignoreMode = mode,
            ignoredStableReleaseKey = stableReleaseKey.normalizedReleaseIgnoreKey(),
            ignoredPreReleaseKey = ""
        )

        GitHubTrackedIgnoreMode.CurrentPreRelease -> copy(
            ignoreMode = mode,
            ignoredStableReleaseKey = "",
            ignoredPreReleaseKey = preReleaseKey.normalizedReleaseIgnoreKey()
        )
    }
}

fun GitHubTrackedReleaseCheck.currentIgnoredReleaseChannel(): GitHubReleaseIgnoreChannel? {
    return when {
        recommendsPreRelease &&
            releaseIgnoreKeyForChannel(GitHubReleaseIgnoreChannel.PreRelease).isNotBlank() -> {
            GitHubReleaseIgnoreChannel.PreRelease
        }

        hasUpdate == true &&
            releaseIgnoreKeyForChannel(GitHubReleaseIgnoreChannel.Stable).isNotBlank() -> {
            GitHubReleaseIgnoreChannel.Stable
        }

        hasPreReleaseUpdate &&
            releaseIgnoreKeyForChannel(GitHubReleaseIgnoreChannel.PreRelease).isNotBlank() -> {
            GitHubReleaseIgnoreChannel.PreRelease
        }

        else -> null
    }
}

fun GitHubTrackedReleaseCheck.releaseIgnoreKeyForChannel(
    channel: GitHubReleaseIgnoreChannel
): String {
    return when (channel) {
        GitHubReleaseIgnoreChannel.Stable -> buildGitHubReleaseIgnoreKey(
            release = stableRelease,
            preciseApkVersion = preciseStableApkVersion
        )

        GitHubReleaseIgnoreChannel.PreRelease -> buildGitHubReleaseIgnoreKey(
            release = preRelease,
            preciseApkVersion = precisePreApkVersion
        )
    }
}

private fun String.normalizedReleaseIgnoreKey(): String {
    return trim().lowercase(Locale.ROOT)
}
