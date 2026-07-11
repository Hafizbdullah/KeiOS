package os.kei.feature.github.domain

import java.util.Locale
import os.kei.feature.github.model.GitHubRepositoryCandidate
import os.kei.feature.github.model.GitHubRepositoryCandidateMatchReason
import os.kei.feature.github.model.GitHubRepositoryDiscoverySourceType

internal object GitHubRepositoryCandidateMerger {
    fun dedupe(candidates: List<GitHubRepositoryCandidate>): List<GitHubRepositoryCandidate> {
        val merged = linkedMapOf<String, GitHubRepositoryCandidate>()
        candidates.forEach { candidate ->
            val key = candidate.repoKey()
            merged[key] = merged[key]?.merge(candidate) ?: candidate
        }
        return merged.values.toList()
    }

    private fun GitHubRepositoryCandidate.merge(
        other: GitHubRepositoryCandidate,
    ): GitHubRepositoryCandidate {
        return copy(
            repoUrl = richerText(repoUrl, other.repoUrl),
            description = richerText(description, other.description),
            language = richerText(language, other.language),
            starCount = maxOf(starCount, other.starCount),
            forkCount = maxOf(forkCount, other.forkCount),
            archived = archived || other.archived,
            fork = fork || other.fork,
            updatedAtMillis = maxOf(updatedAtMillis, other.updatedAtMillis),
            sourceType = maxOf(sourceType, other.sourceType, compareBy(::sourcePriority)),
            matchReason = maxOf(matchReason, other.matchReason, compareBy(::matchPriority)),
        )
    }

    private fun GitHubRepositoryCandidate.repoKey(): String {
        return "${owner.lowercase(Locale.ROOT)}/${repo.lowercase(Locale.ROOT)}"
    }

    private fun richerText(left: String, right: String): String {
        return when {
            left.isBlank() -> right
            right.isBlank() -> left
            right.length > left.length -> right
            else -> left
        }
    }

    private fun sourcePriority(source: GitHubRepositoryDiscoverySourceType): Int {
        return when (source) {
            GitHubRepositoryDiscoverySourceType.PreferredRepository -> 5
            GitHubRepositoryDiscoverySourceType.AuthenticatedStars -> 4
            GitHubRepositoryDiscoverySourceType.PublicUserStars -> 3
            GitHubRepositoryDiscoverySourceType.StarList -> 2
            GitHubRepositoryDiscoverySourceType.RepositorySearch -> 1
        }
    }

    private fun matchPriority(reason: GitHubRepositoryCandidateMatchReason): Int {
        return when (reason) {
            GitHubRepositoryCandidateMatchReason.PackageName -> 4
            GitHubRepositoryCandidateMatchReason.AppLabel -> 3
            GitHubRepositoryCandidateMatchReason.RepositoryName -> 2
            GitHubRepositoryCandidateMatchReason.Starred -> 1
        }
    }
}
