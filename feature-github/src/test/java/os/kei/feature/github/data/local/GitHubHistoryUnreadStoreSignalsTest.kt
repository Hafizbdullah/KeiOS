package os.kei.feature.github.data.local

import kotlin.test.assertTrue
import org.junit.Test

class GitHubHistoryUnreadStoreSignalsTest {
    @Test
    fun `notifyChanged keeps versions monotonic when timestamps match`() {
        val previous = GitHubHistoryUnreadStoreSignals.version.value

        GitHubHistoryUnreadStoreSignals.notifyChanged(previous)
        val first = GitHubHistoryUnreadStoreSignals.version.value
        GitHubHistoryUnreadStoreSignals.notifyChanged(previous)
        val second = GitHubHistoryUnreadStoreSignals.version.value

        assertTrue(first > previous)
        assertTrue(second > first)
    }
}
