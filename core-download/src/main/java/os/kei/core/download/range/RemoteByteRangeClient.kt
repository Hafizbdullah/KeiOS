package os.kei.core.download.range

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import os.kei.core.io.executeCancellable

data class RemoteByteRangeProbe(
    val totalSize: Long,
    val finalUrl: String,
    val identity: RemoteResourceIdentity? = null,
)

data class RemoteByteRangeResult(
    val bytes: ByteArray,
    val start: Long,
    val endInclusive: Long,
    val totalSize: Long,
    val finalUrl: String,
    val identity: RemoteResourceIdentity? = null,
)

data class RemoteResourceIdentity(
    val strongEtag: String? = null,
    val lastModified: String? = null,
) {
    init {
        require(strongEtag != null || lastModified != null) { "resource identity cannot be empty" }
    }

    val ifRangeValue: String
        get() = strongEtag ?: requireNotNull(lastModified)
}

class RemoteByteRangeClient(
    private val client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun probe(request: Request): RemoteByteRangeProbe =
        withContext(dispatcher) {
            client.executeCancellable(request.asRangeRequest(start = 0L, endInclusive = 0L)) { response ->
                val contentRange = response.requireByteRangeResponse(expectedStart = 0L, expectedEndInclusive = 0L)
                RemoteByteRangeProbe(
                    totalSize = contentRange.totalSize,
                    finalUrl = response.request.url.toString(),
                    identity = response.resourceIdentity(),
                )
            }
        }

    suspend fun read(
        request: Request,
        start: Long,
        endInclusive: Long,
        maxBytes: Long,
        expectedTotalSize: Long? = null,
        expectedIdentity: RemoteResourceIdentity? = null,
    ): RemoteByteRangeResult =
        withContext(dispatcher) {
            require(start >= 0L) { "range start cannot be negative" }
            require(endInclusive >= start) { "range end cannot be before start" }
            require(maxBytes > 0L) { "maxBytes must be positive" }
            val expectedLength = endInclusive - start + 1L
            require(expectedLength > 0L && expectedLength <= maxBytes) {
                "range length $expectedLength exceeds limit $maxBytes"
            }
            val rangeRequest =
                request
                    .asRangeRequest(start = start, endInclusive = endInclusive)
                    .newBuilder()
                    .apply { expectedIdentity?.let { header("If-Range", it.ifRangeValue) } }
                    .build()
            client.executeCancellable(rangeRequest) { response ->
                val contentRange = response.requireByteRangeResponse(start, endInclusive)
                if (expectedTotalSize != null && contentRange.totalSize != expectedTotalSize) {
                    throw RemoteByteRangeResourceChangedException(
                        "remote size changed expected=$expectedTotalSize actual=${contentRange.totalSize}",
                    )
                }
                val responseIdentity = response.resourceIdentity()
                if (expectedIdentity != null && responseIdentity.conflictsWith(expectedIdentity)) {
                    throw RemoteByteRangeResourceChangedException("remote resource identity changed")
                }
                val body = response.body
                val declaredLength = body.contentLength()
                if (declaredLength > expectedLength) {
                    throw RemoteByteRangeProtocolException(
                        "range body exceeds expected length expected=$expectedLength declared=$declaredLength",
                    )
                }
                val source = body.source()
                val bytes = source.readByteArray(expectedLength)
                if (!source.exhausted()) {
                    throw RemoteByteRangeProtocolException(
                        "range body exceeds expected length expected=$expectedLength",
                    )
                }
                RemoteByteRangeResult(
                    bytes = bytes,
                    start = contentRange.start,
                    endInclusive = contentRange.endInclusive,
                    totalSize = contentRange.totalSize,
                    finalUrl = response.request.url.toString(),
                    identity = responseIdentity,
                )
            }
        }
}

class RemoteByteRangeProtocolException(message: String) : IOException(message)

class RemoteByteRangeResourceChangedException(message: String) : IOException(message)

private data class ParsedByteContentRange(
    val start: Long,
    val endInclusive: Long,
    val totalSize: Long,
)

private fun Request.asRangeRequest(
    start: Long,
    endInclusive: Long,
): Request =
    newBuilder()
        .get()
        .header("Range", "bytes=$start-$endInclusive")
        .removeHeader("If-Range")
        .build()

private fun Response.requireByteRangeResponse(
    expectedStart: Long,
    expectedEndInclusive: Long,
): ParsedByteContentRange {
    if (code != 206) {
        if (code == 200 || code == 416) {
            throw RemoteByteRangeResourceChangedException("byte range was not satisfied (HTTP $code)")
        }
        throw RemoteByteRangeProtocolException("byte-range request failed (HTTP $code)")
    }
    val parsed = parseByteContentRange(header("Content-Range"))
        ?: throw RemoteByteRangeProtocolException("missing or invalid Content-Range")
    if (parsed.start != expectedStart || parsed.endInclusive != expectedEndInclusive) {
        throw RemoteByteRangeProtocolException(
            "unexpected Content-Range ${header("Content-Range").orEmpty()} " +
                "expected=bytes $expectedStart-$expectedEndInclusive/*",
        )
    }
    return parsed
}

private fun parseByteContentRange(value: String?): ParsedByteContentRange? {
    val match = CONTENT_RANGE_REGEX.matchEntire(value?.trim().orEmpty()) ?: return null
    val start = match.groupValues[1].toLongOrNull() ?: return null
    val endInclusive = match.groupValues[2].toLongOrNull() ?: return null
    val totalSize = match.groupValues[3].toLongOrNull() ?: return null
    if (start < 0L || endInclusive < start || totalSize <= endInclusive) return null
    return ParsedByteContentRange(start, endInclusive, totalSize)
}

private fun Response.resourceIdentity(): RemoteResourceIdentity? {
    val strongEtag = header("ETag")
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.startsWith("W/", ignoreCase = true) }
    val lastModified = header("Last-Modified")?.trim()?.takeIf(String::isNotEmpty)
    return when {
        strongEtag != null -> RemoteResourceIdentity(strongEtag = strongEtag)
        lastModified != null -> RemoteResourceIdentity(lastModified = lastModified)
        else -> null
    }
}

private fun RemoteResourceIdentity?.conflictsWith(expected: RemoteResourceIdentity): Boolean =
    when {
        this == null -> false
        expected.strongEtag != null -> strongEtag != null && strongEtag != expected.strongEtag
        expected.lastModified != null -> lastModified != null && lastModified != expected.lastModified
        else -> false
    }

private val CONTENT_RANGE_REGEX = Regex("""bytes\s+(\d+)-(\d+)/(\d+)""")
