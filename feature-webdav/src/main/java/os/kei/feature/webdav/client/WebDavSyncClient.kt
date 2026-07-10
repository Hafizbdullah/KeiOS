package os.kei.feature.webdav.client

import at.bitfire.dav4jvm.ktor.DavCollection
import at.bitfire.dav4jvm.ktor.DavResource
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.ktor.createDomainBasicAuthProvider
import at.bitfire.dav4jvm.ktor.createDomainDigestAuthProvider
import at.bitfire.dav4jvm.ktor.exception.ConflictException
import at.bitfire.dav4jvm.ktor.exception.DavException
import at.bitfire.dav4jvm.ktor.exception.ForbiddenException
import at.bitfire.dav4jvm.ktor.exception.HttpException
import at.bitfire.dav4jvm.ktor.exception.NotFoundException
import at.bitfire.dav4jvm.ktor.exception.PreconditionFailedException
import at.bitfire.dav4jvm.ktor.exception.UnauthorizedException
import at.bitfire.dav4jvm.ktor.resolve
import at.bitfire.dav4jvm.property.webdav.GetContentLength
import at.bitfire.dav4jvm.property.webdav.GetETag
import at.bitfire.dav4jvm.property.webdav.GetLastModified
import at.bitfire.dav4jvm.property.webdav.ResourceType
import at.bitfire.dav4jvm.property.webdav.WebDAV
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.http.withCharset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.feature.webdav.model.WebDavConfig
import os.kei.feature.webdav.model.WebDavRemoteFile
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * WebDAV client backed by dav4jvm's Ktor API.
 *
 * The public result model is intentionally stable because WebDAV sync planning, merge previews,
 * and conflict handling live in the app layer. Internally this client uses the current dav4jvm
 * Ktor surface exclusively.
 */
