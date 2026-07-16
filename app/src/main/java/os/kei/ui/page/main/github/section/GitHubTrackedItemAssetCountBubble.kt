package os.kei.ui.page.main.github.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.status.StatusPill

@Suppress("FunctionName")
@Composable
internal fun GitHubAssetCountBubble(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val isDark = isAppInDarkTheme()
    Box(
        modifier = modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        StatusPill(
            label = if (loading) "" else label,
            color = color,
            modifier = Modifier.matchParentSize(),
            contentPadding = PaddingValues(0.dp),
            backgroundAlphaOverride = if (isDark) 0.18f else 0.12f,
            borderAlphaOverride = if (isDark) 0.34f else 0.24f,
            backdrop = LocalLiquidParentBackdrop.current,
            maxLines = 1,
            contentColorOverride = if (isDark) color else color.copy(alpha = 0.96f),
            typographyOverride =
                TextStyle(
                    fontSize = AppTypographyTokens.Caption.fontSize,
                    lineHeight = AppTypographyTokens.Caption.lineHeight,
                    fontWeight = AppTypographyTokens.Caption.fontWeight,
                ),
        )
        if (loading) {
            LiquidCircularProgressBar(
                size = 14.dp,
                strokeWidth = 2.dp,
                activeColor = color,
                inactiveColor = color.copy(alpha = 0.18f),
            )
        }
    }
}
