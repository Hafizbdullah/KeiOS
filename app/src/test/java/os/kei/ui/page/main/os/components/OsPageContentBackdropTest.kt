package os.kei.ui.page.main.os.components

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class OsPageContentBackdropTest {
    @Test
    fun pageSceneUsesDirectContentMaterialBeforeListAndFloatingDockConsumers() {
        val source = sourceFile(OS_PAGE_MAIN_LIST_SOURCE)
        val pageSource = sourceFile(OS_PAGE_SOURCE)
        val uiContextSource = sourceFile(OS_PAGE_UI_CONTEXT_SOURCE)
        val sceneIndex = source.indexOf("MainPageContentBackdropScene(")
        val listIndex = source.indexOf("AppPageLazyColumn(", startIndex = sceneIndex.coerceAtLeast(0))
        val dockIndex = source.indexOf("AppFloatingVerticalSearchActionDock(", startIndex = listIndex.coerceAtLeast(0))

        assertTrue(sceneIndex >= 0, "OS page must host the shared content Backdrop scene")
        assertTrue(listIndex > sceneIndex, "The page list must be a consumer sibling inside the scene")
        assertTrue(dockIndex > listIndex, "The floating dock must be composed after the page list")
        assertEquals(1, source.occurrencesOf("MainPageContentBackdropScene("))
        assertTrue(
            """MainPageContentBackdropScene(
        contentBackdrop = contentBackdrop,
        sheetBackdrop = sheetBackdrop,
        producerActive = chromeState.backdropProducerActive,
        modifier = Modifier.fillMaxSize(),""" in source,
        )
        assertTrue("contentBackdrop: Backdrop" in source)
        assertTrue("contentBackdrop = backdrops.contentMaterial" in pageSource)
        assertTrue("backdropProducerActive = pageBackdropEffectsEnabled && overlaySheetVisible" in pageSource)
        assertTrue("useSolidSurfaceBackdrops = true" in uiContextSource)
        assertEquals(0, source.occurrencesOf(".layerBackdrop(contentBackdrop)"))
    }

    @Test
    fun topBarProducerAndContentConsumersRemainConnected() {
        val source = sourceFile(OS_PAGE_MAIN_LIST_SOURCE)

        assertEquals(1, source.occurrencesOf(".layerBackdrop(topBarBackdrop)"))
        assertTrue("backdrop = contentBackdrop" in source)
        assertTrue("contentBackdrop = contentBackdrop" in source)
        assertTrue(
            source.indexOf("backdrop = topBarBackdrop", startIndex = source.indexOf("AppFloatingVerticalSearchActionDock(")) >= 0,
            "Floating dock must sample the scrolling-content identity",
        )
    }

    @Test
    fun overviewMovesTopMetricIntoTheTitleRow() {
        val source = sourceFile(OS_PAGE_MAIN_LIST_SOURCE)

        assertTrue("val topOverviewPill = overviewPills.firstOrNull()" in source)
        assertTrue("val bodyOverviewPills = overviewPills.drop(1)" in source)
        assertTrue("titleAccessory = {" in source)
        assertTrue("AppOverviewPillItem(pill = pill)" in source)
        assertTrue("pills = bodyOverviewPills" in source)
    }

    @Test
    fun pullToRefreshReplacesTheVisibleDockRefreshAction() {
        val source = sourceFile(OS_PAGE_MAIN_LIST_SOURCE)
        val pullToRefreshIndex = source.indexOf("PullToRefresh(")
        val pullRefreshCallbackIndex =
            source.indexOf("onRefreshAll()", startIndex = pullToRefreshIndex.coerceAtLeast(0))
        val listIndex = source.indexOf("AppPageLazyColumn(", startIndex = pullToRefreshIndex.coerceAtLeast(0))
        val dockIndex = source.indexOf("AppFloatingVerticalSearchActionDock(")
        val hiddenRefreshActionIndex =
            source.indexOf("showRefreshAction = false,", startIndex = dockIndex.coerceAtLeast(0))

        assertTrue(pullToRefreshIndex >= 0, "OS content must provide pull-to-refresh")
        assertTrue(
            pullRefreshCallbackIndex > pullToRefreshIndex,
            "Pull-to-refresh must trigger the full OS parameter refresh",
        )
        assertTrue(listIndex > pullRefreshCallbackIndex, "The parameter list must be inside pull-to-refresh")
        assertTrue(dockIndex > listIndex, "The floating dock must remain after pull-to-refresh content")
        assertTrue(hiddenRefreshActionIndex > dockIndex, "The OS dock must hide its redundant refresh action")
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

private const val OS_PAGE_MAIN_LIST_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/components/OsPageMainList.kt"
private const val OS_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/OsPage.kt"
private const val OS_PAGE_UI_CONTEXT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/state/OsPageUiContext.kt"
