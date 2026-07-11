package os.kei.core.versioning

enum class VersionChannel(val isPreRelease: Boolean) {
    DEV(true),
    ALPHA(true),
    BETA(true),
    RC(true),
    PREVIEW(true),
    STABLE(false),
    UNKNOWN(false),
}

data class VersionCandidate(
    val value: String,
    val sourcePriority: Int,
)

enum class VersionOrder(val legacyValue: Int) {
    Older(-1),
    Same(0),
    Newer(1),
}

enum class VersionConfidence {
    Exact,
    High,
    Medium,
    Low,
}

enum class VersionComparisonReason {
    ExactCandidate,
    SemanticVersion,
    ReleaseRanking,
}

data class VersionComparison(
    val order: VersionOrder,
    val confidence: VersionConfidence,
    val reason: VersionComparisonReason,
    val leftEvidence: String,
    val rightEvidence: String,
)

data class ReleaseRankingEvidence(
    val versionCandidates: List<VersionCandidate>,
    val publishedAtMillis: Long? = null,
    val stableKey: String = "",
)
