@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.flow.distinctUntilChanged
import os.kei.R
import os.kei.core.ui.snapshot.rememberAppSnapshotFlowManager
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.catalog.state.BaGuideCatalogFilterSortState
import os.kei.ui.page.main.student.catalog.state.BaGuideCatalogListDerivedState
import os.kei.ui.page.main.student.catalog.state.rememberBaGuideCatalogTabContentUiState
import os.kei.ui.page.main.student.catalog.state.rememberBaGuideCatalogTabListState
import os.kei.ui.page.main.student.catalog.state.visibleCatalogEntriesWithFavoriteVisibility
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppAronaLoadingPanel
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.glass.AppEdgeStackListTopInset
import os.kei.ui.page.main.widget.glass.LiquidInfoBlock
import os.kei.ui.page.main.widget.glass.LocalAppEdgeStackCards
import os.kei.ui.page.main.widget.glass.appEdgeStackContainer
import os.kei.ui.page.main.widget.glass.rememberAppEdgeStackState
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaGuideCatalogV2ListContent(
    tab: BaGuideCatalogTab,
    catalogSceneBackdrop: Backdrop,
    filterSortState: BaGuideCatalogFilterSortState,
    derivedState: BaGuideCatalogListDerivedState,
    favoriteCatalogEntries: Map<Long, Long>,
    searchQuery: String,
    loading: Boolean,
    error: String?,
    accent: Color,
    innerPadding: PaddingValues,
    nestedScrollConnection: NestedScrollConnection,
    isPageActive: Boolean,
    scrollToTopSignal: Int,
    onScrollBoundsChange: (canScrollBackward: Boolean, canScrollForward: Boolean) -> Unit,
    onRequestVisibleImages: (List<String>) -> Unit,
    onOpenGuide: (String) -> Unit,
    onToggleFavorite: (Long) -> Unit,
) {
    var favoritesHidden by rememberSaveable(tab) { mutableStateOf(false) }
    val studentFavoriteHeaderVisible = tab == BaGuideCatalogTab.Student
    val visibleFilteredEntries =
        remember(tab, derivedState.filteredEntries, favoriteCatalogEntries, favoritesHidden) {
            if (studentFavoriteHeaderVisible) {
                visibleCatalogEntriesWithFavoriteVisibility(
                    filteredEntries = derivedState.filteredEntries,
                    favoriteCatalogEntries = favoriteCatalogEntries,
                    favoritesHidden = favoritesHidden,
                )
            } else {
                derivedState.filteredEntries
            }
        }
    val favoriteCount =
        remember(derivedState.filteredEntries, favoriteCatalogEntries, studentFavoriteHeaderVisible) {
            if (studentFavoriteHeaderVisible) {
                derivedState.filteredEntries.count { entry -> favoriteCatalogEntries.containsKey(entry.contentId) }
            } else {
                0
            }
        }
    val effectiveLoading = loading || (derivedState.deriving && derivedState.filteredEntries.isEmpty())
    val tabListState =
        rememberBaGuideCatalogTabListState(
            tab = tab,
            filteredEntries = visibleFilteredEntries,
            loading = effectiveLoading,
            isPageActive = isPageActive,
        )
    val uiState =
        rememberBaGuideCatalogTabContentUiState(
            tab = tab,
            searchQuery = searchQuery,
            activeFilterCount = derivedState.activeFilterCount,
            loading = effectiveLoading,
            error = error,
            filteredEntriesEmpty = tabListState.filteredEntries.isEmpty(),
        )
    val snapshotFlowManager = rememberAppSnapshotFlowManager()
    val requestVisibleImages by rememberUpdatedState(onRequestVisibleImages)
    val consumedScrollToTopSignal = remember(tab) { mutableIntStateOf(0) }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > consumedScrollToTopSignal.intValue && isPageActive) {
            consumedScrollToTopSignal.intValue = scrollToTopSignal
            tabListState.listState.animateScrollToItem(0)
        } else {
            consumedScrollToTopSignal.intValue = scrollToTopSignal
        }
    }
    LaunchedEffect(tabListState.listState, isPageActive, snapshotFlowManager) {
        if (!isPageActive) return@LaunchedEffect
        snapshotFlowManager
            .snapshotFlow {
                tabListState.listState.canScrollBackward to tabListState.listState.canScrollForward
            }.distinctUntilChanged()
            .collect { (canScrollBackward, canScrollForward) ->
                onScrollBoundsChange(canScrollBackward, canScrollForward)
            }
    }
    LaunchedEffect(
        tabListState.listState,
        tabListState.displayedEntries,
        uiState.showError,
        uiState.showLoading,
        uiState.showEmpty,
        isPageActive,
        snapshotFlowManager,
    ) {
        if (!isPageActive || uiState.showLoading || uiState.showEmpty || tabListState.displayedEntries.isEmpty()) {
            return@LaunchedEffect
        }
        val entryStartIndex = if (uiState.showError) 1 else 0
        snapshotFlowManager
            .snapshotFlow {
                val visibleItems = tabListState.listState.layoutInfo.visibleItemsInfo
                buildBaGuideCatalogVisibleImageRequestUrls(
                    displayedEntries = tabListState.displayedEntries,
                    visibleItemRange =
                        BaGuideVisibleItemRange(
                            firstItemIndex = visibleItems.firstOrNull()?.index ?: -1,
                            lastItemIndex = visibleItems.lastOrNull()?.index ?: -1,
                            visibleItemCount = visibleItems.size,
                        ),
                    entryStartIndex = entryStartIndex,
                )
            }.distinctUntilChanged()
            .collect { imageUrls ->
                if (imageUrls.isNotEmpty()) {
                    requestVisibleImages(imageUrls)
                }
            }
    }
    val entryListGap = rememberBaGuideCatalogEntryListGap()
    val showPinnedFavoritesHeader = studentFavoriteHeaderVisible && !uiState.showLoading
    val listTopPadding =
        if (showPinnedFavoritesHeader) {
            AppEdgeStackListTopInset
        } else {
            innerPadding.calculateTopPadding()
        }
    val edgeStackState = rememberAppEdgeStackState(stackLine = listTopPadding)
    Column(modifier = Modifier.fillMaxSize()) {
    if (showPinnedFavoritesHeader) {
        // The favorites summary is the catalog's status hub: pinned above the list so
        // the entry pile always forms beneath it.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppChromeTokens.pageHorizontalPadding,
                        end = AppChromeTokens.pageHorizontalPadding,
                        top = innerPadding.calculateTopPadding(),
                    ),
        ) {
            BaGuideCatalogFavoriteVisibilityHeader(
                totalCount = derivedState.filteredEntries.size,
                favoriteCount = favoriteCount,
                favoritesHidden = favoritesHidden,
                onToggleFavoritesHidden = {
                    if (favoriteCount > 0) {
                        favoritesHidden = !favoritesHidden
                    }
                },
            )
        }
    }
    CompositionLocalProvider(LocalAppEdgeStackCards provides edgeStackState) {
    LazyColumn(
        state = tabListState.listState,
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(nestedScrollConnection)
                .appEdgeStackContainer(edgeStackState),
        contentPadding =
            PaddingValues(
                top = listTopPadding,
                bottom = innerPadding.calculateBottomPadding() + AppChromeTokens.pageSectionGap,
                start = AppChromeTokens.pageHorizontalPadding,
                end = AppChromeTokens.pageHorizontalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(entryListGap),
    ) {
        if (uiState.showError) {
            item(
                key = "ba-guide-catalog-error-${tab.name}",
                contentType = "ba_guide_catalog_status",
            ) {
                LiquidInfoBlock(
                    backdrop = catalogSceneBackdrop,
                    title = uiState.syncStatusTitle,
                    subtitle = uiState.errorText,
                    body = uiState.syncStatusBody,
                    accent = Color(0xFFEF4444),
                )
            }
        }
        if (uiState.showLoading) {
            item(
                key = "ba-guide-catalog-loading-${tab.name}",
                contentType = "ba_guide_catalog_loading",
            ) {
                AppAronaLoadingPanel(accent = accent)
            }
        }
        if (uiState.showEmpty) {
            item(
                key = "ba-guide-catalog-empty-${tab.name}",
                contentType = "ba_guide_catalog_status",
            ) {
                LiquidInfoBlock(
                    backdrop = catalogSceneBackdrop,
                    title = uiState.emptyTitle,
                    subtitle = uiState.emptySubtitle,
                    accent = accent,
                )
            }
        } else if (!uiState.showLoading) {
            renderBaGuideCatalogEntryListAdapter(
                displayedEntries = tabListState.displayedEntries,
                hasMoreEntries = tabListState.hasMoreEntries,
                favoriteCatalogEntries = favoriteCatalogEntries,
                accent = accent,
                loadingMoreText = uiState.loadingMoreText,
                onOpenGuide = onOpenGuide,
                onToggleFavorite = onToggleFavorite,
            )
        }
    }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BaGuideCatalogFavoriteVisibilityHeader(
    totalCount: Int,
    favoriteCount: Int,
    favoritesHidden: Boolean,
    onToggleFavoritesHidden: () -> Unit,
) {
    val hasFavorites = favoriteCount > 0
    AppSurfaceCard(
        containerColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.62f),
        borderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
        showIndication = hasFavorites,
        onClick = if (hasFavorites) onToggleFavoritesHidden else null,
    ) {
        FlowRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            maxItemsInEachRow = 2,
        ) {
            BaGuideCatalogFavoriteMetricPill(
                label = stringResource(R.string.ba_catalog_student_bgm_metric_students),
                value = totalCount,
                color = Color(0xFF6366F1),
            )
            BaGuideCatalogFavoriteMetricPill(
                label = stringResource(R.string.ba_catalog_student_bgm_metric_favorites),
                value = favoriteCount,
                color =
                    if (favoritesHidden) {
                        MiuixTheme.colorScheme.onBackgroundVariant
                    } else {
                        Color(0xFFEC4899)
                    },
            )
        }
    }
}

@Composable
private fun BaGuideCatalogFavoriteMetricPill(
    label: String,
    value: Int,
    color: Color,
) {
    StatusPill(
        label = "$label ${value.coerceAtLeast(0)}",
        color = color,
        size = AppStatusPillSize.Compact,
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 3.dp),
    )
}
