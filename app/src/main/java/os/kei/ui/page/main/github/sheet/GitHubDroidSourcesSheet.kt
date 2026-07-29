@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideConfirmIcon
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionHeader
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.sheet.UnsavedSheetDismissConfirmDialog
import os.kei.ui.page.main.widget.sheet.rememberUnsavedSheetDismissHandler

@Composable
internal fun GitHubDroidSourcesSheet(
    show: Boolean,
    backdrop: LayerBackdrop,
    lookupConfig: GitHubLookupConfig,
    selectedRepoIds: List<String>,
    onDismissRequest: () -> Unit,
    onApply: () -> Unit,
    onRepoEnabledChange: (String, Boolean) -> Unit,
) {
    val savedRepoIds = lookupConfig.normalizedFdroidCommonRepoIds
    val selectedNormalizedRepoIds =
        remember(selectedRepoIds) {
            FdroidRepositoryPresets.normalizedCommonSearchRepoIds(selectedRepoIds)
        }
    val draftChanged = selectedNormalizedRepoIds != savedRepoIds
    val dismissHandler =
        rememberUnsavedSheetDismissHandler(
            hasUnsavedChanges = draftChanged,
            onDismissRequest = onDismissRequest,
        )

    SnapshotWindowBottomSheet(
        show = show,
        preferExportedBackdrop = true,
        title = stringResource(R.string.github_droid_sources_sheet_title),
        onDismissRequest = dismissHandler.requestDismiss,
        allowDismiss = dismissHandler.allowDismiss,
        onBlockedDismissRequest = dismissHandler.requestDismiss,
        startAction = {
            AppLiquidIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = dismissHandler.requestDismiss,
            )
        },
        endAction = {
            AppLiquidIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideConfirmIcon(),
                contentDescription = stringResource(R.string.github_droid_sources_sheet_cd_save),
                onClick = onApply,
            )
        },
    ) {
        SheetContentColumn(verticalSpacing = 14.dp) {
            SheetSectionHeader(stringResource(R.string.github_droid_sources_section_common))
            SheetSectionCard {
                SheetDescriptionText(
                    text =
                        stringResource(
                            R.string.github_droid_sources_summary_common,
                            selectedNormalizedRepoIds.size,
                        ),
                )
                FdroidRepositoryPresets.commonSearchRepos.forEach { preset ->
                    SheetControlRow(
                        label = preset.displayName,
                        summary =
                            stringResource(
                                R.string.github_droid_sources_repo_summary_format,
                                preset.repoUrl,
                            ),
                    ) {
                        AppSwitch(
                            checked = preset.id in selectedNormalizedRepoIds,
                            onCheckedChange = { enabled ->
                                onRepoEnabledChange(preset.id, enabled)
                            },
                        )
                    }
                }
            }
            SheetDescriptionText(
                text = stringResource(R.string.github_droid_sources_summary_scope),
            )
        }
    }

    UnsavedSheetDismissConfirmDialog(
        show = dismissHandler.showConfirmDialog,
        onKeepEditing = dismissHandler.keepEditing,
        onDiscardChanges = dismissHandler.discardChanges,
    )
}
