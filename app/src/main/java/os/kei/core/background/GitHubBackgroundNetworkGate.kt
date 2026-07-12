package os.kei.core.background

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.delay
import os.kei.core.log.AppLogger

internal data class GitHubBackgroundNetworkState(
    val present: Boolean,
    val internet: Boolean,
    val validated: Boolean,
    val notSuspended: Boolean,
) {
    val ready: Boolean
        get() = present && internet && validated && notSuspended
}

internal class GitHubBackgroundNetworkGate(
    private val stateProvider: () -> GitHubBackgroundNetworkState,
    private val resolveHost: (String) -> Boolean,
    private val delayBetweenAttempts: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun awaitReady(
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS,
    ): GitHubBackgroundNetworkGateResult {
        val attempts = maxAttempts.coerceAtLeast(1)
        var lastState: GitHubBackgroundNetworkState? = null
        var lastFailedHost = ""
        repeat(attempts) { index ->
            val currentState = stateProvider()
            lastState = currentState
            if (currentState.ready) {
                lastFailedHost = REQUIRED_HOSTS.firstOrNull { host -> !resolveHost(host) }.orEmpty()
                if (lastFailedHost.isEmpty()) {
                    return GitHubBackgroundNetworkGateResult(
                        ready = true,
                        attempts = index + 1,
                        state = currentState,
                    )
                }
            }
            if (index < attempts - 1) {
                delayBetweenAttempts(retryDelayMs.coerceAtLeast(0L) * (index + 1L))
            }
        }
        return GitHubBackgroundNetworkGateResult(
            ready = false,
            attempts = attempts,
            state = checkNotNull(lastState),
            failedHost = lastFailedHost,
        )
    }

    companion object {
        private const val DEFAULT_MAX_ATTEMPTS = 3
        private const val DEFAULT_RETRY_DELAY_MS = 1_500L
        private val REQUIRED_HOSTS = listOf("github.com", "api.github.com")

        fun forJob(
            context: Context,
            network: Network?,
        ): GitHubBackgroundNetworkGate {
            val connectivityManager = context.applicationContext
                .getSystemService(ConnectivityManager::class.java)
            return GitHubBackgroundNetworkGate(
                stateProvider = {
                    val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
                    GitHubBackgroundNetworkState(
                        present = network != null && capabilities != null,
                        internet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
                        validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
                        notSuspended =
                            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED) == true,
                    )
                },
                resolveHost = { host ->
                    runCatching { network?.getAllByName(host)?.isNotEmpty() == true }
                        .onFailure { error ->
                            AppLogger.i(TAG, "github background DNS preflight failed host=$host type=${error.javaClass.simpleName}")
                        }
                        .getOrDefault(false)
                },
            )
        }

        private const val TAG = "GitHubBackgroundNetwork"
    }
}

internal data class GitHubBackgroundNetworkGateResult(
    val ready: Boolean,
    val attempts: Int,
    val state: GitHubBackgroundNetworkState,
    val failedHost: String = "",
)
