package os.kei.baselineprofile

import android.os.Trace
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainNavigationFrameBenchmarks {
    @get:Rule
    val rule = MacrobenchmarkRule()

    private val targetAppId: String
        get() =
            InstrumentationRegistry
                .getArguments()
                .getString("targetAppId")
                ?: error("targetAppId not passed as instrumentation runner arg")

    @Test
    fun homeRestingDynamicBackground() {
        rule.measureRepeated(
            packageName = targetAppId,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                pressHome()
                grantRuntimePermissions(targetAppId)
                startActivityAndWait()
                waitForTag(HOME_PAGE_ROOT)
                waitForTag(MAIN_PAGER_SETTLED_HOME)
            },
            measureBlock = {
                traceSection("benchmark:home_resting_dynamic_background") {
                    Thread.sleep(HOME_RESTING_MEASURE_MS)
                }
            },
        )
    }

    @Test
    fun homeScrollWithFullEffects() {
        rule.measureRepeated(
            packageName = targetAppId,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                pressHome()
                grantRuntimePermissions(targetAppId)
                startActivityAndWait()
                waitForTag(HOME_PAGE_ROOT)
                waitForTag(MAIN_PAGER_SETTLED_HOME)
            },
            measureBlock = {
                traceSection("benchmark:home_scroll_full_effects") {
                    repeat(HOME_SCROLL_FLING_COUNT) {
                        swipePage(up = true, waitForIdle = false)
                    }
                    repeat(HOME_SCROLL_FLING_COUNT) {
                        swipePage(up = false, waitForIdle = false)
                    }
                    Thread.sleep(HOME_SCROLL_SETTLE_MS)
                }
            },
        )
    }

    @Test
    fun homeGitHubMcpHome() {
        rule.measureRepeated(
            packageName = targetAppId,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                pressHome()
                grantRuntimePermissions(targetAppId)
                startActivityAndWait()
                waitForTag(HOME_PAGE_ROOT)
                waitForTag(MAIN_PAGER_SETTLED_HOME)
            },
            measureBlock = {
                traceSection("benchmark:home_to_github") {
                    clickAndWait(
                        tabTag = MAIN_BOTTOM_TAB_GITHUB,
                        pageTag = GITHUB_PAGE_ROOT,
                        settledTag = MAIN_PAGER_SETTLED_GITHUB,
                    )
                }
                traceSection("benchmark:github_to_mcp") {
                    clickAndWait(
                        tabTag = MAIN_BOTTOM_TAB_MCP,
                        pageTag = MCP_PAGE_ROOT,
                        settledTag = MAIN_PAGER_SETTLED_MCP,
                    )
                }
                traceSection("benchmark:mcp_to_home") {
                    clickAndWait(
                        tabTag = MAIN_BOTTOM_TAB_HOME,
                        pageTag = HOME_PAGE_ROOT,
                        settledTag = MAIN_PAGER_SETTLED_HOME,
                    )
                }
            },
        )
    }

    @Test
    fun mcpStackedCardsScroll() {
        rule.measureRepeated(
            packageName = targetAppId,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                pressHome()
                grantRuntimePermissions(targetAppId)
                startActivityAndWait()
                waitForTag(HOME_PAGE_ROOT)
                waitForTag(MAIN_PAGER_SETTLED_HOME)
                clickAndWait(
                    tabTag = MAIN_BOTTOM_TAB_MCP,
                    pageTag = MCP_PAGE_ROOT,
                    settledTag = MAIN_PAGER_SETTLED_MCP,
                )
            },
            measureBlock = {
                repeat(MCP_SCROLL_FLING_COUNT) {
                    swipeMcpPage(up = true)
                }
                repeat(MCP_SCROLL_FLING_COUNT) {
                    swipeMcpPage(up = false)
                }
            },
        )
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.clickAndWait(
        tabTag: String,
        pageTag: String,
        settledTag: String,
    ) {
        val tab = device.findObject(By.res(tabTag))
            ?: error("Unable to find tab testTag=$tabTag")
        tab.click()
        waitForTag(pageTag)
        waitForTag(settledTag)
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.waitForTag(tag: String) {
        check(device.wait(Until.hasObject(By.res(tag)), PAGE_TIMEOUT_MS)) {
            "Timed out waiting for testTag=$tag in $targetAppId"
        }
        device.waitForIdle()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.swipeMcpPage(up: Boolean) {
        swipePage(up = up, waitForIdle = true)
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.swipePage(
        up: Boolean,
        waitForIdle: Boolean,
    ) {
        val centerX = device.displayWidth / 2
        val upperY = (device.displayHeight * MCP_SCROLL_UPPER_FRACTION).toInt()
        val lowerY = (device.displayHeight * MCP_SCROLL_LOWER_FRACTION).toInt()
        val startY = if (up) lowerY else upperY
        val endY = if (up) upperY else lowerY
        check(device.swipe(centerX, startY, centerX, endY, MCP_SCROLL_STEPS)) {
            "Unable to swipe page ${if (up) "up" else "down"}"
        }
        if (waitForIdle) {
            device.waitForIdle()
        } else {
            Thread.sleep(HOME_SCROLL_BETWEEN_SWIPES_MS)
        }
    }
}

private inline fun <T> traceSection(
    name: String,
    block: () -> T,
): T {
    Trace.beginSection(name)
    return try {
        block()
    } finally {
        Trace.endSection()
    }
}

private const val MAIN_BOTTOM_TAB_HOME = "main_bottom_tab_home"
private const val MAIN_BOTTOM_TAB_MCP = "main_bottom_tab_mcp"
private const val MAIN_BOTTOM_TAB_GITHUB = "main_bottom_tab_github"
private const val MAIN_PAGER_SETTLED_HOME = "main_pager_settled_home"
private const val MAIN_PAGER_SETTLED_MCP = "main_pager_settled_mcp"
private const val MAIN_PAGER_SETTLED_GITHUB = "main_pager_settled_github"
private const val HOME_PAGE_ROOT = "home_page_root"
private const val MCP_PAGE_ROOT = "mcp_page_root"
private const val GITHUB_PAGE_ROOT = "github_page_root"
private const val PAGE_TIMEOUT_MS = 15_000L
private const val HOME_RESTING_MEASURE_MS = 3_000L
private const val HOME_SCROLL_BETWEEN_SWIPES_MS = 180L
private const val HOME_SCROLL_SETTLE_MS = 600L
private const val HOME_SCROLL_FLING_COUNT = 2
private const val MCP_SCROLL_FLING_COUNT = 3
private const val MCP_SCROLL_UPPER_FRACTION = 0.30f
private const val MCP_SCROLL_LOWER_FRACTION = 0.78f
private const val MCP_SCROLL_STEPS = 18
