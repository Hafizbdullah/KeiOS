package os.kei.ui.page.main.debug

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugLiquidBackdropContractTest {
    @Test
    fun debugPageMaterialsFollowTheAppTheme() {
        listOf(
            Triple("Component Lab", DEBUG_COMPONENT_LAB_PAGE_SOURCE, 2),
            Triple("Liquid Catalog", DEBUG_LIQUID_CATALOG_PAGE_SOURCE, 1),
        ).forEach { (pageName, sourcePath, expectedReads) ->
            val source = sourceFile(sourcePath)

            assertFalse("isSystemInDarkTheme" in source, pageName)
            assertEquals(expectedReads, source.occurrencesOf("isAppInDarkTheme()"), pageName)
        }
    }

    @Test
    fun debugPagesProvideTheirSiblingProducedBackdropToCards() {
        listOf(
            "Component Lab" to DEBUG_COMPONENT_LAB_PAGE_SOURCE,
            "Liquid Catalog" to DEBUG_LIQUID_CATALOG_PAGE_SOURCE,
        ).forEach { (pageName, sourcePath) ->
            val source = sourceFile(sourcePath)
            val producer = ".layerBackdrop(pageBackdrop)"
            val provider =
                "CompositionLocalProvider(LocalLiquidParentBackdrop provides pageBackdrop)"
            val producerIndex = source.indexOf(producer)
            val providerIndex = source.indexOf(provider)

            assertEquals(1, source.occurrencesOf("rememberLayerBackdrop()"), pageName)
            assertEquals(1, source.occurrencesOf(producer), pageName)
            assertEquals(1, source.occurrencesOf(provider), pageName)
            assertTrue(producerIndex >= 0, pageName)
            assertTrue(providerIndex > producerIndex, pageName)

            val consumerTree = source.substring(providerIndex)
            assertTrue("AppPageLazyColumn(" in consumerTree, pageName)
            assertFalse(producer in consumerTree, pageName)
        }
    }

    @Test
    fun searchSamplesConsumeTheNearestExportedCardMaterial() {
        val source = sourceFile(DEBUG_LIQUID_SEARCH_FORM_SOURCE)
        val card =
            source.functionBody(
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
    fun componentLabCardsExportTheirRenderedMaterialToNestedControls() {
        val source = sourceFile(DEBUG_COMPONENT_LAB_PAGE_SOURCE)
        val previewCard =
            source.functionBody(
                start = "private fun DebugLiquidPreviewCard(",
                end = "private fun DebugLabIntroCard(",
            )
        val iterationCard =
            source.functionBody(
                start = "private fun DebugIterationQueueCard(",
                end = "\n}",
                fromEnd = true,
            )

        assertTrue("backdrop = backdrop," in previewCard)
        assertTrue("exportBackdropToContent = true," in previewCard)
        assertTrue("val cardBackdrop = LocalLiquidParentBackdrop.current ?: backdrop" in previewCard)
        assertEquals(1, previewCard.occurrencesOf("backdrop = cardBackdrop,"))
        assertEquals(1, previewCard.occurrencesOf("backdrop = backdrop,"))
        assertFalse(".layerBackdrop(cardBackdrop)" in previewCard)

        assertTrue("exportBackdropToContent = true," in iterationCard)
        assertTrue("StatusIconPill(" in iterationCard)
        assertTrue("icon = appLucideLayersIcon()" in iterationCard)
        assertFalse("rememberLayerBackdrop()" in iterationCard)
        assertFalse(".layerBackdrop(" in iterationCard)
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
                    functionName = "DebugLiquidGlassDropdownCard",
                    nextFunctionName = "DebugLiquidBackdropCard",
                    expectedConsumerCount = 4,
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

    @Test
    fun chromeAndActionMenuTriggersConsumeTheirExportedCardMaterial() {
        val chrome = sourceFile(DEBUG_LIQUID_CHROME_SOURCE)
        val actionMenu = sourceFile(DEBUG_LIQUID_ACTION_MENU_SOURCE)

        assertTrue("backdrop = backdrop," in chrome)
        assertTrue("exportBackdropToContent = true," in chrome)
        assertTrue("val cardBackdrop = LocalLiquidParentBackdrop.current ?: backdrop" in chrome)
        assertEquals(3, chrome.occurrencesOf("backdrop = cardBackdrop,"))
        assertEquals(1, chrome.occurrencesOf("backdrop = backdrop,"))
        assertFalse("rememberLayerBackdrop()" in chrome)
        assertFalse(".layerBackdrop(cardBackdrop)" in chrome)

        assertTrue("exportBackdropToContent = true," in actionMenu)
        assertTrue("val cardBackdrop = LocalLiquidParentBackdrop.current ?: backdrop" in actionMenu)
        assertEquals(1, actionMenu.occurrencesOf("backdrop = cardBackdrop,"))
        assertEquals(2, actionMenu.occurrencesOf("backdrop = backdrop,"))
        assertTrue("SnapshotWindowListPopup(" in actionMenu)
        assertFalse("backdrop = cardBackdrop," in actionMenu.substringAfter("SnapshotWindowListPopup("))
        assertFalse("rememberLayerBackdrop()" in actionMenu)
        assertFalse(".layerBackdrop(cardBackdrop)" in actionMenu)
    }

    @Test
    fun nestedCatalogSurfacesExportTheirRenderedMaterialToDescendants() {
        val source = sourceFile(DEBUG_LIQUID_CATALOG_SAMPLES_SOURCE)
        val surfaces =
            listOf(
                NestedSurfaceContract(
                    functionName = "DebugLiquidSurfaceFamilySamples",
                    nextFunctionName = "DebugLiquidClusterCardSample",
                    exportedBackdropName = "cardBackdrop",
                    expectedInputCount = 2,
                    expectedDescendantCount = 1,
                    flexibleMinimumHeightDp = 128,
                    allowedFixedHeightCount = 1,
                ),
                NestedSurfaceContract(
                    functionName = "DebugLiquidClusterCardSample",
                    nextFunctionName = "DebugLiquidParentBackdropSample",
                    exportedBackdropName = "clusterBackdrop",
                    expectedInputCount = 1,
                    expectedDescendantCount = 5,
                    flexibleMinimumHeightDp = 158,
                    allowedFixedHeightCount = 0,
                ),
                NestedSurfaceContract(
                    functionName = "DebugLiquidParameterPanelSample",
                    nextFunctionName = "DebugLiquidParameterSlider",
                    exportedBackdropName = "panelBackdrop",
                    expectedInputCount = 1,
                    expectedDescendantCount = 5,
                    flexibleMinimumHeightDp = 96,
                    allowedFixedHeightCount = 0,
                ),
            )

        surfaces.forEach { contract ->
            val sample =
                source.functionBody(
                    start = "internal fun ${contract.functionName}(",
                    end = "fun ${contract.nextFunctionName}(",
                )

            assertTrue("exportBackdropToContent = true," in sample, contract.functionName)
            assertTrue(
                "val ${contract.exportedBackdropName} = " +
                    "LocalLiquidParentBackdrop.current ?: backdrop" in sample,
                contract.functionName,
            )
            assertEquals(
                contract.expectedInputCount,
                sample.occurrencesOf("backdrop = backdrop,"),
                contract.functionName,
            )
            assertEquals(
                contract.expectedDescendantCount,
                sample.occurrencesOf("backdrop = ${contract.exportedBackdropName},"),
                contract.functionName,
            )
            assertFalse(
                ".layerBackdrop(${contract.exportedBackdropName})" in sample,
                contract.functionName,
            )
            assertFalse(
                "val ${contract.exportedBackdropName} = rememberLayerBackdrop()" in sample,
                contract.functionName,
            )
            assertTrue(
                ".heightIn(min = ${contract.flexibleMinimumHeightDp}.dp)" in sample,
                contract.functionName,
            )
            assertEquals(
                contract.allowedFixedHeightCount,
                sample.occurrencesOf(".height(${contract.flexibleMinimumHeightDp}.dp)"),
                contract.functionName,
            )
        }
    }
}

private data class CatalogCardContract(
    val functionName: String,
    val nextFunctionName: String,
    val expectedConsumerCount: Int,
)

private data class NestedSurfaceContract(
    val functionName: String,
    val nextFunctionName: String,
    val exportedBackdropName: String,
    val expectedInputCount: Int,
    val expectedDescendantCount: Int,
    val flexibleMinimumHeightDp: Int,
    val allowedFixedHeightCount: Int,
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
    fromEnd: Boolean = false,
): String {
    val startIndex = indexOf(start)
    val endIndex =
        if (fromEnd) {
            lastIndexOf(end)
        } else {
            indexOf(end, startIndex = startIndex.coerceAtLeast(0))
        }
    require(startIndex >= 0) { "Missing function start: $start" }
    require(endIndex > startIndex) { "Missing function end: $end" }
    return substring(startIndex, endIndex)
}

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private const val DEBUG_LIQUID_SEARCH_FORM_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidSearchFormCard.kt"

private const val DEBUG_LIQUID_CATALOG_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidCatalogCard.kt"

private const val DEBUG_LIQUID_CATALOG_SAMPLES_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidCatalogSamples.kt"

private const val DEBUG_LIQUID_CHROME_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidChromeCard.kt"

private const val DEBUG_LIQUID_ACTION_MENU_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidActionMenuCard.kt"

private const val DEBUG_COMPONENT_LAB_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugComponentLabPage.kt"

private const val DEBUG_LIQUID_CATALOG_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidCatalogPage.kt"
