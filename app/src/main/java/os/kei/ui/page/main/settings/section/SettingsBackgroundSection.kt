@file:Suppress("FunctionName")

package os.kei.ui.page.main.settings.section

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.core.prefs.NonHomeBackgroundAlignment
import os.kei.core.prefs.NonHomeBackgroundContentScale
import os.kei.core.prefs.NonHomeBackgroundPageStyle
import os.kei.ui.page.main.os.appLucideMediaIcon
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_DEFAULT
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_KEY_POINTS
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_MAGNET_THRESHOLD
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_MAX
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_OPACITY_MIN
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_SCRIM_DEFAULT
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_SCRIM_KEY_POINTS
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_SCRIM_MAGNET_THRESHOLD
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_SCRIM_MAX
import os.kei.ui.page.main.settings.support.NON_HOME_BACKGROUND_SCRIM_MIN
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsInfoItem
import os.kei.ui.page.main.settings.support.SettingsPickerItem
import os.kei.ui.page.main.settings.support.SettingsToggleItem
import os.kei.ui.page.main.settings.support.SettingsValueItem
import os.kei.ui.page.main.settings.support.formatOpacityPercent
import os.kei.ui.page.main.widget.core.AppDualActionRow
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.chrome.AppManagedBackgroundHost
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.LiquidSheetInitialDetent
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsBackgroundSection(
    nonHomeBackgroundEnabled: Boolean,
    onNonHomeBackgroundEnabledChanged: (Boolean) -> Unit,
    nonHomeBackgroundUri: String,
    nonHomeBackgroundOpacity: Float,
    onNonHomeBackgroundOpacityChanged: (Float) -> Unit,
    nonHomeBackgroundContentScale: NonHomeBackgroundContentScale,
    onNonHomeBackgroundContentScaleChanged: (NonHomeBackgroundContentScale) -> Unit,
    nonHomeBackgroundAlignment: NonHomeBackgroundAlignment,
    onNonHomeBackgroundAlignmentChanged: (NonHomeBackgroundAlignment) -> Unit,
    nonHomeBackgroundPageStyle: NonHomeBackgroundPageStyle,
    onNonHomeBackgroundPageStyleChanged: (NonHomeBackgroundPageStyle) -> Unit,
    nonHomeBackgroundScrim: Float,
    onNonHomeBackgroundScrimChanged: (Float) -> Unit,
    onResetNonHomeBackgroundRendering: () -> Unit,
    onApplyNonHomeBackgroundReadableSuggestion: (Boolean) -> Unit,
    backgroundPickerLauncher: ActivityResultLauncher<Array<String>>,
    onClearBackground: () -> Unit,
    enabledCardColor: Color,
    disabledCardColor: Color,
    onSliderInteractionChanged: (Boolean) -> Unit = {},
) {
    var scaleDropdownExpanded by remember { mutableStateOf(false) }
    var scaleDropdownAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var alignmentDropdownExpanded by remember { mutableStateOf(false) }
    var alignmentDropdownAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var pageStyleDropdownExpanded by remember { mutableStateOf(false) }
    var pageStyleDropdownAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var previewSheetVisible by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()
    val presentation =
        deriveBackgroundPresentation(
            nonHomeBackgroundEnabled = nonHomeBackgroundEnabled,
            nonHomeBackgroundUri = nonHomeBackgroundUri,
        )
    val contentScaleOptions =
        listOf(
            NonHomeBackgroundContentScale.Crop to stringResource(R.string.settings_non_home_background_scale_crop),
            NonHomeBackgroundContentScale.Fit to stringResource(R.string.settings_non_home_background_scale_fit),
            NonHomeBackgroundContentScale.FillBounds to stringResource(R.string.settings_non_home_background_scale_fill_bounds),
        )
    val contentScaleIndex =
        contentScaleOptions
            .indexOfFirst { it.first == nonHomeBackgroundContentScale }
            .coerceAtLeast(0)
    val contentScaleSummary =
        when (nonHomeBackgroundContentScale) {
            NonHomeBackgroundContentScale.Crop -> stringResource(R.string.settings_non_home_background_scale_summary_crop)
            NonHomeBackgroundContentScale.Fit -> stringResource(R.string.settings_non_home_background_scale_summary_fit)
            NonHomeBackgroundContentScale.FillBounds -> stringResource(R.string.settings_non_home_background_scale_summary_fill_bounds)
        }
    val alignmentOptions =
        listOf(
            NonHomeBackgroundAlignment.Top to stringResource(R.string.settings_non_home_background_alignment_top),
            NonHomeBackgroundAlignment.Center to stringResource(R.string.settings_non_home_background_alignment_center),
            NonHomeBackgroundAlignment.Bottom to stringResource(R.string.settings_non_home_background_alignment_bottom),
            NonHomeBackgroundAlignment.Start to stringResource(R.string.settings_non_home_background_alignment_start),
            NonHomeBackgroundAlignment.End to stringResource(R.string.settings_non_home_background_alignment_end),
        )
    val alignmentIndex =
        alignmentOptions
            .indexOfFirst { it.first == nonHomeBackgroundAlignment }
            .coerceAtLeast(0)
    val alignmentSummary =
        when (nonHomeBackgroundAlignment) {
            NonHomeBackgroundAlignment.Top -> stringResource(R.string.settings_non_home_background_alignment_summary_top)
            NonHomeBackgroundAlignment.Center -> stringResource(R.string.settings_non_home_background_alignment_summary_center)
            NonHomeBackgroundAlignment.Bottom -> stringResource(R.string.settings_non_home_background_alignment_summary_bottom)
            NonHomeBackgroundAlignment.Start -> stringResource(R.string.settings_non_home_background_alignment_summary_start)
            NonHomeBackgroundAlignment.End -> stringResource(R.string.settings_non_home_background_alignment_summary_end)
        }
    val pageStyleOptions =
        listOf(
            NonHomeBackgroundPageStyle.Standard to stringResource(R.string.settings_non_home_background_style_standard),
            NonHomeBackgroundPageStyle.Readable to stringResource(R.string.settings_non_home_background_style_readable),
            NonHomeBackgroundPageStyle.Soft to stringResource(R.string.settings_non_home_background_style_soft),
            NonHomeBackgroundPageStyle.Focused to stringResource(R.string.settings_non_home_background_style_focused),
        )
    val pageStyleIndex =
        pageStyleOptions
            .indexOfFirst { it.first == nonHomeBackgroundPageStyle }
            .coerceAtLeast(0)
    val pageStyleSummary =
        when (nonHomeBackgroundPageStyle) {
            NonHomeBackgroundPageStyle.Standard -> stringResource(R.string.settings_non_home_background_style_summary_standard)
            NonHomeBackgroundPageStyle.Readable -> stringResource(R.string.settings_non_home_background_style_summary_readable)
            NonHomeBackgroundPageStyle.Soft -> stringResource(R.string.settings_non_home_background_style_summary_soft)
            NonHomeBackgroundPageStyle.Focused -> stringResource(R.string.settings_non_home_background_style_summary_focused)
        }
    SettingsGroupCard(
        header = stringResource(R.string.settings_group_background_header),
        title = stringResource(R.string.settings_group_background_title),
        sectionIcon = appLucideMediaIcon(),
        containerColor = settingsSectionContainerColor(presentation, enabledCardColor, disabledCardColor),
    ) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_non_home_background_title),
            summary =
                if (nonHomeBackgroundEnabled) {
                    stringResource(R.string.settings_non_home_background_summary_enabled)
                } else {
                    stringResource(R.string.settings_non_home_background_summary_disabled)
                },
            checked = nonHomeBackgroundEnabled,
            onCheckedChange = onNonHomeBackgroundEnabledChanged,
            infoKey = stringResource(R.string.common_scope),
            infoValue = stringResource(R.string.settings_non_home_background_scope),
        )
        SettingsValueItem(
            title = stringResource(R.string.settings_non_home_background_image_title),
            summary =
                if (nonHomeBackgroundUri.isBlank()) {
                    stringResource(R.string.settings_non_home_background_image_summary_empty)
                } else {
                    stringResource(R.string.settings_non_home_background_image_summary_ready)
                },
        )
        AppDualActionRow(
            first = { modifier ->
                AppStandaloneLiquidTextButton(
                    variant = GlassVariant.SheetPrimaryAction,
                    text = stringResource(R.string.settings_non_home_background_action_select),
                    modifier = modifier,
                    buttonModifier = Modifier.fillMaxWidth(),
                    textColor = MiuixTheme.colorScheme.primary,
                    onClick = { backgroundPickerLauncher.launch(arrayOf("image/*")) },
                )
            },
            second = { modifier ->
                AppStandaloneLiquidTextButton(
                    variant = GlassVariant.SheetDangerAction,
                    text = stringResource(R.string.settings_non_home_background_action_clear),
                    modifier = modifier,
                    buttonModifier = Modifier.fillMaxWidth(),
                    textColor = MiuixTheme.colorScheme.error,
                    enabled = nonHomeBackgroundUri.isNotBlank(),
                    onClick = {
                        onClearBackground()
                    },
                )
            },
        )
        SettingsBackgroundQuickActions(
            previewLabel = stringResource(R.string.settings_non_home_background_action_preview),
            suggestionLabel = stringResource(R.string.settings_non_home_background_action_suggest),
            resetLabel = stringResource(R.string.common_reset),
            enabled = nonHomeBackgroundEnabled,
            onPreview = { previewSheetVisible = true },
            onSuggestion = { onApplyNonHomeBackgroundReadableSuggestion(isDarkTheme) },
            onReset = onResetNonHomeBackgroundRendering,
        )
        SettingsPickerItem(
            title = stringResource(R.string.settings_non_home_background_scale_title),
            summary = contentScaleSummary,
            onClick = if (nonHomeBackgroundEnabled) {
                { scaleDropdownExpanded = true }
            } else {
                null
            },
        ) {
            AppDropdownSelector(
                selectedText = contentScaleOptions.getOrElse(contentScaleIndex) { contentScaleOptions.first() }.second,
                options = contentScaleOptions.map { it.second },
                selectedIndex = contentScaleIndex,
                expanded = scaleDropdownExpanded,
                anchorBounds = scaleDropdownAnchorBounds,
                onExpandedChange = { scaleDropdownExpanded = it },
                onSelectedIndexChange = { selectedIndex ->
                    onNonHomeBackgroundContentScaleChanged(contentScaleOptions[selectedIndex].first)
                },
                onAnchorBoundsChange = { scaleDropdownAnchorBounds = it },
                variant = GlassVariant.SheetAction,
                enabled = nonHomeBackgroundEnabled,
                popupMatchAnchorWidth = true,
            )
        }
        SettingsPickerItem(
            title = stringResource(R.string.settings_non_home_background_alignment_title),
            summary = alignmentSummary,
            onClick = if (nonHomeBackgroundEnabled) {
                { alignmentDropdownExpanded = true }
            } else {
                null
            },
        ) {
            AppDropdownSelector(
                selectedText = alignmentOptions.getOrElse(alignmentIndex) { alignmentOptions[1] }.second,
                options = alignmentOptions.map { it.second },
                selectedIndex = alignmentIndex,
                expanded = alignmentDropdownExpanded,
                anchorBounds = alignmentDropdownAnchorBounds,
                onExpandedChange = { alignmentDropdownExpanded = it },
                onSelectedIndexChange = { selectedIndex ->
                    onNonHomeBackgroundAlignmentChanged(alignmentOptions[selectedIndex].first)
                },
                onAnchorBoundsChange = { alignmentDropdownAnchorBounds = it },
                variant = GlassVariant.SheetAction,
                enabled = nonHomeBackgroundEnabled,
                popupMatchAnchorWidth = true,
            )
        }
        SettingsPickerItem(
            title = stringResource(R.string.settings_non_home_background_style_title),
            summary = pageStyleSummary,
            onClick = if (nonHomeBackgroundEnabled) {
                { pageStyleDropdownExpanded = true }
            } else {
                null
            },
        ) {
            AppDropdownSelector(
                selectedText = pageStyleOptions.getOrElse(pageStyleIndex) { pageStyleOptions.first() }.second,
                options = pageStyleOptions.map { it.second },
                selectedIndex = pageStyleIndex,
                expanded = pageStyleDropdownExpanded,
                anchorBounds = pageStyleDropdownAnchorBounds,
                onExpandedChange = { pageStyleDropdownExpanded = it },
                onSelectedIndexChange = { selectedIndex ->
                    onNonHomeBackgroundPageStyleChanged(pageStyleOptions[selectedIndex].first)
                },
                onAnchorBoundsChange = { pageStyleDropdownAnchorBounds = it },
                variant = GlassVariant.SheetAction,
                enabled = nonHomeBackgroundEnabled,
                popupMatchAnchorWidth = true,
            )
        }
        val opacityTitle = stringResource(R.string.settings_non_home_background_opacity_title)
        SettingsValueItem(
            title = opacityTitle,
            summary =
                stringResource(
                    R.string.settings_non_home_background_opacity_summary,
                    formatOpacityPercent(nonHomeBackgroundOpacity),
                ),
        )
        SettingsLiquidKeyPointSlider(
            value =
                nonHomeBackgroundOpacity.coerceIn(
                    NON_HOME_BACKGROUND_OPACITY_MIN,
                    NON_HOME_BACKGROUND_OPACITY_MAX,
                ),
            onValueChange = onNonHomeBackgroundOpacityChanged,
            valueRange = NON_HOME_BACKGROUND_OPACITY_MIN..NON_HOME_BACKGROUND_OPACITY_MAX,
            keyPoints = NON_HOME_BACKGROUND_OPACITY_KEY_POINTS,
            magnetThreshold = NON_HOME_BACKGROUND_OPACITY_MAGNET_THRESHOLD,
            enabled = nonHomeBackgroundEnabled,
            contentDescription = opacityTitle,
            onInteractionChanged = onSliderInteractionChanged,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
        )
        SettingsInfoItem(
            key = stringResource(R.string.common_note),
            value =
                stringResource(
                    R.string.settings_non_home_background_opacity_default,
                    formatOpacityPercent(NON_HOME_BACKGROUND_OPACITY_DEFAULT),
                ),
        )
        val scrimTitle = stringResource(R.string.settings_non_home_background_scrim_title)
        SettingsValueItem(
            title = scrimTitle,
            summary =
                stringResource(
                    R.string.settings_non_home_background_scrim_summary,
                    formatOpacityPercent(nonHomeBackgroundScrim),
                ),
        )
        SettingsLiquidKeyPointSlider(
            value =
                nonHomeBackgroundScrim.coerceIn(
                    NON_HOME_BACKGROUND_SCRIM_MIN,
                    NON_HOME_BACKGROUND_SCRIM_MAX,
                ),
            onValueChange = onNonHomeBackgroundScrimChanged,
            valueRange = NON_HOME_BACKGROUND_SCRIM_MIN..NON_HOME_BACKGROUND_SCRIM_MAX,
            keyPoints = NON_HOME_BACKGROUND_SCRIM_KEY_POINTS,
            magnetThreshold = NON_HOME_BACKGROUND_SCRIM_MAGNET_THRESHOLD,
            enabled = nonHomeBackgroundEnabled,
            contentDescription = scrimTitle,
            onInteractionChanged = onSliderInteractionChanged,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
        )
        SettingsInfoItem(
            key = stringResource(R.string.common_note),
            value =
                stringResource(
                    R.string.settings_non_home_background_scrim_default,
                    formatOpacityPercent(NON_HOME_BACKGROUND_SCRIM_DEFAULT),
                ),
        )
    }
    BackgroundPreviewSheet(
        visible = previewSheetVisible,
        onDismissRequest = { previewSheetVisible = false },
        nonHomeBackgroundEnabled = nonHomeBackgroundEnabled,
        nonHomeBackgroundUri = nonHomeBackgroundUri,
        nonHomeBackgroundOpacity = nonHomeBackgroundOpacity,
        nonHomeBackgroundContentScale = nonHomeBackgroundContentScale,
        nonHomeBackgroundAlignment = nonHomeBackgroundAlignment,
        nonHomeBackgroundPageStyle = nonHomeBackgroundPageStyle,
        nonHomeBackgroundScrim = nonHomeBackgroundScrim,
    )
}

