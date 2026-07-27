package os.kei.core.privilege

import os.kei.core.log.AppLogger
import java.io.File
import java.util.concurrent.TimeUnit

/** Outcome of asking a `su` candidate who it runs as. */
internal sealed interface RootProbeOutcome {
    /** `su` ran and reported uid 0. */
    data class Granted(val suPath: String) : RootProbeOutcome

    /** `su` ran and reported some other uid, or the superuser manager refused the request. */
    data class Rejected(val uid: Int?, val message: String) : RootProbeOutcome

    /** The binary could not be launched at all. */
    data class Unavailable(val reason: String) : RootProbeOutcome
}

/**
 * Resolves the `su` entry point offered by the installed superuser manager.
 *
 * Magisk, KernelSU and APatch all publish a `su` binary and disagree about where it lives, and some
 * expose it only through `PATH`. The plain name is tried first so a manager that overlays `PATH`
 * wins, then the well-known absolute paths are probed in turn.
 */
internal object RootShellLocator {
    private const val TAG = "RootShellLocator"

    /** Passive checks must not stall a status refresh. */
    const val PASSIVE_PROBE_TIMEOUT_MS = 4_000L

    /** Interactive checks wait for the superuser manager's grant dialog. */
    const val INTERACTIVE_PROBE_TIMEOUT_MS = 60_000L

    private val ABSOLUTE_CANDIDATES =
        listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/debug_ramdisk/su",
            "/sbin/su",
            "/su/bin/su",
            "/data/adb/magisk/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/ap/bin/su",
        )

    /** True when at least one well-known `su` path exists on disk. */
    fun hasInstalledBinary(): Boolean =
        ABSOLUTE_CANDIDATES.any { path -> runCatching { File(path).exists() }.getOrDefault(false) }

    fun candidates(): List<String> =
        buildList {
            add("su")
            addAll(ABSOLUTE_CANDIDATES.filter { path -> runCatching { File(path).exists() }.getOrDefault(false) })
        }

    /**
     * Walks the candidate list until one reports uid 0.
     *
     * The first call on a device makes the superuser manager raise its grant dialog, so this doubles
     * as the access request. A rejection from any candidate outranks a launch failure, because it
     * means a manager is present and said no.
     */
    fun probe(timeoutMs: Long): RootProbeOutcome {
        var rejection: RootProbeOutcome.Rejected? = null
        candidates().forEach { candidate ->
            when (val outcome = probeCandidate(candidate, timeoutMs)) {
                is RootProbeOutcome.Granted -> return outcome
                is RootProbeOutcome.Rejected -> rejection = rejection ?: outcome
                is RootProbeOutcome.Unavailable -> Unit
            }
        }
        return rejection ?: RootProbeOutcome.Unavailable("su not launchable")
    }

    fun probeCandidate(suPath: String, timeoutMs: Long): RootProbeOutcome {
        val process =
            runCatching { ProcessBuilder(suPath).start() }
                .onFailure { AppLogger.d(TAG, "probe launch failed for $suPath: ${it.javaClass.simpleName}") }
                .getOrElse { error ->
                    return RootProbeOutcome.Unavailable(error.javaClass.simpleName)
                }
        return try {
            runCatching {
                process.outputStream.use { stream ->
                    stream.write(PROBE_SCRIPT.toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
            }
            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return RootProbeOutcome.Rejected(uid = null, message = "su probe timed out")
            }
            val stdout = runCatching { process.inputStream.readBytes().toString(Charsets.UTF_8) }.getOrDefault("")
            val stderr = runCatching { process.errorStream.readBytes().toString(Charsets.UTF_8) }.getOrDefault("")
            val uid = parseProbeUid(stdout)
            when (uid) {
                0 -> RootProbeOutcome.Granted(suPath)
                null -> RootProbeOutcome.Rejected(
                    uid = null,
                    message = stderr.lineSequence().map(String::trim).firstOrNull(String::isNotBlank)
                        ?: "su produced no uid",
                )

                else -> RootProbeOutcome.Rejected(uid = uid, message = "su reported uid $uid")
            }
        } catch (error: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            RootProbeOutcome.Unavailable(error.javaClass.simpleName)
        } finally {
            runCatching { process.inputStream.close() }
            runCatching { process.errorStream.close() }
        }
    }

    private const val PROBE_SCRIPT = "id -u\n"
}

internal fun parseProbeUid(output: String): Int? =
    output.lineSequence()
        .map(String::trim)
        .firstOrNull { line -> line.isNotEmpty() && line.all(Char::isDigit) }
        ?.toIntOrNull()
