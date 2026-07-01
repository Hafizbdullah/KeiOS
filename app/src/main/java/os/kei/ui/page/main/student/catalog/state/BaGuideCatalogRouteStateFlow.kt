package os.kei.ui.page.main.student.catalog.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab

private data class BaGuideCatalogRouteCoreState(
    val catalogDataState: BaGuideCatalogDataUiState,
    val catalogListDerivedStates: Map<BaGuideCatalogTab, BaGuideCatalogListDerivedState>,
    val studentBgmListDerivedState: BaGuideStudentBgmListDerivedState,
    val memoryLobbyListDerivedState: BaGuideMemoryLobbyListDerivedState,
    val favoriteBgmListDerivedState: BaGuideFavoriteBgmListDerivedState,
    val studentBgmDisplayedDerivedState: BaGuideStudentBgmDisplayedDerivedState,
)

private data class BaGuideCatalogRouteListState(
    val catalogDataState: BaGuideCatalogDataUiState,
    val catalogListDerivedStates: Map<BaGuideCatalogTab, BaGuideCatalogListDerivedState>,
    val studentBgmListDerivedState: BaGuideStudentBgmListDerivedState,
    val memoryLobbyListDerivedState: BaGuideMemoryLobbyListDerivedState,
    val favoriteBgmListDerivedState: BaGuideFavoriteBgmListDerivedState,
)

private data class BaGuideCatalogRouteLibraryState(
    val core: BaGuideCatalogRouteCoreState,
    val catalogFavoriteEntries: Map<Long, Long>,
    val favoriteBgms: List<GuideBgmFavoriteItem>,
    val bgmCacheSnapshot: BaGuideFavoriteBgmCacheSnapshot,
    val favoriteBgmOfflineCacheState: BaGuideFavoriteBgmOfflineCacheUiState,
)

private data class BaGuideCatalogRouteSettingsState(
    val library: BaGuideCatalogRouteLibraryState,
    val nativeBgmMediaNotificationEnabled: Boolean,
    val mediaAdaptiveRotationEnabled: Boolean,
)

