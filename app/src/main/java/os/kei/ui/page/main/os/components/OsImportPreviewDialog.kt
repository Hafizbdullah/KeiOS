@file:Suppress("FunctionName")

package os.kei.ui.page.main.os.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.state.OsCardImportTarget
import os.kei.ui.page.main.os.transfer.OsCardImportFileKind
import os.kei.ui.page.main.os.transfer.OsCardImportPreview
import os.kei.ui.page.main.widget.core.MiuixInfoItem
import os.kei.ui.page.main.widget.dialog.AppDialogDimensions
import os.kei.ui.page.main.widget.dialog.AppWindowDialogHost
import os.kei.ui.page.main.widget.glass.AppLiquidDialogActionButton
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val PreviewValidColor = Color(0xFF16A34A)
private val PreviewDuplicateColor = Color(0xFFD97706)
private val PreviewUpdateColor = Color(0xFF0EA5E9)
private val ImportPreviewStatisticsMaxHeight = 360.dp
internal const val OS_IMPORT_PREVIEW_STATISTICS_TEST_TAG = "os_import_preview_statistics"

@Immutable
internal data class OsImportDialogSnapshot(
    val preview: OsCardImportPreview,
    val importInProgress: Boolean,
)

@Stable
internal class OsImportDialogExitSnapshot(
    initialValue: OsImportDialogSnapshot?,
) {
    var retainedValue: OsImportDialogSnapshot? by mutableStateOf(initialValue)
        private set

    fun resolve(currentValue: OsImportDialogSnapshot?): OsImportDialogSnapshot? = currentValue ?: retainedValue

    fun retain(currentValue: OsImportDialogSnapshot?) {
        if (currentValue != null) {
            retainedValue = currentValue
        }
    }

    fun clear() {
        retainedValue = null
    }
}

@Composable
internal fun rememberOsImportDialogExitSnapshot(currentValue: OsImportDialogSnapshot?): OsImportDialogExitSnapshot {
    val snapshot = remember { OsImportDialogExitSnapshot(currentValue) }
    SideEffect { snapshot.retain(currentValue) }
    return snapshot
}

@Composable
internal fun OsImportPreviewDialog(
    preview: OsCardImportPreview?,
    importInProgress: Boolean,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirmImport: () -> Unit,
) {
    val currentSnapshot =
        remember(preview, importInProgress) {
            preview?.let {
                OsImportDialogSnapshot(
                    preview = it,
                    importInProgress = importInProgress,
                )
            }
        }
    val exitSnapshot = rememberOsImportDialogExitSnapshot(currentSnapshot)
    val renderedSnapshot = exitSnapshot.resolve(currentSnapshot)

    AppWindowDialogHost(
        show = preview != null,
        title = stringResource(R.string.os_import_dialog_title),
        summary = renderedSnapshot?.let { importPreviewSummary(it.preview) },
        onDismissRequest = onDismissRequest,
        onDismissFinished = exitSnapshot::clear,
        maxWidth = AppDialogDimensions.ContentRichMaxWidth,
    ) {
        renderedSnapshot?.let { snapshot ->
            OsImportPreviewDialogBody(
                snapshot = snapshot,
                actionsEnabled = preview != null,
                onDismissRequest = onDismissRequest,
                onCancel = onCancel,
                onConfirmImport = onConfirmImport,
            )
        }
    }
}

