package os.kei.ui.page.main.github.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import os.kei.R
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.sheet.SheetSurfaceCard
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubActionsArtifactHintText(
    text: String
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = githubActionsSecondaryTextColor(isAppInDarkTheme()),
        fontSize = AppTypographyTokens.Supporting.fontSize,
        lineHeight = AppTypographyTokens.Supporting.lineHeight,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun GitHubActionsLoadMoreRunsButton(
    backdrop: LayerBackdrop,
    visibleRunLimit: Int,
    loading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        AppLiquidTextButton(
            backdrop = backdrop,
            variant = GlassVariant.SheetAction,
            text = if (loading) {
                stringResource(R.string.common_loading)
            } else {
                stringResource(R.string.github_actions_action_load_more_runs, visibleRunLimit)
            },
            leadingIcon = appLucideRefreshIcon(),
            enabled = !loading,
            textColor = MiuixTheme.colorScheme.primary,
            iconTint = MiuixTheme.colorScheme.primary,
            onClick = onClick,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun GitHubActionsSelectableCard(
    selected: Boolean,
    isDark: Boolean,
    containerColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val resolvedContainerColor = containerColor ?: if (selected) {
        githubActionsNeutralCardColor(isDark, prominent = true)
    } else {
        githubActionsNeutralCardColor(isDark)
    }
    val resolvedBorderColor = borderColor ?: githubActionsNeutralBorderColor(isDark, prominent = selected)
    SheetSurfaceCard(
        containerColor = resolvedContainerColor,
        borderColor = resolvedBorderColor,
        verticalSpacing = 10.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onClick,
        role = Role.RadioButton,
        selected = selected,
    ) {
        content()
    }
}

@Composable
internal fun GitHubActionsTitleRow(
    title: String,
    accent: Color,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = accent,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            fontWeight = AppTypographyTokens.BodyEmphasis.fontWeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        trailing()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GitHubActionsPillRow(
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = { content() }
    )
}

@Composable
internal fun GitHubActionsLoadingCard(text: String) {
    val isDark = isAppInDarkTheme()
    SheetSurfaceCard(
        containerColor = githubActionsNeutralCardColor(isDark),
        borderColor = githubActionsNeutralBorderColor(isDark),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidCircularProgressBar(
                size = 18.dp,
                strokeWidth = 2.dp,
                activeColor = MiuixTheme.colorScheme.primary,
                inactiveColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
            )
            Text(
                text = text,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Body.fontSize,
                lineHeight = AppTypographyTokens.Body.lineHeight
            )
        }
    }
}

@Composable
internal fun GitHubActionsNoticeCard(
    text: String,
    accent: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = githubActionsNoticeColors(accent = accent, isDark = isDark)
    AppSupportingBlock(
        text = text,
        modifier = modifier.fillMaxWidth(),
        accentColor = colors.accentColor,
        containerColor = colors.containerColor,
        contentColor = colors.contentColor,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        typography = AppTypographyTokens.Body,
        cornerRadius = CardLayoutRhythm.cardCornerRadius,
        borderColor = colors.borderColor,
        borderWidth = 1.dp,
        fillWidth = true,
        depthEffect = true,
        highlightAlpha = 0.82f,
        shadow = false,
        shadowAlpha = 0.10f,
    )
}

@Immutable
internal data class GitHubActionsNoticeColors(
    val accentColor: Color,
    val containerColor: Color,
    val borderColor: Color,
    val contentColor: Color,
)

@Composable
internal fun githubActionsNoticeColors(
    accent: Color,
    isDark: Boolean,
): GitHubActionsNoticeColors {
    val isError = accent == GitHubStatusPalette.Error
    return GitHubActionsNoticeColors(
        accentColor =
            if (isError) {
                GitHubStatusPalette.Error
            } else {
                MiuixTheme.colorScheme.onBackgroundVariant
            },
        containerColor =
            if (isError) {
                GitHubStatusPalette.tonedSurface(GitHubStatusPalette.Error, isDark).copy(
                    alpha = if (isDark) 0.16f else 0.09f,
                )
            } else {
                githubActionsNeutralCardColor(isDark)
            },
        borderColor =
            if (isError) {
                GitHubStatusPalette.Error.copy(alpha = if (isDark) 0.24f else 0.16f)
            } else {
                githubActionsNeutralBorderColor(isDark)
            },
        contentColor =
            if (isError) {
                GitHubStatusPalette.Error
            } else {
                githubActionsSecondaryTextColor(isDark)
            },
    )
}
