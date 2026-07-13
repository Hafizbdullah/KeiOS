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

internal fun sheetCardOptics(
    visualMode: SheetVisualMode,
    interactive: Boolean,
): SheetCardOptics =
    when (visualMode) {
        SheetVisualMode.Liquid ->
            SheetCardOptics(
                depthEffect = true,
                highlightAlpha = if (interactive) 1f else 0.82f,
                borderWidth = 1.dp,
            )

        SheetVisualMode.Miuix ->
            SheetCardOptics(
                depthEffect = false,
                highlightAlpha = null,
                borderWidth = 1.dp,
            )
    }
