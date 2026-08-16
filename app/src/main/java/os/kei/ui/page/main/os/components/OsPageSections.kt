@file:Suppress("FunctionName")

package os.kei.ui.page.main.os.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.OsSectionCard
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.osActivityShortcutIconKey
import os.kei.ui.page.main.os.osLucideEnterIcon
import os.kei.ui.page.main.os.shortcut.OsActivityShortcutCard
import os.kei.ui.page.main.os.shortcut.ShortcutActivityIcon
import os.kei.ui.page.main.os.titleText
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidSearchField
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionHeader
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OsCardVisibilityManagerSheet(
    show: Boolean,
    title: String,
    sheetBackdrop: Backdrop,
    cardsHintText: String,
    onDismissRequest: () -> Unit,
    isCardVisible: (OsSectionCard) -> Boolean,
    onCardVisibilityChange: (OsSectionCard, Boolean) -> Unit,
) {
    SnapshotWindowBottomSheet(
        show = show,
        preferExportedBackdrop = true,
        title = title,
        onDismissRequest = onDismissRequest,
        startAction = {
            AppLiquidIconButton(
                backdrop = sheetBackdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest,
            )
        },
    ) {
        SheetContentColumn(
            verticalSpacing = 14.dp,
        ) {
            @Composable
            fun CardLabel(
                card: OsSectionCard,
                modifier: Modifier = Modifier,
            ) {
                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val iconModifier =
                        Modifier
                            .size(18.dp)
                            .defaultMinSize(minHeight = 18.dp)
                    val icon = sectionCardIcon(card)
                    val title = card.titleText()
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MiuixTheme.colorScheme.onBackground,
                        modifier = iconModifier,
                    )
                    Text(text = title, color = MiuixTheme.colorScheme.onBackground)
                }
            }

            SheetSectionHeader(
                text = stringResource(R.string.os_visibility_group_cards),
                summary = cardsHintText,
            )
            SheetSectionCard(verticalSpacing = 10.dp) {
                OsSectionCard.entries
                    .filter { card ->
                        card != OsSectionCard.GOOGLE_SYSTEM_SERVICE &&
                            card != OsSectionCard.SHELL_RUNNER
                    }.forEach { card ->
                        SheetControlRow(
                            labelContent = {
                                CardLabel(card = card, modifier = Modifier.defaultMinSize(minHeight = 24.dp))
                            },
                        ) {
                            AppSwitch(
                                checked = isCardVisible(card),
                                onCheckedChange = { checked -> onCardVisibilityChange(card, checked) },
                            )
                        }
                    }
            }

        }
    }
}

