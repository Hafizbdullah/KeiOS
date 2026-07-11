package os.kei.feature.github.engine.apk

import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.model.GitHubApkManifestInfo
import java.util.Locale

data class GitHubInspectedApkCandidate(
    val asset: GitHubReleaseAssetFile,
    val manifest: GitHubApkManifestInfo,
)

data class GitHubApkInspectionSelection(
    val candidate: GitHubInspectedApkCandidate,
    val packageMatched: Boolean,
)

object GitHubApkCandidateSelectionEngine {
    const val DEFAULT_MAX_INSPECTION_CANDIDATES = 12

    fun planInspection(
        assets: List<GitHubReleaseAssetFile>,
        expectedPackageName: String,
        maxCandidates: Int = DEFAULT_MAX_INSPECTION_CANDIDATES,
    ): List<GitHubReleaseAssetFile> {
        if (maxCandidates <= 0) return emptyList()
        val packageEvidence = PackageNameEvidence.from(expectedPackageName)
        return assets
            .mapIndexedNotNull { index, asset ->
                asset.takeIf { it.isInspectableApk() }?.let {
                    RankedApkAsset(
                        asset = it,
                        originalIndex = index,
                        score = inspectionPriority(it.name, packageEvidence),
                    )
                }
            }
            .sortedWith(
                compareByDescending<RankedApkAsset> { it.score }
                    .thenBy { it.originalIndex },
            )
            .take(maxCandidates)
            .map { it.asset }
    }

    fun selectInspected(
        inspected: List<GitHubInspectedApkCandidate>,
        expectedPackageName: String,
    ): GitHubApkInspectionSelection? {
        val valid = inspected.filter { candidate -> candidate.manifest.hasRemoteVersion() }
        if (valid.isEmpty()) return null
        val expected = expectedPackageName.trim()
        if (expected.isNotBlank()) {
            valid.firstOrNull { candidate ->
                candidate.manifest.packageName.equals(expected, ignoreCase = true)
            }?.let { matched ->
                return GitHubApkInspectionSelection(
                    candidate = matched,
                    packageMatched = true,
                )
            }
        }
        return GitHubApkInspectionSelection(
            candidate = valid.first(),
            packageMatched = false,
        )
    }

    private fun GitHubReleaseAssetFile.isInspectableApk(): Boolean {
        val lowerName = name.lowercase(Locale.ROOT)
        return lowerName.endsWith(".apk") &&
            "metadata" !in lowerName
    }

    private fun GitHubApkManifestInfo.hasRemoteVersion(): Boolean {
        return versionName.isNotBlank() || versionCode.isNotBlank()
    }

    private fun inspectionPriority(
        fileName: String,
        packageEvidence: PackageNameEvidence,
    ): Int {
        val lowerName = fileName.lowercase(Locale.ROOT)
        var score = packageNameAffinity(lowerName, packageEvidence)
        score += when {
            arm64Regex.containsMatchIn(lowerName) -> 220
            universalRegex.containsMatchIn(lowerName) -> 170
            armeabiRegex.containsMatchIn(lowerName) -> 60
            x86Regex.containsMatchIn(lowerName) -> 20
            else -> 100
        }
        if (releaseRegex.containsMatchIn(lowerName)) score += 90
        if (signedRegex.containsMatchIn(lowerName)) score += 25
        if (debugRegex.containsMatchIn(lowerName)) score -= 620
        if (testRegex.containsMatchIn(lowerName)) score -= 520
        if (benchmarkRegex.containsMatchIn(lowerName)) score -= 480
        if (unsignedRegex.containsMatchIn(lowerName)) score -= 180
        if (splitRegex.containsMatchIn(lowerName)) score -= 360
        return score
    }

    private fun packageNameAffinity(
        lowerFileName: String,
        evidence: PackageNameEvidence,
    ): Int {
        if (evidence.isEmpty) return 0
        val compactFileName = lowerFileName.filter(Char::isLetterOrDigit)
        return when {
            evidence.fullPackage.length >= 6 &&
                evidence.fullPackage in compactFileName -> 700
            evidence.trailingPair.length >= 6 &&
                evidence.trailingPair in compactFileName -> 420
            evidence.finalSegment.length >= 5 &&
                evidence.finalSegment !in genericPackageSegments &&
                evidence.finalSegment in compactFileName -> 220
            else -> 0
        }
    }

    private data class RankedApkAsset(
        val asset: GitHubReleaseAssetFile,
        val originalIndex: Int,
        val score: Int,
    )

    private data class PackageNameEvidence(
        val fullPackage: String,
        val trailingPair: String,
        val finalSegment: String,
    ) {
        val isEmpty: Boolean
            get() = fullPackage.isEmpty()

        companion object {
            fun from(packageName: String): PackageNameEvidence {
                val expected = packageName.trim().lowercase(Locale.ROOT)
                val segments = expected.split('.').filter { it.isNotBlank() }
                return PackageNameEvidence(
                    fullPackage = expected.filter(Char::isLetterOrDigit),
                    trailingPair = segments.takeLast(2)
                        .joinToString("")
                        .filter(Char::isLetterOrDigit),
                    finalSegment = segments.lastOrNull()
                        .orEmpty()
                        .filter(Char::isLetterOrDigit),
                )
            }
        }
    }

    private val genericPackageSegments = setOf(
        "android",
        "mobile",
        "client",
        "release",
        "debug",
    )
    private val arm64Regex = Regex("""(?:arm64(?:-v8a)?|aarch64)""")
    private val universalRegex = Regex("""(?:^|[^a-z0-9])(?:universal|fat)(?:[^a-z0-9]|$)""")
    private val armeabiRegex = Regex("""(?:armeabi(?:-v7a)?|armv7)""")
    private val x86Regex = Regex("""(?:^|[^a-z0-9])x86(?:_64)?(?:[^a-z0-9]|$)""")
    private val releaseRegex = Regex("""(?:^|[^a-z0-9])(?:release|prod|production)(?:[^a-z0-9]|$)""")
    private val signedRegex = Regex("""(?:^|[^a-z0-9])signed(?:[^a-z0-9]|$)""")
    private val debugRegex = Regex("""(?:^|[^a-z0-9])debug(?:[^a-z0-9]|$)""")
    private val testRegex = Regex("""(?:^|[^a-z0-9])(?:test|androidtest|uitest)(?:[^a-z0-9]|$)""")
    private val benchmarkRegex = Regex("""(?:^|[^a-z0-9])benchmark(?:[^a-z0-9]|$)""")
    private val unsignedRegex = Regex("""(?:^|[^a-z0-9])unsigned(?:[^a-z0-9]|$)""")
    private val splitRegex = Regex("""(?:^|[^a-z0-9])(?:config|split)[._-]""")
}
