package os.kei.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = true,
        ) {
            launchHomeFromColdStart()
        }
    }

    @Test
    fun homeAndGitHubInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            flingVisibleScrollable(times = 2)
            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_GITHUB,
                pageTag = GITHUB_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_GITHUB,
            )
            flingVisibleScrollable(times = 2)
        }
    }

    @Test
    fun osPageInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_OS,
                pageTag = OS_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_OS,
            )
            flingVisibleScrollable(times = 3)
        }
    }

    @Test
    fun mcpPageInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_GITHUB,
                pageTag = GITHUB_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_GITHUB,
            )
            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_MCP,
                pageTag = MCP_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_MCP,
            )
            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_HOME,
                pageTag = HOME_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_HOME,
            )
            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_MCP,
                pageTag = MCP_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_MCP,
            )
            flingVisibleScrollable(times = 3)
        }
    }

    @Test
    fun baPageInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_BA,
                pageTag = BA_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_BA,
            )
            flingVisibleScrollable(times = 3)
        }
    }

    /**
     * The Craft Chamber's disclosure, and the first sheet any journey has ever reached.
     *
     * Two gaps in one journey, both on the BA page. Counted in the shipped profile before this landed:
     * **zero** `BaCraft*` rules, for a feature with six timers, an edit sheet and its own models — it
     * shipped after the last regeneration and nothing had walked it.
     *
     * The craft card folds. Its rows come and go through `appExpandIn`/`appExpandOut` inside a glass
     * card in a lazy list, and [baPageInteractions] only ever composes whichever state was persisted —
     * the transition itself was never run, so it was interpreted on the teacher's first tap. That is
     * the same trap recorded on the calendar and pool journeys, and it costs dropped frames rather than
     * a slower launch because the animation *is* the first composition.
     *
     * Then a sheet. `LiquidSheetPanelTestTag` was added for exactly this and never worked: the overlay
     * layer is a sibling of the page content, so it inherited no `testTagsAsResourceId` and every tag
     * inside a sheet was invisible to UiAutomator. `SceneBackdropHost` now sets it for the whole
     * overlay layer, which is what makes this wait resolve. Verified by dumping the hierarchy with the
     * craft sheet open — `liquid_sheet_panel` appears, and did not before. The 15 `LiquidSheet` rules
     * already in the profile came in incidentally through other journeys; nothing had opened a sheet.
     *
     * The header is toggled to a known state first rather than assumed: the expansion is persisted, so
     * whatever the previous run or the teacher left behind decides whether the rows exist. Ending
     * expanded also puts the device back on the shipped default.
     */
    @Test
    fun baCraftCardInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_BA,
                pageTag = BA_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_BA,
            )

            // Both halves of the disclosure animate, and both are what a teacher actually triggers.
            setCraftCardExpanded(expanded = true)
            setCraftCardExpanded(expanded = false)
            setCraftCardExpanded(expanded = true)

            openAndDismissOverlay(
                triggerTag = BA_CRAFT_SLOT_FIRST,
                panelTag = LIQUID_SHEET_PANEL,
            )
        }
    }

    /**
     * The activity calendar and pool pages became nav routes, so their first composition now runs
     * inside the push transition instead of behind an activity launch. Nothing had ever profiled
     * them — the shipped profile carried 606 BaCalendarPool* rules and not one for either page
     * composable — which left the whole route path to be interpreted on first entry, mid-animation.
     */
    @Test
    fun baCalendarPoolRouteInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_BA,
                pageTag = BA_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_BA,
            )

            openDockRouteAndReturn(BA_DOCK_OPEN_CALENDAR)
            openDockRouteAndReturn(BA_DOCK_OPEN_POOL)
        }
    }

    /**
     * The Settings route: the most-reached push in the app, and still cold on first entry. Its
     * category pager shares MainLoadedPager, so this also covers the section-switch path there.
     */
    @Test
    fun settingsRouteInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            pushRouteAndReturn(
                entryTag = HOME_SETTINGS_BUTTON,
                pageTag = SETTINGS_PAGE_ROOT,
                returnTag = HOME_PAGE_ROOT,
            )
        }
    }

    /**
     * The GitHub Actions history route. Its tabbed section switch had no profile coverage at all —
     * TabbedPageContentMotion resolved to zero rules — so that path was interpreted on first use,
     * the same gap the calendar and pool pages had before they got a journey.
     */
    @Test
    fun gitHubActionsHistoryRouteInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_GITHUB,
                pageTag = GITHUB_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_GITHUB,
            )
            pushRouteAndReturn(
                entryTag = GITHUB_ACTIONS_HISTORY_BUTTON,
                pageTag = GITHUB_ACTIONS_HISTORY_PAGE_ROOT,
                returnTag = GITHUB_PAGE_ROOT,
            )
        }
    }

    /**
     * The two routes Home pushes besides Settings. About renders the changelog and the component
     * inventory; the WebDAV card opens the sync route. Both were reachable only through paths no
     * journey walked, so every class on them was interpreted on first entry.
     */
    @Test
    fun homeAboutAndWebDavRouteInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            pushRouteAndReturn(
                entryTag = HOME_ABOUT_BUTTON,
                pageTag = ABOUT_PAGE_ROOT,
                returnTag = HOME_PAGE_ROOT,
            )
            pushRouteAndReturn(
                entryTag = HOME_WEBDAV_CARD,
                pageTag = WEBDAV_SYNC_PAGE_ROOT,
                returnTag = HOME_PAGE_ROOT,
            )
        }
    }

    /**
     * The MCP skill route, pushed from the MCP page's action bar. It renders Markdown, which is the
     * most expensive first composition of any pushed route in the app.
     */
    @Test
    fun mcpSkillRouteInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_MCP,
                pageTag = MCP_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_MCP,
            )
            pushRouteAndReturn(
                entryTag = MCP_SKILL_BUTTON,
                pageTag = MCP_SKILL_PAGE_ROOT,
                returnTag = MCP_PAGE_ROOT,
            )
        }
    }

    /**
     * The menu — the whole presentation layer, which had no coverage at all.
     *
     * Every journey before this one walked pages and routes, so not one class in the overlay layer was
     * ever profiled: the menu presentation, the shared overlay host and the shared presentation
     * material were all interpreted the first time a user opened them. That is the worst case for it,
     * and for exactly the reason recorded on the calendar and pool journeys above — a menu composes
     * *inside* its present transition, so an interpreted class there costs a dropped frame rather than
     * a slower launch.
     *
     * The GitHub top-bar menu is the one to use: it routes through `SnapshotWindowListPopup` into the
     * menu presentation and renders a `LiquidGlassActionMenu` inside it, so one tap reaches the menu
     * surface, the action-menu layouts, the dropdown rows, the overlay host and the shared material.
     *
     * It waits on a menu *row*, not on the panel container. A bare `Modifier.testTag` on a container
     * with no other semantics never becomes its own accessibility node, so `SnapshotMenuPanelTestTag`
     * is invisible to UiAutomator — verified by dumping the hierarchy with the menu open, where the
     * rows appear and the panel does not. Sheets are still uncovered: the BA account toolbar action
     * does not open its sheet under a synthetic tap, and an unverified wait here costs a 25-minute run.
     */
    @Test
    fun presentationChromeInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_GITHUB,
                pageTag = GITHUB_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_GITHUB,
            )
            openAndDismissOverlay(
                triggerTag = GITHUB_IMPORT_MENU_BUTTON,
                panelTag = GITHUB_IMPORT_TRACKS,
            )
        }
    }

    /**
     * The card pile at a standstill, which a fling never reaches.
     *
     * The other journeys fling, and a fling crosses the stack line so fast that the receding states in
     * between are barely sampled. The pile's transform, its progressive blur and its scrim only run
     * while a card is *part way* into the pile, so a slow drag that parks it there is what gets that
     * code compiled. OS is the page to do it on: it is the most card-dense one in the app.
     */
    @Test
    fun cardPileInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_OS,
                pageTag = OS_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_OS,
            )
            dragSlowly(times = 3)
        }
    }

    /**
     * The shell runner, which stopped being an activity and became a route. That move put its first
     * composition inside the push transition, where an interpreted class costs a dropped frame
     * rather than a slower activity launch — the same trap the calendar and pool pages fell into.
     */
    @Test
    fun osShellRunnerRouteInteractions() {
        rule.collect(
            packageName = targetAppId(),
            includeInStartupProfile = false,
        ) {
            launchHomeFromColdStart()

            clickAndWaitForPage(
                tabTag = MAIN_BOTTOM_TAB_OS,
                pageTag = OS_PAGE_ROOT,
                settledTag = MAIN_PAGER_SETTLED_OS,
            )
            pushRouteAndReturn(
                entryTag = OS_SHELL_RUNNER_BUTTON,
                pageTag = OS_SHELL_RUNNER_PAGE_ROOT,
                returnTag = OS_PAGE_ROOT,
            )
        }
    }
}

