@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.data.remote.GitHubReleaseAssetFile
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.asset.assetIsPreferredForDevice
import os.kei.ui.page.main.github.asset.assetLikelyCompatibleWithDevice
import os.kei.ui.page.main.github.asset.formatAssetSize
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.sheet.SheetChoiceCard
import os.kei.ui.page.main.widget.sheet.SheetChoiceCardDensity
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val GitHubShareAssetChoiceContentPadding =
    PaddingValues(horizontal = 10.dp, vertical = 4.dp)
private val GitHubShareAssetChoicePressSafePadding = 2.dp
private val GitHubShareAssetBadgeMaxWidth = 112.dp

@Immutable
internal data class GitHubShareAssetChoiceColors(
    val containerColor: Color,
    val borderColor: Color,
    val titleColor: Color,
)

@Composable
internal fun gitHubShareAssetChoiceColors(
    selected: Boolean,
    isDark: Boolean,
): GitHubShareAssetChoiceColors =
    if (selected) {
        GitHubShareAssetChoiceColors(
            containerColor = GitHubStatusPalette.tonedSurface(GitHubStatusPalette.Active, isDark),
            borderColor = GitHubStatusPalette.Active.copy(alpha = 0.30f),
            titleColor = GitHubStatusPalette.Active,
        )
    } else {
        GitHubShareAssetChoiceColors(
            containerColor =
                MiuixTheme.colorScheme.surfaceContainer.copy(
                    alpha = if (isDark) 0.38f else 0.48f,
                ),
            borderColor =
                MiuixTheme.colorScheme.onBackgroundVariant.copy(
                    alpha = if (isDark) 0.16f else 0.12f,
                ),
            titleColor = MiuixTheme.colorScheme.onBackground,
        )
    }

@Composable
internal fun GitHubShareImportAssetPickerList(
    assets: List<GitHubReleaseAssetFile>,
    supportedAbis: List<String>,
    selectedIndex: Int,
    selectionEnabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectableGroup()
                .heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(
            items = assets,
            key = { _, asset -> asset.name },
            contentType = { _, _ -> "github_share_asset" },
        ) { index, asset ->
            GitHubShareImportAssetPickerRow(
                asset = asset,
                supportedAbis = supportedAbis,
                selected = selectedIndex == index,
                selectionEnabled = selectionEnabled,
                onSelect = { onSelect(index) },
            )
        }
    }
}

@Composable
internal fun GitHubShareImportAssetPickerRow(
    asset: GitHubReleaseAssetFile,
    supportedAbis: List<String>,
    selected: Boolean,
    selectionEnabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors =
        gitHubShareAssetChoiceColors(
            selected = selected,
            isDark = isAppInDarkTheme(),
        )
    val preferredForDevice = assetIsPreferredForDevice(asset.name, supportedAbis)
    val likelyCompatible = assetLikelyCompatibleWithDevice(asset.name, supportedAbis)
    val compatibilityHint =
        if (!likelyCompatible) {
            stringResource(R.string.github_share_import_dialog_asset_hint_maybe_incompatible)
        } else {
            null
        }
    val baseAssetSummary =
        stringResource(
            R.string.github_share_import_dialog_asset_summary,
            formatAssetSize(asset.sizeBytes, context),
            if (asset.apiAssetUrl.isNotBlank()) {
                stringResource(R.string.github_asset_fetch_source_api)
            } else {
                stringResource(R.string.github_asset_transport_direct)
            },
        )
    val assetSummary =
        compatibilityHint?.let { hint ->
            "$baseAssetSummary · $hint"
        } ?: baseAssetSummary
    SheetChoiceCard(
        title = asset.name,
        summary = assetSummary,
        selected = selected,
        enabled = selectionEnabled,
        onSelect = onSelect,
        modifier = modifier.fillMaxWidth(),
        density = SheetChoiceCardDensity.Compact,
        pressSafePadding = GitHubShareAssetChoicePressSafePadding,
        contentPadding = GitHubShareAssetChoiceContentPadding,
        selectedAccentColor = GitHubStatusPalette.Active,
        unselectedTitleColor = colors.titleColor,
        summaryColor = MiuixTheme.colorScheme.onBackgroundVariant,
        selectedLabel = null,
        containerColor = colors.containerColor,
        borderColor = colors.borderColor,
        trailing = {
            GitHubShareImportAssetCompatibilityBadge(
                preferredForDevice = preferredForDevice,
                likelyCompatible = likelyCompatible,
            )
        },
        showIndicator = false,
    )
}

@Composable
private fun GitHubShareImportAssetCompatibilityBadge(
    preferredForDevice: Boolean,
    likelyCompatible: Boolean,
) {
    when {
        preferredForDevice -> {
            StatusPill(
                label = stringResource(R.string.github_share_import_dialog_asset_badge_recommended),
                color = GitHubStatusPalette.Update,
                modifier = Modifier.widthIn(max = GitHubShareAssetBadgeMaxWidth),
                size = AppStatusPillSize.Compact,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        !likelyCompatible -> {
            StatusPill(
                label = stringResource(R.string.github_share_import_dialog_asset_badge_incompatible),
                color = GitHubStatusPalette.PreRelease,
                modifier = Modifier.widthIn(max = GitHubShareAssetBadgeMaxWidth),
                size = AppStatusPillSize.Compact,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
