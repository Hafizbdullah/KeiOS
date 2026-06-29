package os.kei.ui.page.main.github.sheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.feature.github.model.FdroidAntiFeaturePolicy
import os.kei.feature.github.model.FdroidTrustPolicy
import os.kei.feature.github.model.FdroidVersionSelectionMode
import os.kei.feature.github.model.GITHUB_FDROID_DEFAULT_REFRESH_INTERVAL_HOURS
import os.kei.feature.github.model.GitHubTrackedActionsUpdateIntervalMode
import os.kei.feature.github.model.GitHubTrackedIgnoreMode
import os.kei.feature.github.model.GitHubTrackedPreciseApkVersionMode
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.feature.github.model.GitHubTrackedUpdateIntervalMode

@Composable
internal fun trackedSourceModeLabel(mode: GitHubTrackedSourceMode): String =
    when (mode) {
        GitHubTrackedSourceMode.GitHubRepository -> {
            stringResource(R.string.github_track_sheet_source_mode_github)
        }

        GitHubTrackedSourceMode.GitRepository -> {
            stringResource(R.string.github_track_sheet_source_mode_git)
        }

        GitHubTrackedSourceMode.DirectApk -> {
            stringResource(R.string.github_track_sheet_source_mode_direct_apk)
        }

        GitHubTrackedSourceMode.FdroidRepository -> {
            stringResource(R.string.github_track_sheet_source_mode_fdroid)
        }
    }

@Composable
internal fun preciseApkVersionModeLabel(mode: GitHubTrackedPreciseApkVersionMode): String =
    when (mode) {
        GitHubTrackedPreciseApkVersionMode.FollowGlobal -> {
            stringResource(R.string.github_track_sheet_precise_apk_version_follow_global)
        }

        GitHubTrackedPreciseApkVersionMode.Enabled -> {
            stringResource(R.string.github_track_sheet_precise_apk_version_enabled)
        }

        GitHubTrackedPreciseApkVersionMode.Disabled -> {
            stringResource(R.string.github_track_sheet_precise_apk_version_disabled)
        }
    }

@Composable
internal fun updateIntervalModeLabel(
    mode: GitHubTrackedUpdateIntervalMode,
    globalRefreshIntervalHours: Int,
    sourceMode: GitHubTrackedSourceMode? = null,
): String =
    when (mode) {
        GitHubTrackedUpdateIntervalMode.FollowGlobal -> {
            if (sourceMode == GitHubTrackedSourceMode.FdroidRepository) {
                stringResource(
                    R.string.github_track_sheet_update_interval_fdroid_default_format,
                    refreshIntervalLabel(GITHUB_FDROID_DEFAULT_REFRESH_INTERVAL_HOURS),
                )
            } else {
                stringResource(
                    R.string.github_track_sheet_update_interval_follow_global_format,
                    refreshIntervalLabel(globalRefreshIntervalHours),
                )
            }
        }

        GitHubTrackedUpdateIntervalMode.Hour1 -> {
            stringResource(R.string.github_refresh_interval_1h)
        }

        GitHubTrackedUpdateIntervalMode.Hours3 -> {
            stringResource(R.string.github_refresh_interval_3h)
        }

        GitHubTrackedUpdateIntervalMode.Hours6 -> {
            stringResource(R.string.github_refresh_interval_6h)
        }

        GitHubTrackedUpdateIntervalMode.Hours12 -> {
            stringResource(R.string.github_refresh_interval_12h)
        }

        GitHubTrackedUpdateIntervalMode.Hours24 -> {
            stringResource(R.string.github_refresh_interval_24h)
        }
    }

@Composable
internal fun trackedIgnoreModeLabel(mode: GitHubTrackedIgnoreMode): String =
    when (mode) {
        GitHubTrackedIgnoreMode.None -> {
            stringResource(R.string.github_track_sheet_ignore_none)
        }

        GitHubTrackedIgnoreMode.Temporary -> {
            stringResource(R.string.github_track_sheet_ignore_temporary)
        }

        GitHubTrackedIgnoreMode.AllVersions -> {
            stringResource(R.string.github_track_sheet_ignore_all_versions)
        }

        GitHubTrackedIgnoreMode.CurrentStable -> {
            stringResource(R.string.github_track_sheet_ignore_current_stable)
        }

        GitHubTrackedIgnoreMode.CurrentPreRelease -> {
            stringResource(R.string.github_track_sheet_ignore_current_prerelease)
        }
    }

@Composable
internal fun trackedIgnoreModeSummary(mode: GitHubTrackedIgnoreMode): String =
    when (mode) {
        GitHubTrackedIgnoreMode.None -> {
            stringResource(R.string.github_track_sheet_summary_ignore_none)
        }

        GitHubTrackedIgnoreMode.Temporary -> {
            stringResource(R.string.github_track_sheet_summary_ignore_temporary)
        }

        GitHubTrackedIgnoreMode.AllVersions -> {
            stringResource(R.string.github_track_sheet_summary_ignore_all_versions)
        }

        GitHubTrackedIgnoreMode.CurrentStable -> {
            stringResource(R.string.github_track_sheet_summary_ignore_current_stable)
        }

        GitHubTrackedIgnoreMode.CurrentPreRelease -> {
            stringResource(R.string.github_track_sheet_summary_ignore_current_prerelease)
        }
    }

