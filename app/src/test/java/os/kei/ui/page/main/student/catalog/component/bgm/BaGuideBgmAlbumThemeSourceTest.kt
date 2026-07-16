package os.kei.ui.page.main.student.catalog.component.bgm

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BaGuideBgmAlbumThemeSourceTest {
    @Test
    fun albumActionsFollowTheKeiOSAppTheme() {
        val source = sourceFile(BA_GUIDE_BGM_ALBUM_HERO_VISUALS_SOURCE)

        assertFalse(
            "isSystemInDarkTheme" in source,
            "Album actions must follow the selected KeiOS theme",
        )
        assertEquals(
            2,
            source.occurrencesOf("isAppInDarkTheme()"),
            "Both round and primary album actions must use the app theme",
        )
    }
}

private fun String.occurrencesOf(needle: String): Int =
    windowed(needle.length).count { it == needle }

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

private const val BA_GUIDE_BGM_ALBUM_HERO_VISUALS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/bgm/BaGuideBgmAlbumHeroVisuals.kt"
