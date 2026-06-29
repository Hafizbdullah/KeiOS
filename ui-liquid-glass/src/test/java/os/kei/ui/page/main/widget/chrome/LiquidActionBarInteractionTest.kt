package os.kei.ui.page.main.widget.chrome

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = LiquidActionBarInteractionTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class LiquidActionBarInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun layeredVisualOverlayDoesNotBlockUnselectedIconTaps() {
        val clicked = mutableListOf<String>()

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                Box(
                    modifier =
                        Modifier
                            .size(width = 360.dp, height = 120.dp)
                            .background(Color(0xFFF3F4F6))
                            .layerBackdrop(backdrop),
                    contentAlignment = Alignment.Center,
                ) {
                    LiquidActionBar(
                        backdrop = backdrop,
                        items =
                            listOf(
                                LiquidActionItem(
                                    icon = TestActionIcon,
                                    contentDescription = "First",
                                    testTag = "first-action",
                                    tooltipText = "First tooltip",
                                    onClick = { clicked += "first" },
                                ),
                                LiquidActionItem(
                                    icon = TestActionIcon,
                                    contentDescription = "Selected",
                                    testTag = "selected-action",
                                    tooltipText = "Selected tooltip",
                                    onClick = { clicked += "selected" },
                                ),
                                LiquidActionItem(
                                    icon = TestActionIcon,
                                    contentDescription = "Third",
                                    testTag = "third-action",
                                    tooltipText = "Third tooltip",
                                    onClick = { clicked += "third" },
                                ),
                            ),
                        isBlurEnabled = false,
                        layeredStyleEnabled = true,
                        selectedIndex = 1,
                    )
                }
            }
        }

        composeRule.onNodeWithTag("first-action").performTouchInput {
            down(center)
            up()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("third-action").performTouchInput {
            down(center)
            up()
        }
        composeRule.waitForIdle()

        assertEquals(listOf("first", "third"), clicked)
    }
}

class LiquidActionBarInteractionTestApp : Application()

private val TestActionIcon =
    ImageVector.Builder(
        name = "TestActionIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 5f)
            lineTo(19f, 5f)
            lineTo(19f, 19f)
            lineTo(5f, 19f)
            close()
        }
    }.build()