internal fun buildBaGuideCatalogRouteStateFlow(
    scope: CoroutineScope,
    dataState: StateFlow<BaGuideCatalogDataUiState>,
    catalogListDerivedStates: StateFlow<Map<BaGuideCatalogTab, BaGuideCatalogListDerivedState>>,
    studentBgmListDerivedState: StateFlow<BaGuideStudentBgmListDerivedState>,
    memoryLobbyListDerivedState: StateFlow<BaGuideMemoryLobbyListDerivedState>,
    favoriteBgmListDerivedState: StateFlow<BaGuideFavoriteBgmListDerivedState>,
    studentBgmDisplayedDerivedState: StateFlow<BaGuideStudentBgmDisplayedDerivedState>,
    catalogFavoriteEntries: StateFlow<Map<Long, Long>>,
    favoriteBgms: StateFlow<List<GuideBgmFavoriteItem>>,
    bgmCacheSnapshot: StateFlow<BaGuideFavoriteBgmCacheSnapshot>,
    favoriteBgmOfflineCacheState: StateFlow<BaGuideFavoriteBgmOfflineCacheUiState>,
    nativeBgmMediaNotificationEnabled: StateFlow<Boolean>,
    mediaAdaptiveRotationEnabled: StateFlow<Boolean>,
    transferSettings: StateFlow<BaGuideCatalogTransferSettingsUiState>,
): StateFlow<BaGuideCatalogRouteState> {
    val routeListState =
        combine(
            dataState,
            catalogListDerivedStates,
            studentBgmListDerivedState,
            memoryLobbyListDerivedState,
            favoriteBgmListDerivedState,
        ) {
                catalogDataState,
                catalogListDerivedStates,
                studentBgmListDerivedState,
                memoryLobbyListDerivedState,
                favoriteBgmListDerivedState,
            ->
            BaGuideCatalogRouteListState(
                catalogDataState = catalogDataState,
                catalogListDerivedStates = catalogListDerivedStates,
                studentBgmListDerivedState = studentBgmListDerivedState,
                memoryLobbyListDerivedState = memoryLobbyListDerivedState,
                favoriteBgmListDerivedState = favoriteBgmListDerivedState,
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue =
                BaGuideCatalogRouteListState(
                    catalogDataState = dataState.value,
                    catalogListDerivedStates = catalogListDerivedStates.value,
                    studentBgmListDerivedState = studentBgmListDerivedState.value,
                    memoryLobbyListDerivedState = memoryLobbyListDerivedState.value,
                    favoriteBgmListDerivedState = favoriteBgmListDerivedState.value,
                ),
        )

    val routeCoreState =
        combine(
            routeListState,
            studentBgmDisplayedDerivedState,
        ) { listState, studentBgmDisplayedDerivedState ->
            BaGuideCatalogRouteCoreState(
                catalogDataState = listState.catalogDataState,
                catalogListDerivedStates = listState.catalogListDerivedStates,
                studentBgmListDerivedState = listState.studentBgmListDerivedState,
                memoryLobbyListDerivedState = listState.memoryLobbyListDerivedState,
                favoriteBgmListDerivedState = listState.favoriteBgmListDerivedState,
                studentBgmDisplayedDerivedState = studentBgmDisplayedDerivedState,
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue =
                BaGuideCatalogRouteCoreState(
                    catalogDataState = routeListState.value.catalogDataState,
                    catalogListDerivedStates = routeListState.value.catalogListDerivedStates,
                    studentBgmListDerivedState = routeListState.value.studentBgmListDerivedState,
                    memoryLobbyListDerivedState = routeListState.value.memoryLobbyListDerivedState,
                    favoriteBgmListDerivedState = routeListState.value.favoriteBgmListDerivedState,
                    studentBgmDisplayedDerivedState = studentBgmDisplayedDerivedState.value,
                ),
        )

    val routeLibraryState =
        combine(
            routeCoreState,
            catalogFavoriteEntries,
            favoriteBgms,
            bgmCacheSnapshot,
            favoriteBgmOfflineCacheState,
        ) { core, catalogFavoriteEntries, favoriteBgms, bgmCacheSnapshot, favoriteBgmOfflineCacheState ->
            BaGuideCatalogRouteLibraryState(
                core = core,
                catalogFavoriteEntries = catalogFavoriteEntries,
                favoriteBgms = favoriteBgms,
                bgmCacheSnapshot = bgmCacheSnapshot,
                favoriteBgmOfflineCacheState = favoriteBgmOfflineCacheState,
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue =
                BaGuideCatalogRouteLibraryState(
                    core = routeCoreState.value,
                    catalogFavoriteEntries = catalogFavoriteEntries.value,
                    favoriteBgms = favoriteBgms.value,
                    bgmCacheSnapshot = bgmCacheSnapshot.value,
                    favoriteBgmOfflineCacheState = favoriteBgmOfflineCacheState.value,
                ),
        )

    val routeSettingsState =
        combine(
            routeLibraryState,
            nativeBgmMediaNotificationEnabled,
            mediaAdaptiveRotationEnabled,
        ) { libraryState, nativeBgmMediaNotificationEnabled, mediaAdaptiveRotationEnabled ->
            BaGuideCatalogRouteSettingsState(
                library = libraryState,
                nativeBgmMediaNotificationEnabled = nativeBgmMediaNotificationEnabled,
                mediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled,
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue =
                BaGuideCatalogRouteSettingsState(
                    library = routeLibraryState.value,
                    nativeBgmMediaNotificationEnabled = nativeBgmMediaNotificationEnabled.value,
                    mediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled.value,
                ),
        )

    return combine(
        routeSettingsState,
        transferSettings,
    ) { settingsState, transferSettings ->
        val libraryState = settingsState.library
        BaGuideCatalogRouteState(
            catalogDataState = libraryState.core.catalogDataState,
            catalogListDerivedStates = libraryState.core.catalogListDerivedStates,
            studentBgmListDerivedState = libraryState.core.studentBgmListDerivedState,
            memoryLobbyListDerivedState = libraryState.core.memoryLobbyListDerivedState,
            favoriteBgmListDerivedState = libraryState.core.favoriteBgmListDerivedState,
            studentBgmDisplayedDerivedState = libraryState.core.studentBgmDisplayedDerivedState,
            catalogFavoriteEntries = libraryState.catalogFavoriteEntries,
            favoriteBgms = libraryState.favoriteBgms,
            bgmCacheSnapshot = libraryState.bgmCacheSnapshot,
            favoriteBgmOfflineCacheState = libraryState.favoriteBgmOfflineCacheState,
            nativeBgmMediaNotificationEnabled = settingsState.nativeBgmMediaNotificationEnabled,
            mediaAdaptiveRotationEnabled = settingsState.mediaAdaptiveRotationEnabled,
            transferSettings = transferSettings,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue =
            BaGuideCatalogRouteState(
                catalogDataState = routeCoreState.value.catalogDataState,
                catalogListDerivedStates = routeCoreState.value.catalogListDerivedStates,
                studentBgmListDerivedState = routeCoreState.value.studentBgmListDerivedState,
                memoryLobbyListDerivedState = routeCoreState.value.memoryLobbyListDerivedState,
                favoriteBgmListDerivedState = routeCoreState.value.favoriteBgmListDerivedState,
                studentBgmDisplayedDerivedState = routeCoreState.value.studentBgmDisplayedDerivedState,
                catalogFavoriteEntries = catalogFavoriteEntries.value,
                favoriteBgms = favoriteBgms.value,
                bgmCacheSnapshot = bgmCacheSnapshot.value,
                favoriteBgmOfflineCacheState = favoriteBgmOfflineCacheState.value,
                nativeBgmMediaNotificationEnabled = nativeBgmMediaNotificationEnabled.value,
                mediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled.value,
                transferSettings = transferSettings.value,
            ),
    )
}
