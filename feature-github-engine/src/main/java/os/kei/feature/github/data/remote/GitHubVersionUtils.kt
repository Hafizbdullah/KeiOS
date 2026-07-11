package os.kei.feature.github.data.remote

import java.net.URI
import os.kei.core.versioning.VersionCandidate
import os.kei.core.versioning.VersionChannel
import os.kei.core.versioning.VersioningEngine
import os.kei.feature.github.model.GitHubReleaseChannel
import os.kei.feature.github.model.GitHubVersionCandidate
import os.kei.feature.github.model.GitHubVersionCandidateSource

object GitHubVersionUtils {
    fun buildRepositoryUrl(owner: String, repo: String): String {
        return "https://github.com/$owner/$repo"
    }

    fun buildReleaseUrl(owner: String, repo: String): String {
        return "${buildRepositoryUrl(owner, repo)}/releases"
    }

    fun buildReleaseTagUrl(owner: String, repo: String, tag: String): String {
        val normalized = tag.trim()
        if (normalized.isBlank()) return buildReleaseUrl(owner, repo)
        val encodedTag = java.net.URLEncoder.encode(normalized, Charsets.UTF_8.name())
            .replace("+", "%20")
        return "https://github.com/$owner/$repo/releases/tag/$encodedTag"
    }

    fun parseOwnerRepo(urlOrPath: String): Pair<String, String>? {
        val raw = urlOrPath.trim()
            .removePrefix("git+")
            .removeSuffix(".git")
            .trimEnd('/')
        if (raw.isBlank()) return null

        if (raw.contains(":") && raw.contains("@") && raw.contains("github.com")) {
            val afterColon = raw.substringAfter(':', "")
            val ownerRepo = afterColon.removePrefix("/").split("/")
            if (ownerRepo.size >= 2) return ownerRepo[0] to ownerRepo[1]
        }

        val asUri = runCatching { URI(raw) }.getOrNull()
        if (asUri != null && asUri.host?.contains("github.com", ignoreCase = true) == true) {
            val segments = asUri.path.trim('/').split('/').filter { it.isNotBlank() }
            if (segments.size >= 2) return segments[0] to segments[1].removeSuffix(".git")
        }

        val normalized = raw
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("github.com/")
            .trim('/')
        val parts = normalized.split('/').filter { it.isNotBlank() }
        if (parts.size >= 2) return parts[0] to parts[1].removeSuffix(".git")
        return null
    }

    fun buildVersionCandidates(
        vararg inputs: Pair<GitHubVersionCandidateSource, String>,
    ): List<GitHubVersionCandidate> {
        return VersioningEngine.buildCandidates(
            inputs = inputs.map { (source, value) -> source.priority to value },
        ).map { candidate -> candidate.toGitHubCandidate() }
    }

    fun normalizeVersionCandidates(text: String): List<String> {
        return VersioningEngine.normalizeCandidates(text)
    }

    fun compareVersionToCandidates(
        localVersion: String,
        candidates: List<String>,
    ): Int? {
        return VersioningEngine.compareLocalVersionToRemote(
            localVersion = localVersion,
            remoteCandidates = candidates.map { value ->
                VersionCandidate(
                    value = value,
                    sourcePriority = GitHubVersionCandidateSource.Content.priority,
                )
            },
        )?.order?.legacyValue
    }

    fun compareVersionToStructuredCandidates(
        localVersion: String,
        candidates: List<GitHubVersionCandidate>,
    ): Int? {
        return VersioningEngine.compareLocalVersionToRemote(
            localVersion = localVersion,
            remoteCandidates = candidates.toCoreCandidates(),
        )?.order?.legacyValue
    }

    fun remoteCandidateMatchesLocalVersionNameAndCode(
        localVersion: String,
        localVersionCode: Long,
        remoteCandidates: List<GitHubVersionCandidate>,
    ): Boolean {
        return VersioningEngine.remoteCandidateMatchesLocalVersionNameAndCode(
            localVersion = localVersion,
            localVersionCode = localVersionCode,
            remoteCandidates = remoteCandidates.toCoreCandidates(),
        )
    }

    fun compareStructuredCandidateSets(
        leftCandidates: List<GitHubVersionCandidate>,
        rightCandidates: List<GitHubVersionCandidate>,
    ): Int? {
        return VersioningEngine.compareRemoteCandidateSets(
            leftCandidates = leftCandidates.toCoreCandidates(),
            rightCandidates = rightCandidates.toCoreCandidates(),
        )?.order?.legacyValue
    }

