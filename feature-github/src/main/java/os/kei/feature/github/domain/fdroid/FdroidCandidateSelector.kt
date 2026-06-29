package os.kei.feature.github.domain.fdroid

import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidVersionSnapshot
import os.kei.feature.github.model.FdroidAntiFeaturePolicy
import os.kei.feature.github.model.FdroidTrackedAppConfig
import os.kei.feature.github.model.FdroidVersionSelectionMode
import java.util.Locale

object FdroidCandidateSelector {
    fun select(
        snapshot: FdroidPackageSnapshot,
        config: FdroidTrackedAppConfig,
        deviceSdk: Int
    ): FdroidVersionSnapshot? {
        val versionNameRegex = config.versionNameRegex
            .takeIf { config.selectionMode == FdroidVersionSelectionMode.VersionNameRegex }
            ?.compileRegexOrNull()
            ?: if (config.selectionMode == FdroidVersionSelectionMode.VersionNameRegex) return null else null
        val apkNameRegex = config.apkNameRegex
            .takeIf { it.isNotBlank() }
            ?.compileRegexOrNull()
            ?: if (config.apkNameRegex.isNotBlank()) return null else null
        val candidates = snapshot.versions
            .filter { version -> version.isAllowedByAntiFeaturePolicy(config) }
            .filter { version -> versionNameRegex?.containsMatchIn(version.versionName) ?: true }
            .filter { version -> apkNameRegex?.containsMatchIn(version.apkName) ?: true }
        if (candidates.isEmpty()) return null

        return when (config.selectionMode) {
            FdroidVersionSelectionMode.SuggestedVersionCode -> {
                snapshot.suggestedVersionCode
                    ?.let { suggestedCode ->
                        candidates.firstOrNull { version ->
                            version.versionCode == suggestedCode && version.isCompatibleWith(deviceSdk)
                        }
                    }
                    ?: candidates.highestCompatible(deviceSdk)
            }

            FdroidVersionSelectionMode.HighestCompatibleVersionCode,
            FdroidVersionSelectionMode.VersionNameRegex -> candidates.highestCompatible(deviceSdk)

            FdroidVersionSelectionMode.HighestVersionCode -> candidates.highestVersion()
        }
    }

    private fun List<FdroidVersionSnapshot>.highestCompatible(
        deviceSdk: Int
    ): FdroidVersionSnapshot? {
        return filter { version -> version.isCompatibleWith(deviceSdk) }
            .highestVersion()
    }

    private fun List<FdroidVersionSnapshot>.highestVersion(): FdroidVersionSnapshot? {
        return maxByOrNull { version -> version.versionCode }
    }

    private fun FdroidVersionSnapshot.isCompatibleWith(deviceSdk: Int): Boolean {
        val min = minSdk ?: 1
        return min <= deviceSdk
    }

    private fun FdroidVersionSnapshot.isAllowedByAntiFeaturePolicy(
        config: FdroidTrackedAppConfig
    ): Boolean {
        val ids = antiFeatures.map { feature -> feature.id.lowercase(Locale.ROOT) }.toSet()
        if (ids.isEmpty()) return true
        return when (config.antiFeaturePolicy) {
            FdroidAntiFeaturePolicy.ShowAndWarn -> true
            FdroidAntiFeaturePolicy.HideTracking -> "tracking" !in ids
            FdroidAntiFeaturePolicy.HideSecurityRisk -> ids.none { id ->
                id in securityRiskAntiFeatureIds
            }

            FdroidAntiFeaturePolicy.Custom -> {
                val blocked = config.blockedAntiFeatures
                    .map { it.trim().lowercase(Locale.ROOT) }
                    .filter { it.isNotBlank() }
                    .toSet()
                blocked.isEmpty() || ids.none { id -> id in blocked }
            }
        }
    }

    private fun String.compileRegexOrNull(): Regex? {
        return runCatching { Regex(this) }.getOrNull()
    }

    private val securityRiskAntiFeatureIds = setOf(
        "knownvuln",
        "disabledalgorithm",
        "nosourcesince"
    )
}
