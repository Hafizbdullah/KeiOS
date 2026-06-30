@file:Suppress("FunctionName")

package os.kei.ui.page.main.settings.section

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.core.prefs.NonHomeBackgroundContentScale
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
import os.kei.ui.page.main.widget.glass.AppDropdownSelector
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
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
    nonHomeBackgroundScrim: Float,
    onNonHomeBackgroundScrimChanged: (Float) -> Unit,
    backgroundPickerLauncher: ActivityResultLauncher<Array<String>>,
    onClearBackground: () -> Unit,
    enabledCardColor: Color,
    disabledCardColor: Color,
    onSliderInteractionChanged: (Boolean) -> Unit = {},
) {
    var scaleDropdownExpanded by remember { mutableStateOf(false) }
    var scaleDropdownAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
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
}
