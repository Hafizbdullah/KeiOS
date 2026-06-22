package os.kei.ui.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class KeiosRouteSerializationTest {
    @Test
    fun routeStackRoundTripsThroughKotlinxSerialization() {
        val routes =
            listOf(
                KeiosRoute.Main,
                KeiosRoute.Settings,
                KeiosRoute.McpSkill,
                KeiosRoute.GitHubActionsNotificationHistory,
                KeiosRoute.About,
                KeiosRoute.BaStudentGuide(nonce = 42L),
                KeiosRoute.BaGuideCatalog(openBgmPlaybackToken = 7L),
                KeiosRoute.WebDavSync,
            )

        val encoded = Json.encodeToString(routes)
        val decoded = Json.decodeFromString<List<KeiosRoute>>(encoded)

        assertEquals(routes, decoded)
    }
}
