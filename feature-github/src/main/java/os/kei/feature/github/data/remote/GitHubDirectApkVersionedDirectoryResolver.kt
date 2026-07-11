package os.kei.feature.github.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import os.kei.core.io.SharedHttpClient
import os.kei.core.io.cancellableResult
import os.kei.core.io.executeCancellable
import os.kei.core.io.stringLimitedBlocking
import os.kei.feature.github.model.GitHubReleaseChannel
import java.net.URI
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

data class GitHubDirectApkVersionedDirectoryResolution(
    val indexUrl: String,
    val version: String,
    val downloadUrl: String,
    val channel: GitHubReleaseChannel
) {
    val isPreRelease: Boolean
        get() = channel.isPreRelease

    fun toAsset(fallbackName: String): GitHubReleaseAssetFile {
        return GitHubReleaseAssetFile(
            name = directApkFileNameFromUrl(downloadUrl).ifBlank { fallbackName },
            downloadUrl = downloadUrl,
            sizeBytes = 0L,
            downloadCount = 0,
            contentType = "application/vnd.android.package-archive"
        )
    }
}

data class GitHubDirectApkVersionedDirectoryTargets(
    val stable: GitHubDirectApkVersionedDirectoryResolution?,
    val preRelease: GitHubDirectApkVersionedDirectoryResolution?
)

