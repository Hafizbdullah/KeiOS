package os.kei.ui.page.main.student.catalog.state

import androidx.compose.runtime.Stable
import os.kei.ui.page.main.ba.support.BA_NATIVE_BGM_MEDIA_NOTIFICATION_DEFAULT
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab

@Stable
internal data class BaGuideCatalogRouteState(
    val catalogDataState: BaGuideCatalogDataUiState = BaGuideCatalogDataUiState(),
    val catalogListDerivedStates: Map<BaGuideCatalogTab, BaGuideCatalogListDerivedState> = emptyMap(),
    val studentBgmListDerivedState: BaGuideStudentBgmListDerivedState = BaGuideStudentBgmListDerivedState.Empty,
    val memoryLobbyListDerivedState: BaGuideMemoryLobbyListDerivedState = BaGuideMemoryLobbyListDerivedState.Empty,
    val favoriteBgmListDerivedState: BaGuideFavoriteBgmListDerivedState = BaGuideFavoriteBgmListDerivedState.Empty,
    val studentBgmDisplayedDerivedState: BaGuideStudentBgmDisplayedDerivedState =
        BaGuideStudentBgmDisplayedDerivedState.Empty,
    val catalogFavoriteEntries: Map<Long, Long> = emptyMap(),
    val favoriteBgms: List<GuideBgmFavoriteItem> = emptyList(),
    val bgmCacheSnapshot: BaGuideFavoriteBgmCacheSnapshot = BaGuideFavoriteBgmCacheSnapshot(),
    val favoriteBgmOfflineCacheState: BaGuideFavoriteBgmOfflineCacheUiState =
        BaGuideFavoriteBgmOfflineCacheUiState(),
    val nativeBgmMediaNotificationEnabled: Boolean = BA_NATIVE_BGM_MEDIA_NOTIFICATION_DEFAULT,
    val mediaAdaptiveRotationEnabled: Boolean = true,
    val transferSettings: BaGuideCatalogTransferSettingsUiState =
        BaGuideCatalogTransferSettingsUiState(),
)
