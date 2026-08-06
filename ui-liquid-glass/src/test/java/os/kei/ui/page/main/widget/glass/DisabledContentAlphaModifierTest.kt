package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Test

class DisabledContentAlphaModifierTest {
    @Test
    fun enabledControlOmitsIdentityAlphaLayer() {
        assertEquals(0, disabledContentAlphaModifier(enabled = true).elementCount())
    }

    @Test
    fun disabledControlKeepsSingleAlphaLayer() {
        assertEquals(1, disabledContentAlphaModifier(enabled = false).elementCount())
    }

    @Test
    fun disabledModifierIsSharedAcrossCallSites() {
        assertEquals(
            disabledContentAlphaModifier(enabled = false),
            disabledContentAlphaModifier(enabled = false),
        )
    }
}

private fun Modifier.elementCount(): Int = foldIn(0) { count, _ -> count + 1 }
