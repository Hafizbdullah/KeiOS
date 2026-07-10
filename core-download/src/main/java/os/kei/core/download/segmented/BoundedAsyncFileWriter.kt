package os.kei.core.download.segmented

import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

internal class BoundedAsyncFileWriter(
    scope: CoroutineScope,
    capacity: Int,
    private val writeAt: (position: Long, bytes: ByteArray) -> Unit,
    private val onBytesWritten: suspend (Long) -> Unit = {},
) {
    private val failure = AtomicReference<BoundedAsyncFileWriterException?>(null)
    private val writes: Channel<QueuedWrite>
    private val writerJob: kotlinx.coroutines.Job

    init {
        require(capacity > 0) { "capacity must be positive" }
        writes = Channel(capacity)
        writerJob = scope.launch {
            try {
                for (write in writes) {
                    writeAt(write.position, write.bytes)
                    onBytesWritten(write.bytes.size.toLong())
                }
            } catch (error: CancellationException) {
                writes.cancel(error)
                throw error
            } catch (error: Throwable) {
                val writeFailure = BoundedAsyncFileWriterException(error)
                failure.compareAndSet(null, writeFailure)
                writes.close(writeFailure)
            }
        }
    }

    suspend fun enqueue(
        position: Long,
        source: ByteArray,
        byteCount: Int,
    ) {
        require(position >= 0L) { "position cannot be negative" }
        require(byteCount in 0..source.size) { "byteCount is outside source bounds" }
        if (byteCount == 0) return
        throwStoredFailure()
        val write = QueuedWrite(
            position = position,
            bytes = source.copyOf(byteCount),
        )
        try {
            writes.send(write)
        } catch (error: Throwable) {
            failure.get()?.let { throw it }
            throw error
        }
        throwStoredFailure()
    }

    suspend fun closeAndJoin() {
        writes.close()
        writerJob.join()
        throwStoredFailure()
    }

    suspend fun cancelAndJoin() {
        writes.cancel()
        writerJob.cancelAndJoin()
    }

    private fun throwStoredFailure() {
        failure.get()?.let { throw it }
    }

    private data class QueuedWrite(
        val position: Long,
        val bytes: ByteArray,
    )
}

internal class BoundedAsyncFileWriterException(
    cause: Throwable,
) : IOException(
    "positioned file write failed: ${cause.message.orEmpty()}",
    cause,
)
