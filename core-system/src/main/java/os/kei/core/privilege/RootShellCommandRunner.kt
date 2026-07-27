package os.kei.core.privilege

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import os.kei.core.log.AppLogger
import os.kei.core.system.AppCommandResult
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

internal data class RootCommandOutputSnapshot(
    val stdout: String,
    val stderr: String,
)

/**
 * Runs one command per `su` invocation and mirrors the semantics of the Shizuku user service:
 * separate stdout/stderr, bounded capture with truncation flags, periodic snapshots, real exit
 * codes, and cancellation.
 *
 * The command text is fed to `su` over stdin instead of `su -c`, because AOSP's debug `su` parses
 * its first argument as a uid while Magisk-family builds expect a command there. Reading a script
 * from stdin is the one form every implementation agrees on, and it sidesteps shell quoting.
 *
 * Cancellation cannot go through [Process.destroy]: the child runs as uid 0 and the app process may
 * not signal it. Termination therefore spawns a second short-lived `su` that kills the recorded
 * process group.
 */
internal class RootShellCommandRunner {
    private val gate = Semaphore(MAX_CONCURRENT_COMMANDS)

    suspend fun execute(
        suPath: String,
        command: String,
        timeoutMs: Long,
        maxOutputBytes: Int,
        dispatcher: CoroutineDispatcher,
        onSnapshot: (suspend (RootCommandOutputSnapshot) -> Unit)? = null,
    ): AppCommandResult = coroutineScope {
        val normalizedCommand = command.trim()
        if (normalizedCommand.isBlank()) {
            return@coroutineScope AppCommandResult(
                stdout = "",
                stderr = "",
                exitCode = null,
                timedOut = false,
                cancelled = false,
            )
        }
        if (normalizedCommand.length > MAX_COMMAND_CHARS) {
            return@coroutineScope AppCommandResult(
                stdout = "",
                stderr = "Root command exceeds $MAX_COMMAND_CHARS characters",
                exitCode = null,
                timedOut = false,
                cancelled = false,
            )
        }

        val snapshots = onSnapshot?.let { Channel<RootCommandOutputSnapshot>(Channel.CONFLATED) }
        val snapshotJob =
            onSnapshot?.let { callback ->
                launch {
                    for (snapshot in requireNotNull(snapshots)) callback(snapshot)
                }
            }
        try {
            gate.withPermit {
                runInterruptible(dispatcher) {
                    runBlockingCommand(
                        suPath = suPath,
                        command = normalizedCommand,
                        timeoutMs = timeoutMs.coerceIn(1L, MAX_COMMAND_TIMEOUT_MS),
                        maxOutputBytes = maxOutputBytes.coerceIn(MIN_OUTPUT_BYTES, MAX_OUTPUT_BYTES_PER_STREAM),
                        publish = { snapshot -> snapshots?.trySend(snapshot) },
                    )
                }
            }
        } finally {
            snapshots?.close()
            withContext(NonCancellable) { snapshotJob?.join() }
        }
    }

    private fun runBlockingCommand(
        suPath: String,
        command: String,
        timeoutMs: Long,
        maxOutputBytes: Int,
        publish: (RootCommandOutputSnapshot) -> Unit,
    ): AppCommandResult {
        val stdout = BoundedOutput(maxOutputBytes)
        val stderr = BoundedOutput(maxOutputBytes)
        var process: Process? = null
        var rootPid: Int? = null
        var timedOut = false
        var cancelled = false
        var exitCode: Int? = null
        try {
            val started =
                runCatching { ProcessBuilder(suPath).start() }
                    .getOrElse { error ->
                        return AppCommandResult(
                            stdout = "",
                            stderr = error.message?.ifBlank { null } ?: error.javaClass.simpleName,
                            exitCode = null,
                            timedOut = false,
                            cancelled = false,
                        )
                    }
            process = started
            started.outputStream.use { stream ->
                stream.write(buildScript(command).toByteArray(Charsets.UTF_8))
                stream.flush()
            }

            val publisher = SnapshotPublisher(stdout, stderr, publish)
            val pidHandshake = started.inputStream.readPidMarker()
            rootPid = pidHandshake.pid
            if (pidHandshake.leftover.isNotEmpty()) {
                stdout.append(pidHandshake.leftover, pidHandshake.leftover.size)
            }
            val stdoutReader = streamReader(started.inputStream, stdout, publisher, "stdout")
            val stderrReader = streamReader(started.errorStream, stderr, publisher, "stderr")

            val completed = started.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (completed) {
                exitCode = started.exitValue()
            } else {
                timedOut = true
                terminateRoot(suPath, rootPid, started)
            }
            stdoutReader.join(READER_JOIN_TIMEOUT_MS)
            stderrReader.join(READER_JOIN_TIMEOUT_MS)
            publisher.publish(force = true)
        } catch (error: InterruptedException) {
            cancelled = true
            terminateRoot(suPath, rootPid, process)
            Thread.currentThread().interrupt()
        } catch (error: CancellationException) {
            terminateRoot(suPath, rootPid, process)
            throw error
        } catch (error: Throwable) {
            stderr.append(error.message?.ifBlank { null } ?: error.javaClass.simpleName)
        } finally {
            process?.closeStreams()
        }
        return AppCommandResult(
            stdout = stdout.text().trim(),
            stderr = stderr.text().trim(),
            exitCode = exitCode,
            timedOut = timedOut,
            cancelled = cancelled,
            stdoutTruncated = stdout.truncated,
            stderrTruncated = stderr.truncated,
        )
    }

