@file:Suppress("FunctionName")

package os.kei.ui.page.main.ba

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.ba.support.BA_AP_LIMIT_MAX
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidSearchField
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionHeader
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.AppStatusColors
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaApLimitToolsSheet(
    show: Boolean,
    backdrop: Backdrop?,
    apLimitInput: String,
    onApLimitInputChange: (String) -> Unit,
    onSaveApLimit: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val accentGreen = AppStatusColors.Fresh
    SnapshotWindowBottomSheet(
        show = show,
        title = stringResource(R.string.ba_ap_limit_tools_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            AppLiquidIconButton(
                backdrop = backdrop,
                icon = MiuixIcons.Regular.Close,
                contentDescription = stringResource(R.string.common_close),
                variant = GlassVariant.Bar,
                onClick = onDismissRequest,
            )
        },
    ) {
        SheetContentColumn(verticalSpacing = 14.dp) {
            SheetSectionHeader(
                text = stringResource(R.string.ba_ap_limit_tools_section_title),
                summary =
                    stringResource(
                        R.string.ba_ap_limit_tools_summary,
                        BA_AP_LIMIT_MAX,
                    ),
            )
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetControlRow(label = stringResource(R.string.ba_ap_limit_tools_limit_label)) {
                    AppLiquidSearchField(
                        modifier = Modifier.width(116.dp),
                        value = apLimitInput,
                        onValueChange = { input ->
                            onApLimitInputChange(normalizeApLimitInput(input))
                        },
                        onImeActionDone = onSaveApLimit,
                        label = BA_AP_LIMIT_MAX.toString(),
                        backdrop = backdrop,
                        variant = GlassVariant.SheetInput,
                        singleLine = true,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        textColor = accentGreen,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                    )
                }
            }
            AppDualActionRow(
                spacing = 8.dp,
                first = { modifier ->
                    AppLiquidTextButton(
                        modifier = modifier,
                        backdrop = backdrop,
                        text = stringResource(R.string.ba_ap_limit_tools_set_max),
                        textColor = MiuixTheme.colorScheme.onBackgroundVariant,
                        containerColor = MiuixTheme.colorScheme.onBackgroundVariant,
                        variant = GlassVariant.SheetAction,
                        textMaxLines = 1,
                        textOverflow = TextOverflow.Ellipsis,
                        pressOverlayEnabled = true,
                        onClick = {
                            onApLimitInputChange(BA_AP_LIMIT_MAX.toString())
                        },
                    )
                },
                second = { modifier ->
                    AppLiquidTextButton(
                        modifier = modifier,
                        backdrop = backdrop,
                        text = stringResource(R.string.common_save),
                        textColor = accentGreen,
                        containerColor = accentGreen,
                        variant = GlassVariant.SheetAction,
                        textMaxLines = 1,
                        textOverflow = TextOverflow.Ellipsis,
                        pressOverlayEnabled = true,
                        onClick = onSaveApLimit,
                    )
                },
            )
        }
    }
}

private fun normalizeApLimitInput(input: String): String =
    input
        .filter(Char::isDigit)
        .take(3)
