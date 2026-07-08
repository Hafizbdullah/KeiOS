@file:Suppress("FunctionName")

package os.kei.ui.page.main.settings.section

import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.ui.page.main.os.appLucideLockIcon
import os.kei.ui.page.main.settings.support.SettingsAppListAccessMode
import os.kei.ui.page.main.settings.support.SettingsAppStandbyBucketState
import os.kei.ui.page.main.settings.support.SettingsButtonActionItem
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsOemAutoStartState
import os.kei.ui.page.main.settings.support.SettingsValueItem
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import java.util.Locale

@Composable
internal fun SettingsPermissionKeepAliveSection(
    state: SettingsPermissionKeepAliveSectionState,
    actions: SettingsPermissionKeepAliveSectionActions,
    enabledCardColor: Color,
    disabledCardColor: Color,
) {
    val presentation = derivePermissionKeepAlivePresentation(state)
    SettingsGroupCard(
        header = stringResource(R.string.settings_group_permissions_header),
        title = stringResource(R.string.settings_group_permissions_title),
        sectionIcon = appLucideLockIcon(),
        containerColor = settingsSectionContainerColor(presentation, enabledCardColor, disabledCardColor),
    ) {
        SettingsButtonActionItem(
            title = stringResource(R.string.settings_notification_permission_title),
            summary =
                if (state.notificationPermissionGranted && state.notificationsEnabled) {
                    stringResource(R.string.settings_notification_permission_summary_granted)
                } else {
                    stringResource(R.string.settings_notification_permission_summary_restricted)
                },
            infoKey = stringResource(R.string.settings_permissions_info_status),
            infoValue =
                if (state.notificationPermissionGranted && state.notificationsEnabled) {
                    stringResource(R.string.settings_notification_permission_status_granted)
                } else {
                    stringResource(R.string.settings_notification_permission_status_restricted)
                },
            trailing = {
                AppStandaloneLiquidTextButton(
                    variant = GlassVariant.Compact,
                    text =
                        if (state.notificationPermissionGranted) {
                            stringResource(R.string.common_open)
                        } else {
                            stringResource(R.string.settings_notification_permission_action_request)
                        },
                    enabled =
                        if (state.notificationPermissionGranted) {
                            state.notificationSettingsActionAvailable
                        } else {
                            true
                        },
                    onClick = {
                        if (state.notificationPermissionGranted) {
                            actions.onOpenNotificationSettings()
                        } else {
                            actions.onRequestNotificationPermission()
                        }
                    },
                )
            },
        )
        SettingsButtonActionItem(
            title = stringResource(R.string.settings_android_background_title),
            summary = androidBackgroundSummary(state),
            infoKey = stringResource(R.string.settings_permissions_info_status),
            infoValue = androidBackgroundStatus(state),
            trailing = {
                AppStandaloneLiquidTextButton(
                    variant = GlassVariant.Compact,
                    text = stringResource(R.string.common_open),
                    enabled = state.androidBackgroundSettingsActionAvailable,
                    onClick = actions.onOpenAndroidBackgroundSettings,
                )
            },
        )
        SettingsValueItem(
            title = stringResource(R.string.settings_background_recovery_title),
            summary = backgroundRecoverySummary(state),
            infoKey = stringResource(R.string.settings_background_recovery_info_count),
            infoValue = backgroundRecoveryStatus(state),
        )
        SettingsButtonActionItem(
            title = stringResource(R.string.settings_battery_optimization_title),
            summary =
                if (state.ignoringBatteryOptimizations) {
                    stringResource(R.string.settings_battery_optimization_summary_ignored)
                } else {
                    stringResource(R.string.settings_battery_optimization_summary_restricted)
                },
            infoKey = stringResource(R.string.settings_battery_optimization_info_status),
            infoValue =
                if (state.ignoringBatteryOptimizations) {
                    stringResource(R.string.settings_battery_optimization_status_ignored)
                } else {
                    stringResource(R.string.settings_battery_optimization_status_restricted)
                },
            trailing = {
                AppStandaloneLiquidTextButton(
                    variant = GlassVariant.Compact,
                    text =
                        if (state.ignoringBatteryOptimizations) {
                            stringResource(R.string.common_open)
                        } else {
                            stringResource(R.string.settings_battery_optimization_action_request)
                        },
                    enabled = state.batteryOptimizationActionAvailable,
                    onClick = actions.onOpenBatteryOptimizationSettings,
                )
            },
        )
        SettingsButtonActionItem(
            title = stringResource(R.string.settings_oem_autostart_title),
            summary =
                when (state.oemAutoStartState) {
                    SettingsOemAutoStartState.Allowed -> {
                        stringResource(
                            R.string.settings_oem_autostart_summary_allowed,
                            state.oemAutoStartVendorLabel,
                        )
                    }

                    SettingsOemAutoStartState.Restricted -> {
                        stringResource(
                            R.string.settings_oem_autostart_summary_restricted,
                            state.oemAutoStartVendorLabel,
                        )
                    }

                    SettingsOemAutoStartState.Unknown -> {
                        stringResource(
                            R.string.settings_oem_autostart_summary_unknown,
                            state.oemAutoStartVendorLabel,
                        )
                    }

                    SettingsOemAutoStartState.Fallback -> {
                        stringResource(R.string.settings_oem_autostart_summary_fallback)
                    }

                    SettingsOemAutoStartState.Unsupported -> {
                        stringResource(R.string.settings_oem_autostart_summary_unsupported)
                    }
                },
            infoKey = stringResource(R.string.settings_permissions_info_status),
            infoValue =
                when (state.oemAutoStartState) {
                    SettingsOemAutoStartState.Allowed -> {
                        stringResource(
                            R.string.settings_oem_autostart_status_allowed,
                            state.oemAutoStartVendorLabel,
                        )
                    }

                    SettingsOemAutoStartState.Restricted -> {
                        stringResource(
                            R.string.settings_oem_autostart_status_restricted,
                            state.oemAutoStartVendorLabel,
                        )
                    }

                    SettingsOemAutoStartState.Unknown -> {
                        stringResource(
                            R.string.settings_oem_autostart_status_unknown,
                            state.oemAutoStartVendorLabel,
                        )
                    }

                    SettingsOemAutoStartState.Fallback -> {
                        stringResource(R.string.settings_oem_autostart_status_fallback)
                    }

                    SettingsOemAutoStartState.Unsupported -> {
                        stringResource(R.string.settings_oem_autostart_status_unsupported)
                    }
                },
            trailing = {
                if (state.oemAutoStartActionAvailable) {
                    AppStandaloneLiquidTextButton(
                        variant = GlassVariant.Compact,
                        text =
                            if (state.oemAutoStartState == SettingsOemAutoStartState.Allowed) {
                                stringResource(R.string.common_open)
                            } else {
                                stringResource(R.string.settings_oem_autostart_action_request)
                            },
                        onClick = actions.onOpenOemAutoStartSettings,
                    )
                }
            },
        )
        SettingsButtonActionItem(
            title = stringResource(R.string.settings_app_list_access_title),
            summary =
                when (state.appListAccessMode) {
                    SettingsAppListAccessMode.Shizuku -> {
                        stringResource(R.string.settings_app_list_access_summary_shizuku)
                    }

                    SettingsAppListAccessMode.Direct -> {
                        stringResource(R.string.settings_app_list_access_summary_direct)
                    }

                    SettingsAppListAccessMode.Restricted -> {
                        stringResource(R.string.settings_app_list_access_summary_restricted)
                    }
                },
            infoKey = stringResource(R.string.settings_app_list_access_info_mode),
            infoValue =
                when (state.appListAccessMode) {
                    SettingsAppListAccessMode.Shizuku -> {
                        stringResource(
                            R.string.settings_app_list_access_mode_shizuku,
                            state.appListDetectedCount,
                        )
                    }

                    SettingsAppListAccessMode.Direct -> {
                        stringResource(
                            R.string.settings_app_list_access_mode_direct,
                            state.appListDetectedCount,
                        )
                    }

                    SettingsAppListAccessMode.Restricted -> {
                        stringResource(R.string.settings_app_list_access_mode_restricted)
                    }
                },
            trailing = {
                AppStandaloneLiquidTextButton(
                    variant = GlassVariant.Compact,
                    text =
                        if (state.appListSettingsActionAvailable) {
                            stringResource(R.string.common_open)
                        } else {
                            stringResource(R.string.common_refresh)
                        },
                    enabled = state.appListSettingsActionAvailable || state.shizukuGranted,
                    onClick = {
                        if (state.appListSettingsActionAvailable) {
                            actions.onOpenAppListPermissionSettings()
                        } else {
                            actions.onCheckOrRequestShizuku()
                        }
                    },
                )
            },
        )
        SettingsButtonActionItem(
            title = stringResource(R.string.settings_shizuku_permission_title),
            summary =
                if (state.shizukuGranted) {
                    stringResource(R.string.settings_shizuku_permission_summary_granted)
                } else {
                    stringResource(R.string.settings_shizuku_permission_summary_restricted)
                },
            infoKey = stringResource(R.string.settings_permissions_info_status),
            infoValue =
                localizedShizukuStatusText(
                    statusText = state.shizukuStatusText,
                    granted = state.shizukuGranted,
                ).ifBlank {
                    if (state.shizukuGranted) {
                        stringResource(R.string.settings_shizuku_permission_status_granted)
                    } else {
                        stringResource(R.string.settings_shizuku_permission_status_restricted)
                    }
                },
            trailing = {
                AppStandaloneLiquidTextButton(
                    variant = GlassVariant.Compact,
                    text =
                        if (state.shizukuGranted) {
                            stringResource(R.string.common_refresh)
                        } else {
                            stringResource(R.string.settings_shizuku_permission_action_request)
                        },
                    onClick = actions.onCheckOrRequestShizuku,
                )
            },
        )
    }
}

