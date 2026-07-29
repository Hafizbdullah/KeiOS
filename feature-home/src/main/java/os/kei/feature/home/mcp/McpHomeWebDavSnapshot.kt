package os.kei.feature.home.mcp

data class McpHomeWebDavSnapshot(
    val configured: Boolean,
    val autoSyncEnabled: Boolean,
    val enabledItemCount: Int,
    val totalItemCount: Int,
    val lastFullSyncMs: Long,
)

fun interface McpHomeWebDavSnapshotProvider {
    fun loadSnapshot(): McpHomeWebDavSnapshot
}
