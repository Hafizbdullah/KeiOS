package os.kei.ui.page.main.student.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.isAppInDarkTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GuideErrorSupportingBlock(
    error: String,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    if (error.isBlank()) return

    val isDark = isAppInDarkTheme()
    val errorColor = MiuixTheme.colorScheme.error
    AppSupportingBlock(
        text = error,
        modifier = modifier,
        accentColor = errorColor,
        containerColor = errorColor.copy(alpha = if (isDark) 0.12f else 0.08f),
        contentColor = MiuixTheme.colorScheme.onErrorContainer,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
        typography = AppTypographyTokens.Supporting,
        cornerRadius = 16.dp,
        fillWidth = true,
        backdrop = backdrop,
    )
}
