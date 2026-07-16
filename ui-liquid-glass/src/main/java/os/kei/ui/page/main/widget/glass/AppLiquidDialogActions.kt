@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.glass

import os.kei.ui.page.main.widget.isAppInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalLiquidDialogBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun AppLiquidDialogActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    containerColor: Color? = null,
    textColor: Color? = null,
    leadingIcon: ImageVector? = null,
    iconTint: Color? = null,
    variant: GlassVariant =
        if (containerColor != null) {
            GlassVariant.SheetPrimaryAction
        } else {
            GlassVariant.SheetAction
        },
) {
    val isDark = isAppInDarkTheme()
    val accent = containerColor ?: MiuixTheme.colorScheme.primary
    val defaultContentColor =
        if (isDark) {
            accent
        } else {
            resolveLightGlassContentColor(
                accent = accent,
                backgroundAlpha = glassContainerOverlayAlpha(variant, false),
            )
        }
    val resolvedTextColor = textColor ?: defaultContentColor
    val resolvedIconTint = iconTint ?: resolvedTextColor
    val dialogBackdrop = LocalLiquidDialogBackdrop.current
    if (dialogBackdrop != null) {
        AppLiquidTextButton(
            backdrop = dialogBackdrop,
            text = text,
            onClick = onClick,
            modifier = modifier.then(buttonModifier),
            textColor = resolvedTextColor,
            containerColor = containerColor,
            leadingIcon = leadingIcon,
            iconTint = resolvedIconTint,
            enabled = enabled,
            variant = variant,
            minHeight = 40.dp,
            horizontalPadding = 12.dp,
            verticalPadding = 8.dp,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis,
            textSoftWrap = false,
        )
    } else {
        AppStandaloneLiquidTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            buttonModifier = buttonModifier,
            textColor = resolvedTextColor,
            containerColor = containerColor,
            leadingIcon = leadingIcon,
            iconTint = resolvedIconTint,
            enabled = enabled,
            variant = variant,
            minHeight = 40.dp,
            horizontalPadding = 12.dp,
            verticalPadding = 8.dp,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis,
            textSoftWrap = false,
        )
    }
}
