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

    @Test
    fun catalogSamplesConsumeTheNearestExportedCardMaterial() {
        val source = sourceFile(DEBUG_LIQUID_CATALOG_SOURCE)
        val cards =
            listOf(
                CatalogCardContract(
                    functionName = "DebugLiquidButtonsCard",
                    nextFunctionName = "DebugLiquidGlassDropdownCard",
                    expectedConsumerCount = 8,
                ),
                CatalogCardContract(
                    functionName = "DebugLiquidTransparentButtonsCard",
                    nextFunctionName = "DebugLiquidSurfaceCardsCard",
                    expectedConsumerCount = 1,
                ),
                CatalogCardContract(
                    functionName = "DebugLiquidSurfaceCardsCard",
                    nextFunctionName = "DebugLiquidParentBackdropCard",
                    expectedConsumerCount = 2,
                ),
                CatalogCardContract(
                    functionName = "DebugLiquidParameterCard",
                    nextFunctionName = "DebugLiquidControlsCard",
                    expectedConsumerCount = 1,
                ),
                CatalogCardContract(
                    functionName = "DebugLiquidControlsCard",
                    nextFunctionName = "DebugLiquidSliderSamples",
                    expectedConsumerCount = 2,
                ),
            )

        cards.forEach { contract ->
            val card =
                source.functionBody(
                    start = "internal fun ${contract.functionName}(",
                    end = "fun ${contract.nextFunctionName}(",
                )

            assertTrue("backdrop = backdrop," in card, contract.functionName)
            assertTrue("exportBackdropToContent = true," in card, contract.functionName)
            assertTrue(
                "val cardBackdrop = LocalLiquidParentBackdrop.current ?: backdrop" in card,
                contract.functionName,
            )
            assertEquals(
                contract.expectedConsumerCount,
                card.occurrencesOf("backdrop = cardBackdrop,"),
                contract.functionName,
            )
            assertEquals(1, card.occurrencesOf("backdrop = backdrop,"), contract.functionName)
            assertFalse(".layerBackdrop(cardBackdrop)" in card, contract.functionName)
        }
    }
}

private data class CatalogCardContract(
    val functionName: String,
    val nextFunctionName: String,
    val expectedConsumerCount: Int,
)

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

private const val DEBUG_LIQUID_CATALOG_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidCatalogCard.kt"