    fun referToSameReleaseVersion(
        leftCandidates: List<GitHubVersionCandidate>,
        rightCandidates: List<GitHubVersionCandidate>,
        maxSourcePriority: Int = GitHubVersionCandidateSource.Link.priority,
    ): Boolean {
        return VersioningEngine.referToSameReleaseVersion(
            leftCandidates = leftCandidates.toCoreCandidates(),
            rightCandidates = rightCandidates.toCoreCandidates(),
            maxSourcePriority = maxSourcePriority,
        )
    }

    fun hasComparableVersionCandidates(
        candidates: List<GitHubVersionCandidate>,
        maxSourcePriority: Int = GitHubVersionCandidateSource.Link.priority,
    ): Boolean {
        return VersioningEngine.hasComparableCandidates(
            candidates = candidates.toCoreCandidates(),
            maxSourcePriority = maxSourcePriority,
        )
    }

    fun hasMeaningfulPreReleaseVersionCandidates(
        candidates: List<GitHubVersionCandidate>,
        maxSourcePriority: Int = GitHubVersionCandidateSource.Link.priority,
    ): Boolean {
        return VersioningEngine.hasMeaningfulPreReleaseCandidates(
            candidates = candidates.toCoreCandidates(),
            maxSourcePriority = maxSourcePriority,
        )
    }

    fun isRelevantPreRelease(
        preReleaseCandidates: List<GitHubVersionCandidate>,
        stableCandidates: List<GitHubVersionCandidate>,
        preReleaseUpdatedAtMillis: Long? = null,
        stableUpdatedAtMillis: Long? = null,
    ): Boolean {
        return VersioningEngine.isRelevantPreRelease(
            preReleaseCandidates = preReleaseCandidates.toCoreCandidates(),
            stableCandidates = stableCandidates.toCoreCandidates(),
            preReleaseUpdatedAtMillis = preReleaseUpdatedAtMillis,
            stableUpdatedAtMillis = stableUpdatedAtMillis,
        )
    }

    fun classifyVersionChannel(text: String): GitHubReleaseChannel? {
        return VersioningEngine.classifyChannel(text)?.toGitHubChannel()
    }

    fun compareCandidateSets(
        leftCandidates: List<String>,
        rightCandidates: List<String>,
    ): Int? {
        return compareCandidateSetsWithSources(
            leftCandidates = leftCandidates,
            rightCandidates = rightCandidates.map { value ->
                GitHubVersionCandidate(value, GitHubVersionCandidateSource.Content)
            },
        )
    }

    fun compareCandidateSetsWithSources(
        leftCandidates: List<String>,
        rightCandidates: List<GitHubVersionCandidate>,
    ): Int? {
        return VersioningEngine.compareLocalCandidateSets(
            leftCandidates = leftCandidates,
            rightCandidates = rightCandidates.toCoreCandidates(),
        )?.order?.legacyValue
    }
}

private fun List<GitHubVersionCandidate>.toCoreCandidates(): List<VersionCandidate> {
    return map { candidate ->
        VersionCandidate(
            value = candidate.value,
            sourcePriority = candidate.source.priority,
        )
    }
}

private fun VersionCandidate.toGitHubCandidate(): GitHubVersionCandidate {
    val source = GitHubVersionCandidateSource.entries.firstOrNull { entry ->
        entry.priority == sourcePriority
    } ?: GitHubVersionCandidateSource.Content
    return GitHubVersionCandidate(value = value, source = source)
}

private fun VersionChannel.toGitHubChannel(): GitHubReleaseChannel {
    return when (this) {
        VersionChannel.DEV -> GitHubReleaseChannel.DEV
        VersionChannel.ALPHA -> GitHubReleaseChannel.ALPHA
        VersionChannel.BETA -> GitHubReleaseChannel.BETA
        VersionChannel.RC -> GitHubReleaseChannel.RC
        VersionChannel.PREVIEW -> GitHubReleaseChannel.PREVIEW
        VersionChannel.STABLE -> GitHubReleaseChannel.STABLE
        VersionChannel.UNKNOWN -> GitHubReleaseChannel.UNKNOWN
    }
}
