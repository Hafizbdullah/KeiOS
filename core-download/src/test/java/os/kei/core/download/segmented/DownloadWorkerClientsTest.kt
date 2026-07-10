package os.kei.core.download.segmented

import okhttp3.OkHttpClient
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DownloadWorkerClientsTest {
    @Test
    fun `isolated strategy gives every worker a distinct connection pool`() {
        val baseClient = OkHttpClient()

        createDownloadWorkerClientSet(
            baseClient = baseClient,
            count = 4,
            strategy = SegmentedDownloadConnectionStrategy.IsolatedPerWorker,
        ).use { clientSet ->
            assertEquals(4, clientSet.clients.size)
            assertEquals(4, clientSet.clients.map { it.connectionPool }.distinctBy { System.identityHashCode(it) }.size)
        }
    }

    @Test
    fun `shared strategy reuses the supplied client`() {
        val baseClient = OkHttpClient()

        createDownloadWorkerClientSet(
            baseClient = baseClient,
            count = 4,
            strategy = SegmentedDownloadConnectionStrategy.Shared,
        ).use { clientSet ->
            assertEquals(4, clientSet.clients.size)
            clientSet.clients.forEach { client -> assertSame(baseClient, client) }
        }
    }
}
