package os.kei.ui.page.main.mcp.model

internal fun String.toMcpTokenPreview(): String {
    val token = trim()
    if (token.isBlank()) return ""
    if (token.length <= 8) return token
    return "${token.take(4)}...${token.takeLast(4)}"
}
