@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.component

import android.graphics.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.core.ext.showToast
import os.kei.ui.page.main.os.appLucideChevronDownIcon
import os.kei.ui.page.main.os.appLucideChevronUpIcon
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucideFullscreenIcon
import os.kei.ui.page.main.os.appLucideHeartIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.student.BaGuideGalleryItem
import os.kei.ui.page.main.student.GuideVideoFullscreenActivity
import os.kei.ui.page.main.student.GuideRemoteImageAdaptive
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.normalizeGuideMediaSource
import os.kei.ui.page.main.student.isRenderableGalleryImageUrl
import os.kei.ui.page.main.student.section.gallery.GuideImageFullscreenDialog
import os.kei.ui.page.main.widget.core.AppCompactIconAction
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppDropdownAnchorButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownActionItem
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownColumn
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownSingleChoiceItem
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.motion.appExpandIn
import os.kei.ui.page.main.widget.motion.appExpandOut
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import os.kei.ui.page.main.widget.sheet.capturePopupAnchor
import os.kei.ui.page.main.widget.status.AppStatusColors
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private val MemoryLobbyMoreMenuMinWidth = 156.dp
private val MemoryLobbyMoreMenuMaxWidth = 196.dp
private val MemoryLobbyMoreMenuMaxHeight = 240.dp
private val MemoryLobbyVariantMenuMinWidth = 136.dp
private val MemoryLobbyVariantMenuMaxWidth = 196.dp
private val MemoryLobbyVariantMenuMaxHeight = 220.dp
private const val MemoryLobbyPreviewDecodeMinPx = 720
private const val MemoryLobbyPreviewDecodeMaxPx = 1280

@Composable
internal fun BaGuideMemoryLobbyHeader(
    totalCount: Int,
    displayedCount: Int,
    readyCount: Int,
    favoriteCount: Int,
    cachedCount: Int,
    searchActive: Boolean,
    accent: Color,
) {
    val matchedCount = if (searchActive) displayedCount else totalCount
    AppSurfaceCard(
        containerColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.62f),
        borderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.16f),
        showIndication = false,
    ) {
        BaGuideMemoryLobbyHeaderMetrics(
            matchedLabel =
                stringResource(
                    if (searchActive) {
                        R.string.ba_catalog_student_bgm_metric_matched
                    } else {
                        R.string.ba_catalog_student_bgm_metric_students
                    },
                ),
            matchedCount = matchedCount,
            favoriteLabel = stringResource(R.string.ba_catalog_student_bgm_metric_favorites),
            favoriteCount = favoriteCount,
            readyLabel = stringResource(R.string.ba_catalog_memory_lobby_status_ready),
            readyCount = readyCount,
            cachedLabel = stringResource(R.string.ba_catalog_student_bgm_status_cached_detail),
            cachedCount = cachedCount,
            accent = accent,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BaGuideMemoryLobbyHeaderMetrics(
    matchedLabel: String,
    matchedCount: Int,
    favoriteLabel: String,
    favoriteCount: Int,
    readyLabel: String,
    readyCount: Int,
    cachedLabel: String,
    cachedCount: Int,
    accent: Color,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        maxItemsInEachRow = 4,
    ) {
        BaGuideMemoryLobbyMetricPill(
            label = matchedLabel,
            value = matchedCount,
            color = MiuixTheme.colorScheme.onBackground,
        )
        BaGuideMemoryLobbyMetricPill(
            label = favoriteLabel,
            value = favoriteCount,
            color = Color(0xFFEC4899),
        )
        BaGuideMemoryLobbyMetricPill(
            label = readyLabel,
            value = readyCount,
            color = accent,
        )
        BaGuideMemoryLobbyMetricPill(
            label = cachedLabel,
            value = cachedCount,
            color = MiuixTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun BaGuideMemoryLobbyMetricPill(
    label: String,
    value: Int,
    color: Color,
) {
    StatusPill(
        label = "$label ${value.coerceAtLeast(0)}",
        color = color,
        size = AppStatusPillSize.Compact,
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 3.dp),
    )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = entry.name,
                            modifier = Modifier.weight(1f),
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = AppTypographyTokens.CompactTitle.fontSize,
                            lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                            fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BaGuideMemoryLobbyStatusRow(
                            lookupState = lookupState,
                            resolvedItem = ready?.item,
                        )
                    }
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
                }
                Row(
                    modifier = Modifier.width(82.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BaGuideMemoryLobbyMoreActions(
                        favorite = favorite,
                        firstFullscreenImageUrl = firstFullscreenImageUrl,
                        iconTint = neutralTint,
                        onToggleFavorite = onToggleFavorite,
                        onOpenFullscreen = { fullscreenImageUrl = firstFullscreenImageUrl },
                        onOpenGuide = onOpenGuide,
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
                    accent = accent,
                    onOpenFullscreenImage = { imageUrl -> fullscreenImageUrl = imageUrl },
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
    accent: Color,
    onOpenFullscreenImage: (String) -> Unit,
) {
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
            val imageItems = item.galleryItems.filterNot(BaGuideGalleryItem::isMemoryLobbyVideo)
            val videoItems = item.galleryItems.filter(BaGuideGalleryItem::isMemoryLobbyVideo)
            val previewImageUrl = imageItems.firstFullscreenImageUrl()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (videoItems.isNotEmpty()) {
                    BaGuideMemoryLobbyVideoGroup(
                        items = videoItems,
                        previewFallbackUrl = previewImageUrl,
                        unlockLevel = item.memoryUnlockLevel,
                        accent = accent,
                    )
                } else if (previewImageUrl.isNotBlank()) {
                    BaGuideMemoryLobbyImagePreviewGroup(
                        previewImageUrl = previewImageUrl,
                        unlockLevel = item.memoryUnlockLevel,
                        accent = accent,
                        onOpenFullscreen = { onOpenFullscreenImage(previewImageUrl) },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.ba_catalog_memory_lobby_missing_body),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    )
                }
            }
        }
    }
}

