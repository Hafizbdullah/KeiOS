package os.kei.core.shizuku.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.core.system.AppBuildEnv
import os.kei.core.system.AppCommandExecutor
import os.kei.core.system.AppCommandResult
import rikka.shizuku.Shizuku
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

internal object ShizukuUserServiceClient {
    data class OutputSnapshot(
        val stdout: String,
        val stderr: String,
    )

    private const val TAG = "ShizukuUserService"
    private const val SERVICE_TAG = "keios_shell_command_service"
    private const val SERVICE_BIND_TIMEOUT_MS = 8_000L
    private const val COMPLETION_GRACE_MS = 2_000L
    private const val MAX_COMMAND_TIMEOUT_MS = 10 * 60 * 1000L
    private const val MAX_COMMAND_CHARS = 64 * 1024
    private const val MAX_OUTPUT_BYTES_PER_STREAM = 192 * 1024

    private val lock = Any()
    @Volatile private var service: IShizukuCommandService? = null
    @Volatile private var serviceBinder: IBinder? = null
    @Volatile private var verifiedServiceBinder: IBinder? = null
    private var binding = false
    private var connectionWaiter: CompletableDeferred<IShizukuCommandService>? = null
    private val compatibilityMutex = Mutex()

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val connected = IShizukuCommandService.Stub.asInterface(binder)
                if (connected == null || !binder.isBinderAlive) {
                    invalidate("invalid-binder:${name.className}")
                    return
                }
                runCatching { binder.linkToDeath(serviceDeathRecipient, 0) }
                    .onFailure {
                        invalidate("link-to-death:${name.className}")
                        return
                    }
                synchronized(lock) {
                    serviceBinder?.unlinkDeathRecipientQuietly(serviceDeathRecipient)
                    service = connected
                    serviceBinder = binder
                    binding = false
                    connectionWaiter?.complete(connected)
                    connectionWaiter = null
                }
                AppLogger.i(TAG, "UserService connected: ${name.className}")
            }

            override fun onServiceDisconnected(name: ComponentName) {
                invalidate("disconnected:${name.className}")
            }
        }

    private val serviceDeathRecipient = IBinder.DeathRecipient {
        invalidate("user-service-binder-dead")
    }

    suspend fun execute(
        command: String,
        timeoutMs: Long,
        dispatcher: CoroutineDispatcher = AppDispatchers.osOperations,
        onOutputSnapshot: (suspend (OutputSnapshot) -> Unit)? = null,
    ): AppCommandResult = coroutineScope {
        if (command.length > MAX_COMMAND_CHARS) {
            return@coroutineScope AppCommandResult(
                stdout = "",
                stderr = "Shizuku command exceeds $MAX_COMMAND_CHARS characters",
                exitCode = null,
                timedOut = false,
                cancelled = false,
            )
        }
        val effectiveTimeoutMs = timeoutMs.coerceIn(1L, MAX_COMMAND_TIMEOUT_MS)
        val commandId = UUID.randomUUID().toString()
        val snapshotCallback = onOutputSnapshot
        val snapshots = snapshotCallback?.let { Channel<OutputSnapshot>(Channel.CONFLATED) }
        val snapshotJob =
            snapshotCallback?.let { callback ->
                launch {
                    for (snapshot in requireNotNull(snapshots)) callback(snapshot)
                }
            }
        val completion = CompletableDeferred<AppCommandResult>()
        val callback =
            object : IShizukuCommandCallback.Stub() {
                override fun onSnapshot(
                    stdout: String,
                    stderr: String,
                    stdoutTruncated: Boolean,
                    stderrTruncated: Boolean,
                ) {
                    snapshots?.trySend(OutputSnapshot(stdout = stdout, stderr = stderr))
                }

                override fun onCompleted(
                    stdout: String,
                    stderr: String,
                    exitCode: Int,
                    hasExitCode: Boolean,
                    timedOut: Boolean,
                    cancelled: Boolean,
                    stdoutTruncated: Boolean,
                    stderrTruncated: Boolean,
                ) {
                    snapshots?.trySend(OutputSnapshot(stdout = stdout, stderr = stderr))
                    completion.complete(
                        AppCommandResult(
                            stdout = stdout,
                            stderr = stderr,
                            exitCode = exitCode.takeIf { hasExitCode },
                            timedOut = timedOut,
                            cancelled = cancelled,
                            stdoutTruncated = stdoutTruncated,
                            stderrTruncated = stderrTruncated,
                        ),
                    )
                }
            }

        var connectedService: IShizukuCommandService? = null
        try {
            connectedService = awaitCompatibleService(dispatcher)
            withContext(dispatcher) {
                connectedService.execute(
                    commandId,
                    command,
                    effectiveTimeoutMs,
                    minOf(AppCommandExecutor.DEFAULT_MAX_OUTPUT_BYTES, MAX_OUTPUT_BYTES_PER_STREAM),
                    callback,
                )
            }
            withTimeoutOrNull(effectiveTimeoutMs + COMPLETION_GRACE_MS) {
                completion.await()
            } ?: AppCommandResult(
                stdout = "",
                stderr = "Shizuku UserService command timed out",
                exitCode = null,
                timedOut = true,
                cancelled = false,
            ).also {
                connectedService.cancelQuietly(commandId, dispatcher)
            }
        } catch (error: CancellationException) {
            connectedService?.cancelQuietly(commandId, dispatcher)
            throw error
        } catch (error: Throwable) {
            connectedService?.cancelQuietly(commandId, dispatcher)
            invalidate("execute:${error.javaClass.simpleName}")
            AppCommandResult(
                stdout = "",
                stderr = error.message?.ifBlank { null } ?: error.javaClass.simpleName,
                exitCode = null,
                timedOut = false,
                cancelled = false,
            )
        } finally {
            snapshots?.close()
            withContext(NonCancellable) { snapshotJob?.join() }
        }
    }

    fun invalidate(reason: String) {
        val waiter: CompletableDeferred<IShizukuCommandService>?
        val binder: IBinder?
        synchronized(lock) {
            binder = serviceBinder
            service = null
            serviceBinder = null
            verifiedServiceBinder = null
            binding = false
            waiter = connectionWaiter
            connectionWaiter = null
        }
        binder?.unlinkDeathRecipientQuietly(serviceDeathRecipient)
        waiter?.completeExceptionally(IllegalStateException("Shizuku UserService $reason"))
        AppLogger.w(TAG, "UserService invalidated: $reason")
    }

    private suspend fun awaitService(dispatcher: CoroutineDispatcher): IShizukuCommandService {
        service?.takeIf { it.asBinder().isBinderAlive }?.let { return it }
        var shouldBind = false
        val waiter =
            synchronized(lock) {
                service?.takeIf { it.asBinder().isBinderAlive }?.let { return it }
                connectionWaiter ?: CompletableDeferred<IShizukuCommandService>().also {
                    connectionWaiter = it
                }.also {
                    if (!binding) {
                        binding = true
                        shouldBind = true
                    }
                }
            }
        if (shouldBind) {
            runCatching {
                withContext(dispatcher) {
                    Shizuku.bindUserService(userServiceArgs(), connection)
                }
            }.onFailure { error ->
                synchronized(lock) {
                    binding = false
                    if (connectionWaiter === waiter) connectionWaiter = null
                }
                waiter.completeExceptionally(error)
            }
        }
        return withTimeoutOrNull(SERVICE_BIND_TIMEOUT_MS) { waiter.await() }
            ?: run {
                synchronized(lock) {
                    binding = false
                    if (connectionWaiter === waiter) connectionWaiter = null
                }
                waiter.cancel(CancellationException("Shizuku UserService bind timed out"))
                throw IllegalStateException("Shizuku UserService bind timed out")
            }
    }

    private suspend fun awaitCompatibleService(
        dispatcher: CoroutineDispatcher,
    ): IShizukuCommandService {
        val current = service
        if (
            current != null &&
            current.asBinder().isBinderAlive &&
            verifiedServiceBinder === current.asBinder()
        ) {
            return current
        }
        return compatibilityMutex.withLock {
            val candidate = awaitService(dispatcher)
            if (verifiedServiceBinder === candidate.asBinder()) return@withLock candidate
            val remoteVersion =
                runCatching {
                    withContext(dispatcher) { candidate.serviceVersion }
                }.getOrNull()
            if (remoteVersion == ShizukuUserServiceContract.VERSION) {
                verifiedServiceBinder = candidate.asBinder()
                return@withLock candidate
            }
            AppLogger.w(
                TAG,
                "Replacing incompatible UserService: remote=$remoteVersion expected=${ShizukuUserServiceContract.VERSION}",
            )
            withContext(dispatcher) {
                runCatching { candidate.destroy() }
                runCatching {
                    Shizuku.unbindUserService(userServiceArgs(), connection, true)
                }
            }
            invalidate("service-version:$remoteVersion")
            delay(SERVICE_REBIND_DELAY_MS)
            val rebound = awaitService(dispatcher)
            val reboundVersion = withContext(dispatcher) { rebound.serviceVersion }
            check(reboundVersion == ShizukuUserServiceContract.VERSION) {
                "Shizuku UserService version mismatch: $reboundVersion"
            }
            verifiedServiceBinder = rebound.asBinder()
            rebound
        }
    }

    private fun userServiceArgs(): Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(
                AppBuildEnv.applicationId,
                KeiShizukuUserService::class.java.name,
            ),
        ).daemon(false)
            .tag(SERVICE_TAG)
            .version(ShizukuUserServiceContract.VERSION)
            .debuggable(AppBuildEnv.isDebugBuild)
            .processNameSuffix("shizuku_shell")

    private const val SERVICE_REBIND_DELAY_MS = 250L
}

private suspend fun IShizukuCommandService.cancelQuietly(
    commandId: String,
    dispatcher: CoroutineDispatcher,
) {
    runCatching { withContext(NonCancellable + dispatcher) { cancel(commandId) } }
}

private fun IBinder.unlinkDeathRecipientQuietly(recipient: IBinder.DeathRecipient) {
    runCatching { unlinkToDeath(recipient, 0) }
}
