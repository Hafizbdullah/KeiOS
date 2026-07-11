package os.kei.feature.github.install

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitHubInstallArchiveSupportTest {
    @Test
    fun `hma module archive selects nested manager apk`() {
        val archive = createZip("manager.apk", "module.prop")
        try {
            ZipFile(archive).use { zip ->
                assertEquals("manager.apk", selectGitHubInstallApkEntry(zip)?.name)
            }
        } finally {
            archive.delete()
        }
    }

    @Test
    fun `ordinary source archive has no installable apk`() {
        val archive = createZip("README.md", "src/main.kt")
        try {
            ZipFile(archive).use { zip ->
                assertNull(selectGitHubInstallApkEntry(zip))
            }
        } finally {
            archive.delete()
        }
    }

    private fun createZip(vararg entryNames: String): File {
        val file = File.createTempFile("keios-install-archive-", ".zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            entryNames.forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }
        }
        return file
    }
}
