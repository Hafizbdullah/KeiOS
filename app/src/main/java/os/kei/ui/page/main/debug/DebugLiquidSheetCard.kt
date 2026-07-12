@file:Suppress("FunctionName")

package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideLayersIcon
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassVariant
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
internal fun DebugLiquidSheetCard(accent: Color) {
    var show by remember { mutableStateOf(false) }
    var allowDismiss by remember { mutableStateOf(true) }
    var initialDetent by remember { mutableStateOf(LiquidSheetInitialDetent.ThreeQuarter) }
    var blockedDismissCount by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    val contentColor = MiuixTheme.colorScheme.onBackground

    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_sheet_title),
        subtitle = stringResource(R.string.debug_component_lab_liquid_sheet_subtitle),
        sectionIcon = appLucideLayersIcon(),
        titleColor = accent,
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap,
    ) {
        SheetControlRow(label = stringResource(R.string.debug_component_lab_liquid_sheet_allow_dismiss)) {
            AppSwitch(checked = allowDismiss, onCheckedChange = { allowDismiss = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LiquidSheetInitialDetent.entries.forEach { detent ->
                AppLiquidTextButton(
                    backdrop = null,
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
            backdrop = null,
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
        useLiquidGlassSheet = true,
    ) {
        SheetContentColumn(verticalSpacing = 12.dp) {
            SheetSectionTitle(stringResource(R.string.debug_component_lab_liquid_sheet_ime_section))
            SheetSectionCard {
                val fieldDescription = stringResource(R.string.debug_component_lab_liquid_sheet_input)
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = fieldDescription },
                    textStyle = TextStyle(color = contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                                        RoundedCornerShape(14.dp),
                                    ).padding(horizontal = 14.dp, vertical = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (input.isEmpty()) {
                                Text(
                                    text = fieldDescription,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
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
            AppLiquidTextButton(
                backdrop = null,
                text = stringResource(R.string.common_close),
                onClick = { show = false },
                modifier = Modifier.fillMaxWidth().widthIn(min = 48.dp),
                variant = GlassVariant.SheetPrimaryAction,
                textColor = contentColor,
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
