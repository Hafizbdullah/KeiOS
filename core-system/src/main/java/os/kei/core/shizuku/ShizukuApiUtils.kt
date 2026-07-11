package os.kei.core.shizuku

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.core.shizuku.service.ShizukuUserServiceClient
import os.kei.core.system.AppBuildEnv
import os.kei.core.system.AppCommandResult
import rikka.shizuku.Shizuku
import java.util.Locale

class ShizukuApiUtils(
    private val requestCode: Int = DEFAULT_REQUEST_CODE,
    private val commandDispatcher: CoroutineDispatcher = AppDispatchers.osOperations,
) {
    data class AppCommandOutputSnapshot(
        val stdout: String,
        val stderr: String,
    ) {
        fun combinedOutput(): String = stdout.ifBlank { stderr }
    }

    private enum class CommandIdentity(val label: String) {
        ROOT("root"),
        SHELL("shell"),
        UNSUPPORTED("unsupported");

        val canRunCommand: Boolean
            get() = this == ROOT || this == SHELL

        companion object {
            fun fromUid(uid: Int?): CommandIdentity = when (uid) {
                0 -> ROOT
                2000 -> SHELL
                else -> UNSUPPORTED
            }
        }
    }

    private data class RuntimeState(
        val binderAlive: Boolean,
        val preV11: Boolean,
        val permissionGranted: Boolean,
        val serviceUid: Int?,
        val commandIdentity: CommandIdentity
    ) {
        val commandReady: Boolean =
            binderAlive && !preV11 && permissionGranted && commandIdentity.canRunCommand

        val statusText: String
            get() = when {
                !binderAlive -> "Shizuku service unavailable (start Shizuku app first)"
                preV11 -> "Shizuku pre-v11 is unsupported"
                !permissionGranted -> "Shizuku permission: not granted"
                commandIdentity.canRunCommand -> "Shizuku permission: granted (${commandIdentity.label})"
                else -> {
                    val uidText = serviceUid?.toString() ?: "unknown"
                    "Shizuku command unavailable: unsupported service uid $uidText"
                }
            }
    }

    private data class CachedRuntimeState(
        val state: RuntimeState,
        val capturedAtNanos: Long
    ) {
        fun isFresh(nowNanos: Long): Boolean {
            val age = nowNanos - capturedAtNanos
            return age in 0..RUNTIME_STATE_CACHE_TTL_NANOS
        }
    }

    private data class InteractiveCommandRewriteResult(
        val command: String,
        val adaptedTopOnce: Boolean = false
    )

    private data class UiDumpRewriteResult(
        val command: String,
        val redirectedPath: String?
    )


    private var statusCallback: ((String) -> Unit)? = null
    @Volatile
    private var attached = false
    private val listenerLock = Any()
    @Volatile
    private var cachedRuntimeState: CachedRuntimeState? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        invalidateRuntimeStateCache()
        publishStatus(currentStatus())
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        invalidateRuntimeStateCache()
        publishStatus("Shizuku service disconnected")
        ShizukuUserServiceClient.invalidate("binder-dead")
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { code, grantResult ->
        if (code != requestCode) return@OnRequestPermissionResultListener
        invalidateRuntimeStateCache()
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            publishStatus(currentStatus())
        } else {
            publishStatus("Shizuku permission: denied")
        }
    }

    fun attach(onStatusChanged: (String) -> Unit) {
        synchronized(listenerLock) {
            statusCallback = onStatusChanged
            if (!attached) {
                registerListeners().onFailure { error ->
                    publishStatus("Shizuku init failed: ${error.javaClass.simpleName}")
                }
            }
        }
        publishStatus(currentStatus())
    }

    fun detach() {
        synchronized(listenerLock) {
            statusCallback = null
            if (attached) {
                unregisterListeners()
            }
        }
    }

    private fun registerListeners(): Result<Unit> = runCatching {
        var binderReceivedRegistered = false
        var binderDeadRegistered = false
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            binderReceivedRegistered = true
            Shizuku.addBinderDeadListener(binderDeadListener)
            binderDeadRegistered = true
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            attached = true
        } catch (error: Throwable) {
            if (binderReceivedRegistered) {
                runCatching { Shizuku.removeBinderReceivedListener(binderReceivedListener) }
            }
            if (binderDeadRegistered) {
                runCatching { Shizuku.removeBinderDeadListener(binderDeadListener) }
            }
            runCatching { Shizuku.removeRequestPermissionResultListener(permissionResultListener) }
            attached = false
            throw error
        }
    }

    private fun unregisterListeners() {
        runCatching { Shizuku.removeBinderReceivedListener(binderReceivedListener) }
        runCatching { Shizuku.removeBinderDeadListener(binderDeadListener) }
        runCatching { Shizuku.removeRequestPermissionResultListener(permissionResultListener) }
        attached = false
    }

    fun requestPermissionIfNeeded() {
        runCatching {
            when {
                !Shizuku.pingBinder() -> publishStatus("Shizuku service unavailable (start Shizuku app first)")
                Shizuku.isPreV11() -> publishStatus("Shizuku pre-v11 is unsupported")
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                    publishStatus(currentStatus())
                }

                Shizuku.shouldShowRequestPermissionRationale() -> {
                    publishStatus("Shizuku permission denied permanently")
                }

                else -> {
                    publishStatus("Requesting Shizuku permission...")
                    Shizuku.requestPermission(requestCode)
                }
            }
        }.onFailure {
            publishStatus("Shizuku request failed: ${it.javaClass.simpleName}")
        }
    }

    fun currentStatus(): String {
        return resolveRuntimeState(forceRefresh = true).statusText
    }

    fun canUseCommand(): Boolean {
        return resolveRuntimeState().commandReady
    }

    suspend fun execCommandCancellableResult(
        command: String,
        timeoutMs: Long = 2000L
    ): AppCommandResult {
        val normalizedCommand = command.trim()
        if (normalizedCommand.isBlank()) {
            return AppCommandResult(
                stdout = "",
                stderr = "",
                exitCode = null,
                timedOut = false,
                cancelled = false
            )
        }
        val state = withContext(commandDispatcher) { resolveRuntimeState() }
        if (!state.commandReady) {
            return AppCommandResult(
                stdout = "",
                stderr = state.statusText,
                exitCode = null,
                timedOut = false,
                cancelled = false
            )
        }
        return ShizukuUserServiceClient.execute(
            command = prepareCommand(normalizedCommand),
            timeoutMs = timeoutMs,
            dispatcher = commandDispatcher,
        )
    }

    suspend fun execCommandCancellableStreaming(
        command: String,
        timeoutMs: Long = 2000L,
        onOutputSnapshot: suspend (String) -> Unit,
    ): String? {
        val result =
            execCommandCancellableResultStreaming(
                command = command,
                timeoutMs = timeoutMs,
                onOutputSnapshot = { snapshot ->
                    val output = snapshot.combinedOutput()
                    if (output.isNotBlank()) {
                        onOutputSnapshot(output)
                    }
                },
            )
        return shizukuCommandOutputOrNull(result)
    }

    suspend fun execCommandCancellableResultStreaming(
        command: String,
        timeoutMs: Long = 2000L,
        onOutputSnapshot: suspend (AppCommandOutputSnapshot) -> Unit,
    ): AppCommandResult {
        val normalizedCommand = command.trim()
        if (normalizedCommand.isBlank()) {
            return AppCommandResult(
                stdout = "",
                stderr = "",
                exitCode = null,
                timedOut = false,
                cancelled = false
            )
        }
        val state = withContext(commandDispatcher) { resolveRuntimeState() }
        if (!state.commandReady) {
            return AppCommandResult(
                stdout = "",
                stderr = state.statusText,
                exitCode = null,
                timedOut = false,
                cancelled = false
            )
        }
        return ShizukuUserServiceClient.execute(
            command = prepareCommand(normalizedCommand),
            timeoutMs = timeoutMs,
            dispatcher = commandDispatcher,
            onOutputSnapshot = { snapshot ->
                onOutputSnapshot(
                    AppCommandOutputSnapshot(
                        stdout = snapshot.stdout,
                        stderr = snapshot.stderr,
                    ),
                )
            },
        )
    }

    suspend fun execCommandCancellable(command: String, timeoutMs: Long = 2000L): String? {
        val result = execCommandCancellableResult(command = command, timeoutMs = timeoutMs)
        return shizukuCommandOutputOrNull(result)
    }

    private fun prepareCommand(command: String): String {
        val interactiveRewrite = rewriteInteractiveShellCommand(command)
        if (interactiveRewrite.adaptedTopOnce) {
            publishStatus("top command adapted: run once with -n 1")
        }
        val resolved = rewriteUiAutomatorDumpCommand(interactiveRewrite.command)
        if (!resolved.redirectedPath.isNullOrBlank()) {
            publishStatus("UI dump redirected: ${resolved.redirectedPath}")
        }
        return resolved.command
    }

    private fun rewriteInteractiveShellCommand(command: String): InteractiveCommandRewriteResult {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return InteractiveCommandRewriteResult(command = command)
        val normalized = trimmed.lowercase(Locale.ROOT)
        if (!normalized.startsWith("top")) {
            return InteractiveCommandRewriteResult(command = command)
        }
        val hasIterationCount = Regex("""(^|\s)-n(\s*\d+)?(\s|$)""").containsMatchIn(trimmed)
        if (hasIterationCount) {
            return InteractiveCommandRewriteResult(command = command)
        }
        return InteractiveCommandRewriteResult(
            command = "$trimmed -n 1",
            adaptedTopOnce = true
        )
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
        for (i in (dumpIndex + 1) until tokens.size) {
            val token = tokens[i]
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
            ?: "window_dump.xml"
        val safeName = sanitizeUiDumpFileName(requestedName)
        val targetDir = AppBuildEnv.uiDumpShellDirectory()
        val targetPath = "$targetDir/$safeName"
        val optionText = if (options.isEmpty()) "" else options.joinToString(prefix = " ", separator = " ")
        val rewritten = "mkdir -p \"$targetDir\" && uiautomator dump$optionText \"$targetPath\""
        return UiDumpRewriteResult(command = rewritten, redirectedPath = targetPath)
    }

    private fun sanitizeUiDumpFileName(raw: String): String {
        val cleaned = raw
            .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
            .trim('_')
        val withExt = if (cleaned.lowercase(Locale.ROOT).endsWith(".xml")) cleaned else "${cleaned}.xml"
        return withExt.ifBlank { "window_dump.xml" }.take(64)
    }

    suspend fun detailedRows(): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        val state = resolveRuntimeState()

        rows += "Shizuku Binder Alive" to state.binderAlive.toString()
        rows += "Shizuku Permission Granted" to state.permissionGranted.toString()
        rows += "Shizuku Activated" to state.commandReady.toString()
        rows += "Shizuku Command Identity" to state.commandIdentity.label
        rows += "Shizuku Command Backend" to "UserService"
        rows += "Shizuku Pre-v11" to state.preV11.toString()
        rows += "Shizuku Permission Rationale" to runCatching { Shizuku.shouldShowRequestPermissionRationale().toString() }.getOrDefault("unknown")
        state.serviceUid?.let { rows += "Shizuku Service UID" to it.toString() }

        runCatching { Shizuku.getVersion() }
            .getOrNull()
            ?.let { rows += "Shizuku Service Version" to it.toString() }
        runCatching { Shizuku.getServerPatchVersion() }
            .getOrNull()
            ?.let { rows += "Shizuku Server Patch Version" to it.toString() }
        runCatching { Shizuku.getSELinuxContext() }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
            ?.let { rows += "Shizuku SELinux Context" to it }
        runCatching { Shizuku.getLatestServiceVersion() }
            .getOrNull()
            ?.let { rows += "Shizuku Latest Service Version" to it.toString() }

        if (state.commandReady) rows += loadDetailedCommandRows()

        return rows.filter { it.first.isNotBlank() && it.second.isNotBlank() }
    }

    private suspend fun loadDetailedCommandRows(): List<Pair<String, String>> {
        val output =
            execCommandCancellable(
                command = DETAILED_PROBE_COMMAND,
                timeoutMs = DETAILED_PROBE_TIMEOUT_MS,
            ).orEmpty()
        return parseShizukuDetailedCommandRows(output)
    }

    private fun resolveRuntimeState(forceRefresh: Boolean = false): RuntimeState {
        val now = System.nanoTime()
        if (!forceRefresh) {
            cachedRuntimeState?.takeIf { it.isFresh(now) }?.let { return it.state }
        }
        val state = readRuntimeState()
        cachedRuntimeState = CachedRuntimeState(state = state, capturedAtNanos = now)
        return state
    }

    private fun readRuntimeState(): RuntimeState {
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!binderAlive) {
            return RuntimeState(
                binderAlive = false,
                preV11 = false,
                permissionGranted = false,
                serviceUid = null,
                commandIdentity = CommandIdentity.UNSUPPORTED
            )
        }

        val preV11 = runCatching { Shizuku.isPreV11() }.getOrDefault(true)
        if (preV11) {
            return RuntimeState(
                binderAlive = true,
                preV11 = true,
                permissionGranted = false,
                serviceUid = null,
                commandIdentity = CommandIdentity.UNSUPPORTED
            )
        }

        val permissionGranted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        val serviceUid = if (permissionGranted) {
            runCatching { Shizuku.getUid() }
                .onFailure {
                    AppLogger.w(TAG, "resolveRuntimeState getUid failed: ${it.javaClass.simpleName}")
                }
                .getOrNull()
        } else {
            null
        }
        val identity = CommandIdentity.fromUid(serviceUid)

        return RuntimeState(
            binderAlive = true,
            preV11 = false,
            permissionGranted = permissionGranted,
            serviceUid = serviceUid,
            commandIdentity = identity
        )
    }

    private fun invalidateRuntimeStateCache() {
        cachedRuntimeState = null
    }

    private fun publishStatus(message: String) {
        val callback = statusCallback ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback.publishStatusSafely(message)
        } else {
            mainHandler.post {
                if (statusCallback === callback) {
                    callback.publishStatusSafely(message)
                }
            }
        }
    }

    private fun ((String) -> Unit).publishStatusSafely(message: String) {
        runCatching { invoke(message) }
            .onFailure { error ->
                AppLogger.w(TAG, "Status callback failed: ${error.javaClass.simpleName}", error)
            }
    }

    companion object {
        private const val TAG = "ShizukuApiUtils"
        private const val RUNTIME_STATE_CACHE_TTL_NANOS = 750_000_000L
        private const val DETAILED_PROBE_PREFIX = "__keios_shizuku_"
        private const val DETAILED_PROBE_TIMEOUT_MS = 5_000L
        private val DETAILED_PROBE_COMMAND =
            listOf(
                "printf '${DETAILED_PROBE_PREFIX}id=%s\\n' \"\$(id 2>/dev/null | head -n 1)\"",
                "printf '${DETAILED_PROBE_PREFIX}whoami=%s\\n' \"\$(whoami 2>/dev/null | head -n 1)\"",
                "printf '${DETAILED_PROBE_PREFIX}uname=%s\\n' \"\$(uname -a 2>/dev/null | head -n 1)\"",
                "printf '${DETAILED_PROBE_PREFIX}getenforce=%s\\n' \"\$(getenforce 2>/dev/null | head -n 1)\"",
                "printf '${DETAILED_PROBE_PREFIX}process_count=%s\\n' \"\$(ps -A 2>/dev/null | wc -l)\"",
            ).joinToString(separator = "; ")
        const val DEFAULT_REQUEST_CODE = 1001
        const val API_VERSION = "13.1.5"

        fun isCommandReadyStatusText(status: String): Boolean =
            status.trim().startsWith("Shizuku permission: granted (", ignoreCase = true)
    }
}

internal fun parseShizukuDetailedCommandRows(output: String): List<Pair<String, String>> {
    val values =
        output.lineSequence().mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val key = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            key.takeIf { it.startsWith("__keios_shizuku_") && value.isNotBlank() }
                ?.removePrefix("__keios_shizuku_")
                ?.let { it to value }
        }.toMap()
    return buildList {
        values["id"]?.let { add("Shizuku id" to it) }
        values["whoami"]?.let { add("Shizuku whoami" to it) }
        values["uname"]?.let { add("Shizuku uname" to it) }
        values["getenforce"]?.let { add("Shizuku getenforce" to it) }
        values["process_count"]?.let { add("Shizuku process count" to it) }
    }
}

internal fun shizukuCommandOutputOrNull(result: AppCommandResult): String? =
    result.combinedOutput().ifBlank { null }
