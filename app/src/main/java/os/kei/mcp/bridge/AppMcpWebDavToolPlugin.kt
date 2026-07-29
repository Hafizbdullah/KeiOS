package os.kei.mcp.bridge

import android.net.Uri
import io.modelcontextprotocol.kotlin.sdk.server.Server
import os.kei.mcp.server.McpServerToolPlugin
import os.kei.mcp.server.McpToolCatalog
import os.kei.mcp.server.McpToolEnvironment
import os.kei.mcp.server.addMcpTextTool
import os.kei.mcp.server.argBoolean
import os.kei.mcp.server.argInt
import os.kei.mcp.server.argString
import os.kei.ui.page.main.sync.WebDavSyncHistoryEntry
import os.kei.ui.page.main.sync.WebDavSyncItem
import os.kei.ui.page.main.sync.WebDavSyncStore

internal data object AppMcpWebDavToolPlugin : McpServerToolPlugin {
    override val toolNames: List<String> = McpToolCatalog.webDavToolNames

    override fun registerTools(
        server: Server,
        environment: McpToolEnvironment,
    ) {
        server.addMcpTextTool(environment, name = "keios.webdav.status") {
            buildStatusText()
        }
        server.addMcpTextTool(environment, name = "keios.webdav.history") { request ->
            buildHistoryText(
                mode = argString(request.arguments?.get("mode")).ifBlank { "summary" },
                limit = argInt(request.arguments?.get("limit"), 20).coerceIn(1, 100),
                issuesOnly = argBoolean(request.arguments?.get("issuesOnly"), false),
                id = argString(request.arguments?.get("id")).trim(),
            )
        }
    }

    private fun buildStatusText(): String {
        val config = WebDavSyncStore.loadConfig()
        val items = WebDavSyncItem.entries
        val enabledItems = items.filter(WebDavSyncStore::isItemEnabled)
        val pendingItems = items.mapNotNull { item ->
            WebDavSyncStore.loadItemPendingSummary(item)?.let { item to it }
        }
        val autoSummary = WebDavSyncStore.loadLastAutoSyncSummary()
        return buildString {
            appendLine("configured=${config != null}")
            appendLine("provider=${WebDavSyncStore.loadProvider().name}")
            appendLine("serverHost=${config?.serverUrl?.safeWebDavHost().orEmpty()}")
            appendLine("remoteDir=${config?.remoteDir.orEmpty().safeValue()}")
            appendLine("autoSyncEnabled=${WebDavSyncStore.isAutoSyncEnabled()}")
            appendLine("autoSyncIntervalHours=${WebDavSyncStore.getAutoSyncIntervalHours()}")
            appendLine("enabledItems=${enabledItems.joinToString(",") { it.name }}")
            appendLine("enabledItemCount=${enabledItems.size}")
            appendLine("totalItemCount=${items.size}")
            appendLine("pendingReviewCount=${pendingItems.size}")
            appendLine("lastFullSyncMs=${WebDavSyncStore.getLastFullSyncTime()}")
            appendLine("lastRemoteProbeMs=${WebDavSyncStore.getLastRemoteProbeTime()}")
            appendLine("lastJobStopReason=${WebDavSyncStore.getLastJobStopReason().orEmpty().safeValue()}")
            appendLine("lastAutoSync.status=${autoSummary?.status?.name.orEmpty()}")
            appendLine("lastAutoSync.reason=${autoSummary?.reason.orEmpty().safeValue()}")
            appendLine("lastAutoSync.finishedAtMs=${autoSummary?.finishedAtMs ?: 0L}")
            appendLine("lastAutoSync.targetCount=${autoSummary?.targetCount ?: 0}")
            appendLine("lastAutoSync.succeededCount=${autoSummary?.succeededCount ?: 0}")
            appendLine("lastAutoSync.failedCount=${autoSummary?.failedCount ?: 0}")
            appendLine("lastAutoSync.skippedCount=${autoSummary?.skippedCount ?: 0}")
            items.forEach { item ->
                val pending = WebDavSyncStore.loadItemPendingSummary(item)
                val remote = WebDavSyncStore.loadRemoteSummary(item)
                appendLine("item.${item.name}.enabled=${WebDavSyncStore.isItemEnabled(item)}")
                appendLine("item.${item.name}.lastSyncMs=${WebDavSyncStore.getLastSyncTime(item)}")
                appendLine("item.${item.name}.pending=${pending?.state?.name.orEmpty()}")
                appendLine("item.${item.name}.pendingUpdatedAtMs=${pending?.updatedAtMs ?: 0L}")
                appendLine("item.${item.name}.remoteEmpty=${remote?.empty ?: false}")
                appendLine("item.${item.name}.remoteItemCount=${remote?.itemCount ?: -1}")
                appendLine("item.${item.name}.remoteByteSize=${remote?.byteSize ?: -1L}")
                appendLine("item.${item.name}.remoteProbedAtMs=${remote?.probedAtMs ?: 0L}")
            }
        }.trim()
    }

