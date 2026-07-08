package os.kei.ui.page.main.settings.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import os.kei.R
import os.kei.ui.page.main.widget.chrome.TabbedPageCategory
import com.composables.icons.lucide.R as LucideR

internal enum class SettingsCategory(
    override val iconRes: Int,
    override val labelRes: Int,
) : TabbedPageCategory {
    Access(LucideR.drawable.lucide_ic_shield_check, R.string.settings_category_access),
    KeepAlive(LucideR.drawable.lucide_ic_bell, R.string.settings_category_keep_alive),
    Interface(LucideR.drawable.lucide_ic_palette, R.string.settings_category_interface),
    Data(LucideR.drawable.lucide_ic_database, R.string.settings_category_data),
}

@Composable
internal fun SettingsCategory.label(): String {
    return stringResource(
        when (this) {
            SettingsCategory.Access -> R.string.settings_category_access
            SettingsCategory.KeepAlive -> R.string.settings_category_keep_alive
            SettingsCategory.Interface -> R.string.settings_category_interface
            SettingsCategory.Data -> R.string.settings_category_data
        }
    )
}

@Composable
internal fun SettingsCategory.icon(): ImageVector {
    val drawableRes = when (this) {
        SettingsCategory.Access -> LucideR.drawable.lucide_ic_shield_check
        SettingsCategory.KeepAlive -> LucideR.drawable.lucide_ic_bell
        SettingsCategory.Interface -> LucideR.drawable.lucide_ic_palette
        SettingsCategory.Data -> LucideR.drawable.lucide_ic_database
    }
    return ImageVector.vectorResource(drawableRes)
}
