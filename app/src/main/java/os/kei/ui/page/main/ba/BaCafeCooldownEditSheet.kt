@file:Suppress("FunctionName")

package os.kei.ui.page.main.ba

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import os.kei.ui.page.main.ba.support.formatBaDateTimeWithSeconds
import os.kei.ui.page.main.ba.support.formatBaRemainingTime
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidSearchField
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionHeader
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaCafeCooldownEditSheet(
    show: Boolean,
    target: BaCafeCooldownEditTarget?,
    backdrop: Backdrop?,
    coffeeHeadpatMs: Long,
    coffeeInvite1UsedMs: Long,
    coffeeInvite2UsedMs: Long,
    serverIndex: Int,
    uiNowMs: Long,
    onSaveRemaining: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val editTarget = target
    val visible = show && editTarget != null
    val accentPink = Color(0xFFF472B6)
    val countdownBlue = Color(0xFF60A5FA)
    val notSyncedText = stringResource(R.string.ba_state_not_synced)
    val currentRemainingMs =
        editTarget?.let { currentTarget ->
            calculateBaCafeCooldownRemainingMs(
                target = currentTarget,
                coffeeHeadpatMs = coffeeHeadpatMs,
                coffeeInvite1UsedMs = coffeeInvite1UsedMs,
                coffeeInvite2UsedMs = coffeeInvite2UsedMs,
                serverIndex = serverIndex,
                nowMs = uiNowMs,
            )
        } ?: 0L
    val maxRemainingMs =
        editTarget?.let { currentTarget ->
            maxBaCafeCooldownRemainingMs(
                target = currentTarget,
                serverIndex = serverIndex,
                nowMs = uiNowMs,
            )
        } ?: 0L
    val currentRemainingText =
        formatBaRemainingTime(
            targetMs = uiNowMs + currentRemainingMs,
            nowMs = uiNowMs,
            includeSeconds = true,
        )
    val maxRemainingText =
        formatBaRemainingTime(
            targetMs = uiNowMs + maxRemainingMs,
            nowMs = uiNowMs,
            includeSeconds = true,
        )
    val availableAtText =
        if (currentRemainingMs <= 0L) {
            stringResource(R.string.common_available)
        } else {
            formatBaDateTimeWithSeconds(uiNowMs + currentRemainingMs, notSyncedText)
        }

    var hoursInput by rememberSaveable { mutableStateOf("0") }
    var minutesInput by rememberSaveable { mutableStateOf("0") }
    var secondsInput by rememberSaveable { mutableStateOf("0") }

    LaunchedEffect(visible, editTarget) {
        if (!visible) return@LaunchedEffect
        val parts = splitCooldownInputParts(currentRemainingMs)
        hoursInput = parts.hours.toString()
        minutesInput = parts.minutes.toString()
        secondsInput = parts.seconds.toString()
    }

    SnapshotWindowBottomSheet(
        show = visible,
        title = editTarget?.titleText().orEmpty(),
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
        if (editTarget == null) return@SnapshotWindowBottomSheet
        SheetContentColumn(verticalSpacing = 14.dp) {
            SheetSectionHeader(
                text = stringResource(R.string.ba_cafe_cooldown_edit_section_title),
                summary = stringResource(R.string.ba_cafe_cooldown_edit_summary),
            )
            SheetSectionCard(verticalSpacing = 10.dp) {
                if (editTarget == BaCafeCooldownEditTarget.Headpat) {
                    SheetDescriptionText(stringResource(R.string.ba_cafe_cooldown_edit_headpat_summary))
                }
                SheetControlRow(label = stringResource(R.string.ba_cafe_cooldown_edit_current_label)) {
                    BaCafeCooldownEditValue(
                        text = currentRemainingText,
                        color = countdownBlue,
                    )
                }
                SheetControlRow(label = stringResource(R.string.ba_cafe_cooldown_edit_available_at_label)) {
                    BaCafeCooldownEditValue(
                        text = availableAtText,
                        color = countdownBlue,
                    )
                }
                SheetControlRow(label = stringResource(R.string.ba_cafe_cooldown_edit_max_label)) {
                    BaCafeCooldownEditValue(
                        text = maxRemainingText,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BaCafeCooldownTimeInput(
                        modifier = Modifier.weight(1f),
                        value = hoursInput,
                        onValueChange = { hoursInput = normalizeCooldownInput(it) },
                        label = stringResource(R.string.ba_cafe_cooldown_edit_hours_label),
                        accentColor = accentPink,
                        backdrop = backdrop,
                        imeAction = ImeAction.Next,
                    )
                    BaCafeCooldownTimeInput(
                        modifier = Modifier.weight(1f),
                        value = minutesInput,
                        onValueChange = { minutesInput = normalizeCooldownInput(it) },
                        label = stringResource(R.string.ba_cafe_cooldown_edit_minutes_label),
                        accentColor = accentPink,
                        backdrop = backdrop,
                        imeAction = ImeAction.Next,
                    )
                    BaCafeCooldownTimeInput(
                        modifier = Modifier.weight(1f),
                        value = secondsInput,
                        onValueChange = { secondsInput = normalizeCooldownInput(it) },
                        label = stringResource(R.string.ba_cafe_cooldown_edit_seconds_label),
                        accentColor = accentPink,
                        backdrop = backdrop,
                        imeAction = ImeAction.Done,
                        onImeActionDone = {
                            onSaveRemaining(parseCooldownInputMs(hoursInput, minutesInput, secondsInput))
                        },
                    )
                }
            }
            AppDualActionRow(
                spacing = 8.dp,
                first = { modifier ->
                    AppLiquidTextButton(
                        modifier = modifier,
                        backdrop = backdrop,
                        text = stringResource(R.string.ba_cafe_cooldown_action_ready_now),
                        textColor = MiuixTheme.colorScheme.onBackgroundVariant,
                        containerColor = MiuixTheme.colorScheme.onBackgroundVariant,
                        variant = GlassVariant.SheetAction,
                        textMaxLines = 1,
                        textOverflow = TextOverflow.Ellipsis,
                        onClick = { onSaveRemaining(0L) },
                    )
                },
                second = { modifier ->
                    AppLiquidTextButton(
                        modifier = modifier,
                        backdrop = backdrop,
                        text = stringResource(R.string.common_save),
                        textColor = accentPink,
                        containerColor = accentPink,
                        variant = GlassVariant.SheetAction,
                        textMaxLines = 1,
                        textOverflow = TextOverflow.Ellipsis,
                        onClick = {
                            onSaveRemaining(parseCooldownInputMs(hoursInput, minutesInput, secondsInput))
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun BaCafeCooldownEditTarget.titleText(): String =
    stringResource(
        when (this) {
            BaCafeCooldownEditTarget.Headpat -> R.string.ba_cafe_cooldown_edit_headpat_title
            BaCafeCooldownEditTarget.InviteTicket1 -> R.string.ba_cafe_cooldown_edit_invite1_title
            BaCafeCooldownEditTarget.InviteTicket2 -> R.string.ba_cafe_cooldown_edit_invite2_title
        },
    )

@Composable
private fun BaCafeCooldownEditValue(
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

@Composable
private fun BaCafeCooldownTimeInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accentColor: Color,
    backdrop: Backdrop?,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    onImeActionDone: () -> Unit = {},
) {
    AppLiquidSearchField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        onImeActionDone = onImeActionDone,
        label = label,
        backdrop = backdrop,
        variant = GlassVariant.SheetInput,
        singleLine = true,
        textAlign = TextAlign.Center,
        fontSize = 18.sp,
        textColor = accentColor,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = imeAction,
            ),
    )
}

private data class CooldownInputParts(
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
)

private fun splitCooldownInputParts(remainingMs: Long): CooldownInputParts {
    var totalSeconds = ((remainingMs.coerceAtLeast(0L) + 999L) / 1000L)
    val hours = totalSeconds / 3_600L
    totalSeconds %= 3_600L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return CooldownInputParts(hours, minutes, seconds)
}

private fun parseCooldownInputMs(
    hours: String,
    minutes: String,
    seconds: String,
): Long {
    val safeHours = hours.toLongOrNull()?.coerceIn(0L, 99L) ?: 0L
    val safeMinutes = minutes.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
    val safeSeconds = seconds.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
    return (safeHours * 3_600L + safeMinutes * 60L + safeSeconds) * 1000L
}

private fun normalizeCooldownInput(input: String): String =
    input
        .filter(Char::isDigit)
        .take(2)
