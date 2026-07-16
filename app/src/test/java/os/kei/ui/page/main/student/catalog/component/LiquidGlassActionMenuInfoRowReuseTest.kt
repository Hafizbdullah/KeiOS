package os.kei.ui.page.main.student.catalog.component

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class LiquidGlassActionMenuInfoRowReuseTest {
    @Test
    fun baRefreshScopeUsesPassiveInfoWhileFullRefreshRemainsDisabledAction() {
        val source = sourceFile(BA_CATALOG_MORE_ACTION_POPUP_SOURCE)
        val refreshScopeItem =
            source.itemBody(
                constructor = "LiquidGlassActionMenuInfoRow",
                id = "refresh_scope",
            )
        val fullRefreshItem =
            source.itemBody(
                constructor = "LiquidGlassActionMenuActionRow",
                id = "full_refresh",
            )

        assertEquals(1, source.occurrencesOf("LiquidGlassActionMenuInfoRow("))
        assertTrue("ba_catalog_more_refresh_scope_title" in refreshScopeItem)
        assertTrue("ba_catalog_more_refresh_scope_summary" in refreshScopeItem)
        assertTrue("leadingIcon = infoIcon" in refreshScopeItem)
        assertFalse("enabled =" in refreshScopeItem)
        assertFalse("onClick =" in refreshScopeItem)

        assertTrue("ba_catalog_more_full_refresh_title" in fullRefreshItem)
        assertTrue("enabled = false" in fullRefreshItem)
        assertTrue("onClick = {}" in fullRefreshItem)
        assertTrue("backdrop = backdrop," in source)
        assertFalse("rememberLayerBackdrop" in source)
        assertFalse(".layerBackdrop(" in source)
    }

    @Test
    fun componentLabPlacesPassiveInfoBesideARealDisabledAction() {
        val source = sourceFile(DEBUG_LIQUID_ACTION_MENU_SOURCE)
        val infoItem =
            source.itemBody(
                constructor = "LiquidGlassActionMenuInfoRow",
                id = "passive_info",
            )
        val disabledItem =
            source.itemBody(
                constructor = "LiquidGlassActionMenuActionRow",
                id = "disabled",
            )

        assertEquals(1, source.occurrencesOf("LiquidGlassActionMenuInfoRow("))
        assertTrue("text = infoLabel" in infoItem)
        assertTrue("subtitle = infoSummary" in infoItem)
        assertTrue("leadingIcon = appLucideInfoIcon()" in infoItem)
        assertFalse("enabled =" in infoItem)
        assertFalse("onClick =" in infoItem)

        assertTrue("text = disabledLabel" in disabledItem)
        assertTrue("enabled = false" in disabledItem)
        assertTrue("onClick = {}" in disabledItem)
        assertTrue(source.indexOf("id = \"passive_info\"") < source.indexOf("id = \"disabled\""))
        assertTrue("backdrop = backdrop," in source)
        assertFalse("rememberLayerBackdrop" in source)
        assertFalse(".layerBackdrop(" in source)
    }

    @Test
    fun componentLabInfoCopyIsLocalizedAcrossSupportedResources() {
        ABOUT_STRING_SOURCES.forEach { path ->
            val source = sourceFile(path)
            assertTrue(
                "name=\"debug_component_lab_liquid_action_menu_info\"" in source,
                path,
            )
            assertTrue(
                "name=\"debug_component_lab_liquid_action_menu_info_summary\"" in source,
                path,
            )
        }
    }
}

private fun String.itemBody(
    constructor: String,
    id: String,
): String {
    val idIndex = indexOf("id = \"$id\",")
    require(idIndex >= 0) { "Unable to locate item id=$id" }
    val itemStart = lastIndexOf("$constructor(", startIndex = idIndex)
    require(itemStart >= 0) { "Unable to locate $constructor with id=$id" }

    var depth = 0
    var insideString = false
    var escaped = false
    for (index in itemStart until length) {
        val character = this[index]
        when {
            escaped -> escaped = false
            character == '\\' && insideString -> escaped = true
            character == '"' -> insideString = !insideString
            insideString -> Unit
            character == '(' -> depth += 1
            character == ')' -> {
                depth -= 1
                if (depth == 0) return substring(itemStart, index + 1)
            }
        }
    }
    error("Unbalanced $constructor call with id=$id")
}

private fun String.occurrencesOf(needle: String): Int =
    windowed(needle.length).count { candidate -> candidate == needle }

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

private const val BA_CATALOG_MORE_ACTION_POPUP_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/BaGuideCatalogMoreActionPopup.kt"
private const val DEBUG_LIQUID_ACTION_MENU_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidActionMenuCard.kt"
private val ABOUT_STRING_SOURCES =
    listOf(
        "app/src/main/res/values/strings_about.xml",
        "app/src/main/res/values-zh-rCN/strings_about.xml",
        "app/src/main/res/values-en/strings_about.xml",
        "app/src/main/res/values-ja/strings_about.xml",
    )
