package os.kei.ui.page.main.student.catalog.page

import java.io.File
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaGuideCatalogThemeSourceTest {
    @Test
    fun catalogPanelFollowsTheKeiOSAppTheme() {
        val source = sourceFile(BA_GUIDE_CATALOG_PAGE_SOURCE)

        assertFalse("isSystemInDarkTheme" in source)
        assertEquals(1, source.occurrencesOf("isAppInDarkTheme()"))
    }

    @Test
    fun catalogStatusIconPillDelegatesThemeResolutionToTheSharedAtom() {
        val wrapperSource = sourceFile(BA_GUIDE_CATALOG_STATUS_PILL_SOURCE)
        val sharedSource = sourceFile(STATUS_ICON_PILL_SOURCE)

        assertFalse("isSystemInDarkTheme" in wrapperSource)
        assertFalse("isAppInDarkTheme" in wrapperSource)
        assertTrue("StatusIconPill(" in wrapperSource)
        assertFalse("isSystemInDarkTheme" in sharedSource)
        assertEquals(1, sharedSource.occurrencesOf("isAppInDarkTheme()"))
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

private const val BA_GUIDE_CATALOG_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/page/BaGuideCatalogPage.kt"
private const val BA_GUIDE_CATALOG_STATUS_PILL_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/BaGuideCatalogStatusIconPill.kt"
private const val STATUS_ICON_PILL_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/status/StatusIconPill.kt"
