package os.kei.ui.page.main.github.sheet

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class GitHubDetailInfoRowReuseTest {
    @Test
    fun detailRowsDelegateToTopAlignedSharedInfoRowsWithoutChangingGeometry() {
        wrapperSpecs.forEach { spec ->
            val source = sourceFile(spec.relativePath)
            val body = functionBlock(source, spec.functionMarker)

            assertTrue("import os.kei.ui.page.main.widget.core.AppInfoRow" in source, spec.relativePath)
            assertTrue("AppInfoRow(" in body, spec.functionMarker)
            assertTrue("labelWeight = ${spec.labelWeight}" in body, spec.functionMarker)
            assertTrue("valueWeight = ${spec.valueWeight}" in body, spec.functionMarker)
            assertTrue("horizontalSpacing = 8.dp" in body, spec.functionMarker)
            assertTrue("rowVerticalPadding = 0.dp" in body, spec.functionMarker)
            assertTrue("verticalAlignment = Alignment.Top" in body, spec.functionMarker)
            assertTrue("valueTextAlign = TextAlign.Start" in body, spec.functionMarker)
            assertTrue("labelMaxLines = ${spec.labelMaxLines}" in body, spec.functionMarker)
            assertTrue("valueMaxLines = ${spec.valueMaxLines}" in body, spec.functionMarker)
            assertTrue("labelOverflow = TextOverflow.Ellipsis" in body, spec.functionMarker)
            assertTrue("valueOverflow = TextOverflow.Ellipsis" in body, spec.functionMarker)
            assertTrue("labelFontSize = AppTypographyTokens.Supporting.fontSize" in body, spec.functionMarker)
            assertTrue("labelLineHeight = AppTypographyTokens.Supporting.lineHeight" in body, spec.functionMarker)
            assertTrue("valueFontSize = AppTypographyTokens.Supporting.fontSize" in body, spec.functionMarker)
            assertTrue("valueLineHeight = AppTypographyTokens.Supporting.lineHeight" in body, spec.functionMarker)
            assertTrue("emphasizedValue = false" in body, spec.functionMarker)
            assertFalse(Regex("(?m)^\\s*Row\\(").containsMatchIn(body), spec.functionMarker)
        }
    }

    @Test
    fun optionalApkAndFdroidRowsStillSkipBlankValues() {
        wrapperSpecs
            .filter(WrapperSpec::skipsBlankValue)
            .forEach { spec ->
                val body = functionBlock(sourceFile(spec.relativePath), spec.functionMarker)
                assertTrue("if (value.isBlank()) return" in body, spec.functionMarker)
            }
    }
}

private data class WrapperSpec(
    val relativePath: String,
    val functionMarker: String,
    val labelWeight: String,
    val valueWeight: String,
    val labelMaxLines: String,
    val valueMaxLines: String,
    val skipsBlankValue: Boolean,
)

private val wrapperSpecs =
    listOf(
        WrapperSpec(
            relativePath = REPOSITORY_PROFILE_SOURCE,
            functionMarker = "private fun DetailInfoRow(",
            labelWeight = "0.28f",
            valueWeight = "0.72f",
            labelMaxLines = "2",
            valueMaxLines = "3",
            skipsBlankValue = false,
        ),
        WrapperSpec(
            relativePath = DECISION_ASSIST_SOURCE,
            functionMarker = "private fun DetailInfoRow(",
            labelWeight = "0.28f",
            valueWeight = "0.72f",
            labelMaxLines = "1",
            valueMaxLines = "valueMaxLines",
            skipsBlankValue = false,
        ),
        WrapperSpec(
            relativePath = APK_INFO_SOURCE,
            functionMarker = "internal fun InfoRow(",
            labelWeight = "0.36f",
            valueWeight = "0.64f",
            labelMaxLines = "1",
            valueMaxLines = "valueMaxLines",
            skipsBlankValue = true,
        ),
        WrapperSpec(
            relativePath = FDROID_DETAIL_SOURCE,
            functionMarker = "private fun FdroidInfoRow(",
            labelWeight = "0.36f",
            valueWeight = "0.64f",
            labelMaxLines = "1",
            valueMaxLines = "valueMaxLines.coerceAtLeast(1)",
            skipsBlankValue = true,
        ),
    )

private fun functionBlock(
    source: String,
    marker: String,
): String {
    val functionStart = source.indexOf(marker)
    require(functionStart >= 0) { "Unable to locate $marker" }
    val bodyStart = source.indexOf('{', startIndex = functionStart)
    require(bodyStart >= 0) { "Unable to locate body for $marker" }
    var depth = 0
    for (index in bodyStart until source.length) {
        when (source[index]) {
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) {
                    return source.substring(functionStart, index + 1)
                }
            }
        }
    }
    error("Unable to locate closing brace for $marker")
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

private const val REPOSITORY_PROFILE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubRepositoryProfileDetailContent.kt"
private const val DECISION_ASSIST_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubDecisionAssistDetailSheets.kt"
private const val APK_INFO_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubApkInfoSections.kt"
private const val FDROID_DETAIL_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubFdroidDetailSheet.kt"
