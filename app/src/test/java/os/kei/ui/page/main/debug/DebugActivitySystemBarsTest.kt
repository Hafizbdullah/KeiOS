package os.kei.ui.page.main.debug

import android.app.Application
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import os.kei.core.prefs.AppThemeMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
@Suppress("DEPRECATION")
class DebugActivitySystemBarsTest {
    @Test
    @Config(qualifiers = "night")
    fun lightAppThemeUsesDarkIconsWhenSystemThemeIsDark() {
        assertSystemBars(
            appThemeMode = AppThemeMode.LIGHT,
            expectDarkIcons = true,
        )
    }

    @Test
    @Config(qualifiers = "notnight")
    fun darkAppThemeUsesLightIconsWhenSystemThemeIsLight() {
        assertSystemBars(
            appThemeMode = AppThemeMode.DARK,
            expectDarkIcons = false,
        )
    }

    @Test
    @Config(qualifiers = "night")
    fun followSystemUsesLightIconsWhenSystemThemeIsDark() {
        assertSystemBars(
            appThemeMode = AppThemeMode.FOLLOW_SYSTEM,
            expectDarkIcons = false,
        )
    }

    @Test
    @Config(qualifiers = "notnight")
    fun followSystemUsesDarkIconsWhenSystemThemeIsLight() {
        assertSystemBars(
            appThemeMode = AppThemeMode.FOLLOW_SYSTEM,
            expectDarkIcons = true,
        )
    }

    private fun assertSystemBars(
        appThemeMode: AppThemeMode,
        expectDarkIcons: Boolean,
    ) {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()

        activity.enableDebugEdgeToEdge(appThemeMode)

        val insetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        assertEquals(Color.TRANSPARENT, activity.window.statusBarColor)
        assertEquals(Color.TRANSPARENT, activity.window.navigationBarColor)
        assertEquals(expectDarkIcons, insetsController.isAppearanceLightStatusBars)
        assertEquals(expectDarkIcons, insetsController.isAppearanceLightNavigationBars)

        activity.finish()
    }

    private class TestActivity : ComponentActivity()
}
