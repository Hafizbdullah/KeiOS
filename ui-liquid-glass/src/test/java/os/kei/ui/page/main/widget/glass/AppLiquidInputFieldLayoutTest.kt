package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import org.junit.Test
import kotlin.test.assertEquals

class AppLiquidInputFieldLayoutTest {
    @Test
    fun singleLineContentUsesVerticallyCenteredAlignment() {
        assertEquals(Alignment.CenterStart, liquidInputContentAlignment(true, TextAlign.Start))
        assertEquals(Alignment.Center, liquidInputContentAlignment(true, TextAlign.Center))
        assertEquals(Alignment.CenterEnd, liquidInputContentAlignment(true, TextAlign.End))
    }

    @Test
    fun multilineContentUsesTopAlignment() {
        assertEquals(Alignment.TopStart, liquidInputContentAlignment(false, TextAlign.Start))
        assertEquals(Alignment.TopCenter, liquidInputContentAlignment(false, TextAlign.Center))
        assertEquals(Alignment.TopEnd, liquidInputContentAlignment(false, TextAlign.End))
    }
}
