package os.kei.ui.page.main.sync

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class WebDavSyncPageDesignTest {
    @Test
    fun connectionWorkspaceUsesStableTaskShapedCards() {
        val pageSource = sourceFile(WEB_DAV_SYNC_PAGE_SOURCE)

        listOf(
            "webdav-connection-overview",
            "webdav-connection-credentials",
            "webdav-connection-workspace",
            "webdav-connection-actions",
        ).forEach { key ->
            assertTrue("key = \"$key\"" in pageSource, "Missing stable list key: $key")
        }
        listOf(
            "WebDavConnectionOverviewCard(",
            "WebDavCredentialsCard(",
            "WebDavRemoteWorkspaceCard(",
            "WebDavConnectionActionsCard(",
        ).forEach { call ->
            assertTrue(call in pageSource, "Missing connection section: $call")
        }
        assertFalse("WebDavConnectionCard(" in pageSource)
    }

    @Test
    fun redesignedSectionsUseSharedCardHierarchy() {
        val connectionSource = sourceFile(WEB_DAV_CONNECTION_CARDS_SOURCE)
        val dataSource = sourceFile(WEB_DAV_SYNC_CARDS_SOURCE)
        val historySource = sourceFile(WEB_DAV_HISTORY_CARDS_SOURCE)

        assertTrue("AppOverviewCard(" in connectionSource)
        assertTrue(connectionSource.countOccurrences("SettingsGroupCard(") == 3)
        assertFalse("internal fun WebDavConnectionCard(" in dataSource)
        assertTrue(dataSource.countOccurrences("AppOverviewCard(") >= 1)
        assertTrue(dataSource.countOccurrences("AppFeatureCard(") >= 2)
        assertTrue(historySource.countOccurrences("AppOverviewCard(") >= 1)
        assertTrue(historySource.countOccurrences("AppFeatureCard(") >= 1)
    }
}

private fun String.countOccurrences(needle: String): Int = windowed(needle.length).count { it == needle }

private fun sourceFile(relativePath: String): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val source =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, relativePath) }
            .firstOrNull(File::isFile)
    return requireNotNull(source) {
        "Unable to locate $relativePath from $workingDirectory"
    }.readText()
}

private const val WEB_DAV_SYNC_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/sync/WebDavSyncPage.kt"
private const val WEB_DAV_CONNECTION_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/sync/WebDavConnectionCards.kt"
private const val WEB_DAV_SYNC_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/sync/WebDavSyncCards.kt"
private const val WEB_DAV_HISTORY_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/sync/WebDavSyncHistoryCards.kt"
