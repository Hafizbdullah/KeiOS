package os.kei.core.io

import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

suspend fun <T> OkHttpClient.executeCancellable(
    request: Request,
    block: (Response) -> T,
): T {
    return newCall(request).executeCancellable(block)
}

suspend fun OkHttpClient.executeCancellable(request: Request): Response {
    return newCall(request).executeCancellable()
}

suspend fun <T> Call.executeCancellable(
    block: (Response) -> T,
): T {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        try {
            val result = execute().use(block)
            if (continuation.isActive) {
                continuation.resume(result)
            }
        } catch (error: Throwable) {
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }
    }
}

suspend fun Call.executeCancellable(): Response {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        try {
            val response = execute()
            if (continuation.isActive) {
                continuation.resume(response)
            } else {
                response.close()
            }
        } catch (error: Throwable) {
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }
    }
}

suspend inline fun <T> cancellableResult(
    crossinline block: suspend () -> T,
): Result<T> {
    return try {
        Result.success(block())
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        Result.failure(error)
    }
}

inline fun <T> resultPreservingCancellation(
    block: () -> T,
): Result<T> {
    return try {
        Result.success(block())
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        Result.failure(error)
    }
}