/**
 * Drives the Craft Chamber card to [expanded], scrolling it into reach and retrying the tap.
 *
 * Written this way after the first version failed on the device with "did not collapse". Two things
 * make a single blind tap unreliable here, and both are properties of the card rather than of the test:
 * the expansion is *persisted*, so neither state can be assumed on entry — a fresh install is expanded
 * and a device someone has used may not be; and the card is the last one in the list, so the header can
 * sit under the floating dock, which is drawn above the list and eats the tap.
 *
 * Three attempts, then a loud failure. A real break still fails; a swallowed tap does not cost a run.
 */
private fun MacrobenchmarkScope.setCraftCardExpanded(expanded: Boolean) {
    val rows = testTagSelector(BA_CRAFT_SLOT_FIRST)
    repeat(3) {
        if (device.hasObject(rows) == expanded) {
            device.waitForIdle()
            return
        }
        scrollCraftCardHeaderClearOfTheDock()
        clickTestTag(BA_CRAFT_CARD_HEADER)
        val settled =
            if (expanded) {
                device.wait(Until.hasObject(rows), 5_000)
            } else {
                device.wait(Until.gone(rows), 5_000)
            }
        if (settled) {
            device.waitForIdle()
            return
        }
    }
    error("Craft card would not settle to expanded=$expanded in ${targetAppId()}")
}

