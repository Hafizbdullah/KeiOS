@file:Suppress("FunctionName")

package os.kei.ui.page.main.ba

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.ba.support.cafeDailyCapacity
import os.kei.ui.page.main.ba.support.cafeHourlyGain
import os.kei.ui.page.main.ba.support.calculateCafeFullAtMs
import os.kei.ui.page.main.ba.support.formatBaRemainingTime
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale
import kotlin.math.floor

@Composable
internal fun BaCafeApToolsSheet(
    show: Boolean,
    backdrop: Backdrop?,
    cafeLevel: Int,
    cafeStoredAp: Double,
    cafeLastHourMs: Long,
    uiMinuteMs: Long,
    onClearCafeStoredAp: () -> Unit,
    onFillCafeStoredAp: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val accentPink = Color(0xFFF472B6)
    val countdownBlue = Color(0xFF60A5FA)
    val cafeCap = cafeDailyCapacity(cafeLevel)
    val hourlyGain = cafeHourlyGain(cafeLevel)
    val cafeFullAt =
        calculateCafeFullAtMs(
            cafeLevel = cafeLevel,
            cafeStoredAp = cafeStoredAp,
            cafeLastHourMs = cafeLastHourMs,
            nowMs = uiMinuteMs,
        )
    val hourlyText = stringResource(R.string.ba_cafe_ap_hourly_gain_format, hourlyGain)
    val fullText =
        if (cafeStoredAp >= cafeCap.toDouble()) {
            stringResource(R.string.ba_cafe_ap_full_now)
        } else {
            stringResource(
                R.string.ba_cafe_ap_full_remaining_format,
                formatBaRemainingTime(cafeFullAt, uiMinuteMs),
            )
        }
    val currentValueText =
        stringResource(
            R.string.ba_cafe_ap_tools_current_value,
            formatCafeApPrecise(cafeStoredAp),
            cafeCap,
        )

    SnapshotWindowBottomSheet(
        show = show,
        title = stringResource(R.string.ba_cafe_ap_tools_title),
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
        SheetContentColumn(verticalSpacing = 10.dp) {
            SheetSectionCard(verticalSpacing = 10.dp) {
                Text(
                    text = stringResource(R.string.ba_cafe_ap_tools_current_title),
                    color = accentPink,
                    fontWeight = FontWeight.Bold,
                )
                SheetDescriptionText(stringResource(R.string.ba_cafe_ap_tools_summary))
                SheetControlRow(label = stringResource(R.string.ba_cafe_ap_tools_current_label)) {
                    BaCafeApToolsValue(
                        text = currentValueText,
                        color = accentPink,
                    )
                }
                SheetControlRow(label = stringResource(R.string.ba_cafe_ap_tools_hourly_label)) {
                    BaCafeApToolsValue(
                        text = hourlyText,
                        color = accentPink,
                    )
                }
                SheetControlRow(label = stringResource(R.string.ba_cafe_ap_tools_full_label)) {
                    BaCafeApToolsValue(
                        text = fullText,
                        color = countdownBlue,
                    )
                }
            }
            SheetSectionCard(verticalSpacing = 10.dp) {
                Text(
                    text = stringResource(R.string.ba_cafe_ap_tools_actions_title),
                    color = accentPink,
                    fontWeight = FontWeight.Bold,
                )
                AppDualActionRow(
                    spacing = 8.dp,
                    first = { modifier ->
                        AppLiquidTextButton(
                            modifier = modifier,
                            backdrop = backdrop,
                            text = stringResource(R.string.ba_cafe_ap_action_clear),
                            textColor = MiuixTheme.colorScheme.onBackgroundVariant,
                            containerColor = MiuixTheme.colorScheme.onBackgroundVariant,
                            variant = GlassVariant.SheetAction,
                            textMaxLines = 1,
                            textOverflow = TextOverflow.Ellipsis,
                            onClick = {
                                onClearCafeStoredAp()
                                onDismissRequest()
                            },
                        )
                    },
                    second = { modifier ->
                        AppLiquidTextButton(
                            modifier = modifier,
                            backdrop = backdrop,
                            text = stringResource(R.string.ba_cafe_ap_action_fill),
                            textColor = accentPink,
                            containerColor = accentPink,
                            variant = GlassVariant.SheetAction,
                            textMaxLines = 1,
                            textOverflow = TextOverflow.Ellipsis,
                            onClick = {
                                onFillCafeStoredAp()
                                onDismissRequest()
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun BaCafeApToolsValue(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private fun formatCafeApPrecise(value: Double): String {
    val safeValue = value.coerceAtLeast(0.0)
    val truncated = floor(safeValue * 1000.0) / 1000.0
    return String
        .format(Locale.US, "%.3f", truncated)
        .trimEnd('0')
        .trimEnd('.')
}
