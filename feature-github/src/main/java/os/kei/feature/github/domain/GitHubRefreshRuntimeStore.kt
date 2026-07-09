package os.kei.feature.github.domain

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class GitHubRefreshRuntimePhase {
    Idle,
    Running,
    Completed,
    Cancelled,
}

enum class GitHubRefreshScope {
    AllTracked,
    DueTracked,
    VisibleTracked,
    RequestedTracked,
    MissingCache,
    SingleTracked,
    ShortcutAllTracked,
}

enum class GitHubRefreshSource {
    Page,
    BackgroundTick,
    Shortcut,
    Debug,
}

enum class GitHubRefreshBeginPolicy {
    SupersedeRunning,
    SkipWhenRunning,
}

data class GitHubRefreshRuntimeSession(
    val id: Long,
    val scope: GitHubRefreshScope,
    val source: GitHubRefreshSource,
    val targetTrackIds: List<String> = emptyList(),
)

data class GitHubRefreshRuntimeState(
    val sessionId: Long = 0L,
    val phase: GitHubRefreshRuntimePhase = GitHubRefreshRuntimePhase.Idle,
    val scope: GitHubRefreshScope = GitHubRefreshScope.AllTracked,
    val source: GitHubRefreshSource = GitHubRefreshSource.Page,
    val running: Boolean = false,
    val totalTrackedCount: Int = 0,
    val targetCount: Int = 0,
    val targetTrackIds: List<String> = emptyList(),
    val completedCount: Int = 0,
    val updatableCount: Int = 0,
    val preReleaseUpdateCount: Int = 0,
    val failedCount: Int = 0,
    val startedAtMs: Long = 0L,
    val updatedAtMs: Long = 0L,
    val finishedAtMs: Long = 0L,
    val terminalCleanupClaimed: Boolean = false,
) {
    val safeTargetCount: Int
        get() = targetCount.coerceAtLeast(1)

    val safeCompletedCount: Int
        get() = completedCount.coerceIn(0, safeTargetCount)

    val progressFraction: Float
        get() = if (targetCount <= 0) 0f else safeCompletedCount.toFloat() / safeTargetCount.toFloat()
}

object GitHubRefreshRuntimeStore {
    private val sessionIds = AtomicLong(0L)
    private val terminalCleanupClaims = ConcurrentHashMap.newKeySet<Long>()
    private val runtimeStateLock = Any()
    private val _state = MutableStateFlow(GitHubRefreshRuntimeState())
    val state: StateFlow<GitHubRefreshRuntimeState> = _state.asStateFlow()

    fun begin(
        scope: GitHubRefreshScope,
        source: GitHubRefreshSource,
        totalTrackedCount: Int,
        targetCount: Int,
        targetTrackIds: Collection<String> = emptyList(),
        policy: GitHubRefreshBeginPolicy = GitHubRefreshBeginPolicy.SupersedeRunning,
        nowMs: Long = System.currentTimeMillis(),
    ): GitHubRefreshRuntimeSession? =
        synchronized(runtimeStateLock) {
            val current = _state.value
            if (policy == GitHubRefreshBeginPolicy.SkipWhenRunning && current.running) {
                return@synchronized null
            }
            val sessionId = sessionIds.incrementAndGet()
            val normalizedTargetTrackIds = normalizeRuntimeTargetTrackIds(targetTrackIds)
            _state.value =
                GitHubRefreshRuntimeState(
                    sessionId = sessionId,
                    phase = GitHubRefreshRuntimePhase.Running,
                    scope = scope,
                    source = source,
                    running = true,
                    totalTrackedCount = totalTrackedCount.coerceAtLeast(0),
                    targetCount = targetCount.coerceAtLeast(0),
                    targetTrackIds = normalizedTargetTrackIds,
                    completedCount = 0,
                    startedAtMs = nowMs,
                    updatedAtMs = nowMs,
                )
            GitHubRefreshRuntimeSession(
                id = sessionId,
                scope = scope,
                source = source,
                targetTrackIds = normalizedTargetTrackIds,
            )
        }