@Composable
internal fun fdroidVersionSelectionModeLabel(mode: FdroidVersionSelectionMode): String =
    when (mode) {
        FdroidVersionSelectionMode.SuggestedVersionCode -> {
            stringResource(R.string.github_track_sheet_fdroid_version_selection_suggested)
        }

        FdroidVersionSelectionMode.HighestCompatibleVersionCode -> {
            stringResource(R.string.github_track_sheet_fdroid_version_selection_highest_compatible)
        }

        FdroidVersionSelectionMode.HighestVersionCode -> {
            stringResource(R.string.github_track_sheet_fdroid_version_selection_highest)
        }

        FdroidVersionSelectionMode.VersionNameRegex -> {
            stringResource(R.string.github_track_sheet_fdroid_version_selection_regex)
        }
    }

@Composable
internal fun fdroidVersionSelectionModeSummary(mode: FdroidVersionSelectionMode): String =
    when (mode) {
        FdroidVersionSelectionMode.SuggestedVersionCode -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_version_selection_suggested)
        }

        FdroidVersionSelectionMode.HighestCompatibleVersionCode -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_version_selection_highest_compatible)
        }

        FdroidVersionSelectionMode.HighestVersionCode -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_version_selection_highest)
        }

        FdroidVersionSelectionMode.VersionNameRegex -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_version_selection_regex)
        }
    }

@Composable
internal fun fdroidTrustPolicyLabel(policy: FdroidTrustPolicy): String =
    when (policy) {
        FdroidTrustPolicy.TrackOnlyWarn -> {
            stringResource(R.string.github_track_sheet_fdroid_trust_track_warn)
        }

        FdroidTrustPolicy.RequireRepoFingerprint -> {
            stringResource(R.string.github_track_sheet_fdroid_trust_repo_fingerprint)
        }

        FdroidTrustPolicy.RequireApkHash -> {
            stringResource(R.string.github_track_sheet_fdroid_trust_apk_hash)
        }

        FdroidTrustPolicy.RequireOfficialSignerIndex -> {
            stringResource(R.string.github_track_sheet_fdroid_trust_official_signer)
        }
    }

@Composable
internal fun fdroidTrustPolicySummary(policy: FdroidTrustPolicy): String =
    when (policy) {
        FdroidTrustPolicy.TrackOnlyWarn -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_trust_track_warn)
        }

        FdroidTrustPolicy.RequireRepoFingerprint -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_trust_repo_fingerprint)
        }

        FdroidTrustPolicy.RequireApkHash -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_trust_apk_hash)
        }

        FdroidTrustPolicy.RequireOfficialSignerIndex -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_trust_official_signer)
        }
    }

@Composable
internal fun fdroidAntiFeaturePolicyLabel(policy: FdroidAntiFeaturePolicy): String =
    when (policy) {
        FdroidAntiFeaturePolicy.ShowAndWarn -> {
            stringResource(R.string.github_track_sheet_fdroid_antifeature_show_warn)
        }

        FdroidAntiFeaturePolicy.HideTracking -> {
            stringResource(R.string.github_track_sheet_fdroid_antifeature_hide_tracking)
        }

        FdroidAntiFeaturePolicy.HideSecurityRisk -> {
            stringResource(R.string.github_track_sheet_fdroid_antifeature_hide_security)
        }

        FdroidAntiFeaturePolicy.Custom -> {
            stringResource(R.string.github_track_sheet_fdroid_antifeature_custom)
        }
    }

@Composable
internal fun fdroidAntiFeaturePolicySummary(policy: FdroidAntiFeaturePolicy): String =
    when (policy) {
        FdroidAntiFeaturePolicy.ShowAndWarn -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_antifeature_show_warn)
        }

        FdroidAntiFeaturePolicy.HideTracking -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_antifeature_hide_tracking)
        }

        FdroidAntiFeaturePolicy.HideSecurityRisk -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_antifeature_hide_security)
        }

        FdroidAntiFeaturePolicy.Custom -> {
            stringResource(R.string.github_track_sheet_summary_fdroid_antifeature_custom)
        }
    }

@Composable
internal fun actionsUpdateIntervalModeLabel(
    mode: GitHubTrackedActionsUpdateIntervalMode,
    globalRefreshIntervalHours: Int,
): String =
    when (mode) {
        GitHubTrackedActionsUpdateIntervalMode.FollowGlobal -> {
            stringResource(
                R.string.github_track_sheet_actions_update_interval_follow_global_format,
                refreshIntervalLabel(globalRefreshIntervalHours),
            )
        }

        GitHubTrackedActionsUpdateIntervalMode.Minutes15 -> {
            stringResource(R.string.github_track_sheet_actions_update_interval_15m)
        }

        GitHubTrackedActionsUpdateIntervalMode.Minutes30 -> {
            stringResource(R.string.github_track_sheet_actions_update_interval_30m)
        }

        GitHubTrackedActionsUpdateIntervalMode.Hour1 -> {
            stringResource(R.string.github_track_sheet_actions_update_interval_1h)
        }

        GitHubTrackedActionsUpdateIntervalMode.Hours2 -> {
            stringResource(R.string.github_track_sheet_actions_update_interval_2h)
        }

        GitHubTrackedActionsUpdateIntervalMode.Hours3 -> {
            stringResource(R.string.github_track_sheet_actions_update_interval_3h)
        }
    }

@Composable
internal fun refreshIntervalLabel(hours: Int): String =
    when (hours) {
        1 -> stringResource(R.string.github_refresh_interval_1h)
        3 -> stringResource(R.string.github_refresh_interval_3h)
        6 -> stringResource(R.string.github_refresh_interval_6h)
        12 -> stringResource(R.string.github_refresh_interval_12h)
        24 -> stringResource(R.string.github_refresh_interval_24h)
        else -> stringResource(R.string.github_refresh_interval_3h)
    }
