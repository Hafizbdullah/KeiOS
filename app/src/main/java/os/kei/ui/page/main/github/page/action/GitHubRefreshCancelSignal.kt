package os.kei.ui.page.main.github.page.action

/**
 * How a cancelled refresh session ends on the notification surface.
 *
 * Only a cancellation the user themselves initiated may leave a terminal "cancelled"
 * card; every machine-driven cancellation ends silently, otherwise internal supersedes
 * and lifecycle teardowns occasionally surface as spurious "已取消" results.
 */
internal enum class GitHubRefreshCancelPresentation {
    /** User pressed cancel: post the terminal cancelled notification. */
    Visible,

    /**
     * A newer page session replaces this one and immediately reposts on the same
     * notification id: post nothing and leave the surface to the successor.
     */
    SilentTakeover,

    /**
     * Lifecycle teardown (scope disposal, no successor): dismiss the progress card
     * without a terminal so nothing lingers and nothing pops.
     */
    SilentDismiss,
}

internal object GitHubRefreshCancelSignal {
    const val EXPLICIT_REASON = "cancelRefreshAll"
    const val SUPERSEDED_BY_PAGE_BATCH_REASON = "superseded_by_page_batch"

    fun presentation(reason: String?): GitHubRefreshCancelPresentation =
        when (reason) {
            EXPLICIT_REASON -> GitHubRefreshCancelPresentation.Visible
            SUPERSEDED_BY_PAGE_BATCH_REASON -> GitHubRefreshCancelPresentation.SilentTakeover
            else -> GitHubRefreshCancelPresentation.SilentDismiss
        }
}
