package os.kei.ui.page.main.widget.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.widget.status.StatusPill

@Immutable
data class AppOverviewPill(
    val label: String,
    val color: Color,
)

private val OverviewPillHeight = 28.dp
private val OverviewPillPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppOverviewPillFlow(
    pills: List<AppOverviewPill>,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pills.forEach { pill ->
            AppOverviewPillItem(
                pill = pill,
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .height(OverviewPillHeight),
                backdrop = backdrop,
            )
        }
    }
}

@Composable
fun AppOverviewPillItem(
    pill: AppOverviewPill,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    StatusPill(
        label = pill.label,
        color = pill.color,
        modifier = modifier.height(OverviewPillHeight),
        size = AppStatusPillSize.Compact,
        contentPadding = OverviewPillPadding,
        backdrop = backdrop,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
