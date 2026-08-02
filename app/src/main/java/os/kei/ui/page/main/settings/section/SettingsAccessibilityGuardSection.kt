@file:Suppress("FunctionName")

package os.kei.ui.page.main.settings.section

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.feature.keepalive.accessibility.AccessibilityGuardCheckReason
import os.kei.feature.keepalive.accessibility.AccessibilityGuardCheckStatus
import os.kei.ui.page.main.os.appLucideHistoryIcon
import os.kei.ui.page.main.os.appLucideLockIcon
import os.kei.ui.page.main.settings.support.SettingsButtonActionItem
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsToggleItem
import os.kei.ui.page.main.settings.support.SettingsValueItem
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import java.util.Locale

@Composable
internal fun SettingsAccessibilityGuardPolicyCard(
    state: SettingsPermissionKeepAliveSectionState,
    actions: SettingsPermissionKeepAliveSectionActions,
    containerColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val guardState = state.accessibilityGuardState
    SettingsGroupCard(
        header = stringResource(R.string.settings_accessibility_guard_header),
        title = stringResource(R.string.settings_accessibility_guard_policy_title),
        subtitle = accessibilityGuardPolicySubtitle(state),
        sectionIcon = appLucideLockIcon(),
        containerColor = containerColor,
        exportBackdropToContent = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        SettingsValueItem(
            title = stringResource(R.string.settings_accessibility_guard_capability_title),
            summary = stringResource(R.string.settings_accessibility_guard_capability_summary),
            infoKey = stringResource(R.string.settings_accessibility_guard_info_capability),
            infoValue = accessibilityGuardCapabilityLabel(guardState),
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_accessibility_guard_daemon_title),
            summary = stringResource(R.string.settings_accessibility_guard_daemon_summary),
            checked = guardState.daemonEnabled,
            onCheckedChange = actions.onAccessibilityGuardDaemonChanged,
            infoKey = stringResource(R.string.settings_permissions_info_status),
            infoValue = accessibilityGuardOnOffLabel(guardState.daemonEnabled),
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_accessibility_guard_boot_check_title),
            summary = stringResource(R.string.settings_accessibility_guard_boot_check_summary),
            checked = guardState.bootCheckEnabled,
            onCheckedChange = actions.onAccessibilityGuardBootCheckChanged,
            infoKey = stringResource(R.string.settings_permissions_info_status),
            infoValue = accessibilityGuardOnOffLabel(guardState.bootCheckEnabled),
        )
        SettingsToggleItem(
            title = stringResource(R.string.settings_accessibility_guard_screen_on_title),
            summary = stringResource(R.string.settings_accessibility_guard_screen_on_summary),
            checked = guardState.screenOnCheckEnabled,
            onCheckedChange = actions.onAccessibilityGuardScreenOnChanged,
            infoKey = stringResource(R.string.settings_permissions_info_status),
            infoValue = accessibilityGuardOnOffLabel(guardState.screenOnCheckEnabled),
        )
        SettingsValueItem(
            title = stringResource(R.string.settings_accessibility_guard_disclosure_title),
            summary = stringResource(R.string.settings_accessibility_guard_disclosure_summary),
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_accessibility_guard_disclosure_scope),
        )
        SettingsButtonActionItem(
            title = stringResource(R.string.settings_accessibility_guard_actions_title),
            summary = stringResource(R.string.settings_accessibility_guard_actions_summary),
            trailing = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppStandaloneLiquidTextButton(
                        variant = GlassVariant.Compact,
                        text =
                            if (guardState.manualCheckRunning) {
                                stringResource(R.string.settings_accessibility_guard_action_checking)
                            } else {
                                stringResource(R.string.settings_accessibility_guard_action_check)
                            },
                        enabled = !guardState.manualCheckRunning,
                        onClick = actions.onRunAccessibilityGuardCheck,
                    )
                    AppStandaloneLiquidTextButton(
                        variant = GlassVariant.Compact,
                        text =
                            if (guardState.exportingHistory) {
                                stringResource(R.string.settings_accessibility_guard_action_exporting)
                            } else {
                                stringResource(R.string.settings_accessibility_guard_action_export_history)
                            },
                        enabled = !guardState.exportingHistory,
                        onClick = actions.onExportAccessibilityGuardHistory,
                    )
                }
            },
        )
    }
}

