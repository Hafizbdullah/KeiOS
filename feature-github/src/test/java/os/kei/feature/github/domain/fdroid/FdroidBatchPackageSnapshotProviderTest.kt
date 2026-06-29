package os.kei.feature.github.domain.fdroid

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidRepositorySnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidVersionSnapshot
import os.kei.feature.github.model.FdroidIndexFormat
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedSourceMode
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class FdroidBatchPackageSnapshotProviderTest {
    @Test
    fun `loadPackageSnapshot fans out same repo packages from one repository snapshot`() = runBlocking {
        val repositoryLoads = AtomicInteger(0)
        val provider = FdroidBatchPackageSnapshotProvider(
            trackedItems = listOf(
                fdroidItem("demo.one"),
                fdroidItem("demo.two")
            ),
            packageProvider = FdroidPackageSnapshotProvider { _, _ ->
                Result.failure(IllegalStateException("package api should not be used"))
            },
            repositoryProvider = FdroidRepositorySnapshotProvider { repoUrl, _ ->
                repositoryLoads.incrementAndGet()
                Result.success(
                    repositorySnapshot(
                        repoUrl = repoUrl,
                        packages = listOf("demo.one", "demo.two")
                    )
                )
            },
            minRepositoryBatchSize = 2
        )

        val one = provider.loadPackageSnapshot(fdroidItem("demo.one"), forceRefresh = true)
        val two = provider.loadPackageSnapshot(fdroidItem("demo.two"), forceRefresh = true)

        assertEquals(1, repositoryLoads.get())
        assertEquals("demo.one", one.getOrThrow().packageName)
        assertEquals("demo.two", two.getOrThrow().packageName)
    }

    @Test
    fun `loadPackageSnapshot uses package provider for small repo groups`() = runBlocking {
        val packageLoads = AtomicInteger(0)
        val provider = FdroidBatchPackageSnapshotProvider(
            trackedItems = listOf(fdroidItem("demo.one")),
            packageProvider = FdroidPackageSnapshotProvider { item, _ ->
                packageLoads.incrementAndGet()
                Result.success(packageSnapshot(item.packageName))
            },
            repositoryProvider = FdroidRepositorySnapshotProvider { _, _ ->
                Result.failure(IllegalStateException("repository index should not be used"))
            },
            minRepositoryBatchSize = 2
        )

        val result = provider.loadPackageSnapshot(fdroidItem("demo.one"), forceRefresh = true)

        assertEquals(1, packageLoads.get())
        assertEquals("demo.one", result.getOrThrow().packageName)
    }

    @Test
    fun `loadPackageSnapshot falls back to repository index when package api fails`() = runBlocking {
        val packageLoads = AtomicInteger(0)
        val repositoryLoads = AtomicInteger(0)
        val provider = FdroidBatchPackageSnapshotProvider(
            trackedItems = listOf(fdroidItem("demo.one")),
            packageProvider = FdroidPackageSnapshotProvider { _, _ ->
                packageLoads.incrementAndGet()
                Result.failure(IllegalStateException("package api unavailable"))
            },
            repositoryProvider = FdroidRepositorySnapshotProvider { repoUrl, _ ->
                repositoryLoads.incrementAndGet()
                Result.success(
                    repositorySnapshot(
                        repoUrl = repoUrl,
                        packages = listOf("demo.one")
                    )
                )
            },
            minRepositoryBatchSize = 2
        )

        val result = provider.loadPackageSnapshot(fdroidItem("demo.one"), forceRefresh = true)

        assertEquals(1, packageLoads.get())
        assertEquals(1, repositoryLoads.get())
        assertEquals("demo.one", result.getOrThrow().packageName)
    }

    @Test
    fun `loadPackageSnapshot shares concurrent package api requests for the same package`() = runBlocking {
        val packageLoads = AtomicInteger(0)
        val provider = FdroidBatchPackageSnapshotProvider(
            trackedItems = listOf(fdroidItem("demo.one")),
            packageProvider = FdroidPackageSnapshotProvider { item, _ ->
                packageLoads.incrementAndGet()
                delay(30)
                Result.success(packageSnapshot(item.packageName))
            },
            repositoryProvider = FdroidRepositorySnapshotProvider { _, _ ->
                Result.failure(IllegalStateException("repository index should not be used"))
            },
            minRepositoryBatchSize = 2
        )

        val results = listOf(
            async { provider.loadPackageSnapshot(fdroidItem("demo.one"), forceRefresh = true) },
            async { provider.loadPackageSnapshot(fdroidItem("demo.one"), forceRefresh = true) },
            async { provider.loadPackageSnapshot(fdroidItem("demo.one"), forceRefresh = true) }
        ).awaitAll()

        assertEquals(1, packageLoads.get())
        assertEquals(
            listOf("demo.one", "demo.one", "demo.one"),
            results.map { result -> result.getOrThrow().packageName }
        )
    }

    @Test
    fun `loadPackageSnapshot shares concurrent repository index requests for same repo`() = runBlocking {
        val repositoryLoads = AtomicInteger(0)
        val provider = FdroidBatchPackageSnapshotProvider(
            trackedItems = listOf(
                fdroidItem("demo.one"),
                fdroidItem("demo.two")
            ),
            packageProvider = FdroidPackageSnapshotProvider { _, _ ->
                Result.failure(IllegalStateException("package api should not be used"))
            },
            repositoryProvider = FdroidRepositorySnapshotProvider { repoUrl, _ ->
                repositoryLoads.incrementAndGet()
                delay(30)
                Result.success(
                    repositorySnapshot(
                        repoUrl = repoUrl,
                        packages = listOf("demo.one", "demo.two")
                    )
                )
            },
            minRepositoryBatchSize = 2
        )

        val results = listOf(
            async { provider.loadPackageSnapshot(fdroidItem("demo.one"), forceRefresh = true) },
            async { provider.loadPackageSnapshot(fdroidItem("demo.two"), forceRefresh = true) }
        ).awaitAll()

        assertEquals(1, repositoryLoads.get())
        assertEquals(
            listOf("demo.one", "demo.two"),
            results.map { result -> result.getOrThrow().packageName }
        )
    }

    private fun fdroidItem(packageName: String): GitHubTrackedApp {
        return GitHubTrackedApp(
            repoUrl = "https://f-droid.org/repo",
            owner = "f-droid.org",
            repo = "repo",
            packageName = packageName,
            appLabel = packageName,
            sourceMode = GitHubTrackedSourceMode.FdroidRepository
        )
    }

    private fun repositorySnapshot(
        repoUrl: String,
        packages: List<String>
    ): FdroidRepositorySnapshot {
        return FdroidRepositorySnapshot(
            repoUrl = repoUrl,
            format = FdroidIndexFormat.V2,
            repoName = "F-Droid",
            repoDescription = "",
            timestampMillis = null,
            mirrors = emptyList(),
            packages = packages.associateWith(::packageSnapshot)
        )
    }

    private fun packageSnapshot(packageName: String): FdroidPackageSnapshot {
        return FdroidPackageSnapshot(
            repoUrl = "https://f-droid.org/repo",
            packageName = packageName,
            suggestedVersionCode = 1L,
            versions = listOf(version(packageName))
        )
    }

    private fun version(packageName: String): FdroidVersionSnapshot {
        return FdroidVersionSnapshot(
            versionName = "1.0",
            versionCode = 1L,
            apkName = "$packageName.apk",
            apkPath = "/repo/$packageName.apk",
            apkSha256 = "sha256",
            apkSizeBytes = 1L,
            addedAtMillis = null,
            minSdk = 23,
            targetSdk = 37,
            nativeAbis = emptyList(),
            signerSha256 = emptyList(),
            releaseChannels = emptyList(),
            whatsNew = "",
            antiFeatures = emptyList()
        )
    }
}
