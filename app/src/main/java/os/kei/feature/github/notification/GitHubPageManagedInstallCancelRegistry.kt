package os.kei.feature.github.notification

import java.util.concurrent.atomic.AtomicLong

internal object GitHubPageManagedInstallCancelRegistry {
    private data class Registration(
        val token: Long,
        val cancel: suspend () -> Unit,
    )

    private val lock = Any()
    private val tokenSequence = AtomicLong(0L)
    private var active: Registration? = null

    fun register(cancel: suspend () -> Unit): Long {
        val token = tokenSequence.incrementAndGet()
        synchronized(lock) {
            active = Registration(token = token, cancel = cancel)
        }
        return token
    }

    fun clear(token: Long) {
        synchronized(lock) {
            if (active?.token == token) {
                active = null
            }
        }
    }

    suspend fun cancelActive(): Boolean {
        val callback =
            synchronized(lock) {
                active?.cancel.also { active = null }
            }
                ?: return false
        callback()
        return true
    }

    internal fun resetForTest() {
        synchronized(lock) {
            active = null
        }
    }
}
