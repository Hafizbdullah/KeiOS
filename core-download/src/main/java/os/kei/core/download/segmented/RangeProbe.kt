package os.kei.core.download.segmented

import okhttp3.OkHttpClient
import okhttp3.Response
import os.kei.core.io.executeCancellable
import java.io.IOException

internal data class RangeProbeResult(
    val rangeSupported: Boolean,
    val totalBytes: Long,
    val finalUrl: String,
    val fallbackReason: String? = null,
    val resourceValidator: RangeResourceValidator? = null,
)

internal data class RangeResourceValidator(
    val strongEtag: String? = null,
    val lastModified: String? = null,
) {
    init {
        require(strongEtag != null || lastModified != null) { "resource validator cannot be empty" }
    }

    val ifRangeValue: String
        get() = strongEtag ?: requireNotNull(lastModified)

    fun responseChanged(response: Response): Boolean =
        when {
            strongEtag != null ->
                response.header("ETag")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { it != strongEtag } == true

            lastModified != null ->
                response.header("Last-Modified")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { it != lastModified } == true

            else -> false
        }

    companion object {
        fun from(response: Response): RangeResourceValidator? {
            val strongEtag = response.header("ETag")
                ?.trim()
                ?.takeIf { it.isNotEmpty() && !it.startsWith("W/", ignoreCase = true) }
            val lastModified = response.header("Last-Modified")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            return when {
                strongEtag != null -> RangeResourceValidator(strongEtag = strongEtag)
                lastModified != null -> RangeResourceValidator(lastModified = lastModified)
                else -> null
            }
        }
    }
}

internal class RangeProbe(
    client: OkHttpClient,
) {
    private val probeClient = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun probe(request: SegmentedDownloadRequest): RangeProbeResult {
        var currentUrl = request.url
        repeat(MAX_PROBE_REDIRECTS + 1) { redirectCount ->
            val probeRequest = request.newRequestBuilder(targetUrl = currentUrl)
                .header("Range", "bytes=0-0")
                .get()
                .build()
            val step = probeClient.executeCancellable(probeRequest) { response ->
                if (response.code.isProbeRedirect()) {
                    val location = response.header("Location")
                        ?: throw IOException("redirect missing Location")
                    val nextUrl = response.request.url.resolve(location)
                        ?: throw IOException("invalid redirect Location")
                    ProbeStep.Redirect(nextUrl.toString())
                } else {
                    ProbeStep.Complete(response.toRangeProbeResult())
                }
            }
            when (step) {
                is ProbeStep.Complete -> return step.result
                is ProbeStep.Redirect -> {
                    if (redirectCount >= MAX_PROBE_REDIRECTS) {
                        throw IOException("too many redirects")
                    }
                    currentUrl = step.url
                }
            }
        }
        throw IOException("too many redirects")
    }
}

private sealed interface ProbeStep {
    data class Redirect(val url: String) : ProbeStep

    data class Complete(val result: RangeProbeResult) : ProbeStep
}

private fun Response.toRangeProbeResult(): RangeProbeResult {
    val finalUrl = request.url.toString()
    return when (code) {
        206 -> {
            val range = parseContentRange(header("Content-Range"))
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
                    resourceValidator = RangeResourceValidator.from(this),
                )
            }
        }

        200 -> {
            RangeProbeResult(
                rangeSupported = false,
                totalBytes = body.contentLength().takeIf { it > 0L } ?: -1L,
                finalUrl = finalUrl,
                fallbackReason = "range-ignored",
                resourceValidator = RangeResourceValidator.from(this),
            )
        }

        else -> throw IOException("HTTP $code")
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

private val CONTENT_RANGE_REGEX = Regex("""bytes\s+(\d+)-(\d+)/(\d+)""")

private fun Int.isProbeRedirect(): Boolean =
    this == 301 || this == 302 || this == 303 || this == 307 || this == 308

private const val MAX_PROBE_REDIRECTS = 20
