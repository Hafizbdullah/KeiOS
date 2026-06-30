package os.kei.ui.page.main.widget.sheet

import androidx.compose.runtime.staticCompositionLocalOf

enum class SheetVisualMode {
    Liquid,
    Miuix,
}

val LocalSheetVisualMode = staticCompositionLocalOf { SheetVisualMode.Miuix }
