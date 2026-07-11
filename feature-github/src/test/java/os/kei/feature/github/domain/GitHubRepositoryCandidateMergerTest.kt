package os.kei.feature.github.domain

import org.junit.Test
import os.kei.feature.github.model.GitHubRepositoryCandidate
import os.kei.feature.github.model.GitHubRepositoryCandidateMatchReason
import os.kei.feature.github.model.GitHubRepositoryDiscoverySourceType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubRepositoryCandidateMergerTest {
    @Test
    fun `duplicate repository keeps richer metadata and strongest evidence`() {
        val merged = GitHubRepositoryCandidateMerger.dedupe(
            listOf(
                candidate(
                    description = "Android app",
                    stars = 4,
                    source = GitHubRepositoryDiscoverySourceType.RepositorySearch,
                    reason = GitHubRepositoryCandidateMatchReason.RepositoryName,
                ),
                candidate(
                    description = "Android app for os.kei with package metadata",
                    stars = 128,
                    source = GitHubRepositoryDiscoverySourceType.AuthenticatedStars,
                    reason = GitHubRepositoryCandidateMatchReason.PackageName,
                    archived = true,
                ),
            ),
        ).single()

        assertEquals(128, merged.starCount)
        assertEquals("Android app for os.kei with package metadata", merged.description)
        assertEquals(GitHubRepositoryDiscoverySourceType.AuthenticatedStars, merged.sourceType)
        assertEquals(GitHubRepositoryCandidateMatchReason.PackageName, merged.matchReason)
        assertTrue(merged.archived)
    }

    private fun candidate(
        description: String,
        stars: Int,
        source: GitHubRepositoryDiscoverySourceType,
        reason: GitHubRepositoryCandidateMatchReason,
        archived: Boolean = false,
    ): GitHubRepositoryCandidate {
        return GitHubRepositoryCandidate(
            owner = "hosizoraru",
            repo = "KeiOS",
            repoUrl = "https://github.com/hosizoraru/KeiOS",
            description = description,
            starCount = stars,
            archived = archived,
            sourceType = source,
            matchReason = reason,
        )
    }
}
