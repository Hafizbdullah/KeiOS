package os.kei.ui.page.main.student.page.component

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaStudentGuidePagerPageBackdropTest {
    @Test
    fun pageBackdropProducerPrecedesTheProvidedCardMaterial() {
        val source = sourceFile(BA_STUDENT_GUIDE_PAGER_PAGE_SOURCE)
        val pageBackdropIndex = source.indexOf("val pageBackdrop: LayerBackdrop =")
        val stableSurfaceIndex = source.indexOf("drawRect(surfaceColor)", pageBackdropIndex)
        val siblingProducerIndex = source.indexOf(".layerBackdrop(pageBackdrop)", stableSurfaceIndex)
        val providerIndex =
            source.indexOf(
                "CompositionLocalProvider(LocalLiquidParentBackdrop provides pageBackdrop)",
                siblingProducerIndex,
            )
        val listIndex = source.indexOf("LazyColumn(", providerIndex)
        val loadingOverlayIndex = source.indexOf("BaStudentGuidePagerLoadingOverlay(", listIndex)

        assertTrue(pageBackdropIndex >= 0, "The page must retain its dedicated Backdrop identity")
        assertTrue(stableSurfaceIndex > pageBackdropIndex, "The page Backdrop must pre-paint its surface color")
        assertTrue(siblingProducerIndex > stableSurfaceIndex, "The sibling producer must follow Backdrop creation")
        assertTrue(providerIndex > siblingProducerIndex, "Cards must consume the already-produced page material")
        assertTrue(listIndex > providerIndex, "The provider must wrap the entire scrolling content")
        assertTrue(loadingOverlayIndex > listIndex, "The loading overlay must remain outside the scrolling list")
        assertEquals(1, source.occurrencesOf(".layerBackdrop(pageBackdrop)"))
        assertEquals(
            1,
            source.occurrencesOf("LocalLiquidParentBackdrop provides pageBackdrop"),
        )
    }

    @Test
    fun materialProviderPreservesTheListLayoutAndScrollContracts() {
        val source = sourceFile(BA_STUDENT_GUIDE_PAGER_PAGE_SOURCE)
        val providerIndex =
            source.indexOf("CompositionLocalProvider(LocalLiquidParentBackdrop provides pageBackdrop)")
        val loadingOverlayIndex = source.indexOf("BaStudentGuidePagerLoadingOverlay(", providerIndex)
        val providedList = source.substring(providerIndex, loadingOverlayIndex)

        assertTrue("state = pageListState," in providedList)
        assertTrue(".nestedScroll(pageNestedScrollConnection)" in providedList)
        assertTrue(
            "top = innerPadding.calculateTopPadding() + AppChromeTokens.topBarToHeaderGap" in providedList,
        )
        assertTrue("bottom = innerPadding.calculateBottomPadding() + 16.dp" in providedList)
        assertTrue("start = 16.dp" in providedList)
        assertTrue("end = 16.dp" in providedList)
        assertTrue("key = \"ba-student-guide-header-\${tabRenderState.activeBottomTab.name}\"" in providedList)
        assertTrue("renderBaStudentGuideTabContent(" in providedList)
        assertEquals(2, providedList.occurrencesOf("backdrop = pageBackdrop,"))
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

private const val BA_STUDENT_GUIDE_PAGER_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/page/component/BaStudentGuidePagerPage.kt"
