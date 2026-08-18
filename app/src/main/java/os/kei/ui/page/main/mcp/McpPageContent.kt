@file:Suppress("FunctionName")

package os.kei.ui.page.main.mcp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import os.kei.R
import os.kei.mcp.server.McpServerUiState
import os.kei.ui.page.main.host.pager.MainPageBackdropSet
import os.kei.ui.page.main.host.pager.MainPageContentBackdropScene
import os.kei.ui.page.main.host.pager.MainPageRuntime
import os.kei.ui.page.main.mcp.section.McpOnboardingGuideSection
import os.kei.ui.page.main.mcp.section.McpLogsSection
import os.kei.ui.page.main.mcp.section.McpOverviewCardSection
import os.kei.ui.page.main.mcp.section.McpServiceControlSection
import os.kei.ui.page.main.mcp.section.McpToolAdvancedSection
import os.kei.ui.page.main.mcp.section.McpToolBaSection
import os.kei.ui.page.main.mcp.section.McpToolCodexSection
import os.kei.ui.page.main.mcp.section.McpToolEntrypointsSection
import os.kei.ui.page.main.mcp.section.McpToolGithubSection
import os.kei.ui.page.main.mcp.section.McpToolRuntimeSection
import os.kei.ui.page.main.mcp.section.McpToolSystemSection
import os.kei.ui.page.main.mcp.section.McpToolWorkflowSection
import os.kei.ui.page.main.mcp.state.McpPageOverviewState
import os.kei.ui.page.main.mcp.state.McpToolBuckets
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.appPageEdgePaddingStart
import os.kei.ui.page.main.widget.chrome.appPageEdgePaddingEnd
import os.kei.ui.page.main.widget.chrome.appPageBottomPaddingWithFloatingOverlay
import os.kei.ui.page.main.widget.chrome.rememberAppPullToRefreshState
import os.kei.ui.page.main.widget.glass.AppEdgeStackKeepAlive
import os.kei.ui.page.main.widget.glass.AppEdgeStackListTopInset
import os.kei.ui.page.main.widget.glass.LocalAppEdgeStackCards
import os.kei.ui.page.main.widget.glass.appEdgeStackKeepAliveTopPadding
import os.kei.ui.page.main.widget.glass.rememberAppEdgeStackState
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun McpPageContent(
    uiState: McpServerUiState,
    pageUiState: McpPageUiState,
    toolBuckets: McpToolBuckets,
    overviewState: McpPageOverviewState,
    runtime: MainPageRuntime,
    innerPadding: PaddingValues,
    listState: LazyListState,
    scrollBehavior: ScrollBehavior,
    backdrops: MainPageBackdropSet,
    backdropProducerActive: Boolean,
    titleColor: Color,
    subtitleColor: Color,
    isDark: Boolean,
    refreshRunning: Boolean,
    actions: McpPageActions,
) {
    val edgeStackState = rememberAppEdgeStackState(stackLine = AppEdgeStackListTopInset)
    MainPageContentBackdropScene(
        contentProducer = null,
        sheetProducer = backdrops.sheetProducer,
        producerActive = backdropProducerActive,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        // The MCP status hub stays pinned above the list; tool cards stack beneath it.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = appPageEdgePaddingStart(),
                        end = appPageEdgePaddingEnd(),
                        top = innerPadding.calculateTopPadding() + AppChromeTokens.topBarToHeaderGap,
                    ),
        ) {
            McpOverviewCardSection(
                backdrop = backdrops.contentMaterial,
                titleColor = titleColor,
                overviewCardColor = overviewState.overviewCardColor,
                overviewBorderColor = overviewState.overviewBorderColor,
                overviewAccentColor = overviewState.overviewAccentColor,
                runtimeText = overviewState.runtimeText,
                isDark = isDark,
                running = uiState.running,
                overviewPills = overviewState.overviewPills,
                onToggleServer = actions.onToggleServer,
                onOpenEditSheet = actions.onOpenEditSheet,
            )
        }
        CompositionLocalProvider(LocalAppEdgeStackCards provides edgeStackState) {
        PullToRefresh(
            isRefreshing = refreshRunning,
            onRefresh = actions.onRefreshNow,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .layerBackdrop(backdrops.topBarProducer),
            pullToRefreshState = rememberAppPullToRefreshState(),
            topAppBarScrollBehavior = scrollBehavior,
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
            refreshTexts =
                listOf(
                    stringResource(R.string.mcp_pull_refresh_pull),
                    stringResource(R.string.mcp_pull_refresh_release),
                    stringResource(R.string.mcp_pull_refresh_refreshing),
                    stringResource(R.string.mcp_pull_refresh_done),
                ),
        ) {
        // Inside PullToRefresh, so the RefreshHeader above keeps its own anchor — see OsPageMainList.
        AppEdgeStackKeepAlive(
            state = edgeStackState,
            modifier = Modifier.fillMaxSize(),
        ) {
        AppPageLazyColumn(
            innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            bottomExtra = appPageBottomPaddingWithFloatingOverlay(runtime.contentBottomPadding),
            topExtra = appEdgeStackKeepAliveTopPadding(AppEdgeStackListTopInset),
            sectionSpacing = 12.dp,
        ) {
            item(key = "mcp-onboarding-guide", contentType = "mcp_onboarding_guide_section") {
                McpOnboardingGuideSection(
                    backdrop = backdrops.contentMaterial,
                    expanded = pageUiState.onboardingExpanded,
                    onExpandedChange = actions.onOnboardingExpandedChange,
                    onCopyCurrentConfig = actions.onCopyCurrentConfig,
                    onCopySkillResource = actions.onCopySkillResource,
                    onCopySubAgentResource = actions.onCopySubAgentResource,
                    onCopyWorkflowResource = actions.onCopyWorkflowResource,
                )
            }
            item(key = "mcp-service-control", contentType = "mcp_service_control_section") {
                McpServiceControlSection(
                    backdrop = backdrops.contentMaterial,
                    expanded = pageUiState.controlExpanded,
                    contentVisible = true,
                    onExpandedChange = actions.onControlExpandedChange,
                    onSendTestNotification = actions.onSendTestNotification,
                    onShowResetConfigConfirm = actions.onShowResetConfigConfirm,
                    onCopySkillResource = actions.onCopySkillResource,
                    onCopyWorkflowResource = actions.onCopyWorkflowResource,
                )
            }
            item(key = "mcp-tool-entrypoints", contentType = "mcp_tool_entrypoints_section") {
                McpToolEntrypointsSection(
                    backdrop = backdrops.contentMaterial,
                    buckets = toolBuckets,
                    searchQuery = pageUiState.toolsSearchQuery,
                    onSearchQueryChange = actions.onToolsSearchQueryChange,
                    expanded = pageUiState.toolEntrypointsExpanded,
                    onExpandedChange = actions.onToolEntrypointsExpandedChange,
                )
            }
            item(key = "mcp-tool-runtime", contentType = "mcp_tool_runtime_section") {
                McpToolRuntimeSection(
                    backdrop = backdrops.contentMaterial,
                    tools = toolBuckets.runtimeTools,
                    searchQuery = pageUiState.toolsSearchQuery,
                    expanded = pageUiState.runtimeToolsExpanded,
                    onExpandedChange = actions.onRuntimeToolsExpandedChange,
                )
            }
            item(key = "mcp-tool-system", contentType = "mcp_tool_system_section") {
                McpToolSystemSection(
                    backdrop = backdrops.contentMaterial,
                    tools = toolBuckets.systemTools,
                    searchQuery = pageUiState.toolsSearchQuery,
                    expanded = pageUiState.systemToolsExpanded,
                    onExpandedChange = actions.onSystemToolsExpandedChange,
                )
            }
            item(key = "mcp-tool-github", contentType = "mcp_tool_github_section") {
                McpToolGithubSection(
                    backdrop = backdrops.contentMaterial,
                    tools = toolBuckets.githubTools,
                    searchQuery = pageUiState.toolsSearchQuery,
                    expanded = pageUiState.githubToolsExpanded,
                    onExpandedChange = actions.onGithubToolsExpandedChange,
                )
            }
            item(key = "mcp-tool-ba", contentType = "mcp_tool_ba_section") {
                McpToolBaSection(
                    backdrop = backdrops.contentMaterial,
                    tools = toolBuckets.baTools,
                    searchQuery = pageUiState.toolsSearchQuery,
                    expanded = pageUiState.baToolsExpanded,
                    onExpandedChange = actions.onBaToolsExpandedChange,
                )
            }
            item(key = "mcp-tool-codex", contentType = "mcp_tool_codex_section") {
                McpToolCodexSection(
                    backdrop = backdrops.contentMaterial,
                    tools = toolBuckets.codexTools,
                    searchQuery = pageUiState.toolsSearchQuery,
                    expanded = pageUiState.codexToolsExpanded,
                    onExpandedChange = actions.onCodexToolsExpandedChange,
                )
            }
            item(key = "mcp-tool-workflows", contentType = "mcp_tool_workflows_section") {
                McpToolWorkflowSection(
                    backdrop = backdrops.contentMaterial,
                    tools = toolBuckets.workflowTools,
                    searchQuery = pageUiState.toolsSearchQuery,
                    expanded = pageUiState.workflowToolsExpanded,
                    onExpandedChange = actions.onWorkflowToolsExpandedChange,
                )
            }
            if (toolBuckets.advancedTools.isNotEmpty()) {
                item(key = "mcp-tool-advanced", contentType = "mcp_tool_advanced_section") {
                    McpToolAdvancedSection(
                        backdrop = backdrops.contentMaterial,
                        tools = toolBuckets.advancedTools,
                        searchQuery = pageUiState.toolsSearchQuery,
                        expanded = pageUiState.advancedToolsExpanded,
                        onExpandedChange = actions.onAdvancedToolsExpandedChange,
                    )
                }
            }
            item(key = "mcp-logs", contentType = "mcp_logs_section") {
                McpLogsSection(
                    backdrop = backdrops.contentMaterial,
                    expanded = pageUiState.logsExpanded,
                    onExpandedChange = actions.onLogsExpandedChange,
                    uiState = uiState,
                    logsExporting = pageUiState.logsExporting,
                    onExportLogs = actions.onExportLogs,
                    onClearLogs = actions.onClearLogs,
                    subtitleColor = subtitleColor,
                )
            }
        }
        }
        }
        }
        }

        McpPageFloatingActionDock(
            backdrop = backdrops.topBar,
            uiState = uiState,
            runtime = runtime,
            actions = actions,
        )
    }
}
