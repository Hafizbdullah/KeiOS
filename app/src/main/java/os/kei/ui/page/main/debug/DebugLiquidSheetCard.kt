@file:Suppress("FunctionName")

package os.kei.ui.page.main.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideLayersIcon
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppLiquidInputField
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.sheet.LiquidSheetInitialDetent
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugLiquidSheetCard(
    accent: Color,
    backdrop: Backdrop,
) {
    var show by remember { mutableStateOf(false) }
    var allowDismiss by remember { mutableStateOf(true) }
    var initialDetent by remember { mutableStateOf(LiquidSheetInitialDetent.ThreeQuarter) }
    var blockedDismissCount by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    val contentColor = MiuixTheme.colorScheme.onBackground

    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_sheet_title),
        subtitle = stringResource(R.string.debug_component_lab_liquid_sheet_subtitle),
        backdrop = backdrop,
        exportBackdropToContent = true,
        sectionIcon = appLucideLayersIcon(),
        titleColor = accent,
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap,
    ) {
        val cardBackdrop = LocalLiquidParentBackdrop.current ?: backdrop
        SheetControlRow(label = stringResource(R.string.debug_component_lab_liquid_sheet_allow_dismiss)) {
            AppSwitch(checked = allowDismiss, onCheckedChange = { allowDismiss = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LiquidSheetInitialDetent.entries.forEach { detent ->
                AppLiquidTextButton(
                    backdrop = cardBackdrop,
                    text = detent.debugLabel(),
                    onClick = { initialDetent = detent },
                    modifier = Modifier.weight(1f),
                    variant = if (detent == initialDetent) GlassVariant.SheetPrimaryAction else GlassVariant.Compact,
                    minHeight = 42.dp,
                    horizontalPadding = 4.dp,
                    textColor = contentColor,
                )
            }
        }
        AppLiquidTextButton(
            backdrop = cardBackdrop,
            text = stringResource(R.string.debug_component_lab_liquid_sheet_open),
            onClick = { show = true },
            modifier = Modifier.fillMaxWidth(),
            variant = GlassVariant.SheetPrimaryAction,
            textColor = contentColor,
        )
        Text(
            text = stringResource(R.string.debug_component_lab_liquid_sheet_blocked_count, blockedDismissCount),
            color = contentColor,
        )
    }

    SnapshotWindowBottomSheet(
        show = show,
        title =
            stringResource(
                R.string.debug_component_lab_liquid_sheet_window_title,
                blockedDismissCount,
            ),
        onDismissRequest = { show = false },
        allowDismiss = allowDismiss,
        onBlockedDismissRequest = { blockedDismissCount++ },
        initialDetent = initialDetent,
        preferExportedBackdrop = true,
        useLiquidGlassSheet = true,
    ) {
        SheetContentColumn(verticalSpacing = 12.dp) {
            SheetSectionTitle(stringResource(R.string.debug_component_lab_liquid_sheet_ime_section))
            SheetSectionCard {
                val fieldDescription = stringResource(R.string.debug_component_lab_liquid_sheet_input)
                AppLiquidInputField(
                    value = input,
                    onValueChange = { input = it },
                    label = fieldDescription,
                    backdrop = LocalLiquidParentBackdrop.current,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = fieldDescription },
                    singleLine = true,
                    variant = GlassVariant.SheetInput,
                    minHeight = 48.dp,
                    cornerRadius = 14.dp,
                    horizontalPadding = 14.dp,
                    verticalPadding = 14.dp,
                )
                SheetDescriptionText(stringResource(R.string.debug_component_lab_liquid_sheet_ime_desc))
            }
            SheetSectionTitle(stringResource(R.string.debug_component_lab_liquid_sheet_scroll_section))
            repeat(12) { index ->
                SheetSectionCard {
                    Text(
                        text = stringResource(R.string.debug_component_lab_liquid_sheet_row, index + 1),
                        color = contentColor,
                    )
                    SheetDescriptionText(stringResource(R.string.debug_component_lab_liquid_sheet_row_desc))
                }
            }
            AppStandaloneLiquidTextButton(
                text = stringResource(R.string.common_close),
                onClick = { show = false },
                modifier = Modifier.fillMaxWidth().widthIn(min = 48.dp),
                surfaceModifier = Modifier.fillMaxWidth(),
                variant = GlassVariant.SheetPrimaryAction,
                textColor = contentColor,
                pressSafePadding = 0.dp,
            )
        }
    }
}

@Composable
private fun LiquidSheetInitialDetent.debugLabel(): String =
    when (this) {
        LiquidSheetInitialDetent.OneThird -> stringResource(R.string.debug_component_lab_liquid_sheet_detent_third)
        LiquidSheetInitialDetent.Half -> stringResource(R.string.debug_component_lab_liquid_sheet_detent_half)
        LiquidSheetInitialDetent.ThreeQuarter -> stringResource(R.string.debug_component_lab_liquid_sheet_detent_three_quarter)
        LiquidSheetInitialDetent.Full -> stringResource(R.string.debug_component_lab_liquid_sheet_detent_full)
    }
