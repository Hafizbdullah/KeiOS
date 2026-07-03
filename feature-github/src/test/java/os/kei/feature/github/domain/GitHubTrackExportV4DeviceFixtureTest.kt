package os.kei.feature.github.domain

import org.junit.Test
import os.kei.feature.github.data.local.GitHubTrackStore
import os.kei.feature.github.model.GitHubTrackedSourceMode
import kotlin.test.assertEquals

class GitHubTrackExportV4DeviceFixtureTest {
    @Test
    fun `real device v4 export imports every tracked source`() {
        val payload =
            GitHubTrackStore.parseTrackedItemsImport(GitHubTrackExportV4DeviceFixture.rawJson)
        val sourceCounts = GitHubTrackStore.calculateTrackedItemsSourceCounts(payload.items)
        val optionCounts = GitHubTrackStore.calculateTrackedItemsOptionCounts(payload.items)

        assertEquals("keios.github.tracked/v4", payload.format)
        assertEquals(4, payload.schemaVersion)
        assertEquals(1_783_081_672_942L, payload.exportedAtMillis)
        assertEquals(GitHubTrackExportV4DeviceFixture.expectedItemCount, payload.sourceCount)
        assertEquals(GitHubTrackExportV4DeviceFixture.expectedItemCount, payload.items.size)
        assertEquals(0, payload.invalidCount)
        assertEquals(0, payload.duplicateCount)
        assertEquals(
            GitHubTrackExportV4DeviceFixture.expectedGitHubRepositoryCount,
            sourceCounts.githubRepositoryCount,
        )
        assertEquals(
            GitHubTrackExportV4DeviceFixture.expectedGitRepositoryCount,
            sourceCounts.gitRepositoryCount,
        )
        assertEquals(
            GitHubTrackExportV4DeviceFixture.expectedDirectApkCount,
            sourceCounts.directApkCount,
        )
        assertEquals(
            GitHubTrackExportV4DeviceFixture.expectedFdroidRepositoryCount,
            sourceCounts.fdroidRepositoryCount,
        )
        assertEquals(6, optionCounts.preferPreReleaseCount)
        assertEquals(5, optionCounts.actionsUpdateCount)
    }

    @Test
    fun `real device v4 export keeps git repository identities`() {
        val pronto = GitHubTrackExportV4DeviceFixture.itemByPackage("com.mt.pronto")
        val aShell = GitHubTrackExportV4DeviceFixture.itemByPackage("in.sunilpaulmathew.ashell")

        assertEquals(GitHubTrackedSourceMode.GitRepository, pronto.sourceMode)
        assertEquals("https://gitee.com/hugedog233/Pronto", pronto.repoUrl)
        assertEquals("gitee.com/hugedog233", pronto.owner)
        assertEquals("Pronto", pronto.repo)
        assertEquals("大狗记", pronto.appLabel)

        assertEquals(GitHubTrackedSourceMode.GitRepository, aShell.sourceMode)
        assertEquals("https://gitlab.com/sunilpaulmathew/ashell", aShell.repoUrl)
        assertEquals("gitlab.com/sunilpaulmathew", aShell.owner)
        assertEquals("ashell", aShell.repo)
        assertEquals("aShell", aShell.appLabel)
    }

    @Test
    fun `real device v4 export keeps direct apk identities`() {
        val telegramBeta =
            GitHubTrackExportV4DeviceFixture.itemByPackage("org.telegram.messenger.beta")
        val telegram = GitHubTrackExportV4DeviceFixture.itemByPackage("org.telegram.messenger.web")
        val scene = GitHubTrackExportV4DeviceFixture.itemByPackage("com.omarea.vtools")

        assertEquals(GitHubTrackedSourceMode.DirectApk, telegramBeta.sourceMode)
        assertEquals("https://telegram.org/dl/android/apk-public-beta", telegramBeta.repoUrl)
        assertEquals("telegram.org", telegramBeta.owner)
        assertEquals("dl-android-apk-public-beta", telegramBeta.repo)
        assertEquals("Telegram Beta", telegramBeta.appLabel)

        assertEquals(GitHubTrackedSourceMode.DirectApk, telegram.sourceMode)
        assertEquals("https://telegram.org/dl/android/apk", telegram.repoUrl)
        assertEquals("telegram.org", telegram.owner)
        assertEquals("dl-android-apk", telegram.repo)
        assertEquals("Telegram", telegram.appLabel)

        assertEquals(GitHubTrackedSourceMode.DirectApk, scene.sourceMode)
        assertEquals("https://download.omarea.com/scene9/scene_9.2.11.apk", scene.repoUrl)
        assertEquals("download.omarea.com", scene.owner)
        assertEquals("scene9-scene_9.2.11.apk", scene.repo)
        assertEquals("Scene", scene.appLabel)
    }
}
