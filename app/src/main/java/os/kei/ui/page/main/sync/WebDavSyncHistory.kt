package os.kei.ui.page.main.sync

import kotlinx.serialization.Serializable

internal const val WebDavSyncHistoryMaxEntries = 120

@Serializable
internal data class WebDavSyncHistoryPayload(
    val version: Int = 1,
    val entries: List<WebDavSyncHistoryEntry> = emptyList(),
)

@Serializable
internal data class WebDavSyncHistoryEntry(
    val id: String,
    val source: WebDavSyncHistorySource,
    val kind: WebDavSyncHistoryKind? = null,
    val reason: String,
    val status: WebDavAutoSyncStatus,
    val startedAtMs: Long,
    val finishedAtMs: Long,
    val targetCount: Int,
    val succeededCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val items: List<WebDavSyncHistoryItem> = emptyList(),
) {
    val durationMs: Long
        get() = (finishedAtMs - startedAtMs).coerceAtLeast(0L)

    val hasIssues: Boolean
        get() = status == WebDavAutoSyncStatus.NeedsReview || status == WebDavAutoSyncStatus.Failed
}

@Serializable
internal data class WebDavSyncHistoryItem(
    val item: WebDavSyncItem,
    val status: WebDavItemStatus,
    val detail: String? = null,
)

@Serializable
internal enum class WebDavSyncHistorySource {
    Manual,
    Auto,
    RemoteProbe,
}

@Serializable
internal enum class WebDavSyncHistoryKind {
    Sync,
    Upload,
    Download,
    RemoteProbe,
}

internal fun appendWebDavSyncHistory(
    current: List<WebDavSyncHistoryEntry>,
    entry: WebDavSyncHistoryEntry,
): List<WebDavSyncHistoryEntry> {
    val normalized = normalizeWebDavSyncHistoryEntry(entry)
    return (listOf(normalized) + current.filterNot { it.id == normalized.id })
        .sortedWith(
            compareByDescending<WebDavSyncHistoryEntry> { it.finishedAtMs }
                .thenByDescending { it.startedAtMs }
                .thenBy { it.id },
        )
        .take(WebDavSyncHistoryMaxEntries)
}

internal fun buildWebDavSyncHistoryEntry(
    source: WebDavSyncHistorySource,
    kind: WebDavSyncHistoryKind?,
    reason: String,
    startedAtMs: Long,
    finishedAtMs: Long,
    targetCount: Int,
    outcomes: List<Pair<WebDavSyncItem, WebDavItemOutcome>>,
    skippedCount: Int,
): WebDavSyncHistoryEntry {
    val succeededCount = outcomes.count { (_, outcome) -> outcome.isSuccess }
    val reviewCount = outcomes.count { (_, outcome) ->
        outcome.status == WebDavItemStatus.BaselineRequired ||
            outcome.status == WebDavItemStatus.ConflictUnresolved
    }
    val failedCount = outcomes.size - succeededCount
    val technicalFailureCount = failedCount - reviewCount
    val status =
        when {
            targetCount <= 0 || outcomes.isEmpty() -> WebDavAutoSyncStatus.Skipped
            technicalFailureCount > 0 -> WebDavAutoSyncStatus.Failed
            reviewCount > 0 -> WebDavAutoSyncStatus.NeedsReview
            else -> WebDavAutoSyncStatus.Success
        }
    return WebDavSyncHistoryEntry(
        id = buildWebDavSyncHistoryId(source, kind, reason, startedAtMs, finishedAtMs),
        source = source,
        kind = kind,
        reason = reason,
        status = status,
        startedAtMs = startedAtMs.coerceAtLeast(0L),
        finishedAtMs = finishedAtMs.coerceAtLeast(0L),
        targetCount = targetCount.coerceAtLeast(0),
        succeededCount = succeededCount.coerceAtLeast(0),
        failedCount = failedCount.coerceAtLeast(0),
        skippedCount = skippedCount.coerceAtLeast(0),
        items =
            outcomes.map { (item, outcome) ->
                WebDavSyncHistoryItem(
                    item = item,
                    status = outcome.status,
                    detail = outcome.detail?.takeIf { it.isNotBlank() }?.take(WebDavSyncHistoryDetailMaxLength),
                )
            },
    )
}

internal fun WebDavAutoSyncSummary.toHistoryEntry(
    source: WebDavSyncHistorySource,
    startedAtMs: Long,
    items: List<WebDavSyncHistoryItem> = emptyList(),
): WebDavSyncHistoryEntry =
    WebDavSyncHistoryEntry(
        id = buildWebDavSyncHistoryId(source, kind = null, reason, startedAtMs, finishedAtMs),
        source = source,
        kind = null,
        reason = reason,
        status = status,
        startedAtMs = startedAtMs.coerceAtLeast(0L),
        finishedAtMs = finishedAtMs.coerceAtLeast(0L),
        targetCount = targetCount.coerceAtLeast(0),
        succeededCount = succeededCount.coerceAtLeast(0),
        failedCount = failedCount.coerceAtLeast(0),
        skippedCount = skippedCount.coerceAtLeast(0),
        items = items.map(::normalizeWebDavSyncHistoryItem),
    )

internal fun WebDavBatchKind.toHistoryKind(): WebDavSyncHistoryKind =
    when (this) {
        WebDavBatchKind.Sync -> WebDavSyncHistoryKind.Sync
        WebDavBatchKind.Upload -> WebDavSyncHistoryKind.Upload
        WebDavBatchKind.Download -> WebDavSyncHistoryKind.Download
    }

private const val WebDavSyncHistoryDetailMaxLength = 240

private fun normalizeWebDavSyncHistoryEntry(entry: WebDavSyncHistoryEntry): WebDavSyncHistoryEntry =
    entry.copy(
        id = entry.id.ifBlank {
            buildWebDavSyncHistoryId(entry.source, entry.kind, entry.reason, entry.startedAtMs, entry.finishedAtMs)
        },
        reason = entry.reason.take(WebDavSyncHistoryDetailMaxLength),
        startedAtMs = entry.startedAtMs.coerceAtLeast(0L),
        finishedAtMs = entry.finishedAtMs.coerceAtLeast(0L),
        targetCount = entry.targetCount.coerceAtLeast(0),
        succeededCount = entry.succeededCount.coerceAtLeast(0),
        failedCount = entry.failedCount.coerceAtLeast(0),
        skippedCount = entry.skippedCount.coerceAtLeast(0),
        items = entry.items.map(::normalizeWebDavSyncHistoryItem),
    )

private fun normalizeWebDavSyncHistoryItem(item: WebDavSyncHistoryItem): WebDavSyncHistoryItem =
    item.copy(
        detail = item.detail?.takeIf { it.isNotBlank() }?.take(WebDavSyncHistoryDetailMaxLength),
    )

private fun buildWebDavSyncHistoryId(
    source: WebDavSyncHistorySource,
    kind: WebDavSyncHistoryKind?,
    reason: String,
    startedAtMs: Long,
    finishedAtMs: Long,
): String =
    listOf(
        source.name,
        kind?.name.orEmpty(),
        reason.ifBlank { "sync" },
        startedAtMs.coerceAtLeast(0L).toString(),
        finishedAtMs.coerceAtLeast(0L).toString(),
    ).joinToString(separator = ":")
