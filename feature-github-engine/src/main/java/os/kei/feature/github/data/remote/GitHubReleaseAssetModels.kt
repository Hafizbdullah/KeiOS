package os.kei.feature.github.data.remote

data class GitHubReleaseAssetFile(
    val name: String,
    val downloadUrl: String,
    val apiAssetUrl: String = "",
    val sizeBytes: Long,
    val downloadCount: Int,
    val contentType: String = "",
    val updatedAtMillis: Long? = null,
    val digest: String = "",
    val signerSha256: List<String> = emptyList(),
)

const val GITHUB_ACTIONS_APK_ARTIFACT_CONTENT_TYPE =
    "application/vnd.keios.github-actions.apk-artifact+zip"

fun GitHubReleaseAssetFile.isGitHubActionsApkArtifactArchive(): Boolean =
    contentType.equals(GITHUB_ACTIONS_APK_ARTIFACT_CONTENT_TYPE, ignoreCase = true)

fun GitHubReleaseAssetFile.isPotentialNestedApkArchive(): Boolean =
    isGitHubActionsApkArtifactArchive() || name.endsWith(".zip", ignoreCase = true)

fun GitHubReleaseAssetFile.isVerifiedManagedInstallAsset(
    expectedPackageName: String,
    inspectedPackageName: String,
): Boolean {
    if (name.endsWith(".apk", ignoreCase = true)) return true
    if (!isPotentialNestedApkArchive()) return false
    val expected = expectedPackageName.trim()
    val inspected = inspectedPackageName.trim()
    return expected.isNotBlank() && inspected.equals(expected, ignoreCase = true)
}

data class GitHubReleaseAssetBundle(
    val releaseName: String,
    val tagName: String,
    val htmlUrl: String,
    val releaseUpdatedAtMillis: Long? = null,
    val releaseNotesBody: String = "",
    val assets: List<GitHubReleaseAssetFile>,
    val showingAllAssets: Boolean = false,
    val shortCommitSha: String = "",
    val fetchSource: String = "",
    val sourceConfigSignature: String = "",
)

data class GitHubReleaseNotesTarget(
    val releaseName: String,
    val tagName: String,
    val htmlUrl: String,
    val prerelease: Boolean,
    val latestInChannel: Boolean,
    val updatedAtMillis: Long? = null,
) {
    val id: String
        get() = "${tagName.trim()}|${htmlUrl.trim()}"
}

object GitHubReleaseAssetFetchSources {
    const val HTML = "html"
    const val API = "api"
}
