package os.kei.mcp.notification

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.core.prefs.UiPrefs
import os.kei.core.privilege.PrivilegedShell
import os.kei.core.shizuku.ShizukuConnectivityBridge
import kotlin.time.Duration.Companion.milliseconds

internal data class McpXiaomiMagicDispatchEnvironment(
    val canPostNotifications: () -> Boolean,
    val resolveTargetUid: () -> Int?,
    val canUseCommand: () -> Boolean,
    val shouldExecute: suspend () -> Boolean,
    val healNetworking: suspend () -> Unit,
    val blockNetworking: suspend () -> Boolean,
    val postNotification: (Context, NotificationManagerCompat, Int, Notification) -> Boolean,
    val awaitRestore: suspend () -> Unit,
    val restoreNetworking: suspend () -> Unit,
    val needsRestore: () -> Boolean,
)

internal object McpXiaomiMagicDispatcher {
    private const val TAG = "McpXiaomiMagic"
    private const val XMSF_PACKAGE_NAME = "com.xiaomi.xmsf"
    private const val CANCELLATION_JOIN_TIMEOUT_MS = 6_000L
    private const val RESTORE_DELAY_TIMEOUT_MS = 1_000L
    private const val RESTORE_COMMAND_TIMEOUT_MS = 1_500L

    private enum class CommandSet {
        DIRECT_UID_FIREWALL,
        PACKAGE_NETWORKING,
        UID_FIREWALL,
        NONE
    }

    private val privilegedShell = PrivilegedShell()
    private val scope = CoroutineScope(SupervisorJob() + AppDispatchers.mcpServer)
    private val networkMutex = Mutex()

    @Volatile
    private var commandSet: CommandSet? = null

    @Volatile
    private var isXmsfNetworkBlocked = false

    @Volatile
    private var isUidFirewallChainEnabled = false

    fun canUseCommand(): Boolean {
        return privilegedShell.canUseCommand()
    }

    suspend fun notify(
        context: Context,
        notificationId: Int,
        notification: Notification,
        environment: McpXiaomiMagicDispatchEnvironment = productionEnvironment(context),
        dispatchScope: CoroutineScope = scope,
        mutex: Mutex = networkMutex,
        onDelivered: suspend () -> Unit = {},
    ): Boolean {
        if (!environment.canPostNotifications()) return false
        val notificationManager = NotificationManagerCompat.from(context)
        val initialPostResult = CompletableDeferred<Boolean>()
        val dispatchJob = launchDispatchLifecycle(
            context = context,
            notificationManager = notificationManager,
            notificationId = notificationId,
            notification = notification,
            environment = environment,
            dispatchScope = dispatchScope,
            mutex = mutex,
            initialPostResult = initialPostResult,
            onDelivered = onDelivered,
        )
        return try {
            initialPostResult.await()
        } catch (cancellation: CancellationException) {
            if (!initialPostResult.isCompleted) {
                dispatchJob.cancel(cancellation)
            }
            val joined =
                withContext(NonCancellable) {
                    withTimeoutOrNull(CANCELLATION_JOIN_TIMEOUT_MS) {
                        dispatchJob.join()
                        true
                    } ?: false
                }
            if (!joined) {
                AppLogger.e(TAG, "Xiaomi magic cancellation cleanup timed out")
            }
            throw cancellation
        }
    }

    fun enqueue(
        context: Context,
        notificationId: Int,
        notification: Notification,
    ): Boolean {
        val environment = productionEnvironment(context)
        if (!environment.canPostNotifications()) return false
        val notificationManager = NotificationManagerCompat.from(context)
        val targetUid = environment.resolveTargetUid()
        AppLogger.i(TAG, "enqueue: targetUid=$targetUid notifId=$notificationId")
        if (targetUid == null) {
            AppLogger.w(TAG, "skip Xiaomi magic: xmsf uid is null")
            return environment.postNotification(
                context,
                notificationManager,
                notificationId,
                notification,
            )
        }

        launchDispatchLifecycle(
            context = context,
            notificationManager = notificationManager,
            notificationId = notificationId,
            notification = notification,
            environment = environment,
            dispatchScope = scope,
            mutex = networkMutex,
            initialPostResult = CompletableDeferred(),
            onDelivered = {},
        )
        return true
    }

