@file:Suppress("FunctionName")

package os.kei.ui.page.main.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle
import os.kei.R
import os.kei.feature.home.model.HomeOverviewCard
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.model.toHomeOverviewCardOrNull
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppSwitch
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionTitle
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max
import androidx.compose.ui.graphics.Shadow as ComposeTextShadow

private val HOME_KEI_TITLE_GRADIENT_COLORS =
    listOf(
        Color(0xFFFFD2DE),
        Color(0xFFFFCAD9),
        Color(0xFFFF99BB),
        Color(0xFFFF76A5),
        Color(0xFFFF6098),
        Color(0xFFFF5893),
    )
private val HOME_HERO_SHARED_AVOIDANCE_LIFT = 72.dp
private val HOME_HERO_TOP_PADDING = 36.dp
private val HOME_HERO_COMPACT_LANDSCAPE_TOP_PADDING = 12.dp
private val HOME_HERO_ICON_SIZE = 88.dp
private val HOME_HERO_COMPACT_LANDSCAPE_ICON_SIZE = 64.dp
private val HOME_HERO_TITLE_TOP_PADDING = 10.dp
private val HOME_HERO_COMPACT_LANDSCAPE_TITLE_TOP_PADDING = 4.dp
private val HOME_HERO_TITLE_BOTTOM_PADDING = 4.dp
private val HOME_HERO_COMPACT_LANDSCAPE_TITLE_BOTTOM_PADDING = 0.dp
private val HOME_HERO_SPACER_TRAILING_CLEARANCE = 90.dp
private val HOME_HERO_COMPACT_LANDSCAPE_SPACER_TRAILING_CLEARANCE = 12.dp
private const val HOME_HERO_AVOIDANCE_ALPHA_WEIGHT = 0.28f
private const val HOME_HERO_FOREGROUND_BLUR_RADIUS_DP = 50f

internal data class HomeHeaderStatusPillState(
    val label: String,
    val color: Color,
    val minWidth: Dp,
    val contentPadding: PaddingValues? = null,
)

@Composable
internal fun HomePageControlSheet(
    show: Boolean,
    actionBarBackdrop: Backdrop,
    visibleBottomPages: Set<BottomPage>,
    visibleOverviewCards: Set<HomeOverviewCard>,
    homeSheetTitle: String,
    tableTitle: String,
    tableDesc: String,
    homeCardMcp: String,
    homeCardGitHub: String,
    homeCardWebDav: String,
    homeCardBa: String,
    showCacheFreshnessInCards: Boolean,
    cacheFreshnessToggleLabel: String,
    cacheFreshnessToggleDesc: String,
    debugSectionTitle: String,
    onDismissRequest: () -> Unit,
    onBottomPageVisibilityChange: (BottomPage, Boolean) -> Unit,
    onOverviewCardVisibilityChange: (HomeOverviewCard, Boolean) -> Unit,
    onCacheFreshnessVisibilityChange: (Boolean) -> Unit,
) {
    SnapshotWindowBottomSheet(
        show = show,
        title = homeSheetTitle,
        onDismissRequest = onDismissRequest,
        startAction = {
            AppLiquidIconButton(
                backdrop = actionBarBackdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription =
                    androidx.compose.ui.res
                        .stringResource(R.string.common_close),
                onClick = onDismissRequest,
            )
        },
    ) {
        SheetContentColumn(
            scrollable = true,
            verticalSpacing = 10.dp,
        ) {
            SheetSectionTitle(tableTitle)
            SheetSectionCard(verticalSpacing = 8.dp) {
                HomePageVisibilityTableHeader()
                HomePageVisibilityTableRow(
                    page = BottomPage.Home,
                    cardLabel = null,
                    bottomVisible = true,
                    cardVisible = false,
                    bottomFixed = true,
                    cardAvailable = false,
                    onBottomVisibleChange = {},
                    onCardVisibleChange = {},
                )
                BottomPage.entries
                    .filter { it != BottomPage.Home }
                    .forEach { page ->
                        val overviewCard = page.toHomeOverviewCardOrNull()
                        val bottomVisible = visibleBottomPages.contains(page)
                        HomePageVisibilityTableRow(
                            page = page,
                            cardLabel =
                                when (overviewCard) {
                                    HomeOverviewCard.MCP -> homeCardMcp
                                    HomeOverviewCard.GITHUB -> homeCardGitHub
                                    HomeOverviewCard.WEBDAV -> homeCardWebDav
                                    HomeOverviewCard.BA -> homeCardBa
                                    null -> null
                                },
                            bottomVisible = bottomVisible,
                            cardVisible = overviewCard?.let(visibleOverviewCards::contains) == true,
                            bottomFixed = false,
                            cardAvailable = overviewCard != null && bottomVisible,
                            onBottomVisibleChange = { checked ->
                                onBottomPageVisibilityChange(page, checked)
                            },
                            onCardVisibleChange = { checked ->
                                if (overviewCard != null) {
                                    onOverviewCardVisibilityChange(overviewCard, checked)
                                }
                            },
                        )
                    }
                HomePageStandaloneCardVisibilityRow(
                    label = homeCardWebDav,
                    cardVisible = visibleOverviewCards.contains(HomeOverviewCard.WEBDAV),
                    onCardVisibleChange = { checked ->
                        onOverviewCardVisibilityChange(HomeOverviewCard.WEBDAV, checked)
                    },
                )
                SheetDescriptionText(text = tableDesc)
            }
            SheetSectionTitle(debugSectionTitle)
            SheetSectionCard(verticalSpacing = 10.dp) {
                SheetControlRow(label = cacheFreshnessToggleLabel) {
                    AppSwitch(
                        checked = showCacheFreshnessInCards,
                        onCheckedChange = onCacheFreshnessVisibilityChange,
                    )
                }
                SheetDescriptionText(text = cacheFreshnessToggleDesc)
            }
        }
    }
}

