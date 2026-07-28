package os.kei.ui.page.main.github.page

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubPagePermissionLaunchPolicyTest {
    @Test
    fun `android 17 avd stays on github page when app list is empty`() {
        assertFalse(
            shouldAutoRequestAppListPermission(
                appListLoaded = true,
                appListEmpty = true,
                hasAutoRequestedPermission = false,
                isHyperOs3Device = false,
            ),
        )
    }

    @Test
    fun `hyper os 3 can request its app list permission once`() {
        assertTrue(
            shouldAutoRequestAppListPermission(
                appListLoaded = true,
                appListEmpty = true,
                hasAutoRequestedPermission = false,
                isHyperOs3Device = true,
            ),
        )
        assertFalse(
            shouldAutoRequestAppListPermission(
                appListLoaded = true,
                appListEmpty = true,
                hasAutoRequestedPermission = true,
                isHyperOs3Device = true,
            ),
        )
    }
}
