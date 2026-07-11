package os.kei.core.versioning

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VersioningEngineTest {
    @Test
    fun `stable release outranks same base release candidate`() {
        assertOrder(
            expected = VersionOrder.Older,
            localVersion = "3.8.0-rc04",
            remoteVersion = "3.8.0",
        )
    }

    @Test
    fun `newer alpha outranks older stable release`() {
        assertOrder(
            expected = VersionOrder.Newer,
            localVersion = "11.2.0-alpha01",
            remoteVersion = "11.1.0",
        )
    }

    @Test
    fun `date prefix exposes semantic release version`() {
        val comparison = VersioningEngine.compareLocalVersionToRemote(
            localVersion = "1.22",
            remoteCandidates = candidates(0 to "260412_1.22"),
        )

        assertEquals(VersionOrder.Same, comparison?.order)
    }

    @Test
    fun `title semantic version outranks raw date tag during release ranking`() {
        val left = candidates(
            0 to "r20260410",
            1 to "Release v2.8.0-20260410",
        )
        val right = candidates(0 to "v2.7.0")

        val comparison = VersioningEngine.compareRemoteCandidateSets(left, right)

        assertEquals(VersionOrder.Newer, comparison?.order)
        assertTrue(comparison?.leftEvidence.orEmpty().contains("2.8.0"))
    }

    @Test
    fun `hyphenated release date does not become a semantic version component`() {
        assertOrder(
            expected = VersionOrder.Same,
            localVersion = "2.7.0",
            remoteVersion = "Release v2.7.0-20260320",
        )
    }

    @Test
    fun `release ranking comparison is antisymmetric`() {
        val older = candidates(
            0 to "v1.9.0",
            4 to "changelog from v1.8.0 to v1.9.0",
        )
        val newer = candidates(
            0 to "v2.0.0",
            4 to "mentions v99.0.0 in an example",
        )

        val forward = VersioningEngine.compareRemoteCandidateSets(older, newer)
        val reverse = VersioningEngine.compareRemoteCandidateSets(newer, older)

        assertEquals(VersionOrder.Older, forward?.order)
        assertEquals(VersionOrder.Newer, reverse?.order)
        assertEquals(
            forward?.order?.legacyValue,
            reverse?.order?.legacyValue?.let { -it },
        )
    }

    @Test
    fun `release ranking remains transitive`() {
        val first = candidates(0 to "v1.0.0")
        val second = candidates(0 to "v1.5.0")
        val third = candidates(0 to "v2.0.0")

        assertEquals(
            VersionOrder.Older,
            VersioningEngine.compareRemoteCandidateSets(first, second)?.order,
        )
        assertEquals(
            VersionOrder.Older,
            VersioningEngine.compareRemoteCandidateSets(second, third)?.order,
        )
        assertEquals(
            VersionOrder.Older,
            VersioningEngine.compareRemoteCandidateSets(first, third)?.order,
        )
    }

    @Test
    fun `long numeric component remains comparable`() {
        assertOrder(
            expected = VersionOrder.Older,
            localVersion = "1.0.2147483648",
            remoteVersion = "1.0.2147483649",
        )
    }

    @Test
    fun `hyphen separated numeric components remain comparable`() {
        assertOrder(
            expected = VersionOrder.Older,
            localVersion = "1-2-3",
            remoteVersion = "1-2-4",
        )
    }

    @Test
    fun `recognized revision tokens break same version ties`() {
        assertOrder(
            expected = VersionOrder.Older,
            localVersion = "Version.1.3.Fix2_C359",
            remoteVersion = "Version.1.3.Fix3_C360",
        )
    }

    @Test
    fun `release ranker uses publication time after semantic equality`() {
        val older = ReleaseRankingEvidence(
            versionCandidates = candidates(0 to "v1.0.0"),
            publishedAtMillis = 100L,
            stableKey = "older",
        )
        val newer = ReleaseRankingEvidence(
            versionCandidates = candidates(0 to "1.0.0"),
            publishedAtMillis = 200L,
            stableKey = "newer",
        )

        assertTrue(ReleaseCandidateRanker.compare(older, newer) < 0)
    }

    @Test
    fun `local version and build code can match remote composite tag`() {
        assertTrue(
            VersioningEngine.remoteCandidateMatchesLocalVersionNameAndCode(
                localVersion = "1.2.18",
                localVersionCode = 2102L,
                remoteCandidates = candidates(0 to "v1.2.18.2102"),
            ),
        )
    }

    private fun assertOrder(
        expected: VersionOrder,
        localVersion: String,
        remoteVersion: String,
    ) {
        val comparison = VersioningEngine.compareLocalVersionToRemote(
            localVersion = localVersion,
            remoteCandidates = candidates(0 to remoteVersion),
        )
        assertNotNull(comparison)
        assertEquals(expected, comparison.order)
    }

    private fun candidates(vararg values: Pair<Int, String>): List<VersionCandidate> {
        return VersioningEngine.buildCandidates(values.asIterable())
    }
}