    private fun launchDispatchLifecycle(
        context: Context,
        notificationManager: NotificationManagerCompat,
        notificationId: Int,
        notification: Notification,
        environment: McpXiaomiMagicDispatchEnvironment,
        dispatchScope: CoroutineScope,
        mutex: Mutex,
        initialPostResult: CompletableDeferred<Boolean>,
        onDelivered: suspend () -> Unit,
    ): Job =
        dispatchScope.launch {
            mutex.withLock {
                runDispatchLifecycle(
                    context = context,
                    notificationManager = notificationManager,
                    notificationId = notificationId,
                    notification = notification,
                    environment = environment,
                    initialPostResult = initialPostResult,
                    onDelivered = onDelivered,
                )
            }
        }

    private suspend fun runDispatchLifecycle(
        context: Context,
        notificationManager: NotificationManagerCompat,
        notificationId: Int,
        notification: Notification,
        environment: McpXiaomiMagicDispatchEnvironment,
        initialPostResult: CompletableDeferred<Boolean>,
        onDelivered: suspend () -> Unit,
    ) {
        var networkTouched = false
        var restorationHandled = false
        try {
            val targetUid = environment.resolveTargetUid()
            AppLogger.i(TAG, "notify: targetUid=$targetUid notifId=$notificationId")
            if (targetUid == null) {
                AppLogger.w(TAG, "skip Xiaomi magic: xmsf uid is null")
                completeInitialPostAtomically(
                    context = context,
                    notificationManager = notificationManager,
                    notificationId = notificationId,
                    notification = notification,
                    environment = environment,
                    initialPostResult = initialPostResult,
                    retryOnFailure = false,
                    onDelivered = onDelivered,
                    restoreAfterPost = false,
                    onRestorationHandled = {},
                )
                return
            }
            if (!environment.shouldExecute()) {
                AppLogger.w(TAG, "skip Xiaomi magic: preconditions not satisfied")
                if (environment.canUseCommand()) {
                    environment.healNetworking()
                }
                completeInitialPostAtomically(
                    context = context,
                    notificationManager = notificationManager,
                    notificationId = notificationId,
                    notification = notification,
                    environment = environment,
                    initialPostResult = initialPostResult,
                    retryOnFailure = false,
                    onDelivered = onDelivered,
                    restoreAfterPost = false,
                    onRestorationHandled = {},
                )
                return
            }

            environment.healNetworking()
            AppLogger.i(TAG, "blocking xmsf network")
            networkTouched = environment.blockNetworking()
            completeInitialPostAtomically(
                context = context,
                notificationManager = notificationManager,
                notificationId = notificationId,
                notification = notification,
                environment = environment,
                initialPostResult = initialPostResult,
                retryOnFailure = true,
                onDelivered = onDelivered,
                restoreAfterPost = networkTouched || environment.needsRestore(),
                onRestorationHandled = { restorationHandled = true },
            )
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                initialPostResult.completeExceptionally(throwable)
                throw throwable
            }
            AppLogger.e(TAG, "Xiaomi magic execution failed", throwable)
            if (!initialPostResult.isCompleted) {
                completeInitialPostAtomically(
                    context = context,
                    notificationManager = notificationManager,
                    notificationId = notificationId,
                    notification = notification,
                    environment = environment,
                    initialPostResult = initialPostResult,
                    retryOnFailure = false,
                    onDelivered = onDelivered,
                    restoreAfterPost = networkTouched || environment.needsRestore(),
                    onRestorationHandled = { restorationHandled = true },
                )
            }
        } finally {
            withContext(NonCancellable) {
                if (!restorationHandled && (networkTouched || environment.needsRestore())) {
                    restoreNetworkSafely(environment)
                }
            }
        }
    }

    private suspend fun completeInitialPostAtomically(
        context: Context,
        notificationManager: NotificationManagerCompat,
        notificationId: Int,
        notification: Notification,
        environment: McpXiaomiMagicDispatchEnvironment,
        initialPostResult: CompletableDeferred<Boolean>,
        retryOnFailure: Boolean,
        onDelivered: suspend () -> Unit,
        restoreAfterPost: Boolean,
        onRestorationHandled: () -> Unit,
    ) {
        currentCoroutineContext().ensureActive()
        withContext(NonCancellable) {
            val primaryResult =
                postNotificationSafely(
                    context = context,
                    notificationManager = notificationManager,
                    notificationId = notificationId,
                    notification = notification,
                    environment = environment,
                )
            val delivered =
                primaryResult ||
                    retryOnFailure &&
                    postNotificationSafely(
                        context = context,
                        notificationManager = notificationManager,
                        notificationId = notificationId,
                        notification = notification,
                        environment = environment,
                    )
            if (!delivered) {
                initialPostResult.complete(false)
                return@withContext
            }

            val restorationJob =
                if (restoreAfterPost) {
                    launch {
                        try {
                            val delayCompleted =
                                runCatching {
                                    withTimeoutOrNull(RESTORE_DELAY_TIMEOUT_MS) {
                                        environment.awaitRestore()
                                        true
                                    } ?: false
                                }.getOrElse { throwable ->
                                    AppLogger.e(TAG, "Xiaomi magic restore delay failed", throwable)
                                    false
                                }
                            if (!delayCompleted) {
                                AppLogger.e(TAG, "Xiaomi magic restore delay timed out")
                            }
                        } finally {
                            restoreNetworkSafely(environment)
                            onRestorationHandled()
                        }
                    }
                } else {
                    null
                }

            var commitFailure: Throwable? = null
            try {
                onDelivered()
            } catch (throwable: Throwable) {
                commitFailure = throwable
            }
            if (commitFailure == null) {
                initialPostResult.complete(true)
            } else {
                initialPostResult.completeExceptionally(commitFailure)
            }
            restorationJob?.join()
            commitFailure?.let { throw it }
        }
    }

    private suspend fun restoreNetworkSafely(environment: McpXiaomiMagicDispatchEnvironment) {
        AppLogger.i(TAG, "restoring xmsf network")
        val restored =
            runCatching {
                withTimeoutOrNull(RESTORE_COMMAND_TIMEOUT_MS) {
                    environment.restoreNetworking()
                    true
                } ?: false
            }.getOrElse {
                AppLogger.e(TAG, "Xiaomi magic network restoration failed", it)
                false
            }
        if (!restored) {
            AppLogger.e(TAG, "Xiaomi magic network restoration timed out")
        }
        val healed =
            runCatching {
                withTimeoutOrNull(RESTORE_COMMAND_TIMEOUT_MS) {
                    environment.healNetworking()
                    true
                } ?: false
            }.getOrElse {
                AppLogger.e(TAG, "Xiaomi magic network healing failed", it)
                false
            }
        if (!healed) {
            AppLogger.e(TAG, "Xiaomi magic network healing timed out")
        }
    }

    private fun postNotificationSafely(
        context: Context,
        notificationManager: NotificationManagerCompat,
        notificationId: Int,
        notification: Notification,
        environment: McpXiaomiMagicDispatchEnvironment,
    ): Boolean =
        runCatching {
            environment.postNotification(
                context,
                notificationManager,
                notificationId,
                notification,
            )
        }.getOrElse { throwable ->
            AppLogger.e(TAG, "Xiaomi magic notification post failed", throwable)
            false
        }

    private fun productionEnvironment(context: Context): McpXiaomiMagicDispatchEnvironment {
        val targetUid by lazy { resolveXmsfUid(context) }
        return McpXiaomiMagicDispatchEnvironment(
            canPostNotifications = { McpNotificationHelper.canPostNotifications(context) },
            resolveTargetUid = { targetUid },
            canUseCommand = ::canUseCommand,
            shouldExecute = { targetUid?.let { uid -> shouldExecuteLocked(uid) } ?: false },
            healNetworking = { targetUid?.let { healXmsfNetworkingLocked(it) } },
            blockNetworking = {
                targetUid?.let { uid ->
                    blockXmsfNetworkingLocked(uid)
                    isXmsfNetworkBlocked || isUidFirewallChainEnabled
                } ?: false
            },
            postNotification = McpNotificationHelper::notifySafely,
            awaitRestore = { delay(resolveBlockIntervalMs().milliseconds) },
            restoreNetworking = { targetUid?.let { restoreXmsfNetworkingLocked(it) } },
            needsRestore = { isXmsfNetworkBlocked || isUidFirewallChainEnabled },
        )
    }

    fun restoreNetworkIfNeeded(context: Context) {
        val xmsfUid = resolveXmsfUid(context)
        scope.launch {
            networkMutex.withLock {
                if (xmsfUid != null && canUseCommand()) {
                    healXmsfNetworkingLocked(xmsfUid)
                }
            }
        }
    }

    private fun resolveXmsfUid(context: Context): Int? {
        return runCatching {
            context.packageManager.getPackageUid(XMSF_PACKAGE_NAME, PackageManager.PackageInfoFlags.of(0))
        }.getOrNull()?.takeIf { it > 0 }
    }

    private suspend fun shouldExecuteLocked(xmsfUid: Int): Boolean {
        if (!canUseCommand()) {
            AppLogger.w(TAG, "shouldExecute=false: Shizuku command unavailable")
            return false
        }
        val mode = resolveCommandSet()
        val canUseMode = mode != CommandSet.NONE
        AppLogger.i(TAG, "Shizuku mode=$mode, allowMagic=$canUseMode")
        return canUseMode
    }

    private suspend fun blockXmsfNetworkingLocked(uid: Int) {
        val mode = resolveCommandSet()
        when (mode) {
            CommandSet.DIRECT_UID_FIREWALL -> {
                val blocked = ShizukuConnectivityBridge.setUidNetworkingEnabled(
                    uid = uid,
                    enabled = false
                )
                isXmsfNetworkBlocked = blocked
                isUidFirewallChainEnabled = blocked
                AppLogger.i(TAG, "blockXmsfNetworkingLocked(direct): uid=$uid blocked=$blocked")
                if (!blocked) {
                    commandSet = null
                    blockXmsfNetworkingWithShellLocked(uid, resolveShellCommandSet())
                }
            }

            else -> blockXmsfNetworkingWithShellLocked(uid, mode)
        }
    }

    private suspend fun restoreXmsfNetworkingLocked(uid: Int) {
        val mode = resolveCommandSet()
        val restored = when (mode) {
            CommandSet.DIRECT_UID_FIREWALL -> {
                if (isXmsfNetworkBlocked) {
                    ShizukuConnectivityBridge.setUidNetworkingEnabled(uid = uid, enabled = true)
                } else {
                    true
                }
            }

            else -> restoreXmsfNetworkingWithShellLocked(uid, mode)
        }.let { restored ->
            if (!restored && mode == CommandSet.DIRECT_UID_FIREWALL) {
                commandSet = null
                restoreXmsfNetworkingWithShellLocked(uid, resolveShellCommandSet())
            } else {
                restored
            }
        }
        AppLogger.i(TAG, "restoreXmsfNetworkingLocked: uid=$uid mode=$mode restored=$restored")
        isXmsfNetworkBlocked = false
        isUidFirewallChainEnabled = false
    }

    private suspend fun healXmsfNetworkingLocked(uid: Int) {
        val mode = resolveCommandSet()
        when (mode) {
            CommandSet.DIRECT_UID_FIREWALL -> {
                val restored = ShizukuConnectivityBridge.setUidNetworkingEnabled(
                    uid = uid,
                    enabled = true
                )
                AppLogger.i(TAG, "healXmsfNetworkingLocked(direct): uid=$uid restored=$restored")
                if (!restored) {
                    commandSet = null
                    healXmsfNetworkingWithShellLocked(uid, resolveShellCommandSet())
                }
            }

            else -> healXmsfNetworkingWithShellLocked(uid, mode)
        }
        isXmsfNetworkBlocked = false
        isUidFirewallChainEnabled = false
    }

    private suspend fun blockXmsfNetworkingWithShellLocked(uid: Int, mode: CommandSet) {
        when (mode) {
            CommandSet.PACKAGE_NETWORKING -> {
                val blocked =
                    execCommand("cmd connectivity set-package-networking-enabled false $XMSF_PACKAGE_NAME")
                isXmsfNetworkBlocked = blocked
                AppLogger.i(TAG, "blockXmsfNetworkingLocked(package): uid=$uid blocked=$blocked")
            }

            CommandSet.UID_FIREWALL -> {
                val chainEnabled = execCommand("cmd connectivity set-firewall-chain-enabled 9 true")
                val blocked = execCommand("cmd connectivity set-uid-firewall-rule 9 $uid 2")
                isXmsfNetworkBlocked = blocked
                isUidFirewallChainEnabled = chainEnabled
                AppLogger.i(
                    TAG,
                    "blockXmsfNetworkingLocked(uid): uid=$uid chainEnabled=$chainEnabled blocked=$blocked"
                )
            }

            else -> {
                isXmsfNetworkBlocked = false
                isUidFirewallChainEnabled = false
                AppLogger.w(
                    TAG,
                    "blockXmsfNetworkingLocked skipped: no supported connectivity command"
                )
            }
        }
    }

    private suspend fun restoreXmsfNetworkingWithShellLocked(uid: Int, mode: CommandSet): Boolean {
        return when (mode) {
            CommandSet.PACKAGE_NETWORKING -> {
                if (isXmsfNetworkBlocked) {
                    execCommand("cmd connectivity set-package-networking-enabled true $XMSF_PACKAGE_NAME")
                } else {
                    true
                }
            }

            CommandSet.UID_FIREWALL -> {
                if (isXmsfNetworkBlocked) {
                    execCommand("cmd connectivity set-uid-firewall-rule 9 $uid 0")
                } else {
                    true
                }
            }

            else -> false
        }
    }

    private suspend fun healXmsfNetworkingWithShellLocked(uid: Int, mode: CommandSet) {
        when (mode) {
            CommandSet.PACKAGE_NETWORKING -> {
                val restored =
                    execCommand("cmd connectivity set-package-networking-enabled true $XMSF_PACKAGE_NAME")
                AppLogger.i(TAG, "healXmsfNetworkingLocked(package): uid=$uid restored=$restored")
            }

            CommandSet.UID_FIREWALL -> {
                val ruleRestored = execCommand("cmd connectivity set-uid-firewall-rule 9 $uid 0")
                AppLogger.i(
                    TAG,
                    "healXmsfNetworkingLocked(uid): uid=$uid ruleRestored=$ruleRestored"
                )
            }

            else -> {
                AppLogger.w(
                    TAG,
                    "healXmsfNetworkingLocked skipped: no supported connectivity command"
                )
            }
        }
    }

    private suspend fun resolveCommandSet(): CommandSet {
        commandSet?.takeIf { it != CommandSet.NONE }?.let { return it }
        if (ShizukuConnectivityBridge.canUseUidFirewall()) {
            commandSet = CommandSet.DIRECT_UID_FIREWALL
            AppLogger.i(TAG, "resolved Xiaomi magic command set: ${CommandSet.DIRECT_UID_FIREWALL}")
            return CommandSet.DIRECT_UID_FIREWALL
        }
        return resolveShellCommandSet()
    }

    private suspend fun resolveShellCommandSet(): CommandSet {
        val helpText = privilegedShell.execCommandCancellable("cmd connectivity help")
        if (helpText.isNullOrBlank()) {
            AppLogger.w(TAG, "resolveCommandSet skipped: connectivity help unavailable")
            return CommandSet.NONE
        }
        val resolved = when {
            helpText.contains("set-package-networking-enabled") -> CommandSet.PACKAGE_NETWORKING
            helpText.contains("set-firewall-chain-enabled") &&
                    helpText.contains("set-uid-firewall-rule") -> CommandSet.UID_FIREWALL

            else -> CommandSet.NONE
        }
        commandSet = resolved
        AppLogger.i(TAG, "resolved Xiaomi magic command set: $resolved")
        return resolved
    }

    private suspend fun execCommand(command: String): Boolean {
        val output =
            privilegedShell.execCommandCancellable("($command) >/dev/null 2>&1 && echo __OK__ || echo __FAIL__")
                ?: return false
        val success = output.contains("__OK__")
        if (!success) {
            AppLogger.w(TAG, "magic command failed: $command; output=$output")
        }
        return success
    }

    private fun resolveBlockIntervalMs(): Long {
        return UiPrefs.getSuperIslandRestoreDelayMs().toLong()
    }
}
