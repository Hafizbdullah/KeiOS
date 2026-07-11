package os.kei.core.shizuku.service

import android.content.Context
import android.os.IBinder
import android.system.Os
import android.system.OsConstants
import androidx.annotation.Keep
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

class KeiShizukuUserService() : IShizukuCommandService.Stub() {
    private val threadIndex = AtomicInteger(0)
    private val executor: ExecutorService =
        ThreadPoolExecutor(
            MAX_CONCURRENT_COMMANDS,
            MAX_CONCURRENT_COMMANDS,
            30L,
            TimeUnit.SECONDS,
            ArrayBlockingQueue(MAX_QUEUED_COMMANDS),
            { runnable ->
                Thread(
                    runnable,
                    "KeiOS-ShizukuCommand-${threadIndex.incrementAndGet()}",
                ).apply { isDaemon = true }
            },
            ThreadPoolExecutor.AbortPolicy(),
        ).apply { allowCoreThreadTimeOut(true) }
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
        if (normalizedCommand.length > MAX_COMMAND_CHARS) {
            callback.safeComplete(
                stdout = "",
                stderr = "Shizuku command exceeds $MAX_COMMAND_CHARS characters",
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
            try {
                executor.submit {
                    runCommand(
                        commandId = normalizedId,
                        command = normalizedCommand,
                        timeoutMs = timeoutMs.coerceIn(1L, MAX_COMMAND_TIMEOUT_MS),
                        maxOutputBytes = maxOutputBytes.coerceIn(1, MAX_OUTPUT_BYTES_PER_STREAM),
                        running = running,
                    )
                }
            } catch (_: RejectedExecutionException) {
                commands.remove(normalizedId, running)
                running.unlinkDeathRecipient()
                running.completed.set(true)
                callback.safeComplete(
                    stdout = "",
                    stderr = "Shizuku command capacity reached",
                    exitCode = null,
                    timedOut = false,
                    cancelled = false,
                    stdoutTruncated = false,
                    stderrTruncated = false,
                )
                return
            }
        running.future = future
        if (running.cancelled.get()) {
            future.cancel(true)
        }
    }

    override fun cancel(commandId: String) {
        commands.remove(commandId)?.cancel()
    }

    override fun getServiceVersion(): Int = ShizukuUserServiceContract.VERSION

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
            val startedCommand = startCommand(commandId, command)
            process = startedCommand.process
            running.process = process
            running.processPid = startedCommand.pid
            if (running.cancelled.get()) {
                cancelled = true
                process.terminateTree(startedCommand.pid)
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
                process.terminateTree(startedCommand.pid)
            }
            stdoutReader.join(READER_JOIN_TIMEOUT_MS)
            stderrReader.join(READER_JOIN_TIMEOUT_MS)
            publisher.publish(force = true)
        } catch (_: InterruptedException) {
            cancelled = true
            process?.terminateTree(running.processPid)
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
        @Volatile var processPid: Int? = null
        @Volatile var future: Future<*>? = null
        @Volatile var deathRecipient: IBinder.DeathRecipient? = null

        fun cancel() {
            cancelled.set(true)
            process?.terminateTree(processPid)
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
        const val MAX_CONCURRENT_COMMANDS = 4
        const val MAX_QUEUED_COMMANDS = 24
        const val MAX_COMMAND_CHARS = 64 * 1024
        const val MAX_COMMAND_TIMEOUT_MS = 10 * 60 * 1000L
        const val MAX_OUTPUT_BYTES_PER_STREAM = 192 * 1024
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

private fun startCommand(commandId: String, command: String): StartedCommand {
    val pidFile = File("/data/local/tmp/.keios_command_$commandId.pid")
    runCatching { pidFile.delete() }
    val process =
        ProcessBuilder(
            "sh",
            "-c",
            "printf '%s' \"\$\$\" > \"\$0\"; exec sh -c \"\$1\"",
            pidFile.absolutePath,
            command,
        ).start()
    return try {
        val pid =
            repeatUntilNotNull(PID_CAPTURE_ATTEMPTS) {
                pidFile.readTextOrNull()?.trim()?.toIntOrNull()?.takeIf { it > 0 }
            }
        StartedCommand(process = process, pid = pid)
    } catch (error: InterruptedException) {
        runCatching { process.destroyForcibly() }
        throw error
    } finally {
        runCatching { pidFile.delete() }
    }
}

private fun Process.terminateTree(rootPid: Int?) {
    rootPid?.descendants()
        ?.asReversed()
        ?.forEach { childPid -> childPid.signalQuietly(OsConstants.SIGTERM) }
    rootPid?.signalQuietly(OsConstants.SIGTERM)
    runCatching { destroy() }
    runCatching {
        if (!waitFor(250L, TimeUnit.MILLISECONDS)) {
            rootPid?.descendants()
                ?.asReversed()
                ?.forEach { childPid -> childPid.signalQuietly(OsConstants.SIGKILL) }
            rootPid?.signalQuietly(OsConstants.SIGKILL)
            destroyForcibly()
        }
    }
}

private inline fun <T> repeatUntilNotNull(attempts: Int, block: () -> T?): T? {
    repeat(attempts) {
        block()?.let { return it }
        Thread.sleep(PID_CAPTURE_RETRY_MS)
    }
    return null
}

private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()

private fun Int.descendants(): List<Int> {
    val childPidsByParent =
        runCatching {
            val output =
                ProcessBuilder("ps", "-A", "-o", "PID,PPID")
                .start()
                .also { it.outputStream.closeQuietly() }
                .inputStream
                .bufferedReader()
                .use { it.readText() }
            parseProcessParentMap(output)
        }.getOrDefault(emptyMap())
    return collectDescendantPids(this, childPidsByParent, MAX_DESCENDANT_PIDS)
}

internal fun parseProcessParentMap(output: String): Map<Int, List<Int>> =
    output.lineSequence()
        .mapNotNull { line ->
            val columns = line.trim().split(Regex("\\s+"))
            if (columns.size < 2) return@mapNotNull null
            val pid = columns[0].toIntOrNull() ?: return@mapNotNull null
            val parentPid = columns[1].toIntOrNull() ?: return@mapNotNull null
            parentPid to pid
        }.groupBy(
            keySelector = Pair<Int, Int>::first,
            valueTransform = Pair<Int, Int>::second,
        )

internal fun collectDescendantPids(
    rootPid: Int,
    childPidsByParent: Map<Int, List<Int>>,
    limit: Int,
): List<Int> {
    val discovered = LinkedHashSet<Int>()
    val visited = hashSetOf(rootPid)
    val pending = ArrayDeque<Int>()
    pending.add(rootPid)
    while (pending.isNotEmpty() && discovered.size < limit.coerceAtLeast(0)) {
        val parentPid = pending.removeFirst()
        childPidsByParent[parentPid]
            .orEmpty()
            .asSequence()
            .filter { visited.add(it) }
            .forEach { childPid ->
                if (discovered.size < limit.coerceAtLeast(0)) {
                    discovered += childPid
                    pending.addLast(childPid)
                }
            }
    }
    return discovered.toList()
}

private fun Int.signalQuietly(signal: Int) {
    runCatching { Os.kill(this, signal) }
}

private fun Process.closeStreams() {
    outputStream.closeQuietly()
    inputStream.closeQuietly()
    errorStream.closeQuietly()
}

private fun AutoCloseable.closeQuietly() {
    runCatching { close() }
}

private const val MAX_DESCENDANT_PIDS = 256
private const val PID_CAPTURE_ATTEMPTS = 20
private const val PID_CAPTURE_RETRY_MS = 5L

private data class StartedCommand(
    val process: Process,
    val pid: Int?,
)
