package os.kei.ui.page.main.debug

import java.io.File
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugLiquidBackdropContractTest {
    @Test
    fun searchSamplesConsumeTheNearestExportedCardMaterial() {
        val source = sourceFile(DEBUG_LIQUID_SEARCH_FORM_SOURCE)
        val card = source.functionBody(
            start = "internal fun DebugLiquidSearchFormCard(",
            end = "private fun DebugLiquidSearchSectionLabel(",
        )

        assertTrue("backdrop = backdrop," in card)
        assertTrue("exportBackdropToContent = true," in card)
        assertTrue("val cardBackdrop = LocalLiquidParentBackdrop.current ?: backdrop" in card)
        assertEquals(5, card.occurrencesOf("backdrop = cardBackdrop,"))
        assertEquals(1, card.occurrencesOf("backdrop = backdrop,"))
        assertFalse(".layerBackdrop(cardBackdrop)" in card)
    }
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

private fun String.functionBody(
    start: String,
    end: String,
): String {
    val startIndex = indexOf(start)
    val endIndex = indexOf(end, startIndex = startIndex.coerceAtLeast(0))
    require(startIndex >= 0) { "Missing function start: $start" }
    require(endIndex > startIndex) { "Missing function end: $end" }
    return substring(startIndex, endIndex)
}

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private const val DEBUG_LIQUID_SEARCH_FORM_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidSearchFormCard.kt"
