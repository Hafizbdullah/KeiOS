package os.kei.ui.page.main.home.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import os.kei.feature.home.model.HomeAppOverview
import os.kei.feature.home.model.HomeBaOverview
import os.kei.feature.home.model.HomeGitHubOverview
import os.kei.feature.home.model.HomeMcpOverview
import os.kei.feature.home.model.HomeWebDavOverview
import os.kei.ui.page.main.widget.status.AppStatusColors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import os.kei.core.privilege.PrivilegeStatus

@Immutable
internal data class HomePageContentState(
    val homeNa: String,
    val homeAppName: String,
    val homeTagline: String,
    val homeStatusMcp: String,
    val homeStatusGitHub: String,
    val homeStatusWebDav: String,
    val homeStatusPrivilege: String?,
    val homeCardMcp: String,
    val homeCardGitHub: String,
    val homeCardWebDav: String,
    val homeCardBa: String,
    val privilegeGranted: Boolean,
    val runningColor: Color,
    val stoppedColor: Color,
    val inactiveColor: Color,
    val cacheStateColor: Color,
    val appVersionText: String,
    val githubFocusLine: String,
    val mcpStatusText: String,
    val mcpRuntimeText: String,
    val networkModeText: String,
    val homeStatToken: String,
    val mcpTokenStatusText: String,
    val homeStatStableUpdates: String,
    val homeStatPreReleaseUpdates: String,
    val homeStatFailed: String,
    val homeStatTracked: String,
    val homeStatActions: String,
    val homeStatPreciseVersion: String,
    val homeStatCached: String,
    val homeStatCacheState: String,
    val githubCacheFreshnessLine: String,
    val homeStatShare: String,
    val githubShareLine: String,
    val homeStatLastUpdate: String,
    val githubLastUpdateLine: String,
    val githubStrategyLine: String,
    val webDavStatusLine: String,
    val webDavSyncItemsLine: String,
    val webDavLastAutoSyncLine: String,
    val webDavLastFullSyncLine: String,
    val homeStatSyncItems: String,
    val homeStatLastAutoSync: String,
    val homeStatLastFullSync: String,
    val baActivationLine: String,
    val homeStatAp: String,
    val baApLine: String,
    val homeStatCafeAp: String,
    val baCafeApLine: String,
    val homeStatBaAccounts: String,
    val baAccountsLine: String,
    val homeStatBaActiveAccount: String,
    val baActiveAccountLine: String,
    val homeStatBaServer: String,
    val baServerLine: String,
    val homeStatBaNotify: String,
    val baNotifyLine: String,
    val baCacheFreshnessLine: String,
)

@Immutable
internal data class HomePageContentColors(
    val runningColor: Color,
    val stoppedColor: Color,
    val inactiveColor: Color,
    val githubCacheColor: Color,
)

@Composable
internal fun rememberHomePageContentState(
    privilegeStatus: PrivilegeStatus,
    appOverview: HomeAppOverview,
    mcpOverview: HomeMcpOverview,
    githubOverview: HomeGitHubOverview,
    webDavOverview: HomeWebDavOverview,
    baOverview: HomeBaOverview,
    runtimeNowMs: Long,
): HomePageContentState {
    val text = rememberHomePageContentTextBundle()
    val inactiveColor = MiuixTheme.colorScheme.onBackgroundVariant
    val colors =
        remember(inactiveColor) {
            HomePageContentColors(
                runningColor = AppStatusColors.Fresh,
                stoppedColor = AppStatusColors.Failed,
                inactiveColor = inactiveColor,
                githubCacheColor = AppStatusColors.Cached,
            )
        }
    return remember(
        privilegeStatus,
        appOverview,
        mcpOverview,
        githubOverview,
        webDavOverview,
        baOverview,
        runtimeNowMs,
        text,
        colors,
    ) {
        deriveHomePageContentState(
            privilegeStatus = privilegeStatus,
            appOverview = appOverview,
            mcpOverview = mcpOverview,
            githubOverview = githubOverview,
            webDavOverview = webDavOverview,
            baOverview = baOverview,
            runtimeNowMs = runtimeNowMs,
            text = text,
            colors = colors,
        )
    }
}