/**
 * Flings until the craft header is a real target: tall enough to hit and clear of the bottom band the
 * floating dock and bottom bar occupy.
 *
 * The list stops at its end, so this converges rather than scrolling past — the card is the last one.
 */
private fun MacrobenchmarkScope.scrollCraftCardHeaderClearOfTheDock() {
    val safeBottom = (device.displayHeight * 0.80f).toInt()
    repeat(6) {
        waitForTestTag(BA_CRAFT_CARD_HEADER, timeoutMs = 15_000)
        val bounds = device.findObject(testTagSelector(BA_CRAFT_CARD_HEADER))?.visibleBounds ?: return
        if (bounds.height() >= MIN_TAPPABLE_HEIGHT_PX && bounds.centerY() <= safeBottom) return
        flingVisibleScrollable(times = 1)
    }
}

private fun MacrobenchmarkScope.clickTestTag(tag: String) {
    val node = device.findObject(testTagSelector(tag))
        ?: error("Unable to find testTag=$tag in ${targetAppId()}")
    node.click()
    device.waitForIdle()
}

/** A clipped header reports single-digit pixels; anything that thin is not worth tapping. */
private const val MIN_TAPPABLE_HEIGHT_PX = 40

/**
 * Taps a control that pushes a nav route, exercises the route, then pops back.
 *
 * Both directions matter: the pop replays the covered entry's restore path, which is what a user
 * feels on the way out.
 */
private fun MacrobenchmarkScope.pushRouteAndReturn(
    entryTag: String,
    pageTag: String,
    returnTag: String,
) {
    waitForTestTag(entryTag, timeoutMs = 15_000)
    val entry = device.findObject(testTagSelector(entryTag))
        ?: error("Unable to find testTag=$entryTag in ${targetAppId()}")
    entry.click()
    waitForTestTag(pageTag, timeoutMs = 15_000)
    flingVisibleScrollable(times = 2)

    device.pressBack()
    waitForTestTag(returnTag, timeoutMs = 15_000)
    device.waitForIdle()
}

/**
 * Pushes a route from the BA floating dock, exercises it, then pops back. Both directions matter:
 * the pop replays the covered entry's restore path, which is what a user feels on the way out.
 */
