@file:Suppress("FunctionName")

package os.kei.ui.page.main.feedback

import android.view.WindowInsets as AndroidWindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidInputField
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FeedbackLiquidTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    minHeight: Dp,
    singleLine: Boolean = false,
) {
    val isBody = minHeight > 120.dp
    var fieldFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current

    BackHandler(enabled = fieldFocused) {
        focusManager.clearFocus(force = true)
        view.windowInsetsController?.hide(AndroidWindowInsets.Type.ime())
            ?: keyboardController?.hide()
    }

    AppStandaloneLiquidInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        fieldModifier =
            Modifier
                .fillMaxWidth()
                .then(if (isBody) Modifier.height(minHeight) else Modifier),
        enabled = enabled,
        singleLine = singleLine,
        fontSize = if (isBody) 14.sp else AppTypographyTokens.Body.fontSize,
        lineHeight = if (isBody) 20.sp else AppTypographyTokens.Body.lineHeight,
        variant = GlassVariant.SheetInput,
        minHeight = minHeight,
        cornerRadius = 18.dp,
        horizontalPadding = 14.dp,
        verticalPadding = 12.dp,
        placeholderMaxLines = if (isBody) 2 else 1,
        onFocusActiveChange = { fieldFocused = it },
    )
}

@Composable
internal fun FeedbackFieldLabel(text: String) {
    Text(
        text = text,
        color = feedbackSecondaryTextColor(),
        fontSize = AppTypographyTokens.Caption.fontSize,
        lineHeight = AppTypographyTokens.Caption.lineHeight,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 2.dp),
    )
}
