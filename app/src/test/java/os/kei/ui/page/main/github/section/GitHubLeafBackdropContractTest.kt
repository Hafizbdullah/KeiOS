package os.kei.ui.page.main.github.section

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubLeafBackdropContractTest {
    @Test
    fun linkedInfoCardConsumesInheritedBackdropAndKeepsStaticFallback() {
        val source = sourceFile(GITHUB_TRACKED_ITEM_INFO_CARDS_SOURCE)
        val linkedCard = source.functionBody("internal fun GitHubLinkedInfoCard(")
        val inlineSurface = source.functionBody("internal fun GitHubInlineLiquidSurface(")

        assertEquals(0, linkedCard.occurrencesOf("rememberLayerBackdrop"))
        assertEquals(0, linkedCard.occurrencesOf(".layerBackdrop("))
        assertTrue("val parentBackdrop = LocalLiquidParentBackdrop.current" in linkedCard)
        assertTrue("backdrop = parentBackdrop," in linkedCard)
        assertTrue("onClick = onClick," in linkedCard)

        assertTrue("backdrop: Backdrop?" in inlineSurface)
        assertTrue("backdrop = backdrop," in inlineSurface)
        assertEquals(0, inlineSurface.occurrencesOf("captureBackdrop"))
        assertEquals(0, inlineSurface.occurrencesOf(".layerBackdrop("))
    }

    @Test
    fun healthPreviewConsumesInheritedBackdropAndKeepsStaticFallback() {
        val healthSource = sourceFile(GITHUB_TRACKED_ITEM_HEALTH_CARDS_SOURCE)
        val infoSource = sourceFile(GITHUB_TRACKED_ITEM_INFO_CARDS_SOURCE)
        val healthPreview = healthSource.functionBody("internal fun GitHubHealthPreviewBlock(")
        val inlineSurface = infoSource.functionBody("internal fun GitHubInlineLiquidSurface(")

        assertEquals(0, healthPreview.occurrencesOf("rememberLayerBackdrop"))
        assertEquals(0, healthPreview.occurrencesOf(".layerBackdrop("))
        assertEquals(0, healthPreview.occurrencesOf("localBackdrop"))
        assertEquals(0, healthPreview.occurrencesOf("captureBackdrop"))
        assertTrue("val parentBackdrop = LocalLiquidParentBackdrop.current" in healthPreview)
        assertTrue("backdrop = parentBackdrop," in healthPreview)
        assertTrue("surfaceContainer.copy(alpha = 0.76f)" in healthPreview)

        assertTrue("backdrop: Backdrop?" in inlineSurface)
        assertTrue("backdrop = backdrop," in inlineSurface)
        assertEquals(0, inlineSurface.occurrencesOf("rememberLayerBackdrop"))
        assertEquals(0, inlineSurface.occurrencesOf(".layerBackdrop("))
    }

    @Test
    fun assetCountBubbleReusesStatusPillAndKeepsLegacyOptics() {
        val source = sourceFile(GITHUB_TRACKED_ITEM_ASSET_COUNT_BUBBLE_SOURCE)

        assertEquals(0, source.occurrencesOf("rememberLayerBackdrop"))
        assertEquals(0, source.occurrencesOf(".layerBackdrop("))
        assertEquals(0, source.occurrencesOf("LiquidSurface("))
        assertTrue("StatusPill(" in source)
        assertTrue("label = if (loading) \"\" else label" in source)
        assertTrue("backdrop = LocalLiquidParentBackdrop.current," in source)
        assertTrue("modifier = modifier.size(28.dp)" in source)
        assertTrue("modifier = Modifier.matchParentSize()" in source)
        assertTrue("contentPadding = PaddingValues(0.dp)" in source)
        assertTrue("backgroundAlphaOverride = if (isDark) 0.18f else 0.12f" in source)
        assertTrue("borderAlphaOverride = if (isDark) 0.34f else 0.24f" in source)
        assertTrue(
            "contentColorOverride = if (isDark) color else color.copy(alpha = 0.96f)" in source,
        )
        assertTrue("fontSize = AppTypographyTokens.Caption.fontSize" in source)
        assertTrue("lineHeight = AppTypographyTokens.Caption.lineHeight" in source)
        assertTrue("fontWeight = AppTypographyTokens.Caption.fontWeight" in source)
        assertTrue("maxLines = 1" in source)
        assertTrue("if (loading)" in source)
        assertTrue("LiquidCircularProgressBar(" in source)
        assertTrue("size = 14.dp" in source)
        assertTrue("strokeWidth = 2.dp" in source)
        assertTrue("inactiveColor = color.copy(alpha = 0.18f)" in source)
    }
}

private fun String.functionBody(signature: String): String {
    val start = indexOf(signature)
    require(start >= 0) { "Unable to locate $signature" }
    val nextFunction = indexOf("\n@Suppress(\"FunctionName\")", start + signature.length)
    return substring(start, if (nextFunction >= 0) nextFunction else length)
}

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

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private const val GITHUB_TRACKED_ITEM_INFO_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemInfoCards.kt"
private const val GITHUB_TRACKED_ITEM_HEALTH_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemHealthCards.kt"
private const val GITHUB_TRACKED_ITEM_ASSET_COUNT_BUBBLE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/section/GitHubTrackedItemAssetCountBubble.kt"
