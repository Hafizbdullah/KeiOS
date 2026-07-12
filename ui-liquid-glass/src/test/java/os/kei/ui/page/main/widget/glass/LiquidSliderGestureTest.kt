package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.emptyBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = LiquidSliderGestureTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class LiquidSliderGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun verticalGestureOnSliderScrollsParentWithoutChangingValue() {
        var sliderValue = 0.5f
        var valueChangeCount = 0
        var observedScrollValue = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val scrollState = rememberScrollState()
                observedScrollValue = scrollState.value
                Column(
                    Modifier
                        .height(180.dp)
                        .verticalScroll(scrollState),
                ) {
                    LiquidVolumeSlider(
                        value = { sliderValue },
                        onValueChange = { value ->
                            sliderValue = value
                            valueChangeCount++
                        },
                        valueRange = 0f..1f,
                        visibilityThreshold = 0.001f,
                        backdrop = emptyBackdrop(),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("scrolling-liquid-slider"),
                    )
                    Spacer(Modifier.height(600.dp))
                }
            }
        }

        composeRule.onNodeWithTag("scrolling-liquid-slider").performTouchInput {
            down(center)
            moveBy(Offset(0f, -120f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(0, valueChangeCount)
        assertEquals(0.5f, sliderValue)
        assertTrue(observedScrollValue > 0)
    }
}

class LiquidSliderGestureTestApp : Application()
