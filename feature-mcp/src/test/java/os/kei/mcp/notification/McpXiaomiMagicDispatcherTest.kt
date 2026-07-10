package os.kei.mcp.notification

import android.app.Application
import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = McpXiaomiMagicDispatcherTestApp::class, sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class McpXiaomiMagicDispatcherTest {
    @Test
    fun `notify returns false when primary and fallback posts fail`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        var postAttempts = 0
        val environment =
            McpXiaomiMagicDispatchEnvironment(
                canPostNotifications = { true },
                resolveTargetUid = { 12_345 },
                canUseCommand = { true },
                shouldExecute = { true },
                healNetworking = {},
                blockNetworking = { true },
                postNotification = { _, _, _, _ ->
                    postAttempts += 1
                    false
                },
                awaitRestore = {},
                restoreNetworking = {},
                needsRestore = { true },
            )

        val result =
            McpXiaomiMagicDispatcher.notify(
                context = context,
                notificationId = 42,
                notification = Notification(),
                environment = environment,
                dispatchScope = this,
                mutex = Mutex(),
            )

        assertFalse(result)
        assertEquals(2, postAttempts)
    }

    @Test
    fun `notify returns initial post result before delayed network restoration completes`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val restoreGate = CompletableDeferred<Unit>()
        val restoreCompleted = CompletableDeferred<Unit>()
        var restored = false
        val environment =
            McpXiaomiMagicDispatchEnvironment(
                canPostNotifications = { true },
                resolveTargetUid = { 12_345 },
                canUseCommand = { true },
                shouldExecute = { true },
                healNetworking = {},
                blockNetworking = { true },
                postNotification = { _, _, _, _ -> true },
                awaitRestore = { restoreGate.await() },
                restoreNetworking = {
                    restored = true
                    restoreCompleted.complete(Unit)
                },
                needsRestore = { true },
            )

        val result =
            McpXiaomiMagicDispatcher.notify(
                context = context,
                notificationId = 43,
                notification = Notification(),
                environment = environment,
                dispatchScope = backgroundScope,
                mutex = Mutex(),
            )

        assertTrue(result)
        assertFalse(restored)
        restoreGate.complete(Unit)
        restoreCompleted.await()
        assertTrue(restored)
    }
}

class McpXiaomiMagicDispatcherTestApp : Application()
