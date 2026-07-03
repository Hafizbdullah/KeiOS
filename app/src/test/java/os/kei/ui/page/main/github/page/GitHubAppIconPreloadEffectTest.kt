package os.kei.ui.page.main.github.page

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubAppIconPreloadEffectTest {
    @Test
    fun `picker expanded keeps all tracked icons and limits picker lane`() {
        val packages =
            buildGitHubAppIconPreloadPackages(
                trackedPackages = (0 until 40).map { "tracked.$it" },
                installedPackages = (0 until 40).map { "installed.$it" },
                selectedPackageName = "selected.app",
                pickerExpanded = true,
                appPickerFilteredPackages = (0 until 80).map { "picker.$it" }
            )

        assertEquals(
            (0 until 40).map { "tracked.$it" } +
                "selected.app" +
                (0 until 32).map { "picker.$it" },
            packages
        )
        assertTrue("tracked.39" in packages)
        assertTrue("installed.0" !in packages)
    }

    @Test
    fun `picker collapsed keeps installed preload lane`() {
        val packages =
            buildGitHubAppIconPreloadPackages(
                trackedPackages = listOf("tracked.app"),
                installedPackages = listOf("installed.one", "installed.two"),
                selectedPackageName = "",
                pickerExpanded = false,
                appPickerFilteredPackages = listOf("picker.one")
            )

        assertEquals(
            listOf("tracked.app", "installed.one", "installed.two"),
            packages
        )
    }
}
