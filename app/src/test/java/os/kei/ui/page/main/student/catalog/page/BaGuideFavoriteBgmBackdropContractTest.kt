package os.kei.ui.page.main.student.catalog.page

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaGuideFavoriteBgmBackdropContractTest {
    @Test
    fun favoriteAlbumInheritsTheCatalogSceneMaterial() {
        val favoriteSource = sourceFile(FAVORITE_CONTENT_SOURCE)

        assertTrue(
            "val contentBackdrop = LocalLiquidParentBackdrop.current" in favoriteSource,
            "Favorite BGM must inherit the real catalog scene material",
        )
        assertFalse(
            "rememberLayerBackdrop" in favoriteSource,
            "Favorite BGM must not replace the catalog material with an empty producer",
        )
        assertFalse(
            ".layerBackdrop(" in favoriteSource,
            "Favorite BGM must not publish an empty sibling producer",
        )
    }

    @Test
    fun albumLeavesConsumeOneNullableParentMaterial() {
        val contentSource = sourceFile(ALBUM_CONTENT_SOURCE)
        val heroSource = sourceFile(ALBUM_HERO_SOURCE)
        val visualsSource = sourceFile(ALBUM_VISUALS_SOURCE)

        assertTrue("contentBackdrop: Backdrop?," in contentSource)
        assertTrue("contentBackdrop: Backdrop?," in heroSource)
        assertTrue(
            "backdrop = contentBackdrop," in heroSource.functionCallBlock("BaGuideBgmAlbumPrimaryActions"),
            "Album actions must consume the same real material as artwork and volume",
        )
        assertEquals(
            3,
            visualsSource.occurrencesOf("backdrop: Backdrop?,"),
            "Artwork, the shared action group and volume must support static fallback",
        )
        assertFalse("rememberLayerBackdrop" in visualsSource)
        assertFalse("rememberCombinedBackdrop" in visualsSource)
        assertFalse(".layerBackdrop(" in visualsSource)
    }

    @Test
    fun emptyTrackResultOnlyExportsNestedMaterialWhenGlassIsActive() {
        val source = sourceFile(TRACK_LIST_SOURCE)

        assertEquals(3, source.occurrencesOf("backdrop: Backdrop?,"))
        assertTrue("val activeBackdrop = activeGlassBackdrop(backdrop)" in source)
        assertTrue("if (activeBackdrop != null)" in source)
        assertTrue("rememberCombinedBackdrop(activeBackdrop, resultSurfaceBackdrop)" in source)
        assertTrue("exportedBackdrop = resultSurfaceBackdrop," in source)
        assertTrue("backdrop = iconBackdrop," in source)
    }

    @Test
    fun albumControlGeometryRemainsCompactAndPressSafe() {
        val source = sourceFile(ALBUM_VISUALS_SOURCE)

        assertTrue(".height(50.dp + AppInteractiveTokens.liquidPressSafePadding * 2)" in source)
        assertTrue("Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)" in source)
        assertEquals(2, source.occurrencesOf("modifier = Modifier.size(50.dp)"))
        assertEquals(2, source.occurrencesOf("width = 50.dp,"))
        assertEquals(2, source.occurrencesOf("height = 50.dp,"))
        assertEquals(2, source.occurrencesOf("iconModifier = Modifier.size(22.dp)"))
        assertTrue(".widthIn(min = 108.dp, max = 128.dp)" in source)
        assertTrue("minHeight = 50.dp," in source)
        assertTrue("leadingIconModifier = Modifier.size(20.dp)" in source)
        assertTrue("leadingContentGap = 7.dp" in source)
        assertTrue(".padding(horizontal = 10.dp)" in source)
        assertTrue("Arrangement.spacedBy(12.dp)" in source)
    }

    @Test
    fun albumActionsReuseSharedButtonsWithOriginalThemeFilm() {
        val source = sourceFile(ALBUM_VISUALS_SOURCE)

        assertEquals(2, source.occurrencesOf("AppLiquidIconButton("))
        assertEquals(1, source.occurrencesOf("AppLiquidTextButton("))
        assertEquals(1, source.occurrencesOf("LiquidSurface("), "Only album artwork keeps a raw liquid surface")
        assertFalse("BaGuideBgmRoundAction" in source)
        assertFalse("BaGuideBgmPlayAction" in source)
        assertTrue(
            "Color.White.copy(alpha = if (isAppInDarkTheme()) 0.14f else 0.34f)" in source,
        )
        assertEquals(3, source.occurrencesOf("variant = GlassVariant.SheetAction"))
        assertEquals(3, source.occurrencesOf("containerColor = actionSurfaceColor"))
        assertEquals(3, source.occurrencesOf("containerAlphaOverride = actionSurfaceColor.alpha"))
    }
}

private fun String.functionCallBlock(functionName: String): String {
    val marker = "$functionName("
    val start = indexOf(marker)
    require(start >= 0) { "Unable to locate $marker" }

    var depth = 1
    var index = start + marker.length
    while (index < length && depth > 0) {
        when (this[index]) {
            '(' -> depth += 1
            ')' -> depth -= 1
        }
        index += 1
    }
    require(depth == 0) { "Unable to locate the closing parenthesis for $marker" }
    return substring(start, index)
}

private fun String.occurrencesOf(needle: String): Int =
    windowed(needle.length).count { it == needle }

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

private const val FAVORITE_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/page/BaGuideFavoriteBgmMusicContent.kt"
private const val ALBUM_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/bgm/BaGuideBgmAlbumContent.kt"
private const val ALBUM_HERO_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/bgm/BaGuideBgmAlbumHero.kt"
private const val ALBUM_VISUALS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/bgm/BaGuideBgmAlbumHeroVisuals.kt"
private const val TRACK_LIST_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/bgm/BaGuideBgmTrackList.kt"
