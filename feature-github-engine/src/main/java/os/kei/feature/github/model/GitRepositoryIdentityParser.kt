package os.kei.feature.github.model

import java.net.URI
import java.util.Locale

fun buildGitRepositoryTrackIdentity(rawUrl: String): GitRepositoryTrackIdentity? {
    val normalizedUrl = rawUrl.trim()
        .removePrefix("git+")
        .trimEnd('/')
    if (normalizedUrl.isBlank()) return null

    val (rawHost, rawPath) = parseGitRepositoryHostAndPath(normalizedUrl) ?: return null
    val host = rawHost
        .lowercase(Locale.ROOT)
        .removePrefix("www.")
        .ifBlank { return null }
    val pathSegments = normalizeGitRepositoryPathSegments(rawPath)
    if (pathSegments.size < 2) return null

    val namespace = pathSegments
        .dropLast(1)
        .joinToString("/")
        .ifBlank { return null }
    val repo = pathSegments.last().removeSuffix(".git").ifBlank { return null }
    val owner = "$host/$namespace"
    return GitRepositoryTrackIdentity(
        url = normalizedUrl,
        host = host,
        namespace = namespace,
        repo = repo,
        owner = owner,
        displayName = "$owner/$repo",
        platform = gitRepositoryPlatform(host),
    )
}

private fun parseGitRepositoryHostAndPath(rawUrl: String): Pair<String, String>? {
    parseScpLikeGitRepositoryUrl(rawUrl)?.let { return it }

    val uri = runCatching { URI(rawUrl) }.getOrNull()
    if (uri?.scheme != null) {
        val scheme = uri.scheme.orEmpty().lowercase(Locale.ROOT)
        if (scheme !in supportedGitRepositorySchemes) return null
        val host = uri.host.orEmpty().ifBlank { return null }
        return host to uri.path.orEmpty()
    }

    val parts = rawUrl
        .split('/')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return if (parts.size >= 3 && looksLikeGitHost(parts.first())) {
        parts.first().substringAfter('@') to parts.drop(1).joinToString("/")
    } else {
        null
    }
}

private fun parseScpLikeGitRepositoryUrl(rawUrl: String): Pair<String, String>? {
    if ("://" in rawUrl) return null
    val colonIndex = rawUrl.indexOf(':')
    if (colonIndex <= 0) return null

    val beforeColon = rawUrl.substring(0, colonIndex)
    if ('/' in beforeColon) return null
    val host = beforeColon.substringAfter('@').trim()
    val path = rawUrl.substring(colonIndex + 1).trim()
    if (host.isBlank() || path.isBlank() || '/' !in path) return null
    return host to path
}

private fun normalizeGitRepositoryPathSegments(path: String): List<String> {
    val rawSegments = path
        .substringBefore('?')
        .substringBefore('#')
        .trim('/')
        .split('/')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val markerIndex = rawSegments.indexOfFirst { segment ->
        segment.lowercase(Locale.ROOT) in repositoryPagePathMarkers
    }
    val repositorySegments = if (markerIndex >= 2) {
        rawSegments.take(markerIndex)
    } else {
        rawSegments
    }
    if (repositorySegments.isEmpty()) return emptyList()
    return repositorySegments.dropLast(1) + repositorySegments.last().removeSuffix(".git")
}

private fun gitRepositoryPlatform(host: String): GitRepositoryPlatform {
    val normalized = host.lowercase(Locale.ROOT).removePrefix("www.")
    return when {
        normalized == "github.com" -> GitRepositoryPlatform.GitHub
        normalized == "gitee.com" -> GitRepositoryPlatform.Gitee
        normalized == "gitlab.com" || normalized.endsWith(".gitlab.com") ->
            GitRepositoryPlatform.GitLab
        normalized == "gitea.com" || normalized.endsWith(".gitea.com") ->
            GitRepositoryPlatform.Gitea
        else -> GitRepositoryPlatform.Generic
    }
}

private fun looksLikeGitHost(value: String): Boolean {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return '.' in normalized ||
        normalized == "localhost" ||
        normalized.startsWith("git@")
}

private val supportedGitRepositorySchemes = setOf("http", "https", "ssh", "git")

private val repositoryPagePathMarkers = setOf(
    "-",
    "tree",
    "blob",
    "src",
    "commits",
    "commit",
    "releases",
    "tags",
    "issues",
    "pulls",
    "merge_requests",
)
