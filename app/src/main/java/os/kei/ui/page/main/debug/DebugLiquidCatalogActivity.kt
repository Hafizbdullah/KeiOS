package os.kei.ui.page.main.debug

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import os.kei.core.prefs.UiPrefs
import os.kei.ui.page.main.widget.sheet.SceneBackdropHost

class DebugLiquidCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appThemeMode = UiPrefs.getAppThemeMode()
        enableDebugEdgeToEdge(appThemeMode)

        setContent {
            DebugActivityTheme(appThemeMode) {
                // The catalog previews sheets, alerts and action sheets, and those only get real
                // glass where a scene backdrop exists to sample. Without this they fall back to an
                // opaque fill, which is legible but not what the catalog is for.
                SceneBackdropHost {
                    DebugLiquidCatalogPage(onClose = { finish() })
                }
            }
        }
    }

    companion object {
        fun launch(context: Context) {
            context.launchDebugActivity(DebugLiquidCatalogActivity::class.java)
        }
    }
}
