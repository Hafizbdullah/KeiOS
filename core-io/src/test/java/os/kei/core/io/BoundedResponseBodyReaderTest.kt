package os.kei.core.io

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BoundedResponseBodyReaderTest {
    @Test
    fun `declared oversized response reports length before reading`() {
        val body = "oversized".toResponseBody("text/plain".toMediaType())

        val error = assertFailsWith<BoundedContentTextReadTooLargeException> {
            body.stringLimitedBlocking(maxBytes = 4L)
        }

        assertEquals(4L, error.maxBytes)
        assertEquals(9L, error.observedBytes)
        assertEquals(9L, error.declaredBytes)
        assertEquals(BoundedContentReadLimitStage.DeclaredLength, error.stage)
    }

    @Test
    fun `streamed oversized response reports observed bytes`() {
        val source = Buffer().writeUtf8("oversized")
        val body = UnknownLengthResponseBody(source)

        val error = assertFailsWith<BoundedContentTextReadTooLargeException> {
            body.stringLimitedBlocking(maxBytes = 4L)
        }

        assertEquals(4L, error.maxBytes)
        assertEquals(9L, error.observedBytes)
        assertEquals(null, error.declaredBytes)
        assertEquals(BoundedContentReadLimitStage.Streaming, error.stage)
    }

    private class UnknownLengthResponseBody(
        private val source: BufferedSource,
    ) : okhttp3.ResponseBody() {
        override fun contentType() = "text/plain".toMediaType()

        override fun contentLength(): Long = -1L

        override fun source(): BufferedSource = source
    }
}
