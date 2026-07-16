@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.page.GitHubTrackImportPreview
import os.kei.ui.page.main.widget.core.MiuixInfoItem
import os.kei.ui.page.main.widget.dialog.AppDialogDimensions
import os.kei.ui.page.main.widget.dialog.AppWindowDialogHost
import os.kei.ui.page.main.widget.glass.AppLiquidDialogActionButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Stable
internal class GitHubTrackDialogExitSnapshot<T : Any>(
    initialValue: T?,
) {
    var retainedValue: T? by mutableStateOf(initialValue)
        private set

    fun resolve(currentValue: T?): T? = currentValue ?: retainedValue

    fun retain(currentValue: T?) {
        if (currentValue != null) {
            retainedValue = currentValue
        }
    }

    fun clear() {
        retainedValue = null
    }
}

@Composable
internal fun <T : Any> rememberGitHubTrackDialogExitSnapshot(currentValue: T?): GitHubTrackDialogExitSnapshot<T> {
    val snapshot = remember { GitHubTrackDialogExitSnapshot(currentValue) }
    SideEffect { snapshot.retain(currentValue) }
    return snapshot
}

@Composable
internal fun GitHubDeleteTrackDialog(
    pendingDeleteItem: GitHubTrackedApp?,
    deleteInProgress: Boolean,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val exitSnapshot = rememberGitHubTrackDialogExitSnapshot(pendingDeleteItem)
    val renderedDeleteItem = exitSnapshot.resolve(pendingDeleteItem)
    AppWindowDialogHost(
        show = pendingDeleteItem != null,
        title = stringResource(R.string.github_delete_dialog_title),
        summary =
            renderedDeleteItem?.let {
                stringResource(
                    R.string.github_delete_dialog_summary,
                    it.appLabel,
                    it.owner,
                    it.repo,
                )
            },
        onDismissRequest = onDismissRequest,
        onDismissFinished = exitSnapshot::clear,
    ) {
        GitHubDeleteTrackDialogContent(
            deleteInProgress = deleteInProgress,
            actionsEnabled = pendingDeleteItem != null,
            onCancel = onCancel,
            onConfirmDelete = onConfirmDelete,
        )
    }
}

@Composable
internal fun GitHubDeleteTrackDialogContent(
    deleteInProgress: Boolean,
    actionsEnabled: Boolean = true,
    onCancel: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppLiquidDialogActionButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.common_cancel),
                enabled = actionsEnabled && !deleteInProgress,
                onClick = onCancel,
            )
            AppLiquidDialogActionButton(
                modifier = Modifier.weight(1f),
                text =
                    if (deleteInProgress) {
                        stringResource(R.string.github_delete_dialog_deleting)
                    } else {
                        stringResource(R.string.common_delete)
                    },
                containerColor = MiuixTheme.colorScheme.error,
                variant = GlassVariant.SheetDangerAction,
                enabled = actionsEnabled && !deleteInProgress,
                onClick = onConfirmDelete,
            )
        }
    }
}

@Composable
internal fun GitHubTrackImportDialog(
    preview: GitHubTrackImportPreview?,
    importInProgress: Boolean,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirmImport: () -> Unit,
) {
    val exitSnapshot = rememberGitHubTrackDialogExitSnapshot(preview)
    val renderedPreview = exitSnapshot.resolve(preview)
    AppWindowDialogHost(
        show = preview != null,
        title = stringResource(R.string.github_import_dialog_title),
        summary =
            renderedPreview?.let {
                stringResource(
                    if (it.canImport) {
                        R.string.github_import_dialog_summary_ready
                    } else {
                        R.string.github_import_dialog_summary_invalid
                    },
                )
            },
        onDismissRequest = onDismissRequest,
        onDismissFinished = exitSnapshot::clear,
        maxWidth = AppDialogDimensions.ContentRichMaxWidth,
    ) {
        renderedPreview?.let {
            GitHubTrackImportDialogContent(
                preview = it,
                importInProgress = importInProgress,
                actionsEnabled = preview != null,
                onDismissRequest = onDismissRequest,
                onCancel = onCancel,
                onConfirmImport = onConfirmImport,
            )
        }
    }
}

