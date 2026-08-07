@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import os.kei.R
import os.kei.core.ext.showToast
import os.kei.core.intent.SafeExternalIntents
import os.kei.core.ui.effect.rememberAppTopBarColor
import os.kei.feature.github.model.GitHubActionsNotificationHistoryRecord
import os.kei.ui.page.main.github.AppIconImage
import os.kei.ui.page.main.os.appLucideBackIcon
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucideHistoryIcon
import os.kei.ui.page.main.os.appLucideListIcon
import os.kei.ui.page.main.os.appLucideSearchIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppLiquidNavigationButton
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import os.kei.ui.page.main.widget.chrome.TabbedPageBottomChrome
import os.kei.ui.page.main.widget.chrome.appPageBottomPaddingWithFloatingOverlay
import os.kei.ui.page.main.widget.chrome.rememberAppPullToRefreshState
import os.kei.ui.page.main.widget.chrome.rememberTabbedPageChromeScrollState
import os.kei.ui.page.main.widget.chrome.rememberTabbedPageContentSwitchState
import os.kei.ui.page.main.widget.glass.AppEdgeStackListTopInset
import os.kei.ui.page.main.widget.glass.LocalAppEdgeStackCards
import os.kei.ui.page.main.widget.glass.appEdgeStackContainer
import os.kei.ui.page.main.widget.glass.rememberAppEdgeStackState
import os.kei.ui.page.main.widget.chrome.tabbedPageContentItemModifier
import os.kei.ui.page.main.widget.chrome.tabbedPageContentNestedScrollConnection
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppInfoRow
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.status.StatusPill
import os.kei.ui.testing.KeiOsTestTags
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun GitHubActionsNotificationHistoryPage(
    onBack: () -> Unit,
    onOpenTrackActions: (String) -> Unit,
    onRetryRefreshTargets: (List<String>) -> Unit,
    viewModel: GitHubActionsNotificationHistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appIconState by viewModel.appIconState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val pageBackdrop = rememberLayerBackdrop()
    val bottomBarBackdrop = rememberLayerBackdrop()
    val topBarColor = rememberAppTopBarColor(enableBackdropEffects = true)
    val categories = remember { GitHubHistoryMode.entries.toList() }
    val selectedHistoryPage = categories.indexOf(uiState.historyMode).coerceAtLeast(0)
    val historyContentSwitchState = rememberTabbedPageContentSwitchState(selectedHistoryPage)
    var bottomBarVisible by remember { mutableStateOf(true) }
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomChromeScrollState =
        rememberTabbedPageChromeScrollState(
            visible = bottomBarVisible,
            activeListStateProvider = { listState },
            onVisibleChange = { bottomBarVisible = it },
        )
    val pageNestedScrollConnection =
        remember(listState, bottomChromeScrollState, scrollBehavior.nestedScrollConnection) {
            tabbedPageContentNestedScrollConnection(
                listState = listState,
                chrome = bottomChromeScrollState.chromeNestedScrollConnection,
                delegate = scrollBehavior.nestedScrollConnection,
            )
        }
    val selectHistoryCategory =
        remember(categories, scope, listState, viewModel, bottomChromeScrollState) {
            { index: Int ->
                categories.getOrNull(index)?.let { mode ->
                    viewModel.setHistoryMode(mode)
                    bottomChromeScrollState.showNow()
                    scope.launch { listState.animateScrollToItem(0) }
                }
                Unit
            }
        }
    var expandedRecordKeys by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var expandedRefreshRecordKeys by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var expandedTrackChangeRecordKeys by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var expandedAppInstallRecordKeys by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showActionMenuPopup by rememberSaveable { mutableStateOf(false) }
    // The initial page load also raises uiState.loading, so the pull header keys on a
    // gesture-scoped session instead of the shared loading flag.
    var pullRefreshSessionActive by remember { mutableStateOf(false) }
    val historyLoadingNow by rememberUpdatedState(uiState.loading)
    LaunchedEffect(pullRefreshSessionActive) {
        if (!pullRefreshSessionActive) return@LaunchedEffect
        val refreshStarted =
            withTimeoutOrNull(GitHubHistoryPullRefreshStartTimeoutMs) {
                snapshotFlow { historyLoadingNow }.first { it }
            } != null
        if (refreshStarted) {
            snapshotFlow { historyLoadingNow }.first { !it }
        }
        pullRefreshSessionActive = false
    }
    val refreshHistoryExportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            viewModel.writePendingRefreshHistoryExport(
                contentResolver = context.contentResolver,
                uri = uri,
            )
        }
    val currentTotalRecordCount =
        when (uiState.historyMode) {
            GitHubHistoryMode.Refresh -> uiState.totalRefreshRecordCount
            GitHubHistoryMode.Actions -> uiState.totalRecordCount
            GitHubHistoryMode.Tracking -> uiState.totalTrackChangeRecordCount
            GitHubHistoryMode.Apps -> uiState.totalAppInstallRecordCount
        }
    val currentDisplayRecordCount =
        when (uiState.historyMode) {
            GitHubHistoryMode.Refresh -> uiState.refreshRecords.size
            GitHubHistoryMode.Actions -> uiState.records.size
            GitHubHistoryMode.Tracking -> uiState.trackChangeRecords.size
            GitHubHistoryMode.Apps -> uiState.appInstallRecords.size
        }
    val searchActive = uiState.searchQuery.trim().isNotEmpty()
    val iconPackageNames =
        remember(uiState.records, uiState.trackChangeRecords, uiState.appInstallRecords) {
            (uiState.records.map { it.packageName.trim() } +
                uiState.trackChangeRecords.map { it.record.packageName.trim() } +
                uiState.appInstallRecords.map { it.record.packageName.trim() })
                .filter { it.isNotBlank() }
                .distinct()
        }

    LaunchedEffect(uiState.records) {
        val activeKeys = uiState.records.map(::githubActionsHistoryRecordKey).toSet()
        expandedRecordKeys = expandedRecordKeys.filter { it in activeKeys }
    }

    LaunchedEffect(uiState.refreshRecords) {
        val activeKeys = uiState.refreshRecords.map(::githubRefreshHistoryRecordKey).toSet()
        expandedRefreshRecordKeys = expandedRefreshRecordKeys.filter { it in activeKeys }
    }

    LaunchedEffect(uiState.trackChangeRecords) {
        val activeKeys = uiState.trackChangeRecords.map(::githubTrackChangeHistoryRecordKey).toSet()
        expandedTrackChangeRecordKeys = expandedTrackChangeRecordKeys.filter { it in activeKeys }
    }

    LaunchedEffect(uiState.appInstallRecords) {
        val activeKeys = uiState.appInstallRecords.map(::githubAppInstallHistoryRecordKey).toSet()
        expandedAppInstallRecordKeys = expandedAppInstallRecordKeys.filter { it in activeKeys }
    }

    LaunchedEffect(context, iconPackageNames) {
        viewModel.requestAppIcons(context, iconPackageNames)
    }

    LaunchedEffect(context, lifecycleOwner, refreshHistoryExportLauncher, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is GitHubActionsNotificationHistoryEvent.LaunchRefreshHistoryExport ->
                        refreshHistoryExportLauncher.launch(event.fileName)

                    GitHubActionsNotificationHistoryEvent.RefreshHistoryExported ->
                        context.showToast(context.getString(R.string.common_export_success))

                    is GitHubActionsNotificationHistoryEvent.RefreshHistoryExportFailed ->
                        context.showToast(
                            context.getString(
                                R.string.common_export_failed_with_reason,
                                event.reason.ifBlank { context.getString(R.string.common_unknown) },
                            ),
                        )

                    is GitHubActionsNotificationHistoryEvent.RefreshRetryRequested -> {
                        context.showToast(
                            context.getString(
                                R.string.github_history_refresh_retry_requested,
                                event.targetIds.size,
                            ),
                        )
                        onRetryRefreshTargets(event.targetIds)
                    }

                    GitHubActionsNotificationHistoryEvent.RefreshRetryUnavailable ->
                        context.showToast(context.getString(R.string.github_history_refresh_retry_unavailable))

                    is GitHubActionsNotificationHistoryEvent.RefreshRetryRequestFailed ->
                        context.showToast(
                            context.getString(
                                R.string.github_history_refresh_retry_failed,
                                event.reason.ifBlank { context.getString(R.string.common_unknown) },
                            ),
                        )
                }
            }
        }
    }

    BackHandler(enabled = uiState.searchExpanded) {
        viewModel.setSearchExpanded(false)
    }

    LaunchedEffect(uiState.historyMode) {
        bottomChromeScrollState.showNow()
    }

    LaunchedEffect(searchActive, uiState.searchQuery) {
        if (searchActive) {
            listState.scrollToItem(0)
        }
    }

    AppPageScaffold(
        title = stringResource(R.string.github_history_title),
        modifier =
            Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
                .testTag(KeiOsTestTags.GitHubActionsHistoryPageRoot),
        scrollBehavior = scrollBehavior,
        topBarColor = topBarColor,
        titleBackdrop = pageBackdrop,
        onTitleClick = {
            scope.launch { listState.animateScrollToItem(0) }
        },
        navigationIcon = {
            AppLiquidNavigationButton(
                icon = appLucideBackIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onBack,
                backdrop = pageBackdrop,
            )
        },
        actions = {
            GitHubActionsNotificationHistoryActionBar(
                backdrop = pageBackdrop,
                loading = uiState.loading || uiState.exportInProgress,
                hasRecords = currentTotalRecordCount > 0,
                showActionMenuPopup = showActionMenuPopup,
                historyMode = uiState.historyMode,
                filterMode = uiState.filterMode,
                refreshFilterMode = uiState.refreshFilterMode,
                trackChangeFilterMode = uiState.trackChangeFilterMode,
                appInstallFilterMode = uiState.appInstallFilterMode,
                sortMode = uiState.sortMode,
                refreshSortMode = uiState.refreshSortMode,
                trackChangeSortMode = uiState.trackChangeSortMode,
                appInstallSortMode = uiState.appInstallSortMode,
                sortDirection = uiState.sortDirection,
                onRefresh = viewModel::refresh,
                onShowActionMenuPopupChange = { showActionMenuPopup = it },
                onFilterModeChange = viewModel::setFilterMode,
                onRefreshFilterModeChange = viewModel::setRefreshFilterMode,
                onTrackChangeFilterModeChange = viewModel::setTrackChangeFilterMode,
                onAppInstallFilterModeChange = viewModel::setAppInstallFilterMode,
                onSortModeChange = viewModel::setSortMode,
                onRefreshSortModeChange = viewModel::setRefreshSortMode,
                onTrackChangeSortModeChange = viewModel::setTrackChangeSortMode,
                onAppInstallSortModeChange = viewModel::setAppInstallSortMode,
                onSortDirectionChange = viewModel::setSortDirection,
                onCleanupAgeSelect = viewModel::pruneOlderThan,
                onExportRefreshHistory = viewModel::requestRefreshHistoryExport,
            )
        },
        reserveTopEndActionSpace = true,
        bottomBar = {
            TabbedPageBottomChrome(
                visible = bottomBarVisible,
                navigationBarBottom = navigationBarBottom,
                categories = categories,
                selectedPage = selectedHistoryPage,
                selectedPagePosition = null,
                selectedPageProvider = { selectedHistoryPage },
                searchExpanded = uiState.searchExpanded,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                onSearchExpandedChange = viewModel::setSearchExpanded,
                searchIcon = appLucideSearchIcon(),
                searchContentDescription = stringResource(R.string.github_history_search_placeholder),
                searchPlaceholder = stringResource(R.string.github_history_search_placeholder),
                backdrop = bottomBarBackdrop,
                isLiquidEffectEnabled = true,
                onSelectCategory = selectHistoryCategory,
                onExpandDock = {
                    bottomChromeScrollState.showNow()
                },
                labelPrefix = "github_history",
            )
        },
    ) { innerPadding ->
        val showPinnedHistoryHub = !uiState.loading && uiState.errorMessage.isBlank()
        val listTopPadding =
            if (showPinnedHistoryHub) {
                AppEdgeStackListTopInset
            } else {
                innerPadding.calculateTopPadding() + AppChromeTokens.topBarToHeaderGap
            }
        val edgeStackState = rememberAppEdgeStackState(stackLine = listTopPadding)
        Column(modifier = Modifier.fillMaxSize()) {
        if (showPinnedHistoryHub) {
            // The history summary hub stays pinned above every category list; record
            // cards stack beneath it while the pull gesture runs in the list region.
            GitHubHistoryOverviewCard(
                uiState = uiState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = AppChromeTokens.pageHorizontalPadding,
                            end = AppChromeTokens.pageHorizontalPadding,
                            top = innerPadding.calculateTopPadding() + AppChromeTokens.topBarToHeaderGap,
                        ),
            )
        }
        CompositionLocalProvider(LocalAppEdgeStackCards provides edgeStackState) {
        PullToRefresh(
            isRefreshing = pullRefreshSessionActive,
            onRefresh = {
                pullRefreshSessionActive = true
                viewModel.refresh()
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .layerBackdrop(pageBackdrop)
                    .layerBackdrop(bottomBarBackdrop),
            pullToRefreshState = rememberAppPullToRefreshState(),
            topAppBarScrollBehavior = scrollBehavior,
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
            refreshTexts =
                listOf(
                    stringResource(R.string.github_history_pull_refresh_pull),
                    stringResource(R.string.github_pull_refresh_release),
                    stringResource(R.string.github_pull_refresh_refreshing),
                    stringResource(R.string.github_pull_refresh_done),
                ),
        ) {
            AppPageLazyColumn(
                innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(pageNestedScrollConnection)
                        .appEdgeStackContainer(edgeStackState),
                bottomExtra =
                    appPageBottomPaddingWithFloatingOverlay(
                        AppChromeTokens.floatingBottomBarOuterHeight,
                    ),
                topExtra = listTopPadding,
                sectionSpacing = CardLayoutRhythm.denseSectionGap,
            ) {
                when {
                    uiState.loading -> {
                        item(
                            key = "github-actions-history-loading",
                            contentType = "github-actions-history-state",
                        ) {
                            GitHubActionsHistoryStateCard(
                                title = stringResource(R.string.github_actions_history_loading_title),
                                summary = stringResource(R.string.github_history_loading_summary),
                                modifier =
                                    tabbedPageContentItemModifier(
                                        switchState = historyContentSwitchState,
                                        itemIndex = 0,
                                    ),
                            )
                        }
                    }

                    uiState.errorMessage.isNotBlank() -> {
                        item(
                            key = "github-actions-history-error",
                            contentType = "github-actions-history-state",
                        ) {
                            GitHubActionsHistoryStateCard(
                                title = stringResource(R.string.github_actions_history_error_title),
                                summary =
                                    stringResource(
                                        R.string.github_actions_history_error_summary,
                                        uiState.errorMessage,
                                    ),
                                modifier =
                                    tabbedPageContentItemModifier(
                                        switchState = historyContentSwitchState,
                                        itemIndex = 0,
                                    ),
                            )
                        }
                    }

                    currentTotalRecordCount == 0 -> {
                        item(
                            key = "github-actions-history-empty",
                            contentType = "github-actions-history-state",
                        ) {
                            val emptyTitle =
                                when (uiState.historyMode) {
                                    GitHubHistoryMode.Refresh -> R.string.github_history_refresh_empty_title
                                    GitHubHistoryMode.Actions -> R.string.github_actions_history_empty_title
                                    GitHubHistoryMode.Tracking -> R.string.github_history_tracking_empty_title
                                    GitHubHistoryMode.Apps -> R.string.github_history_apps_empty_title
                                }
                            val emptySummary =
                                when (uiState.historyMode) {
                                    GitHubHistoryMode.Refresh -> R.string.github_history_refresh_empty_summary
                                    GitHubHistoryMode.Actions -> R.string.github_actions_history_empty_summary
                                    GitHubHistoryMode.Tracking -> R.string.github_history_tracking_empty_summary
                                    GitHubHistoryMode.Apps -> R.string.github_history_apps_empty_summary
                                }
                            GitHubActionsHistoryStateCard(
                                title = stringResource(emptyTitle),
                                summary = stringResource(emptySummary),
                                modifier =
                                    tabbedPageContentItemModifier(
                                        switchState = historyContentSwitchState,
                                        itemIndex = 1,
                                    ),
                            )
                        }
                    }

                    else -> {
                        if (currentDisplayRecordCount == 0) {
                            item(
                                key = "github-actions-history-filtered-empty",
                                contentType = "github-actions-history-state",
                            ) {
                                GitHubActionsHistoryStateCard(
                                    title =
                                        if (searchActive) {
                                            stringResource(R.string.common_no_matched_results)
                                        } else {
                                            stringResource(R.string.github_actions_history_empty_filtered_title)
                                        },
                                    summary =
                                        if (searchActive) {
                                            stringResource(R.string.github_history_empty_search_summary)
                                        } else {
                                            stringResource(R.string.github_actions_history_empty_filtered_summary)
                                        },
                                    modifier =
                                        tabbedPageContentItemModifier(
                                            switchState = historyContentSwitchState,
                                            itemIndex = 1,
                                        ),
                                )
                            }
                        }
                        when (uiState.historyMode) {
                            GitHubHistoryMode.Refresh -> {
                                itemsIndexed(
                                    items = uiState.refreshRecords,
                                    key = { _, item -> githubRefreshHistoryRecordKey(item) },
                                    contentType = { _, _ -> "github-refresh-history-record" },
                                ) { index, item ->
                                    val recordKey = githubRefreshHistoryRecordKey(item)
                                    val expanded = recordKey in expandedRefreshRecordKeys
                                    GitHubRefreshHistoryRecordCard(
                                        item = item,
                                        expanded = expanded,
                                        modifier =
                                            tabbedPageContentItemModifier(
                                                switchState = historyContentSwitchState,
                                                itemIndex = index + 1,
                                            ),
                                        onExpandedChange = { nextExpanded ->
                                            expandedRefreshRecordKeys =
                                                if (nextExpanded) {
                                                    (expandedRefreshRecordKeys + recordKey).distinct()
                                                } else {
                                                    expandedRefreshRecordKeys - recordKey
                                                }
                                        },
                                        onRetryRefreshTargets = { viewModel.requestRetryRefresh(item.record) },
                                    )
                                }
                            }
                            GitHubHistoryMode.Actions -> {
                                itemsIndexed(
                                    items = uiState.records,
                                    key = { _, item -> githubActionsHistoryRecordKey(item) },
                                    contentType = { _, _ -> "github-actions-history-record" },
                                ) { index, item ->
                                    val recordKey = githubActionsHistoryRecordKey(item)
                                    val expanded = recordKey in expandedRecordKeys
                                    GitHubActionsHistoryRecordCard(
                                        item = item,
                                        appIconBitmap = appIconState.bitmaps[item.packageName.trim()],
                                        expanded = expanded,
                                        modifier =
                                            tabbedPageContentItemModifier(
                                                switchState = historyContentSwitchState,
                                                itemIndex = index + 1,
                                            ),
                                        onExpandedChange = { nextExpanded ->
                                            expandedRecordKeys =
                                                if (nextExpanded) {
                                                    (expandedRecordKeys + recordKey).distinct()
                                                } else {
                                                    expandedRecordKeys - recordKey
                                                }
                                        },
                                        onOpenTrackActions = { onOpenTrackActions(item.record.trackId) },
                                    )
                                }
                            }
                            GitHubHistoryMode.Tracking -> {
                                itemsIndexed(
                                    items = uiState.trackChangeRecords,
                                    key = { _, item -> githubTrackChangeHistoryRecordKey(item) },
                                    contentType = { _, _ -> "github-track-change-history-record" },
                                ) { index, item ->
                                    val recordKey = githubTrackChangeHistoryRecordKey(item)
                                    val expanded = recordKey in expandedTrackChangeRecordKeys
                                    GitHubTrackChangeHistoryRecordCard(
                                        item = item,
                                        appIconBitmap = appIconState.bitmaps[item.record.packageName.trim()],
                                        expanded = expanded,
                                        modifier =
                                            tabbedPageContentItemModifier(
                                                switchState = historyContentSwitchState,
                                                itemIndex = index + 1,
                                            ),
                                        onExpandedChange = { nextExpanded ->
                                            expandedTrackChangeRecordKeys =
                                                if (nextExpanded) {
                                                    (expandedTrackChangeRecordKeys + recordKey).distinct()
                                                } else {
                                                    expandedTrackChangeRecordKeys - recordKey
                                                }
                                        },
                                    )
                                }
                            }
                            GitHubHistoryMode.Apps -> {
                                itemsIndexed(
                                    items = uiState.appInstallRecords,
                                    key = { _, item -> githubAppInstallHistoryRecordKey(item) },
                                    contentType = { _, _ -> "github-app-install-history-record" },
                                ) { index, item ->
                                    val recordKey = githubAppInstallHistoryRecordKey(item)
                                    val expanded = recordKey in expandedAppInstallRecordKeys
                                    GitHubAppInstallHistoryRecordCard(
                                        item = item,
                                        appIconBitmap = appIconState.bitmaps[item.record.packageName.trim()],
                                        expanded = expanded,
                                        modifier =
                                            tabbedPageContentItemModifier(
                                                switchState = historyContentSwitchState,
                                                itemIndex = index + 1,
                                            ),
                                        onExpandedChange = { nextExpanded ->
                                            expandedAppInstallRecordKeys =
                                                if (nextExpanded) {
                                                    (expandedAppInstallRecordKeys + recordKey).distinct()
                                                } else {
                                                    expandedAppInstallRecordKeys - recordKey
                                                }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        }
    }
}

@Composable
private fun GitHubActionsHistoryStateCard(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    AppFeatureCard(
        title = title,
        subtitle = summary,
        modifier = modifier,
        sectionIcon = appLucideHistoryIcon(),
        showIndication = false,
    ) {
    }
}

@Composable
private fun GitHubActionsHistoryRecordCard(
    item: GitHubActionsNotificationHistoryUiRecord,
    appIconBitmap: android.graphics.Bitmap?,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onExpandedChange: (Boolean) -> Unit,
    onOpenTrackActions: () -> Unit,
) {
    val context = LocalContext.current
    val record = item.record
    val packageName = item.packageName.trim()
    val repoLabel = record.repositoryLabel
    val workflowLabel =
        record.workflowName
            .ifBlank { record.workflowPath }
            .ifBlank { stringResource(R.string.common_na) }
    val runValue = rememberFullRunValue(record)
    val title = record.appLabel.ifBlank { repoLabel }
    val subtitle =
        record.notificationContent.ifBlank {
            stringResource(
                R.string.github_actions_history_record_summary,
                record.runLabel,
                workflowLabel,
            )
        }
    val notifiedAt = rememberHistoryDateTime(record.notifiedAtMillis)
    val checkedAt =
        if (record.checkedAtMillis > 0L) {
            rememberHistoryDateTime(record.checkedAtMillis)
        } else {
            stringResource(R.string.common_na)
        }
    val statusValue = rememberStatusValue(record)
    val statusPillLabel = rememberStatusPillLabel(record)
    val artifactValue =
        stringResource(
            R.string.github_actions_history_artifacts_value,
            record.androidArtifactCount,
            record.artifactCount,
        )
    val eventValue = rememberActionsEventLabel(record.event)
    val branchValue = record.headBranch.ifBlank { stringResource(R.string.common_na) }
    val commitValue = record.headSha.ifBlank { stringResource(R.string.common_na) }
    val openLinkFailed = stringResource(R.string.github_error_open_link)
    val openRun = {
        val runUrl = record.htmlUrl.trim()
        if (runUrl.isNotBlank() && !SafeExternalIntents.startBrowsableUrl(context, runUrl)) {
            context.showToast(openLinkFailed)
        }
    }

    AppFeatureCard(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        exportBackdropToContent = true,
        eyebrow =
            stringResource(
                R.string.github_actions_history_time_notified,
                notifiedAt,
            ),
        sectionStartAction = {
            AppIconImage(
                packageName = packageName,
                bitmap = appIconBitmap,
                size = 32.dp,
            )
        },
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        headerEndActions = {
            StatusPill(
                label = statusPillLabel,
                color = historyStatusColor(record),
                size = AppStatusPillSize.Compact,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.compactSectionGap),
        ) {
            AppInfoRow(
                label = stringResource(R.string.github_actions_history_label_repo),
                value = repoLabel,
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_actions_history_label_workflow),
                value = workflowLabel,
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_actions_history_label_run),
                value = runValue,
                stacked = true,
                valueTextAlign = TextAlign.Start,
                valueMaxLines = Int.MAX_VALUE,
                valueOverflow = TextOverflow.Clip,
            )
            AppInfoRow(
                label = stringResource(R.string.github_actions_history_label_branch),
                value = branchValue,
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_actions_history_label_event),
                value = eventValue,
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_actions_history_label_status),
                value = statusValue,
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
                valueColor = historyStatusColor(record),
            )
            AppInfoRow(
                label = stringResource(R.string.github_actions_history_label_artifacts),
                value = artifactValue,
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_actions_history_label_commit),
                value = commitValue,
                stacked = true,
                valueTextAlign = TextAlign.Start,
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_actions_history_label_checked),
                value = checkedAt,
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_actions_history_label_notified),
                value = notifiedAt,
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
            )
            GitHubActionsHistoryActionRow(
                runUrl = record.htmlUrl,
                onOpenTrackActions = onOpenTrackActions,
                onOpenRun = openRun,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GitHubActionsHistoryActionRow(
    runUrl: String,
    onOpenTrackActions: () -> Unit,
    onOpenRun: () -> Unit,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = CardLayoutRhythm.controlRowTextGap),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        AppStandaloneLiquidTextButton(
            text = stringResource(R.string.github_actions_history_action_open_actions),
            leadingIcon = appLucideListIcon(),
            variant = GlassVariant.Compact,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis,
            onClick = onOpenTrackActions,
            pressSafePadding = 0.dp,
        )
        if (runUrl.isNotBlank()) {
            AppStandaloneLiquidTextButton(
                text = stringResource(R.string.github_actions_history_action_open_run),
                leadingIcon = appLucideExternalLinkIcon(),
                variant = GlassVariant.Compact,
                textMaxLines = 1,
                textOverflow = TextOverflow.Ellipsis,
                onClick = onOpenRun,
                pressSafePadding = 0.dp,
            )
        }
    }
}

@Composable
private fun rememberStatusValue(record: GitHubActionsNotificationHistoryRecord): String {
    val status = rememberActionsStatusLabel(record.status)
    val conclusion = rememberActionsConclusionLabel(record.conclusion)
    return if (record.conclusion.isBlank()) {
        status
    } else {
        stringResource(R.string.github_actions_history_status_value, status, conclusion)
    }
}

@Composable
private fun rememberStatusPillLabel(record: GitHubActionsNotificationHistoryRecord): String {
    return if (record.conclusion.isBlank()) {
        rememberActionsStatusLabel(record.status)
    } else {
        rememberActionsConclusionLabel(record.conclusion)
    }
}

@Composable
private fun rememberFullRunValue(record: GitHubActionsNotificationHistoryRecord): String {
    val runName = record.runDisplayName.trim()
    val runLabel = record.runLabel.takeIf { it != "#0" }.orEmpty()
    val attempt =
        if (record.runAttempt > 1) {
            stringResource(R.string.github_actions_value_run_attempt, record.runAttempt)
        } else {
            ""
        }
    val parts =
        listOf(
            runLabel,
            runName,
            attempt,
        ).filter { it.isNotBlank() }
    return parts.joinToString(separator = " · ").ifBlank { stringResource(R.string.common_na) }
}

@Composable
private fun historyStatusColor(record: GitHubActionsNotificationHistoryRecord) =
    when {
        record.conclusion.equals("success", ignoreCase = true) -> ColorSuccess
        record.conclusion.equals("failure", ignoreCase = true) ||
            record.conclusion.equals("cancelled", ignoreCase = true) -> MiuixTheme.colorScheme.error
        record.status.equals("completed", ignoreCase = true) -> MiuixTheme.colorScheme.onBackground
        else -> MiuixTheme.colorScheme.primary
    }

@Composable
private fun rememberHistoryDateTime(millis: Long): String {
    val locale = Locale.getDefault()
    val formatter =
        androidx.compose.runtime.remember(locale) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", locale)
        }
    return formatter.format(Date(millis))
}

@Composable
private fun rememberActionsStatusLabel(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return when (normalized) {
        "completed" -> stringResource(R.string.github_actions_badge_completed)
        "in_progress" -> stringResource(R.string.github_actions_badge_running)
        "queued" -> stringResource(R.string.github_actions_badge_queued)
        "requested" -> stringResource(R.string.github_actions_history_status_requested)
        "waiting" -> stringResource(R.string.github_actions_history_status_waiting)
        "pending" -> stringResource(R.string.github_actions_history_status_pending)
        else -> value.ifBlank { stringResource(R.string.common_na) }
    }
}

@Composable
private fun rememberActionsConclusionLabel(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return when (normalized) {
        "success" -> stringResource(R.string.github_actions_badge_success)
        "failure" -> stringResource(R.string.github_actions_badge_failed)
        "cancelled" -> stringResource(R.string.github_actions_history_conclusion_cancelled)
        "skipped" -> stringResource(R.string.github_actions_history_conclusion_skipped)
        "neutral" -> stringResource(R.string.github_actions_history_conclusion_neutral)
        "timed_out" -> stringResource(R.string.github_actions_history_conclusion_timed_out)
        "action_required" -> stringResource(R.string.github_actions_history_conclusion_action_required)
        "startup_failure" -> stringResource(R.string.github_actions_history_conclusion_startup_failure)
        "stale" -> stringResource(R.string.github_actions_history_conclusion_stale)
        else -> value.ifBlank { stringResource(R.string.common_na) }
    }
}

@Composable
private fun rememberActionsEventLabel(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return when (normalized) {
        "push" -> stringResource(R.string.github_actions_history_event_push)
        "pull_request" -> stringResource(R.string.github_actions_history_event_pull_request)
        "pull_request_target" -> stringResource(R.string.github_actions_history_event_pull_request_target)
        "workflow_dispatch" -> stringResource(R.string.github_actions_history_event_workflow_dispatch)
        "schedule" -> stringResource(R.string.github_actions_history_event_schedule)
        "release" -> stringResource(R.string.github_actions_history_event_release)
        "repository_dispatch" -> stringResource(R.string.github_actions_history_event_repository_dispatch)
        "issues" -> stringResource(R.string.github_actions_history_event_issues)
        "issue_comment" -> stringResource(R.string.github_actions_history_event_issue_comment)
        else -> value.ifBlank { stringResource(R.string.common_na) }
    }
}

private fun githubActionsHistoryRecordKey(item: GitHubActionsNotificationHistoryUiRecord): String =
    githubActionsHistoryRecordKey(item.record)

private fun githubActionsHistoryRecordKey(record: GitHubActionsNotificationHistoryRecord): String =
    "${record.trackId}|${record.runId}|${record.runNumber}|${record.notifiedAtMillis}"

private fun githubRefreshHistoryRecordKey(item: GitHubRefreshHistoryUiRecord): String =
    item.record.id.ifBlank {
        "${item.record.sessionId}|${item.record.source}|${item.record.scope}|${item.record.startedAtMillis}"
    }

private fun githubTrackChangeHistoryRecordKey(item: GitHubTrackChangeHistoryUiRecord): String =
    item.record.id.ifBlank {
        "${item.record.trackId}|${item.record.action}|${item.record.changedAtMillis}"
    }

private fun githubAppInstallHistoryRecordKey(item: GitHubAppInstallHistoryUiRecord): String =
    item.record.id.ifBlank {
        "${item.record.trackId}|${item.record.action}|${item.record.packageName}|${item.record.changedAtMillis}"
    }

// Covers the frame gap before viewModel.refresh() raises the shared loading flag; a pull that
// never starts a reload ends here instead of pinning the header open.
private const val GitHubHistoryPullRefreshStartTimeoutMs = 4_000L

private val ColorSuccess = androidx.compose.ui.graphics.Color(0xFF22C55E)
