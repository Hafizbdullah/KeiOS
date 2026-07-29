package os.kei.ui.page.main.ba

import java.io.File
import kotlin.test.assertTrue
import org.junit.Test

class BaCalendarPoolStackedLayoutSourceTest {
    @Test
    fun serverPanelStaysPinnedAboveTheStackedDataList() {
        val source = sourceFile(BA_CALENDAR_POOL_STACKED_LAYOUT_SOURCE)
        val serverPanelIndex = source.indexOf("BaCalendarPoolServerPanel(")
        val providerIndex =
            source.indexOf(
                "CompositionLocalProvider(LocalAppEdgeStackCards provides edgeStackState)",
            )
        val listIndex = source.indexOf("AppPageLazyColumn(", startIndex = providerIndex.coerceAtLeast(0))
        val stackContainerIndex =
            source.indexOf(".appEdgeStackContainer(edgeStackState)", startIndex = listIndex.coerceAtLeast(0))

        assertTrue(serverPanelIndex >= 0, "The shared layout must render the server panel")
        assertTrue(providerIndex > serverPanelIndex, "The server panel must stay outside the stack provider")
        assertTrue(listIndex > providerIndex, "The data list must consume the stack provider")
        assertTrue(stackContainerIndex > listIndex, "The data list must anchor the edge-stack geometry")
        assertTrue("topExtra = AppEdgeStackListTopInset" in source)
    }

    @Test
    fun bothStandalonePagesUseTheSharedStackedLayout() {
        listOf(
            sourceFile(BA_ACTIVITY_CALENDAR_SOURCE),
            sourceFile(BA_POOL_SOURCE),
        ).forEach { source ->
            assertTrue("BaCalendarPoolStackedLayout(" in source)
            assertTrue("ba-calendar-server-panel" !in source)
            assertTrue("ba-pool-server-panel" !in source)
        }
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

private const val BA_CALENDAR_POOL_STACKED_LAYOUT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaCalendarPoolStackedLayout.kt"
private const val BA_ACTIVITY_CALENDAR_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaActivityCalendarActivity.kt"
private const val BA_POOL_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaPoolActivity.kt"