    private fun buildHistoryText(
        mode: String,
        limit: Int,
        issuesOnly: Boolean,
        id: String,
    ): String {
        val history = WebDavSyncStore.loadHistory()
            .asSequence()
            .filter { !issuesOnly || it.hasIssues }
            .take(limit)
            .toList()
        return when (mode.lowercase()) {
            "detail" -> {
                val entry = history.firstOrNull { id.isBlank() || it.id == id }
                entry?.toDetailText() ?: "found=false\nrequestedId=${id.safeValue()}"
            }
            "list" -> buildString {
                appendLine("count=${history.size}")
                history.forEachIndexed { index, entry ->
                    appendLine("entry.$index=${entry.toSummaryLine()}")
                }
            }.trim()
            else -> buildString {
                appendLine("totalCount=${WebDavSyncStore.loadHistory().size}")
                appendLine("matchedCount=${history.size}")
                appendLine("issueCount=${history.count { it.hasIssues }}")
                appendLine("successCount=${history.count { it.status.name == "Success" }}")
                appendLine("lastId=${history.firstOrNull()?.id.orEmpty()}")
                appendLine("lastStatus=${history.firstOrNull()?.status?.name.orEmpty()}")
                appendLine("lastFinishedAtMs=${history.firstOrNull()?.finishedAtMs ?: 0L}")
            }.trim()
        }
    }

    private fun WebDavSyncHistoryEntry.toSummaryLine(): String =
        listOf(
            "id=${id.safeValue()}",
            "source=${source.name}",
            "kind=${kind?.name.orEmpty()}",
            "status=${status.name}",
            "finishedAtMs=$finishedAtMs",
            "durationMs=$durationMs",
            "succeeded=$succeededCount",
            "failed=$failedCount",
            "skipped=$skippedCount",
        ).joinToString(" | ")

    private fun WebDavSyncHistoryEntry.toDetailText(): String = buildString {
        appendLine("found=true")
        appendLine("id=${id.safeValue()}")
        appendLine("source=${source.name}")
        appendLine("kind=${kind?.name.orEmpty()}")
        appendLine("reason=${reason.safeValue()}")
        appendLine("status=${status.name}")
        appendLine("startedAtMs=$startedAtMs")
        appendLine("finishedAtMs=$finishedAtMs")
        appendLine("durationMs=$durationMs")
        appendLine("targetCount=$targetCount")
        appendLine("succeededCount=$succeededCount")
        appendLine("failedCount=$failedCount")
        appendLine("skippedCount=$skippedCount")
        items.forEachIndexed { index, item ->
            appendLine(
                "item.$index=${item.item.name} | status=${item.status.name} | detail=${item.detail.orEmpty().safeValue()}",
            )
        }
        runtimeDiagnostics?.let { diagnostics ->
            appendLine("runtime.powerRestricted=${diagnostics.powerRestricted}")
            appendLine("runtime.networkRestricted=${diagnostics.networkRestricted}")
            appendLine("runtime.backgroundDataRestricted=${diagnostics.backgroundDataRestricted}")
            appendLine("runtime.networkValidated=${diagnostics.networkValidated}")
            appendLine("runtime.appStandbyBucket=${diagnostics.appStandbyBucket.safeValue()}")
            appendLine("runtime.queuedDurationMs=${diagnostics.queuedDurationMs}")
            appendLine("runtime.pendingReasons=${diagnostics.pendingReasons.joinToString(",").safeValue()}")
            appendLine("runtime.previousStopReason=${diagnostics.previousStopReason.orEmpty().safeValue()}")
        }
    }.trim()

    private fun String.safeWebDavHost(): String =
        runCatching { Uri.parse(this).host.orEmpty() }.getOrDefault("")

    private fun String.safeValue(): String =
        replace('\n', ' ').replace('\r', ' ').take(240)
}
