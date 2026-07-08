package os.kei.ui.page.main.github.sheet

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.ui.page.main.github.page.GitHubDebugVisibleRefreshDefaultLimit
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet

@Composable
internal fun GitHubDebugSheet(
    show: Boolean,
    backdrop: LayerBackdrop,
    trackedCount: Int,
    visibleIncrementalTargetCount: Int,
    failedCount: Int,
    backgroundFullRefreshLoading: Boolean,
    backgroundDueRefreshLoading: Boolean,
    visibleIncrementalRefreshLoading: Boolean,
    actionsUpdateNotificationLoading: Boolean,
    onDismissRequest: () -> Unit,
    onRunBackgroundFullRefresh: () -> Unit,
    onRunBackgroundDueRefresh: () -> Unit,
    onRefreshVisibleIncremental: () -> Unit,
    onRefreshFailedIncremental: () -> Unit,
    onSendActionsUpdateNotification: () -> Unit,
) {
    if (!show) return
    SnapshotWindowBottomSheet(
        show = true,
        title = stringResource(R.string.github_debug_sheet_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            AppLiquidIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest,
            )
        },
    ) {
        SheetContentColumn(verticalSpacing = 10.dp) {
            SheetDescriptionText(stringResource(R.string.github_debug_sheet_desc))
            SheetSectionTitle(stringResource(R.string.github_debug_sheet_section_refresh))
            SheetSectionCard {
                SheetControlRow(
                    label = stringResource(R.string.github_debug_sheet_label_background_full_refresh),
                    summary =
                        stringResource(
                            R.string.github_debug_sheet_summary_background_full_refresh,
                            trackedCount,
                        ),
                ) {
                    GitHubDebugActionButton(
                        backdrop = backdrop,
                        text =
                            if (backgroundFullRefreshLoading) {
                                stringResource(R.string.github_debug_sheet_action_running)
                            } else {
                                stringResource(R.string.github_debug_sheet_action_run)
                            },
                        loading = backgroundFullRefreshLoading,
                        onClick = onRunBackgroundFullRefresh,
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_debug_sheet_label_background_due_refresh),
                    summary = stringResource(R.string.github_debug_sheet_summary_background_due_refresh),
                ) {
                    GitHubDebugActionButton(
                        backdrop = backdrop,
                        text =
                            if (backgroundDueRefreshLoading) {
                                stringResource(R.string.github_debug_sheet_action_running)
                            } else {
                                stringResource(R.string.github_debug_sheet_action_run)
                            },
                        loading = backgroundDueRefreshLoading,
                        onClick = onRunBackgroundDueRefresh,
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_debug_sheet_label_visible_incremental_refresh),
                    summary =
                        stringResource(
                            R.string.github_debug_sheet_summary_visible_incremental_refresh,
                            visibleIncrementalTargetCount,
                            GitHubDebugVisibleRefreshDefaultLimit,
                        ),
                ) {
                    GitHubDebugActionButton(
                        backdrop = backdrop,
                        text =
                            if (visibleIncrementalRefreshLoading) {
                                stringResource(R.string.github_debug_sheet_action_running)
                            } else {
                                stringResource(R.string.github_debug_sheet_action_refresh)
                            },
                        loading = visibleIncrementalRefreshLoading,
                        onClick = onRefreshVisibleIncremental,
                    )
                }
                SheetControlRow(
                    label = stringResource(R.string.github_debug_sheet_label_failed_incremental_refresh),
                    summary =
                        stringResource(
                            R.string.github_debug_sheet_summary_failed_incremental_refresh,
                            failedCount,
                        ),
                ) {
                    GitHubDebugActionButton(
                        backdrop = backdrop,
                        text = stringResource(R.string.github_debug_sheet_action_refresh),
                        loading = false,
                        enabled = failedCount > 0,
                        onClick = onRefreshFailedIncremental,
                    )
                }
            }
            SheetSectionTitle(stringResource(R.string.github_debug_sheet_section_notification))
            SheetSectionCard {
                SheetControlRow(
                    label = stringResource(R.string.github_check_sheet_label_debug_actions_update_notification),
                    summary = stringResource(R.string.github_check_sheet_summary_debug_actions_update_notification),
                ) {
                    GitHubDebugActionButton(
                        backdrop = backdrop,
                        text =
                            if (actionsUpdateNotificationLoading) {
                                stringResource(R.string.github_check_sheet_action_debug_actions_update_notification_loading)
                            } else {
                                stringResource(R.string.github_check_sheet_action_debug_actions_update_notification)
                            },
                        loading = actionsUpdateNotificationLoading,
                        onClick = onSendActionsUpdateNotification,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.GitHubDebugActionButton(
    backdrop: LayerBackdrop,
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    AppLiquidTextButton(
        backdrop = backdrop,
        text = text,
        onClick = onClick,
        enabled = enabled && !loading,
        variant = GlassVariant.SheetAction,
        modifier = Modifier,
        textMaxLines = 1,
        textOverflow = TextOverflow.Ellipsis,
    )
}
