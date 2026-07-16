package os.kei.ui.page.main.student.tabcontent.simulate

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class GuideSimulateAbilityChoiceSemanticsTest {
    @Test
    fun abilityChoicesExposeOneRadioGroupWithoutChangingCompactVisualRhythm() {
        val source = sourceFile(GUIDE_SIMULATE_ABILITY_CARD_SOURCE)
        val choiceRowSource =
            source
                .substringAfter("listOf(\"初始能力\" to initialStatsLabel")
                .substringBefore("selectedHint.takeIf")

        assertEquals(1, source.occurrencesOf(".selectableGroup()"))
        assertTrue("horizontalArrangement = Arrangement.spacedBy(8.dp)" in source)
        assertTrue("variant = GlassVariant.Compact" in choiceRowSource)
        assertTrue("role = Role.RadioButton" in choiceRowSource)
        assertTrue("selected = selected" in choiceRowSource)
        assertTrue("containerColor = if (selected) Color(0x443B82F6) else null" in choiceRowSource)
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

private fun String.occurrencesOf(needle: String): Int =
    windowed(needle.length).count { candidate -> candidate == needle }

private const val GUIDE_SIMULATE_ABILITY_CARD_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/tabcontent/simulate/GuideSimulateAbilityCard.kt"
