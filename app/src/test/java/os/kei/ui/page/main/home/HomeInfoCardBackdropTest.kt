package os.kei.ui.page.main.home

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = HomeInfoCardBackdropTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class HomeInfoCardBackdropTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exportsIndependentBackdropToOverviewPills() {
        var sceneBackdrop: Backdrop? = null
        var cardContentBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                sceneBackdrop = backdrop
                Box(modifier = Modifier.size(260.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .background(Color.White)
                                .layerBackdrop(backdrop),
                    )
                    HomeInfoCard(
                        backdrop = backdrop,
                        blurEnabled = true,
                    ) {
                        cardContentBackdrop = LocalLiquidParentBackdrop.current
                        Box(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .testTag("home-overview-card-content"),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("home-overview-card-content").assertExists()
        composeRule.runOnIdle {
            assertNotNull(sceneBackdrop)
            assertNotNull(cardContentBackdrop)
            assertNotSame(sceneBackdrop, cardContentBackdrop)
        }
    }
}

class HomeInfoCardBackdropTestApp : Application()
