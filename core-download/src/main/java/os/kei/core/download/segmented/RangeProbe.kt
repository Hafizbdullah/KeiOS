package os.kei.core.download.segmented

import okhttp3.OkHttpClient
import okhttp3.Request
import os.kei.core.io.executeCancellable
import java.io.IOException

internal data class RangeProbeResult(
    val rangeSupported: Boolean,
    val totalBytes: Long,
    val finalUrl: String,
    val fallbackReason: String? = null,
)

internal class RangeProbe(
    private val client: OkHttpClient,
) {
    suspend fun probe(request: SegmentedDownloadRequest): RangeProbeResult {
        val probeRequest = request.newRequestBuilder()
            .header("Range", "bytes=0-0")
            .get()
            .build()
        return client.executeCancellable(probeRequest) { response ->
            val finalUrl = response.request.url.toString()
            when (response.code) {
                206 -> {
                    val range = parseContentRange(response.header("Content-Range"))
                    if (range == null || range.start != 0L || range.end != 0L || range.totalBytes <= 0L) {
                        RangeProbeResult(
                            rangeSupported = false,
                            totalBytes = -1L,
                            finalUrl = finalUrl,
                            fallbackReason = "invalid-content-range",
                        )
                    } else {
                        RangeProbeResult(
                            rangeSupported = true,
                            totalBytes = range.totalBytes,
                            finalUrl = finalUrl,
                        )
                    }
                }

                200 -> {
                    RangeProbeResult(
                        rangeSupported = false,
                        totalBytes = response.body.contentLength().takeIf { it > 0L } ?: -1L,
                        finalUrl = finalUrl,
                        fallbackReason = "range-ignored",
                    )
                }

                else -> throw IOException("HTTP ${response.code}")
            }
        }
    }
}

internal data class ParsedContentRange(
    val start: Long,
    val end: Long,
    val totalBytes: Long,
)

internal fun parseContentRange(value: String?): ParsedContentRange? {
    val match = CONTENT_RANGE_REGEX.matchEntire(value?.trim().orEmpty()) ?: return null
    val start = match.groupValues[1].toLongOrNull() ?: return null
    val end = match.groupValues[2].toLongOrNull() ?: return null
    val total = match.groupValues[3].toLongOrNull() ?: return null
    if (start < 0L || end < start || total <= end) return null
    return ParsedContentRange(start = start, end = end, totalBytes = total)
}

internal fun SegmentedDownloadRequest.newRequestBuilder(): Request.Builder {
    val builder = Request.Builder().url(url)
    headers.forEach { (name, value) ->
        if (name.isNotBlank() && value.isNotBlank()) {
            builder.header(name, value)
        }
    }
    return builder
}

private val CONTENT_RANGE_REGEX = Regex("""bytes\s+(\d+)-(\d+)/(\d+)""")
