@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import os.kei.R
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.state.BaGuideMemoryLobbyListDerivedState
import os.kei.ui.page.main.student.catalog.state.rememberBaGuideCatalogTabListState
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppAronaLoadingPanel
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.glass.LiquidInfoBlock
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val MEMORY_LOBBY_ENTRY_START_INDEX = 1

@Composable
internal fun BaGuideMemoryLobbyTabContent(
    catalogSyncedAtMs: Long,
    derivedState: BaGuideMemoryLobbyListDerivedState,
    favoriteCatalogEntries: Map<Long, Long>,
    searchQuery: String,
    loading: Boolean,
    error: String?,
    innerPadding: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    accent: Color,
    isPageActive: Boolean,
    scrollToTopSignal: Int,
    mediaAdaptiveRotationEnabled: Boolean,
    onScrollBoundsChange: (canScrollBackward: Boolean, canScrollForward: Boolean) -> Unit,
    onRequestVisibleImages: (List<String>) -> Unit,
    onOpenGuide: (String) -> Unit,
    onRequestGuideDetailTab: (String, GuideBottomTab) -> Unit,
    onToggleFavorite: (Long) -> Unit,
) {
    val pageScope = rememberCoroutineScope()
    val lookupCoordinator =
        remember(pageScope) {
            BaGuideMemoryLobbyLookupCoordinator(scope = pageScope)
        }
    val lookupStates by lookupCoordinator.states.collectAsStateWithLifecycle()
    val requestVisibleImages by rememberUpdatedState(onRequestVisibleImages)
    val allStudentEntries = derivedState.allStudentEntries
    val filteredEntries = derivedState.filteredEntries
    val effectiveLoading = loading || (derivedState.deriving && allStudentEntries.isEmpty())
    val listStateHolder =
        rememberBaGuideCatalogTabListState(
            tab = BaGuideCatalogTab.Student,
            filteredEntries = filteredEntries,
            loading = effectiveLoading,
            isPageActive = isPageActive,
        )
    val listState = listStateHolder.listState
    val displayedEntries = listStateHolder.displayedEntries
    val showError = !error.isNullOrBlank()
    val showLoading = effectiveLoading && allStudentEntries.isEmpty()
    val showEmpty = !effectiveLoading && filteredEntries.isEmpty()
    val entryStartIndex = if (showError) MEMORY_LOBBY_ENTRY_START_INDEX + 1 else MEMORY_LOBBY_ENTRY_START_INDEX
    val snapshotFlowManager = rememberAppSnapshotFlowManager()
    var consumedScrollToTopSignal by remember { mutableStateOf(0) }
    var expandedContentIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    LaunchedEffect(catalogSyncedAtMs) {
        lookupCoordinator.clear()
    }
    LaunchedEffect(filteredEntries) {
        val visibleContentIds = filteredEntries.mapTo(mutableSetOf()) { entry -> entry.contentId }
        expandedContentIds = expandedContentIds.filter { contentId -> contentId in visibleContentIds }.toSet()
    }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > consumedScrollToTopSignal && isPageActive) {
            consumedScrollToTopSignal = scrollToTopSignal
            listState.animateScrollToItem(0)
        } else {
            consumedScrollToTopSignal = scrollToTopSignal
        }
    }
    LaunchedEffect(listState, isPageActive, snapshotFlowManager) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlowManager
            .snapshotFlow {
                listState.canScrollBackward to listState.canScrollForward
            }.distinctUntilChanged()
            .collect { (canScrollBackward, canScrollForward) ->
                onScrollBoundsChange(canScrollBackward, canScrollForward)
            }
    }
    LaunchedEffect(
        listState,
        displayedEntries,
        isPageActive,
        showLoading,
        showEmpty,
        entryStartIndex,
        snapshotFlowManager,
        lookupCoordinator,
    ) {
        if (!isPageActive || showLoading || showEmpty || displayedEntries.isEmpty()) {
            return@LaunchedEffect
        }
        snapshotFlowManager
            .snapshotFlow {
                listState.layoutInfo.visibleItemsInfo.map { item -> item.index }
            }.distinctUntilChanged()
            .collect { visibleItemIndices ->
                val imageUrls =
                    buildBaGuideCatalogVisibleImageRequestUrls(
                        displayedEntries = displayedEntries,
                        visibleItemIndices = visibleItemIndices,
                        entryStartIndex = entryStartIndex,
                    )
                requestVisibleImages(imageUrls)
                val prewarmEntries =
                    buildBaGuideStudentBgmVisiblePrewarmEntries(
                        displayedEntries = displayedEntries,
                        visibleItemIndices = visibleItemIndices,
                        entryStartIndex = entryStartIndex,
                        limit = 6,
                    )
                lookupCoordinator.prewarmCached(prewarmEntries)
                lookupCoordinator.prewarmVisibleNetwork(prewarmEntries)
            }
    }
    LazyColumn(
        state = listState,
        modifier =
            Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
        contentPadding =
            PaddingValues(
                top = innerPadding.calculateTopPadding() + AppChromeTokens.topBarToHeaderGap,
                bottom = innerPadding.calculateBottomPadding() + AppChromeTokens.pageSectionGap,
                start = AppChromeTokens.pageHorizontalPadding,
                end = AppChromeTokens.pageHorizontalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showError) {
            item(
                key = "memory-lobby-error",
                contentType = "memory_lobby_status",
            ) {
                LiquidInfoBlock(
                    backdrop = null,
                    title = stringResource(R.string.ba_catalog_sync_status_title),
                    subtitle = error.orEmpty(),
                    body = stringResource(R.string.ba_catalog_sync_status_body_retry),
                    accent = Color(0xFFEF4444),
                )
            }
        }
        if (showLoading) {
            item(
                key = "memory-lobby-loading",
                contentType = "memory_lobby_status",
            ) {
                AppAronaLoadingPanel(accent = accent)
            }
        } else {
            item(
                key = "memory-lobby-header",
                contentType = "memory_lobby_header",
            ) {
                BaGuideMemoryLobbyHeader(
                    totalCount = allStudentEntries.size,
                    displayedCount = filteredEntries.size,
                    readyCount = filteredEntries.count { entry ->
                        lookupStates[entry.contentId] is BaGuideMemoryLobbyLookupState.Ready
                    },
                    favoriteCount = allStudentEntries.count { entry ->
                        favoriteCatalogEntries.containsKey(entry.contentId)
                    },
                    cachedCount = filteredEntries.count { entry ->
                        (lookupStates[entry.contentId] as? BaGuideMemoryLobbyLookupState.Ready)?.item?.fromCache == true
                    },
                    searchActive = searchQuery.isNotBlank(),
                    accent = accent,
                )
            }
        }

        if (showEmpty) {
            item(
                key = "memory-lobby-empty",
                contentType = "memory_lobby_status",
            ) {
                LiquidInfoBlock(
                    backdrop = null,
                    title = stringResource(R.string.ba_catalog_empty_title),
                    subtitle =
                        stringResource(
                            if (searchQuery.isNotBlank()) {
                                R.string.ba_catalog_empty_subtitle_search
                            } else {
                                R.string.ba_catalog_empty_subtitle_default
                            },
                        ),
                    accent = accent,
                )
            }
        } else if (!showLoading) {
            items(
                items = displayedEntries,
                key = { entry -> "memory-lobby-${entry.contentId}" },
                contentType = { "memory_lobby_entry" },
            ) { entry ->
                val expanded = entry.contentId in expandedContentIds
                BaGuideMemoryLobbyCard(
                    entry = entry,
                    lookupState = lookupStates[entry.contentId] ?: BaGuideMemoryLobbyLookupState.Idle,
                    expanded = expanded,
                    favorite = favoriteCatalogEntries.containsKey(entry.contentId),
                    accent = accent,
                    mediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled,
                    onToggleExpanded = {
                        expandedContentIds =
                            if (expanded) {
                                expandedContentIds - entry.contentId
                            } else {
                                expandedContentIds + entry.contentId
                            }
                    },
                    onResolve = {
                        lookupCoordinator.resolveEntry(
                            entry = entry,
                            allowNetwork = true,
                        )
                    },
                    onOpenGuide = {
                        onRequestGuideDetailTab(entry.detailUrl, GuideBottomTab.Gallery)
                        onOpenGuide(entry.detailUrl)
                    },
                    onToggleFavorite = { onToggleFavorite(entry.contentId) },
                )
            }

            if (listStateHolder.hasMoreEntries) {
                item(
                    key = "memory-lobby-loading-more",
                    contentType = "memory_lobby_loading_more",
                ) {
                    BaGuideCatalogLoadingMoreRow(
                        loadingMoreText = stringResource(R.string.ba_catalog_loading_more),
                        accent = accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun BaGuideCatalogLoadingMoreRow(
    loadingMoreText: String,
    accent: Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiquidCircularProgressBar(
            progress = { 0.3f },
            size = 16.dp,
            strokeWidth = 2.dp,
            activeColor = accent,
            inactiveColor = accent.copy(alpha = 0.30f),
        )
        Text(
            text = loadingMoreText,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
}