@Composable
internal fun SettingsAccessibilityGuardHistoryCard(
    state: SettingsPermissionKeepAliveSectionState,
    containerColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val guardState = state.accessibilityGuardState
    SettingsGroupCard(
        header = stringResource(R.string.settings_accessibility_guard_header),
        title = stringResource(R.string.settings_accessibility_guard_history_title),
        subtitle = stringResource(R.string.settings_accessibility_guard_history_summary),
        sectionIcon = appLucideHistoryIcon(),
        containerColor = containerColor,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        val latest = guardState.latestHistory
        if (latest == null) {
            SettingsValueItem(
                title = stringResource(R.string.settings_accessibility_guard_history_empty_title),
                summary = stringResource(R.string.settings_accessibility_guard_history_empty_summary),
                infoKey = stringResource(R.string.settings_accessibility_guard_history_info_count),
                infoValue = stringResource(R.string.settings_accessibility_guard_history_count, guardState.historyCount),
            )
        } else {
            SettingsValueItem(
                title = stringResource(R.string.settings_accessibility_guard_history_latest_title),
                summary =
                    stringResource(
                        R.string.settings_accessibility_guard_history_latest_summary,
                        accessibilityGuardReasonLabel(latest.reason),
                        formatAccessibilityGuardHistoryTime(latest.timestampMs),
                        accessibilityGuardStatusLabel(latest.status),
                    ),
                infoKey = stringResource(R.string.settings_accessibility_guard_history_info_result),
                infoValue =
                    stringResource(
                        R.string.settings_accessibility_guard_history_latest_value,
                        latest.checkCount,
                        latest.healthyCount,
                        latest.warningCount,
                        formatAccessibilityGuardElapsed(latest.elapsedMs),
                    ),
            )
            if (latest.failureReason.isNotBlank()) {
                SettingsValueItem(
                    title = stringResource(R.string.settings_accessibility_guard_history_failure_title),
                    summary = latest.failureReason,
                    infoKey = stringResource(R.string.settings_accessibility_guard_history_info_trigger),
                    infoValue = latest.triggerAction.ifBlank { stringResource(R.string.common_na) },
                )
            }
        }
    }
}

@Composable
private fun accessibilityGuardPolicySubtitle(state: SettingsPermissionKeepAliveSectionState): String =
    if (state.privilegeGranted) {
        stringResource(R.string.settings_accessibility_guard_policy_summary_ready)
    } else {
        stringResource(R.string.settings_accessibility_guard_policy_summary_need_privilege)
    }

@Composable
private fun accessibilityGuardOnOffLabel(enabled: Boolean): String =
    if (enabled) {
        stringResource(R.string.settings_accessibility_guard_state_enabled)
    } else {
        stringResource(R.string.settings_accessibility_guard_state_disabled)
    }

@Composable
private fun accessibilityGuardCapabilityLabel(state: SettingsAccessibilityGuardUiState): String =
    if (state.secureSettingsReadable) {
        stringResource(R.string.settings_accessibility_guard_capability_ready)
    } else {
        stringResource(R.string.settings_accessibility_guard_capability_need_privilege)
    }

@Composable
private fun accessibilityGuardReasonLabel(reason: AccessibilityGuardCheckReason): String =
    when (reason) {
        AccessibilityGuardCheckReason.Manual -> {
            stringResource(R.string.settings_accessibility_guard_reason_manual)
        }

        AccessibilityGuardCheckReason.ForegroundServiceStart -> {
            stringResource(R.string.settings_accessibility_guard_reason_foreground_service_start)
        }

        AccessibilityGuardCheckReason.SecureSettingChanged -> {
            stringResource(R.string.settings_accessibility_guard_reason_secure_setting_changed)
        }

        AccessibilityGuardCheckReason.ScreenOn -> {
            stringResource(R.string.settings_accessibility_guard_reason_screen_on)
        }

        AccessibilityGuardCheckReason.BootCompleted -> {
            stringResource(R.string.settings_accessibility_guard_reason_boot_completed)
        }

        AccessibilityGuardCheckReason.PackageReplaced -> {
            stringResource(R.string.settings_accessibility_guard_reason_package_replaced)
        }

        AccessibilityGuardCheckReason.TimeoutRecovery -> {
            stringResource(R.string.settings_accessibility_guard_reason_timeout_recovery)
        }
    }

@Composable
private fun accessibilityGuardStatusLabel(status: AccessibilityGuardCheckStatus): String =
    when (status) {
        AccessibilityGuardCheckStatus.Healthy -> {
            stringResource(R.string.settings_accessibility_guard_status_healthy)
        }

        AccessibilityGuardCheckStatus.Checked -> {
            stringResource(R.string.settings_accessibility_guard_status_checked)
        }

        AccessibilityGuardCheckStatus.MissingPrivilege -> {
            stringResource(R.string.settings_accessibility_guard_status_missing_privilege)
        }

        AccessibilityGuardCheckStatus.Failed -> {
            stringResource(R.string.settings_accessibility_guard_status_failed)
        }

        AccessibilityGuardCheckStatus.TimedOut -> {
            stringResource(R.string.settings_accessibility_guard_status_timed_out)
        }
    }

private fun formatAccessibilityGuardHistoryTime(epochMs: Long): String =
    DateFormat
        .format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMdHm"), epochMs)
        .toString()

@Composable
private fun formatAccessibilityGuardElapsed(elapsedMs: Long): String =
    if (elapsedMs < 1000L) {
        stringResource(R.string.settings_background_recovery_elapsed_ms, elapsedMs.coerceAtLeast(0L))
    } else {
        stringResource(R.string.settings_background_recovery_elapsed_s, elapsedMs / 1000L)
    }
