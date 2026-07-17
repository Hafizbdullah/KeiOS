@file:Suppress("FunctionName", "PropertyName")

package os.kei.ui.page.main.debug

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideSearchIcon
import os.kei.ui.page.main.widget.chrome.TabbedPageBottomChrome
import os.kei.ui.page.main.widget.chrome.TabbedPageCategory
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.shape.appSquircleBackground
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.composables.icons.lucide.R as LucideR

@Composable
internal fun DebugTabbedPageBottomChromeStage(
    accent: Color,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var useFiveCategories by remember { mutableStateOf(false) }
    var dockVisible by remember { mutableStateOf(true) }
    var simulatedNavigationInset by remember { mutableStateOf(false) }
    var selectedPage by remember { mutableIntStateOf(0) }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val categories =
        remember(useFiveCategories) {
            DebugTabbedChromeCategory.entries.take(if (useFiveCategories) 5 else 3)
        }
    val navigationBarBottom = if (simulatedNavigationInset) 24.dp else 0.dp
    val stageBackgroundColor = MiuixTheme.colorScheme.background
    val stageBackdrop =
        rememberLayerBackdrop {
            drawRect(stageBackgroundColor)
            drawContent()
        }

    LaunchedEffect(categories.size) {
        selectedPage = selectedPage.coerceIn(0, categories.lastIndex)
    }
    BackHandler(enabled = searchExpanded) {
        searchExpanded = false
    }

    Column(
        modifier =
            modifier.appSquircleBackground(
                color = stageBackgroundColor,
                cornerRadius = 0.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 14.dp, top = 12.dp, end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.debug_component_lab_liquid_chrome_production_stage),
                        color = MiuixTheme.colorScheme.onBackground,
                        fontSize = AppTypographyTokens.Body.fontSize,
                        lineHeight = AppTypographyTokens.Body.lineHeight,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.debug_component_lab_liquid_chrome_production_hint),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AppLiquidIconButton(
                    backdrop = null,
                    icon = appLucideCloseIcon(),
                    contentDescription = stringResource(R.string.common_close),
                    onClick = onClose,
                    width = 48.dp,
                    height = 48.dp,
                    iconModifier = Modifier.size(20.dp),
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DebugTabbedChromeToggle(
                    backdrop = null,
                    text =
                        stringResource(
                            if (useFiveCategories) {
                                R.string.debug_component_lab_liquid_chrome_tabs_five
                            } else {
                                R.string.debug_component_lab_liquid_chrome_tabs_three
                            },
                        ),
                    selected = useFiveCategories,
                    testTag = DEBUG_TABBED_CHROME_TAB_COUNT_TAG,
                    onClick = { useFiveCategories = !useFiveCategories },
                )
                DebugTabbedChromeToggle(
                    backdrop = null,
                    text =
                        stringResource(
                            if (dockVisible) {
                                R.string.debug_component_lab_liquid_chrome_dock_expanded
                            } else {
                                R.string.debug_component_lab_liquid_chrome_dock_compact
                            },
                        ),
                    selected = dockVisible,
                    testTag = DEBUG_TABBED_CHROME_DOCK_MODE_TAG,
                    onClick = {
                        dockVisible = !dockVisible
                        searchExpanded = false
                    },
                )
                DebugTabbedChromeToggle(
                    backdrop = null,
                    text =
                        stringResource(
                            if (simulatedNavigationInset) {
                                R.string.debug_component_lab_liquid_chrome_inset_24
                            } else {
                                R.string.debug_component_lab_liquid_chrome_inset_zero
                            },
                        ),
                    selected = simulatedNavigationInset,
                    testTag = DEBUG_TABBED_CHROME_INSET_TAG,
                    onClick = { simulatedNavigationInset = !simulatedNavigationInset },
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag(DEBUG_TABBED_CHROME_VIEWPORT_TAG),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(stageBackdrop)
                        .testTag(DEBUG_TABBED_CHROME_CONTENT_TAG),
                contentPadding =
                    PaddingValues(
                        start = 12.dp,
                        top = 12.dp,
                        end = 12.dp,
                        bottom = 116.dp + navigationBarBottom,
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = (1..12).toList(),
                    key = { index -> index },
                ) { index ->
                    DebugTabbedChromeContentRow(
                        index = index,
                        category = categories[(index - 1) % categories.size],
                        accent = accent,
                    )
                }
            }

            TabbedPageBottomChrome(
                visible = dockVisible,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .testTag(DEBUG_TABBED_CHROME_OVERLAY_TAG),
                navigationBarBottom = navigationBarBottom,
                categories = categories,
                selectedPage = selectedPage,
                selectedPagePosition = selectedPage.toFloat(),
                selectedPagePositionProvider = { selectedPage.toFloat() },
                selectedPageProvider = { selectedPage },
                searchExpanded = searchExpanded,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearchExpandedChange = { searchExpanded = it },
                searchIcon = appLucideSearchIcon(),
                searchContentDescription =
                    stringResource(R.string.debug_component_lab_search_placeholder),
                searchPlaceholder = stringResource(R.string.debug_component_lab_search_placeholder),
                backdrop = stageBackdrop,
                isLiquidEffectEnabled = true,
                onSelectCategory = { index -> selectedPage = index },
                onExpandDock = { dockVisible = true },
                labelPrefix = "debug_tabbed_chrome",
            )
        }
    }
}