    private fun streamReader(
        stream: InputStream,
        output: BoundedOutput,
        publisher: SnapshotPublisher,
        name: String,
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
            "KeiOS-RootCommand-$name",
        ).apply {
            isDaemon = true
            start()
        }

    private fun terminateRoot(suPath: String, rootPid: Int?, process: Process?) {
        if (rootPid != null) {
            runCatching {
                val killer = ProcessBuilder(suPath).redirectErrorStream(true).start()
                killer.outputStream.use { stream ->
                    stream.write(buildKillScript(rootPid).toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
                if (!killer.waitFor(KILL_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    killer.destroyForcibly()
                }
                killer.closeStreams()
            }.onFailure {
                AppLogger.w(TAG, "root terminate failed: ${it.javaClass.simpleName}")
            }
        }
        runCatching { process?.destroy() }
        runCatching { process?.destroyForcibly() }
    }

    private class SnapshotPublisher(
        private val stdout: BoundedOutput,
        private val stderr: BoundedOutput,
        private val publish: (RootCommandOutputSnapshot) -> Unit,
    ) {
        private var lastPublishedNanos = 0L

        @Synchronized
        fun publish(force: Boolean) {
            val now = System.nanoTime()
            if (!force && now - lastPublishedNanos < SNAPSHOT_INTERVAL_NANOS) return
            lastPublishedNanos = now
            runCatching {
                publish(RootCommandOutputSnapshot(stdout = stdout.text(), stderr = stderr.text()))
            }
        }
    }

    internal companion object {
        private const val TAG = "RootShellCommandRunner"
        const val PID_MARKER = "__KEIOS_ROOT_PID__:"
        private const val MAX_CONCURRENT_COMMANDS = 4
        private const val MAX_COMMAND_CHARS = 64 * 1024
        private const val MAX_COMMAND_TIMEOUT_MS = 10 * 60 * 1000L
        private const val MIN_OUTPUT_BYTES = 1_024
        private const val MAX_OUTPUT_BYTES_PER_STREAM = 192 * 1024
        private const val READER_JOIN_TIMEOUT_MS = 600L
        private const val KILL_TIMEOUT_MS = 2_000L
        private const val SNAPSHOT_INTERVAL_NANOS = 120_000_000L

        /**
         * The marker line lets the app learn the privileged shell's pid without touching the file
         * system, which `untrusted_app` cannot share with the `su` domain.
         */
        fun buildScript(command: String): String =
            buildString {
                append("printf '").append(PID_MARKER).append("%s\\n' \"$$\"\n")
                append(command)
                append('\n')
            }

        /**
         * Kills the process group first so descendants go down with the shell, then the direct
         * children and the shell itself for managers whose `su` keeps the caller's group.
         */
        fun buildKillScript(rootPid: Int): String =
            buildString {
                append("kill -9 -").append(rootPid).append(" 2>/dev/null\n")
                append("pkill -9 -P ").append(rootPid).append(" 2>/dev/null\n")
                append("kill -9 ").append(rootPid).append(" 2>/dev/null\n")
            }
    }
}

internal data class RootPidHandshake(
    val pid: Int?,
    val leftover: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is RootPidHandshake && pid == other.pid && leftover.contentEquals(other.leftover))

    override fun hashCode(): Int = 31 * (pid ?: 0) + leftover.contentHashCode()
}

/**
 * Consumes the pid marker the privileged shell prints before the command runs.
 *
 * A shell that never printed the marker keeps its bytes: they are returned as [RootPidHandshake.leftover]
 * so no output is lost.
 */
internal fun InputStream.readPidMarker(scanLimit: Int = 128): RootPidHandshake {
    val buffer = ByteArrayOutputStream()
    while (buffer.size() < scanLimit) {
        val next = runCatching { read() }.getOrDefault(-1)
        if (next < 0) break
        if (next == '\n'.code) {
            val line = buffer.toByteArray().toString(Charsets.UTF_8)
            val pid = line.trim().removePrefix(RootShellCommandRunner.PID_MARKER).trim().toIntOrNull()
            return if (line.trimStart().startsWith(RootShellCommandRunner.PID_MARKER) && pid != null) {
                RootPidHandshake(pid = pid, leftover = ByteArray(0))
            } else {
                RootPidHandshake(pid = null, leftover = (line + "\n").toByteArray(Charsets.UTF_8))
            }
        }
        buffer.write(next)
    }
    return RootPidHandshake(pid = null, leftover = buffer.toByteArray())
}

internal class BoundedOutput(private val maxBytes: Int) {
    private val output = ByteArrayOutputStream()
    private var capturedBytes = 0

    @Volatile
    var truncated: Boolean = false
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
        val bytes = value.toByteArray(Charsets.UTF_8)
        append(bytes, bytes.size)
    }

    @Synchronized
    fun text(): String = output.toByteArray().toString(Charsets.UTF_8)
}

private fun Process.closeStreams() {
    runCatching { outputStream.close() }
    runCatching { inputStream.close() }
    runCatching { errorStream.close() }
}
