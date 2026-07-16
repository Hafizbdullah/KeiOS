@file:Suppress("FunctionName")

package os.kei.ui.page.main.github

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.model.InstalledAppItem
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.rememberAppStatusPillMetrics
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import os.kei.ui.page.main.widget.shape.appSquircleBorder
import os.kei.ui.page.main.widget.shape.appSquircleClip
import os.kei.ui.page.main.widget.sheet.SheetChoiceCard
import os.kei.ui.page.main.widget.sheet.SheetChoiceCardDensity
import os.kei.ui.page.main.widget.sheet.SheetSurfaceCard
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
internal data class GitHubAppCandidateColors(
    val containerColor: Color,
    val borderColor: Color,
    val titleColor: Color,
)

@Composable
internal fun gitHubAppCandidateColors(
    selected: Boolean,
    isDark: Boolean,
): GitHubAppCandidateColors =
    if (selected) {
        GitHubAppCandidateColors(
            containerColor = GitHubStatusPalette.tonedSurface(GitHubStatusPalette.Update, isDark),
            borderColor = GitHubStatusPalette.Update.copy(alpha = 0.3f),
            titleColor = GitHubStatusPalette.Update,
        )
    } else {
        GitHubAppCandidateColors(
            containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f),
            borderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.12f),
            titleColor = MiuixTheme.colorScheme.primary,
        )
    }

@Composable
internal fun GitHubSelectedAppCard(
    selectedApp: InstalledAppItem,
    showInstallSource: Boolean = false,
) {
    val appIconBitmaps = LocalGitHubAppIconBitmaps.current
    SheetSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor =
            GitHubStatusPalette.tonedSurface(
                GitHubStatusPalette.Update,
                isDark = isAppInDarkTheme(),
            ),
        borderColor = GitHubStatusPalette.Update.copy(alpha = 0.28f),
        verticalSpacing = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val packageName = selectedApp.packageName.trim()
            AppIconImage(
                packageName = packageName,
                bitmap = appIconBitmaps[packageName],
                size = 38.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = selectedApp.label,
                    color = MiuixTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = selectedApp.packageName,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showInstallSource) {
                InstallSourcePill(label = selectedApp.installSourceDisplayLabel())
            }
        }
    }
}

@Composable
internal fun GitHubAppCandidateRow(
    app: InstalledAppItem,
    selected: Boolean,
    showInstallSource: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appIconBitmaps = LocalGitHubAppIconBitmaps.current
    val colors = gitHubAppCandidateColors(selected = selected, isDark = isAppInDarkTheme())
    SheetChoiceCard(
        title = app.label,
        summary = app.packageName,
        selected = selected,
        onSelect = onClick,
        modifier = modifier.fillMaxWidth(),
        density = SheetChoiceCardDensity.Compact,
        pressSafePadding = AppInteractiveTokens.compactLiquidPressSafePadding,
        selectedLabel = null,
        selectedAccentColor = GitHubStatusPalette.Update,
        unselectedTitleColor = colors.titleColor,
        containerColor = colors.containerColor,
        borderColor = colors.borderColor,
        leading = {
            val packageName = app.packageName.trim()
            AppIconImage(
                packageName = packageName,
                bitmap = appIconBitmaps[packageName],
                size = 32.dp,
            )
        },
        trailing =
            if (showInstallSource) {
                {
                    InstallSourcePill(
                        label = app.installSourceDisplayLabel(),
                        selected = selected,
                    )
                }
            } else {
                null
            },
        showIndicator = false,
    )
}

@Composable
private fun InstalledAppItem.installSourceDisplayLabel(): String =
    installSourceLabel
        .ifBlank { installSourcePackageName }
        .ifBlank { stringResource(R.string.github_track_sheet_app_install_source_unknown) }

@Composable
private fun InstallSourcePill(
    label: String,
    selected: Boolean = false,
) {
    val color = if (selected) GitHubStatusPalette.Update else MiuixTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()
    val metrics = rememberAppStatusPillMetrics(AppStatusPillSize.Compact)
    Box(
        modifier =
            Modifier
                .widthIn(max = 156.dp)
                .appSquircleBackground(color.copy(alpha = if (isDark) 0.16f else 0.2f), 999.dp)
                .appSquircleBorder(
                    width = 0.8.dp,
                    color = color.copy(alpha = if (isDark) 0.32f else 0.4f),
                    cornerRadius = 999.dp,
                ).padding(metrics.contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isDark) color else color.copy(alpha = 0.96f),
            fontSize = metrics.typography.fontSize,
            lineHeight = metrics.typography.lineHeight,
            fontWeight = metrics.typography.fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun AppIconImage(
    packageName: String,
    bitmap: Bitmap?,
    size: Dp,
) {
    val normalizedPackageName = packageName.trim()
    if (bitmap != null) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            bitmap = imageBitmap,
            contentDescription = normalizedPackageName,
            modifier =
                Modifier
                    .width(size)
                    .height(size)
                    .appSquircleClip(999.dp),
        )
    } else {
        AppIconFallback(size = size)
    }
}

@Composable
private fun AppIconFallback(size: Dp) {
    Box(
        modifier =
            Modifier
                    .width(size)
                    .height(size)
                    .appSquircleClip(999.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.github_strategy_app_fallback),
            color = MiuixTheme.colorScheme.primary,
            fontSize = AppTypographyTokens.Caption.fontSize,
            lineHeight = AppTypographyTokens.Caption.lineHeight,
        )
    }
}
