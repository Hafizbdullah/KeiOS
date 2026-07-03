package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import os.kei.ui.page.main.widget.shape.appSquircleBorder
import top.yukonga.miuix.kmp.basic.Icon

private val GuideStatusIconPillWidth = 28.dp
private val GuideStatusIconPillHeight = 22.dp
private val GuideStatusIconSize = 13.dp

@Composable
internal fun BaGuideCatalogStatusIconPill(
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val backgroundAlpha = if (isDark) 0.20f else 0.16f
    val borderAlpha = if (isDark) 0.42f else 0.36f
    val iconTint = if (isDark) color else color.copy(alpha = 0.96f)
    Box(
        modifier =
            modifier
                .size(width = GuideStatusIconPillWidth, height = GuideStatusIconPillHeight)
                .appSquircleBackground(color.copy(alpha = backgroundAlpha), 999.dp)
                .appSquircleBorder(
                    width = 0.8.dp,
                    color = color.copy(alpha = borderAlpha),
                    cornerRadius = 999.dp,
                ).semantics {
                    contentDescription = label
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(GuideStatusIconSize),
        )
    }
}
