package os.kei.ui.page.main.github.asset

import os.kei.feature.github.data.local.fdroid.FdroidMetadataSidecar
import os.kei.feature.github.data.local.fdroid.FdroidVersionMetadataSummary
import os.kei.feature.github.data.remote.GitHubReleaseAssetBundle
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.buildFdroidRepositoryTrackIdentity
import os.kei.feature.github.model.fdroidRepositoryCheckSourceSignature
import os.kei.feature.github.model.isFdroidRepositoryTrack
import java.net.URI
import java.net.URLDecoder
import java.util.Locale

internal const val GITHUB_FDROID_ASSET_FETCH_SOURCE = "fdroid_repository"

internal data class GitHubFdroidAssetPanelData(
    val bundle: GitHubReleaseAssetBundle,
    val targetRawTag: String,
)

internal fun GitHubTrackedApp.fdroidAssetPanelData(
    sidecar: FdroidMetadataSidecar,
): GitHubFdroidAssetPanelData? {
    if (!isFdroidRepositoryTrack()) return null
    if (sidecar.sourceConfigSignature != fdroidRepositoryCheckSourceSignature()) return null
    val selectedVersion = sidecar.selectedVersion ?: return null
    val repoUrl = sidecar.repo.repoUrl.trim().ifBlank { this.repoUrl.trim() }
    val downloadUrl = resolveFdroidApkDownloadUrl(repoUrl, selectedVersion.apkPath)
        .ifBlank { resolveFdroidApkDownloadUrl(repoUrl, selectedVersion.apkName) }
    if (downloadUrl.isBlank()) return null

    val identity = buildFdroidRepositoryTrackIdentity(repoUrl, packageName)
    val assetName = selectedVersion.assetName(downloadUrl)
    val targetTag = selectedVersion.versionName.trim()
        .ifBlank { selectedVersion.versionCode.takeIf { it > 0L }?.toString().orEmpty() }
        .ifBlank { assetDisplayName(assetName) }
    val releaseName = sidecar.packageInfo.appName.trim()
        .ifBlank { appLabel.trim() }
        .ifBlank { sidecar.packageInfo.packageName.trim() }
        .ifBlank { packageName.trim() }
        .ifBlank { assetDisplayName(assetName) }
    val htmlUrl = fdroidConfig.packagePageUrl.trim()
        .ifBlank { identity?.packagePageUrl.orEmpty() }
        .ifBlank { repoUrl }
    val bundle =
        GitHubReleaseAssetBundle(
            releaseName = releaseName,
            tagName = targetTag,
            htmlUrl = htmlUrl,
            releaseUpdatedAtMillis = selectedVersion.addedAtMillis?.takeIf { it > 0L },
            releaseNotesBody = selectedVersion.whatsNew.trim(),
            assets =
                listOf(
                    GitHubReleaseAssetFile(
                        name = assetName,
                        downloadUrl = downloadUrl,
                        sizeBytes = selectedVersion.apkSizeBytes,
                        downloadCount = 0,
                        contentType = "application/vnd.android.package-archive",
                        updatedAtMillis = selectedVersion.addedAtMillis?.takeIf { it > 0L },
                        digest = selectedVersion.apkSha256.toSha256Digest(),
                        signerSha256 = selectedVersion.signerSha256
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    )
                ),
            showingAllAssets = false,
            fetchSource = GITHUB_FDROID_ASSET_FETCH_SOURCE,
            sourceConfigSignature = sidecar.sourceConfigSignature,
        )
    return GitHubFdroidAssetPanelData(
        bundle = bundle,
        targetRawTag = targetTag,
    )
}

internal fun resolveFdroidApkDownloadUrl(
    repoUrl: String,
    apkPath: String,
): String {
    val normalizedPath = apkPath.trim()
    if (normalizedPath.isBlank()) return ""
    val pathUri = runCatching { URI(normalizedPath) }.getOrNull()
    if (pathUri?.scheme?.lowercase(Locale.ROOT) in setOf("http", "https")) {
        return normalizedPath
    }
    val base = repoUrl.trim().ifBlank { return "" }
    val baseWithSlash = if (base.endsWith('/')) base else "$base/"
    return runCatching {
        URI(baseWithSlash).resolve(normalizedPath).toString()
    }.getOrDefault("")
}

private fun FdroidVersionMetadataSummary.assetName(downloadUrl: String): String {
    return apkName.trim()
        .ifBlank { apkPath.trim().substringAfterLast('/') }
        .ifBlank { fdroidAssetNameFromUrl(downloadUrl) }
        .ifBlank { "fdroid.apk" }
}

private fun String.toSha256Digest(): String {
    val hash = trim()
    if (hash.isBlank()) return ""
    return if (hash.startsWith("sha256:", ignoreCase = true)) {
        hash
    } else {
        "sha256:$hash"
    }
}

private fun fdroidAssetNameFromUrl(url: String): String {
    return runCatching {
        val rawName = URI(url).rawPath
            .orEmpty()
            .substringAfterLast('/')
        URLDecoder.decode(rawName, Charsets.UTF_8.name())
    }.getOrDefault("")
}
