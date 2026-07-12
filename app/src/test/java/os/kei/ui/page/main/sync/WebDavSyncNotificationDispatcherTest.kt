package os.kei.ui.page.main.sync

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class WebDavSyncNotificationDispatcherTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `focus titles identify WebDAV for every operation`() {
        val expected =
            mapOf(
                WebDavSyncNotificationOperation.RemoteProbe to "WebDAV Remote refresh",
                WebDavSyncNotificationOperation.Sync to "WebDAV Sync",
                WebDavSyncNotificationOperation.Upload to "WebDAV Upload",
                WebDavSyncNotificationOperation.Download to "WebDAV Download",
            )

        expected.forEach { (operation, title) ->
            assertEquals(
                title,
                WebDavSyncNotificationDispatcher.run { operation.notificationTitle(context) },
            )
        }
    }
}