@Composable
private fun androidBackgroundSummary(state: SettingsPermissionKeepAliveSectionState): String =
    when {
        state.androidBackgroundRestricted -> {
            stringResource(R.string.settings_android_background_summary_restricted)
        }

        state.androidPowerSaveMode -> {
            stringResource(R.string.settings_android_background_summary_power_save)
        }

        state.appStandbyBucket.isStandbyLimited() -> {
            stringResource(
                R.string.settings_android_background_summary_standby_limited,
                appStandbyBucketLabel(state.appStandbyBucket),
            )
        }

        state.androidDeviceIdleMode -> {
            stringResource(R.string.settings_android_background_summary_idle)
        }

        else -> {
            stringResource(R.string.settings_android_background_summary_normal)
        }
    }

@Composable
private fun androidBackgroundStatus(state: SettingsPermissionKeepAliveSectionState): String =
    when {
        state.androidBackgroundRestricted -> {
            stringResource(
                R.string.settings_android_background_status_restricted,
                appStandbyBucketLabel(state.appStandbyBucket),
            )
        }

        state.androidPowerSaveMode -> {
            stringResource(
                R.string.settings_android_background_status_power_save,
                appStandbyBucketLabel(state.appStandbyBucket),
            )
        }

        state.appStandbyBucket.isStandbyLimited() -> {
            stringResource(
                R.string.settings_android_background_status_standby_limited,
                appStandbyBucketLabel(state.appStandbyBucket),
            )
        }

        state.androidDeviceIdleMode -> {
            stringResource(
                R.string.settings_android_background_status_idle,
                appStandbyBucketLabel(state.appStandbyBucket),
            )
        }

        else -> {
            stringResource(
                R.string.settings_android_background_status_normal,
                appStandbyBucketLabel(state.appStandbyBucket),
            )
        }
    }

