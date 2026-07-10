package os.kei.core.download.segmented

import java.util.IdentityHashMap
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Protocol

internal class BenchmarkConnectionTracker : EventListener() {
    private val lock = Any()
    private val connections = IdentityHashMap<Connection, Protocol>()
    private var requests = 0

    override fun requestHeadersStart(call: Call) {
        synchronized(lock) {
            requests += 1
        }
    }

    override fun connectionAcquired(
        call: Call,
        connection: Connection,
    ) {
        synchronized(lock) {
            connections[connection] = connection.protocol()
        }
    }

    val physicalConnectionCount: Int
        get() = synchronized(lock) { connections.size }

    val requestCount: Int
        get() = synchronized(lock) { requests }

    fun protocolLabel(fallback: String): String =
        synchronized(lock) {
            connections.values
                .map(Protocol::toString)
                .distinct()
                .sorted()
                .joinToString(separator = "+")
                .ifBlank { fallback }
        }
}
