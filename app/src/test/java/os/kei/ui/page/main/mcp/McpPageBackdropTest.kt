package os.kei.ui.page.main.mcp

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class McpPageBackdropTest {
    @Test
    fun liquidPresentationFollowsTheKeiOSAppTheme() {
        val source = sourceFile(MCP_PAGE_SOURCE)

        assertFalse("isSystemInDarkTheme" in source)
        assertEquals(1, source.occurrencesOf("isAppInDarkTheme()"))
    }

    @Test
    fun contentProducerPrecedesPullRefreshListAndFloatingDockConsumers() {
        val source = sourceFile(MCP_PAGE_CONTENT_SOURCE)
        val sceneIndex = source.indexOf("MainPageContentBackdropScene(")
        val contentIdentityIndex =
            source.indexOf(
                "contentBackdrop = backdrops.content,",
                startIndex = sceneIndex.coerceAtLeast(0),
            )
        val listIndex =
            source.indexOf(
                "AppPageLazyColumn(",
                startIndex = contentIdentityIndex.coerceAtLeast(0),
            )
        val pullRefreshIndex =
            source.indexOf(
                "PullToRefresh(",
                startIndex = contentIdentityIndex.coerceAtLeast(0),
            )
        val dockIndex = source.indexOf("McpPageFloatingActionDock(", startIndex = listIndex.coerceAtLeast(0))

        assertTrue(sceneIndex >= 0, "MCP page must host the shared content Backdrop scene")
        assertTrue(contentIdentityIndex > sceneIndex, "The scene producer must receive the MCP content identity")
        assertTrue(pullRefreshIndex > contentIdentityIndex, "Pull refresh must use the produced content scene")
        assertTrue(listIndex > pullRefreshIndex, "The page list must be composed inside pull refresh")
        assertTrue(dockIndex > listIndex, "The floating dock must be composed after the page list")
        assertTrue(
            source.indexOf("backdrop = backdrops.topBar", startIndex = dockIndex) > dockIndex,
            "The floating dock must sample scrolling content",
        )
        assertEquals(1, source.occurrencesOf("MainPageContentBackdropScene("))
        assertEquals(1, source.occurrencesOf("contentBackdrop = backdrops.content,"))
    }

    @Test
    fun refreshLivesInPullGestureAndLeavesTheFloatingDock() {
        val contentSource = sourceFile(MCP_PAGE_CONTENT_SOURCE)
        val dockSource = sourceFile(MCP_PAGE_FLOATING_ACTIONS_SOURCE)

        assertEquals(1, contentSource.occurrencesOf("PullToRefresh("))
        assertEquals(1, contentSource.occurrencesOf("onRefresh = actions.onRefreshNow"))
        assertFalse("appLucideRefreshIcon" in dockSource)
        assertFalse("actions.onRefreshNow" in dockSource)
        assertFalse("refreshRunning" in dockSource)
    }

    @Test
    fun topBarProducerStaysIndependentFromContentConsumers() {
        val source = sourceFile(MCP_PAGE_CONTENT_SOURCE)

        assertEquals(1, source.occurrencesOf(".layerBackdrop(backdrops.topBar)"))
        assertEquals(0, source.occurrencesOf(".layerBackdrop(backdrops.content)"))
        assertEquals(12, source.occurrencesOf("backdrop = backdrops.content"))
    }

    @Test
    fun pageWiresTopContentAndSheetBackdropRolesSeparately() {
        val pageSource = sourceFile(MCP_PAGE_SOURCE)
        val contentSource = sourceFile(MCP_PAGE_CONTENT_SOURCE)
        val sheetSource = sourceFile(MCP_PAGE_SHEETS_SOURCE)

        assertTrue("rememberMainPageBackdropSet(" in pageSource)
        assertTrue("distinctLayers = pageBackdropEffectsEnabled" in pageSource)
        assertTrue("titleBackdrop = backdrops.topBar" in pageSource)
        assertTrue("backdrop = backdrops.topBar" in pageSource)
        assertTrue("backdrops = backdrops" in pageSource)
        assertTrue("contentBackdrop = backdrops.content" in contentSource)
        assertEquals(1, sheetSource.occurrencesOf("backdrop = backdrops.sheet"))
        assertEquals(0, sheetSource.occurrencesOf("backdrop = backdrops.content"))
        assertEquals(0, sheetSource.occurrencesOf("backdrop = backdrops.topBar"))
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

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private const val MCP_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/mcp/McpPage.kt"

private const val MCP_PAGE_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/mcp/McpPageContent.kt"

private const val MCP_PAGE_SHEETS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/mcp/McpPageSheets.kt"

private const val MCP_PAGE_FLOATING_ACTIONS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/mcp/McpPageFloatingActions.kt"
