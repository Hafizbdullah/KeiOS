package os.kei.ui.page.main.student.catalog.component.bgm

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaGuideBgmAlbumThemeSourceTest {
    @Test
    fun albumActionsFollowTheKeiOSAppTheme() {
        val source = sourceFile(BA_GUIDE_BGM_ALBUM_HERO_VISUALS_SOURCE)

        assertFalse(
            "isSystemInDarkTheme" in source,
            "Album actions must follow the selected KeiOS theme",
        )
        assertEquals(
            1,
            source.occurrencesOf("isAppInDarkTheme()"),
            "The shared album action surface must use the app theme",
        )
        assertTrue(
            "val actionSurfaceColor = Color.White.copy(alpha = if (isAppInDarkTheme()) 0.14f else 0.34f)" in
                source,
            "Album actions must keep the shared light and dark glass film",
        )
        assertEquals(2, source.occurrencesOf("AppLiquidIconButton("))
        assertEquals(1, source.occurrencesOf("AppLiquidTextButton("))
        assertEquals(3, source.occurrencesOf("containerColor = actionSurfaceColor"))
        assertEquals(3, source.occurrencesOf("containerAlphaOverride = actionSurfaceColor.alpha"))
    }

    @Test
    fun trackListSurfacesFollowTheKeiOSAppTheme() {
        val source = sourceFile(BA_GUIDE_BGM_TRACK_LIST_SOURCE)

        assertFalse(
            "isSystemInDarkTheme" in source,
            "Track list materials must follow the selected KeiOS theme",
        )
        assertEquals(
            2,
            source.occurrencesOf("isAppInDarkTheme()"),
            "Both populated and empty track surfaces must use the app theme",
        )
    }

    @Test
    fun bottomChromeSearchFollowsTheKeiOSAppTheme() {
        val source = sourceFile(BA_GUIDE_BGM_BOTTOM_CHROME_SOURCE)

        assertFalse(
            "isSystemInDarkTheme" in source,
            "Bottom chrome search material must follow the selected KeiOS theme",
        )
        assertEquals(1, source.occurrencesOf("isAppInDarkTheme()"))
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
private const val BA_GUIDE_BGM_TRACK_LIST_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/bgm/BaGuideBgmTrackList.kt"
private const val BA_GUIDE_BGM_BOTTOM_CHROME_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/bgm/BaGuideBgmBottomChrome.kt"
