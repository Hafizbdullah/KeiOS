package os.kei.ui.page.main.widget.glass

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
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = LiquidRoundedCardBackdropTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class LiquidRoundedCardBackdropTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun roundedCardExportsIndependentMaterialAndPrioritizesItForContent() {
        var pageBackdrop: Backdrop? = null
        var contentBackdrop: Backdrop? = null
        var resolvedExplicitFallback: Backdrop? = null
        var overridesExplicitFallback = false

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                val explicitFallback = rememberLayerBackdrop()
                pageBackdrop = backdrop
                Box(modifier = Modifier.size(280.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .background(Color.White)
                                .layerBackdrop(backdrop),
                    )
                    LiquidRoundedCard(
                        backdrop = backdrop,
                        exportBackdropToContent = true,
                        modifier = Modifier.testTag("rounded-card"),
                    ) {
                        contentBackdrop = LocalLiquidParentBackdrop.current
                        resolvedExplicitFallback = preferredLiquidBackdrop(explicitFallback)
                        overridesExplicitFallback =
                            LocalLiquidParentBackdropOverridesFallback.current
                        Box(modifier = Modifier.testTag("rounded-card-content"))
                    }
                }
            }
        }

        composeRule.onNodeWithTag("rounded-card").assertExists()
        composeRule.onNodeWithTag("rounded-card-content").assertExists()
        composeRule.runOnIdle {
            assertNotNull(pageBackdrop)
            assertNotNull(contentBackdrop)
            assertNotSame(pageBackdrop, contentBackdrop)
            assertSame(contentBackdrop, resolvedExplicitFallback)
            assertTrue(overridesExplicitFallback)
        }
    }
}

class LiquidRoundedCardBackdropTestApp : Application()
