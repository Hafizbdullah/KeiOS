package os.kei.mcp.bridge

import os.kei.feature.home.mcp.McpHomeWebDavSnapshot
import os.kei.feature.home.mcp.McpHomeWebDavSnapshotProvider
import os.kei.ui.page.main.sync.WebDavSyncItem
import os.kei.ui.page.main.sync.WebDavSyncStore

internal data object AppMcpHomeWebDavSnapshotProvider : McpHomeWebDavSnapshotProvider {
    override fun loadSnapshot(): McpHomeWebDavSnapshot =
        McpHomeWebDavSnapshot(
            configured = WebDavSyncStore.hasConfig(),
            autoSyncEnabled = WebDavSyncStore.isAutoSyncEnabled(),
            enabledItemCount = WebDavSyncItem.entries.count(WebDavSyncStore::isItemEnabled),
            totalItemCount = WebDavSyncItem.entries.size,
            lastFullSyncMs = WebDavSyncStore.getLastFullSyncTime(),
        )
}
