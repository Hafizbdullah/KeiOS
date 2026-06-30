package os.kei.ui.page.main.student.page.state

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.ui.page.main.student.BaGuideDataClock
import os.kei.ui.page.main.student.BaGuideRow
import os.kei.ui.page.main.student.BaGuideStudentDetailCacheMeta
import os.kei.ui.page.main.student.BaGuideStudentDetailFileCacheStore
import os.kei.ui.page.main.student.BaGuideStudentDetailFreshnessTier
import os.kei.ui.page.main.student.BaStudentGuideCacheSnapshot
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.BaGuideCatalogTab
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class BaStudentGuideRepositoryTest {
    @Test
    fun `fresh implemented student detail cache returns without network and writes migrated meta`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val nowMs = 100L * DAY_MS
            val sourceUrl = "https://www.gamekee.com/ba/tj/10001.html"
            val cached = guideInfo(sourceUrl = sourceUrl, title = "缓存学生", syncedAtMs = nowMs - DAY_MS)
            val metaStore = tempMetaStore(context)
            var fetchCalled = false
            val repository =
                repository(
                    nowMs = nowMs,
                    cacheSnapshot = BaStudentGuideCacheSnapshot(cached, hasCache = true, isComplete = true, cached.syncedAtMs),
                    catalog = catalogBundle(
                        catalogEntry(
                            sourceUrl = sourceUrl,
                            contentId = 10001L,
                            tab = BaGuideCatalogTab.Student,
                            createdAtSec = (nowMs - 40L * DAY_MS) / 1000L,
                        ),
                    ),
                    metaStore = metaStore,
                    fetcher = {
                        fetchCalled = true
                        guideInfo(sourceUrl = sourceUrl, title = "网络学生", syncedAtMs = nowMs)
                    },
                )

            val result =
                repository.loadGuide(
                    context = context,
                    sourceUrl = sourceUrl,
                    currentInfo = null,
                    manualRefresh = false,
                    loadFailedText = "加载失败",
                    refreshFailedKeepCacheText = "保留缓存",
                )

            assertEquals(cached, result.info)
            assertNull(result.error)
            assertFalse(fetchCalled)
            assertNotNull(result.cacheMeta)
            assertEquals(BaGuideStudentDetailFreshnessTier.Stable, result.cacheMeta.freshnessTier)
            val meta = metaStore.loadMeta(sourceUrl)
            assertNotNull(meta)
            assertEquals(BaGuideStudentDetailFreshnessTier.Stable, meta.freshnessTier)
            assertEquals(cached.syncedAtMs, meta.cachedAtMs)
            assertEquals(nowMs, meta.lastValidatedAtMs)
        }

    @Test
    fun `manual refresh implemented student detail fetches network and updates meta`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val nowMs = 100L * DAY_MS
            val sourceUrl = "https://www.gamekee.com/ba/tj/10002.html"
            val cached = guideInfo(sourceUrl = sourceUrl, title = "旧缓存", syncedAtMs = nowMs - DAY_MS)
            val latest = guideInfo(sourceUrl = sourceUrl, title = "新详情", syncedAtMs = nowMs)
            val metaStore = tempMetaStore(context)
            var savedInfo: BaStudentGuideInfo? = null
            var fetchCount = 0
            val repository =
                repository(
                    nowMs = nowMs,
                    cacheSnapshot = BaStudentGuideCacheSnapshot(cached, hasCache = true, isComplete = true, cached.syncedAtMs),
                    catalog = catalogBundle(
                        catalogEntry(
                            sourceUrl = sourceUrl,
                            contentId = 10002L,
                            tab = BaGuideCatalogTab.Student,
                            createdAtSec = (nowMs - 2L * DAY_MS) / 1000L,
                        ),
                    ),
                    metaStore = metaStore,
                    fetcher = {
                        fetchCount += 1
                        latest
                    },
                    saver = { savedInfo = it },
                )

            val result =
                repository.loadGuide(
                    context = context,
                    sourceUrl = sourceUrl,
                    currentInfo = cached,
                    manualRefresh = true,
                    loadFailedText = "加载失败",
                    refreshFailedKeepCacheText = "保留缓存",
                )

            assertEquals(latest, result.info)
            assertNull(result.error)
            assertEquals(1, fetchCount)
            assertEquals(latest, savedInfo)
            assertNotNull(result.cacheMeta)
            assertEquals(BaGuideStudentDetailFreshnessTier.HotUpdate, result.cacheMeta.freshnessTier)
            val meta = metaStore.loadMeta(sourceUrl)
            assertNotNull(meta)
            assertEquals(BaGuideStudentDetailFreshnessTier.HotUpdate, meta.freshnessTier)
            assertEquals(nowMs, meta.cachedAtMs)
            assertEquals(nowMs, meta.lastValidatedAtMs)
        }

    @Test
    fun `expired hot update student cache returns first paint and requests background validation`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val nowMs = 100L * DAY_MS
            val sourceUrl = "https://www.gamekee.com/ba/tj/10003.html"
            val cached = guideInfo(sourceUrl = sourceUrl, title = "热更新缓存", syncedAtMs = nowMs - DAY_MS)
            val latest = guideInfo(sourceUrl = sourceUrl, title = "热更新网络", syncedAtMs = nowMs)
            val metaStore = tempMetaStore(context)
            val entry =
                catalogEntry(
                    sourceUrl = sourceUrl,
                    contentId = 10003L,
                    tab = BaGuideCatalogTab.Student,
                    createdAtSec = (nowMs - 2L * DAY_MS) / 1000L,
                )
            metaStore.saveMeta(
                detailMeta(
                    sourceUrl = sourceUrl,
                    contentId = entry.contentId,
                    tier = BaGuideStudentDetailFreshnessTier.HotUpdate,
                    catalogCreatedAtSec = entry.createdAtSec,
                    cachedAtMs = cached.syncedAtMs,
                    lastValidatedAtMs = nowMs - 2L * HOUR_MS,
                ),
            )
            var fetchCount = 0
            val repository =
                repository(
                    nowMs = nowMs,
                    cacheSnapshot = BaStudentGuideCacheSnapshot(cached, hasCache = true, isComplete = true, cached.syncedAtMs),
                    catalog = catalogBundle(entry),
                    metaStore = metaStore,
                    fetcher = {
                        fetchCount += 1
                        latest
                    },
                )

            val firstPaint =
                repository.loadGuide(
                    context = context,
                    sourceUrl = sourceUrl,
                    currentInfo = null,
                    manualRefresh = false,
                    loadFailedText = "加载失败",
                    refreshFailedKeepCacheText = "保留缓存",
                )

            assertEquals(cached, firstPaint.info)
            assertNull(firstPaint.error)
            assertTrue(firstPaint.validateInBackground)
            assertNotNull(firstPaint.cacheMeta)
            assertEquals(BaGuideStudentDetailFreshnessTier.HotUpdate, firstPaint.cacheMeta.freshnessTier)
            assertEquals(0, fetchCount)

            val validation =
                repository.loadGuide(
                    context = context,
                    sourceUrl = sourceUrl,
                    currentInfo = cached,
                    manualRefresh = false,
                    forceValidation = true,
                    loadFailedText = "加载失败",
                    refreshFailedKeepCacheText = "保留缓存",
                )

            assertEquals(latest, validation.info)
            assertNull(validation.error)
            assertFalse(validation.validateInBackground)
            assertNotNull(validation.cacheMeta)
            assertEquals(nowMs, validation.cacheMeta.lastValidatedAtMs)
            assertEquals(1, fetchCount)
        }

    @Test
    fun `npc satellite detail keeps legacy refresh interval and skips student meta`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val nowMs = 100L * DAY_MS
            val sourceUrl = "https://www.gamekee.com/ba/tj/20001.html"
            val cached = guideInfo(sourceUrl = sourceUrl, title = "卫星缓存", syncedAtMs = nowMs - 2L * HOUR_MS)
            val metaStore = tempMetaStore(context)
            var fetchCalled = false
            val repository =
                repository(
                    nowMs = nowMs,
                    refreshIntervalHours = 12,
                    cacheSnapshot = BaStudentGuideCacheSnapshot(cached, hasCache = true, isComplete = true, cached.syncedAtMs),
                    catalog = catalogBundle(
                        catalogEntry(
                            sourceUrl = sourceUrl,
                            contentId = 20001L,
                            tab = BaGuideCatalogTab.NpcSatellite,
                            createdAtSec = (nowMs - DAY_MS) / 1000L,
                        ),
                    ),
                    metaStore = metaStore,
                    fetcher = {
                        fetchCalled = true
                        guideInfo(sourceUrl = sourceUrl, title = "网络卫星", syncedAtMs = nowMs)
                    },
                )

            val result =
                repository.loadGuide(
                    context = context,
                    sourceUrl = sourceUrl,
                    currentInfo = null,
                    manualRefresh = false,
                    loadFailedText = "加载失败",
                    refreshFailedKeepCacheText = "保留缓存",
                )

            assertEquals(cached, result.info)
            assertNull(result.error)
            assertFalse(fetchCalled)
            assertNull(metaStore.loadMeta(sourceUrl))
        }

    @Test
    fun `concurrent forced validations for the same student share one network fetch`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val nowMs = 100L * DAY_MS
            val sourceUrl = "https://www.gamekee.com/ba/tj/10004.html"
            val cached = guideInfo(sourceUrl = sourceUrl, title = "并发旧缓存", syncedAtMs = nowMs - DAY_MS)
            val latest = guideInfo(sourceUrl = sourceUrl, title = "并发新详情", syncedAtMs = nowMs)
            val entry =
                catalogEntry(
                    sourceUrl = sourceUrl,
                    contentId = 10004L,
                    tab = BaGuideCatalogTab.Student,
                    createdAtSec = (nowMs - 40L * DAY_MS) / 1000L,
                )
            val metaStore = tempMetaStore(context)
            metaStore.saveMeta(
                detailMeta(
                    sourceUrl = sourceUrl,
                    contentId = entry.contentId,
                    tier = BaGuideStudentDetailFreshnessTier.Stable,
                    catalogCreatedAtSec = entry.createdAtSec,
                    cachedAtMs = cached.syncedAtMs,
                    lastValidatedAtMs = nowMs - 5L * DAY_MS,
                ),
            )
            val releaseFetch = CompletableDeferred<Unit>()
            val firstFetchStarted = CompletableDeferred<Unit>()
            var fetchCount = 0
            val repository =
                repository(
                    nowMs = nowMs,
                    cacheSnapshot = BaStudentGuideCacheSnapshot(cached, hasCache = true, isComplete = true, cached.syncedAtMs),
                    catalog = catalogBundle(entry),
                    metaStore = metaStore,
                    fetcher = {
                        fetchCount += 1
                        if (fetchCount == 1) {
                            firstFetchStarted.complete(Unit)
                        }
                        releaseFetch.await()
                        latest
                    },
                )

            val first =
                async {
                    repository.loadGuide(
                        context = context,
                        sourceUrl = sourceUrl,
                        currentInfo = cached,
                        manualRefresh = false,
                        forceValidation = true,
                        loadFailedText = "加载失败",
                        refreshFailedKeepCacheText = "保留缓存",
                    )
                }
            firstFetchStarted.await()
            val second =
                async {
                    repository.loadGuide(
                        context = context,
                        sourceUrl = sourceUrl,
                        currentInfo = cached,
                        manualRefresh = false,
                        forceValidation = true,
                        loadFailedText = "加载失败",
                        refreshFailedKeepCacheText = "保留缓存",
                    )
                }
            releaseFetch.complete(Unit)
            val results = awaitAll(first, second)

            assertEquals(1, fetchCount)
            assertEquals(listOf(latest, latest), results.map { it.info })
            assertTrue(results.all { it.error == null })
        }

    @Test
    fun `student cache inside retry window skips automatic background validation`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val nowMs = 100L * DAY_MS
            val sourceUrl = "https://www.gamekee.com/ba/tj/10005.html"
            val cached = guideInfo(sourceUrl = sourceUrl, title = "重试窗口缓存", syncedAtMs = nowMs - DAY_MS)
            val entry =
                catalogEntry(
                    sourceUrl = sourceUrl,
                    contentId = 10005L,
                    tab = BaGuideCatalogTab.Student,
                    createdAtSec = (nowMs - 2L * DAY_MS) / 1000L,
                )
            val metaStore = tempMetaStore(context)
            metaStore.saveMeta(
                detailMeta(
                    sourceUrl = sourceUrl,
                    contentId = entry.contentId,
                    tier = BaGuideStudentDetailFreshnessTier.HotUpdate,
                    catalogCreatedAtSec = entry.createdAtSec,
                    cachedAtMs = cached.syncedAtMs,
                    lastValidatedAtMs = nowMs - 2L * HOUR_MS,
                ).copy(
                    failureCount = 2,
                    lastFailureAtMs = nowMs - 5L * 60L * 1000L,
                    nextRetryAtMs = nowMs + HOUR_MS,
                ),
            )
            var fetchCalled = false
            val repository =
                repository(
                    nowMs = nowMs,
                    cacheSnapshot = BaStudentGuideCacheSnapshot(cached, hasCache = true, isComplete = true, cached.syncedAtMs),
                    catalog = catalogBundle(entry),
                    metaStore = metaStore,
                    fetcher = {
                        fetchCalled = true
                        guideInfo(sourceUrl = sourceUrl, title = "重试窗口网络", syncedAtMs = nowMs)
                    },
                )

            val result =
                repository.loadGuide(
                    context = context,
                    sourceUrl = sourceUrl,
                    currentInfo = null,
                    manualRefresh = false,
                    loadFailedText = "加载失败",
                    refreshFailedKeepCacheText = "保留缓存",
                )

            assertEquals(cached, result.info)
            assertNull(result.error)
            assertFalse(result.validateInBackground)
            assertFalse(fetchCalled)
        }

    @Test
    fun `changed detail url keeps first seen time from content id meta`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val nowMs = 100L * DAY_MS
            val oldUrl = "https://www.gamekee.com/ba/tj/10006-old.html"
            val newUrl = "https://www.gamekee.com/ba/tj/10006.html"
            val cached = guideInfo(sourceUrl = newUrl, title = "迁移缓存", syncedAtMs = nowMs - DAY_MS)
            val metaStore = tempMetaStore(context)
            val oldFirstSeenAtMs = nowMs - 60L * DAY_MS
            metaStore.saveMeta(
                detailMeta(
                    sourceUrl = oldUrl,
                    contentId = 10006L,
                    tier = BaGuideStudentDetailFreshnessTier.Stable,
                    catalogCreatedAtSec = 0L,
                    cachedAtMs = nowMs - 10L * DAY_MS,
                    lastValidatedAtMs = nowMs - 5L * DAY_MS,
                ).copy(firstSeenAtMs = oldFirstSeenAtMs),
            )
            var fetchCalled = false
            val repository =
                repository(
                    nowMs = nowMs,
                    cacheSnapshot = BaStudentGuideCacheSnapshot(cached, hasCache = true, isComplete = true, cached.syncedAtMs),
                    catalog = catalogBundle(
                        catalogEntry(
                            sourceUrl = newUrl,
                            contentId = 10006L,
                            tab = BaGuideCatalogTab.Student,
                            createdAtSec = 0L,
                        ),
                    ),
                    metaStore = metaStore,
                    fetcher = {
                        fetchCalled = true
                        guideInfo(sourceUrl = newUrl, title = "迁移网络", syncedAtMs = nowMs)
                    },
                )

            val result =
                repository.loadGuide(
                    context = context,
                    sourceUrl = newUrl,
                    currentInfo = null,
                    manualRefresh = false,
                    loadFailedText = "加载失败",
                    refreshFailedKeepCacheText = "保留缓存",
                )

            assertEquals(cached, result.info)
            assertFalse(fetchCalled)
            assertNotNull(result.cacheMeta)
            assertEquals(oldFirstSeenAtMs, result.cacheMeta.firstSeenAtMs)
            assertEquals(oldFirstSeenAtMs, metaStore.loadMeta(newUrl)?.firstSeenAtMs)
        }

    @Test
    fun `detail refresh extracts release date and upserts catalog release index`() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val nowMs = 100L * DAY_MS
            val sourceUrl = "https://www.gamekee.com/ba/tj/10007.html"
            val cached = guideInfo(sourceUrl = sourceUrl, title = "日期旧缓存", syncedAtMs = nowMs - DAY_MS)
            val latest =
                guideInfo(sourceUrl = sourceUrl, title = "日期新详情", syncedAtMs = nowMs).copy(
                    profileRows = listOf(BaGuideRow("实装日期", "2025年1月2日")),
                )
            val metaStore = tempMetaStore(context)
            var upsertedReleaseDates: Map<Long, Long> = emptyMap()
            val repository =
                repository(
                    nowMs = nowMs,
                    cacheSnapshot = BaStudentGuideCacheSnapshot(cached, hasCache = true, isComplete = true, cached.syncedAtMs),
                    catalog = catalogBundle(
                        catalogEntry(
                            sourceUrl = sourceUrl,
                            contentId = 10007L,
                            tab = BaGuideCatalogTab.Student,
                            createdAtSec = (nowMs - 40L * DAY_MS) / 1000L,
                        ),
                    ),
                    metaStore = metaStore,
                    fetcher = { latest },
                    releaseDateIndexUpserter = { upsertedReleaseDates = it },
                )

            val result =
                repository.loadGuide(
                    context = context,
                    sourceUrl = sourceUrl,
                    currentInfo = cached,
                    manualRefresh = true,
                    loadFailedText = "加载失败",
                    refreshFailedKeepCacheText = "保留缓存",
                )

            assertEquals(latest, result.info)
            assertNotNull(result.cacheMeta)
            assertTrue(result.cacheMeta.releaseDateSec > 0L)
            assertEquals(result.cacheMeta.releaseDateSec, upsertedReleaseDates[10007L])
        }

    private fun repository(
        nowMs: Long,
        refreshIntervalHours: Int = 12,
        cacheSnapshot: BaStudentGuideCacheSnapshot,
        catalog: BaGuideCatalogBundle?,
        metaStore: BaGuideStudentDetailFileCacheStore,
        fetcher: suspend (String) -> BaStudentGuideInfo,
        saver: suspend (BaStudentGuideInfo) -> Unit = {},
        releaseDateIndexUpserter: suspend (Map<Long, Long>) -> Unit = {},
    ): BaStudentGuideRepository =
        BaStudentGuideRepository(
            ioDispatcher = Dispatchers.Unconfined,
            parseDispatcher = Dispatchers.Unconfined,
            clock = BaGuideDataClock { nowMs },
            refreshIntervalLoader = { refreshIntervalHours },
            cacheSnapshotLoader = { cacheSnapshot },
            cacheSaver = saver,
            cacheClearer = { _, _ -> },
            guideFetcher = { sourceUrl, _, _, _ -> fetcher(sourceUrl) },
            catalogBundleLoader = { catalog },
            detailCacheStoreProvider = { metaStore },
            releaseDateIndexUpserter = releaseDateIndexUpserter,
        )

    private fun tempMetaStore(context: Application): BaGuideStudentDetailFileCacheStore {
        val root = File(context.filesDir, "student-repository-cache-test-${System.nanoTime()}").apply {
            deleteRecursively()
            mkdirs()
        }
        return BaGuideStudentDetailFileCacheStore(root)
    }

    private fun guideInfo(
        sourceUrl: String,
        title: String,
        syncedAtMs: Long,
    ): BaStudentGuideInfo =
        BaStudentGuideInfo(
            sourceUrl = sourceUrl,
            title = title,
            subtitle = "GameKee",
            description = "desc",
            imageUrl = "https://example.com/$title.png",
            summary = "summary",
            stats = listOf("学校" to "夏莱"),
            profileRows = listOf(BaGuideRow("学校", "夏莱")),
            syncedAtMs = syncedAtMs,
        )

    private fun catalogBundle(entry: BaGuideCatalogEntry): BaGuideCatalogBundle =
        BaGuideCatalogBundle(
            entriesByTab =
                mapOf(
                    BaGuideCatalogTab.Student to listOf(entry).filter { it.tab == BaGuideCatalogTab.Student },
                    BaGuideCatalogTab.NpcSatellite to
                        listOf(entry).filter { it.tab == BaGuideCatalogTab.NpcSatellite },
                ),
            syncedAtMs = 1_000L,
        )

    private fun catalogEntry(
        sourceUrl: String,
        contentId: Long,
        tab: BaGuideCatalogTab,
        createdAtSec: Long,
    ): BaGuideCatalogEntry =
        BaGuideCatalogEntry(
            entryId = contentId.toInt(),
            pid = 1,
            contentId = contentId,
            name = "Entry $contentId",
            alias = "",
            aliasDisplay = "",
            iconUrl = "https://example.com/icon.png",
            type = 1,
            order = 1,
            createdAtSec = createdAtSec,
            releaseDateSec = 0L,
            detailUrl = sourceUrl,
            tab = tab,
        )

    private fun detailMeta(
        sourceUrl: String,
        contentId: Long,
        tier: BaGuideStudentDetailFreshnessTier,
        catalogCreatedAtSec: Long,
        cachedAtMs: Long,
        lastValidatedAtMs: Long,
    ): BaGuideStudentDetailCacheMeta =
        BaGuideStudentDetailCacheMeta(
            sourceUrl = sourceUrl,
            contentId = contentId,
            tab = BaGuideCatalogTab.Student,
            catalogCreatedAtSec = catalogCreatedAtSec,
            releaseDateSec = 0L,
            firstSeenAtMs = 0L,
            cachedAtMs = cachedAtMs,
            lastValidatedAtMs = lastValidatedAtMs,
            lastChangedAtMs = cachedAtMs,
            nextAutoRefreshAtMs = lastValidatedAtMs + tier.validationIntervalMs,
            freshnessTier = tier,
            contentHash = "cached",
            unchangedValidationCount = 0,
            failureCount = 0,
            lastFailureAtMs = 0L,
            nextRetryAtMs = 0L,
        )

    private companion object {
        private const val HOUR_MS = 60L * 60L * 1000L
        private const val DAY_MS = 24L * HOUR_MS
    }
}
