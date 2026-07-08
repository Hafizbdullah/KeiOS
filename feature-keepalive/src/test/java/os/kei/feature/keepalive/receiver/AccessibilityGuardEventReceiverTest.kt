package os.kei.feature.keepalive.receiver

import android.content.Intent
import kotlin.test.assertEquals
import org.junit.Test
import os.kei.feature.keepalive.service.AccessibilityGuardForegroundService

class AccessibilityGuardEventReceiverTest {
    @Test
    fun `boot and package replaced events require boot restore policy`() {
        assertEquals(
            false,
            AccessibilityGuardEventReceiver.shouldHandle(
                action = Intent.ACTION_BOOT_COMPLETED,
                bootRestoreEnabled = false,
            ),
        )
        assertEquals(
            true,
            AccessibilityGuardEventReceiver.shouldHandle(
                action = Intent.ACTION_BOOT_COMPLETED,
                bootRestoreEnabled = true,
            ),
        )
        assertEquals(
            false,
            AccessibilityGuardEventReceiver.shouldHandle(
                action = Intent.ACTION_MY_PACKAGE_REPLACED,
                bootRestoreEnabled = false,
            ),
        )
        assertEquals(
            true,
            AccessibilityGuardEventReceiver.shouldHandle(
                action = Intent.ACTION_MY_PACKAGE_REPLACED,
                bootRestoreEnabled = true,
            ),
        )
    }

    @Test
    fun `explicit check action is always handled`() {
        assertEquals(
            true,
            AccessibilityGuardEventReceiver.shouldHandle(
                action = AccessibilityGuardForegroundService.ACTION_CHECK_ACCESSIBILITY_GUARD,
                bootRestoreEnabled = false,
            ),
        )
    }
}
