package os.kei.feature.github.data.remote

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubReleaseAssetModelsTest {
    @Test
    fun `nested release zip requires matching inspected package`() {
        val asset = asset("HMA-OSS-ZYGISK-oss-164-release.zip")

        assertTrue(
            asset.isVerifiedManagedInstallAsset(
                expectedPackageName = "org.frknkrc44.hma_oss",
                inspectedPackageName = "org.frknkrc44.hma_oss",
            ),
        )
        assertFalse(
            asset.isVerifiedManagedInstallAsset(
                expectedPackageName = "org.frknkrc44.hma_oss",
                inspectedPackageName = "other.package",
            ),
        )
        assertFalse(
            asset.isVerifiedManagedInstallAsset(
                expectedPackageName = "",
                inspectedPackageName = "org.frknkrc44.hma_oss",
            ),
        )
    }

    @Test
    fun `direct apk remains an install candidate before manifest inspection`() {
        assertTrue(
            asset("app-release.apk").isVerifiedManagedInstallAsset(
                expectedPackageName = "demo.app",
                inspectedPackageName = "",
            ),
        )
    }

    private fun asset(name: String): GitHubReleaseAssetFile =
        GitHubReleaseAssetFile(
            name = name,
            downloadUrl = "https://example.test/$name",
            sizeBytes = 1L,
            downloadCount = 0,
        )
}
