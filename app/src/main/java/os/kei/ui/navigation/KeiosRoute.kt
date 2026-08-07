package os.kei.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Type-safe navigation keys for KeiOS.
 */
@Serializable
sealed interface KeiosRoute : NavKey {
    @Serializable
    data object Main : KeiosRoute

    @Serializable
    data object Settings : KeiosRoute

    @Serializable
    data object McpSkill : KeiosRoute

    @Serializable
    data object GitHubActionsNotificationHistory : KeiosRoute

    @Serializable
    data object About : KeiosRoute

    @Serializable
    data class BaStudentGuide(
        val nonce: Long = 0L
    ) : KeiosRoute

    @Serializable
    data class BaGuideCatalog(
        val openBgmPlaybackToken: Long = 0L
    ) : KeiosRoute

    /**
     * @param serverIndex CN/Global/JP as 0..2, or `null` to keep whatever the page last showed.
     * @param nonce keeps the content key unique when the same server is opened again while an
     *   earlier instance is still on the back stack; NavDisplay rejects duplicate content keys.
     */
    @Serializable
    data class BaActivityCalendar(
        val serverIndex: Int? = null,
        val nonce: Long = 0L
    ) : KeiosRoute

    /** @see BaActivityCalendar for the parameter contract. */
    @Serializable
    data class BaPool(
        val serverIndex: Int? = null,
        val nonce: Long = 0L
    ) : KeiosRoute

    @Serializable
    data object WebDavSync : KeiosRoute
}
