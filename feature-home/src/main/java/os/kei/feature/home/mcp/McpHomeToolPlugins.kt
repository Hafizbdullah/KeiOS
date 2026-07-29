package os.kei.feature.home.mcp

import io.modelcontextprotocol.kotlin.sdk.server.Server
import os.kei.mcp.server.McpServerToolPlugin
import os.kei.mcp.server.McpToolCatalog
import os.kei.mcp.server.McpToolEnvironment

object McpHomeToolPlugins {
    fun create(
        baSnapshotProvider: McpHomeBaSnapshotProvider,
        webDavSnapshotProvider: McpHomeWebDavSnapshotProvider,
    ): List<McpServerToolPlugin> =
        listOf(HomePlugin(baSnapshotProvider, webDavSnapshotProvider))

    private data class HomePlugin(
        private val baSnapshotProvider: McpHomeBaSnapshotProvider,
        private val webDavSnapshotProvider: McpHomeWebDavSnapshotProvider,
    ) : McpServerToolPlugin {
        override val toolNames: List<String> = McpToolCatalog.homeToolNames

        override fun registerTools(
            server: Server,
            environment: McpToolEnvironment,
        ) {
            McpHomeTools(
                environment = environment,
                baSnapshotProvider = baSnapshotProvider,
                webDavSnapshotProvider = webDavSnapshotProvider,
            ).register(server)
        }
    }
}
