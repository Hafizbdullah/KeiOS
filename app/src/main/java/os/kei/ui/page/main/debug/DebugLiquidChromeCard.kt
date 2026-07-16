@file:Suppress("FunctionName")

package os.kei.ui.page.main.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideHeartIcon
import os.kei.ui.page.main.os.appLucideHomeIcon
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideSearchIcon
import os.kei.ui.page.main.os.appLucideShareIcon
import os.kei.ui.page.main.widget.chrome.LiquidActionBar
import os.kei.ui.page.main.widget.chrome.LiquidActionItem
import os.kei.ui.page.main.widget.chrome.LiquidGlassBottomBar
import os.kei.ui.page.main.widget.chrome.LiquidGlassBottomBarItem
import os.kei.ui.page.main.widget.chrome.liquidGlassBottomBarItemContentColor
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugLiquidChromeCard(
    accent: Color,
    backdrop: Backdrop,
) {
    var selectedActionIndex by remember { mutableIntStateOf(0) }
    var actionBarInteractionActive by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val contentColor = MiuixTheme.colorScheme.onBackground
    val secondaryColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.78f)
    val actionLabels =
        listOf(
            stringResource(R.string.debug_component_lab_action_play),
            stringResource(R.string.debug_component_lab_action_favorite),
            stringResource(R.string.debug_component_lab_action_share),
            stringResource(R.string.debug_component_lab_action_download),
        )
    val tabLabels =
        listOf(
            stringResource(R.string.debug_component_lab_nav_home),
            stringResource(R.string.debug_component_lab_nav_now),
            stringResource(R.string.debug_component_lab_nav_search),
        )
    val disabledTabIndex = tabLabels.lastIndex

    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_chrome_title),
        subtitle = stringResource(R.string.debug_component_lab_liquid_chrome_subtitle),
        backdrop = backdrop,
        exportBackdropToContent = true,
        sectionIcon = appLucideConfigIcon(),
        titleColor = accent,
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap,
    ) {
        val cardBackdrop = LocalLiquidParentBackdrop.current ?: backdrop
        DebugLiquidChromeSectionLabel(
            text = stringResource(R.string.debug_component_lab_liquid_chrome_action_bar),
            color = contentColor,
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            LiquidActionBar(
                backdrop = cardBackdrop,
                selectedIndex = selectedActionIndex,
                items =
                    listOf(
                        LiquidActionItem(
                            icon = appLucidePlayIcon(),
                            contentDescription = actionLabels[0],
                            tooltipText = actionLabels[0],
                            onClick = { selectedActionIndex = 0 },
                        ),
                        LiquidActionItem(
                            icon = appLucideHeartIcon(),
                            contentDescription = actionLabels[1],
                            tooltipText = actionLabels[1],
                            onClick = { selectedActionIndex = 1 },
                        ),
                        LiquidActionItem(
                            icon = appLucideShareIcon(),
                            contentDescription = actionLabels[2],
                            tooltipText = actionLabels[2],
                            onClick = { selectedActionIndex = 2 },
                        ),
                        LiquidActionItem(
                            icon = appLucideDownloadIcon(),
                            contentDescription = actionLabels[3],
                            tooltipText = actionLabels[3],
                            enabled = false,
                            onClick = {},
                        ),
                    ),
                onInteractionChanged = { actionBarInteractionActive = it },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    stringResource(
                        R.string.debug_component_lab_liquid_chrome_current_action,
                        actionLabels[selectedActionIndex],
                    ),
                color = secondaryColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text =
                    stringResource(
                        if (actionBarInteractionActive) {
                            R.string.debug_component_lab_liquid_chrome_gesture_active
                        } else {
                            R.string.debug_component_lab_liquid_chrome_gesture_idle
                        },
                    ),
                color = if (actionBarInteractionActive) accent else secondaryColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1,
            )
        }

        DebugLiquidChromeSectionLabel(
            text = stringResource(R.string.debug_component_lab_liquid_chrome_bottom_bar),
            color = contentColor,
        )
        LiquidGlassBottomBar(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            selectedIndex = selectedTabIndex,
            onSelected = { index -> selectedTabIndex = index },
            backdrop = cardBackdrop,
            tabsCount = tabLabels.size,
            isTabEnabled = { index -> index != disabledTabIndex },
            expandToMaxWidth = true,
        ) {
            tabLabels.forEachIndexed { index, label ->
                val tabColor = liquidGlassBottomBarItemContentColor(index)
                val icon =
                    when (index) {
                        0 -> appLucideHomeIcon()
                        1 -> appLucideMusicIcon()
                        else -> appLucideSearchIcon()
                    }
                LiquidGlassBottomBarItem(
                    selected = selectedTabIndex == index,
                    tabIndex = index,
                    enabled = index != disabledTabIndex,
                    label = label,
                    onClick = {
                        if (index != disabledTabIndex) {
                            selectedTabIndex = index
                        }
                    },
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tabColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = label,
                        color = tabColor,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Text(
            text =
                stringResource(
                    R.string.debug_component_lab_liquid_chrome_current_tab,
                    tabLabels[selectedTabIndex],
                ),
            color = secondaryColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
    }
}

@Composable
private fun DebugLiquidChromeSectionLabel(
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
