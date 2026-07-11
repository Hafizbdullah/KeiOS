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
