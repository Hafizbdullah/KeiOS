package os.kei.core.perf

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppJankMonitorBuildModeTest {
    @Test
    fun `in-process jank tracking stays limited to debug builds`() {
        val source = sourceFile(MAIN_ACTIVITY_SOURCE)

        assertTrue("enabled = BuildConfig.DEBUG" in source)
        assertFalse("BuildConfig.BUILD_TYPE != \"release\"" in source)
    }
}

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

private const val MAIN_ACTIVITY_SOURCE = "app/src/main/java/os/kei/MainActivity.kt"
