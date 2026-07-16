@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.sheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val CandidateStatusPillMaxWidth = 136.dp
internal val GitHubCandidateChoiceContentPadding =
    PaddingValues(horizontal = 10.dp, vertical = 8.dp)
internal val GitHubCandidateChoicePressSafePadding = 2.dp

@Immutable
internal data class GitHubCandidateChoiceColors(
    val accentColor: Color,
    val containerColor: Color,
    val borderColor: Color,
    val titleColor: Color,
)

@Composable
internal fun gitHubCandidateChoiceColors(
    selected: Boolean,
    recommended: Boolean,
    isDark: Boolean,
): GitHubCandidateChoiceColors {
    val accentColor =
        when {
            selected -> GitHubStatusPalette.Update
            recommended -> GitHubStatusPalette.Active
            else -> MiuixTheme.colorScheme.primary
        }
    return GitHubCandidateChoiceColors(
        accentColor = accentColor,
        containerColor = accentColor.copy(alpha = if (isDark) 0.08f else 0.10f),
        borderColor =
            accentColor.copy(
                alpha = if (selected || recommended) 0.34f else 0.18f,
            ),
        titleColor =
            if (selected) {
                GitHubStatusPalette.Update
            } else {
                MiuixTheme.colorScheme.onBackground
            },
    )
}

@Composable
internal fun GitHubCandidateStatusPill(
    label: String,
    color: Color,
) {
    StatusPill(
        label = label,
        color = color,
        modifier = Modifier.widthIn(max = CandidateStatusPillMaxWidth),
        size = AppStatusPillSize.Compact,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
