package os.kei.ui.page.main.github.section

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GitHubTrackedItemAssetLoadingCard(
    alwaysLatestReleaseDownload: Boolean,
    targetAccent: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val stateContainerColor = if (alwaysLatestReleaseDownload) {
        GitHubStatusPalette.tonedSurface(
            targetAccent,
            isDark = isDark
        ).copy(alpha = if (isDark) 0.62f else 0.34f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
    }
    AppFeatureCard(
        title = stringResource(R.string.github_asset_loading_title),
        subtitle = stringResource(R.string.github_asset_loading_summary),
        modifier = modifier,
        containerColor = stateContainerColor,
        borderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
        titleColor = MiuixTheme.colorScheme.onBackground,
        subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant,
        titleMaxLines = Int.MAX_VALUE,
        subtitleMaxLines = Int.MAX_VALUE,
        titleTypography = AppTypographyTokens.BodyEmphasis,
        subtitleTypography = AppTypographyTokens.Supporting,
        headerTextVerticalSpacing = CardLayoutRhythm.metricCardTextGap,
        headerStartActionSize = 18.dp,
        sectionStartAction = {
            LiquidCircularProgressBar(
                size = 18.dp,
                strokeWidth = 2.dp,
                activeColor = MiuixTheme.colorScheme.primary,
                inactiveColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.18f),
            )
        },
        showIndication = false,
        contentPadding = PaddingValues(0.dp),
    ) {}
}

@Composable
internal fun GitHubTrackedItemAssetErrorCard(
    assetError: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    AppFeatureCard(
        title = stringResource(R.string.github_asset_error_title),
        subtitle = assetError,
        modifier = modifier,
        containerColor =
            GitHubStatusPalette
                .tonedSurface(
                    GitHubStatusPalette.Error,
                    isDark = isDark,
                ).copy(alpha = if (isDark) 0.84f else 0.96f),
        borderColor = GitHubStatusPalette.Error.copy(alpha = if (isDark) 0.34f else 0.22f),
        titleColor = GitHubStatusPalette.Error,
        subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant,
        titleMaxLines = Int.MAX_VALUE,
        subtitleMaxLines = Int.MAX_VALUE,
        titleTypography = AppTypographyTokens.BodyEmphasis,
        subtitleTypography = AppTypographyTokens.Supporting,
        headerTextVerticalSpacing = CardLayoutRhythm.compactSectionGap,
        showIndication = false,
        contentPadding = PaddingValues(0.dp),
    ) {}
}
