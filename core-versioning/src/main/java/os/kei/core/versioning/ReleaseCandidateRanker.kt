package os.kei.core.versioning

object ReleaseCandidateRanker {
    fun compare(
        left: ReleaseRankingEvidence,
        right: ReleaseRankingEvidence,
    ): Int {
        val versionComparison = VersioningEngine.compareRemoteCandidateSets(
            leftCandidates = left.versionCandidates,
            rightCandidates = right.versionCandidates,
        )
        if (versionComparison != null && versionComparison.order != VersionOrder.Same) {
            return versionComparison.order.legacyValue
        }

        val publishedComparison = compareValues(
            left.publishedAtMillis ?: Long.MIN_VALUE,
            right.publishedAtMillis ?: Long.MIN_VALUE,
        )
        if (publishedComparison != 0) return publishedComparison

        return left.stableKey.compareTo(right.stableKey)
    }
}