@Composable
private fun appStandbyBucketLabel(bucket: SettingsAppStandbyBucketState): String =
    when (bucket) {
        SettingsAppStandbyBucketState.Exempted -> {
            stringResource(R.string.settings_app_standby_bucket_exempted)
        }

        SettingsAppStandbyBucketState.Active -> {
            stringResource(R.string.settings_app_standby_bucket_active)
        }

        SettingsAppStandbyBucketState.WorkingSet -> {
            stringResource(R.string.settings_app_standby_bucket_working_set)
        }

        SettingsAppStandbyBucketState.Frequent -> {
            stringResource(R.string.settings_app_standby_bucket_frequent)
        }

        SettingsAppStandbyBucketState.Rare -> {
            stringResource(R.string.settings_app_standby_bucket_rare)
        }

        SettingsAppStandbyBucketState.Restricted -> {
            stringResource(R.string.settings_app_standby_bucket_restricted)
        }

        SettingsAppStandbyBucketState.Never -> {
            stringResource(R.string.settings_app_standby_bucket_never)
        }

        SettingsAppStandbyBucketState.Unknown -> {
            stringResource(R.string.settings_app_standby_bucket_unknown)
        }
    }

private fun SettingsAppStandbyBucketState.isStandbyLimited(): Boolean =
    this == SettingsAppStandbyBucketState.Rare ||
        this == SettingsAppStandbyBucketState.Restricted ||
        this == SettingsAppStandbyBucketState.Never

@Composable
private fun backgroundRecoverySummary(state: SettingsPermissionKeepAliveSectionState): String {
    val snapshot = state.backgroundRecoverySnapshot
    if (snapshot.lastFinishedAtMs <= 0L) {
        return stringResource(R.string.settings_background_recovery_summary_empty)
    }
    val action = backgroundRecoveryActionLabel(snapshot.lastAction)
    val time = formatBackgroundRecoveryTime(snapshot.lastFinishedAtMs)
    val elapsed = formatBackgroundRecoveryElapsed(snapshot.lastElapsedMs)
    val failureReason =
        if (snapshot.lastFailureReason.isBlank()) {
            stringResource(R.string.common_unknown)
        } else {
            snapshot.lastFailureReason
        }
    return if (snapshot.lastFailed) {
        stringResource(
            R.string.settings_background_recovery_summary_failed,
            action,
            time,
            failureReason,
        )
    } else {
        stringResource(
            R.string.settings_background_recovery_summary_succeeded,
            action,
            time,
            elapsed,
        )
    }
}

