package os.kei.ui.page.main.github.page

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class GitHubPageBackdropTest {
    @Test
    fun contentProducerPrecedesOverviewTrackedAndDockConsumers() {
        val source = sourceFile(GITHUB_MAIN_CONTENT_SOURCE)
        val sceneIndex = source.indexOf("MainPageContentBackdropScene(")
        val sceneBackdropIndex =
            source.indexOf(
                "contentBackdrop = surfaces.contentBackdrop,",
                startIndex = sceneIndex.coerceAtLeast(0),
            )
        val scaffoldIndex = source.indexOf("AppScaffold(", startIndex = sceneBackdropIndex.coerceAtLeast(0))
        val overviewIndex = source.indexOf("GitHubOverviewCard(", startIndex = scaffoldIndex.coerceAtLeast(0))
        val overviewBackdropIndex =
            source.indexOf("backdrop = surfaces.contentBackdrop,", startIndex = overviewIndex.coerceAtLeast(0))
        val trackedIndex =
            source.indexOf("GitHubTrackedItemsSurfaces(", startIndex = overviewBackdropIndex.coerceAtLeast(0))
        val trackedBackdropIndex =
            source.indexOf("contentBackdrop = surfaces.contentBackdrop,", startIndex = trackedIndex.coerceAtLeast(0))
        val dockIndex =
            source.indexOf("AppFloatingVerticalSearchActionDock(", startIndex = trackedBackdropIndex.coerceAtLeast(0))
        val dockBackdropIndex =
            source.indexOf("backdrop = surfaces.topBarBackdrop,", startIndex = dockIndex.coerceAtLeast(0))

        assertTrue(sceneIndex >= 0, "GitHub page must host the shared content Backdrop scene")
        assertTrue(sceneBackdropIndex > sceneIndex, "The scene must produce the page content identity")
        assertTrue(scaffoldIndex > sceneBackdropIndex, "The content producer must precede the Scaffold tree")
        assertTrue(overviewBackdropIndex > overviewIndex, "Overview must consume the page content identity")
        assertTrue(trackedBackdropIndex > trackedIndex, "Tracked cards must consume the page content identity")
        assertTrue(dockBackdropIndex > dockIndex, "The floating dock must sample the scrolling-content identity")
        assertTrue(overviewIndex < trackedIndex, "Tracked content must remain after the overview")
        assertTrue(trackedIndex < dockIndex, "The floating dock must remain after scrolling content")
        assertEquals(1, source.occurrencesOf("MainPageContentBackdropScene("))
        assertEquals(0, source.occurrencesOf(".layerBackdrop(surfaces.contentBackdrop)"))
    }

    @Test
    fun topBarProducerKeepsAnIdentitySeparateFromContentConsumers() {
        val source = sourceFile(GITHUB_MAIN_CONTENT_SOURCE)
        val topBarProducerIndex = source.indexOf(".layerBackdrop(surfaces.topBarBackdrop)")
        val contentSceneIndex = source.indexOf("contentBackdrop = surfaces.contentBackdrop,")
        val titleConsumerIndex = source.indexOf("titleBackdrop = surfaces.topBarBackdrop,")

        assertTrue(contentSceneIndex >= 0, "GitHub scene must use the content Backdrop identity")
        assertTrue(titleConsumerIndex > contentSceneIndex, "The title must consume the top-bar identity")
        assertTrue(topBarProducerIndex > titleConsumerIndex, "Scrolling content must produce the top-bar identity")
        assertEquals(1, source.occurrencesOf(".layerBackdrop(surfaces.topBarBackdrop)"))
        assertEquals(1, source.occurrencesOf("titleBackdrop = surfaces.topBarBackdrop,"))
    }

    @Test
    fun sceneMigrationPreservesScrollingAndLazyItemContracts() {
        val source = sourceFile(GITHUB_MAIN_CONTENT_SOURCE)
        val sceneIndex = source.indexOf("MainPageContentBackdropScene(")
        val rootTagIndex =
            source.indexOf(
                ".testTag(KeiOsTestTags.GitHubPageRoot)",
                startIndex = sceneIndex.coerceAtLeast(0),
            )
        val listIndex = source.indexOf("AppPageLazyColumn(", startIndex = rootTagIndex.coerceAtLeast(0))

        assertTrue(rootTagIndex > sceneIndex, "The page root tag must remain on the scene container")
        assertTrue(listIndex > rootTagIndex, "The scrolling list must remain inside the page scene")
        assertTrue(".nestedScroll(layout.scrollBehavior.nestedScrollConnection)" in source)
        assertTrue("state = layout.listState," in source)
        assertTrue("innerPadding = innerPadding," in source)
        assertTrue("bottomExtra = appPageBottomPaddingWithFloatingOverlay(layout.contentBottomPadding)" in source)
        assertTrue("key = \"github_overview_card\"" in source)
        assertTrue("contentType = \"github_overview\"" in source)
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

private const val GITHUB_MAIN_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/section/GitHubMainContentSection.kt"
