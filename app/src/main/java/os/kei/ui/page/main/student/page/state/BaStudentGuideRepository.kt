package os.kei.ui.page.main.student.page.state

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.student.BaGuideBgmFavoriteRepository
import os.kei.ui.page.main.student.BaGuideDataClock
import os.kei.ui.page.main.student.BaGuideStudentDetailCacheMeta
import os.kei.ui.page.main.student.BaGuideStudentDetailFileCacheStore
import os.kei.ui.page.main.student.BaGuideSystemDataClock
import os.kei.ui.page.main.student.BaGuideTempMediaCache
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.BaStudentGuideStore
import os.kei.ui.page.main.student.BaStudentGuideCacheSnapshot
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.GuideBottomTab
import os.kei.ui.page.main.student.baGuideStudentDetailCacheStore
import os.kei.ui.page.main.student.buildBaGuideStudentDetailCacheMetaFromInfo
import os.kei.ui.page.main.student.catalog.BaGuideCatalogStore
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import os.kei.ui.page.main.student.decideBaGuideStudentDetailCacheRefresh
import os.kei.ui.page.main.student.fetch.extractGuideContentIdFromUrl
import os.kei.ui.page.main.student.fetchGuideInfoAsync
import os.kei.ui.page.main.student.isNpcSatelliteLikeGuide
import os.kei.ui.page.main.student.normalizeStudentGuideSourceUrl
import os.kei.ui.page.main.student.page.support.collectGuideStaticImagePrefetchUrls
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

internal data class BaStudentGuideLoadResult(
    val info: BaStudentGuideInfo?,
    val error: String?,
    val validateInBackground: Boolean = false,
    val cacheMeta: BaGuideStudentDetailCacheMeta? = null,
)

internal data class BaStudentGuideMediaSettings(
    val mediaAdaptiveRotationEnabled: Boolean = false,
)

private data class BaStudentGuideDetailCacheContext(
    val entry: BaGuideCatalogEntry,
    val store: BaGuideStudentDetailFileCacheStore,
)

