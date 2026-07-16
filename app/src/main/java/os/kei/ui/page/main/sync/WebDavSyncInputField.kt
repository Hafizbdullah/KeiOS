@file:Suppress("FunctionName")

package os.kei.ui.page.main.sync

import android.view.WindowInsets as AndroidWindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.VisualTransformation
import os.kei.ui.page.main.widget.glass.AppStandaloneLiquidInputField
import os.kei.ui.page.main.widget.glass.GlassVariant

/**
 * Keeps a focused WebDAV field inside the form when Back is pressed so the route stays open
 * while the field releases focus and the input method closes.
 */
@Composable
internal fun WebDavLiquidTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
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
        variant = GlassVariant.SheetInput,
        singleLine = true,
        visualTransformation = visualTransformation,
        onFocusActiveChange = { fieldFocused = it },
    )
}
