package os.kei.core.shizuku.service

import android.content.Context
import android.os.IBinder
import androidx.annotation.Keep
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class KeiShizukuUserService() : IShizukuCommandService.Stub() {
    private val executor: ExecutorService =
        Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "KeiOS-ShizukuUserService").apply { isDaemon = true }
        }
    private val commands = ConcurrentHashMap<String, RunningCommand>()

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : this()

    override fun execute(
        commandId: String,
        command: String,
        timeoutMs: Long,
        maxOutputBytes: Int,
        callback: IShizukuCommandCallback,
    ) {
        val normalizedId = commandId.trim()
        val normalizedCommand = command.trim()
        if (normalizedId.isBlank() || normalizedCommand.isBlank()) {
            callback.safeComplete(
                stdout = "",
                stderr = "",
                exitCode = null,
                timedOut = false,
                cancelled = false,
                stdoutTruncated = false,
                stderrTruncated = false,
            )
            return
        }
        cancel(normalizedId)
        val running = RunningCommand(callback)
        commands[normalizedId] = running
        val deathRecipient = IBinder.DeathRecipient { cancel(normalizedId) }
        running.deathRecipient = deathRecipient
        runCatching {
            callback.asBinder().linkToDeath(deathRecipient, 0)
        }.onFailure {
            commands.remove(normalizedId, running)
            return
        }
        val future =
            executor.submit {
                runCommand(
                    commandId = normalizedId,
                    command = normalizedCommand,
                    timeoutMs = timeoutMs.coerceAtLeast(1L),
                    maxOutputBytes = maxOutputBytes.coerceAtLeast(1),
                    running = running,
                )
            }
        running.future = future
        if (running.cancelled.get()) {
            future.cancel(true)
        }
    }

    override fun cancel(commandId: String) {
        commands.remove(commandId)?.cancel()
    }

    override fun destroy() {
        commands.values.forEach(RunningCommand::cancel)
        commands.clear()
        executor.shutdownNow()
        exitProcess(0)
    }

    private fun runCommand(
        commandId: String,
        command: String,
        timeoutMs: Long,
        maxOutputBytes: Int,
        running: RunningCommand,
    ) {
        val stdout = BoundedOutput(maxOutputBytes)
        val stderr = BoundedOutput(maxOutputBytes)
        var process: Process? = null
        var timedOut = false
        var cancelled = false
        var exitCode: Int? = null
        try {
            if (running.cancelled.get()) {
                cancelled = true
                return
            }
            process = ProcessBuilder("sh", "-c", command).start()
            running.process = process
            if (running.cancelled.get()) {
                cancelled = true
                process.terminate()
                return
            }
            process.outputStream.closeQuietly()
            val publisher = SnapshotPublisher(running.callback, stdout, stderr)
            val stdoutReader = streamReader(process.inputStream, stdout, publisher)
            val stderrReader = streamReader(process.errorStream, stderr, publisher)
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (completed) {
                exitCode = process.exitValue()
            } else {
                timedOut = true
                process.terminate()
            }
            stdoutReader.join(READER_JOIN_TIMEOUT_MS)
            stderrReader.join(READER_JOIN_TIMEOUT_MS)
            publisher.publish(force = true)
        } catch (_: InterruptedException) {
            cancelled = true
            process?.terminate()
            Thread.currentThread().interrupt()
        } catch (error: Throwable) {
            stderr.append(error.message?.ifBlank { null } ?: error.javaClass.simpleName)
        } finally {
            cancelled = cancelled || running.cancelled.get()
            process?.closeStreams()
            if (running.completed.compareAndSet(false, true)) {
                running.callback.safeComplete(
                    stdout = stdout.text().trim(),
                    stderr = stderr.text().trim(),
                    exitCode = exitCode,
                    timedOut = timedOut,
                    cancelled = cancelled,
                    stdoutTruncated = stdout.truncated,
                    stderrTruncated = stderr.truncated,
                )
            }
            commands.remove(commandId, running)
            running.unlinkDeathRecipient()
        }
    }

    private fun streamReader(
        stream: InputStream,
        output: BoundedOutput,
        publisher: SnapshotPublisher,
    ): Thread =
        Thread(
            {
                runCatching {
                    stream.use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (!Thread.currentThread().isInterrupted) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.append(buffer, read)
                            publisher.publish(force = false)
                        }
                    }
                }
            },
            "KeiOS-ShizukuUserService-Output",
        ).apply {
            isDaemon = true
            start()
        }

    private class RunningCommand(
        val callback: IShizukuCommandCallback,
    ) {
        val cancelled = AtomicBoolean(false)
        val completed = AtomicBoolean(false)
        @Volatile var process: Process? = null
        @Volatile var future: Future<*>? = null
        @Volatile var deathRecipient: IBinder.DeathRecipient? = null

        fun cancel() {
            cancelled.set(true)
            process?.terminate()
            future?.cancel(true)
            unlinkDeathRecipient()
        }

        fun unlinkDeathRecipient() {
            deathRecipient?.let { recipient ->
                runCatching { callback.asBinder().unlinkToDeath(recipient, 0) }
            }
            deathRecipient = null
        }
    }

    private class SnapshotPublisher(
        private val callback: IShizukuCommandCallback,
        private val stdout: BoundedOutput,
        private val stderr: BoundedOutput,
    ) {
        private var lastPublishedNanos = 0L

        @Synchronized
        fun publish(force: Boolean) {
            val now = System.nanoTime()
            if (!force && now - lastPublishedNanos < SNAPSHOT_INTERVAL_NANOS) return
            lastPublishedNanos = now
            runCatching {
                callback.onSnapshot(
                    stdout.text(),
                    stderr.text(),
                    stdout.truncated,
                    stderr.truncated,
                )
            }
        }
    }

    private class BoundedOutput(private val maxBytes: Int) {
        private val output = ByteArrayOutputStream()
        private var capturedBytes = 0
        @Volatile var truncated: Boolean = false
            private set

        @Synchronized
        fun append(buffer: ByteArray, length: Int) {
            val remaining = maxBytes - capturedBytes
            if (remaining > 0) {
                val accepted = minOf(length, remaining)
                output.write(buffer, 0, accepted)
                capturedBytes += accepted
            }
            if (length > remaining) truncated = true
        }

        @Synchronized
        fun append(value: String) {
            val bytes = value.toByteArray()
            append(bytes, bytes.size)
        }

        @Synchronized
        fun text(): String = output.toByteArray().toString(Charsets.UTF_8)
    }

    private companion object {
        const val READER_JOIN_TIMEOUT_MS = 600L
        const val SNAPSHOT_INTERVAL_NANOS = 120_000_000L
    }
}

private fun IShizukuCommandCallback.safeComplete(
    stdout: String,
    stderr: String,
    exitCode: Int?,
    timedOut: Boolean,
    cancelled: Boolean,
    stdoutTruncated: Boolean,
    stderrTruncated: Boolean,
) {
    runCatching {
        onCompleted(
            stdout,
            stderr,
            exitCode ?: 0,
            exitCode != null,
            timedOut,
            cancelled,
            stdoutTruncated,
            stderrTruncated,
        )
    }
}

private fun Process.terminate() {
    runCatching { destroy() }
    runCatching {
        if (!waitFor(250L, TimeUnit.MILLISECONDS)) destroyForcibly()
    }
}

private fun Process.closeStreams() {
    outputStream.closeQuietly()
    inputStream.closeQuietly()
    errorStream.closeQuietly()
}

private fun AutoCloseable.closeQuietly() {
    runCatching { close() }
}
