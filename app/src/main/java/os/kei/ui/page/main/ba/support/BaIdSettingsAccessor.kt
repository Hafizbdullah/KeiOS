package os.kei.ui.page.main.ba.support

internal interface BaIdKeyValueStore {
    fun decodeBool(key: String, defaultValue: Boolean): Boolean
    fun encode(key: String, value: Boolean)
    fun decodeString(key: String, defaultValue: String): String?
    fun encode(key: String, value: String)
}

internal class BaIdSettingsAccessor(
    private val store: BaIdKeyValueStore,
) {
    fun loadIndependentByServerEnabled(): Boolean =
        store.decodeBool(KEY_ID_INDEPENDENT_BY_SERVER, false)

    fun saveIndependentByServerEnabled(enabled: Boolean) {
        store.encode(KEY_ID_INDEPENDENT_BY_SERVER, enabled)
    }

    fun loadNickname(serverIndex: Int? = null): String {
        if (serverIndex != null && loadIndependentByServerEnabled()) {
            val raw = store.decodeString(idNicknameServerKey(serverIndex), "").orEmpty()
            if (raw.isNotBlank()) return sanitizeNickname(raw, serverIndex)
        }
        return loadSharedNickname()
    }

    fun saveNickname(name: String, serverIndex: Int? = null) {
        val serverSpecific = serverIndex != null && loadIndependentByServerEnabled()
        val key = if (serverSpecific) {
            idNicknameServerKey(serverIndex)
        } else {
            KEY_ID_NICKNAME
        }
        val sanitized = sanitizeNickname(name, if (serverSpecific) serverIndex else null)
        store.encode(key, sanitized)
    }

    fun loadFriendCode(serverIndex: Int? = null): String {
        if (serverIndex != null && loadIndependentByServerEnabled()) {
            val raw = store.decodeString(idFriendCodeServerKey(serverIndex), "").orEmpty()
            if (raw.isNotBlank()) return sanitizeFriendCode(raw, serverIndex)
        }
        return loadSharedFriendCode()
    }

    fun saveFriendCode(code: String, serverIndex: Int? = null) {
        val serverSpecific = serverIndex != null && loadIndependentByServerEnabled()
        val key = if (serverSpecific) {
            idFriendCodeServerKey(serverIndex)
        } else {
            KEY_ID_FRIEND_CODE
        }
        val sanitized = sanitizeFriendCode(code, if (serverSpecific) serverIndex else null)
        store.encode(key, sanitized)
    }

    private fun loadSharedNickname(): String {
        return sanitizeNickname(
            store.decodeString(KEY_ID_NICKNAME, BA_DEFAULT_NICKNAME).orEmpty(),
            serverIndex = null,
        )
    }

    private fun loadSharedFriendCode(): String {
        return sanitizeFriendCode(
            store.decodeString(KEY_ID_FRIEND_CODE, BA_DEFAULT_FRIEND_CODE).orEmpty(),
            serverIndex = null,
        )
    }

    private fun sanitizeNickname(name: String, serverIndex: Int?): String {
        return sanitizeBaAccountNickname(name, serverIndex)
    }

    private fun sanitizeFriendCode(code: String, serverIndex: Int?): String {
        return sanitizeBaAccountFriendCode(code, serverIndex)
    }

    private fun idNicknameServerKey(serverIndex: Int): String =
        "$KEY_ID_NICKNAME_SERVER_PREFIX${serverIndex.coerceIn(0, 2)}"

    private fun idFriendCodeServerKey(serverIndex: Int): String =
        "$KEY_ID_FRIEND_CODE_SERVER_PREFIX${serverIndex.coerceIn(0, 2)}"

    private companion object {
        private const val KEY_ID_NICKNAME = "id_nickname"
        private const val KEY_ID_FRIEND_CODE = "id_friend_code"
        private const val KEY_ID_INDEPENDENT_BY_SERVER = "id_independent_by_server"
        private const val KEY_ID_NICKNAME_SERVER_PREFIX = "id_nickname_server_"
        private const val KEY_ID_FRIEND_CODE_SERVER_PREFIX = "id_friend_code_server_"
    }
}
