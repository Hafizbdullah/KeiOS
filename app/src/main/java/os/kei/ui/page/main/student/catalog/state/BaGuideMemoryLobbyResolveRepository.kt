package os.kei.ui.page.main.student.catalog.state

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.ui.page.main.student.BaGuideDataClock
import os.kei.ui.page.main.student.BaGuideGalleryItem
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.BaStudentGuideStore
import os.kei.ui.page.main.student.BaGuideSystemDataClock
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.component.BaGuideMemoryLobbyResolvedItem
import os.kei.ui.page.main.student.fetchGuideInfoAsync
import os.kei.ui.page.main.student.isMemoryHallGalleryItem
import os.kei.ui.page.main.student.tabcontent.render.resolveGuideGalleryTabState

internal class BaGuideMemoryLobbyResolveRepository(
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.baFetch,
    private val parseDispatcher: CoroutineDispatcher = AppDispatchers.uiDerivation,
    private val clock: BaGuideDataClock = BaGuideSystemDataClock,
) {
    suspend fun loadCachedMemoryLobby(entry: BaGuideCatalogEntry): BaGuideMemoryLobbyResolvedItem? =
        withContext(ioDispatcher) {
            val info = BaStudentGuideStore.loadInfoSnapshot(entry.detailUrl).info ?: return@withContext null
            info.toMemoryLobbyResolvedItem(entry = entry, fromCache = true)
        }

    suspend fun fetchMemoryLobby(entry: BaGuideCatalogEntry): BaGuideMemoryLobbyResolvedItem? {
        loadCachedMemoryLobby(entry)?.let { return it }
        val info =
            fetchGuideInfoAsync(
                sourceUrl = entry.detailUrl,
                networkDispatcher = ioDispatcher,
                parseDispatcher = parseDispatcher,
                clock = clock,
            )
        withContext(ioDispatcher) {
            BaStudentGuideStore.saveInfo(info)
        }
        return info.toMemoryLobbyResolvedItem(entry = entry, fromCache = false)
    }
}

internal fun BaStudentGuideInfo.toMemoryLobbyResolvedItem(
    entry: BaGuideCatalogEntry,
    fromCache: Boolean,
): BaGuideMemoryLobbyResolvedItem? {
    val resolvedGallery = resolveGuideGalleryTabState(this)
    val memoryImages =
        resolvedGallery.displayGalleryItems
            .filter(::isMemoryHallGalleryItem)
    val memoryVideos =
        resolvedGallery.memoryHallVideoGroup
            ?.second
            .orEmpty()
    val fallbackPreview =
        resolvedGallery.memoryHallPreview
            .takeIf { it.isNotBlank() && memoryImages.isEmpty() }
            ?.let { preview ->
                BaGuideGalleryItem(
                    title = MEMORY_LOBBY_FALLBACK_GALLERY_TITLE,
                    imageUrl = preview,
                    mediaType = "image",
                    mediaUrl = preview,
                    memoryUnlockLevel = resolvedGallery.memoryUnlockLevel,
                )
            }
    val galleryItems =
        (memoryImages + listOfNotNull(fallbackPreview) + memoryVideos)
            .distinctBy { item ->
                "${item.mediaType.lowercase()}|${item.mediaUrl.ifBlank { item.imageUrl }}"
            }
    if (galleryItems.isEmpty()) return null
    return BaGuideMemoryLobbyResolvedItem(
        galleryItems = galleryItems,
        memoryUnlockLevel = resolvedGallery.memoryUnlockLevel,
        studentTitle = entry.name.ifBlank { title },
        studentImageUrl = imageUrl.ifBlank { entry.iconUrl },
        sourceUrl = entry.detailUrl.ifBlank { sourceUrl },
        fromCache = fromCache,
    )
}

private const val MEMORY_LOBBY_FALLBACK_GALLERY_TITLE = "回忆大厅"