private fun MacrobenchmarkScope.openDockRouteAndReturn(dockTag: String) {
    waitForTestTag(dockTag, timeoutMs = 15_000)
    val action = device.findObject(testTagSelector(dockTag))
        ?: error("Unable to find dock action testTag=$dockTag in ${targetAppId()}")
    action.click()
    device.waitForIdle()
    // The route settles over the push transition; the dock belongs to the covered page and goes away.
    check(device.wait(Until.gone(testTagSelector(dockTag)), 15_000)) {
        "Timed out waiting for the route pushed by testTag=$dockTag in ${targetAppId()}"
    }
    device.waitForIdle()
    flingVisibleScrollable(times = 2)
    device.pressBack()
    waitForTestTag(BA_PAGE_ROOT, timeoutMs = 15_000)
    device.waitForIdle()
}

/**
 * Opens an overlay from a tagged trigger, lets it settle, and dismisses it with back.
 *
 * Waits for the panel to be *gone* rather than for a fixed delay, so the exit animation is collected
 * too — a dismissal recomposes and re-animates the same surface, and it is what the user feels last.
 */
private fun MacrobenchmarkScope.openAndDismissOverlay(
    triggerTag: String,
    panelTag: String,
) {
    waitForTestTag(triggerTag, timeoutMs = 15_000)
    val trigger = device.findObject(testTagSelector(triggerTag))
        ?: error("Unable to find overlay trigger testTag=$triggerTag in ${targetAppId()}")
    trigger.click()
    waitForTestTag(panelTag, timeoutMs = 15_000)

    device.pressBack()
    check(device.wait(Until.gone(testTagSelector(panelTag)), 15_000)) {
        "Timed out waiting for testTag=$panelTag to dismiss in ${targetAppId()}"
    }
    device.waitForIdle()
}

/**
 * Drags roughly one card at a time, slowly, so cards sit part way into the pile.
 *
 * 120 steps against [flingVisibleScrollable]'s 24: the step count is the whole point, because the
 * receding transform, blur and scrim only run for cards mid-pile and a fling skips straight past them.
 */
