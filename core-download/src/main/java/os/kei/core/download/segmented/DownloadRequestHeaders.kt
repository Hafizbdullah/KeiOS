package os.kei.core.download.segmented

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

internal fun SegmentedDownloadRequest.newRequestBuilder(
    targetUrl: String = url,
): Request.Builder {
    val originUrl = url.toHttpUrl()
    val destinationUrl = targetUrl.toHttpUrl()
    val sameOrigin = originUrl.sameOriginAs(destinationUrl)
    val builder = Request.Builder()
        .url(destinationUrl)
        .header("Accept-Encoding", "identity")
    headers.forEach { (name, value) ->
        if (
            name.isNotBlank() &&
            value.isNotBlank() &&
            !name.isDownloadControlledHeader() &&
            (sameOrigin || !name.isOriginSensitiveHeader())
        ) {
            builder.header(name, value)
        }
    }
    return builder
}

private fun HttpUrl.sameOriginAs(other: HttpUrl): Boolean =
    scheme == other.scheme &&
        host == other.host &&
        port == other.port

private fun String.isDownloadControlledHeader(): Boolean =
    DOWNLOAD_CONTROLLED_HEADERS.any { equals(it, ignoreCase = true) }

private fun String.isOriginSensitiveHeader(): Boolean =
    ORIGIN_SENSITIVE_HEADERS.any { equals(it, ignoreCase = true) }

private val DOWNLOAD_CONTROLLED_HEADERS =
    setOf(
        "Accept-Encoding",
        "Content-Length",
        "Host",
        "If-Range",
        "Range",
        "Transfer-Encoding",
    )

private val ORIGIN_SENSITIVE_HEADERS =
    setOf(
        "Authorization",
        "Cookie",
        "Proxy-Authorization",
    )
