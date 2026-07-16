package os.kei.ui.page.main.student.catalog.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.status.StatusIconPill

@Composable
internal fun BaGuideCatalogStatusIconPill(
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    StatusIconPill(
        label = label,
        color = color,
        icon = icon,
        modifier = modifier,
        backdrop = backdrop,
        width = 28.dp,
        height = 22.dp,
        iconSize = 13.dp,
    )
}
