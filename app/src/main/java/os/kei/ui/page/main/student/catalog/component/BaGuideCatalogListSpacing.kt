package os.kei.ui.page.main.student.catalog.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberBaGuideCatalogEntryListGap(): Dp {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        when {
            configuration.screenWidthDp >= 600 -> 8.dp
            configuration.screenWidthDp >= 480 -> 6.dp
            configuration.screenWidthDp <= 430 || configuration.screenHeightDp <= 760 -> 4.dp
            else -> 5.dp
        }
    }
}
