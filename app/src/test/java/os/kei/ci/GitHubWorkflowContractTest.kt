package os.kei.ci

import org.junit.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubWorkflowContractTest {
    @Test
    fun `debug apk workflow includes unit tests as parallel job`() {
        val workflow = workflowText("ci-debug-apk.yml")
        val setupAction = actionText("setup-android-gradle-build/action.yml")

        // Unit tests run as a parallel job inside the debug APK workflow
        assertContains(workflow, "./gradlew :app:testDebugUnitTest --stacktrace")
        assertContains(workflow, "./gradlew :core-log:compileDebugKotlin :core-io:compileDebugKotlin --stacktrace")
        assertContains(workflow, "cache-read-only: \"true\"")
        assertWorkflowTriggersAppAndBuildChanges(workflow)
        assertSetupActionUsesCurrentActions(setupAction)
    }

    @Test
    fun `apk workflows keep expected assemble tasks and artifact signer verification`() {
        val debugWorkflow = workflowText("ci-debug-apk.yml")
        val benchmarkWorkflow = workflowText("ci-benchmark-apk.yml")

        assertWorkflowUsesCurrentActions(debugWorkflow)
        assertContains(debugWorkflow, "./gradlew :app:assembleDebug --stacktrace")
        assertContains(debugWorkflow, "EXPECTED_APK_SIGNER_SHA256")
        assertContains(debugWorkflow, "apksigner\" verify --print-certs")
        assertWorkflowTriggersAppAndBuildChanges(debugWorkflow)

        assertWorkflowUsesCurrentActions(benchmarkWorkflow)
        assertContains(benchmarkWorkflow, "\":app:assembleBenchmark\"")
        assertContains(benchmarkWorkflow, "lintVitalBenchmark")
        assertContains(benchmarkWorkflow, "EXPECTED_APK_SIGNER_SHA256")
        assertContains(benchmarkWorkflow, "apksigner\" verify --print-certs")
    }

    @Test
    fun `tracked workflow set stays explicit`() {
        val workflows =
            workflowsDir()
                .listFiles { file -> file.isFile && file.extension == "yml" }
                .orEmpty()
                .map { it.name }
                .sorted()

        assertEquals(
            listOf(
                "ci-benchmark-apk.yml",
                "ci-debug-apk.yml",
            ),
            workflows,
        )
    }

    private fun assertWorkflowTriggersAppAndBuildChanges(workflow: String) {
        listOf(
            "app/**",
            "core-concurrency/**",
            "core-io/**",
            "core-json/**",
            "core-log/**",
            "core-prefs/**",
            "core-system/**",
            "feature-ba/**",
            "feature-github/**",
            "feature-home/**",
            "feature-mcp/**",
            "feature-os/**",
            "feature-webdav/**",
            "miuix-navigation3-ui/**",
            "ui-liquid-glass/**",
            "ui-pip/**",
            "baselineprofile/**",
            "build.gradle.kts",
            "gradle.properties",
            "gradle/**",
            "gradlew",
            "settings.gradle.kts",
        ).forEach { path ->
            assertContains(workflow, "- \"$path\"")
        }
        assertContains(workflow, "- \".github/actions/setup-android-gradle-build/**\"")
    }

    private fun assertWorkflowUsesCurrentActions(workflow: String) {
        assertContains(workflow, "uses: actions/checkout@v7")
        assertContains(workflow, "persist-credentials: false")
        assertContains(workflow, "uses: actions/upload-artifact@v7")
    }

    private fun assertSetupActionUsesCurrentActions(action: String) {
        assertContains(action, "uses: gradle/actions/wrapper-validation@v6")
        assertContains(action, "uses: actions/setup-java@v5")
        assertContains(action, "uses: gradle/actions/setup-gradle@v6")
        assertContains(action, "cache-provider: enhanced")
        assertContains(action, "uses: android-actions/setup-android@v4")
    }

    private fun workflowText(name: String): String {
        val file = File(workflowsDir(), name)
        assertTrue(file.isFile, "Missing workflow: $name")
        return file.readText()
    }

    private fun actionText(name: String): String {
        val file = File(actionsDir(), name)
        assertTrue(file.isFile, "Missing action: $name")
        return file.readText()
    }

    private fun workflowsDir(): File {
        val start = File(checkNotNull(System.getProperty("user.dir"))).absoluteFile
        return generateSequence(start) { it.parentFile }
            .map { File(it, ".github/workflows") }
            .firstOrNull { it.isDirectory }
            ?: error("Cannot locate .github/workflows from ${start.path}")
    }

    private fun actionsDir(): File {
        val start = File(checkNotNull(System.getProperty("user.dir"))).absoluteFile
        return generateSequence(start) { it.parentFile }
            .map { File(it, ".github/actions") }
            .firstOrNull { it.isDirectory }
            ?: error("Cannot locate .github/actions from ${start.path}")
    }
}