class WebDavSyncClient(
    private val config: WebDavConfig,
    private val ioDispatcher: CoroutineDispatcher = AppDispatchers.webDavNetwork,
) {
    private val httpClient = HttpClient(CIO) {
        followRedirects = false

        install(UserAgent) {
            agent = USER_AGENT
        }

        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }

        install(Auth) {
            providers.add(
                createDomainBasicAuthProvider(
                    username = config.username,
                    password = config.appPassword,
                ),
            )
            providers.add(
                createDomainDigestAuthProvider(
                    username = config.username,
                    password = config.appPassword,
                ),
            )
        }
    }

    private val baseUrl: Url by lazy { parseBaseUrl(config.serverUrl) }
    private var collectionKnown = false

    fun close() {
        httpClient.close()
    }

    suspend fun testConnection(): WebDavTestConnectionResult = withContext(ioDispatcher) {
        try {
            collection().propfind(0, WebDAV.ResourceType) { _, _ -> }
            collectionKnown = true
            WebDavTestConnectionResult.Success(dirCreated = false)
        } catch (e: NotFoundException) {
            AppLogger.i(TAG, "Remote dir not found, creating...")
            createDirectoryRecursive()
        } catch (e: ConflictException) {
            AppLogger.i(TAG, "Remote dir parent missing, creating...")
            createDirectoryRecursive()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnauthorizedException) {
            WebDavTestConnectionResult.AuthFailed
        } catch (e: ForbiddenException) {
            WebDavTestConnectionResult.PermissionDenied
        } catch (e: IOException) {
            WebDavTestConnectionResult.NetworkError(e.message ?: "Network unreachable")
        } catch (e: IllegalArgumentException) {
            WebDavTestConnectionResult.InvalidUrl(e.message ?: "Invalid URL")
        } catch (e: Exception) {
            WebDavTestConnectionResult.Error(toMessage(e))
        }
    }

    suspend fun upload(fileName: String, content: String, etag: String? = null): WebDavUploadResult =
        withContext(ioDispatcher) {
            try {
                ensureCollection()
                putFile(fileName, content, ifMatchHeaders(etag))
            } catch (e: PreconditionFailedException) {
                WebDavUploadResult.Conflict
            } catch (e: NotFoundException) {
                retryAfterEnsuringCollection(fileName, content, ifMatchHeaders(etag))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                WebDavUploadResult.Error(classifyError(e))
            }
        }

    suspend fun uploadIfAbsent(fileName: String, content: String): WebDavUploadResult =
        withContext(ioDispatcher) {
            try {
                ensureCollection()
                putFile(fileName, content, headersOf(HttpHeaders.IfNoneMatch, "*"))
            } catch (e: PreconditionFailedException) {
                WebDavUploadResult.Conflict
            } catch (e: NotFoundException) {
                retryAfterEnsuringCollection(fileName, content, headersOf(HttpHeaders.IfNoneMatch, "*"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                WebDavUploadResult.Error(classifyError(e))
            }
        }

    suspend fun download(fileName: String): WebDavDownloadResult = withContext(ioDispatcher) {
        try {
            val resource = DavResource(httpClient, fileUrl(fileName))
            var content: String? = null
            var etag: String? = null

            resource.get {
                content = it.bodyAsText()
                etag = GetETag.fromHttpResponse(it)?.rawETag
            }

            val text = content
            if (text.isNullOrBlank()) {
                WebDavDownloadResult.Empty
            } else {
                WebDavDownloadResult.Success(text, etag)
            }
        } catch (e: NotFoundException) {
            WebDavDownloadResult.Empty
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WebDavDownloadResult.Error(classifyError(e))
        }
    }

    suspend fun listFiles(): List<WebDavRemoteFile> = withContext(ioDispatcher) {
        try {
            ensureCollection()
            val files = mutableListOf<WebDavRemoteFile>()

            collection().propfind(
                1,
                WebDAV.GetETag,
                WebDAV.GetLastModified,
                WebDAV.GetContentLength,
                WebDAV.ResourceType,
            ) { response, relation ->
                if (relation == Response.HrefRelation.MEMBER && !response.isCollection()) {
                    files += WebDavRemoteFile(
                        href = response.href.toString(),
                        displayName = response.hrefName(),
                        lastModified = response[GetLastModified::class.java]?.lastModified?.toString(),
                        contentLength = response[GetContentLength::class.java]?.contentLength ?: 0L,
                        etag = response[GetETag::class.java]?.rawETag,
                    )
                }
            }

            files
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "listFiles failed", e)
            emptyList()
        }
    }

    private suspend fun retryAfterEnsuringCollection(
        fileName: String,
        content: String,
        headers: Headers?,
    ): WebDavUploadResult =
        try {
            collectionKnown = false
            ensureCollection()
            putFile(fileName, content, headers)
        } catch (e: PreconditionFailedException) {
            WebDavUploadResult.Conflict
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WebDavUploadResult.Error(classifyError(e))
        }

    private suspend fun putFile(fileName: String, content: String, headers: Headers?): WebDavUploadResult {
        val resource = DavResource(httpClient, fileUrl(fileName))
        var resultEtag: String? = null
        resource.put(TextContent(content, JSON_CONTENT_TYPE), headers) { response ->
            resultEtag = GetETag.fromHttpResponse(response)?.rawETag
        }
        return WebDavUploadResult.Success(resultEtag)
    }

    private suspend fun ensureCollection() {
        if (collectionKnown) return
        try {
            collection().propfind(0, WebDAV.ResourceType) { _, _ -> }
            collectionKnown = true
        } catch (_: NotFoundException) {
            createDirectorySync()
            collectionKnown = true
        } catch (_: ConflictException) {
            createDirectorySync()
            collectionKnown = true
        }
    }

    private suspend fun createDirectoryRecursive(): WebDavTestConnectionResult =
        try {
            createDirectorySync()
            collectionKnown = true
            WebDavTestConnectionResult.Success(dirCreated = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnauthorizedException) {
            WebDavTestConnectionResult.AuthFailed
        } catch (e: ForbiddenException) {
            WebDavTestConnectionResult.PermissionDenied
        } catch (e: IOException) {
            WebDavTestConnectionResult.NetworkError(e.message ?: "Network error")
        } catch (e: IllegalArgumentException) {
            WebDavTestConnectionResult.InvalidUrl(e.message ?: "Invalid URL")
        } catch (e: Exception) {
            WebDavTestConnectionResult.Error(toMessage(e))
        }

    private suspend fun createDirectorySync() {
        val segments = config.remoteDir
            .trim()
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() }
        if (segments.isEmpty()) return

        var currentPath = ""
        for (segment in segments) {
            currentPath += "$segment/"
            ensureDirectorySegment(resolveFromBase(currentPath), currentPath)
        }
    }

    private suspend fun ensureDirectorySegment(url: Url, label: String) {
        val collection = DavCollection(httpClient, url)
        try {
            collection.propfind(0, WebDAV.ResourceType) { _, _ -> }
            return
        } catch (_: NotFoundException) {
        } catch (_: ConflictException) {
        }

        try {
            collection.mkCol(null) { _ -> }
            AppLogger.i(TAG, "Created WebDAV directory: $label")
        } catch (e: HttpException) {
            if (e.statusCode == 405) {
                AppLogger.i(TAG, "WebDAV directory already exists: $label")
            } else {
                throw e
            }
        }
    }

    private fun collection(): DavCollection = DavCollection(httpClient, collectionUrl())

    private fun collectionUrl(): Url {
        val dir = config.remoteDir
            .trim()
            .trimStart('/')
            .let { path ->
                when {
                    path.isBlank() -> ""
                    path.endsWith("/") -> path
                    else -> "$path/"
                }
            }
        return if (dir.isBlank()) baseUrl else resolveFromBase(dir)
    }

    private fun fileUrl(fileName: String): Url =
        collectionUrl().resolve(fileName)
            ?: throw IllegalArgumentException("Invalid WebDAV file name: $fileName")

    private fun resolveFromBase(relativePath: String): Url =
        baseUrl.resolve(relativePath)
            ?: throw IllegalArgumentException("Invalid WebDAV path: $relativePath")

    private fun parseBaseUrl(url: String): Url {
        val trimmed = url.trim()
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "Server URL must start with http:// or https://, got: $url"
        }
        return Url(trimmed.trimEnd('/') + "/")
    }

    private fun Response.isCollection(): Boolean =
        this[ResourceType::class.java]
            ?.types
            ?.contains(WebDAV.Collection) == true

    private fun ifMatchHeaders(etag: String?): Headers? =
        etag?.takeIf { it.isNotBlank() }?.let {
            headersOf(HttpHeaders.IfMatch, formatEtagForHeader(it))
        }

    private fun formatEtagForHeader(etag: String): String {
        val trimmed = etag.trim()
        return when {
            trimmed.startsWith("W/\"") && trimmed.endsWith('"') -> trimmed
            trimmed.startsWith('"') && trimmed.endsWith('"') -> trimmed
            else -> "\"$trimmed\""
        }
    }

    private fun classifyError(e: Exception): WebDavError = when (e) {
        is UnauthorizedException -> WebDavError.AuthFailed
        is ForbiddenException -> WebDavError.PermissionDenied
        is ConflictException -> WebDavError.Unknown(409, "Directory conflict")
        is HttpException -> WebDavError.Unknown(e.statusCode, toMessage(e))
        is HttpRequestTimeoutException -> WebDavError.NetworkUnreachable
        is SocketTimeoutException -> WebDavError.NetworkUnreachable
        is IOException -> WebDavError.NetworkUnreachable
        is DavException -> WebDavError.Unknown(e.statusCode ?: 0, toMessage(e))
        is IllegalArgumentException -> WebDavError.Unknown(0, e.message ?: "Invalid WebDAV request")
        else -> WebDavError.Unknown(0, e.message ?: "Unknown error")
    }

    private fun toMessage(e: Exception): String = when (e) {
        is HttpException -> "HTTP ${e.statusCode}: ${e.message ?: "request rejected"}"
        is DavException -> e.message ?: "WebDAV error"
        else -> e.message ?: "Unknown error"
    }

    companion object {
        private const val TAG = "WebDavSyncClient"
        private const val CONNECT_TIMEOUT_MS = 30_000L
        private const val REQUEST_TIMEOUT_MS = 60_000L
        private const val SOCKET_TIMEOUT_MS = 60_000L
        private val JSON_CONTENT_TYPE = ContentType.Application.Json.withCharset(Charsets.UTF_8)

        const val USER_AGENT: String = "KeiOS-WebDAV/1 (+https://github.com/KeiOS) ktor"
    }
}

sealed interface WebDavTestConnectionResult {
    data class Success(val dirCreated: Boolean) : WebDavTestConnectionResult
    data object AuthFailed : WebDavTestConnectionResult
    data object PermissionDenied : WebDavTestConnectionResult
    data class NetworkError(val message: String) : WebDavTestConnectionResult
    data class InvalidUrl(val message: String) : WebDavTestConnectionResult
    data class Error(val message: String) : WebDavTestConnectionResult
}

sealed interface WebDavUploadResult {
    data class Success(val etag: String?) : WebDavUploadResult
    data object Conflict : WebDavUploadResult
    data class Error(val error: WebDavError) : WebDavUploadResult
}

sealed interface WebDavDownloadResult {
    data class Success(val content: String, val etag: String?) : WebDavDownloadResult
    data object Empty : WebDavDownloadResult
    data class Error(val error: WebDavError) : WebDavDownloadResult
}

sealed interface WebDavError {
    data object NetworkUnreachable : WebDavError
    data object AuthFailed : WebDavError
    data object PermissionDenied : WebDavError
    data class Unknown(val code: Int, val message: String) : WebDavError
}
