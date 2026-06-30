package os.kei.ui.page.main.widget.sheet

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

internal fun Color.opaqueCompositeOver(base: Color): Color =
    if (alpha >= 0.999f) {
        this
    } else {
        compositeOver(base).copy(alpha = 1f)
    }
