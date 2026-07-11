package os.kei.feature.github.engine.release

import org.junit.Test
import os.kei.core.versioning.VersionCandidate
import os.kei.core.versioning.VersionOrder
import os.kei.core.versioning.VersioningEngine
import os.kei.feature.github.model.GitHubAtomReleaseEntry
import os.kei.feature.github.model.GitHubReleaseChannel
import os.kei.feature.github.model.GitHubVersionCandidate
import os.kei.feature.github.model.GitHubVersionCandidateSource
import kotlin.test.assertEquals

class GitHubReleaseCandidateRankerTest {
    @Test
    fun `semantic version outranks a newer publication timestamp`() {
        val olderVersion = entry(tag = "v1.9.0", publishedAtMillis = 200L)
        val newerVersion = entry(tag = "v2.0.0", publishedAtMillis = 100L)

        assertEquals(newerVersion, GitHubReleaseCandidateRanker.latest(listOf(olderVersion, newerVersion)))
    }

    @Test
    fun `publication timestamp breaks equal version ties`() {
        val olderPublication = entry(tag = "v1.0.0", publishedAtMillis = 100L)
        val newerPublication = entry(tag = "1.0.0", publishedAtMillis = 200L)

        assertEquals(
            newerPublication,
            GitHubReleaseCandidateRanker.latest(listOf(olderPublication, newerPublication)),
        )
    }

    @Test
    fun `content noise cannot outrank a trusted release tag`() {
        val older = entry(
            tag = "v1.0.0",
            publishedAtMillis = 200L,
            contentCandidate = "migration notes mention v99.0.0",
        )
        val newer = entry(tag = "v2.0.0", publishedAtMillis = 100L)

        assertEquals(newer, GitHubReleaseCandidateRanker.latest(listOf(older, newer)))
    }

    @Test
    fun `semantic alpha tag outranks canary alias carrying the same build revision`() {
        val alpha = entry(
            tag = "Version.26.4.Alpha2_C384",
            title = "Version.26.4.Alpha2_C384",
            publishedAtMillis = 200L,
        )
        val canaryAlias = entry(
            tag = "Canary.Version_C384",
            title = "Canary Build Version.26.4.Canary_C384",
            publishedAtMillis = 100L,
        )
        val semanticComparison = VersioningEngine.compareRemoteCandidateSets(
            leftCandidates = alpha.versionCandidates.map { candidate ->
                VersionCandidate(candidate.value, candidate.source.priority)
            },
            rightCandidates = canaryAlias.versionCandidates.map { candidate ->
                VersionCandidate(candidate.value, candidate.source.priority)
            },
        )

        assertEquals(VersionOrder.Newer, semanticComparison?.order, semanticComparison.toString())
        assertEquals(
            alpha,
            GitHubReleaseCandidateRanker.latest(listOf(alpha, canaryAlias)),
            "alpha=${alpha.versionCandidates}, canary=${canaryAlias.versionCandidates}",
        )
    }

    private fun entry(
        tag: String,
        title: String = tag,
        publishedAtMillis: Long,
        contentCandidate: String = "",
    ): GitHubAtomReleaseEntry {
        return GitHubAtomReleaseEntry(
            entryId = tag,
            tag = tag,
            title = title,
            link = "https://example.test/releases/$tag",
            updatedAtMillis = publishedAtMillis,
            versionCandidates = VersioningEngine.buildCandidates(
                inputs = buildList {
                    add(GitHubVersionCandidateSource.Tag.priority to tag)
                    add(GitHubVersionCandidateSource.Title.priority to title)
                    if (contentCandidate.isNotBlank()) {
                        add(GitHubVersionCandidateSource.Content.priority to contentCandidate)
                    }
                },
            ).map { candidate ->
                GitHubVersionCandidate(
                    value = candidate.value,
                    source = GitHubVersionCandidateSource.entries.first { source ->
                        source.priority == candidate.sourcePriority
                    },
                )
            },
            channel = GitHubReleaseChannel.STABLE,
            isLikelyPreRelease = false,
        )
    }
}
