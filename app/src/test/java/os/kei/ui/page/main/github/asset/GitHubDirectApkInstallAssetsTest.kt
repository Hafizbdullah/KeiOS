package os.kei.ui.page.main.github.asset

import kotlin.test.assertEquals
import org.junit.Test

class GitHubDirectApkInstallAssetsTest {
    @Test
    fun `uses release URL when it is valid`() {
        assertEquals(
            "https://cdn.example/app.apk",
            selectDirectApkDownloadUrl(
                releaseUrl = "https://cdn.example/app.apk",
                fetchSource = "https://fallback.example/app.apk",
                repoUrl = "https://origin.example/app.apk",
            ),
        )
    }

    @Test
    fun `recovers stale html source label through tracked URL`() {
        assertEquals(
            "https://telegram.org/dl/android/apk",
            selectDirectApkDownloadUrl(
                releaseUrl = "html",
                fetchSource = "html",
                repoUrl = "https://telegram.org/dl/android/apk",
            ),
        )
    }

    @Test
    fun `uses valid fetch source before tracked URL`() {
        assertEquals(
            "https://telegram.org/file/apk.apk",
            selectDirectApkDownloadUrl(
                releaseUrl = "direct_apk",
                fetchSource = "https://telegram.org/file/apk.apk",
                repoUrl = "https://telegram.org/dl/android/apk",
            ),
        )
    }
}
