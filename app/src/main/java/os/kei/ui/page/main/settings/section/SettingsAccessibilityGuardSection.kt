@file:Suppress("FunctionName")

package os.kei.ui.page.main.settings.section

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.keepalive.accessibility.AccessibilityGuardRestoreReason
import os.kei.feature.keepalive.accessibility.AccessibilityGuardRestoreStatus
import os.kei.ui.page.main.os.appLucideHistoryIcon
import os.kei.ui.page.main.os.appLucideListIcon
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
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        SettingsValueItem(
            title = stringResource(R.string.settings_accessibility_guard_capability_title),
            summary = stringResource(R.string.settings_accessibility_guard_capability_summary),
            infoKey = stringResource(R.string.settings_accessibility_guard_info_targets),
            infoValue =
                stringResource(
                    R.string.settings_accessibility_guard_targets_value,
                    guardState.enabledGuardedCount,
                    guardState.guardedCount,
                    guardState.serviceCount,
                ),
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
            title = stringResource(R.string.settings_accessibility_guard_boot_restore_title),
            summary = stringResource(R.string.settings_accessibility_guard_boot_restore_summary),
            checked = guardState.bootRestoreEnabled,
            onCheckedChange = actions.onAccessibilityGuardBootRestoreChanged,
            infoKey = stringResource(R.string.settings_permissions_info_status),
            infoValue = accessibilityGuardOnOffLabel(guardState.bootRestoreEnabled),
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
internal fun SettingsAccessibilityGuardServicesCard(
    state: SettingsPermissionKeepAliveSectionState,
    actions: SettingsPermissionKeepAliveSectionActions,
    containerColor: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val guardState = state.accessibilityGuardState
    SettingsGroupCard(
        header = stringResource(R.string.settings_accessibility_guard_header),
        title = stringResource(R.string.settings_accessibility_guard_services_title),
        subtitle = stringResource(R.string.settings_accessibility_guard_services_summary),
        sectionIcon = appLucideListIcon(),
        containerColor = containerColor,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        SettingsValueItem(
            title = stringResource(R.string.settings_accessibility_guard_services_overview_title),
            summary =
                if (guardState.loading) {
                    stringResource(R.string.settings_accessibility_guard_services_loading)
                } else {
                    stringResource(R.string.settings_accessibility_guard_services_overview_summary)
                },
            infoKey = stringResource(R.string.settings_accessibility_guard_info_targets),
            infoValue =
                stringResource(
                    R.string.settings_accessibility_guard_targets_value,
                    guardState.enabledGuardedCount,
                    guardState.guardedCount,
                    guardState.serviceCount,
                ),
        )
        if (guardState.services.isEmpty()) {
            SettingsValueItem(
                title = stringResource(R.string.settings_accessibility_guard_services_empty_title),
                summary = stringResource(R.string.settings_accessibility_guard_services_empty_summary),
                infoKey = stringResource(R.string.settings_permissions_info_status),
                infoValue = stringResource(R.string.settings_accessibility_guard_service_status_empty),
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap),
            ) {
                items(
                    items = guardState.services,
                    key = { item -> item.flattenedId },
                    contentType = { "accessibility_guard_service" },
                ) { service ->
                    AccessibilityGuardServiceItem(
                        item = service,
                        enabled = !guardState.loading,
                        onCheckedChange = { checked ->
                            actions.onAccessibilityGuardServiceCheckedChange(service.flattenedId, checked)
                        },
                    )
                }
            }
        }
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
                        latest.selectedCount,
                        latest.restoredCount,
                        latest.skippedCount,
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
private fun AccessibilityGuardServiceItem(
    item: SettingsAccessibilityGuardServiceUiItem,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsToggleItem(
        title = item.label,
        summary = "${item.packageLabel} · ${item.packageName}",
        checked = item.guarded,
        onCheckedChange = onCheckedChange,
        infoKey = stringResource(R.string.settings_permissions_info_status),
        infoValue = accessibilityGuardServiceStatus(item),
        enabled = enabled,
    )
}

@Composable
private fun accessibilityGuardPolicySubtitle(state: SettingsPermissionKeepAliveSectionState): String =
    if (state.shizukuGranted) {
        stringResource(R.string.settings_accessibility_guard_policy_summary_ready)
    } else {
        stringResource(R.string.settings_accessibility_guard_policy_summary_need_shizuku)
    }

@Composable
private fun accessibilityGuardOnOffLabel(enabled: Boolean): String =
    if (enabled) {
        stringResource(R.string.settings_accessibility_guard_state_enabled)
    } else {
        stringResource(R.string.settings_accessibility_guard_state_disabled)
    }

@Composable
private fun accessibilityGuardServiceStatus(item: SettingsAccessibilityGuardServiceUiItem): String =
    listOfNotNull(
        if (item.guarded) {
            stringResource(R.string.settings_accessibility_guard_service_status_guarded)
        } else {
            stringResource(R.string.settings_accessibility_guard_service_status_unselected)
        },
        if (item.enabled) {
            stringResource(R.string.settings_accessibility_guard_service_status_enabled)
        } else {
            stringResource(R.string.settings_accessibility_guard_service_status_off)
        },
        if (item.system) {
            stringResource(R.string.settings_accessibility_guard_service_status_system)
        } else {
            null
        },
    ).joinToString(" / ")

@Composable
private fun accessibilityGuardReasonLabel(reason: AccessibilityGuardRestoreReason): String =
    when (reason) {
        AccessibilityGuardRestoreReason.Manual -> {
            stringResource(R.string.settings_accessibility_guard_reason_manual)
        }

        AccessibilityGuardRestoreReason.ForegroundServiceStart -> {
            stringResource(R.string.settings_accessibility_guard_reason_foreground_service_start)
        }

        AccessibilityGuardRestoreReason.SecureSettingChanged -> {
            stringResource(R.string.settings_accessibility_guard_reason_secure_setting_changed)
        }

        AccessibilityGuardRestoreReason.ScreenOn -> {
            stringResource(R.string.settings_accessibility_guard_reason_screen_on)
        }

        AccessibilityGuardRestoreReason.BootCompleted -> {
            stringResource(R.string.settings_accessibility_guard_reason_boot_completed)
        }

        AccessibilityGuardRestoreReason.PackageReplaced -> {
            stringResource(R.string.settings_accessibility_guard_reason_package_replaced)
        }

        AccessibilityGuardRestoreReason.TimeoutRecovery -> {
            stringResource(R.string.settings_accessibility_guard_reason_timeout_recovery)
        }
    }

@Composable
private fun accessibilityGuardStatusLabel(status: AccessibilityGuardRestoreStatus): String =
    when (status) {
        AccessibilityGuardRestoreStatus.Restored -> {
            stringResource(R.string.settings_accessibility_guard_status_restored)
        }

        AccessibilityGuardRestoreStatus.SkippedNoTargets -> {
            stringResource(R.string.settings_accessibility_guard_status_skipped_no_targets)
        }

        AccessibilityGuardRestoreStatus.SkippedMissingPrivilege -> {
            stringResource(R.string.settings_accessibility_guard_status_skipped_missing_privilege)
        }

        AccessibilityGuardRestoreStatus.SkippedAlreadyEnabled -> {
            stringResource(R.string.settings_accessibility_guard_status_skipped_already_enabled)
        }

        AccessibilityGuardRestoreStatus.SkippedCooldown -> {
            stringResource(R.string.settings_accessibility_guard_status_skipped_cooldown)
        }

        AccessibilityGuardRestoreStatus.Failed -> {
            stringResource(R.string.settings_accessibility_guard_status_failed)
        }

        AccessibilityGuardRestoreStatus.TimedOut -> {
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
