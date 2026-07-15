@file:Suppress("FunctionName")

package os.kei.ui.page.main.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideConfirmIcon
import os.kei.ui.page.main.os.appLucideInfoIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.dialog.LiquidGlassDialog
import os.kei.ui.page.main.widget.glass.AppLiquidCheckbox
import os.kei.ui.page.main.widget.glass.AppLiquidDialogActionButton
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.glass.LiquidLinearProgressBar
import os.kei.ui.page.main.widget.glass.LiquidMusicProgressBar
import os.kei.ui.page.main.widget.glass.LiquidToastHost
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.rememberLiquidToastState
import os.kei.ui.page.main.widget.shape.appSquircleSurface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugLiquidFeedbackCard(
    accent: Color,
    backdrop: Backdrop,
) {
    var primaryChecked by remember { mutableStateOf(true) }
    var secondaryChecked by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.42f) }
    var toastSequence by remember { mutableIntStateOf(0) }
    var dialogVisible by remember { mutableStateOf(false) }
    var dialogDismissFinishedCount by remember { mutableIntStateOf(0) }
    var dialogResultRes by remember {
        mutableIntStateOf(R.string.debug_component_lab_liquid_feedback_dialog_result_idle)
    }
    val context = LocalContext.current
    val toastState = rememberLiquidToastState()
    val toastPreviewBackdrop = rememberLayerBackdrop()
    val confirmIcon = appLucideConfirmIcon()
    val contentColor = MiuixTheme.colorScheme.onBackground
    val secondaryColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.78f)
    val checkedLabel = stringResource(R.string.debug_component_lab_liquid_feedback_checkbox_checked)
    val uncheckedLabel = stringResource(R.string.debug_component_lab_liquid_feedback_checkbox_unchecked)
    val disabledLabel = stringResource(R.string.debug_component_lab_liquid_feedback_checkbox_disabled)
    val linearProgressLabel = stringResource(R.string.debug_component_lab_liquid_feedback_progress_linear)
    val musicProgressLabel = stringResource(R.string.debug_component_lab_liquid_feedback_progress_music)
    val circularProgressLabel = stringResource(R.string.debug_component_lab_liquid_feedback_progress_circular)
    val indeterminateProgressLabel =
        stringResource(R.string.debug_component_lab_liquid_feedback_progress_indeterminate)

    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_feedback_title),
        subtitle = stringResource(R.string.debug_component_lab_liquid_feedback_subtitle),
        backdrop = backdrop,
        exportBackdropToContent = true,
        sectionIcon = appLucideInfoIcon(),
        titleColor = accent,
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap,
    ) {
        val cardBackdrop = LocalLiquidParentBackdrop.current ?: backdrop
        DebugLiquidFeedbackSectionLabel(
            text = stringResource(R.string.debug_component_lab_liquid_feedback_checkbox_section),
            color = contentColor,
        )
        DebugLiquidCheckboxRow(
            label = checkedLabel,
            checked = primaryChecked,
            enabled = true,
            backdrop = cardBackdrop,
            contentColor = contentColor,
            onCheckedChange = { primaryChecked = it },
        )
        DebugLiquidCheckboxRow(
            label = uncheckedLabel,
            checked = secondaryChecked,
            enabled = true,
            backdrop = cardBackdrop,
            contentColor = contentColor,
            onCheckedChange = { secondaryChecked = it },
        )
        DebugLiquidCheckboxRow(
            label = disabledLabel,
            checked = true,
            enabled = false,
            backdrop = cardBackdrop,
            contentColor = contentColor,
            onCheckedChange = {},
        )

        DebugLiquidFeedbackSectionLabel(
            text = stringResource(R.string.debug_component_lab_liquid_feedback_progress_section),
            color = contentColor,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = linearProgressLabel,
                color = contentColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
            )
            Text(
                text =
                    stringResource(
                        R.string.debug_component_lab_liquid_feedback_progress_value,
                        (progress * 100f).toInt(),
                    ),
                color = secondaryColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
            )
        }
        LiquidLinearProgressBar(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            activeColor = accent,
            height = 8.dp,
            contentDescription = linearProgressLabel,
            backdrop = cardBackdrop,
        )
        Text(
            text = musicProgressLabel,
            color = contentColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
        LiquidMusicProgressBar(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            activeColor = accent,
            contentDescription = musicProgressLabel,
            backdrop = cardBackdrop,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DebugLiquidCircularProgressSample(
                label = circularProgressLabel,
                progress = { progress },
                accent = accent,
                contentColor = contentColor,
            )
            DebugLiquidCircularProgressSample(
                label = indeterminateProgressLabel,
                progress = null,
                accent = accent,
                contentColor = contentColor,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppLiquidTextButton(
                backdrop = cardBackdrop,
                text = stringResource(R.string.debug_component_lab_liquid_feedback_progress_decrease),
                onClick = { progress = (progress - 0.1f).coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f),
                textColor = contentColor,
                variant = GlassVariant.SheetAction,
                minHeight = 44.dp,
                textMaxLines = 1,
                textOverflow = TextOverflow.Ellipsis,
            )
            AppLiquidTextButton(
                backdrop = cardBackdrop,
                text = stringResource(R.string.debug_component_lab_liquid_feedback_progress_increase),
                onClick = { progress = (progress + 0.1f).coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f),
                textColor = contentColor,
                variant = GlassVariant.SheetPrimaryAction,
                minHeight = 44.dp,
                textMaxLines = 1,
                textOverflow = TextOverflow.Ellipsis,
            )
        }

        DebugLiquidFeedbackSectionLabel(
            text = stringResource(R.string.debug_component_lab_liquid_feedback_toast_section),
            color = contentColor,
        )
        Text(
            text = stringResource(R.string.debug_component_lab_liquid_feedback_toast_hint),
            color = secondaryColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(154.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .appSquircleSurface(accent.copy(alpha = 0.12f), 24.dp)
                        .layerBackdrop(toastPreviewBackdrop),
            )
            LiquidToastHost(
                state = toastState,
                backdrop = toastPreviewBackdrop,
                modifier = Modifier.matchParentSize(),
            )
        }
        AppLiquidTextButton(
            backdrop = cardBackdrop,
            text = stringResource(R.string.debug_component_lab_liquid_feedback_toast_show),
            onClick = {
                val nextSequence = toastSequence + 1
                toastSequence = nextSequence
                toastState.show(
                    message =
                        context.getString(
                            R.string.debug_component_lab_liquid_feedback_toast_message,
                            nextSequence,
                        ),
                    icon = confirmIcon,
                    iconTint = accent,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            textColor = contentColor,
            leadingIcon = confirmIcon,
            iconTint = accent,
            variant = GlassVariant.SheetAction,
            minHeight = 44.dp,
        )

        DebugLiquidFeedbackSectionLabel(
            text = stringResource(R.string.debug_component_lab_liquid_feedback_dialog_section),
            color = contentColor,
        )
        Text(
            text =
                stringResource(
                    R.string.debug_component_lab_liquid_feedback_dialog_state,
                    stringResource(dialogResultRes),
                    dialogDismissFinishedCount,
                ),
            color = secondaryColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
        AppLiquidTextButton(
            backdrop = cardBackdrop,
            text = stringResource(R.string.debug_component_lab_liquid_feedback_dialog_open),
            onClick = { dialogVisible = true },
            modifier = Modifier.fillMaxWidth(),
            textColor = contentColor,
            leadingIcon = appLucidePlayIcon(),
            iconTint = contentColor,
            variant = GlassVariant.SheetPrimaryAction,
            minHeight = 44.dp,
        )
    }

    LiquidGlassDialog(
        show = dialogVisible,
        title = stringResource(R.string.debug_component_lab_liquid_feedback_dialog_title),
        summary = stringResource(R.string.debug_component_lab_liquid_feedback_dialog_summary),
        onDismissRequest = {
            dialogResultRes = R.string.debug_component_lab_liquid_feedback_dialog_result_dismissed
            dialogVisible = false
        },
        onDismissFinished = { dialogDismissFinishedCount += 1 },
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppLiquidDialogActionButton(
                text = stringResource(R.string.debug_component_lab_liquid_feedback_dialog_cancel),
                onClick = {
                    dialogResultRes = R.string.debug_component_lab_liquid_feedback_dialog_result_cancelled
                    dialogVisible = false
                },
                modifier = Modifier.weight(1f),
                textColor = contentColor,
                variant = GlassVariant.SheetAction,
            )
            AppLiquidDialogActionButton(
                text = stringResource(R.string.debug_component_lab_liquid_feedback_dialog_confirm),
                onClick = {
                    dialogResultRes = R.string.debug_component_lab_liquid_feedback_dialog_result_confirmed
                    dialogVisible = false
                },
                modifier = Modifier.weight(1f),
                textColor = contentColor,
                variant = GlassVariant.SheetPrimaryAction,
            )
        }
    }
}

@Composable
private fun DebugLiquidFeedbackSectionLabel(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        color = color,
        fontSize = AppTypographyTokens.Body.fontSize,
        lineHeight = AppTypographyTokens.Body.lineHeight,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun DebugLiquidCheckboxRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    backdrop: Backdrop,
    contentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLiquidCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            backdrop = backdrop,
            contentDescription = label,
        )
        Text(
            text = label,
            color = contentColor.copy(alpha = if (enabled) 1f else 0.48f),
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
private fun DebugLiquidCircularProgressSample(
    label: String,
    progress: (() -> Float)?,
    accent: Color,
    contentColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(0.46f),
    ) {
        LiquidCircularProgressBar(
            progress = progress,
            activeColor = accent,
            size = 42.dp,
            strokeWidth = 4.dp,
            contentDescription = label,
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
