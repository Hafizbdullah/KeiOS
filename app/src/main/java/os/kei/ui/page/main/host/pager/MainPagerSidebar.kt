@file:Suppress("FunctionName")

package os.kei.ui.page.main.host.pager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import os.kei.ui.page.main.model.BottomPage
import os.kei.ui.page.main.model.bottomPageIconScale
import os.kei.ui.page.main.os.appLucideListIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppSidebarToggleSize
import os.kei.ui.page.main.widget.chrome.AppSidebarWidth
import os.kei.ui.page.main.widget.chrome.appTopBarEdgePadding
import os.kei.ui.page.main.widget.glass.AppLiquidFloatingSurface
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The tab bar's converted form: a leading rail of the app's sections.
 *
 * ## Regular Liquid Glass, deliberately
 *
 * The Materials guidance splits Liquid Glass into a *regular* variant, which blurs and adjusts the luminosity
 * of what is behind it, and a *clear* variant, which is highly translucent and meant for controls floating over
 * photos and video. It then names the case this is: "use the regular variant when background content might
 * create legibility issues, or when components have a significant amount of text, such as alerts, **sidebars**,
 * or popovers". A rail of five labels is a significant amount of text, so it takes the blurred variant that the
 * app's `AppLiquidFloatingSurface` already provides — not the thin translucency the floating docks use.
 *
 * ## Why it floats instead of taking a column
 *
 * The pager is inset by [AppSidebarWidth] rather than placed in a `Row` beside the rail, so the page background
 * still spans the whole window and runs on underneath. That is the background extension effect the guidance asks
 * for — "extend visually rich content beneath the sidebar… to reinforce the separation" — and it costs one
 * padding value instead of restructuring the host.
 */
@Composable
internal fun BoxScope.MainPagerSidebar(
    tabs: List<BottomPage>,
    selectedIndex: Int,
    backdrop: Backdrop,
    topInset: Dp,
    bottomInset: Dp,
    onSelected: (Int) -> Unit,
    onConvertToTabBar: () -> Unit,
) {
    val margin = appTopBarEdgePadding()
    AppLiquidFloatingSurface(
        modifier =
            Modifier
                .align(Alignment.CenterStart)
                .width(AppSidebarWidth - margin)
                .fillMaxSize()
                .padding(
                    start = margin,
                    top = topInset + AppChromeTokens.topBarChromeTopPadding,
                    bottom = bottomInset + 12.dp,
                ),
        shape = RoundedCornerShape(28.dp),
        backdrop = backdrop,
        content = {
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // The toggle stays available in this shape too: the adaptable style keeps a button in *both*
                // forms, so a sidebar is never a state the user cannot leave.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MainPagerSidebarToggle(
                        backdrop = backdrop,
                        expanded = true,
                        onClick = onConvertToTabBar,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "KeiOS",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                tabs.forEachIndexed { index, page ->
                    MainPagerSidebarRow(
                        page = page,
                        selected = index == selectedIndex,
                        backdrop = backdrop,
                        onClick = { onSelected(index) },
                    )
                }
            }
        },
    )
}

@Composable
private fun MainPagerSidebarRow(
    page: BottomPage,
    selected: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit,
) {
    val accent = MiuixTheme.colorScheme.primary
    val label = page.label
    val row: @Composable BoxScope.() -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconModifier = Modifier.size(22.dp).bottomPageIconScale(page)
            if (page.iconRes != null) {
                Icon(
                    painter = painterResource(id = page.iconRes),
                    contentDescription = null,
                    tint = if (page.keepOriginalColors) Color.Unspecified else accent,
                    modifier = iconModifier,
                )
            } else {
                page.icon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = iconModifier,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) accent else MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (selected) {
        // Only the selected row takes a glass fill. "Use Liquid Glass effects sparingly… limit these effects
        // to the most important functional elements" — five glass plates stacked inside a glass rail would be
        // the overuse that guidance warns about, and would flatten the very distinction it is there to make.
        AppLiquidFloatingSurface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag(page.sidebarRowTestTag()),
            shape = RoundedCornerShape(16.dp),
            backdrop = backdrop,
            onClick = onClick,
            content = row,
        )
    } else {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag(page.sidebarRowTestTag()),
            contentAlignment = Alignment.Center,
        ) {
            AppLiquidFloatingSurface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                backdrop = null,
                onClick = onClick,
                content = row,
            )
        }
    }
}

/** The button that converts between the two shapes. Present in both, per the adaptable style. */
@Composable
internal fun MainPagerSidebarToggle(
    backdrop: Backdrop,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = if (expanded) "Show tab bar" else "Show sidebar"
    AppLiquidFloatingSurface(
        modifier = modifier.size(AppSidebarToggleSize),
        shape = RoundedCornerShape(14.dp),
        backdrop = backdrop,
        onClick = onClick,
        content = {
            Icon(
                imageVector = appLucideListIcon(),
                contentDescription = description,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        },
    )
}

private fun BottomPage.sidebarRowTestTag(): String = "main_sidebar_row_${name.lowercase()}"
