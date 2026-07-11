package os.kei.ui.page.main.mcp.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import os.kei.R
import os.kei.mcp.server.McpServerUiState
import os.kei.ui.page.main.mcp.model.toMcpTokenPreview
import os.kei.ui.page.main.mcp.util.formatMcpUptimeText
import os.kei.ui.page.main.widget.core.AppOverviewPill

@Immutable
internal data class McpOverviewPills(
    val service: AppOverviewPill,
    val endpoint: AppOverviewPill,
    val network: AppOverviewPill,
    val clients: AppOverviewPill,
    val token: AppOverviewPill,
)

@Immutable
internal data class McpPageOverviewState(
    val overviewAccentColor: Color,
    val overviewCardColor: Color,
    val overviewBorderColor: Color,
    val runtimeText: String,
    val overviewPills: McpOverviewPills
)

@Composable
internal fun rememberMcpPageOverviewState(
    context: Context,
    uiState: McpServerUiState,
    runtimeNowMs: Long,
    isDark: Boolean,
    subtitleColor: Color,
    infoColor: Color,
    runningColor: Color,
    stoppedColor: Color,
    runtimePendingText: String
): McpPageOverviewState {
    val overviewAccentColor = if (uiState.running) runningColor else stoppedColor
    val overviewCardColor = if (isDark) {
        overviewAccentColor.copy(alpha = 0.16f)
    } else {
        overviewAccentColor.copy(alpha = 0.10f)
    }
    val overviewBorderColor = if (isDark) {
        overviewAccentColor.copy(alpha = 0.32f)
    } else {
        overviewAccentColor.copy(alpha = 0.26f)
    }
    val runtimeText = if (!uiState.running || uiState.runningSinceEpochMs <= 0L) {
        runtimePendingText
    } else {
        formatMcpUptimeText(runtimeNowMs - uiState.runningSinceEpochMs)
    }
    val bindAddress = remember(uiState.allowExternal, uiState.addresses) {
        when {
            !uiState.allowExternal -> "127.0.0.1"
            uiState.addresses.isNotEmpty() -> uiState.addresses.first()
            else -> "0.0.0.0"
        }
    }
    val tokenPreview = remember(uiState.authToken) {
        uiState.authToken.toMcpTokenPreview()
    }.ifBlank { context.getString(R.string.common_na) }
    val endpoint = remember(bindAddress, uiState.port, uiState.endpointPath) {
        val host = if (':' in bindAddress && !bindAddress.startsWith("[")) "[$bindAddress]" else bindAddress
        "$host:${uiState.port}${uiState.endpointPath}"
    }
    val overviewPills = remember(
        context,
        uiState.serverName,
        uiState.allowExternal,
        uiState.connectedClients,
        endpoint,
        tokenPreview,
        overviewAccentColor,
        runningColor,
        subtitleColor,
        infoColor,
        uiState.authToken,
    ) {
        McpOverviewPills(
            service = AppOverviewPill(
                label = uiState.serverName.ifBlank { context.getString(R.string.mcp_default_service_name) },
                color = infoColor,
            ),
            endpoint = AppOverviewPill(
                label = endpoint,
                color = infoColor,
            ),
            network = AppOverviewPill(
                label = if (uiState.allowExternal) {
                    context.getString(R.string.mcp_network_mode_lan_short)
                } else {
                    context.getString(R.string.mcp_network_mode_local_only_short)
                },
                color = overviewAccentColor,
            ),
            clients = AppOverviewPill(
                label = context.getString(
                    R.string.mcp_overview_clients_pill,
                    uiState.connectedClients,
                ),
                color = if (uiState.connectedClients > 0) runningColor else subtitleColor,
            ),
            token = AppOverviewPill(
                label = context.getString(R.string.mcp_overview_token_pill, tokenPreview),
                color = if (uiState.authToken.isBlank()) subtitleColor else infoColor,
            ),
        )
    }
    return remember(
        overviewAccentColor,
        overviewCardColor,
        overviewBorderColor,
        runtimeText,
        overviewPills
    ) {
        McpPageOverviewState(
            overviewAccentColor = overviewAccentColor,
            overviewCardColor = overviewCardColor,
            overviewBorderColor = overviewBorderColor,
            runtimeText = runtimeText,
            overviewPills = overviewPills
        )
    }
}
