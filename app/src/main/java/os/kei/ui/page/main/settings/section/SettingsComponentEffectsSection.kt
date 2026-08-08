@file:Suppress("FunctionName")

package os.kei.ui.page.main.settings.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import os.kei.R
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.settings.state.SettingsCardExpansionId
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.settings.support.SettingsToggleItem
import os.kei.ui.page.main.widget.core.CardLayoutRhythm

@Composable
internal fun SettingsComponentEffectsSection(
    state: SettingsComponentEffectsSectionState,
    actions: SettingsComponentEffectsSectionActions,
    enabledCardColor: Color,
    disabledCardColor: Color,
    isCardExpanded: (SettingsCardExpansionId) -> Boolean,
    onCardExpandedChange: (SettingsCardExpansionId, Boolean) -> Unit,
    onlyCardId: SettingsCardExpansionId? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.sectionGap),
    ) {
        if (onlyCardId == null || onlyCardId == SettingsCardExpansionId.LiquidControls) {
            SettingsGroupCard(
                header = stringResource(R.string.settings_group_liquid_controls_header),
                title = stringResource(R.string.settings_group_liquid_controls_title),
                subtitle = stringResource(R.string.settings_group_liquid_controls_summary),
                sectionIcon = appLucideConfigIcon(),
                containerColor =
                    settingsSectionContainerColor(
                        SettingsSectionPresentationState(
                            active = state.liquidSwitchEnabled || state.liquidToastEnabled,
                        ),
                        enabledCardColor,
                        disabledCardColor,
                    ),
                expanded = isCardExpanded(SettingsCardExpansionId.LiquidControls),
                onExpandedChange = { onCardExpandedChange(SettingsCardExpansionId.LiquidControls, it) },
            ) {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_liquid_switch_title),
                    summary =
                        if (state.liquidSwitchEnabled) {
                            stringResource(R.string.settings_liquid_switch_summary_enabled)
                        } else {
                            stringResource(R.string.settings_liquid_switch_summary_disabled)
                        },
                    checked = state.liquidSwitchEnabled,
                    onCheckedChange = actions.onLiquidSwitchChanged,
                    infoKey = stringResource(R.string.common_scope),
                    infoValue = stringResource(R.string.settings_liquid_switch_scope),
                )
                SettingsToggleItem(
                    title = stringResource(R.string.settings_liquid_toast_title),
                    summary =
                        if (state.liquidToastEnabled) {
                            stringResource(R.string.settings_liquid_toast_summary_enabled)
                        } else {
                            stringResource(R.string.settings_liquid_toast_summary_disabled)
                        },
                    checked = state.liquidToastEnabled,
                    onCheckedChange = actions.onLiquidToastChanged,
                )
            }
        }
        if (onlyCardId == null || onlyCardId == SettingsCardExpansionId.Interaction) {
            SettingsGroupCard(
                header = stringResource(R.string.settings_group_interaction_header),
                title = stringResource(R.string.settings_group_interaction_title),
                subtitle = stringResource(R.string.settings_group_interaction_summary),
                sectionIcon = appLucideConfigIcon(),
                containerColor =
                    settingsSectionContainerColor(
                        SettingsSectionPresentationState(
                            active =
                                state.reduceToastInterruptionEnabled ||
                                    state.searchAutoFocusEnabled ||
                                    state.gripAwareFloatingDockEnabled,
                        ),
                        enabledCardColor,
                        disabledCardColor,
                    ),
                expanded = isCardExpanded(SettingsCardExpansionId.Interaction),
                onExpandedChange = { onCardExpandedChange(SettingsCardExpansionId.Interaction, it) },
            ) {
                SettingsToggleItem(
                    title = stringResource(R.string.settings_reduce_toast_interruption_title),
                    summary =
                        if (state.reduceToastInterruptionEnabled) {
                            stringResource(R.string.settings_reduce_toast_interruption_summary_enabled)
                        } else {
                            stringResource(R.string.settings_reduce_toast_interruption_summary_disabled)
                        },
                    checked = state.reduceToastInterruptionEnabled,
                    onCheckedChange = actions.onReduceToastInterruptionChanged,
                )
                SettingsToggleItem(
                    title = stringResource(R.string.settings_search_auto_focus_title),
                    summary =
                        if (state.searchAutoFocusEnabled) {
                            stringResource(R.string.settings_search_auto_focus_summary_enabled)
                        } else {
                            stringResource(R.string.settings_search_auto_focus_summary_disabled)
                        },
                    checked = state.searchAutoFocusEnabled,
                    onCheckedChange = actions.onSearchAutoFocusChanged,
                    infoKey = stringResource(R.string.common_scope),
                    infoValue = stringResource(R.string.settings_search_auto_focus_scope),
                )
                SettingsToggleItem(
                    title = stringResource(R.string.settings_grip_aware_floating_dock_title),
                    summary =
                        if (state.gripAwareFloatingDockEnabled) {
                            stringResource(R.string.settings_grip_aware_floating_dock_summary_enabled)
                        } else {
                            stringResource(R.string.settings_grip_aware_floating_dock_summary_disabled)
                        },
                    checked = state.gripAwareFloatingDockEnabled,
                    onCheckedChange = actions.onGripAwareFloatingDockChanged,
                    infoKey = stringResource(R.string.common_scope),
                    infoValue = stringResource(R.string.settings_grip_aware_floating_dock_scope),
                )
            }
        }
    }
}
