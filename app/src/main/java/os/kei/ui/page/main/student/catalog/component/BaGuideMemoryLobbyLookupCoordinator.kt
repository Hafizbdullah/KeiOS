package os.kei.ui.page.main.student.catalog.component

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.yield
import os.kei.core.concurrency.AppDispatchers
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.catalog.state.BaGuideMemoryLobbyResolveRepository
import kotlin.coroutines.cancellation.CancellationException

private const val MEMORY_LOBBY_CACHE_PREWARM_BATCH_SIZE = 28
private const val MEMORY_LOBBY_CACHE_PREWARM_PARALLELISM = 3
private const val MEMORY_LOBBY_VISIBLE_NETWORK_PREWARM_LIMIT = 4
private const val MEMORY_LOBBY_VISIBLE_NETWORK_PREWARM_BATCH_SIZE = 2
private const val MEMORY_LOBBY_VISIBLE_NETWORK_PREWARM_PARALLELISM = 1

internal class BaGuideMemoryLobbyLookupCoordinator(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.baFetch,
    private val parseDispatcher: CoroutineDispatcher = AppDispatchers.uiDerivation,
    private val repository: BaGuideMemoryLobbyResolveRepository =
        BaGuideMemoryLobbyResolveRepository(
            ioDispatcher = ioDispatcher,
            parseDispatcher = parseDispatcher,
        ),
    private val cachedLoader: suspend (BaGuideCatalogEntry) -> BaGuideMemoryLobbyResolvedItem? = { entry ->
        repository.loadCachedMemoryLobby(entry)
    },
    private val networkLoader: suspend (BaGuideCatalogEntry) -> BaGuideMemoryLobbyResolvedItem? = { entry ->
        repository.fetchMemoryLobby(entry)
    },
) {
    private val mutableStates = MutableStateFlow<Map<Long, BaGuideMemoryLobbyLookupState>>(emptyMap())
    val states: StateFlow<Map<Long, BaGuideMemoryLobbyLookupState>> = mutableStates.asStateFlow()

    private var cachePrewarmJob: Job? = null
    private val cachePrewarmCheckedContentIds = mutableSetOf<Long>()
    private var visibleNetworkPrewarmJob: Job? = null
    private val visibleNetworkPrewarmCheckedContentIds = mutableSetOf<Long>()
    private val pendingResolveLock = Any()
    private val pendingResolveCallbacksByContentId =
        mutableMapOf<Long, MutableList<(BaGuideMemoryLobbyResolvedItem?) -> Unit>>()
    private var lookupGeneration = 0

    fun clear() {
        cachePrewarmJob?.cancel()
        visibleNetworkPrewarmJob?.cancel()
        cachePrewarmCheckedContentIds.clear()
        visibleNetworkPrewarmCheckedContentIds.clear()
        synchronized(pendingResolveLock) {
            lookupGeneration++
            pendingResolveCallbacksByContentId.clear()
        }
        mutableStates.value = emptyMap()
    }

    fun prewarmCached(entries: List<BaGuideCatalogEntry>) {
        val currentStates = mutableStates.value
        val pendingEntries =
            entries.filter { entry ->
                currentStates[entry.contentId] == null &&
                    entry.contentId !in cachePrewarmCheckedContentIds
            }
        if (pendingEntries.isEmpty()) return
        cachePrewarmJob?.cancel()
        cachePrewarmJob =
            scope.launch {
                val semaphore = Semaphore(MEMORY_LOBBY_CACHE_PREWARM_PARALLELISM)
                pendingEntries.chunked(MEMORY_LOBBY_CACHE_PREWARM_BATCH_SIZE).forEach { batch ->
                    currentCoroutineContext().ensureActive()
                    val cached =
                        batch
                            .map { entry ->
                                async {
                                    semaphore.withPermit {
                                        runCatchingCancellable {
                                            cachedLoader(entry)?.let { item -> entry.contentId to item }
                                        }.getOrNull()
                                    }
                                }
                            }.awaitAll()
                            .filterNotNull()
                    cachePrewarmCheckedContentIds += batch.map { entry -> entry.contentId }
                    if (cached.isNotEmpty()) {
                        mutableStates.update { states ->
                            states + cached.associate { (contentId, item) ->
                                contentId to BaGuideMemoryLobbyLookupState.Ready(item)
                            }
                        }
                    }
                    yield()
                }
            }
    }

    fun prewarmVisibleNetwork(entries: List<BaGuideCatalogEntry>) {
        val currentStates = mutableStates.value
        val pendingEntries =
            entries
                .asSequence()
                .filter { entry -> entry.contentId > 0L }
                .distinctBy { entry -> entry.contentId }
                .filter { entry ->
                    entry.contentId !in visibleNetworkPrewarmCheckedContentIds &&
                        currentStates[entry.contentId] !is BaGuideMemoryLobbyLookupState.Ready &&
                        currentStates[entry.contentId] != BaGuideMemoryLobbyLookupState.Loading &&
                        currentStates[entry.contentId] != BaGuideMemoryLobbyLookupState.Missing
                }
                .take(MEMORY_LOBBY_VISIBLE_NETWORK_PREWARM_LIMIT)
                .toList()
        if (pendingEntries.isEmpty()) return
        visibleNetworkPrewarmJob?.cancel()
        visibleNetworkPrewarmJob =
            scope.launch {
                val semaphore = Semaphore(MEMORY_LOBBY_VISIBLE_NETWORK_PREWARM_PARALLELISM)
                pendingEntries.chunked(MEMORY_LOBBY_VISIBLE_NETWORK_PREWARM_BATCH_SIZE).forEach { batch ->
                    currentCoroutineContext().ensureActive()
                    val resolved =
                        batch
                            .map { entry ->
                                async {
                                    semaphore.withPermit {
                                        val item =
                                            runCatchingCancellable {
                                                networkLoader(entry)
                                            }.getOrNull()
                                        entry.contentId to item
                                    }
                                }
                            }.awaitAll()
                    visibleNetworkPrewarmCheckedContentIds += batch.map { entry -> entry.contentId }
                    val nextStates =
                        resolved
                            .map { (contentId, item) ->
                                contentId to if (item == null) {
                                    BaGuideMemoryLobbyLookupState.Missing
                                } else {
                                    BaGuideMemoryLobbyLookupState.Ready(item)
                                }
                            }.toMap()
                    if (nextStates.isNotEmpty()) {
                        mutableStates.update { states -> states + nextStates }
                    }
                    yield()
                }
            }
    }

    fun resolveEntry(
        entry: BaGuideCatalogEntry,
        allowNetwork: Boolean,
        onResolved: (BaGuideMemoryLobbyResolvedItem?) -> Unit = {},
    ) {
        val contentId = entry.contentId
        val generation: Int
        synchronized(pendingResolveLock) {
            val current = mutableStates.value[contentId]
            if (current is BaGuideMemoryLobbyLookupState.Ready) {
                onResolved(current.item)
                return
            }
            pendingResolveCallbacksByContentId
                .getOrPut(contentId) { mutableListOf() }
                .add(onResolved)
            if (current == BaGuideMemoryLobbyLookupState.Loading) return
            generation = lookupGeneration
            mutableStates.update { states ->
                states + (contentId to BaGuideMemoryLobbyLookupState.Loading)
            }
        }
        scope.launch {
            val resolved =
                runCatchingCancellable {
                    if (allowNetwork) {
                        networkLoader(entry)
                    } else {
                        cachedLoader(entry)
                    }
                }.getOrNull()
            val callbacks =
                synchronized(pendingResolveLock) {
                    if (generation != lookupGeneration) {
                        emptyList()
                    } else {
                        pendingResolveCallbacksByContentId.remove(contentId).orEmpty()
                    }
                }
            if (callbacks.isEmpty() && generation != lookupGeneration) return@launch
            if (allowNetwork || resolved != null) {
                mutableStates.update { states ->
                    states + (
                        contentId to if (resolved == null) {
                            BaGuideMemoryLobbyLookupState.Missing
                        } else {
                            BaGuideMemoryLobbyLookupState.Ready(resolved)
                        }
                    )
                }
            } else {
                mutableStates.update { states -> states - contentId }
            }
            callbacks.forEach { callback -> callback(resolved) }
        }
    }

    private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }
}