@Composable
internal fun GitHubTrackImportDialogContent(
    preview: GitHubTrackImportPreview,
    importInProgress: Boolean,
    actionsEnabled: Boolean = true,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirmImport: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        SheetSectionCard(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalSpacing = 6.dp,
        ) {
            MiuixInfoItem(
                key = stringResource(R.string.github_import_dialog_label_file_items),
                value =
                    stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.fileItemCount,
                    ),
            )
            MiuixInfoItem(
                key = stringResource(R.string.github_import_dialog_label_valid_items),
                value =
                    stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.validCount,
                    ),
                valueColor = GitHubStatusPalette.Active,
            )
            MiuixInfoItem(
                key = stringResource(R.string.github_import_dialog_label_duplicate_items),
                value =
                    stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.duplicateCount,
                    ),
                valueColor = GitHubStatusPalette.PreRelease,
            )
            MiuixInfoItem(
                key = stringResource(R.string.github_import_dialog_label_invalid_items),
                value =
                    stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.invalidCount,
                    ),
                valueColor = GitHubStatusPalette.Error,
            )
            MiuixInfoItem(
                key = stringResource(R.string.github_import_dialog_label_new_items),
                value =
                    stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.newCount,
                    ),
                valueColor = GitHubStatusPalette.Update,
            )
            MiuixInfoItem(
                key = stringResource(R.string.github_import_dialog_label_updated_items),
                value =
                    stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.updatedCount,
                    ),
                valueColor = GitHubStatusPalette.Active,
            )
            MiuixInfoItem(
                key = stringResource(R.string.github_import_dialog_label_unchanged_items),
                value =
                    stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.unchangedCount,
                    ),
                valueColor = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            MiuixInfoItem(
                key = stringResource(R.string.github_import_dialog_label_merged_items),
                value =
                    stringResource(
                        R.string.github_check_sheet_value_track_count,
                        preview.mergedCount,
                    ),
                valueColor = GitHubStatusPalette.Stable,
            )
            if (preview.hasSourceBreakdown) {
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_github_items),
                    value =
                        stringResource(
                            R.string.github_check_sheet_value_track_count,
                            preview.githubRepositoryCount,
                        ),
                    valueColor = GitHubStatusPalette.Cache,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_git_items),
                    value =
                        stringResource(
                            R.string.github_check_sheet_value_track_count,
                            preview.gitRepositoryCount,
                        ),
                    valueColor = GitHubStatusPalette.PreRelease,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_direct_apk_items),
                    value =
                        stringResource(
                            R.string.github_check_sheet_value_track_count,
                            preview.directApkCount,
                        ),
                    valueColor = GitHubStatusPalette.Active,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_fdroid_items),
                    value =
                        stringResource(
                            R.string.github_check_sheet_value_track_count,
                            preview.fdroidRepositoryCount,
                        ),
                    valueColor = GitHubStatusPalette.Install,
                )
            }
        }
        if (preview.hasImportedProjectOptions) {
            SheetSectionCard(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalSpacing = 6.dp,
            ) {
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_project_options),
                    value =
                        stringResource(
                            R.string.github_check_sheet_value_track_count,
                            preview.importedProjectOptionCount,
                        ),
                    valueColor = GitHubStatusPalette.Cache,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_pre_release_items),
                    value =
                        stringResource(
                            R.string.github_check_sheet_value_track_count,
                            preview.preferPreReleaseCount,
                        ),
                    valueColor = GitHubStatusPalette.PreRelease,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_actions_items),
                    value =
                        stringResource(
                            R.string.github_check_sheet_value_track_count,
                            preview.actionsUpdateCount,
                        ),
                    valueColor = GitHubStatusPalette.Cache,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_update_interval_items),
                    value =
                        stringResource(
                            R.string.github_check_sheet_value_track_count,
                            preview.updateIntervalOverrideCount,
                        ),
                    valueColor = GitHubStatusPalette.Stable,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_precise_items),
                    value =
                        stringResource(
                            R.string.github_check_sheet_value_track_count,
                            preview.preciseApkVersionOverrideCount,
                        ),
                    valueColor = GitHubStatusPalette.Active,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.github_import_dialog_label_download_items),
                    value =
                        stringResource(
                            R.string.github_check_sheet_value_track_count,
                            preview.latestReleaseDownloadCount,
                        ),
                    valueColor = GitHubStatusPalette.Update,
                )
            }
        }
        GitHubTrackImportDialogActions(
            canImport = preview.canImport,
            importInProgress = importInProgress,
            actionsEnabled = actionsEnabled,
            onDismissRequest = onDismissRequest,
            onCancel = onCancel,
            onConfirmImport = onConfirmImport,
        )
    }
}

@Composable
internal fun GitHubTrackImportDialogActions(
    canImport: Boolean,
    importInProgress: Boolean,
    actionsEnabled: Boolean = true,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirmImport: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppLiquidDialogActionButton(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.common_cancel),
            onClick = onCancel,
            enabled = actionsEnabled && !importInProgress,
        )
        AppLiquidDialogActionButton(
            modifier = Modifier.weight(1f),
            text =
                when {
                    importInProgress -> stringResource(R.string.github_check_sheet_action_importing)
                    canImport -> stringResource(R.string.github_import_dialog_action_confirm)
                    else -> stringResource(R.string.common_close)
                },
            containerColor = if (canImport) GitHubStatusPalette.Active else null,
            onClick = if (canImport) onConfirmImport else onDismissRequest,
            enabled = actionsEnabled && !importInProgress,
        )
    }
}
