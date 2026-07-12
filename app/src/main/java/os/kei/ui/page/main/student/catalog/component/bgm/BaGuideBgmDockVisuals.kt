@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.component.bgm

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaGuideBgmDockTabIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    iconSize: Dp = 24.dp,
    selectionProgress: Float = if (selected) 1f else 0f,
) {
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint =
            baGuideBgmDockTint(
                selected = selected,
                accent = accent,
                selectionProgress = selectionProgress,
            ),
        modifier = Modifier.size(iconSize),
    )
}

@Composable
internal fun baGuideBgmDockTint(
    selected: Boolean,
    accent: Color,
    selectionProgress: Float = if (selected) 1f else 0f,
): Color =
    lerp(
        MiuixTheme.colorScheme.onBackground.copy(alpha = 0.90f),
        accent,
        selectionProgress.coerceIn(0f, 1f),
    )
