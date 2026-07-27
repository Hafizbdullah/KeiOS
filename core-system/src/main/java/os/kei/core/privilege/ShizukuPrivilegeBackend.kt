package os.kei.core.privilege

import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.log.AppLogger
import os.kei.core.shizuku.service.ShizukuUserServiceClient
import os.kei.core.system.AppCommandResult
import rikka.shizuku.Shizuku

/**
 * Runs privileged work through the Shizuku server.
 *
 * Commands go to a user service the server hosts under its own uid, so the app never spawns a
 * privileged process itself. The server also hands out binders whose transactions execute under
 * that uid, which is what the package installer and the uid firewall depend on.
 */
internal class ShizukuPrivilegeBackend(
    private val requestCode: Int,
    private val commandDispatcher: CoroutineDispatcher,
) : PrivilegeBackend {

    private enum class CommandIdentity(val label: String) {
        ROOT("root"),
        SHELL("shell"),
        UNSUPPORTED("unsupported"),
        ;

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
        val commandIdentity: CommandIdentity,
    ) {
        val commandReady: Boolean =
            binderAlive && !preV11 && permissionGranted && commandIdentity.canRunCommand

        val status: PrivilegeStatus
            get() {
                val code = when {
                    !binderAlive -> PrivilegeStatusCode.ServiceUnavailable
                    preV11 -> PrivilegeStatusCode.PreV11Unsupported
                    !permissionGranted -> PrivilegeStatusCode.PermissionNotGranted
                    commandIdentity.canRunCommand -> PrivilegeStatusCode.Ready
                    else -> PrivilegeStatusCode.UnsupportedIdentity
                }
                val detail = when (code) {
                    PrivilegeStatusCode.Ready -> commandIdentity.label
                    PrivilegeStatusCode.UnsupportedIdentity -> serviceUid?.toString() ?: "unknown"
                    else -> ""
                }
                return PrivilegeStatus(mode = PrivilegeMode.Shizuku, code = code, detail = detail)
            }
    }

    private data class CachedRuntimeState(
        val state: RuntimeState,
        val capturedAtNanos: Long,
    ) {
        fun isFresh(nowNanos: Long): Boolean {
            val age = nowNanos - capturedAtNanos
            return age in 0..RUNTIME_STATE_CACHE_TTL_NANOS
        }
    }

    override val mode: PrivilegeMode = PrivilegeMode.Shizuku

    override val capabilities: Set<PrivilegeCapability> =
        setOf(
            PrivilegeCapability.ShellCommand,
            PrivilegeCapability.BinderPackageInstaller,
            PrivilegeCapability.BinderUidFirewall,
        )

    @Volatile
    private var statusCallback: ((PrivilegeStatus) -> Unit)? = null

    @Volatile
    private var attached = false

    @Volatile
    private var cachedRuntimeState: CachedRuntimeState? = null

    private val listenerLock = Any()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        invalidateRuntimeStateCache()
        publish(currentStatus())
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        invalidateRuntimeStateCache()
        publish(PrivilegeStatus(PrivilegeMode.Shizuku, PrivilegeStatusCode.ServiceDisconnected))
        ShizukuUserServiceClient.invalidate("binder-dead")
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { code, grantResult ->
        if (code != requestCode) return@OnRequestPermissionResultListener
        invalidateRuntimeStateCache()
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            publish(currentStatus())
        } else {
            publish(PrivilegeStatus(PrivilegeMode.Shizuku, PrivilegeStatusCode.PermissionDenied))
        }
    }

    override fun attach(onStatusChanged: (PrivilegeStatus) -> Unit) {
        synchronized(listenerLock) {
            statusCallback = onStatusChanged
            if (!attached) {
                registerListeners().onFailure { error ->
                    publish(
                        PrivilegeStatus(
                            mode = PrivilegeMode.Shizuku,
                            code = PrivilegeStatusCode.InitFailed,
                            detail = error.javaClass.simpleName,
                        ),
                    )
                }
            }
        }
    }

    override fun detach() {
        synchronized(listenerLock) {
            statusCallback = null
            if (attached) {
                unregisterListeners()
            }
        }
    }

    override fun requestAccess() {
        runCatching {
            when {
                !Shizuku.pingBinder() ->
                    publish(PrivilegeStatus(PrivilegeMode.Shizuku, PrivilegeStatusCode.ServiceUnavailable))

                Shizuku.isPreV11() ->
                    publish(PrivilegeStatus(PrivilegeMode.Shizuku, PrivilegeStatusCode.PreV11Unsupported))

                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                    publish(currentStatus(forceRefresh = true))

                Shizuku.shouldShowRequestPermissionRationale() ->
                    publish(PrivilegeStatus(PrivilegeMode.Shizuku, PrivilegeStatusCode.PermissionBlocked))

                else -> {
                    publish(PrivilegeStatus(PrivilegeMode.Shizuku, PrivilegeStatusCode.RequestingPermission))
                    Shizuku.requestPermission(requestCode)
                }
            }
        }.onFailure { error ->
            publish(
                PrivilegeStatus(
                    mode = PrivilegeMode.Shizuku,
                    code = PrivilegeStatusCode.RequestFailed,
                    detail = error.javaClass.simpleName,
                ),
            )
        }
    }

    override fun currentStatus(forceRefresh: Boolean): PrivilegeStatus =
        resolveRuntimeState(forceRefresh).status

    override fun canUseCommand(): Boolean = resolveRuntimeState(forceRefresh = false).commandReady

    override suspend fun execute(
        command: String,
        timeoutMs: Long,
        onSnapshot: (suspend (PrivilegeCommandOutputSnapshot) -> Unit)?,
    ): AppCommandResult {
        val state = withContext(commandDispatcher) { resolveRuntimeState(forceRefresh = false) }
        if (!state.commandReady) {
            return AppCommandResult(
                stdout = "",
                stderr = state.status.text,
                exitCode = null,
                timedOut = false,
                cancelled = false,
            )
        }
        return ShizukuUserServiceClient.execute(
            command = command,
            timeoutMs = timeoutMs,
            dispatcher = commandDispatcher,
            onOutputSnapshot = onSnapshot?.let { callback ->
                { snapshot ->
                    callback(
                        PrivilegeCommandOutputSnapshot(
                            stdout = snapshot.stdout,
                            stderr = snapshot.stderr,
                        ),
                    )
                }
            },
        )
    }

    override suspend fun diagnosticRows(): List<Pair<String, String>> {
        val state = resolveRuntimeState(forceRefresh = false)
        val rows = mutableListOf<Pair<String, String>>()
        rows += "Shizuku Binder Alive" to state.binderAlive.toString()
        rows += "Shizuku Permission Granted" to state.permissionGranted.toString()
        rows += "Shizuku Activated" to state.commandReady.toString()
        rows += "Shizuku Command Identity" to state.commandIdentity.label
        rows += "Shizuku Command Backend" to "UserService"
        rows += "Shizuku Pre-v11" to state.preV11.toString()
        rows += "Shizuku Permission Rationale" to
            runCatching { Shizuku.shouldShowRequestPermissionRationale().toString() }.getOrDefault("unknown")
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
        return rows
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

    private fun resolveRuntimeState(forceRefresh: Boolean): RuntimeState {
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
                commandIdentity = CommandIdentity.UNSUPPORTED,
            )
        }

        val preV11 = runCatching { Shizuku.isPreV11() }.getOrDefault(true)
        if (preV11) {
            return RuntimeState(
                binderAlive = true,
                preV11 = true,
                permissionGranted = false,
                serviceUid = null,
                commandIdentity = CommandIdentity.UNSUPPORTED,
            )
        }

        val permissionGranted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        val serviceUid = if (permissionGranted) {
            runCatching { Shizuku.getUid() }
                .onFailure { AppLogger.w(TAG, "resolveRuntimeState getUid failed: ${it.javaClass.simpleName}") }
                .getOrNull()
        } else {
            null
        }

        return RuntimeState(
            binderAlive = true,
            preV11 = false,
            permissionGranted = permissionGranted,
            serviceUid = serviceUid,
            commandIdentity = CommandIdentity.fromUid(serviceUid),
        )
    }

    private fun invalidateRuntimeStateCache() {
        cachedRuntimeState = null
    }

    private fun publish(status: PrivilegeStatus) {
        statusCallback?.invoke(status)
    }

    private companion object {
        const val TAG = "ShizukuPrivilegeBackend"
        const val RUNTIME_STATE_CACHE_TTL_NANOS = 750_000_000L
    }
}
