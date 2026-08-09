package os.kei.ui.page.main.github.actions

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class GitHubActionsNoticeCardSourceTest {
    @Test
    fun noticeCardDelegatesItsMaterialAndTextContractToSupportingBlock() {
        val primitiveSource = sourceFile(GITHUB_ACTIONS_PRIMITIVES_SOURCE)
        val noticeImplementation =
            primitiveSource.substring(
                startIndex = primitiveSource.indexOf("internal fun GitHubActionsNoticeCard("),
                endIndex = primitiveSource.indexOf("@Immutable", primitiveSource.indexOf("internal fun GitHubActionsNoticeCard(")),
            )

        assertTrue("AppSupportingBlock(" in noticeImplementation)
        assertFalse("SheetSurfaceCard(" in noticeImplementation)
        assertTrue("accentColor = colors.accentColor" in noticeImplementation)
        assertTrue("containerColor = colors.containerColor" in noticeImplementation)
        assertTrue("contentColor = colors.contentColor" in noticeImplementation)
        assertTrue("contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)" in noticeImplementation)
        assertTrue("typography = AppTypographyTokens.Body" in noticeImplementation)
        assertTrue("cornerRadius = CardLayoutRhythm.cardCornerRadius" in noticeImplementation)
        assertTrue("borderColor = colors.borderColor" in noticeImplementation)
        assertTrue("borderWidth = 1.dp" in noticeImplementation)
        assertTrue("fillWidth = true" in noticeImplementation)
        assertTrue("depthEffect = true" in noticeImplementation)
        assertTrue("highlightAlpha = 0.82f" in noticeImplementation)
        // No outer drop shadow: the notice card scrolls, and a clipped shadow ring squares off its
        // corners. See LiquidSurface's `shadow`.
        assertTrue("shadow = false" in noticeImplementation)
        assertTrue("shadowAlpha = 0.10f" in noticeImplementation)
        assertFalse("maxLines =" in noticeImplementation)
        assertFalse("TextOverflow.Ellipsis" in noticeImplementation)
    }

    @Test
    fun allEightStatusBranchesContinueThroughTheSharedNoticePrimitive() {
        val callCount =
            GITHUB_ACTIONS_NOTICE_CALL_SOURCES.sumOf { path ->
                sourceFile(path).occurrencesOf("GitHubActionsNoticeCard(")
            }

        assertEquals(8, callCount)
    }

    @Test
    fun supportingBlockCustomizationsRemainOptInWithBackdropAndFallbackPaths() {
        val supportingSource = sourceFile(APP_STATUS_PRIMITIVES_SOURCE)

        assertTrue("containerColor: Color? = null" in supportingSource)
        assertTrue("contentColor: Color? = null" in supportingSource)
        assertTrue(
            "contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 9.dp)" in
                supportingSource,
        )
        assertTrue("typography: AppTypographyToken = AppTypographyTokens.Supporting" in supportingSource)
        assertTrue("cornerRadius: Dp = 12.dp" in supportingSource)
        assertTrue("borderColor: Color = Color.Unspecified" in supportingSource)
        assertTrue("borderWidth: Dp = 0.dp" in supportingSource)
        assertTrue("fillWidth: Boolean = false" in supportingSource)
        assertTrue("depthEffect: Boolean = false" in supportingSource)
        assertTrue("highlightAlpha: Float? = null" in supportingSource)
        assertTrue("shadow: Boolean = false" in supportingSource)
        assertTrue("shadowAlpha: Float = 0.10f" in supportingSource)
        assertTrue("activeGlassBackdrop(backdrop ?: LocalLiquidParentBackdrop.current)" in supportingSource)
        assertTrue("if (activeBackdrop != null)" in supportingSource)
        assertTrue(".appSquircleBackground(backgroundColor, cornerRadius)" in supportingSource)
        assertTrue(".appSquircleBorder(borderWidth, borderColor, cornerRadius)" in supportingSource)
        assertTrue("depthEffect = depthEffect" in supportingSource)
        assertTrue("highlightAlpha = highlightAlpha" in supportingSource)
        assertTrue("shadow = shadow" in supportingSource)
        assertTrue("shadowAlpha = shadowAlpha" in supportingSource)
    }
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

private const val GITHUB_ACTIONS_PRIMITIVES_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/actions/GitHubActionsPrimitives.kt"

private const val APP_STATUS_PRIMITIVES_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/core/AppStatusPrimitives.kt"

private val GITHUB_ACTIONS_NOTICE_CALL_SOURCES =
    listOf(
        "app/src/main/java/os/kei/ui/page/main/github/actions/GitHubActionsArtifactSection.kt",
        "app/src/main/java/os/kei/ui/page/main/github/actions/GitHubActionsBranchSection.kt",
        "app/src/main/java/os/kei/ui/page/main/github/actions/GitHubActionsWorkflowSection.kt",
        "app/src/main/java/os/kei/ui/page/main/github/actions/GitHubActionsSheetContent.kt",
        "app/src/main/java/os/kei/ui/page/main/github/actions/GitHubActionsRunSection.kt",
    )
