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
        val keepAliveIndex =
            source.indexOf("AppEdgeStackKeepAlive(", startIndex = providerIndex.coerceAtLeast(0))
        val listIndex = source.indexOf("AppPageLazyColumn(", startIndex = keepAliveIndex.coerceAtLeast(0))

        assertTrue(serverPanelIndex >= 0, "The shared layout must render the server panel")
        assertTrue(providerIndex > serverPanelIndex, "The server panel must stay outside the stack provider")
        assertTrue(keepAliveIndex > providerIndex, "The keep-alive box must consume the stack provider")
        assertTrue(listIndex > keepAliveIndex, "The data list must sit inside the keep-alive box")

        // The keep-alive box anchors the geometry now, not the list. That is what keeps the stack line
        // measured from the *visible* top edge — the list inside it is shifted up by the headroom, so
        // anchoring on the list would move the line up with it.
        assertTrue(
            ".appEdgeStackContainer(" !in source,
            "The list must not also anchor the stack; AppEdgeStackKeepAlive owns it",
        )
        // And the headroom has to be absorbed by the content inset, or the first card starts off screen.
        assertTrue(
            "topExtra = appEdgeStackKeepAliveTopPadding(AppEdgeStackListTopInset)" in source,
            "The list's top inset must include the keep-alive headroom",
        )
    }

    @Test
    fun bothCalendarPoolRoutesUseTheSharedStackedLayout() {
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
    "app/src/main/java/os/kei/ui/page/main/ba/BaActivityCalendarPage.kt"
private const val BA_POOL_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaPoolPage.kt"