private fun MacrobenchmarkScope.dragSlowly(times: Int) {
    val centerX = device.displayWidth / 2
    val startY = (device.displayHeight * 0.68f).toInt()
    val endY = (device.displayHeight * 0.42f).toInt()
    repeat(times) {
        device.swipe(centerX, startY, centerX, endY, 120)
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.waitForHome() {
    check(device.wait(Until.hasObject(By.pkg(targetAppId()).depth(0)), 10_000)) {
        "Timed out waiting for target package ${targetAppId()}"
    }
    waitForTestTag(HOME_PAGE_ROOT, timeoutMs = 10_000)
    device.waitForIdle()
}

private const val MAIN_BOTTOM_TAB_HOME = "main_bottom_tab_home"
private const val MAIN_BOTTOM_TAB_OS = "main_bottom_tab_os"
private const val MAIN_BOTTOM_TAB_MCP = "main_bottom_tab_mcp"
private const val MAIN_BOTTOM_TAB_GITHUB = "main_bottom_tab_github"
private const val MAIN_BOTTOM_TAB_BA = "main_bottom_tab_ba"
private const val MAIN_PAGER_SETTLED_HOME = "main_pager_settled_home"
private const val MAIN_PAGER_SETTLED_OS = "main_pager_settled_os"
private const val MAIN_PAGER_SETTLED_MCP = "main_pager_settled_mcp"
private const val MAIN_PAGER_SETTLED_GITHUB = "main_pager_settled_github"
private const val MAIN_PAGER_SETTLED_BA = "main_pager_settled_ba"
private const val HOME_PAGE_ROOT = "home_page_root"
private const val HOME_SETTINGS_BUTTON = "home_settings_button"
private const val HOME_ABOUT_BUTTON = "home_about_button"
private const val HOME_WEBDAV_CARD = "home_webdav_card"
private const val SETTINGS_PAGE_ROOT = "settings_page_root"
private const val ABOUT_PAGE_ROOT = "about_page_root"
private const val WEBDAV_SYNC_PAGE_ROOT = "webdav_sync_page_root"
private const val OS_PAGE_ROOT = "os_page_root"
private const val OS_SHELL_RUNNER_BUTTON = "os_shell_runner_button"
private const val OS_SHELL_RUNNER_PAGE_ROOT = "os_shell_runner_page_root"
private const val MCP_PAGE_ROOT = "mcp_page_root"
private const val MCP_SKILL_BUTTON = "mcp_skill_button"
private const val MCP_SKILL_PAGE_ROOT = "mcp_skill_page_root"
private const val GITHUB_PAGE_ROOT = "github_page_root"
private const val BA_PAGE_ROOT = "ba_page_root"
private const val GITHUB_IMPORT_MENU_BUTTON = "github_import_menu_button"

/** A menu row, used as the "menu is open" signal — see [presentationChromeInteractions]. */
private const val GITHUB_IMPORT_TRACKS = "github_import_tracks"
private const val BA_DOCK_OPEN_CALENDAR = "ba_dock_open_calendar"
private const val BA_DOCK_OPEN_POOL = "ba_dock_open_pool"
private const val BA_CRAFT_CARD_HEADER = "ba_craft_card_header"

/** The first Generate row, which opens the craft sheet — see [BaselineProfileGenerator.baCraftCardInteractions]. */
private const val BA_CRAFT_SLOT_FIRST = "ba_craft_slot_first"

/**
 * Any sheet's panel, from `LiquidSheetPanelTestTag`. Declared in ui-liquid-glass rather than
 * `KeiOsTestTags`, because the sheet component and not a page owns it.
 */
private const val LIQUID_SHEET_PANEL = "liquid_sheet_panel"
private const val GITHUB_ACTIONS_HISTORY_BUTTON = "github_actions_history_button"
private const val GITHUB_ACTIONS_HISTORY_PAGE_ROOT = "github_actions_history_page_root"

private fun targetAppId(): String {
    return InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: error("targetAppId not passed as instrumentation runner arg")
}

private fun MacrobenchmarkScope.launchHomeFromColdStart() {
    pressHome()
    grantRuntimePermissions()
    val launcherComponent = resolveLauncherComponent()
    device.executeShellCommand("am force-stop ${targetAppId()}")
    device.executeShellCommand(
        "am start -W -a android.intent.action.MAIN " +
            "-c android.intent.category.LAUNCHER " +
            "-n $launcherComponent",
    )
    waitForHome()
}

private fun MacrobenchmarkScope.resolveLauncherComponent(): String {
    val output = device.executeShellCommand("cmd package resolve-activity --brief ${targetAppId()}")
    return output
        .lineSequence()
        .map(String::trim)
        .lastOrNull { line -> "/" in line }
        ?: error("Unable to resolve launcher activity for ${targetAppId()}: $output")
}

internal fun MacrobenchmarkScope.grantRuntimePermissions(packageName: String = targetAppId()) {
    listOf(
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.POST_PROMOTED_NOTIFICATIONS",
        "android.permission.ACCESS_LOCAL_NETWORK",
        "android.permission.USE_LOOPBACK_INTERFACE",
    ).forEach { permission ->
        device.executeShellCommand("pm grant $packageName $permission >/dev/null 2>&1 || true")
    }
}

private fun MacrobenchmarkScope.testTagSelector(tag: String): BySelector = By.res(tag)

private fun MacrobenchmarkScope.waitForTestTag(
    tag: String,
    timeoutMs: Long = 5_000,
) {
    check(device.wait(Until.hasObject(testTagSelector(tag)), timeoutMs)) {
        "Timed out waiting for testTag=$tag in ${targetAppId()}"
    }
    device.waitForIdle()
}

private fun MacrobenchmarkScope.clickAndWaitForPage(
    tabTag: String,
    pageTag: String,
    settledTag: String,
    timeoutMs: Long = 15_000,
) {
    check(device.wait(Until.hasObject(testTagSelector(tabTag)), timeoutMs)) {
        "Timed out waiting for tab testTag=$tabTag in ${targetAppId()}"
    }
    val node = device.findObject(testTagSelector(tabTag))
        ?: error("Unable to find tab testTag=$tabTag in ${targetAppId()}")
    node.click()
    waitForTestTag(pageTag, timeoutMs)
    waitForTestTag(settledTag, timeoutMs)
    device.waitForIdle()
}

private fun MacrobenchmarkScope.flingVisibleScrollable(times: Int) {
    val centerX = device.displayWidth / 2
    val startY = (device.displayHeight * 0.74f).toInt()
    val endY = (device.displayHeight * 0.34f).toInt()
    repeat(times) {
        device.swipe(centerX, startY, centerX, endY, 24)
        device.waitForIdle()
    }
}
