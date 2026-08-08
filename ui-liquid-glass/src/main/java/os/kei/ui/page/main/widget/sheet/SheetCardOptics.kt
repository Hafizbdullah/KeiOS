package os.kei.ui.page.main.widget.sheet

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
internal data class SheetCardOptics(
    val depthEffect: Boolean,
    val highlightAlpha: Float?,
    val borderWidth: Dp,
)

/**
 * Optics for a card inside a sheet. There is only one sheet material now — the Miuix variant and the
 * toggle that selected it are gone — so this varies only with whether the card is interactive.
 */
internal fun sheetCardOptics(interactive: Boolean): SheetCardOptics =
    SheetCardOptics(
        depthEffect = true,
        highlightAlpha = if (interactive) 1f else 0.82f,
        borderWidth = 1.dp,
    )
