package os.kei.ui.page.main.student.catalog.component

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaGuideCatalogStatusIconPillReuseTest {
    @Test
    fun baWrapperLocksLegacyGeometryAroundTheSharedAtom() {
        val source = sourceFile(BA_STATUS_ICON_PILL_SOURCE)

        assertEquals(1, source.occurrencesOf("\n    StatusIconPill("))
        assertTrue("backdrop: Backdrop? = null" in source)
        assertTrue("backdrop = backdrop" in source)
        assertTrue("width = 28.dp" in source)
        assertTrue("height = 22.dp" in source)
        assertTrue("iconSize = 13.dp" in source)
        assertFalse("Box(" in source)
        assertFalse("isAppInDarkTheme" in source)
        assertFalse("appSquircleBackground" in source)
        assertFalse("appSquircleBorder" in source)
        assertFalse("rememberLayerBackdrop" in source)
        assertFalse(".layerBackdrop(" in source)
    }

    @Test
    fun fiveProductionStatusesContinueThroughTheThinBaWrapper() {
        val expectations =
            listOf(
                SourceExpectation(BA_MEMORY_LOBBY_CARDS_SOURCE, expectedCalls = 2),
                SourceExpectation(BA_STUDENT_BGM_CARDS_SOURCE, expectedCalls = 1),
                SourceExpectation(BA_BGM_FAVORITE_CARDS_SOURCE, expectedCalls = 2),
            )

        val callCount =
            expectations.sumOf { expectation ->
                val source = sourceFile(expectation.path)
                val calls = source.occurrencesOf("BaGuideCatalogStatusIconPill(")
                assertEquals(expectation.expectedCalls, calls, expectation.path)
                assertFalse("\n    StatusIconPill(" in source, expectation.path)
                calls
            }

        assertEquals(5, callCount)
    }

    @Test
    fun componentLabShowsTheSharedAtomAsACompactIterationStatus() {
        val source = sourceFile(DEBUG_COMPONENT_LAB_PAGE_SOURCE)
        val iterationCard = source.substringAfter("private fun DebugIterationQueueCard(")

        assertEquals(1, iterationCard.occurrencesOf("StatusIconPill("))
        assertTrue("label = stringResource(R.string.debug_component_lab_queue_badge)" in iterationCard)
        assertTrue("icon = appLucideLayersIcon()" in iterationCard)
        assertFalse("rememberLayerBackdrop" in iterationCard)
        assertFalse(".layerBackdrop(" in iterationCard)
    }

    @Test
    fun sharedAtomConsumesParentMaterialAndStaysPassive() {
        val source = sourceFile(STATUS_ICON_PILL_SOURCE)

        assertTrue("backdrop: Backdrop? = null" in source)
        assertTrue("val parentBackdrop = LocalLiquidParentBackdrop.current" in source)
        assertTrue("activeGlassBackdrop(backdrop ?: parentBackdrop)" in source)
        assertTrue("statusPillFallbackOptics(" in source)
        assertTrue(".statusPillMaterial(" in source)
        assertTrue("isInteractive = false" in source)
        assertTrue("contentDescription = label" in source)
        assertFalse("rememberLayerBackdrop" in source)
        assertFalse(".layerBackdrop(" in source)
        assertFalse("onClick:" in source)
        assertFalse("enabled:" in source)
    }
}

private data class SourceExpectation(
    val path: String,
    val expectedCalls: Int,
)

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

private const val BA_STATUS_ICON_PILL_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/BaGuideCatalogStatusIconPill.kt"
private const val BA_MEMORY_LOBBY_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/BaGuideMemoryLobbyCards.kt"
private const val BA_STUDENT_BGM_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/BaGuideStudentBgmCards.kt"
private const val BA_BGM_FAVORITE_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/BaGuideBgmFavoriteCards.kt"
private const val DEBUG_COMPONENT_LAB_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugComponentLabPage.kt"
private const val STATUS_ICON_PILL_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/status/StatusIconPill.kt"
