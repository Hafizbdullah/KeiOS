package os.kei.core.io

import okhttp3.ResponseBody

fun ResponseBody.stringLimitedBlocking(maxBytes: Long): String {
    require(maxBytes > 0L) { "maxBytes must be positive" }
    val declaredLength = contentLength()
    if (declaredLength > maxBytes) {
        throw BoundedContentTextReadTooLargeException(
            maxBytes = maxBytes,
            observedBytes = declaredLength,
            declaredBytes = declaredLength,
            stage = BoundedContentReadLimitStage.DeclaredLength,
        )
    }
    return byteStream().use { input ->
        input.readTextLimitedBlocking(maxBytes).text
    }
}
