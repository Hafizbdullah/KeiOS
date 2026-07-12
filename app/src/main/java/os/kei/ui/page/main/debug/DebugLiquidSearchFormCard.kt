@file:Suppress("FunctionName")

package os.kei.ui.page.main.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideAddIcon
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.appLucideSearchIcon
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppFloatingSearchDock
import os.kei.ui.page.main.widget.glass.AppFloatingVerticalSearchActionDock
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidSearchField
import os.kei.ui.page.main.widget.glass.AppSwitch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugLiquidSearchFormCard(
    accent: Color,
    backdrop: Backdrop,
) {
    var formQuery by remember { mutableStateOf("") }
    var imeActionCount by remember { mutableIntStateOf(0) }
    var floatingExpanded by remember { mutableStateOf(false) }
    var floatingQuery by remember { mutableStateOf("") }
    var verticalExpanded by remember { mutableStateOf(false) }
    var verticalQuery by remember { mutableStateOf("") }
    var refreshEnabled by remember { mutableStateOf(false) }
    var addActionCount by remember { mutableIntStateOf(0) }
    var refreshActionCount by remember { mutableIntStateOf(0) }
    val contentColor = MiuixTheme.colorScheme.onBackground
    val secondaryColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.84f)
    val emptyValue = stringResource(R.string.debug_component_lab_liquid_search_empty_value)

    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_search_title),
        subtitle = stringResource(R.string.debug_component_lab_liquid_search_subtitle),
        sectionIcon = appLucideSearchIcon(),
        titleColor = accent,
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap,
    ) {
        DebugLiquidSearchSectionLabel(
            text = stringResource(R.string.debug_component_lab_liquid_search_field_section),
            color = contentColor,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppLiquidSearchField(
                value = formQuery,
                onValueChange = { formQuery = it },
                label = stringResource(R.string.debug_component_lab_liquid_search_field_placeholder),
                backdrop = backdrop,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onImeActionDone = { imeActionCount++ },
            )
            AppLiquidIconButton(
                backdrop = backdrop,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_clear),
                onClick = { formQuery = "" },
                enabled = formQuery.isNotEmpty(),
                modifier = Modifier.size(48.dp),
                width = 48.dp,
                height = 48.dp,
                iconTint = contentColor,
            )
        }
        Text(
            text =
                stringResource(
                    R.string.debug_component_lab_liquid_search_field_state,
                    formQuery.ifBlank { emptyValue },
                    imeActionCount,
                ),
            color = secondaryColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )

        DebugLiquidSearchSectionLabel(
            text = stringResource(R.string.debug_component_lab_liquid_search_floating_section),
            color = contentColor,
        )
        Text(
            text = stringResource(R.string.debug_component_lab_liquid_search_floating_hint),
            color = secondaryColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(70.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            AppFloatingSearchDock(
                backdrop = backdrop,
                expanded = floatingExpanded,
                query = floatingQuery,
                onQueryChange = { floatingQuery = it },
                onExpandedChange = { floatingExpanded = it },
                searchIcon = appLucideSearchIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_liquid_search_toggle),
                placeholder = stringResource(R.string.debug_component_lab_liquid_search_floating_placeholder),
                horizontalInset = DebugLiquidSearchDockHorizontalInset,
                keyboardLift = 0.dp,
                accent = accent,
            )
        }

        DebugLiquidSearchSectionLabel(
            text = stringResource(R.string.debug_component_lab_liquid_search_vertical_section),
            color = contentColor,
        )
        Text(
            text = stringResource(R.string.debug_component_lab_liquid_search_vertical_hint),
            color = secondaryColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_liquid_search_refresh_enabled),
                color = contentColor,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight,
                modifier = Modifier.weight(1f),
            )
            AppSwitch(
                checked = refreshEnabled,
                onCheckedChange = { refreshEnabled = it },
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(198.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            AppFloatingVerticalSearchActionDock(
                backdrop = backdrop,
                expanded = verticalExpanded,
                query = verticalQuery,
                onQueryChange = { verticalQuery = it },
                onExpandedChange = { verticalExpanded = it },
                searchIcon = appLucideSearchIcon(),
                searchContentDescription = stringResource(R.string.debug_component_lab_liquid_search_toggle),
                placeholder = stringResource(R.string.debug_component_lab_liquid_search_vertical_placeholder),
                addIcon = appLucideAddIcon(),
                addContentDescription = stringResource(R.string.debug_component_lab_liquid_search_add),
                onAddClick = { addActionCount++ },
                refreshIcon = appLucideRefreshIcon(),
                refreshContentDescription = stringResource(R.string.common_refresh),
                onRefreshClick = { refreshActionCount++ },
                refreshEnabled = refreshEnabled,
                horizontalInset = DebugLiquidSearchDockHorizontalInset,
                keyboardLift = 0.dp,
                accent = accent,
            )
        }
        Text(
            text =
                stringResource(
                    R.string.debug_component_lab_liquid_search_action_state,
                    addActionCount,
                    refreshActionCount,
                ),
            color = secondaryColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DebugLiquidSearchSectionLabel(
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

private val DebugLiquidSearchDockHorizontalInset = 28.dp