@Composable
private fun BaGuideMemoryLobbyMoreActions(
    favorite: Boolean,
    firstFullscreenImageUrl: String,
    iconTint: Color,
    onToggleFavorite: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var menuAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    val moreIcon = appLucideMoreIcon()
    val favoriteIcon = appLucideHeartIcon()
    val fullscreenIcon = appLucideFullscreenIcon()
    val openGuideIcon = appLucideExternalLinkIcon()
    val optionSize = 2 + if (firstFullscreenImageUrl.isNotBlank()) 1 else 0
    Box(
        modifier = Modifier.capturePopupAnchor { bounds -> menuAnchorBounds = bounds },
        contentAlignment = Alignment.Center,
    ) {
        AppCompactIconAction(
            icon = moreIcon,
            contentDescription = stringResource(R.string.ba_catalog_memory_lobby_action_more),
            onClick = { menuExpanded = !menuExpanded },
            modifier = Modifier.size(38.dp),
            tint = iconTint,
            minSize = 38.dp,
        )
        if (menuExpanded) {
            SnapshotWindowListPopup(
                show = true,
                alignment = PopupPositionProvider.Align.BottomEnd,
                anchorBounds = menuAnchorBounds,
                placement = SnapshotPopupPlacement.ButtonEnd,
                enableWindowDim = false,
                onDismissRequest = { menuExpanded = false },
            ) {
                LiquidGlassDropdownColumn(
                    minWidth = MemoryLobbyMoreMenuMinWidth,
                    maxWidth = MemoryLobbyMoreMenuMaxWidth,
                    maxHeight = MemoryLobbyMoreMenuMaxHeight,
                ) {
                    var index = 0
                    BaGuideMemoryLobbyMenuAction(
                        text =
                            stringResource(
                                if (favorite) {
                                    R.string.ba_catalog_memory_lobby_menu_unfavorite
                                } else {
                                    R.string.ba_catalog_memory_lobby_menu_favorite
                                },
                            ),
                        leadingIcon = favoriteIcon,
                        index = index++,
                        optionSize = optionSize,
                        onClick = {
                            menuExpanded = false
                            onToggleFavorite()
                        },
                    )
                    if (firstFullscreenImageUrl.isNotBlank()) {
                        BaGuideMemoryLobbyMenuAction(
                            text = stringResource(R.string.ba_catalog_memory_lobby_action_fullscreen),
                            leadingIcon = fullscreenIcon,
                            index = index++,
                            optionSize = optionSize,
                            onClick = {
                                menuExpanded = false
                                onOpenFullscreen()
                            },
                        )
                    }
                    BaGuideMemoryLobbyMenuAction(
                        text = stringResource(R.string.ba_catalog_memory_lobby_action_open_guide),
                        leadingIcon = openGuideIcon,
                        index = index,
                        optionSize = optionSize,
                        onClick = {
                            menuExpanded = false
                            onOpenGuide()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BaGuideMemoryLobbyMenuAction(
    text: String,
    leadingIcon: ImageVector,
    index: Int,
    optionSize: Int,
    onClick: () -> Unit,
) {
    LiquidGlassDropdownActionItem(
        text = text,
        leadingIcon = leadingIcon,
        index = index,
        optionSize = optionSize,
        variant = GlassVariant.SheetAction,
        onClick = onClick,
    )
}

@Composable
private fun BaGuideMemoryLobbyVideoGroup(
    items: List<BaGuideGalleryItem>,
    previewFallbackUrl: String,
    unlockLevel: String,
    accent: Color,
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val itemKey = remember(items) {
        items.joinToString(separator = "|") { item ->
            item.mediaUrl.ifBlank { item.imageUrl }
        }
    }
    var selectedIndex by rememberSaveable(itemKey) { mutableStateOf(0) }
    LaunchedEffect(items.size) {
        if (selectedIndex !in items.indices) selectedIndex = 0
    }
    val selectedItem = items.getOrElse(selectedIndex) { items.first() }
    val displayMediaUrl = remember(selectedItem.mediaUrl) { normalizeGuideMediaSource(selectedItem.mediaUrl) }
    val displayPreviewUrl =
        remember(selectedItem.imageUrl, previewFallbackUrl) {
            val staticPreview = previewFallbackUrl.takeIf(::isRenderableGalleryImageUrl).orEmpty()
            val videoPreview = selectedItem.imageUrl.takeIf(::isRenderableGalleryImageUrl).orEmpty()
            normalizeGuideMediaSource(staticPreview.ifBlank { videoPreview })
        }
    var videoSourceRect by remember(displayMediaUrl) { mutableStateOf<Rect?>(null) }
    val optionLabels =
        if (items.size <= 1) {
            listOf(stringResource(R.string.guide_gallery_video_format, 1))
        } else {
            items.mapIndexed { index, item ->
                item.title
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.guide_gallery_video_format, index + 1)
            }
        }
    val openPictureInPicture = {
        if (displayMediaUrl.isBlank()) {
            context.showToast(context.getString(R.string.guide_media_video_url_invalid))
        } else {
            GuideVideoFullscreenActivity.launchInPictureInPicture(
                context = context,
                mediaUrl = displayMediaUrl,
                previewImageUrl = displayPreviewUrl,
                startPositionMs = 0L,
                sourceRectHint = videoSourceRect,
            )
        }
    }
    val openFullscreen = {
        if (displayMediaUrl.isBlank()) {
            context.showToast(context.getString(R.string.guide_media_video_url_invalid))
        } else {
            GuideVideoFullscreenActivity.launch(
                context = context,
                mediaUrl = displayMediaUrl,
                previewImageUrl = displayPreviewUrl,
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.guide_gallery_memorial_lobby),
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BaGuideMemoryLobbyUnlockPill(
                unlockLevel = unlockLevel,
                accent = accent,
            )
            BaGuideMemoryLobbyVariantSelector(
                optionLabels = optionLabels,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { selectedIndex = it },
            )
            if (displayMediaUrl.isNotBlank()) {
                AppCompactIconAction(
                    icon = appLucidePlayIcon(),
                    contentDescription = stringResource(R.string.guide_gallery_memorial_lobby_pip_play),
                    onClick = openPictureInPicture,
                    modifier = Modifier.size(38.dp),
                    tint = Color(0xFF3B82F6),
                    minSize = 38.dp,
                )
                AppCompactIconAction(
                    icon = appLucideFullscreenIcon(),
                    contentDescription = stringResource(R.string.guide_gallery_memorial_lobby_pip_fullscreen),
                    onClick = openFullscreen,
                    modifier = Modifier.size(38.dp),
                    tint = Color(0xFF3B82F6),
                    minSize = 38.dp,
                )
            }
        }
        if (displayMediaUrl.isBlank()) {
            Text(
                text = stringResource(R.string.guide_gallery_video_not_found),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
            )
        } else if (displayPreviewUrl.isNotBlank()) {
            BaGuideMemoryLobbyVideoPreview(
                previewImageUrl = displayPreviewUrl,
                onBoundsChanged = { rect -> videoSourceRect = rect },
                onClick = openPictureInPicture,
            )
        }
    }
}

@Composable
private fun BaGuideMemoryLobbyVariantSelector(
    optionLabels: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    if (optionLabels.size <= 1) return
    var showPicker by remember(optionLabels) { mutableStateOf(false) }
    var pickerPopupAnchorBounds by remember { mutableStateOf<IntRect?>(null) }
    Box(
        modifier = Modifier.capturePopupAnchor { pickerPopupAnchorBounds = it },
        contentAlignment = Alignment.Center,
    ) {
        AppDropdownAnchorButton(
            text = optionLabels.getOrElse(selectedIndex) {
                stringResource(R.string.guide_gallery_video_format, 1)
            },
            onClick = { showPicker = !showPicker },
            modifier = Modifier.widthIn(min = 54.dp, max = 96.dp),
            textColor = Color(0xFF3B82F6),
            variant = GlassVariant.Compact,
            horizontalPadding = 8.dp,
            textSize = AppTypographyTokens.Supporting.fontSize,
            textLineHeight = AppTypographyTokens.Supporting.lineHeight,
        )
        if (showPicker) {
            SnapshotWindowListPopup(
                show = true,
                alignment = PopupPositionProvider.Align.BottomEnd,
                anchorBounds = pickerPopupAnchorBounds,
                placement = SnapshotPopupPlacement.ButtonEnd,
                enableWindowDim = false,
                onDismissRequest = { showPicker = false },
            ) {
                LiquidGlassDropdownColumn(
                    minWidth = MemoryLobbyVariantMenuMinWidth,
                    maxWidth = MemoryLobbyVariantMenuMaxWidth,
                    maxHeight = MemoryLobbyVariantMenuMaxHeight,
                ) {
                    optionLabels.forEachIndexed { index, option ->
                        LiquidGlassDropdownSingleChoiceItem(
                            text = option,
                            optionSize = optionLabels.size,
                            isSelected = selectedIndex == index,
                            index = index,
                            accentColor = Color(0xFF3B82F6),
                            variant = GlassVariant.SheetAction,
                            textMaxLines = 1,
                            onSelectedIndexChange = { selected ->
                                onSelectedIndexChange(selected)
                                showPicker = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BaGuideMemoryLobbyImagePreviewGroup(
    previewImageUrl: String,
    unlockLevel: String,
    accent: Color,
    onOpenFullscreen: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.guide_gallery_memorial_lobby),
                color = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BaGuideMemoryLobbyUnlockPill(
                unlockLevel = unlockLevel,
                accent = accent,
            )
            AppCompactIconAction(
                icon = appLucideFullscreenIcon(),
                contentDescription = stringResource(R.string.ba_catalog_memory_lobby_action_fullscreen),
                onClick = onOpenFullscreen,
                modifier = Modifier.size(38.dp),
                tint = Color(0xFF3B82F6),
                minSize = 38.dp,
            )
        }
        BaGuideMemoryLobbyVideoPreview(
            previewImageUrl = previewImageUrl,
            onBoundsChanged = {},
            onClick = onOpenFullscreen,
        )
    }
}

@Composable
private fun BaGuideMemoryLobbyUnlockPill(
    unlockLevel: String,
    accent: Color,
) {
    val label = remember(unlockLevel) { unlockLevel.memoryLobbyUnlockLevelPillLabel() }
    if (label.isBlank()) return
    StatusPill(
        label = label,
        color = accent,
        size = AppStatusPillSize.Compact,
    )
}

@Composable
private fun BaGuideMemoryLobbyVideoPreview(
    previewImageUrl: String,
    onBoundsChanged: (Rect?) -> Unit,
    onClick: () -> Unit,
) {
    var loading by remember(previewImageUrl) { mutableStateOf(false) }
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    val rect =
                        Rect(
                            bounds.left.roundToInt(),
                            bounds.top.roundToInt(),
                            bounds.right.roundToInt(),
                            bounds.bottom.roundToInt(),
                        )
                    onBoundsChanged(rect.takeUnless { it.isEmpty })
                }
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val previewDecodeDimension =
            remember(maxWidth, density) {
                with(density) {
                    maxWidth
                        .roundToPx()
                        .coerceIn(MemoryLobbyPreviewDecodeMinPx, MemoryLobbyPreviewDecodeMaxPx)
                }
            }
        GuideRemoteImageAdaptive(
            imageUrl = previewImageUrl,
            maxDecodeDimension = previewDecodeDimension,
            onLoadingChanged = { loading = it },
        )
        if (loading) {
            LiquidCircularProgressBar(
                progress = { 0.35f },
                size = 22.dp,
                strokeWidth = 2.5.dp,
                activeColor = Color(0xFF3B82F6),
                inactiveColor = Color(0xFF3B82F6).copy(alpha = 0.24f),
            )
        }
    }
}

private fun String.memoryLobbyUnlockLevelPillLabel(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return ""
    return Regex("""\d+""").find(trimmed)?.value ?: trimmed
}

private fun BaGuideGalleryItem.isMemoryLobbyVideo(): Boolean =
    mediaType.equals("video", ignoreCase = true)

private fun List<BaGuideGalleryItem>.firstFullscreenImageUrl(): String =
    firstOrNull { item ->
        !item.isMemoryLobbyVideo() &&
            item.mediaType.lowercase() != "audio" &&
            (
                isRenderableGalleryImageUrl(item.imageUrl) ||
                    isRenderableGalleryImageUrl(item.mediaUrl)
            )
    }?.let { item ->
        item.imageUrl.ifBlank { item.mediaUrl }
    }.orEmpty()
