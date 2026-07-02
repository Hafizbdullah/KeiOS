package os.kei.ui.page.main.student.catalog.component

import androidx.compose.runtime.Immutable
import os.kei.ui.page.main.student.BaGuideGalleryItem

internal sealed interface BaGuideMemoryLobbyLookupState {
    data object Idle : BaGuideMemoryLobbyLookupState
    data object Loading : BaGuideMemoryLobbyLookupState
    data object Missing : BaGuideMemoryLobbyLookupState
    data class Ready(val item: BaGuideMemoryLobbyResolvedItem) : BaGuideMemoryLobbyLookupState
}

internal sealed interface BaGuideMemoryLobbyCachedLookupResult {
    data object NoCache : BaGuideMemoryLobbyCachedLookupResult
    data object FreshMissing : BaGuideMemoryLobbyCachedLookupResult
    data class Ready(val item: BaGuideMemoryLobbyResolvedItem) : BaGuideMemoryLobbyCachedLookupResult
}

@Immutable
internal data class BaGuideMemoryLobbyResolvedItem(
    val galleryItems: List<BaGuideGalleryItem>,
    val memoryUnlockLevel: String,
    val studentTitle: String,
    val studentImageUrl: String,
    val sourceUrl: String,
    val fromCache: Boolean,
)
