package os.kei.ui.page.main.sync

internal fun pendingWebDavAutoSyncReviewItems(
    itemStates: Map<WebDavSyncItem, WebDavSyncItemUiState>,
): List<WebDavSyncItem> =
    WebDavSyncItem.entries.filter { item ->
        val state = itemStates[item] ?: return@filter false
        state.enabled && state.pendingSummary?.requiresManualReview == true
    }

internal val WebDavSyncPendingSummary.requiresManualReview: Boolean
    get() =
        state == WebDavSyncPendingState.RemoteConflict ||
            state == WebDavSyncPendingState.BaselineRequired
