package os.kei.ui.testing

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The macrobenchmark module cannot depend on the app's source set, so [BaselineProfileGenerator]
 * re-spells every test tag as its own string constant. A drift between the two spellings fails
 * only on a device, minutes into a profile run, as `Timed out waiting for testTag=…`. This walks
 * the same ground in a second.
 */
class BaselineProfileTestTagContractTest {
    @Test
    fun everyTagTheGeneratorWaitsForIsDeclaredInTheApp() {
        val declared = keiOsTestTagValues()
        val generatorTags = generatorTagConstants()

        assertTrue(generatorTags.isNotEmpty(), "Unable to parse tag constants out of the generator")
        generatorTags.forEach { (constant, value) ->
            assertTrue(
                value in declared,
                "$constant = \"$value\" matches no KeiOsTestTags entry; the journey would time out",
            )
        }
    }

    @Test
    fun everyPageRootTagIsAppliedThroughTheSharedModifier() {
        // testTagsAsResourceId is what publishes a tag to UiAutomator, and pageRootTestTag is the
        // only place that pairs the two. A page root that reaches for a bare testTag is invisible.
        PAGE_ROOT_SOURCES.forEach { (relativePath, tag) ->
            val source = sourceFile(relativePath)

            assertTrue(
                "pageRootTestTag(KeiOsTestTags.$tag)" in source,
                "$relativePath must tag its root through pageRootTestTag",
            )
        }
    }

    @Test
    fun theSharedModifierStillPublishesTagsAsResourceIds() {
        val source = sourceFile("app/src/main/java/os/kei/ui/testing/PageRootTestTag.kt")

        assertTrue(
            "testTagsAsResourceId = true" in source,
            "pageRootTestTag must keep setting testTagsAsResourceId or every journey goes blind",
        )
    }

    @Test
    fun tagValuesAreUnique() {
        val values = keiOsTestTagValues()

        assertEquals(values.size, values.toSet().size, "Two KeiOsTestTags entries share a value")
    }
}

private fun keiOsTestTagValues(): List<String> =
    CONST_DECLARATION
        .findAll(sourceFile("app/src/main/java/os/kei/ui/testing/KeiOsTestTags.kt"))
        .map { match -> match.groupValues[2] }
        .toList()

private fun generatorTagConstants(): List<Pair<String, String>> =
    CONST_DECLARATION
        .findAll(sourceFile("baselineprofile/src/main/java/os/kei/baselineprofile/BaselineProfileGenerator.kt"))
        .map { match -> match.groupValues[1] to match.groupValues[2] }
        .toList()

private val CONST_DECLARATION = Regex("""const val (\w+)\s*(?:=\s*)?\n?\s*"([^"]+)"""")

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

private val PAGE_ROOT_SOURCES =
    listOf(
        "app/src/main/java/os/kei/ui/page/main/settings/page/SettingsPage.kt" to "SettingsPageRoot",
        "app/src/main/java/os/kei/ui/page/main/about/page/AboutPage.kt" to "AboutPageRoot",
        "app/src/main/java/os/kei/ui/page/main/sync/WebDavSyncPage.kt" to "WebDavSyncPageRoot",
        "app/src/main/java/os/kei/ui/page/main/mcp/skill/page/McpSkillPage.kt" to "McpSkillPageRoot",
        "app/src/main/java/os/kei/ui/page/main/os/shell/page/OsShellRunnerContent.kt" to "OsShellRunnerPageRoot",
        "app/src/main/java/os/kei/ui/page/main/github/history/GitHubActionsNotificationHistoryPage.kt"
            to "GitHubActionsHistoryPageRoot",
    )