internal class BaStudentGuideRepository(
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.baFetch,
    private val parseDispatcher: CoroutineDispatcher = AppDispatchers.uiDerivation,
    private val bgmFavoriteRepository: BaGuideBgmFavoriteRepository = BaGuideBgmFavoriteRepository(),
    private val clock: BaGuideDataClock = BaGuideSystemDataClock,
    private val refreshIntervalLoader: suspend () -> Int = {
        BASettingsStore.loadCalendarRefreshIntervalHours()
    },
    private val cacheSnapshotLoader: suspend (String) -> BaStudentGuideCacheSnapshot = { sourceUrl ->
        BaStudentGuideStore.loadInfoSnapshot(sourceUrl)
    },
    private val cacheSaver: suspend (BaStudentGuideInfo) -> Unit = { info ->
        BaStudentGuideStore.saveInfo(info)
    },
    private val cacheClearer: suspend (Context, String) -> Unit = { context, sourceUrl ->
        BaStudentGuideStore.clearCachedInfo(sourceUrl)
        BaGuideTempMediaCache.clearGuideCache(context, sourceUrl)
    },
    private val guideFetcher: suspend (
        String,
        CoroutineDispatcher,
        CoroutineDispatcher,
        BaGuideDataClock,
    ) -> BaStudentGuideInfo = { sourceUrl, networkDispatcher, detailParseDispatcher, dataClock ->
        fetchGuideInfoAsync(
            sourceUrl = sourceUrl,
            networkDispatcher = networkDispatcher,
            parseDispatcher = detailParseDispatcher,
            clock = dataClock,
        )
    },
    private val catalogBundleLoader: suspend () -> BaGuideCatalogBundle? = {
        BaGuideCatalogStore.loadBundle()
    },
    private val detailCacheStoreProvider: (Context) -> BaGuideStudentDetailFileCacheStore =
        ::baGuideStudentDetailCacheStore,
) {
    private val npcSatelliteGuideFlagCache = ConcurrentHashMap<Long, Boolean>()

    suspend fun loadCurrentUrlAsync(): String =
        withContext(ioDispatcher) {
            BaStudentGuideStore.loadCurrentUrl()
        }

    suspend fun saveCurrentUrlAsync(sourceUrl: String) {
        withContext(ioDispatcher) {
            BaStudentGuideStore.setCurrentUrl(sourceUrl)
        }
    }

    fun bgmFavoritesFlow(): StateFlow<List<GuideBgmFavoriteItem>> = bgmFavoriteRepository.favoritesFlow()

    suspend fun hydrateBgmFavorites(): List<GuideBgmFavoriteItem> = bgmFavoriteRepository.hydrateFavorites()

    suspend fun toggleBgmFavorite(item: GuideBgmFavoriteItem): Boolean = bgmFavoriteRepository.toggleFavorite(item)

    suspend fun loadMediaSettings(): BaStudentGuideMediaSettings =
        withContext(ioDispatcher) {
            BaStudentGuideMediaSettings(
                mediaAdaptiveRotationEnabled = BASettingsStore.loadMediaAdaptiveRotationEnabled(),
            )
        }

    fun consumeInitialBottomTab(sourceUrl: String): GuideBottomTab? = GuideDetailTabRequestStore.consume(sourceUrl)

    suspend fun resolveNpcSatelliteGuide(
        sourceUrl: String,
        info: BaStudentGuideInfo?,
    ): Boolean {
        val catalogMatched = resolveNpcSatelliteGuideSource(sourceUrl)
        return withContext(parseDispatcher) {
            info?.isNpcSatelliteLikeGuide(catalogMatched) ?: catalogMatched
        }
    }

    suspend fun prefetchStaticImages(
        context: Context,
        sourceUrl: String,
        rawUrls: List<String>,
    ) {
        if (sourceUrl.isBlank() || rawUrls.isEmpty()) return
        withContext(ioDispatcher) {
            BaGuideTempMediaCache.prefetchForGuide(
                context = context,
                sourceUrl = sourceUrl,
                rawUrls = rawUrls,
                ioDispatcher = ioDispatcher,
            )
        }
    }

    suspend fun collectStaticImagePrefetchUrls(
        info: BaStudentGuideInfo,
        maxCount: Int,
    ): List<String> =
        withContext(parseDispatcher) {
            collectGuideStaticImagePrefetchUrls(
                info = info,
                maxCount = maxCount,
            )
        }

    suspend fun loadGuide(
        context: Context,
        sourceUrl: String,
        currentInfo: BaStudentGuideInfo?,
        manualRefresh: Boolean,
        forceValidation: Boolean = false,
        loadFailedText: String,
        refreshFailedKeepCacheText: String,
    ): BaStudentGuideLoadResult {
        val requestUrl = sourceUrl.trim()
        if (requestUrl.isBlank()) {
            return BaStudentGuideLoadResult(info = null, error = null)
        }

        val now = clock.nowMs()
        val refreshIntervalHours =
            withContext(ioDispatcher) {
                refreshIntervalLoader()
            }
        val cacheSnapshot =
            withContext(ioDispatcher) {
                cacheSnapshotLoader(requestUrl)
            }
        val catalogEntry = resolveCatalogEntrySource(requestUrl)
        val studentCacheContext =
            catalogEntry
                ?.takeIf { it.tab == BaGuideCatalogTab.Student }
                ?.let { entry ->
                    BaStudentGuideDetailCacheContext(
                        entry = entry,
                        store = detailCacheStoreProvider(context),
                    )
                }
        val cacheInfo = cacheSnapshot.info.takeIf { cacheSnapshot.isComplete }
        val legacyCacheExpired =
            BaStudentGuideStore.isCacheExpired(
                snapshot = cacheSnapshot,
                refreshIntervalHours = refreshIntervalHours,
                nowMs = now,
            )
        val existingStudentMeta =
            if (studentCacheContext != null) {
                withContext(ioDispatcher) {
                    studentCacheContext.store.loadMeta(requestUrl)
                }
            } else {
                null
            }
        val studentMeta =
            if (studentCacheContext != null && cacheInfo != null) {
                existingStudentMeta ?: buildAndSaveStudentDetailMeta(
                    store = studentCacheContext.store,
                    info = cacheInfo,
                    entry = studentCacheContext.entry,
                    previous = null,
                    nowMs = now,
                )
            } else {
                existingStudentMeta
            }
        val cacheExpired =
            when {
                forceValidation -> true
                studentMeta != null ->
                    decideBaGuideStudentDetailCacheRefresh(
                        meta = studentMeta,
                        manualRefresh = manualRefresh,
                        nowMs = now,
                    ).shouldValidate
                else -> legacyCacheExpired
            }
        if (!manualRefresh && !forceValidation && cacheInfo != null && cacheExpired && studentMeta != null) {
            return BaStudentGuideLoadResult(
                info = cacheInfo,
                error = null,
                validateInBackground = true,
                cacheMeta = studentMeta,
            )
        }
        if (!manualRefresh && !forceValidation && cacheInfo != null && !cacheExpired) {
            return BaStudentGuideLoadResult(
                info = cacheInfo,
                error = null,
                cacheMeta = studentMeta,
            )
        }

        val visibleInfo =
            when {
                cacheInfo != null -> cacheInfo
                cacheSnapshot.hasCache -> null
                currentInfo?.sourceUrl == requestUrl -> currentInfo
                else -> null
            }
        val shouldClearLocalCache =
            if (studentCacheContext != null) {
                manualRefresh || (cacheSnapshot.hasCache && !cacheSnapshot.isComplete)
            } else {
                manualRefresh || (cacheSnapshot.hasCache && (cacheExpired || !cacheSnapshot.isComplete))
            }

        val result =
            try {
                Result.success(
                    guideFetcher(requestUrl, ioDispatcher, parseDispatcher, clock),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Result.failure(error)
            }
        return result.fold(
            onSuccess = { latest ->
                var latestMeta: BaGuideStudentDetailCacheMeta? = null
                withContext(ioDispatcher) {
                    if (shouldClearLocalCache) {
                        cacheClearer(context, requestUrl)
                    }
                    cacheSaver(latest)
                    if (studentCacheContext != null) {
                        latestMeta = buildAndSaveStudentDetailMeta(
                            store = studentCacheContext.store,
                            info = latest,
                            entry = studentCacheContext.entry,
                            previous = studentMeta,
                            nowMs = clock.nowMs(),
                        )
                    }
                }
                BaStudentGuideLoadResult(
                    info = latest,
                    error = null,
                    cacheMeta = latestMeta,
                )
            },
            onFailure = {
                var failedMeta = studentMeta
                if (studentCacheContext != null && studentMeta != null) {
                    withContext(ioDispatcher) {
                        failedMeta = studentMeta.recordFailure(now)
                        studentCacheContext.store.saveMeta(failedMeta)
                    }
                }
                BaStudentGuideLoadResult(
                    info = visibleInfo,
                    error = if (visibleInfo != null) refreshFailedKeepCacheText else loadFailedText,
                    cacheMeta = failedMeta,
                )
            },
        )
    }

    private suspend fun resolveCatalogEntrySource(sourceUrl: String): BaGuideCatalogEntry? {
        val normalized = normalizeStudentGuideSourceUrl(sourceUrl)
        if (normalized.isBlank()) return null
        val contentId = extractGuideContentIdFromUrl(normalized)
        val bundle =
            withContext(ioDispatcher) {
                catalogBundleLoader()
            } ?: return null
        return withContext(parseDispatcher) {
            val entries = BaGuideCatalogTab.entries.flatMap { tab -> bundle.entries(tab) }
            entries.firstOrNull { entry -> normalizeStudentGuideSourceUrl(entry.detailUrl) == normalized }
                ?: contentId?.let { id -> entries.firstOrNull { entry -> entry.contentId == id } }
        }
    }

    private fun buildAndSaveStudentDetailMeta(
        store: BaGuideStudentDetailFileCacheStore,
        info: BaStudentGuideInfo,
        entry: BaGuideCatalogEntry,
        previous: BaGuideStudentDetailCacheMeta?,
        nowMs: Long,
    ): BaGuideStudentDetailCacheMeta? {
        val meta =
            buildBaGuideStudentDetailCacheMetaFromInfo(
                info = info,
                contentId = entry.contentId,
                tab = entry.tab,
                catalogCreatedAtSec = entry.createdAtSec,
                releaseDateSec = entry.releaseDateSec,
                previous = previous,
                nowMs = nowMs,
            ) ?: return null
        store.saveMeta(meta)
        return meta
    }

    private fun BaGuideStudentDetailCacheMeta.recordFailure(nowMs: Long): BaGuideStudentDetailCacheMeta {
        val nextFailureCount = (failureCount + 1).coerceAtMost(6)
        val retryDelayMs = (30L * 60L * 1000L) * nextFailureCount
        return copy(
            failureCount = nextFailureCount,
            lastFailureAtMs = nowMs,
            nextRetryAtMs = nowMs + retryDelayMs,
        )
    }

    private suspend fun resolveNpcSatelliteGuideSource(sourceUrl: String): Boolean {
        val contentId = extractGuideContentIdFromUrl(sourceUrl) ?: return false
        if (contentId <= 0L) return false
        npcSatelliteGuideFlagCache[contentId]?.let { return it }
        val isNpcSatellite =
            withContext(ioDispatcher) {
                catalogBundleLoader()
                    ?.entries(BaGuideCatalogTab.NpcSatellite)
                    ?.any { entry -> entry.contentId == contentId }
                    ?: false
            }
        npcSatelliteGuideFlagCache[contentId] = isNpcSatellite
        return isNpcSatellite
    }
}