@Composable
private fun backgroundRecoveryStatus(state: SettingsPermissionKeepAliveSectionState): String {
    val snapshot = state.backgroundRecoverySnapshot
    if (snapshot.recoveryCount <= 0) {
        return stringResource(R.string.settings_background_recovery_status_empty)
    }
    return if (snapshot.lastFailed) {
        stringResource(R.string.settings_background_recovery_status_failed, snapshot.recoveryCount)
    } else {
        stringResource(R.string.settings_background_recovery_status_succeeded, snapshot.recoveryCount)
    }
}

@Composable
private fun backgroundRecoveryActionLabel(action: String): String =
    when (action) {
        Intent.ACTION_BOOT_COMPLETED -> {
            stringResource(R.string.settings_background_recovery_action_boot_completed)
        }

        Intent.ACTION_MY_PACKAGE_REPLACED -> {
            stringResource(R.string.settings_background_recovery_action_package_replaced)
        }

        Intent.ACTION_TIME_CHANGED -> {
            stringResource(R.string.settings_background_recovery_action_time_changed)
        }

        Intent.ACTION_TIMEZONE_CHANGED -> {
            stringResource(R.string.settings_background_recovery_action_timezone_changed)
        }

        else -> {
            stringResource(R.string.settings_background_recovery_action_unknown)
        }
    }

private fun formatBackgroundRecoveryTime(epochMs: Long): String =
    DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMdHm"), epochMs)
        .toString()

@Composable
private fun formatBackgroundRecoveryElapsed(elapsedMs: Long): String =
    if (elapsedMs < 1000L) {
        stringResource(R.string.settings_background_recovery_elapsed_ms, elapsedMs.coerceAtLeast(0L))
    } else {
        stringResource(R.string.settings_background_recovery_elapsed_s, elapsedMs / 1000L)
    }

@Composable
private fun localizedShizukuStatusText(
    statusText: String,
    granted: Boolean,
): String {
    val trimmed = statusText.trim()
    if (trimmed.isEmpty()) return ""
    return when {
        trimmed == "Shizuku service unavailable (start Shizuku app first)" -> {
            stringResource(R.string.settings_shizuku_status_service_unavailable)
        }

        trimmed == "Shizuku service disconnected" -> {
            stringResource(R.string.settings_shizuku_status_service_disconnected)
        }

        trimmed == "Shizuku pre-v11 is unsupported" -> {
            stringResource(R.string.settings_shizuku_status_pre_v11_unsupported)
        }

        trimmed == "Shizuku permission: not granted" -> {
            stringResource(R.string.settings_shizuku_status_permission_not_granted)
        }

        trimmed == "Shizuku permission: denied" -> {
            stringResource(R.string.settings_shizuku_status_permission_denied)
        }

        trimmed == "Shizuku permission denied permanently" -> {
            stringResource(R.string.settings_shizuku_status_permission_denied_permanently)
        }

        trimmed == "Requesting Shizuku permission..." -> {
            stringResource(R.string.settings_shizuku_status_requesting_permission)
        }

        trimmed == "Shizuku process API unavailable" -> {
            stringResource(R.string.settings_shizuku_status_process_api_unavailable)
        }

        trimmed.startsWith("Shizuku command unavailable: unsupported service uid ") -> {
            stringResource(
                R.string.settings_shizuku_status_unsupported_service_uid,
                trimmed.substringAfterLast(' ').ifBlank { "unknown" },
            )
        }

        trimmed.startsWith("Shizuku init failed:") -> {
            stringResource(
                R.string.settings_shizuku_status_init_failed,
                trimmed.substringAfter(':').trim().ifBlank { "unknown" },
            )
        }

        trimmed.startsWith("Shizuku request failed:") -> {
            stringResource(
                R.string.settings_shizuku_status_request_failed,
                trimmed.substringAfter(':').trim().ifBlank { "unknown" },
            )
        }

        trimmed.startsWith("Shizuku permission: granted") -> {
            stringResource(
                R.string.settings_shizuku_status_permission_granted_identity,
                trimmed.substringAfter('(', "").substringBefore(')').ifBlank {
                    if (granted) "shell" else "unknown"
                },
            )
        }

        else -> {
            trimmed
        }
    }
}