@Composable
internal fun OsActivityVisibilityManagerSheet(
    show: Boolean,
    title: String,
    sheetBackdrop: Backdrop,
    activityHintText: String,
    cards: List<OsActivityShortcutCard>,
    activityIconBitmaps: Map<String, Bitmap>,
    packageIconBitmaps: Map<String, Bitmap>,
    defaultCardTitle: String,
    transferInProgress: Boolean,
    onExportAllCards: () -> Unit,
    onImportAllCards: () -> Unit,
    onDismissRequest: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onCardVisibilityChange: (String, Boolean) -> Unit,
) {
    SnapshotWindowBottomSheet(
        show = show,
        preferExportedBackdrop = true,
        title = title,
        onDismissRequest = onDismissRequest,
        startAction = {
            AppLiquidIconButton(
                backdrop = sheetBackdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest,
            )
        },
    ) {
        SheetContentColumn(
            verticalSpacing = 14.dp,
        ) {
            val presentationState =
                remember(cards, defaultCardTitle, query) {
                    deriveOsActivityVisibilityPresentationState(
                        cards = cards,
                        defaultCardTitle = defaultCardTitle,
                        query = query,
                    )
                }
            SheetSectionHeader(
                text = stringResource(R.string.os_action_manage_activities),
                summary = activityHintText,
            )
            SheetSectionCard(verticalSpacing = 8.dp) {
                AppLiquidSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = stringResource(R.string.os_visibility_search_activity_label),
                    backdrop = sheetBackdrop,
                    modifier = Modifier.fillMaxWidth(),
                    variant = GlassVariant.SheetInput,
                    textColor = MiuixTheme.colorScheme.primary,
                )
            }
            ActivityVisibilityGroup(
                title = stringResource(R.string.os_visibility_group_built_in),
                items = presentationState.builtInItems,
                activityIconBitmaps = activityIconBitmaps,
                packageIconBitmaps = packageIconBitmaps,
                emptySearchActive = presentationState.emptySearchActive,
                noMatchedResultsText = stringResource(R.string.common_no_matched_results),
                onCardVisibilityChange = onCardVisibilityChange,
            )
            ActivityVisibilityGroup(
                title = stringResource(R.string.os_visibility_group_custom),
                items = presentationState.customItems,
                activityIconBitmaps = activityIconBitmaps,
                packageIconBitmaps = packageIconBitmaps,
                emptySearchActive = false,
                noMatchedResultsText = stringResource(R.string.common_no_matched_results),
                onCardVisibilityChange = onCardVisibilityChange,
            )
            SheetSectionHeader(
                text = stringResource(R.string.os_activity_sheet_transfer_title),
                summary = stringResource(R.string.os_activity_sheet_transfer_desc),
            )
            SheetSectionCard(verticalSpacing = 8.dp) {
                AppDualActionRow(
                    spacing = 8.dp,
                    first = { modifier ->
                        AppLiquidTextButton(
                            backdrop = sheetBackdrop,
                            text = stringResource(R.string.os_activity_sheet_action_export_backup),
                            onClick = onExportAllCards,
                            modifier = modifier,
                            enabled = !transferInProgress,
                            variant = GlassVariant.SheetAction,
                            pressOverlayEnabled = true,
                        )
                    },
                    second = { modifier ->
                        AppLiquidTextButton(
                            backdrop = sheetBackdrop,
                            text = stringResource(R.string.os_activity_sheet_action_import_backup),
                            onClick = onImportAllCards,
                            modifier = modifier,
                            enabled = !transferInProgress,
                            variant = GlassVariant.SheetAction,
                            pressOverlayEnabled = true,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ActivityVisibilityGroup(
    title: String,
    items: List<OsActivityVisibilityItem>,
    activityIconBitmaps: Map<String, Bitmap>,
    packageIconBitmaps: Map<String, Bitmap>,
    emptySearchActive: Boolean,
    noMatchedResultsText: String,
    onCardVisibilityChange: (String, Boolean) -> Unit,
) {
    if (items.isEmpty() && !emptySearchActive) return
    SheetSectionHeader(
        text = visibilityGroupTitle(title, items.size),
    )
    SheetSectionCard(verticalSpacing = 10.dp) {
        if (items.isEmpty()) {
            Text(
                text = noMatchedResultsText,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            return@SheetSectionCard
        }
        items.forEach { item ->
            ActivityVisibilityRow(
                item = item,
                activityIconBitmaps = activityIconBitmaps,
                packageIconBitmaps = packageIconBitmaps,
                onCardVisibilityChange = onCardVisibilityChange,
            )
        }
    }
}

@Composable
private fun ActivityVisibilityRow(
    item: OsActivityVisibilityItem,
    activityIconBitmaps: Map<String, Bitmap>,
    packageIconBitmaps: Map<String, Bitmap>,
    onCardVisibilityChange: (String, Boolean) -> Unit,
) {
    SheetControlRow(
        labelContent = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.packageName.isNotBlank() || item.className.isNotBlank()) {
                    val iconKey =
                        osActivityShortcutIconKey(
                            packageName = item.packageName,
                            className = item.className,
                        )
                    ShortcutActivityIcon(
                        packageName = item.packageName,
                        className = item.className,
                        size = 18.dp,
                        bitmap = activityIconBitmaps[iconKey],
                        packageBitmap = packageIconBitmaps[item.packageName.trim()],
                    )
                } else {
                    Icon(
                        imageVector = osLucideEnterIcon(),
                        contentDescription = item.title,
                        tint = MiuixTheme.colorScheme.onBackground,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .defaultMinSize(minHeight = 18.dp),
                    )
                }
                Text(
                    text = item.title,
                    color = MiuixTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (item.builtInSample) {
                    StatusPill(
                        label = stringResource(R.string.os_activity_card_builtin_badge),
                        color = Color(0xFF3B82F6),
                        size = AppStatusPillSize.Compact,
                    )
                }
            }
        },
    ) {
        AppSwitch(
            checked = item.visible,
            onCheckedChange = { checked ->
                onCardVisibilityChange(item.id, checked)
            },
        )
    }
}

@Composable
internal fun visibilityGroupTitle(
    title: String,
    count: Int,
): String =
    stringResource(
        R.string.os_visibility_group_title_count,
        title,
        stringResource(R.string.common_item_count, count),
    )
