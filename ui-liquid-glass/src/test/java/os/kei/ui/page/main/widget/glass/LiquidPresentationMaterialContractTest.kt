package os.kei.ui.page.main.widget.glass

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Structural guards for the presentation family, pinned at the source level because none of them can be
 * observed from a unit test: they are about which modifier a transform goes through and which file owns
 * a constant.
 */
class LiquidPresentationMaterialContractTest {
    @Test
    fun `nobody inlines the presentation blur expression any more`() {
        // The same two-line expression was pasted into four files. Frame cost lives in it, so it should
        // be one line to find and one line to change.
        val offenders =
            kotlinMainSources()
                .filter { it.name != "LiquidPresentationMaterial.kt" }
                .filter { file ->
                    val text = file.readText()
                    "maxGlassBlur" in text && "blurScaleFor" in text
                }.map { it.name }

        assertEquals(
            emptyList(),
            offenders,
            "these should call presentationGlassBlur() instead of re-deriving it",
        )
    }

    @Test
    fun `nobody inlines the presentation lens expression any more`() {
        val offenders =
            kotlinMainSources()
                .filter { it.name != "LiquidPresentationMaterial.kt" }
                .filter { file ->
                    val text = file.readText()
                    "backdropLens" in text && "lensScaleFor" in text
                }.map { it.name }

        assertEquals(
            emptyList(),
            offenders,
            "these should call presentationGlassLens(...) instead of re-deriving it",
        )
    }

    @Test
    fun `the toast does not animate a glass surface with an ancestor transform`() {
        // AnimatedVisibility's scaleIn/scaleOut is a plain graphicsLayer wrapped *around* the element.
        // LayerBackdrop.drawBackdrop inverse-transforms its sample only by the layerBlock handed to
        // drawBackdrop, so an ancestor scale moves and magnifies the sampled backdrop with the pill —
        // the library's docs call this out explicitly, and the library source even carries a
        // "TODO: outer transformations lead to wrong position calculation".
        val host = toastSource("LiquidToastHost.kt")

        assertFalse(
            "AnimatedVisibility" in host,
            "the toast's enter/exit must go through drawBackdrop's layerBlock, not AnimatedVisibility",
        )
        assertFalse("scaleIn(" in host || "scaleOut(" in host, "same reason: those imply an ancestor layer")
        assertTrue(
            "layerBlock = transformProvider" in toastSource("LiquidToastSurface.kt"),
            "the pill's transform has to reach drawBackdrop's layerBlock",
        )
    }

    @Test
    fun `the toast no longer owns a private backdrop producer`() {
        // It used to wrap NavDisplay in a second full-screen layerBackdrop, inside the scene backdrop's
        // own captured subtree. That cost a second offscreen layer whenever a toast was up, left the
        // toast inside the sample every sheet and alert blurs, and — because the private producer had no
        // onDraw — gave the pill transparent pixels to blur wherever page content did not paint.
        val navHost =
            File(repositoryRoot(), "app/src/main/java/os/kei/ui/page/main/host/main/MainScreenNavHost.kt")
                .readText()

        assertFalse(
            "rememberLayerBackdrop(" in navHost,
            "MainScreenNavHost should sample LocalSceneBackdrop rather than produce its own layer",
        )
    }

    @Test
    fun `the toast is split by concern rather than living in one file`() {
        // The original was one 496-line file holding duration policy, the state holder, host layout, the
        // per-item timer and the pill's rendering.
        listOf("LiquidToastState.kt", "LiquidToastHost.kt", "LiquidToastSurface.kt").forEach { name ->
            assertTrue(
                kotlinMainSources().any { it.name == name },
                "expected $name to exist",
            )
        }
        assertFalse(
            kotlinMainSources().any { it.name == "LiquidToast.kt" },
            "the monolith should be gone, not kept alongside the split files",
        )
        assertFalse(
            kotlinMainSources().any { it.name == "LocalLiquidToastState.kt" },
            "LocalLiquidToastState was declared, never provided and never read",
        )
    }

    private fun toastSource(name: String): String = kotlinMainSources().single { it.name == name }.readText()

    private fun kotlinMainSources(): List<File> {
        val root = repositoryRoot()
        return listOf("app/src/main", "ui-liquid-glass/src/main")
            .map { File(root, it) }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" } }
    }

    private fun repositoryRoot(): File {
        val start = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        return generateSequence(start) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile || File(it, "settings.gradle").isFile }
    }
}
