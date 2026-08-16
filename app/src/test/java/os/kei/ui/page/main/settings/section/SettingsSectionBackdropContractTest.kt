package os.kei.ui.page.main.settings.section

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class SettingsSectionBackdropContractTest {
    @Test
    fun managedBackgroundBackdropDrawsThemeBaseBeforeSceneContent() {
        val source = sourceFile(APP_MANAGED_BACKGROUND_SOURCE)
        // Anchored on the scene producer specifically: the file also holds `rememberAppPageBackdrop`,
        // whose recording skips its base precisely *because* this scene is drawn under it.
        val sceneStart = source.indexOf("val sceneBackdrop =")
        val producerStart = source.indexOf("rememberLayerBackdrop {", sceneStart)
        val producerEnd = source.indexOf("}", producerStart)
        require(sceneStart >= 0 && producerStart >= 0 && producerEnd > producerStart) {
            "Unable to locate the managed background backdrop producer"
        }
        val producer = source.substring(producerStart, producerEnd)

        assertTrue("drawRect(baseColor)" in producer)
        assertTrue("drawContent()" in producer)
        assertTrue(
            producer.indexOf("drawRect(baseColor)") < producer.indexOf("drawContent()"),
            "The page base must be drawn into the backdrop before its image and overlay content",
        )
    }

    @Test
    fun dropdownAndSliderCardsExportTheirRenderedMaterial() {
        val contracts =
            listOf(
                SettingsCardBackdropContract(
                    sourcePath = SETTINGS_LOG_SECTION_SOURCE,
                    marker = "R.string.settings_group_log_level_header",
                ),
                SettingsCardBackdropContract(
                    sourcePath = SETTINGS_NOTIFY_SECTION_SOURCE,
                    marker = "R.string.settings_group_notify_header",
                ),
                SettingsCardBackdropContract(
                    sourcePath = SETTINGS_BACKGROUND_SECTION_SOURCE,
                    marker = "R.string.settings_group_background_layout_header",
                ),
                SettingsCardBackdropContract(
                    sourcePath = SETTINGS_BACKGROUND_SECTION_SOURCE,
                    marker = "R.string.settings_group_background_rendering_header",
                ),
            )

        contracts.forEach { contract ->
            val source = sourceFile(contract.sourcePath)
            val markerIndex = source.indexOf(contract.marker)
            val cardStart = source.lastIndexOf("SettingsGroupCard(", markerIndex)
            val cardEnd = source.indexOf(") {", markerIndex)
            require(markerIndex >= 0 && cardStart >= 0 && cardEnd > markerIndex) {
                "Unable to locate SettingsGroupCard for ${contract.marker}"
            }
            val cardArguments = source.substring(cardStart, cardEnd)
            assertTrue(
                "exportBackdropToContent = true," in cardArguments,
                "${contract.marker} must export the rendered card material to child controls",
            )
        }
    }
}

private data class SettingsCardBackdropContract(
    val sourcePath: String,
    val marker: String,
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

private const val SETTINGS_LOG_SECTION_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/settings/section/SettingsLogSection.kt"
private const val SETTINGS_NOTIFY_SECTION_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/settings/section/SettingsNotifySection.kt"
private const val SETTINGS_BACKGROUND_SECTION_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/settings/section/SettingsBackgroundSection.kt"
private const val APP_MANAGED_BACKGROUND_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/widget/chrome/AppManagedBackground.kt"
