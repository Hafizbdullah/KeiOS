package os.kei.feature.github.engine.apk

import org.junit.Test
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.feature.github.model.GitHubApkManifestInfo
import java.io.File
import java.util.Locale
import kotlin.math.ceil
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubApkCandidateSelectionBenchmarkTest {
    @Test
    fun `apk candidate corpus measures planning and manifest selection`() {
        val realisticAssets = realisticAssetCorpus()
        val planned = GitHubApkCandidateSelectionEngine.planInspection(
            assets = realisticAssets,
            expectedPackageName = "os.kei.keios",
        )
        val legacy = realisticAssets
            .filter { it.name.endsWith(".apk", ignoreCase = true) }
            .take(GitHubApkCandidateSelectionEngine.DEFAULT_MAX_INSPECTION_CANDIDATES)

        assertEquals("KeiOS-arm64-v8a-release.apk", planned.first().name)
        assertTrue(planned.any { it.name == "KeiOS-arm64-v8a-release.apk" })
        assertTrue(legacy.none { it.name == "KeiOS-arm64-v8a-release.apk" })

        val inspected = planned.mapIndexed { index, asset ->
            GitHubInspectedApkCandidate(
                asset = asset,
                manifest = GitHubApkManifestInfo(
                    assetName = asset.name,
                    packageName = if (asset.name.startsWith("KeiOS-")) {
                        "os.kei.keios"
                    } else {
                        "fixture.other.$index"
                    },
                    versionName = "1.0.$index",
                    versionCode = (10_000 + index).toString(),
                ),
            )
        }
        assertEquals(
            "KeiOS-arm64-v8a-release.apk",
            GitHubApkCandidateSelectionEngine.selectInspected(
                inspected = inspected,
                expectedPackageName = "os.kei.keios",
            )?.candidate?.asset?.name,
        )

        val floodAssets = List(FLOOD_ASSET_COUNT) { index ->
            val suffix = when (index % 6) {
                0 -> "arm64-v8a-release"
                1 -> "universal-release"
                2 -> "armeabi-v7a-release"
                3 -> "x86_64-release"
                4 -> "debug-arm64-v8a"
                else -> "benchmark-arm64-v8a"
            }
            asset("fixture-$index-$suffix.apk")
        }

        val metrics = listOf(
            measureMetric(
                name = "legacy-filter-take-realistic-20",
                operationsPerSample = REALISTIC_REPEATS * realisticAssets.size,
            ) {
                repeat(REALISTIC_REPEATS) {
                    val result = realisticAssets
                        .filter { it.name.endsWith(".apk", ignoreCase = true) }
                        .take(GitHubApkCandidateSelectionEngine.DEFAULT_MAX_INSPECTION_CANDIDATES)
                    benchmarkSink = benchmarkSink xor result.hashCode()
                }
            },
            measureMetric(
                name = "engine-plan-realistic-20",
                operationsPerSample = REALISTIC_REPEATS * realisticAssets.size,
            ) {
                repeat(REALISTIC_REPEATS) {
                    benchmarkSink = benchmarkSink xor
                        GitHubApkCandidateSelectionEngine.planInspection(
                            assets = realisticAssets,
                            expectedPackageName = "os.kei.keios",
                        ).hashCode()
                }
            },
            measureMetric(
                name = "engine-plan-flood-1000",
                operationsPerSample = FLOOD_REPEATS * floodAssets.size,
                sampleCount = 5,
            ) {
                repeat(FLOOD_REPEATS) {
                    benchmarkSink = benchmarkSink xor
                        GitHubApkCandidateSelectionEngine.planInspection(
                            assets = floodAssets,
                            expectedPackageName = "fixture.target",
                        ).hashCode()
                }
            },
            measureMetric(
                name = "engine-select-inspected-12",
                operationsPerSample = SELECTION_REPEATS * inspected.size,
            ) {
                repeat(SELECTION_REPEATS) {
                    benchmarkSink = benchmarkSink xor requireNotNull(
                        GitHubApkCandidateSelectionEngine.selectInspected(
                            inspected = inspected,
                            expectedPackageName = "os.kei.keios",
                        ),
                    ).hashCode()
                }
            },
        )

        val report = writeReport(metrics)
        println(report.readText())
        assertTrue(metrics.all { it.averageNsPerOperation > 0.0 })
    }

    private fun realisticAssetCorpus(): List<GitHubReleaseAssetFile> {
        return buildList {
            repeat(12) { index -> add(asset("other-package-$index.apk")) }
            add(asset("notes.txt"))
            add(asset("KeiOS-metadata.apk"))
            add(asset("KeiOS-debug-arm64-v8a.apk"))
            add(asset("KeiOS-benchmark-arm64-v8a.apk"))
            add(asset("KeiOS-x86_64-release.apk"))
            add(asset("KeiOS-armeabi-v7a-release.apk"))
            add(asset("KeiOS-universal-release.apk"))
            add(asset("KeiOS-arm64-v8a-release.apk"))
        }
    }

    private fun asset(name: String): GitHubReleaseAssetFile {
        return GitHubReleaseAssetFile(
            name = name,
            downloadUrl = "https://example.test/$name",
            sizeBytes = 1_024L,
            downloadCount = 1,
        )
    }

    private fun measureMetric(
        name: String,
        operationsPerSample: Int,
        warmupCount: Int = 3,
        sampleCount: Int = 8,
        block: () -> Unit,
    ): BenchmarkMetric {
        repeat(warmupCount) { block() }
        val durations = List(sampleCount) {
            val startedAt = System.nanoTime()
            block()
            System.nanoTime() - startedAt
        }
        return BenchmarkMetric(name, operationsPerSample, durations)
    }

    private fun writeReport(metrics: List<BenchmarkMetric>): File {
        val workingDir = File(System.getProperty("user.dir").orEmpty())
        val moduleDir = if (workingDir.name == "feature-github-engine") {
            workingDir
        } else {
            File(workingDir, "feature-github-engine")
        }
        val reportDir = File(moduleDir, "build/reports/github-apk-selection")
        reportDir.mkdirs()
        return File(reportDir, "apk-candidate-selection-benchmark.md").apply {
            writeText(buildReportMarkdown(metrics))
        }
    }

    private fun buildReportMarkdown(metrics: List<BenchmarkMetric>): String {
        return buildString {
            appendLine("# GitHub APK Candidate Selection Benchmark")
            appendLine()
            appendLine("Corpus: real naming patterns from Mithka and KeiOS plus a 1000-asset synthetic flood.")
            appendLine("Runtime: local JVM Debug unit-test variant with JIT warmup; use values for relative algorithm trends.")
            appendLine()
            appendLine("| workload | operations/sample | avg ms | p50 ms | p95 ms | ns/op | ops/s |")
            appendLine("| --- | ---: | ---: | ---: | ---: | ---: | ---: |")
            metrics.forEach { metric ->
                append("| ")
                append(metric.name)
                append(" | ")
                append(metric.operationsPerSample)
                append(" | ")
                append(metric.averageMs.formatMetric())
                append(" | ")
                append(metric.medianMs.formatMetric())
                append(" | ")
                append(metric.p95Ms.formatMetric())
                append(" | ")
                append(metric.averageNsPerOperation.formatMetric())
                append(" | ")
                append(metric.operationsPerSecond.formatMetric())
                appendLine(" |")
            }
        }
    }

    private fun Double.formatMetric(): String {
        return String.format(Locale.US, "%.3f", this)
    }

    private data class BenchmarkMetric(
        val name: String,
        val operationsPerSample: Int,
        val durationsNs: List<Long>,
    ) {
        private val sortedDurations = durationsNs.sorted()

        val averageMs: Double
            get() = durationsNs.average() / NANOS_PER_MILLISECOND

        val medianMs: Double
            get() = sortedDurations[sortedDurations.size / 2] / NANOS_PER_MILLISECOND

        val p95Ms: Double
            get() {
                val index = (ceil(sortedDurations.size * 0.95).toInt() - 1)
                    .coerceIn(sortedDurations.indices)
                return sortedDurations[index] / NANOS_PER_MILLISECOND
            }

        val averageNsPerOperation: Double
            get() = durationsNs.average() / operationsPerSample.coerceAtLeast(1)

        val operationsPerSecond: Double
            get() = NANOS_PER_SECOND / averageNsPerOperation
    }

    private companion object {
        const val FLOOD_ASSET_COUNT = 1_000
        const val REALISTIC_REPEATS = 2_000
        const val FLOOD_REPEATS = 20
        const val SELECTION_REPEATS = 10_000
        const val NANOS_PER_MILLISECOND = 1_000_000.0
        const val NANOS_PER_SECOND = 1_000_000_000.0

        @Volatile
        var benchmarkSink: Int = 0
    }
}
