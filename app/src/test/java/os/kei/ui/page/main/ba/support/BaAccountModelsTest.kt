package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.assertEquals

class BaAccountModelsTest {
    @Test
    fun `global nickname keeps twelve characters`() {
        assertEquals("ABCDEFGHIJKL", sanitizeBaAccountNickname("ABCDEFGHIJKLM", serverIndex = 1))
    }

    @Test
    fun `cn and jp nickname keep ten characters`() {
        assertEquals("ABCDEFGHIJ", sanitizeBaAccountNickname("ABCDEFGHIJKL", serverIndex = 0))
        assertEquals("ABCDEFGHIJ", sanitizeBaAccountNickname("ABCDEFGHIJKL", serverIndex = 2))
    }

    @Test
    fun `cn friend code keeps seven lowercase letters and digits`() {
        assertEquals("ab12cd3", normalizeBaAccountFriendCodeInput(" AB12cd34Z ", serverIndex = 0))
        assertEquals("ab12cd3", sanitizeBaAccountFriendCode(" AB12cd34Z ", serverIndex = 0))
        assertEquals("arisuke", sanitizeBaAccountFriendCode("A1B2", serverIndex = 0))
    }

    @Test
    fun `global and jp friend code normalize to uppercase`() {
        assertEquals("ABCDWXYZ", normalizeBaAccountFriendCodeInput(" abcd1234wxyz ", serverIndex = 1))
        assertEquals("YUKIARIS", sanitizeBaAccountFriendCode(" yuki-aris ", serverIndex = 2))
        assertEquals(BA_DEFAULT_FRIEND_CODE, sanitizeBaAccountFriendCode(" yuki0001z ", serverIndex = 2))
    }

    @Test
    fun `display name fallback keeps full sanitized nickname`() {
        assertEquals("ABCDEFGHIJKL", sanitizeBaAccountDisplayName("", "ABCDEFGHIJKL"))
    }

    @Test
    fun `account normalization uses profile server identity rules`() {
        val record =
            BaAccountRecord(
                profile =
                    BaAccountProfile(
                        id = BaAccountId("cn-main"),
                        serverIndex = 0,
                        displayName = "",
                        nickname = "ABCDEFGHIJKL",
                        friendCode = "AB12cd3",
                    ),
            )

        val normalized = record.normalized(defaultSortOrder = 0)

        assertEquals("ABCDEFGHIJ", normalized?.profile?.nickname)
        assertEquals("ab12cd3", normalized?.profile?.friendCode)
    }

    @Test
    fun `runtime normalization clamps cafe stored ap to cafe capacity`() {
        val runtime =
            BaAccountRuntime(
                cafeLevel = 1,
                cafeStoredAp = 999.0,
            )

        val normalized = runtime.normalized()

        assertEquals(cafeStorageCap(1), normalized.cafeStoredAp)
    }
}
