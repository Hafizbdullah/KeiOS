package os.kei.core.notification.identity

import kotlin.test.assertEquals
import org.junit.Test
import os.kei.core.notification.R
import os.kei.core.prefs.LauncherIconDesign

class NotificationAppIconResolverTest {
    @Test
    fun `small notification icon keeps shared monochrome brand silhouette`() {
        val apple = NotificationAppIconResolver.smallIconResId(LauncherIconDesign.Apple)
        val android = NotificationAppIconResolver.smallIconResId(LauncherIconDesign.Android)

        assertEquals(R.drawable.ic_kei_notification_small, apple)
        assertEquals(R.drawable.ic_kei_notification_small, android)
        assertEquals(apple, android)
    }
}
