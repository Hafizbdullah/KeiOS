package os.kei.feature.ba.identity

import java.util.Locale

public const val BA_SERVER_INDEX_CN: Int = 0
public const val BA_SERVER_INDEX_GLOBAL: Int = 1
public const val BA_SERVER_INDEX_JP: Int = 2
public const val BA_FALLBACK_NICKNAME: String = "Kei"
public const val BA_FALLBACK_FRIEND_CODE: String = "ARISUKEI"
public const val BA_CN_FRIEND_CODE_LENGTH: Int = 7
public const val BA_DEFAULT_FRIEND_CODE_LENGTH: Int = 8
public const val BA_DEFAULT_NICKNAME_MAX_LENGTH: Int = 10
public const val BA_GLOBAL_NICKNAME_MAX_LENGTH: Int = 12

public enum class BaFriendCodeCase {
    Lowercase,
    Uppercase,
}

public data class BaFriendCodePolicy(
    val length: Int,
    val letterCase: BaFriendCodeCase,
    val digitsAllowed: Boolean,
)

public data class BaIdentityPolicy(
    val serverIndex: Int,
    val nicknameMaxLength: Int,
    val friendCode: BaFriendCodePolicy,
)

public fun normalizeBaServerIndex(serverIndex: Int?): Int =
    serverIndex?.coerceIn(BA_SERVER_INDEX_CN, BA_SERVER_INDEX_JP) ?: BA_SERVER_INDEX_JP

public fun baIdentityPolicy(serverIndex: Int?): BaIdentityPolicy {
    return when (normalizeBaServerIndex(serverIndex)) {
        BA_SERVER_INDEX_CN ->
            BaIdentityPolicy(
                serverIndex = BA_SERVER_INDEX_CN,
                nicknameMaxLength = BA_DEFAULT_NICKNAME_MAX_LENGTH,
                friendCode =
                    BaFriendCodePolicy(
                        length = BA_CN_FRIEND_CODE_LENGTH,
                        letterCase = BaFriendCodeCase.Lowercase,
                        digitsAllowed = true,
                    ),
            )

        BA_SERVER_INDEX_GLOBAL ->
            BaIdentityPolicy(
                serverIndex = BA_SERVER_INDEX_GLOBAL,
                nicknameMaxLength = BA_GLOBAL_NICKNAME_MAX_LENGTH,
                friendCode =
                    BaFriendCodePolicy(
                        length = BA_DEFAULT_FRIEND_CODE_LENGTH,
                        letterCase = BaFriendCodeCase.Uppercase,
                        digitsAllowed = false,
                    ),
            )

        else ->
            BaIdentityPolicy(
                serverIndex = BA_SERVER_INDEX_JP,
                nicknameMaxLength = BA_DEFAULT_NICKNAME_MAX_LENGTH,
                friendCode =
                    BaFriendCodePolicy(
                        length = BA_DEFAULT_FRIEND_CODE_LENGTH,
                        letterCase = BaFriendCodeCase.Uppercase,
                        digitsAllowed = false,
                    ),
            )
    }
}

public fun normalizeBaNicknameInput(
    name: String,
    serverIndex: Int? = null,
): String {
    val policy = baIdentityPolicy(serverIndex)
    return name
        .trim()
        .takeCodePoints(policy.nicknameMaxLength)
}

public fun sanitizeBaNickname(
    name: String,
    serverIndex: Int? = null,
): String =
    normalizeBaNicknameInput(name, serverIndex)
        .ifEmpty { BA_FALLBACK_NICKNAME }

public fun normalizeBaFriendCodeInput(
    code: String,
    serverIndex: Int? = null,
): String {
    val policy = baIdentityPolicy(serverIndex).friendCode
    val normalizedCase =
        when (policy.letterCase) {
            BaFriendCodeCase.Lowercase -> code.trim().lowercase(Locale.ROOT)
            BaFriendCodeCase.Uppercase -> code.trim().uppercase(Locale.ROOT)
        }
    return normalizedCase
        .filter { char -> char.isAllowedFriendCodeChar(policy) }
        .take(policy.length)
}

public fun sanitizeBaFriendCode(
    code: String,
    serverIndex: Int? = null,
): String {
    val policy = baIdentityPolicy(serverIndex).friendCode
    val normalized = normalizeBaFriendCodeInput(code, serverIndex)
    return if (normalized.length == policy.length) {
        normalized
    } else {
        defaultBaFriendCode(serverIndex)
    }
}

public fun defaultBaFriendCode(serverIndex: Int? = null): String =
    normalizeBaFriendCodeInput(BA_FALLBACK_FRIEND_CODE, serverIndex)

public fun isBaFriendCodeConfigured(
    code: String,
    serverIndex: Int? = null,
): Boolean {
    return if (serverIndex == null) {
        listOf(BA_SERVER_INDEX_CN, BA_SERVER_INDEX_GLOBAL, BA_SERVER_INDEX_JP)
            .any { isBaFriendCodeConfiguredForServer(code, it) }
    } else {
        isBaFriendCodeConfiguredForServer(code, serverIndex)
    }
}

private fun isBaFriendCodeConfiguredForServer(
    code: String,
    serverIndex: Int,
): Boolean {
    val policy = baIdentityPolicy(serverIndex).friendCode
    val normalized = normalizeBaFriendCodeInput(code, serverIndex)
    return normalized.length == policy.length &&
        normalized != defaultBaFriendCode(serverIndex)
}

private fun Char.isAllowedFriendCodeChar(policy: BaFriendCodePolicy): Boolean {
    val inLetters =
        when (policy.letterCase) {
            BaFriendCodeCase.Lowercase -> this in 'a'..'z'
            BaFriendCodeCase.Uppercase -> this in 'A'..'Z'
        }
    return inLetters || (policy.digitsAllowed && this in '0'..'9')
}

private fun String.takeCodePoints(maxLength: Int): String {
    if (maxLength <= 0 || isEmpty()) return ""
    val endIndex = offsetByCodePoints(0, codePointCount(0, length).coerceAtMost(maxLength))
    return substring(0, endIndex)
}
