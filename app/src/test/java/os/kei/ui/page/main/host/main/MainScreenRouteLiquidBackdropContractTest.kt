package os.kei.ui.page.main.host.main

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainScreenRouteLiquidBackdropContractTest {
    @Test
    fun navigationTransitionVisualsFollowTheKeiOSAppTheme() {
        val source = sourceFile(MAIN_SCREEN_NAV_HOST_SOURCE)

        assertFalse("isSystemInDarkTheme" in source)
        assertEquals(1, source.occurrencesOf("isAppInDarkTheme()"))
    }

    @Test
    fun liquidRoutesExportManagedPageMaterial() {
        val source = sourceFile(MAIN_SCREEN_NAV_HOST_SOURCE)

        listOf(
            "KeiosRoute.McpSkill",
            "KeiosRoute.GitHubActionsNotificationHistory",
        ).forEach { route ->
            val routeBlock = source.routeEntryBlock(route)
            assertTrue(
                "exportBackdropToContent = true," in routeBlock,
                "$route must export the managed page material to its surface cards",
            )
        }
    }

    @Test
    fun adjacentRouteKeepsManagedBackgroundDefault() {
        val source = sourceFile(MAIN_SCREEN_NAV_HOST_SOURCE)
        val aboutRouteBlock = source.routeEntryBlock("KeiosRoute.About")

        assertFalse(
            "exportBackdropToContent = true," in aboutRouteBlock,
            "Route-level page material remains opt-in while each route is audited",
        )
    }

    /**
     * Strengthened from the visibility-gated producer this replaces.
     *
     * The toast used to wrap NavDisplay in its own full-screen `layerBackdrop`, gated on
     * `liquidToastState.isVisible` so the offscreen layer stayed off the idle path. The property that
     * gate protected — no resident second rasterization of the whole app — now holds unconditionally,
     * because the toast samples `LocalSceneBackdrop` and produces nothing of its own. Sampling the
     * scene backdrop is also strictly better: the private producer had no `onDraw`, so it handed the
     * pill transparent pixels to blur wherever page content did not paint, and being inside the scene
     * capture meant every sheet and alert blurred a ghost of the toast into its own surface.
     */
    @Test
    fun theNavHostProducesNoBackdropLayerOfItsOwn() {
        val source = sourceFile(MAIN_SCREEN_NAV_HOST_SOURCE)

        assertEquals(0, source.occurrencesOf("rememberLayerBackdrop("))
        assertEquals(0, source.occurrencesOf("Modifier.layerBackdrop("))
        assertTrue(
            "LiquidToastHost(state = liquidToastState)" in source,
            "the toast host should take no backdrop here — it samples the scene backdrop",
        )
    }

    @Test
    fun mcpCardsExportMaterialOnlyWhenLiquidChildrenConsumeIt() {
        val source = sourceFile(MCP_SKILL_ACTION_CARDS_SOURCE)

        listOf(
            "McpSkillOnboardingCard",
            "McpSkillQuickCopyCard",
            "McpSkillResourcesCard",
            "McpSkillReferenceCard",
        ).forEach { card ->
            assertTrue(
                "exportBackdropToContent = true," in source.composableFunctionBlock(card),
                "$card must export an independent card material to its Liquid controls",
            )
        }

        assertFalse(
            "exportBackdropToContent = true," in source.composableFunctionBlock("McpSkillFlowsCard"),
            "McpSkillFlowsCard has no nested Liquid consumer and should remain single-layered",
        )
    }
}

private fun String.routeEntryBlock(route: String): String {
    val marker = "entry<$route>"
    val start = indexOf(marker)
    require(start >= 0) { "Unable to locate $marker" }
    val end = indexOf("entry<KeiosRoute.", startIndex = start + marker.length).takeIf { it >= 0 } ?: length
    return substring(start, end)
}

private fun String.composableFunctionBlock(functionName: String): String {
    val marker = "fun $functionName("
    val start = indexOf(marker)
    require(start >= 0) { "Unable to locate $marker" }
    val end = indexOf("\n@Composable", startIndex = start + marker.length).takeIf { it >= 0 } ?: length
    return substring(start, end)
}

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private fun sourceFile(relativePath: String): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val sourceFile =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, relativePath) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $relativePath from $workingDirectory"
    }.readText()
}

private const val MAIN_SCREEN_NAV_HOST_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/host/main/MainScreenNavHost.kt"
private const val MCP_SKILL_ACTION_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/mcp/skill/component/McpSkillActionCards.kt"
