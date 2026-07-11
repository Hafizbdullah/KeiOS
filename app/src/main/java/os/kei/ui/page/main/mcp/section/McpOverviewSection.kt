package os.kei.ui.page.main.mcp.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.mcp.state.McpOverviewPills
import os.kei.ui.page.main.widget.core.AppOverviewCard
import os.kei.ui.page.main.widget.core.AppOverviewPillItem
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun McpOverviewCardSection(
    backdrop: Backdrop?,
    titleColor: Color,
    overviewCardColor: Color,
    overviewBorderColor: Color,
    overviewAccentColor: Color,
    runtimeText: String,
    isDark: Boolean,
    running: Boolean,
    overviewPills: McpOverviewPills,
    onToggleServer: () -> Unit,
    onOpenEditSheet: () -> Unit,
) {
    AppOverviewCard(
        title = "",
        backdrop = backdrop,
        containerColor = overviewCardColor,
        borderColor = overviewBorderColor,
        contentColor = titleColor,
        onClick = onToggleServer,
        onLongClick = onOpenEditSheet,
        titleContent = {
            McpOverviewIdentityPills(
                pills = overviewPills,
                backdrop = backdrop,
            )
        },
        headerEndActions = {
            if (running) {
                StatusPill(
                    label = runtimeText,
                    color = overviewAccentColor,
                    backgroundAlphaOverride = if (isDark) 0.18f else 0.24f,
                    borderAlphaOverride = if (isDark) 0.35f else 0.42f,
                    backdrop = backdrop
                )
            }
            StatusPill(
                label = stringResource(
                    if (running) R.string.common_status_running else R.string.common_status_not_running
                ),
                color = overviewAccentColor,
                backdrop = backdrop
            )
        }
    ) {
        McpOverviewConnectionPills(
            pills = overviewPills,
            backdrop = backdrop,
        )
    }
}

@Composable
private fun McpOverviewIdentityPills(
    pills: McpOverviewPills,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            AppOverviewPillItem(
                pill = pills.service,
                backdrop = backdrop,
            )
        }
        AppOverviewPillItem(pill = pills.network, backdrop = backdrop)
        AppOverviewPillItem(pill = pills.clients, backdrop = backdrop)
    }
}

@Composable
private fun McpOverviewConnectionPills(
    pills: McpOverviewPills,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            AppOverviewPillItem(
                pill = pills.endpoint,
                backdrop = backdrop,
            )
        }
        AppOverviewPillItem(pill = pills.token, backdrop = backdrop)
    }
}

@Composable
internal fun McpSectionHeaderIcon(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MiuixTheme.colorScheme.primary,
        modifier = modifier
            .size(22.dp)
            .defaultMinSize(minHeight = 22.dp)
    )
}
