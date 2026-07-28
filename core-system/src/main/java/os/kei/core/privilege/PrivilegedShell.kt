package os.kei.core.privilege

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.core.system.AppBuildEnv
import os.kei.core.system.AppCommandResult
import java.util.Locale

/**
 * Single entry point for privileged shell work.
 *
 * The instance routes to whichever backend [PrivilegeModeRuntime] currently selects and re-publishes
 * status when the user switches modes, so callers hold one object for the lifetime of their screen
 * and never learn which backend served them.
 */
class PrivilegedShell(
    private val requestCode: Int = DEFAULT_REQUEST_CODE,
    private val commandDispatcher: CoroutineDispatcher = AppDispatchers.osOperations,
) {
    data class CommandOutputSnapshot(
        val stdout: String,
        val stderr: String,
    ) {
        fun combinedOutput(): String = stdout.ifBlank { stderr }
    }

    private data class InteractiveCommandRewriteResult(
        val command: String,
        val adaptedTopOnce: Boolean = false,
    )

    private data class UiDumpRewriteResult(
        val command: String,
        val redirectedPath: String?,
    )

    private val backendLock = Any()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private var shizukuBackend: PrivilegeBackend? = null
    private var rootBackend: PrivilegeBackend? = null

    @Volatile
    private var attachedBackend: PrivilegeBackend? = null

    @Volatile
    private var statusCallback: ((PrivilegeStatus) -> Unit)? = null

    private val modeListener: (PrivilegeMode) -> Unit = { onModeChanged() }

    val activeMode: PrivilegeMode
        get() = PrivilegeModeRuntime.mode

    val capabilities: Set<PrivilegeCapability>
        get() = backend().capabilities

    fun supports(capability: PrivilegeCapability): Boolean = capability in capabilities

    fun attach(onStatusChanged: (PrivilegeStatus) -> Unit) {
        statusCallback = onStatusChanged
        PrivilegeModeRuntime.addListener(modeListener)
        val active = backend()
        synchronized(backendLock) {
            if (attachedBackend !== active) {
                attachedBackend?.detach()
                attachedBackend = active
            }
        }
        active.attach(::publishStatus)
        publishStatus(active.currentStatus(forceRefresh = true))
    }

    fun detach() {
        PrivilegeModeRuntime.removeListener(modeListener)
        statusCallback = null
        synchronized(backendLock) {
            attachedBackend?.detach()
            attachedBackend = null
        }
    }

    fun requestAccessIfNeeded() {
        backend().requestAccess()
    }

    fun currentStatus(): PrivilegeStatus = backend().currentStatus(forceRefresh = true)

    fun canUseCommand(): Boolean = backend().canUseCommand()

    suspend fun execCommandCancellableResult(
        command: String,
        timeoutMs: Long = DEFAULT_COMMAND_TIMEOUT_MS,
    ): AppCommandResult {
        val normalizedCommand = command.trim()
        if (normalizedCommand.isBlank()) return blankResult()
        return backend().execute(
            command = prepareCommand(normalizedCommand),
            timeoutMs = timeoutMs,
        )
    }

    suspend fun execCommandCancellableResultStreaming(
        command: String,
        timeoutMs: Long = DEFAULT_COMMAND_TIMEOUT_MS,
        onOutputSnapshot: suspend (CommandOutputSnapshot) -> Unit,
    ): AppCommandResult {
        val normalizedCommand = command.trim()
        if (normalizedCommand.isBlank()) return blankResult()
        return backend().execute(
            command = prepareCommand(normalizedCommand),
            timeoutMs = timeoutMs,
            onSnapshot = { snapshot ->
                onOutputSnapshot(
                    CommandOutputSnapshot(stdout = snapshot.stdout, stderr = snapshot.stderr),
                )
            },
        )
    }

    suspend fun execCommandCancellableStreaming(
        command: String,
        timeoutMs: Long = DEFAULT_COMMAND_TIMEOUT_MS,
        onOutputSnapshot: suspend (String) -> Unit,
    ): String? {
        val result =
            execCommandCancellableResultStreaming(
                command = command,
                timeoutMs = timeoutMs,
                onOutputSnapshot = { snapshot ->
                    val output = snapshot.combinedOutput()
                    if (output.isNotBlank()) onOutputSnapshot(output)
                },
            )
        return privilegedCommandOutputOrNull(result)
    }

    suspend fun execCommandCancellable(
        command: String,
        timeoutMs: Long = DEFAULT_COMMAND_TIMEOUT_MS,
    ): String? {
        val result = execCommandCancellableResult(command = command, timeoutMs = timeoutMs)
        return privilegedCommandOutputOrNull(result)
    }

    suspend fun detailedRows(): List<Pair<String, String>> {
        val active = backend()
        val rows = mutableListOf<Pair<String, String>>()
        rows += "Privilege Mode" to active.mode.storageId
        rows += active.diagnosticRows()
        if (active.canUseCommand()) {
            val output =
                execCommandCancellable(
                    command = DETAILED_PROBE_COMMAND,
                    timeoutMs = DETAILED_PROBE_TIMEOUT_MS,
                ).orEmpty()
            rows += parsePrivilegeDetailedCommandRows(output)
        }
        return rows.filter { it.first.isNotBlank() && it.second.isNotBlank() }
    }

    private fun backend(): PrivilegeBackend =
        when (PrivilegeModeRuntime.mode) {
            PrivilegeMode.Disabled -> DisabledPrivilegeBackend

            PrivilegeMode.Shizuku -> synchronized(backendLock) {
                shizukuBackend ?: ShizukuPrivilegeBackend(
                    requestCode = requestCode,
                    commandDispatcher = commandDispatcher,
                ).also { shizukuBackend = it }
            }

            PrivilegeMode.Root -> synchronized(backendLock) {
                rootBackend ?: RootPrivilegeBackend(
                    commandDispatcher = commandDispatcher,
                ).also { rootBackend = it }
            }
        }

    private fun onModeChanged() {
        if (statusCallback == null) return
        val active = backend()
        synchronized(backendLock) {
            if (attachedBackend === active) return@synchronized
            attachedBackend?.detach()
            attachedBackend = active
            active.attach(::publishStatus)
        }
        publishStatus(active.currentStatus(forceRefresh = true))
    }

    private fun prepareCommand(command: String): String {
        val interactiveRewrite = rewriteInteractiveShellCommand(command)
        if (interactiveRewrite.adaptedTopOnce) {
            publishNotice(TOP_ADAPTED_NOTICE)
        }
        val resolved = rewriteUiAutomatorDumpCommand(interactiveRewrite.command)
        if (!resolved.redirectedPath.isNullOrBlank()) {
            publishNotice("UI dump redirected: ${resolved.redirectedPath}")
        }
        return resolved.command
    }

    private fun rewriteInteractiveShellCommand(command: String): InteractiveCommandRewriteResult {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return InteractiveCommandRewriteResult(command = command)
        if (!trimmed.lowercase(Locale.ROOT).startsWith("top")) {
            return InteractiveCommandRewriteResult(command = command)
        }
        val hasIterationCount = Regex("""(^|\s)-n(\s*\d+)?(\s|$)""").containsMatchIn(trimmed)
        if (hasIterationCount) return InteractiveCommandRewriteResult(command = command)
        return InteractiveCommandRewriteResult(command = "$trimmed -n 1", adaptedTopOnce = true)
    }

    private fun rewriteUiAutomatorDumpCommand(command: String): UiDumpRewriteResult {
        val normalized = command.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return UiDumpRewriteResult(command = command, redirectedPath = null)
        if (!normalized.contains("uiautomator dump")) {
            return UiDumpRewriteResult(command = command, redirectedPath = null)
        }
        val tokens = command.trim().split(Regex("\\s+"))
        val uiIndex = tokens.indexOfFirst { it.equals("uiautomator", ignoreCase = true) }
        val dumpIndex = uiIndex + 1
        if (uiIndex < 0 || dumpIndex >= tokens.size || !tokens[dumpIndex].equals("dump", ignoreCase = true)) {
            return UiDumpRewriteResult(command = command, redirectedPath = null)
        }

        val options = mutableListOf<String>()
        var rawOutputPath: String? = null
        for (index in (dumpIndex + 1) until tokens.size) {
            val token = tokens[index]
            if (token.startsWith("-")) {
                options += token
                continue
            }
            rawOutputPath = token
            break
        }

        val requestedName = rawOutputPath
            ?.trim('"', '\'')
            ?.substringAfterLast('/')
            ?.ifBlank { null }
            ?: DEFAULT_UI_DUMP_FILE_NAME
        val safeName = sanitizeUiDumpFileName(requestedName)
        val targetDir = AppBuildEnv.uiDumpShellDirectory()
        val targetPath = "$targetDir/$safeName"
        val optionText = if (options.isEmpty()) "" else options.joinToString(prefix = " ", separator = " ")
        val rewritten = "mkdir -p \"$targetDir\" && uiautomator dump$optionText \"$targetPath\""
        return UiDumpRewriteResult(command = rewritten, redirectedPath = targetPath)
    }

    private fun sanitizeUiDumpFileName(raw: String): String {
        val cleaned = raw.replace(Regex("""[^A-Za-z0-9._-]"""), "_").trim('_')
        val withExt =
            if (cleaned.lowercase(Locale.ROOT).endsWith(".xml")) cleaned else "$cleaned.xml"
        return withExt.ifBlank { DEFAULT_UI_DUMP_FILE_NAME }.take(64)
    }

    private fun publishNotice(message: String) {
        publishStatus(
            PrivilegeStatus(
                mode = PrivilegeModeRuntime.mode,
                code = PrivilegeStatusCode.Notice,
                detail = message,
            ),
        )
    }

    private fun publishStatus(status: PrivilegeStatus) {
        val callback = statusCallback ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback.invokeSafely(status)
        } else {
            mainHandler.post {
                if (statusCallback === callback) callback.invokeSafely(status)
            }
        }
    }

    private fun ((PrivilegeStatus) -> Unit).invokeSafely(status: PrivilegeStatus) {
        runCatching { invoke(status) }
            .onFailure { error ->
                AppLogger.w(TAG, "Status callback failed: ${error.javaClass.simpleName}", error)
            }
    }

    private fun blankResult(): AppCommandResult =
        AppCommandResult(
            stdout = "",
            stderr = "",
            exitCode = null,
            timedOut = false,
            cancelled = false,
        )

    companion object {
        private const val TAG = "PrivilegedShell"
        private const val DETAILED_PROBE_PREFIX = "__keios_privilege_"
        private const val DETAILED_PROBE_TIMEOUT_MS = 5_000L
        private const val DEFAULT_UI_DUMP_FILE_NAME = "window_dump.xml"

        const val DEFAULT_REQUEST_CODE = 1001
        const val DEFAULT_COMMAND_TIMEOUT_MS = 2_000L
        const val SHIZUKU_API_VERSION = "13.1.5"
        const val TOP_ADAPTED_NOTICE = "top command adapted: run once with -n 1"

        private val DETAILED_PROBE_COMMAND =
            listOf(
                "printf '${DETAILED_PROBE_PREFIX}id=%s\\n' \"\$(id 2>/dev/null | head -n 1)\"",
                "printf '${DETAILED_PROBE_PREFIX}whoami=%s\\n' \"\$(whoami 2>/dev/null | head -n 1)\"",
                "printf '${DETAILED_PROBE_PREFIX}uname=%s\\n' \"\$(uname -a 2>/dev/null | head -n 1)\"",
                "printf '${DETAILED_PROBE_PREFIX}getenforce=%s\\n' \"\$(getenforce 2>/dev/null | head -n 1)\"",
                "printf '${DETAILED_PROBE_PREFIX}process_count=%s\\n' \"\$(ps -A 2>/dev/null | wc -l)\"",
            ).joinToString(separator = "; ")
    }
}