@Composable
private fun SettingsBackgroundQuickActions(
    previewLabel: String,
    suggestionLabel: String,
    resetLabel: String,
    enabled: Boolean,
    onPreview: () -> Unit,
    onSuggestion: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
    ) {
        AppStandaloneLiquidTextButton(
            variant = GlassVariant.SheetAction,
            text = previewLabel,
            modifier = Modifier.weight(1f),
            buttonModifier = Modifier.fillMaxWidth(),
            textColor = MiuixTheme.colorScheme.primary,
            onClick = onPreview,
        )
        AppStandaloneLiquidTextButton(
            variant = GlassVariant.SheetPrimaryAction,
            text = suggestionLabel,
            modifier = Modifier.weight(1f),
            buttonModifier = Modifier.fillMaxWidth(),
            textColor = MiuixTheme.colorScheme.primary,
            enabled = enabled,
            onClick = onSuggestion,
        )
        AppStandaloneLiquidTextButton(
            variant = GlassVariant.SheetDangerAction,
            text = resetLabel,
            modifier = Modifier.weight(1f),
            buttonModifier = Modifier.fillMaxWidth(),
            textColor = MiuixTheme.colorScheme.error,
            onClick = onReset,
        )
    }
}

@Composable
private fun BackgroundPreviewSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    nonHomeBackgroundEnabled: Boolean,
    nonHomeBackgroundUri: String,
    nonHomeBackgroundOpacity: Float,
    nonHomeBackgroundContentScale: NonHomeBackgroundContentScale,
    nonHomeBackgroundAlignment: NonHomeBackgroundAlignment,
    nonHomeBackgroundPageStyle: NonHomeBackgroundPageStyle,
    nonHomeBackgroundScrim: Float,
) {
    SnapshotWindowBottomSheet(
        show = visible,
        title = stringResource(R.string.settings_non_home_background_preview_title),
        onDismissRequest = onDismissRequest,
        initialDetent = LiquidSheetInitialDetent.ThreeQuarter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppChromeTokens.pageSectionGap),
        ) {
            SettingsInfoItem(
                key = stringResource(R.string.common_scope),
                value = stringResource(R.string.settings_non_home_background_preview_scope),
            )
            AppManagedBackgroundHost(
                enabled = nonHomeBackgroundEnabled,
                imageUri = nonHomeBackgroundUri,
                opacity = nonHomeBackgroundOpacity,
                contentScale = nonHomeBackgroundContentScale,
                alignment = nonHomeBackgroundAlignment,
                pageStyle = nonHomeBackgroundPageStyle,
                scrim = nonHomeBackgroundScrim,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(24.dp)),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(AppChromeTokens.pageSectionGap),
                ) {
                    BackgroundPreviewSampleCard(
                        title = stringResource(R.string.settings_non_home_background_preview_sample_title),
                        body = stringResource(R.string.settings_non_home_background_preview_sample_body),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                    ) {
                        BackgroundPreviewChip(
                            text = stringResource(R.string.settings_non_home_background_preview_sample_primary),
                            modifier = Modifier.weight(1f),
                        )
                        BackgroundPreviewChip(
                            text = stringResource(R.string.settings_non_home_background_preview_sample_secondary),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundPreviewSampleCard(
    title: String,
    body: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.74f),
                    shape = RoundedCornerShape(18.dp),
                ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap),
    ) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = AppTypographyTokens.CompactTitle.fontSize,
            lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
            fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
        )
        Text(
            text = body,
            color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.90f),
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
    }
}

@Composable
private fun BackgroundPreviewChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onBackground,
        fontSize = AppTypographyTokens.Supporting.fontSize,
        lineHeight = AppTypographyTokens.Supporting.lineHeight,
        modifier =
            modifier
                .background(
                    color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(999.dp),
                ).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
