package os.kei.core.download.segmented

import java.io.Closeable
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient

internal class DownloadWorkerClientSet(
    val clients: List<OkHttpClient>,
    private val isolatedPools: List<ConnectionPool>,
) : Closeable {
    override fun close() {
        isolatedPools.forEach(ConnectionPool::evictAll)
    }
}

internal fun createDownloadWorkerClientSet(
    baseClient: OkHttpClient,
    count: Int,
    strategy: SegmentedDownloadConnectionStrategy,
): DownloadWorkerClientSet {
    require(count > 0) { "worker client count must be positive" }
    if (strategy == SegmentedDownloadConnectionStrategy.Shared) {
        return DownloadWorkerClientSet(
            clients = List(count) { baseClient },
            isolatedPools = emptyList(),
        )
    }
    val pools = List(count) {
        ConnectionPool(
            maxIdleConnections = 1,
            keepAliveDuration = WORKER_CONNECTION_KEEP_ALIVE_SECONDS,
            timeUnit = TimeUnit.SECONDS,
        )
    }
    return DownloadWorkerClientSet(
        clients = pools.map { pool ->
            baseClient.newBuilder()
                .connectionPool(pool)
                .build()
        },
        isolatedPools = pools,
    )
}

private const val WORKER_CONNECTION_KEEP_ALIVE_SECONDS = 30L
