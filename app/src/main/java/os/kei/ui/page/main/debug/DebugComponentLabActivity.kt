package os.kei.ui.page.main.debug

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import os.kei.core.prefs.UiPrefs
import os.kei.ui.page.main.widget.sheet.SceneBackdropHost

class DebugComponentLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appThemeMode = UiPrefs.getAppThemeMode()
        enableDebugEdgeToEdge(appThemeMode)

        setContent {
            DebugActivityTheme(appThemeMode) {
                // Sheets, alerts and action sheets only get real glass where a scene backdrop exists
                // to sample. Without this they still work, but fall back to an opaque fill.
                SceneBackdropHost {
                    DebugComponentLabPage(
                        onClose = { finish() },
                        onOpenLiquidCatalog = { DebugLiquidCatalogActivity.launch(this) }
                    )
                }
            }
        }
    }

    companion object {
        fun launch(context: Context) {
            context.launchDebugActivity(DebugComponentLabActivity::class.java)
        }
    }
}
