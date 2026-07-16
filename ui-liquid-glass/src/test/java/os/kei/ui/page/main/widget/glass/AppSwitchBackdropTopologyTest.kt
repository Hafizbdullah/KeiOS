package os.kei.ui.page.main.widget.glass

import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSwitchBackdropTopologyTest {
    @Test
    fun switchCombinesRawTrackBackdropWithParentBackdrop() {
        val source = sourceFile(APP_SWITCH_SOURCE)
        val toggleStart = source.indexOf("private fun LiquidSwitchToggle(")
        require(toggleStart >= 0) { "Missing LiquidSwitchToggle" }
        val toggleSource = source.substring(toggleStart)

        assertTrue(
            "rememberCombinedBackdrop(\n            backdrop,\n            trackBackdrop,\n        )" in toggleSource,
        )
        assertFalse("rememberBackdrop(trackBackdrop)" in toggleSource)
        assertFalse("import com.kyant.backdrop.backdrops.rememberBackdrop" in source)
    }

    @Test
    fun trackProducerAndThumbConsumerRemainSiblingLayers() {
        val source = sourceFile(APP_SWITCH_SOURCE)
        val toggleStart = source.indexOf("private fun LiquidSwitchToggle(")
        val trackProducerStart = source.indexOf(".layerBackdrop(trackBackdrop)", toggleStart)
        val trackProducerEnd = source.indexOf(".size(64.dp, 28.dp)", trackProducerStart)
        val thumbStart = source.indexOf("backdrop = combinedBackdrop", trackProducerEnd)

        require(toggleStart >= 0) { "Missing LiquidSwitchToggle" }
        require(trackProducerStart > toggleStart) { "Missing track backdrop producer" }
        require(trackProducerEnd > trackProducerStart) { "Missing track producer boundary" }
        require(thumbStart > trackProducerEnd) { "Missing sibling thumb consumer" }

        val trackProducer = source.substring(trackProducerStart, trackProducerEnd)
        assertFalse("combinedBackdrop" in trackProducer)
        assertTrue(".layerBackdrop(trackBackdrop)" in trackProducer)
        assertTrue("backdrop = combinedBackdrop" in source.substring(trackProducerEnd))
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

private const val APP_SWITCH_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/AppSwitch.kt"
