package os.kei.feature.github.engine.release

import org.junit.Test
import os.kei.feature.github.data.remote.GitHubVersionUtils
import os.kei.feature.github.model.GitHubAtomFeed
import os.kei.feature.github.model.GitHubAtomReleaseEntry
import os.kei.feature.github.model.GitHubReleaseChannel
import os.kei.feature.github.model.GitHubReleaseSignalSource
import os.kei.feature.github.model.GitHubReleaseVersionSignals
import os.kei.feature.github.model.GitHubRemoteApkVersionInfo
import os.kei.feature.github.model.GitHubRepositoryReleaseSnapshot
import os.kei.feature.github.model.GitHubTrackedIgnoreMode
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import os.kei.feature.github.model.GitHubVersionCandidateSource
import os.kei.feature.github.model.buildGitHubReleaseIgnoreKey
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubReleaseEvaluationEngineTest {
    @Test
    fun `newer stable release is an update`() {
        val result = GitHubReleaseEvaluationEngine.evaluate(
            localVersion = "1.0.0",
            localVersionCode = 100L,
            snapshot = snapshot(
                stable = signal("v1.1.0"),
                entries = listOf(entry("v1.1.0"), entry("v1.0.0")),
            ),
        )

        assertEquals(GitHubTrackedReleaseStatus.UpdateAvailable, result.status)
        assertTrue(result.hasUpdate)
        assertFalse(result.hasPreReleaseUpdate)
    }

    @Test
    fun `preferred newer prerelease is recommended`() {
        val result = GitHubReleaseEvaluationEngine.evaluate(
            localVersion = "1.0.0",
            localVersionCode = 100L,
            snapshot = snapshot(
                stable = signal("v1.0.0"),
                preRelease = signal("v1.1.0-beta1"),
                entries = listOf(entry("v1.1.0-beta1"), entry("v1.0.0")),
            ),
            policy = GitHubReleaseEvaluationPolicy(preferPreRelease = true),
        )

        assertEquals(GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable, result.status)
        assertTrue(result.hasUpdate)
        assertTrue(result.hasPreReleaseUpdate)
        assertTrue(result.recommendsPreRelease)
    }

    @Test
    fun `newer prerelease stays optional when stable is current`() {
        val result = GitHubReleaseEvaluationEngine.evaluate(
            localVersion = "1.0.0",
            localVersionCode = 100L,
            snapshot = snapshot(
                stable = signal("v1.0.0"),
                preRelease = signal("v1.1.0-beta1"),
                entries = listOf(entry("v1.1.0-beta1"), entry("v1.0.0")),
            ),
            policy = GitHubReleaseEvaluationPolicy(checkAllTrackedPreReleases = true),
        )

        assertEquals(GitHubTrackedReleaseStatus.PreReleaseOptional, result.status)
        assertFalse(result.hasUpdate)
        assertTrue(result.hasPreReleaseUpdate)
        assertFalse(result.recommendsPreRelease)
    }

    @Test
    fun `precise apk version code overrides ambiguous release tag`() {
        val result = GitHubReleaseEvaluationEngine.evaluate(
            localVersion = "Version.26.4.Canary_C378",
            localVersionCode = 378L,
            snapshot = snapshot(
                stable = signal("Version.1.3.Fix2_C359"),
                entries = listOf(entry("Version.1.3.Fix2_C359")),
            ),
            preciseStableApkVersion = GitHubRemoteApkVersionInfo(
                packageName = "demo.app",
                versionName = "Version.26.4.Alpha2_C384",
                versionCode = "384",
            ),
        )

        assertEquals(GitHubTrackedReleaseStatus.UpdateAvailable, result.status)
        assertTrue(result.hasUpdate)
    }

    @Test
    fun `current stable ignore key suppresses matching update`() {
        val ignoredTag = "v1.1.0"
        val result = GitHubReleaseEvaluationEngine.evaluate(
            localVersion = "1.0.0",
            localVersionCode = 100L,
            snapshot = snapshot(
                stable = signal(ignoredTag),
                entries = listOf(entry(ignoredTag), entry("v1.0.0")),
            ),
            policy = GitHubReleaseEvaluationPolicy(
                ignoreMode = GitHubTrackedIgnoreMode.CurrentStable,
                ignoredStableReleaseKey = buildGitHubReleaseIgnoreKey(rawTag = ignoredTag),
            ),
        )

        assertEquals(GitHubTrackedReleaseStatus.Ignored, result.status)
        assertFalse(result.hasUpdate)
    }

    @Test
    fun `prerelease only repository exposes a hint when prerelease checks are disabled`() {
        val preRelease = signal("v0.1.0-beta1")
        val result = GitHubReleaseEvaluationEngine.evaluate(
            localVersion = "unknown",
            localVersionCode = -1L,
            snapshot = snapshot(
                stable = preRelease,
                preRelease = preRelease,
                entries = listOf(entry("v0.1.0-beta1")),
                hasStableRelease = false,
            ),
        )

        assertFalse(result.hasStableRelease)
        assertEquals(
            GitHubTrackedReleaseStatus.ONLY_PRERELEASES_HINT_MESSAGE,
            result.releaseHint,
        )
        assertFalse(result.showPreReleaseInfo)
    }

    private fun snapshot(
        stable: GitHubReleaseVersionSignals,
        preRelease: GitHubReleaseVersionSignals? = null,
        entries: List<GitHubAtomReleaseEntry>,
        hasStableRelease: Boolean = true,
    ): GitHubRepositoryReleaseSnapshot {
        return GitHubRepositoryReleaseSnapshot(
            strategyId = "github_api_token",
            feed = GitHubAtomFeed(
                title = "demo/app releases",
                feedUrl = "https://github.com/demo/app/releases",
                entries = entries,
            ),
            latestStable = stable,
            hasStableRelease = hasStableRelease,
            latestPreRelease = preRelease,
        )
    }

    private fun signal(tag: String): GitHubReleaseVersionSignals {
        return GitHubReleaseVersionSignals(
            displayVersion = tag,
            rawTag = tag,
            rawName = tag,
            link = GitHubVersionUtils.buildReleaseTagUrl("demo", "app", tag),
            versionCandidates = GitHubVersionUtils.buildVersionCandidates(
                GitHubVersionCandidateSource.Tag to tag,
                GitHubVersionCandidateSource.Title to tag,
            ),
            source = GitHubReleaseSignalSource.GitHubApi,
            channel = GitHubVersionUtils.classifyVersionChannel(tag)
                ?: GitHubReleaseChannel.UNKNOWN,
        )
    }

    private fun entry(tag: String): GitHubAtomReleaseEntry {
        val channel = GitHubVersionUtils.classifyVersionChannel(tag)
            ?: GitHubReleaseChannel.UNKNOWN
        return GitHubAtomReleaseEntry(
            tag = tag,
            title = tag,
            link = GitHubVersionUtils.buildReleaseTagUrl("demo", "app", tag),
            versionCandidates = GitHubVersionUtils.buildVersionCandidates(
                GitHubVersionCandidateSource.Tag to tag,
                GitHubVersionCandidateSource.Title to tag,
            ),
            channel = channel,
            isLikelyPreRelease = channel.isPreRelease,
        )
    }
}
