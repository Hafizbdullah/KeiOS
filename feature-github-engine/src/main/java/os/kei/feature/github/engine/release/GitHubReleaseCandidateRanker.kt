package os.kei.feature.github.engine.release

import os.kei.core.versioning.ReleaseCandidateRanker
import os.kei.core.versioning.ReleaseRankingEvidence
import os.kei.core.versioning.VersionCandidate
import os.kei.feature.github.data.remote.toCoreVersionChannelHint
import os.kei.feature.github.model.GitHubAtomReleaseEntry

object GitHubReleaseCandidateRanker {
    fun compare(
        left: GitHubAtomReleaseEntry,
        right: GitHubAtomReleaseEntry,
    ): Int {
        return ReleaseCandidateRanker.compare(
            left = left.toRankingEvidence(),
            right = right.toRankingEvidence(),
        )
    }

    fun latest(entries: List<GitHubAtomReleaseEntry>): GitHubAtomReleaseEntry? {
        var latest: RankedReleaseEntry? = null
        entries.forEach { entry ->
            val candidate = RankedReleaseEntry(entry, entry.toRankingEvidence())
            val currentLatest = latest
            if (
                currentLatest == null ||
                ReleaseCandidateRanker.compare(currentLatest.evidence, candidate.evidence) < 0
            ) {
                latest = candidate
            }
        }
        return latest?.entry
    }

    fun newestFirst(entries: List<GitHubAtomReleaseEntry>): List<GitHubAtomReleaseEntry> {
        return entries
            .map { entry -> RankedReleaseEntry(entry, entry.toRankingEvidence()) }
            .sortedWith { left, right ->
                ReleaseCandidateRanker.compare(right.evidence, left.evidence)
            }
            .map { ranked -> ranked.entry }
    }

    private data class RankedReleaseEntry(
        val entry: GitHubAtomReleaseEntry,
        val evidence: ReleaseRankingEvidence,
    )
}

private fun GitHubAtomReleaseEntry.toRankingEvidence(): ReleaseRankingEvidence {
    val channelHint = channel.toCoreVersionChannelHint()
    return ReleaseRankingEvidence(
        versionCandidates = versionCandidates.map { candidate ->
            VersionCandidate(
                value = candidate.value,
                sourcePriority = candidate.source.priority,
                channelHint = channelHint,
            )
        },
        publishedAtMillis = updatedAtMillis,
        stableKey = link.trim()
            .ifBlank { entryId.trim() }
            .ifBlank { tag.trim() },
    )
}