@Composable
internal fun OsImportPreviewDialogBody(
    snapshot: OsImportDialogSnapshot,
    actionsEnabled: Boolean = true,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
    onConfirmImport: () -> Unit,
) {
    val preview = snapshot.preview
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        SheetSectionCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = ImportPreviewStatisticsMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .testTag(OS_IMPORT_PREVIEW_STATISTICS_TEST_TAG),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalSpacing = 6.dp,
        ) {
            MiuixInfoItem(
                key = stringResource(R.string.os_import_dialog_label_detected_type),
                value = fileKindLabel(preview.payload.fileKind),
            )
            MiuixInfoItem(
                key = stringResource(R.string.os_import_dialog_label_backup_format),
                value =
                    if (preview.payload.isLegacyFormat) {
                        stringResource(R.string.os_import_dialog_value_format_legacy)
                    } else {
                        stringResource(R.string.os_import_dialog_value_format_current)
                    },
            )
            MiuixInfoItem(
                key = stringResource(R.string.os_import_dialog_label_file_items),
                value = preview.fileItemCount.toString(),
            )
            MiuixInfoItem(
                key = stringResource(R.string.os_import_dialog_label_valid_items),
                value = preview.validCount.toString(),
                valueColor = PreviewValidColor,
            )
            MiuixInfoItem(
                key = stringResource(R.string.os_import_dialog_label_duplicate_items),
                value = preview.duplicateCount.toString(),
                valueColor = PreviewDuplicateColor,
            )
            MiuixInfoItem(
                key = stringResource(R.string.os_import_dialog_label_invalid_items),
                value = preview.invalidCount.toString(),
                valueColor = MiuixTheme.colorScheme.error,
            )
            if (preview.canImport) {
                MiuixInfoItem(
                    key = stringResource(R.string.os_import_dialog_label_new_items),
                    value = preview.newCount.toString(),
                    valueColor = PreviewValidColor,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.os_import_dialog_label_updated_items),
                    value = preview.updatedCount.toString(),
                    valueColor = PreviewUpdateColor,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.os_import_dialog_label_unchanged_items),
                    value = preview.unchangedCount.toString(),
                    valueColor = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                MiuixInfoItem(
                    key = stringResource(R.string.os_import_dialog_label_merged_items),
                    value = preview.mergedCount.toString(),
                    valueColor = PreviewValidColor,
                )
            }
        }
        OsImportPreviewDialogActions(
            preview = preview,
            importInProgress = snapshot.importInProgress,
            actionsEnabled = actionsEnabled,
            onDismissRequest = onDismissRequest,
            onCancel = onCancel,
            onConfirmImport = onConfirmImport,
        )
    }
}

@Composable
internal fun OsImportPreviewDialogActions(
    preview: OsCardImportPreview,
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
                    importInProgress -> stringResource(R.string.os_import_dialog_action_importing)
                    preview.canImport -> stringResource(R.string.os_import_dialog_action_confirm)
                    else -> stringResource(R.string.common_close)
                },
            containerColor = if (preview.canImport) PreviewValidColor else null,
            onClick = if (preview.canImport) onConfirmImport else onDismissRequest,
            enabled = actionsEnabled && !importInProgress,
        )
    }
}

@Composable
private fun importPreviewSummary(preview: OsCardImportPreview): String =
    when {
        preview.canImport && preview.payload.isLegacyFormat -> {
            stringResource(R.string.os_import_dialog_summary_legacy)
        }

        preview.canImport -> {
            stringResource(R.string.os_import_dialog_summary_ready)
        }

        preview.isWrongTarget -> {
            stringResource(
                R.string.os_import_dialog_summary_wrong_target,
                fileKindLabel(preview.payload.fileKind),
                targetLabel(preview.target),
            )
        }

        else -> {
            stringResource(R.string.os_import_dialog_summary_invalid)
        }
    }

@Composable
private fun fileKindLabel(fileKind: OsCardImportFileKind): String =
    when (fileKind) {
        OsCardImportFileKind.Activity -> stringResource(R.string.os_import_dialog_target_activity)
        OsCardImportFileKind.Shell -> stringResource(R.string.os_import_dialog_target_shell)
        OsCardImportFileKind.Unknown -> stringResource(R.string.os_import_dialog_value_detected_type_unknown)
    }

@Composable
private fun targetLabel(target: OsCardImportTarget): String =
    when (target) {
        OsCardImportTarget.Activity -> stringResource(R.string.os_import_dialog_target_activity)
        OsCardImportTarget.Shell -> stringResource(R.string.os_import_dialog_target_shell)
    }
