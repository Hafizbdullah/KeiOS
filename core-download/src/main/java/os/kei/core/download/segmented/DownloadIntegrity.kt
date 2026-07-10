package os.kei.core.download.segmented

import java.io.File
import java.security.MessageDigest

internal fun validateExpectedDownloadSize(
    actualBytes: Long,
    expectedBytes: Long,
) {
    if (expectedBytes < 0L || actualBytes < 0L || actualBytes == expectedBytes) return
    throw SegmentedDownloadException(
        message = "download size mismatch expected=$expectedBytes actual=$actualBytes",
    )
}

internal fun verifyDownloadedSha256(
    file: File,
    expectedSha256: String,
) {
    val expected = expectedSha256.normalizedSha256OrNull() ?: return
    val actual = file.sha256Hex()
    if (!actual.equals(expected, ignoreCase = true)) {
        throw SegmentedDownloadException(
            message = "sha256 mismatch expected=$expected actual=$actual",
        )
    }
}

private fun String.normalizedSha256OrNull(): String? {
    val raw = trim()
    if (raw.isBlank()) return null
    val normalized =
        if (raw.startsWith("sha256:", ignoreCase = true)) {
            raw.substringAfter(':')
        } else {
            raw
        }.lowercase()
    if (!SHA256_HEX_REGEX.matches(normalized)) {
        throw SegmentedDownloadException(message = "invalid sha256 digest")
    }
    return normalized
}

private fun File.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_SEGMENTED_DOWNLOAD_BUFFER_SIZE_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private val SHA256_HEX_REGEX = Regex("[0-9a-f]{64}")
