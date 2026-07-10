package os.kei.core.download.segmented

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertFailsWith

class DownloadIntegrityTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `invalid raw sha256 is rejected`() {
        val file = temp.newFile("digest.bin").apply { writeText("content") }

        assertFailsWith<SegmentedDownloadException> {
            verifyDownloadedSha256(file, "invalid-digest")
        }
    }
}