class GitHubDirectApkVersionedDirectoryResolver(
    private val client: OkHttpClient = defaultClient
) {
    suspend fun resolve(
        directApkUrl: String,
        preferPreRelease: Boolean = false
    ): Result<GitHubDirectApkVersionedDirectoryResolution?> {
        return resolveTargets(
            directApkUrl = directApkUrl,
            includePreRelease = preferPreRelease
        ).map { targets ->
            if (preferPreRelease) {
                targets?.preRelease ?: targets?.stable
            } else {
                targets?.stable ?: targets?.preRelease
            }
        }
    }

    suspend fun resolveTargets(
        directApkUrl: String,
        includePreRelease: Boolean = false
    ): Result<GitHubDirectApkVersionedDirectoryTargets?> =
        cancellableResult {
            val pattern = DirectApkVersionedDirectoryPattern.from(directApkUrl)
                ?: return@cancellableResult null
            val request = Request.Builder()
                .url(pattern.indexUrl)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,*/*")
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build()
            val html = client.executeCancellable(request) { response ->
                check(response.isSuccessful) {
                    "direct APK version directory failed (HTTP ${response.code})"
                }
                response.body.stringLimitedBlocking(MAX_INDEX_HTML_BYTES)
            }
            val candidates = parseVersionDirectories(
                indexUri = pattern.indexUri,
                indexPath = pattern.indexPath,
                html = html
            )
            val latestStable = candidates
                .preferredReleaseChannelCandidates(preferPreRelease = false)
                .maxWithOrNull(VersionDirectoryCandidateComparator)
            val latestPreRelease = candidates
                .preferredReleaseChannelCandidates(preferPreRelease = true)
                .maxWithOrNull(VersionDirectoryCandidateComparator)
            val fallbackLatest = candidates.maxWithOrNull(VersionDirectoryCandidateComparator)
            val stableCandidate = latestStable
            val preReleaseCandidate = when {
                includePreRelease -> latestPreRelease
                stableCandidate == null -> latestPreRelease ?: fallbackLatest
                    ?.takeIf { it.version.channel.isPreRelease }

                else -> null
            }
            val stable = stableCandidate?.toResolution(pattern)
            val preRelease = preReleaseCandidate?.toResolution(pattern)
            if (stable == null && preRelease == null) return@cancellableResult null
            GitHubDirectApkVersionedDirectoryTargets(
                stable = stable,
                preRelease = preRelease
            )
        }

    private fun VersionDirectoryCandidate.toResolution(
        pattern: DirectApkVersionedDirectoryPattern
    ): GitHubDirectApkVersionedDirectoryResolution {
        val downloadUrl = uri.ensureDirectoryUri()
            .resolve(pattern.suffixPath)
            .toString()
        return GitHubDirectApkVersionedDirectoryResolution(
            indexUrl = pattern.indexUrl,
            version = version.raw,
            downloadUrl = downloadUrl,
            channel = version.channel
        )
    }

    private fun parseVersionDirectories(
        indexUri: URI,
        indexPath: String,
        html: String
    ): List<VersionDirectoryCandidate> {
        return hrefRegex.findAll(html)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.trim() }
            .filter { href -> href.isNotBlank() && href != ".." }
            .mapNotNull { href ->
                val uri = runCatching { indexUri.resolve(href) }.getOrNull()
                    ?: return@mapNotNull null
                val normalizedPath = uri.path.orEmpty()
                if (!normalizedPath.startsWith(indexPath)) return@mapNotNull null
                val segment = normalizedPath
                    .trimEnd('/')
                    .substringAfterLast('/')
                val version = DirectApkDirectoryVersion.parse(segment) ?: return@mapNotNull null
                VersionDirectoryCandidate(uri = uri, version = version)
            }
            .distinctBy { it.uri.normalize().toString() }
            .toList()
    }

    private fun URI.ensureDirectoryUri(): URI {
        val value = toString()
        return if (value.endsWith("/")) this else URI("$value/")
    }

    private data class DirectApkVersionedDirectoryPattern(
        val indexUri: URI,
        val indexUrl: String,
        val indexPath: String,
        val suffixPath: String
    ) {
        companion object {
            fun from(rawUrl: String): DirectApkVersionedDirectoryPattern? {
                val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return null
                val scheme = uri.scheme.orEmpty().lowercase(Locale.ROOT)
                if (scheme != "http" && scheme != "https") return null
                val segments = uri.path.orEmpty()
                    .split('/')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                if (segments.size < 3) return null
                val versionIndex = segments
                    .dropLast(1)
                    .indexOfLast { DirectApkDirectoryVersion.parse(it) != null }
                if (versionIndex < 0) return null
                val prefixSegments = segments.take(versionIndex)
                val suffixSegments = segments.drop(versionIndex + 1)
                if (suffixSegments.isEmpty()) return null
                val indexPath = "/" + prefixSegments.joinToString("/").trim('/') + "/"
                val normalizedIndexPath = indexPath.replace("//", "/")
                val authority = uri.rawAuthority ?: return null
                val indexUri = URI("${uri.scheme}://$authority$normalizedIndexPath")
                return DirectApkVersionedDirectoryPattern(
                    indexUri = indexUri,
                    indexUrl = indexUri.toString(),
                    indexPath = normalizedIndexPath,
                    suffixPath = suffixSegments.joinToString("/")
                )
            }
        }
    }

    private data class VersionDirectoryCandidate(
        val uri: URI,
        val version: DirectApkDirectoryVersion
    )

    private data class DirectApkDirectoryVersion(
        val raw: String,
        val channel: GitHubReleaseChannel
    ) {
        companion object {
            fun parse(segment: String): DirectApkDirectoryVersion? {
                val match = versionSegmentRegex.matchEntire(segment.trim()) ?: return null
                val suffix = match.groupValues.getOrNull(2).orEmpty()
                val classifiedChannel = GitHubVersionUtils.classifyVersionChannel(match.value)
                return DirectApkDirectoryVersion(
                    raw = segment,
                    channel = when {
                        suffix.isBlank() -> GitHubReleaseChannel.STABLE
                        classifiedChannel == GitHubReleaseChannel.STABLE -> GitHubReleaseChannel.UNKNOWN
                        else -> classifiedChannel ?: GitHubReleaseChannel.UNKNOWN
                    },
                )
            }
        }
    }

    private object VersionDirectoryCandidateComparator : Comparator<VersionDirectoryCandidate> {
        override fun compare(
            left: VersionDirectoryCandidate,
            right: VersionDirectoryCandidate
        ): Int {
            return GitHubVersionUtils.compareReleaseCandidateValues(
                left = left.version.raw,
                right = right.version.raw,
            ) ?: left.version.raw.compareTo(right.version.raw, ignoreCase = true)
        }
    }

    private fun List<VersionDirectoryCandidate>.preferredReleaseChannelCandidates(
        preferPreRelease: Boolean
    ): List<VersionDirectoryCandidate> {
        return filter { candidate ->
            candidate.version.channel.isPreRelease == preferPreRelease
        }
    }

    private companion object {
        const val USER_AGENT = "KeiOS-App/1.0 (Android)"
        const val MAX_INDEX_HTML_BYTES = 512L * 1024L
        val hrefRegex = Regex("""href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val versionSegmentRegex =
            Regex("""^[vV]?(\d+(?:\.\d+){1,4})(?:[-_]?([A-Za-z][A-Za-z0-9._-]*))?/?$""")
        val defaultClient: OkHttpClient = SharedHttpClient.base.newBuilder()
            .connectTimeout(12.seconds)
            .readTimeout(20.seconds)
            .callTimeout(28.seconds)
            .build()
    }
}