@Composable
private fun DebugTabbedChromeToggle(
    backdrop: Backdrop?,
    text: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    AppLiquidTextButton(
        backdrop = backdrop,
        text = text,
        onClick = onClick,
        selected = selected,
        minHeight = 48.dp,
        horizontalPadding = 11.dp,
        verticalPadding = 7.dp,
        textMaxLines = 1,
        textOverflow = TextOverflow.Ellipsis,
        textSoftWrap = false,
        textSize = AppTypographyTokens.Supporting.fontSize,
        textLineHeight = AppTypographyTokens.Supporting.lineHeight,
        modifier = Modifier.testTag(testTag),
    )
}

@Composable
private fun DebugTabbedChromeContentRow(
    index: Int,
    category: DebugTabbedChromeCategory,
    accent: Color,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .appSquircleBackground(
                    color =
                        if (index % 2 == 0) {
                            accent.copy(alpha = 0.14f)
                        } else {
                            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
                        },
                    cornerRadius = 18.dp,
                ).padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("$DEBUG_TABBED_CHROME_ROW_TAG_PREFIX$index"),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.debug_component_lab_liquid_chrome_scroll_row, index),
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(category.labelRes),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class DebugTabbedChromeCategory(
    override val iconRes: Int,
    override val labelRes: Int,
) : TabbedPageCategory {
    Home(LucideR.drawable.lucide_ic_house, R.string.debug_component_lab_nav_home),
    Discover(LucideR.drawable.lucide_ic_wand_sparkles, R.string.debug_component_lab_nav_discover),
    Radio(LucideR.drawable.lucide_ic_radio, R.string.debug_component_lab_nav_radio),
    Library(LucideR.drawable.lucide_ic_library, R.string.debug_component_lab_nav_library),
    Search(LucideR.drawable.lucide_ic_search, R.string.debug_component_lab_nav_search),
}

internal const val DEBUG_TABBED_CHROME_VIEWPORT_TAG = "debug-tabbed-chrome-viewport"
internal const val DEBUG_TABBED_CHROME_CONTENT_TAG = "debug-tabbed-chrome-content"
internal const val DEBUG_TABBED_CHROME_OVERLAY_TAG = "debug-tabbed-chrome-overlay"
internal const val DEBUG_TABBED_CHROME_TAB_COUNT_TAG = "debug-tabbed-chrome-tab-count"
internal const val DEBUG_TABBED_CHROME_DOCK_MODE_TAG = "debug-tabbed-chrome-dock-mode"
internal const val DEBUG_TABBED_CHROME_INSET_TAG = "debug-tabbed-chrome-inset"
internal const val DEBUG_TABBED_CHROME_ROW_TAG_PREFIX = "debug-tabbed-chrome-row-"
