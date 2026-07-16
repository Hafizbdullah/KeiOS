package os.kei.ui.page.main.debug

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import os.kei.core.prefs.UiPrefs

class DebugLiquidCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appThemeMode = UiPrefs.getAppThemeMode()
        enableDebugEdgeToEdge(appThemeMode)

        setContent {
            DebugActivityTheme(appThemeMode) {
                DebugLiquidCatalogPage(onClose = { finish() })
            }
        }
    }

    companion object {
        fun launch(context: Context) {
            context.launchDebugActivity(DebugLiquidCatalogActivity::class.java)
        }
    }
}