@Composable
private fun HomePageStandaloneCardVisibilityRow(
    label: String,
    cardVisible: Boolean,
    onCardVisibleChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1.35f),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            StatusPill(
                label =
                    androidx.compose.ui.res
                        .stringResource(R.string.home_sheet_bottom_unavailable),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            AppSwitch(
                checked = cardVisible,
                onCheckedChange = onCardVisibleChange,
            )
        }
    }
}

@Composable
private fun HomePageVisibilityTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text =
                androidx.compose.ui.res
                    .stringResource(R.string.home_sheet_column_section),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.35f),
        )
        Text(
            text =
                androidx.compose.ui.res
                    .stringResource(R.string.home_sheet_column_bottom),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(
            text =
                androidx.compose.ui.res
                    .stringResource(R.string.home_sheet_column_card),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomePageVisibilityTableRow(
    page: BottomPage,
    cardLabel: String?,
    bottomVisible: Boolean,
    cardVisible: Boolean,
    bottomFixed: Boolean,
    cardAvailable: Boolean,
    onBottomVisibleChange: (Boolean) -> Unit,
    onCardVisibleChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1.35f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            HomeBottomPageLabel(
                page = page,
                modifier = Modifier.defaultMinSize(minHeight = 24.dp),
            )
            if (cardLabel != null && cardLabel != page.label) {
                Text(
                    text = cardLabel,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                )
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (bottomFixed) {
                StatusPill(
                    label =
                        androidx.compose.ui.res
                            .stringResource(R.string.common_status_fixed_visible),
                    color = Color(0xFF2563EB),
                )
            } else {
                AppSwitch(
                    checked = bottomVisible,
                    onCheckedChange = onBottomVisibleChange,
                )
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (cardAvailable) {
                AppSwitch(
                    checked = cardVisible,
                    onCheckedChange = onCardVisibleChange,
                )
            } else {
                StatusPill(
                    label =
                        androidx.compose.ui.res
                            .stringResource(R.string.home_sheet_card_unavailable),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
    }
}

@Composable
internal fun HomePageHero(
    foregroundBackdrop: LayerBackdrop?,
    foregroundBlurEnabled: Boolean,
    homeIconHdrEnabled: Boolean,
    hdrSweepProgress: () -> Float,
    homeHeaderSinkOffset: Dp,
    logoPadding: PaddingValues,
    layoutDirection: LayoutDirection,
    homeAppName: String,
    homeTagline: String,
    appVersionText: String,
    avoidanceProgress: () -> Float,
    iconProgress: () -> Float,
    titleProgress: () -> Float,
    summaryProgress: () -> Float,
    statusPills: List<HomeHeaderStatusPillState>,
    compactHeightPresentation: Boolean,
    onHeroHeightChanged: (Int) -> Unit,
    onIconBottomChanged: (Float) -> Unit,
    onTitleBottomChanged: (Float) -> Unit,
    onSummaryBottomChanged: (Float) -> Unit,
) {
    val density = LocalDensity.current
    val sharedAvoidanceLiftPx = with(density) { HOME_HERO_SHARED_AVOIDANCE_LIFT.toPx() }
    val topPadding = homePageHeroTopPadding(compactHeightPresentation)
    val iconSize =
        if (compactHeightPresentation) {
            HOME_HERO_COMPACT_LANDSCAPE_ICON_SIZE
        } else {
            HOME_HERO_ICON_SIZE
        }
    val titleTopPadding =
        if (compactHeightPresentation) {
            HOME_HERO_COMPACT_LANDSCAPE_TITLE_TOP_PADDING
        } else {
            HOME_HERO_TITLE_TOP_PADDING
        }
    val titleBottomPadding =
        if (compactHeightPresentation) {
            HOME_HERO_COMPACT_LANDSCAPE_TITLE_BOTTOM_PADDING
        } else {
            HOME_HERO_TITLE_BOTTOM_PADDING
        }
    val titleFontSize = if (compactHeightPresentation) 24.sp else 30.sp
    val showSupportingDetails = homePageHeroShowsSupportingDetails(compactHeightPresentation)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = logoPadding.calculateTopPadding() + topPadding + homeHeaderSinkOffset,
                    start = logoPadding.calculateStartPadding(layoutDirection),
                    end = logoPadding.calculateEndPadding(layoutDirection),
                )
                .onSizeChanged { size -> onHeroHeightChanged(size.height) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        val avoidanceValue = avoidanceProgress()
                        val iconValue = iconProgress()
                        val titleValue = titleProgress()
                        val summaryValue = summaryProgress()
                        val sharedLiftProgress =
                            homeHeroSharedLiftProgress(
                                avoidanceProgress = avoidanceValue,
                                iconProgress = iconValue,
                                titleProgress = titleValue,
                                summaryProgress = summaryValue,
                            )
                        val iconExitProgress =
                            homeHeroIconExitProgress(
                                avoidanceProgress = avoidanceValue,
                                iconProgress = iconValue,
                            )
                        alpha = 1f - iconExitProgress
                        translationY = -sharedAvoidanceLiftPx * sharedLiftProgress
                        scaleX = 1f - (iconExitProgress * 0.05f)
                        scaleY = 1f - (iconExitProgress * 0.05f)
                    }
                    .onGloballyPositioned { coordinates ->
                        onIconBottomChanged(coordinates.positionInWindow().y + coordinates.size.height)
                    },
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_kei_logo_color),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            val avoidanceValue = avoidanceProgress()
                            val iconValue = iconProgress()
                            val iconExitProgress =
                                homeHeroIconExitProgress(
                                    avoidanceProgress = avoidanceValue,
                                    iconProgress = iconValue,
                                )
                            alpha = (1f - iconExitProgress) * 0.95f
                        }
                        .homeKeiHdrAccent(
                            enabled = homeIconHdrEnabled,
                            sweepProgress = hdrSweepProgress,
                            radialAlpha = 0.30f,
                            radialRadiusScale = 0.72f,
                            radialCenterX = 0.5f,
                            radialCenterY = 0.48f,
                        ),
            )
        }

        val titleTextStyle =
            remember {
                TextStyle(
                    brush =
                        Brush.linearGradient(
                            colors = HOME_KEI_TITLE_GRADIENT_COLORS,
                            start = Offset(14f, 6f),
                            end = Offset(260f, 104f),
                        ),
                    fontWeight = FontWeight.Bold,
                    fontSize = titleFontSize,
                    shadow =
                        ComposeTextShadow(
                            color = Color(0x55FF74A6),
                            offset = Offset(0f, 3f),
                            blurRadius = 16f,
                        ),
                )
            }
        Box(
            modifier =
                Modifier
                    .padding(top = titleTopPadding, bottom = titleBottomPadding)
                    .onGloballyPositioned { coordinates ->
                        val bottom = coordinates.positionInWindow().y + coordinates.size.height
                        onTitleBottomChanged(bottom)
                        if (!showSupportingDetails) {
                            onSummaryBottomChanged(bottom)
                        }
                    }
                    .graphicsLayer {
                        val avoidanceValue = avoidanceProgress()
                        val iconValue = iconProgress()
                        val titleValue = titleProgress()
                        val summaryValue = summaryProgress()
                        val sharedLiftProgress =
                            homeHeroSharedLiftProgress(
                                avoidanceProgress = avoidanceValue,
                                iconProgress = iconValue,
                                titleProgress = titleValue,
                                summaryProgress = summaryValue,
                            )
                        val titleExitProgress =
                            homeHeroTitleExitProgress(
                                avoidanceProgress = avoidanceValue,
                                titleProgress = titleValue,
                            )
                        alpha = 1f - titleExitProgress
                        translationY = -sharedAvoidanceLiftPx * sharedLiftProgress
                        scaleX = 1f - (titleExitProgress * 0.05f)
                        scaleY = 1f - (titleExitProgress * 0.05f)
                    }
                    .homeKeiHdrAccent(
                        enabled = homeIconHdrEnabled,
                        sweepProgress = hdrSweepProgress,
                        radialAlpha = 0.26f,
                        radialRadiusScale = 0.82f,
                        radialCenterX = 0.32f,
                        radialCenterY = 0.34f,
                    ),
        ) {
            BasicText(
                text = homeAppName,
                style = titleTextStyle,
            )
            BasicText(
                text = homeAppName,
                style = titleTextStyle.copy(shadow = null),
                modifier =
                    Modifier
                        .graphicsLayer { alpha = 0.42f }
                        .homeHeroForegroundBlur(
                            backdrop = foregroundBackdrop,
                            enabled = foregroundBlurEnabled,
                            shape = RoundedRectangle(18.dp),
                            blurRadiusDp = HOME_HERO_FOREGROUND_BLUR_RADIUS_DP,
                        ),
            )
        }

        if (showSupportingDetails) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val avoidanceValue = avoidanceProgress()
                            val iconValue = iconProgress()
                            val titleValue = titleProgress()
                            val summaryValue = summaryProgress()
                            val sharedLiftProgress =
                                homeHeroSharedLiftProgress(
                                    avoidanceProgress = avoidanceValue,
                                    iconProgress = iconValue,
                                    titleProgress = titleValue,
                                    summaryProgress = summaryValue,
                                )
                            val summaryExitProgress =
                                homeHeroSummaryExitProgress(
                                    avoidanceProgress = avoidanceValue,
                                    summaryProgress = summaryValue,
                                )
                            alpha = 1f - summaryExitProgress
                            translationY = -sharedAvoidanceLiftPx * sharedLiftProgress
                            scaleX = 1f - (summaryExitProgress * 0.05f)
                            scaleY = 1f - (summaryExitProgress * 0.05f)
                        }
                        .onGloballyPositioned { coordinates ->
                            onSummaryBottomChanged(coordinates.positionInWindow().y + coordinates.size.height)
                        },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = homeTagline,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = appVersionText,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    statusPills.forEach { pill ->
                        val modifier = Modifier.defaultMinSize(minWidth = pill.minWidth)
                        if (pill.contentPadding == null) {
                            StatusPill(
                                label = pill.label,
                                color = pill.color,
                                modifier = modifier,
                            )
                        } else {
                            StatusPill(
                                label = pill.label,
                                color = pill.color,
                                modifier = modifier,
                                contentPadding = pill.contentPadding,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun homeHeroSharedLiftProgress(
    avoidanceProgress: Float,
    iconProgress: Float,
    titleProgress: Float,
    summaryProgress: Float,
): Float =
    max(
        avoidanceProgress,
        max(iconProgress, max(titleProgress, summaryProgress)),
    )

private fun homeHeroIconExitProgress(
    avoidanceProgress: Float,
    iconProgress: Float,
): Float = max(iconProgress, avoidanceProgress * HOME_HERO_AVOIDANCE_ALPHA_WEIGHT)

private fun homeHeroTitleExitProgress(
    avoidanceProgress: Float,
    titleProgress: Float,
): Float = max(titleProgress, avoidanceProgress * HOME_HERO_AVOIDANCE_ALPHA_WEIGHT)

private fun homeHeroSummaryExitProgress(
    avoidanceProgress: Float,
    summaryProgress: Float,
): Float = max(summaryProgress, avoidanceProgress * HOME_HERO_AVOIDANCE_ALPHA_WEIGHT)

@Composable
internal fun HomePageHeroSpacer(
    logoHeightDp: Dp,
    logoPadding: PaddingValues,
    listContentPadding: PaddingValues,
    homeHeaderSinkOffset: Dp,
    compactHeightPresentation: Boolean,
    onLogoHeightPxChanged: (Int) -> Unit,
    onLogoAreaBottomChanged: (Float) -> Unit,
) {
    val topPadding = homePageHeroTopPadding(compactHeightPresentation)
    val trailingClearance = homePageHeroSpacerTrailingClearance(compactHeightPresentation)
    Box(
        Modifier
            .fillMaxWidth()
            .height(
                homePageHeroSpacerHeight(
                    heroContentHeight = logoHeightDp,
                    logoTopPadding = logoPadding.calculateTopPadding(),
                    listTopPadding = listContentPadding.calculateTopPadding(),
                    topPadding = topPadding,
                    trailingClearance = trailingClearance,
                    homeHeaderSinkOffset = homeHeaderSinkOffset,
                ),
            )
            .onSizeChanged { size -> onLogoHeightPxChanged(size.height) }
            .onGloballyPositioned { coordinates ->
                onLogoAreaBottomChanged(coordinates.positionInWindow().y + coordinates.size.height)
            },
    )
}

internal fun homePageHeroTopPadding(compactHeightPresentation: Boolean): Dp =
    if (compactHeightPresentation) {
        HOME_HERO_COMPACT_LANDSCAPE_TOP_PADDING
    } else {
        HOME_HERO_TOP_PADDING
    }

internal fun homePageHeroSpacerTrailingClearance(compactHeightPresentation: Boolean): Dp =
    if (compactHeightPresentation) {
        HOME_HERO_COMPACT_LANDSCAPE_SPACER_TRAILING_CLEARANCE
    } else {
        HOME_HERO_SPACER_TRAILING_CLEARANCE
    }

internal fun homePageHeroShowsSupportingDetails(compactHeightPresentation: Boolean): Boolean =
    !compactHeightPresentation

internal fun homePageHeroSpacerHeight(
    heroContentHeight: Dp,
    logoTopPadding: Dp,
    listTopPadding: Dp,
    topPadding: Dp,
    trailingClearance: Dp,
    homeHeaderSinkOffset: Dp,
): Dp =
    (
        heroContentHeight +
            topPadding +
            logoTopPadding -
            listTopPadding +
            trailingClearance +
            homeHeaderSinkOffset
    ).coerceAtLeast(0.dp)

@Composable
internal fun HomePageOverviewCards(
    visibleOverviewCards: Set<HomeOverviewCard>,
    homeCardBackdrop: Backdrop?,
    blurEnabled: Boolean,
    homeNa: String,
    mcpPills: List<HomeCardPillItem>,
    githubPills: List<HomeCardPillItem>,
    onOpenWebDavSync: () -> Unit,
    webDavPills: List<HomeCardPillItem>,
    baPills: List<HomeCardPillItem>,
) {
    HomeOverviewGlassBatchHost(
        backdrop = homeCardBackdrop,
        blurEnabled = blurEnabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
        ) {
            if (visibleOverviewCards.contains(HomeOverviewCard.MCP)) {
                HomeInfoCard(
                    backdrop = homeCardBackdrop,
                    blurEnabled = blurEnabled,
                ) {
                    HomeInfoPillCard(
                        naText = homeNa,
                        pills = mcpPills,
                    )
                }
            }

            if (visibleOverviewCards.contains(HomeOverviewCard.GITHUB)) {
                HomeInfoCard(
                    backdrop = homeCardBackdrop,
                    blurEnabled = blurEnabled,
                ) {
                    HomeInfoPillCard(
                        naText = homeNa,
                        pills = githubPills,
                    )
                }
            }

            if (visibleOverviewCards.contains(HomeOverviewCard.WEBDAV)) {
                HomeInfoCard(
                    backdrop = homeCardBackdrop,
                    blurEnabled = blurEnabled,
                    onClick = onOpenWebDavSync,
                ) {
                    HomeInfoPillCard(
                        naText = homeNa,
                        pills = webDavPills,
                    )
                }
            }

            if (visibleOverviewCards.contains(HomeOverviewCard.BA)) {
                HomeInfoCard(
                    backdrop = homeCardBackdrop,
                    blurEnabled = blurEnabled,
                ) {
                    HomeInfoPillCard(
                        naText = homeNa,
                        pills = baPills,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
