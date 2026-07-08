package os.kei.feature.keepalive.receiver

import android.content.Intent
import kotlin.test.assertEquals
import org.junit.Test
import os.kei.feature.keepalive.service.AccessibilityGuardForegroundService

class AccessibilityGuardEventReceiverTest {
    @Test
    fun `boot and package replaced events require boot check policy`() {
        assertEquals(
            false,
            AccessibilityGuardEventReceiver.shouldHandle(
                action = Intent.ACTION_BOOT_COMPLETED,
                bootCheckEnabled = false,
            ),
        )
        assertEquals(
            true,
            AccessibilityGuardEventReceiver.shouldHandle(
                action = Intent.ACTION_BOOT_COMPLETED,
                bootCheckEnabled = true,
            ),
        )
        assertEquals(
            false,
            AccessibilityGuardEventReceiver.shouldHandle(
                action = Intent.ACTION_MY_PACKAGE_REPLACED,
                bootCheckEnabled = false,
            ),
        )
        assertEquals(
            true,
            AccessibilityGuardEventReceiver.shouldHandle(
                action = Intent.ACTION_MY_PACKAGE_REPLACED,
                bootCheckEnabled = true,
            ),
        )
    }

    @Test
    fun `explicit check action is always handled`() {
        assertEquals(
            true,
            AccessibilityGuardEventReceiver.shouldHandle(
                action = AccessibilityGuardForegroundService.ACTION_CHECK_ACCESSIBILITY_GUARD,
                bootCheckEnabled = false,
            ),
        )
    }
}
