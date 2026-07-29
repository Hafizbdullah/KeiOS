package os.kei.mcp.bridge

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppMcpWebDavToolPluginSourceTest {
    @Test
    fun statusOutputKeepsWebDavCredentialsOutOfTheToolContract() {
        val source = sourceFile(WEB_DAV_PLUGIN_SOURCE)

        assertTrue("safeWebDavHost()" in source)
        assertTrue("serverHost=" in source)
        assertFalse(".username" in source)
        assertFalse(".appPassword" in source)
        assertFalse("authorization" in source.lowercase())
    }

    @Test
    fun toolsStayReadOnlyAndExposeStatusAndHistory() {
        val source = sourceFile(WEB_DAV_PLUGIN_SOURCE)

        assertTrue("keios.webdav.status" in source)
        assertTrue("keios.webdav.history" in source)
        assertFalse("saveConfig(" in source)
        assertFalse("appendHistory(" in source)
        assertFalse("clearHistory(" in source)
    }
}

private fun sourceFile(relativePath: String): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val sourceFile =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, relativePath) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $relativePath from $workingDirectory"
    }.readText()
}

private const val WEB_DAV_PLUGIN_SOURCE =
    "app/src/main/java/os/kei/mcp/bridge/AppMcpWebDavToolPlugin.kt"
