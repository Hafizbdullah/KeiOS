package os.kei.ui.page.main.jsonimport

import java.io.File
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeiOSJsonImportBackdropContractTest {
    @Test
    fun managedPageMaterialFollowsLiquidPreferences() {
        val activity = sourceFile(JSON_IMPORT_ACTIVITY_SOURCE)

        assertTrue("val prefsSnapshot = remember { UiPrefs.loadSnapshot() }" in activity)
        assertTrue(
            "LocalLiquidControlsEnabled provides prefsSnapshot.liquidSwitchEnabled," in activity,
        )
        assertTrue("saturation = prefsSnapshot.nonHomeBackgroundSaturation," in activity)
        assertTrue("exportBackdropToContent = true," in activity)
    }

    @Test
    fun nestedLiquidControlsInheritTheirCardMaterial() {
        val source = sourceFile(JSON_IMPORT_CONTENT_SOURCE)
        listOf(
            CardContract("JsonImportStatusCard", "JsonImportSourceCard"),
            CardContract("JsonImportPreviewCard", "JsonImportSamplesCard"),
            CardContract("JsonImportActionCard", "JsonImportPreviewPills"),
        ).forEach { contract ->
            val card =
                source.functionBody(
                    start = "private fun ${contract.functionName}(",
                    end = "private fun ${contract.nextFunctionName}(",
                )

            assertTrue(
                "exportBackdropToContent = true," in card,
                "${contract.functionName} must expose its rendered material to nested Liquid controls",
            )
            assertFalse(
                "rememberLayerBackdrop()" in card,
                "${contract.functionName} must rely on AppFeatureCard's exported material",
            )
            assertFalse(
                ".layerBackdrop(" in card,
                "${contract.functionName} must not sample a backdrop that it also produces",
            )
        }
    }

    @Test
    fun errorMessageUsesTheStatusCardBackdropAndCompactSupportingMaterial() {
        val source = sourceFile(JSON_IMPORT_CONTENT_SOURCE)
        val statusCard =
            source.functionBody(
                start = "private fun JsonImportStatusCard(",
                end = "private fun JsonImportSourceCard(",
            )
        val supportingBlock =
            source.functionBody(
                start = "internal fun JsonImportErrorSupportingBlock(",
                end = "private fun JsonImportSourceCard(",
            )

        assertTrue("exportBackdropToContent = true," in statusCard)
        assertTrue("JsonImportErrorSupportingBlock(errorMessage = state.errorMessage)" in statusCard)
        assertTrue("if (errorMessage.isBlank()) return" in supportingBlock)
        assertTrue("AppSupportingBlock(" in supportingBlock)
        assertTrue("text = errorMessage," in supportingBlock)
        assertTrue("accentColor = errorColor," in supportingBlock)
        assertTrue(
            "containerColor = errorColor.copy(alpha = if (isDark) 0.12f else 0.08f)," in supportingBlock,
        )
        assertTrue("contentColor = MiuixTheme.colorScheme.onErrorContainer," in supportingBlock)
        assertTrue(
            "contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)," in supportingBlock,
        )
        assertTrue("typography = AppTypographyTokens.Supporting," in supportingBlock)
        assertTrue("fillWidth = true," in supportingBlock)
        assertFalse(Regex("\\bText\\(").containsMatchIn(supportingBlock))
        assertFalse("maxLines =" in supportingBlock)
        assertFalse("overflow =" in supportingBlock)
        assertFalse("rememberLayerBackdrop()" in supportingBlock)
        assertFalse(".layerBackdrop(" in supportingBlock)
    }

    @Test
    fun informationOnlyCardsAvoidUnusedMaterialExports() {
        val source = sourceFile(JSON_IMPORT_CONTENT_SOURCE)
        listOf(
            CardContract("JsonImportSourceCard", "JsonImportPreviewCard"),
            CardContract("JsonImportSamplesCard", "JsonImportActionCard"),
        ).forEach { contract ->
            val card =
                source.functionBody(
                    start = "private fun ${contract.functionName}(",
                    end = "private fun ${contract.nextFunctionName}(",
                )

            assertFalse(
                "exportBackdropToContent = true," in card,
                "${contract.functionName} has no nested Liquid surface that needs a second material layer",
            )
            assertFalse("rememberLayerBackdrop()" in card, contract.functionName)
            assertFalse(".layerBackdrop(" in card, contract.functionName)
        }
    }

    @Test
    fun topBarSamplingRemainsIndependentFromCardMaterial() {
        val page = sourceFile(JSON_IMPORT_PAGE_SOURCE)
        val content = sourceFile(JSON_IMPORT_CONTENT_SOURCE)

        assertTrue("val pageBackdrop = rememberLayerBackdrop()" in page)
        assertTrue("titleBackdrop = pageBackdrop," in page)
        assertTrue("backdrop = pageBackdrop," in page)
        assertTrue(".layerBackdrop(pageBackdrop)" in content)
        assertFalse("rememberLayerBackdrop()" in content)
        assertFalse("LocalLiquidParentBackdrop provides pageBackdrop" in page)
        assertFalse("LocalLiquidParentBackdrop provides pageBackdrop" in content)
    }
}

private data class CardContract(
    val functionName: String,
    val nextFunctionName: String,
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

private const val JSON_IMPORT_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/jsonimport/KeiOSJsonImportPage.kt"
private const val JSON_IMPORT_ACTIVITY_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/jsonimport/KeiOSJsonImportActivity.kt"
private const val JSON_IMPORT_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/jsonimport/KeiOSJsonImportContent.kt"
