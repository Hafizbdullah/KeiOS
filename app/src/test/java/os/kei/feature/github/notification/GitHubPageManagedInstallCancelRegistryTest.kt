package os.kei.feature.github.notification

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubPageManagedInstallCancelRegistryTest {
    @Before
    fun setUp() {
        GitHubPageManagedInstallCancelRegistry.resetForTest()
    }

    @After
    fun tearDown() {
        GitHubPageManagedInstallCancelRegistry.resetForTest()
    }

    @Test
    fun `active cancellation consumes callback exactly once`() = runBlocking {
        var calls = 0
        GitHubPageManagedInstallCancelRegistry.register { calls += 1 }

        assertTrue(GitHubPageManagedInstallCancelRegistry.cancelActive())
        assertFalse(GitHubPageManagedInstallCancelRegistry.cancelActive())
        assertEquals(1, calls)
    }

    @Test
    fun `clearing old token preserves newer registration`() = runBlocking {
        var cancelled = ""
        val oldToken = GitHubPageManagedInstallCancelRegistry.register { cancelled = "old" }
        GitHubPageManagedInstallCancelRegistry.register { cancelled = "new" }

        GitHubPageManagedInstallCancelRegistry.clear(oldToken)

        assertTrue(GitHubPageManagedInstallCancelRegistry.cancelActive())
        assertEquals("new", cancelled)
    }
}
