package os.kei.feature.github.model

enum class GitHubProfileDepth(
    val storageId: String,
) {
    Basic("basic"),
    Deep("deep");

    companion object {
        fun fromStorageId(value: String): GitHubProfileDepth {
            return entries.firstOrNull { it.storageId == value } ?: Basic
        }
    }
}
