package os.kei.ui.page.main.widget.sheet

import org.junit.Test
import java.io.File
import kotlin.test.assertFalse

/**
 * The Liquid sheet, alert and action bar are no longer optional.
 *
 * They used to sit behind preferences that swapped in the Miuix originals, and those originals could
 * not be more than flat cards: hosted in a Dialog window, `LocalSceneBackdrop` is blanked to
 * `emptyBackdrop()` and every blur they asked for silently drew nothing. Rather than keep shipping a
 * downgrade path, the toggles are gone.
 *
 * This is the inverse of the contract test it replaces — it fails if any of that plumbing grows back.
 */
class LiquidSheetPreferenceProviderContractTest {
    @Test
    fun `no source still reads a liquid sheet or dialog or action bar preference`() {
        val forbidden =
            listOf(
                "isLiquidSheetEnabled",
                "setLiquidSheetEnabled",
                "liquidSheetEnabled",
                "LocalLiquidSheetEnabled",
                "useLiquidGlassSheet",
                "isLiquidDialogEnabled",
                "setLiquidDialogEnabled",
                "liquidDialogEnabled",
                "isLiquidActionBarLayeredStyleEnabled",
                "setLiquidActionBarLayeredStyleEnabled",
                "liquidActionBarLayeredStyleEnabled",
                "layeredStyleEnabled",
                "SheetVisualMode",
            )

        kotlinSources().forEach { file ->
            val text = file.readText()
            forbidden.forEach { symbol ->
                assertFalse(
                    symbol in text,
                    "${file.path} still references the removed '$symbol'",
                )
            }
        }
    }

    private fun kotlinSources(): List<File> {
        val root = repositoryRoot()
        return listOf("app/src/main", "ui-liquid-glass/src/main", "core-prefs/src/main")
            .map { File(root, it) }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" } }
    }

    private fun repositoryRoot(): File {
        val start = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        return generateSequence(start) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile || File(it, "settings.gradle").isFile }
    }
}
