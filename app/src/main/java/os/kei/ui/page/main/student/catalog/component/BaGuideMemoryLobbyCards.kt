@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.core.ext.showToast
import os.kei.core.intent.SafeExternalIntents
import os.kei.ui.page.main.os.appLucideChevronDownIcon
import os.kei.ui.page.main.os.appLucideChevronUpIcon
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucideFullscreenIcon
import os.kei.ui.page.main.os.appLucideHeartIcon
import os.kei.ui.page.main.student.BaGuideGalleryItem
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.isRenderableGalleryImageUrl
import os.kei.ui.page.main.student.section.GuideGalleryCardItem
import os.kei.ui.page.main.student.section.gallery.GuideImageFullscreenDialog
import os.kei.ui.page.main.widget.core.AppCompactIconAction
import os.kei.ui.page.main.widget.core.AppOverviewCard
import os.kei.ui.page.main.widget.core.AppOverviewMetricTile
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import os.kei.ui.page.main.widget.status.AppStatusColors
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BaGuideMemoryLobbyHeader(
    totalCount: Int,
    displayedCount: Int,
    readyCount: Int,
    favoriteCount: Int,
    loadingCount: Int,
    searchActive: Boolean,
    accent: Color,
) {
    val matchedCount = if (searchActive) displayedCount else totalCount
    AppOverviewCard(
        title = stringResource(R.string.ba_catalog_memory_lobby_title),
        subtitle = stringResource(R.string.ba_catalog_memory_lobby_overview_subtitle),
        containerColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.62f),
        borderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
        contentVerticalSpacing = CardLayoutRhythm.denseSectionGap,
        headerEndActions = {
            if (loadingCount > 0) {
                StatusPill(
                    label = stringResource(R.string.ba_catalog_memory_lobby_resolving_count, loadingCount),
                    color = AppStatusColors.Refreshing,
                    size = AppStatusPillSize.Compact,
                )
            }
            StatusPill(
                label = stringResource(R.string.ba_catalog_memory_lobby_ready_count, readyCount.coerceAtLeast(0)),
                color = accent,
                size = AppStatusPillSize.Compact,
            )
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.metricRowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppOverviewMetricTile(
                label =
                    stringResource(
                        if (searchActive) {
                            R.string.ba_catalog_student_bgm_metric_matched
                        } else {
                            R.string.ba_catalog_student_bgm_metric_students
                        },
                    ),
                value = matchedCount.coerceAtLeast(0).toString(),
                valueColor = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            AppOverviewMetricTile(
                label = stringResource(R.string.ba_catalog_student_bgm_metric_favorites),
                value = favoriteCount.coerceAtLeast(0).toString(),
                valueColor = Color(0xFFEC4899),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun BaGuideMemoryLobbyCard(
    entry: BaGuideCatalogEntry,
    lookupState: BaGuideMemoryLobbyLookupState,
    expanded: Boolean,
    favorite: Boolean,
    accent: Color,
    mediaAdaptiveRotationEnabled: Boolean,
    onToggleExpanded: () -> Unit,
    onResolve: () -> Unit,
    onOpenGuide: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    LaunchedEffect(expanded, lookupState, entry.contentId) {
        if (expanded && lookupState == BaGuideMemoryLobbyLookupState.Idle) {
            onResolve()
        }
    }
    var fullscreenImageUrl by remember(entry.contentId) { mutableStateOf<String?>(null) }
    val ready = lookupState as? BaGuideMemoryLobbyLookupState.Ready
    val isLoading = lookupState == BaGuideMemoryLobbyLookupState.Loading
    val isMissing = lookupState == BaGuideMemoryLobbyLookupState.Missing
    val firstFullscreenImageUrl =
        remember(ready?.item?.galleryItems) {
            ready
                ?.item
                ?.galleryItems
                .orEmpty()
                .firstFullscreenImageUrl()
        }
    val borderColor =
        when {
            favorite -> Color(0xFFEC4899).copy(alpha = 0.34f)
            ready != null -> accent.copy(alpha = 0.30f)
            else -> MiuixTheme.colorScheme.outline.copy(alpha = 0.16f)
        }
    val containerColor =
        when {
            favorite -> Color(0xFFEC4899).copy(alpha = 0.08f)
            ready != null -> accent.copy(alpha = 0.10f)
            else -> MiuixTheme.colorScheme.surface.copy(alpha = 0.58f)
        }
    val neutralTint = MiuixTheme.colorScheme.onBackgroundVariant
    AppSurfaceCard(
        containerColor = containerColor,
        borderColor = borderColor,
        onClick = {
            onToggleExpanded()
            if (!expanded) onResolve()
        },
        onLongClick = onOpenGuide,
        clipContent = true,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.infoRowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BaGuideCatalogEntryAvatar(
                        imageUrl = entry.iconUrl,
                        fallbackRes = R.drawable.ba_tab_skill,
                        size = 48.dp,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = entry.name,
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.CompactTitle.fontSize,
                        lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                        fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (entry.aliasDisplay.isNotBlank()) {
                        Text(
                            text = entry.aliasDisplay,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = AppTypographyTokens.Supporting.fontSize,
                            lineHeight = AppTypographyTokens.Supporting.lineHeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BaGuideMemoryLobbyStatusRow(
                        lookupState = lookupState,
                        resolvedItem = ready?.item,
                    )
                }
                Row(
                    modifier = Modifier.width(if (expanded && firstFullscreenImageUrl.isNotBlank()) 156.dp else 118.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppCompactIconAction(
                        icon = appLucideHeartIcon(),
                        contentDescription =
                            stringResource(
                                if (favorite) {
                                    R.string.ba_catalog_cd_unfavorite_student
                                } else {
                                    R.string.ba_catalog_cd_favorite_student
                                },
                            ),
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(38.dp),
                        tint = if (favorite) Color(0xFFEC4899) else neutralTint,
                        minSize = 38.dp,
                    )
                    if (expanded && firstFullscreenImageUrl.isNotBlank()) {
                        AppCompactIconAction(
                            icon = appLucideFullscreenIcon(),
                            contentDescription = stringResource(R.string.ba_catalog_memory_lobby_action_fullscreen),
                            onClick = { fullscreenImageUrl = firstFullscreenImageUrl },
                            modifier = Modifier.size(38.dp),
                            tint = accent,
                            minSize = 38.dp,
                        )
                    }
                    AppCompactIconAction(
                        icon = appLucideExternalLinkIcon(),
                        contentDescription = stringResource(R.string.ba_catalog_memory_lobby_action_open_guide),
                        onClick = onOpenGuide,
                        modifier = Modifier.size(38.dp),
                        tint = neutralTint,
                        minSize = 38.dp,
                    )
                    AppCompactIconAction(
                        icon = if (expanded) appLucideChevronUpIcon() else appLucideChevronDownIcon(),
                        contentDescription =
                            stringResource(
                                if (expanded) {
                                    R.string.ba_catalog_memory_lobby_action_collapse
                                } else {
                                    R.string.ba_catalog_memory_lobby_action_expand
                                },
                            ),
                        onClick = {
                            onToggleExpanded()
                            if (!expanded) onResolve()
                        },
                        modifier = Modifier.size(38.dp),
                        tint = if (expanded) accent else neutralTint,
                        minSize = 38.dp,
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = appExpandIn(),
                exit = appExpandOut(),
            ) {
                BaGuideMemoryLobbyExpandedContent(
                    lookupState = lookupState,
                    mediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled,
                )
            }
        }
    }
    fullscreenImageUrl?.let { imageUrl ->
        GuideImageFullscreenDialog(
            imageUrl = imageUrl,
            mediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled,
            onDismiss = { fullscreenImageUrl = null },
        )
    }
}

@Composable
private fun BaGuideMemoryLobbyStatusRow(
    lookupState: BaGuideMemoryLobbyLookupState,
    resolvedItem: BaGuideMemoryLobbyResolvedItem?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val statusLabel =
            when (lookupState) {
                BaGuideMemoryLobbyLookupState.Idle -> R.string.ba_catalog_memory_lobby_status_idle
                BaGuideMemoryLobbyLookupState.Loading -> R.string.ba_catalog_memory_lobby_status_resolving
                BaGuideMemoryLobbyLookupState.Missing -> R.string.ba_catalog_memory_lobby_status_missing
                is BaGuideMemoryLobbyLookupState.Ready -> R.string.ba_catalog_memory_lobby_status_ready
            }
        val statusColor =
            when (lookupState) {
                BaGuideMemoryLobbyLookupState.Idle -> MiuixTheme.colorScheme.onBackgroundVariant
                BaGuideMemoryLobbyLookupState.Loading -> AppStatusColors.Refreshing
                BaGuideMemoryLobbyLookupState.Missing -> AppStatusColors.Failed
                is BaGuideMemoryLobbyLookupState.Ready -> AppStatusColors.Fresh
            }
        StatusPill(
            label = stringResource(statusLabel),
            color = statusColor,
            size = AppStatusPillSize.Compact,
        )
        if (resolvedItem?.fromCache == true) {
            StatusPill(
                label = stringResource(R.string.ba_catalog_student_bgm_status_cached_detail),
                color = MiuixTheme.colorScheme.primary,
                size = AppStatusPillSize.Compact,
            )
        }
    }
}

@Composable
private fun BaGuideMemoryLobbyExpandedContent(
    lookupState: BaGuideMemoryLobbyLookupState,
    mediaAdaptiveRotationEnabled: Boolean,
) {
    val context = LocalContext.current
    val openFailed = stringResource(R.string.common_open_link_failed)
    when (lookupState) {
        BaGuideMemoryLobbyLookupState.Idle,
        BaGuideMemoryLobbyLookupState.Loading,
        -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiquidCircularProgressBar(
                    progress = { 0.35f },
                    size = 16.dp,
                    strokeWidth = 2.dp,
                    activeColor = MiuixTheme.colorScheme.primary,
                    inactiveColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.30f),
                )
                Text(
                    text = stringResource(R.string.ba_catalog_memory_lobby_loading_body),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                )
            }
        }

        BaGuideMemoryLobbyLookupState.Missing -> {
            Text(
                text = stringResource(R.string.ba_catalog_memory_lobby_missing_body),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
            )
        }

        is BaGuideMemoryLobbyLookupState.Ready -> {
            val item = lookupState.item
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (item.memoryUnlockLevel.isNotBlank()) {
                    Text(
                        text = stringResource(
                            R.string.ba_catalog_memory_lobby_unlock_level,
                            item.memoryUnlockLevel,
                        ),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    )
                }
                item.galleryItems.forEach { galleryItem ->
                    GuideGalleryCardItem(
                        item = galleryItem,
                        backdrop = null,
                        onOpenMedia = { rawUrl ->
                            if (!SafeExternalIntents.startBrowsableUrl(context, rawUrl, newTask = true)) {
                                context.showToast(openFailed)
                            }
                        },
                        embedded = true,
                        showSaveAction = false,
                        showBgmFavoriteAction = false,
                        showAudioLoopAction = false,
                        bgmFavoriteStudentTitle = item.studentTitle,
                        bgmFavoriteStudentImageUrl = item.studentImageUrl,
                        bgmFavoriteSourceUrl = item.sourceUrl,
                        mediaAdaptiveRotationEnabled = mediaAdaptiveRotationEnabled,
                    )
                }
            }
        }
    }
}

private fun List<BaGuideGalleryItem>.firstFullscreenImageUrl(): String =
    firstOrNull { item ->
        val type = item.mediaType.lowercase()
        type != "video" &&
            type != "audio" &&
            (
                isRenderableGalleryImageUrl(item.imageUrl) ||
                    isRenderableGalleryImageUrl(item.mediaUrl)
            )
    }?.let { item ->
        item.imageUrl.ifBlank { item.mediaUrl }
    }.orEmpty()