    fun progress(
        sessionId: Long,
        completedCount: Int,
        updatableCount: Int,
        preReleaseUpdateCount: Int,
        failedCount: Int,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        _state.update { current ->
            if (current.sessionId != sessionId || !current.running) {
                current
            } else {
                current.copy(
                    completedCount = completedCount.coerceIn(0, current.safeTargetCount),
                    updatableCount = updatableCount.coerceAtLeast(0),
                    preReleaseUpdateCount = preReleaseUpdateCount.coerceAtLeast(0),
                    failedCount = failedCount.coerceAtLeast(0),
                    updatedAtMs = nowMs.coerceAtLeast(current.updatedAtMs),
                )
            }
        }
    }

    fun complete(
        sessionId: Long,
        completedCount: Int,
        updatableCount: Int,
        preReleaseUpdateCount: Int,
        failedCount: Int,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        synchronized(runtimeStateLock) {
            _state.update { current ->
                if (
                    current.sessionId != sessionId ||
                    !current.running ||
                    current.terminalCleanupClaimed
                ) {
                    current
                } else {
                    current.copy(
                        phase = GitHubRefreshRuntimePhase.Completed,
                        running = false,
                        completedCount = completedCount.coerceIn(0, current.safeTargetCount),
                        updatableCount = updatableCount.coerceAtLeast(0),
                        preReleaseUpdateCount = preReleaseUpdateCount.coerceAtLeast(0),
                        failedCount = failedCount.coerceAtLeast(0),
                        updatedAtMs = nowMs.coerceAtLeast(current.updatedAtMs),
                        finishedAtMs = nowMs.coerceAtLeast(current.startedAtMs),
                    )
                }
            }
        }
    }

    fun cancel(
        sessionId: Long,
        completedCount: Int,
        updatableCount: Int,
        preReleaseUpdateCount: Int,
        failedCount: Int,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        synchronized(runtimeStateLock) {
            _state.update { current ->
                if (
                    current.sessionId != sessionId ||
                    (
                        current.phase != GitHubRefreshRuntimePhase.Running &&
                            current.phase != GitHubRefreshRuntimePhase.Cancelled
                    )
                ) {
                    current
                } else {
                    current.copy(
                        phase = GitHubRefreshRuntimePhase.Cancelled,
                        running = false,
                        completedCount = completedCount.coerceIn(0, current.safeTargetCount),
                        updatableCount = updatableCount.coerceAtLeast(0),
                        preReleaseUpdateCount = preReleaseUpdateCount.coerceAtLeast(0),
                        failedCount = failedCount.coerceAtLeast(0),
                        updatedAtMs = nowMs.coerceAtLeast(current.updatedAtMs),
                        finishedAtMs = nowMs.coerceAtLeast(current.startedAtMs),
                    )
                }
            }
        }
    }

    fun claimBackgroundTerminalCleanup(sessionId: Long): GitHubRefreshRuntimeState? {
        val runtime = _state.value.takeIf { it.sessionId == sessionId } ?: return null
        return claimBackgroundTerminalCleanup(runtime)
    }

    fun claimBackgroundTerminalCleanup(
        runtime: GitHubRefreshRuntimeState,
    ): GitHubRefreshRuntimeState? =
        synchronized(runtimeStateLock) {
            val canClaim =
                runtime.sessionId > 0L &&
                    runtime.source == GitHubRefreshSource.BackgroundTick &&
                    (runtime.running || runtime.phase == GitHubRefreshRuntimePhase.Cancelled)
            val current = _state.value
            val alreadyCompleted =
                current.sessionId == runtime.sessionId &&
                    current.phase == GitHubRefreshRuntimePhase.Completed
            if (
                !canClaim ||
                alreadyCompleted ||
                !terminalCleanupClaims.add(runtime.sessionId)
            ) {
                return@synchronized null
            }

            _state.update { latest ->
                if (latest.sessionId == runtime.sessionId) {
                    latest.copy(terminalCleanupClaimed = true)
                } else {
                    latest
                }
            }
            runtime.copy(terminalCleanupClaimed = true)
        }

    fun clear(sessionId: Long? = null) {
        if (sessionId == null) {
            terminalCleanupClaims.clear()
        } else {
            terminalCleanupClaims.remove(sessionId)
        }
        _state.update { current ->
            if (sessionId == null || current.sessionId == sessionId) {
                GitHubRefreshRuntimeState()
            } else {
                current
            }
        }
    }
}

private const val MAX_RUNTIME_TARGET_TRACK_IDS = 512

private fun normalizeRuntimeTargetTrackIds(targetTrackIds: Collection<String>): List<String> =
    targetTrackIds
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(MAX_RUNTIME_TARGET_TRACK_IDS)
        .toList()