internal fun parsePrivilegeDetailedCommandRows(output: String): List<Pair<String, String>> {
    val values =
        output.lineSequence().mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val key = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            key.takeIf { it.startsWith("__keios_privilege_") && value.isNotBlank() }
                ?.removePrefix("__keios_privilege_")
                ?.let { it to value }
        }.toMap()
    return buildList {
        values["id"]?.let { add(PRIVILEGE_ROW_ID to it) }
        values["whoami"]?.let { add(PRIVILEGE_ROW_WHOAMI to it) }
        values["uname"]?.let { add(PRIVILEGE_ROW_UNAME to it) }
        values["getenforce"]?.let { add(PRIVILEGE_ROW_GETENFORCE to it) }
        values["process_count"]?.let { add(PRIVILEGE_ROW_PROCESS_COUNT to it) }
    }
}

const val PRIVILEGE_ROW_ID = "Privilege id"
const val PRIVILEGE_ROW_WHOAMI = "Privilege whoami"
const val PRIVILEGE_ROW_UNAME = "Privilege uname"
const val PRIVILEGE_ROW_GETENFORCE = "Privilege getenforce"
const val PRIVILEGE_ROW_PROCESS_COUNT = "Privilege process count"

internal fun privilegedCommandOutputOrNull(result: AppCommandResult): String? =
    result.combinedOutput().ifBlank { null }
