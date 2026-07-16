package os.kei.ui.page.main.about.section

import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AboutAppCardSectionContractTest {
    @Test
    fun appCardReusesFeatureCardWithoutChangingItsVisualOrInteractionContract() {
        val source = aboutAppCardSource()

        assertTrue("AppFeatureCard(" in source)
        assertFalse("AppSurfaceCard(" in source)
        assertFalse("AppCardHeader(" in source)
        assertFalse("AnimatedVisibility(" in source)
        assertFalse("AppInfoListBody(" in source)

        assertTrue("containerColor = cardColor" in source)
        assertTrue("contentColor = MiuixTheme.colorScheme.onBackground" in source)
        assertTrue("titleColor = accent" in source)
        assertTrue("subtitleColor = subtitleColor" in source)
        assertTrue("size = AppInteractiveTokens.cardHeaderLeadingSlotSize" in source)
        assertTrue("collapsible = true" in source)
        assertTrue("expanded = expanded" in source)
        assertTrue("onExpandedChange = onExpandedChange" in source)
        assertTrue("collapseOnSurfaceClick = true" in source)
        assertTrue("top = CardLayoutRhythm.sectionGap" in source)
        assertTrue("contentVerticalSpacing = 0.dp" in source)
    }
}

private fun aboutAppCardSource(): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val sourceFile =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, ABOUT_APP_CARD_SOURCE) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $ABOUT_APP_CARD_SOURCE from $workingDirectory"
    }.readText()
}

private const val ABOUT_APP_CARD_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/about/section/AboutAppCardSection.kt"
