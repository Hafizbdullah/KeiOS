package os.kei.feature.github.domain

import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitHubRefreshRuntimeStoreTest {
    @After
    fun tearDown() {
        GitHubRefreshRuntimeStore.clear()
    }

    @Test
    fun `begin creates a running scoped session`() {
        val session =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.AllTracked,
                source = GitHubRefreshSource.Page,
                totalTrackedCount = 75,
                targetCount = 75,
                targetTrackIds = listOf("track-a", "track-b"),
                nowMs = 1_000L,
            )

        assertNotNull(session)
        assertEquals(listOf("track-a", "track-b"), session.targetTrackIds)
        val state = GitHubRefreshRuntimeStore.state.value
        assertEquals(session.id, state.sessionId)
        assertEquals(GitHubRefreshRuntimePhase.Running, state.phase)
        assertEquals(GitHubRefreshScope.AllTracked, state.scope)
        assertEquals(GitHubRefreshSource.Page, state.source)
        assertEquals(75, state.totalTrackedCount)
        assertEquals(75, state.targetCount)
        assertEquals(listOf("track-a", "track-b"), state.targetTrackIds)
        assertEquals(1_000L, state.startedAtMs)
        assertTrue(state.running)
    }

    @Test
    fun `progress from a stale session is ignored`() {
        val staleSession =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.DueTracked,
                source = GitHubRefreshSource.BackgroundTick,
                totalTrackedCount = 75,
                targetCount = 1,
            )
        val activeSession =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.AllTracked,
                source = GitHubRefreshSource.Page,
                totalTrackedCount = 75,
                targetCount = 75,
            )
        assertNotNull(staleSession)
        assertNotNull(activeSession)

        GitHubRefreshRuntimeStore.progress(
            sessionId = staleSession.id,
            completedCount = 1,
            updatableCount = 1,
            preReleaseUpdateCount = 0,
            failedCount = 0,
        )

        val state = GitHubRefreshRuntimeStore.state.value
        assertEquals(activeSession.id, state.sessionId)
        assertEquals(0, state.completedCount)
        assertEquals(0, state.updatableCount)
        assertEquals(75, state.targetCount)
    }

    @Test
    fun `complete from a stale session is ignored`() {
        val staleSession =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.DueTracked,
                source = GitHubRefreshSource.BackgroundTick,
                totalTrackedCount = 75,
                targetCount = 1,
            )
        val activeSession =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.VisibleTracked,
                source = GitHubRefreshSource.Page,
                totalTrackedCount = 75,
                targetCount = 3,
            )
        assertNotNull(staleSession)
        assertNotNull(activeSession)

        GitHubRefreshRuntimeStore.complete(
            sessionId = staleSession.id,
            completedCount = 1,
            updatableCount = 1,
            preReleaseUpdateCount = 0,
            failedCount = 0,
        )

        val state = GitHubRefreshRuntimeStore.state.value
        assertEquals(activeSession.id, state.sessionId)
        assertEquals(GitHubRefreshRuntimePhase.Running, state.phase)
        assertTrue(state.running)
        assertEquals(0, state.completedCount)
    }

    @Test
    fun `new session supersedes previous session and resets progress`() {
        val first =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.AllTracked,
                source = GitHubRefreshSource.Page,
                totalTrackedCount = 10,
                targetCount = 10,
            )
        assertNotNull(first)
        GitHubRefreshRuntimeStore.progress(
            sessionId = first.id,
            completedCount = 6,
            updatableCount = 2,
            preReleaseUpdateCount = 1,
            failedCount = 1,
        )

        val second =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.ShortcutAllTracked,
                source = GitHubRefreshSource.Shortcut,
                totalTrackedCount = 75,
                targetCount = 75,
            )

        assertNotNull(second)
        val state = GitHubRefreshRuntimeStore.state.value
        assertEquals(second.id, state.sessionId)
        assertEquals(0, state.completedCount)
        assertEquals(0, state.updatableCount)
        assertEquals(0, state.preReleaseUpdateCount)
        assertEquals(0, state.failedCount)
        assertEquals(75, state.targetCount)
        assertEquals(GitHubRefreshScope.ShortcutAllTracked, state.scope)
    }

    @Test
    fun `skip policy rejects background session while another session runs`() {
        val active =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.AllTracked,
                source = GitHubRefreshSource.Page,
                totalTrackedCount = 75,
                targetCount = 75,
            )
        assertNotNull(active)

        val skipped =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.DueTracked,
                source = GitHubRefreshSource.BackgroundTick,
                totalTrackedCount = 75,
                targetCount = 2,
                policy = GitHubRefreshBeginPolicy.SkipWhenRunning,
            )

        assertNull(skipped)
        assertEquals(active.id, GitHubRefreshRuntimeStore.state.value.sessionId)
        assertTrue(GitHubRefreshRuntimeStore.state.value.running)
    }

    @Test
    fun `cancel closes only the matching session`() {
        val stale =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.DueTracked,
                source = GitHubRefreshSource.BackgroundTick,
                totalTrackedCount = 5,
                targetCount = 1,
            )
        val active =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.AllTracked,
                source = GitHubRefreshSource.Page,
                totalTrackedCount = 5,
                targetCount = 5,
            )
        assertNotNull(stale)
        assertNotNull(active)

        GitHubRefreshRuntimeStore.cancel(
            sessionId = stale.id,
            completedCount = 1,
            updatableCount = 1,
            preReleaseUpdateCount = 0,
            failedCount = 0,
        )

        assertTrue(GitHubRefreshRuntimeStore.state.value.running)

        GitHubRefreshRuntimeStore.cancel(
            sessionId = active.id,
            completedCount = 2,
            updatableCount = 1,
            preReleaseUpdateCount = 0,
            failedCount = 0,
        )

        val state = GitHubRefreshRuntimeStore.state.value
        assertFalse(state.running)
        assertEquals(GitHubRefreshRuntimePhase.Cancelled, state.phase)
        assertEquals(2, state.completedCount)
        assertEquals(1, state.updatableCount)
    }

    @Test
    fun `background terminal cleanup can be claimed once per session`() {
        val session =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.DueTracked,
                source = GitHubRefreshSource.BackgroundTick,
                totalTrackedCount = 74,
                targetCount = 74,
                nowMs = 1_000L,
            )
        assertNotNull(session)
        GitHubRefreshRuntimeStore.progress(
            sessionId = session.id,
            completedCount = 50,
            updatableCount = 2,
            preReleaseUpdateCount = 1,
            failedCount = 0,
            nowMs = 2_000L,
        )
        GitHubRefreshRuntimeStore.cancel(
            sessionId = session.id,
            completedCount = 50,
            updatableCount = 2,
            preReleaseUpdateCount = 1,
            failedCount = 0,
            nowMs = 2_100L,
        )

        val claimed = GitHubRefreshRuntimeStore.claimBackgroundTerminalCleanup(session.id)
        val duplicate = GitHubRefreshRuntimeStore.claimBackgroundTerminalCleanup(session.id)

        assertNotNull(claimed)
        assertEquals(50, claimed.completedCount)
        assertTrue(claimed.terminalCleanupClaimed)
        assertNull(duplicate)
        assertTrue(GitHubRefreshRuntimeStore.state.value.terminalCleanupClaimed)
    }

    @Test
    fun `stopped background cleanup claims its captured session without touching a newer session`() {
        val stopped =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.DueTracked,
                source = GitHubRefreshSource.BackgroundTick,
                totalTrackedCount = 74,
                targetCount = 74,
                nowMs = 1_000L,
            )
        assertNotNull(stopped)
        GitHubRefreshRuntimeStore.progress(
            sessionId = stopped.id,
            completedCount = 50,
            updatableCount = 2,
            preReleaseUpdateCount = 1,
            failedCount = 0,
            nowMs = 2_000L,
        )
        val stoppedSnapshot = GitHubRefreshRuntimeStore.state.value
        val active =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.DueTracked,
                source = GitHubRefreshSource.BackgroundTick,
                totalTrackedCount = 74,
                targetCount = 24,
                nowMs = 3_000L,
            )
        assertNotNull(active)

        val claimed = GitHubRefreshRuntimeStore.claimBackgroundTerminalCleanup(stoppedSnapshot)
        val duplicate = GitHubRefreshRuntimeStore.claimBackgroundTerminalCleanup(stoppedSnapshot)

        assertNotNull(claimed)
        assertEquals(stopped.id, claimed.sessionId)
        assertEquals(50, claimed.completedCount)
        assertNull(duplicate)
        assertEquals(active.id, GitHubRefreshRuntimeStore.state.value.sessionId)
        assertTrue(GitHubRefreshRuntimeStore.state.value.running)
        assertFalse(GitHubRefreshRuntimeStore.state.value.terminalCleanupClaimed)
    }

    @Test
    fun `stopped background cleanup does not replace a completed session`() {
        val session =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.DueTracked,
                source = GitHubRefreshSource.BackgroundTick,
                totalTrackedCount = 74,
                targetCount = 74,
                nowMs = 1_000L,
            )
        assertNotNull(session)
        GitHubRefreshRuntimeStore.progress(
            sessionId = session.id,
            completedCount = 74,
            updatableCount = 3,
            preReleaseUpdateCount = 1,
            failedCount = 0,
            nowMs = 2_000L,
        )
        val stoppedSnapshot = GitHubRefreshRuntimeStore.state.value
        GitHubRefreshRuntimeStore.complete(
            sessionId = session.id,
            completedCount = 74,
            updatableCount = 3,
            preReleaseUpdateCount = 1,
            failedCount = 0,
            nowMs = 2_100L,
        )

        val claimed = GitHubRefreshRuntimeStore.claimBackgroundTerminalCleanup(stoppedSnapshot)

        assertNull(claimed)
        assertEquals(GitHubRefreshRuntimePhase.Completed, GitHubRefreshRuntimeStore.state.value.phase)
        assertFalse(GitHubRefreshRuntimeStore.state.value.terminalCleanupClaimed)
    }

    @Test
    fun `completed background session is not replaced by a late cancellation`() {
        val session =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.DueTracked,
                source = GitHubRefreshSource.BackgroundTick,
                totalTrackedCount = 4,
                targetCount = 4,
                nowMs = 1_000L,
            )
        assertNotNull(session)
        GitHubRefreshRuntimeStore.complete(
            sessionId = session.id,
            completedCount = 4,
            updatableCount = 1,
            preReleaseUpdateCount = 0,
            failedCount = 0,
            nowMs = 2_000L,
        )

        GitHubRefreshRuntimeStore.cancel(
            sessionId = session.id,
            completedCount = 3,
            updatableCount = 0,
            preReleaseUpdateCount = 0,
            failedCount = 0,
            nowMs = 2_100L,
        )

        val state = GitHubRefreshRuntimeStore.state.value
        assertEquals(GitHubRefreshRuntimePhase.Completed, state.phase)
        assertEquals(4, state.completedCount)
        assertEquals(1, state.updatableCount)
    }

    @Test
    fun `claimed stopped session is not replaced by a late completion`() {
        val session =
            GitHubRefreshRuntimeStore.begin(
                scope = GitHubRefreshScope.DueTracked,
                source = GitHubRefreshSource.BackgroundTick,
                totalTrackedCount = 4,
                targetCount = 4,
                nowMs = 1_000L,
            )
        assertNotNull(session)
        GitHubRefreshRuntimeStore.progress(
            sessionId = session.id,
            completedCount = 3,
            updatableCount = 0,
            preReleaseUpdateCount = 0,
            failedCount = 0,
            nowMs = 1_900L,
        )
        assertNotNull(GitHubRefreshRuntimeStore.claimBackgroundTerminalCleanup(session.id))

        GitHubRefreshRuntimeStore.complete(
            sessionId = session.id,
            completedCount = 4,
            updatableCount = 1,
            preReleaseUpdateCount = 0,
            failedCount = 0,
            nowMs = 2_000L,
        )

        val state = GitHubRefreshRuntimeStore.state.value
        assertEquals(GitHubRefreshRuntimePhase.Running, state.phase)
        assertEquals(3, state.completedCount)
        assertTrue(state.terminalCleanupClaimed)
    }
}
