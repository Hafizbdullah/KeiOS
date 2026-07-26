package os.kei.ui.page.main.github.page.action

import kotlin.test.assertEquals
import org.junit.Test

class GitHubRefreshCancelSignalTest {
    @Test
    fun `explicit user cancel keeps the terminal cancelled notification`() {
        assertEquals(
            GitHubRefreshCancelPresentation.Visible,
            GitHubRefreshCancelSignal.presentation(GitHubRefreshCancelSignal.EXPLICIT_REASON),
        )
    }

    @Test
    fun `page batch supersede hands the surface to the successor silently`() {
        assertEquals(
            GitHubRefreshCancelPresentation.SilentTakeover,
            GitHubRefreshCancelSignal.presentation(
                GitHubRefreshCancelSignal.SUPERSEDED_BY_PAGE_BATCH_REASON,
            ),
        )
    }

    @Test
    fun `lifecycle teardown cancellations dismiss the progress card silently`() {
        // viewModelScope disposal delivers a CancellationException with no message; any
        // unrecognized reason is machine-driven and must never pop a "cancelled" result.
        assertEquals(
            GitHubRefreshCancelPresentation.SilentDismiss,
            GitHubRefreshCancelSignal.presentation(null),
        )
        assertEquals(
            GitHubRefreshCancelPresentation.SilentDismiss,
            GitHubRefreshCancelSignal.presentation("Job was cancelled"),
        )
        assertEquals(
            GitHubRefreshCancelPresentation.SilentDismiss,
            GitHubRefreshCancelSignal.presentation(""),
        )
    }
}
