package os.kei.feature.github.data.remote.fdroid

import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import os.kei.core.json.optArray
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.feature.github.model.FdroidIndexFormat
import java.io.Reader
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.cancellation.CancellationException

private const val DEFAULT_STREAM_SEARCH_SCAN_MULTIPLIER = 8
private const val STREAM_PARSER_YIELD_EVERY_PACKAGES = 64
private const val JSON_SCANNER_COOPERATIVE_CHECK_CHARS = 32 * 1024
private val whitespaceRegex = Regex("""\s+""")

object FdroidIndexV2StreamParser {
    suspend fun searchIndex(
        repoUrl: String,
        reader: Reader,
        query: String,
        packageName: String,
        limit: Int
    ): Result<FdroidRepositorySnapshot> = fdroidStreamResult {
        val normalizedRepoUrl = repoUrl.trim().trimEnd('/')
        require(normalizedRepoUrl.isNotBlank()) { "F-Droid repository URL is blank" }
        val normalizedQuery = query.trim()
        val normalizedPackageName = packageName.trim()
        require(normalizedQuery.isNotBlank() || normalizedPackageName.isNotBlank()) {
            "F-Droid search query or package name is required"
        }

        val scanner = JsonScanner(reader)
        var repoSummary = FdroidRepoStreamSummary()
        val matches = LinkedHashMap<String, FdroidPackageSnapshot>()
        var packageScan = FdroidPackageScanResult()
        val matchBudget =
            if (normalizedPackageName.isNotBlank() && normalizedQuery.isBlank()) {
                1
            } else {
                limit.coerceIn(1, 50) * DEFAULT_STREAM_SEARCH_SCAN_MULTIPLIER
            }

        scanner.forEachObjectEntry { key ->
            when (key) {
                "repo" -> {
                    repoSummary = scanner.readRawValue()
                        .parseRepoSummary()
                    JsonObjectScanAction.Continue
                }

                "packages" -> {
                    packageScan = scanPackages(
                        scanner = scanner,
                        normalizedRepoUrl = normalizedRepoUrl,
                        query = normalizedQuery,
                        packageName = normalizedPackageName,
                        matchBudget = matchBudget,
                        matches = matches
                    )
                    JsonObjectScanAction.Continue
                }

                else -> {
                    scanner.skipValue()
                    JsonObjectScanAction.Continue
                }
            }
        }

        FdroidRepositorySnapshot(
            repoUrl = normalizedRepoUrl,
            format = FdroidIndexFormat.V2,
            repoName = repoSummary.name,
            repoDescription = repoSummary.description,
            timestampMillis = repoSummary.timestampMillis,
            mirrors = repoSummary.mirrors,
            packages = matches,
            totalPackageCount = packageScan.totalPackageCount.takeIf {
                packageScan.completed && it > 0
            }
        )
    }

    suspend fun loadPackages(
        repoUrl: String,
        reader: Reader,
        packageNames: Set<String>
    ): Result<FdroidRepositorySnapshot> = fdroidStreamResult {
        val normalizedRepoUrl = repoUrl.trim().trimEnd('/')
        require(normalizedRepoUrl.isNotBlank()) { "F-Droid repository URL is blank" }
        val requestedPackages =
            packageNames
                .map { name -> name.trim() }
                .filter { name -> name.isNotBlank() }
                .associateBy { name -> name.lowercase(Locale.ROOT) }
        require(requestedPackages.isNotEmpty()) { "F-Droid package names are blank" }

        val scanner = JsonScanner(reader)
        var repoSummary = FdroidRepoStreamSummary()
        val matches = LinkedHashMap<String, FdroidPackageSnapshot>()
        var totalPackageCount = 0

        scanner.forEachObjectEntry { key ->
            when (key) {
                "repo" -> {
                    repoSummary = scanner.readRawValue()
                        .parseRepoSummary()
                    JsonObjectScanAction.Continue
                }

                "packages" -> {
                    totalPackageCount = scanPackages(
                        scanner = scanner,
                        normalizedRepoUrl = normalizedRepoUrl,
                        requestedPackageNames = requestedPackages,
                        matches = matches
                    ).totalPackageCount
                    JsonObjectScanAction.Continue
                }

                else -> {
                    scanner.skipValue()
                    JsonObjectScanAction.Continue
                }
            }
        }

        FdroidRepositorySnapshot(
            repoUrl = normalizedRepoUrl,
            format = FdroidIndexFormat.V2,
            repoName = repoSummary.name,
            repoDescription = repoSummary.description,
            timestampMillis = repoSummary.timestampMillis,
            mirrors = repoSummary.mirrors,
            packages = matches,
            totalPackageCount = totalPackageCount.takeIf { it > 0 }
        )
    }

    private suspend fun scanPackages(
        scanner: JsonScanner,
        normalizedRepoUrl: String,
        query: String,
        packageName: String,
        matchBudget: Int,
        matches: LinkedHashMap<String, FdroidPackageSnapshot>
    ): FdroidPackageScanResult {
        var totalPackageCount = 0
        val exactPackageOnly = packageName.isNotBlank() && query.isBlank()
        val exactQueryKey = query.searchKey()
        val completed = scanner.forEachObjectEntry { entryPackageName ->
            totalPackageCount += 1
            if (exactPackageOnly && !entryPackageName.equals(packageName, ignoreCase = true)) {
                scanner.skipValue()
                return@forEachObjectEntry JsonObjectScanAction.Continue
            }
            val rawPackage = scanner.readRawValue()
            val shouldParse =
                entryPackageName.matchesSearch(query = query, packageName = packageName) ||
                    rawPackageMatchesSearch(rawPackage, query)
            if (shouldParse && matches.size < matchBudget) {
                FdroidIndexV2Parser.parsePackage(
                    repoUrl = normalizedRepoUrl,
                    packageName = entryPackageName,
                    rawJson = rawPackage
                ).getOrNull()?.let { snapshot ->
                    matches[snapshot.packageName] = snapshot
                    if (packageName.isBlank() &&
                        exactQueryKey.isNotBlank() &&
                        snapshot.isExactQueryMatch(exactQueryKey)
                    ) {
                        return@forEachObjectEntry JsonObjectScanAction.Stop
                    }
                }
            }
            if (exactPackageOnly && matches.isNotEmpty()) {
                return@forEachObjectEntry JsonObjectScanAction.Stop
            }
            if (totalPackageCount % STREAM_PARSER_YIELD_EVERY_PACKAGES == 0) {
                coroutineContext.ensureActive()
                yield()
            }
            JsonObjectScanAction.Continue
        }
        return FdroidPackageScanResult(
            totalPackageCount = totalPackageCount,
            completed = completed
        )
    }

    private suspend fun scanPackages(
        scanner: JsonScanner,
        normalizedRepoUrl: String,
        requestedPackageNames: Map<String, String>,
        matches: LinkedHashMap<String, FdroidPackageSnapshot>
    ): FdroidPackageScanResult {
        var totalPackageCount = 0
        scanner.forEachObjectEntry { entryPackageName ->
            totalPackageCount += 1
            val requested = requestedPackageNames[entryPackageName.lowercase(Locale.ROOT)]
            if (requested != null) {
                FdroidIndexV2Parser.parsePackage(
                    repoUrl = normalizedRepoUrl,
                    packageName = entryPackageName,
                    rawJson = scanner.readRawValue()
                ).getOrNull()?.let { snapshot ->
                    matches[snapshot.packageName] = snapshot
                }
            } else {
                scanner.skipValue()
            }
            if (totalPackageCount % STREAM_PARSER_YIELD_EVERY_PACKAGES == 0) {
                coroutineContext.ensureActive()
                yield()
            }
            JsonObjectScanAction.Continue
        }
        return FdroidPackageScanResult(
            totalPackageCount = totalPackageCount,
            completed = true
        )
    }

    private fun String.matchesSearch(
        query: String,
        packageName: String
    ): Boolean {
        if (packageName.isNotBlank() && equals(packageName, ignoreCase = true)) return true
        if (query.isBlank()) return false
        return contains(query, ignoreCase = true)
    }

    private fun rawPackageMatchesSearch(
        rawPackage: String,
        query: String
    ): Boolean {
        if (query.isBlank()) return false
        return rawPackage.contains(query, ignoreCase = true)
    }

    private fun FdroidPackageSnapshot.isExactQueryMatch(queryKey: String): Boolean {
        return packageName.equals(queryKey, ignoreCase = true) ||
            appName.searchKey() == queryKey
    }

    private fun String.searchKey(): String =
        trim()
            .lowercase(Locale.ROOT)
            .replace(whitespaceRegex, " ")

    private fun String.parseRepoSummary(): FdroidRepoStreamSummary {
        val repo = parseJsonObjectOrNull() ?: JsonObject(emptyMap())
        return FdroidRepoStreamSummary(
            name = repo.localizedString("name"),
            description = repo.localizedString("description"),
            timestampMillis = repo.longValue("timestamp"),
            mirrors =
                repo.optArray("mirrors")
                    ?.mapNotNull { element ->
                        val obj = element as? JsonObject
                        obj?.stringValue("url")
                            ?: (element as? JsonPrimitive)
                                ?.contentOrNull
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                    }
                    .orEmpty()
        )
    }

    private fun JsonObject.localizedString(key: String): String {
        val element = this[key] ?: return ""
        val primitive = (element as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        val obj = element as? JsonObject ?: return primitive
        return obj.stringValue("en-US")
            .ifBlank { obj.stringValue("en") }
            .ifBlank {
                obj.values.firstNotNullOfOrNull { value ->
                    (value as? JsonPrimitive)
                        ?.contentOrNull
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }.orEmpty()
            }
    }

    private fun JsonObject.longValue(key: String): Long? {
        return stringValue(key).toLongOrNull()
    }

    private fun JsonObject.stringValue(key: String): String {
        return (this[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    }

    private data class FdroidRepoStreamSummary(
        val name: String = "",
        val description: String = "",
        val timestampMillis: Long? = null,
        val mirrors: List<String> = emptyList()
    )

    private data class FdroidPackageScanResult(
        val totalPackageCount: Int = 0,
        val completed: Boolean = true
    )

    private suspend inline fun <T> fdroidStreamResult(
        crossinline block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

private class JsonScanner(
    private val reader: Reader
) {
    private var pushedChar: Int = NO_CHAR

    suspend inline fun forEachObjectEntry(
        block: suspend (String) -> JsonObjectScanAction
    ): Boolean {
        expect('{')
        var first = true
        while (true) {
            val delimiter = nextNonWhitespace()
            if (delimiter == '}'.code) return true
            if (!first) {
                check(delimiter == ','.code) { "Expected comma in JSON object" }
                val next = nextNonWhitespace()
                if (next == '}'.code) return true
                unread(next)
            } else {
                unread(delimiter)
            }
            first = false
            val key = readString()
            expect(':')
            when (block(key)) {
                JsonObjectScanAction.Continue -> Unit
                JsonObjectScanAction.Stop -> {
                    skipRemainingObjectEntriesAfterCurrentValue()
                    return false
                }
            }
        }
    }

    suspend fun readRawValue(): String {
        val first = nextNonWhitespace()
        val builder = StringBuilder()
        readValue(first, builder)
        return builder.toString()
    }

    suspend fun skipValue() {
        val first = nextNonWhitespace()
        readValue(first, builder = null)
    }

    private suspend fun skipRemainingObjectEntriesAfterCurrentValue() {
        while (true) {
            val delimiter = nextNonWhitespace()
            if (delimiter == '}'.code) return
            check(delimiter == ','.code) { "Expected comma while skipping JSON object" }
            val keyStart = nextNonWhitespace()
            if (keyStart == '}'.code) return
            unread(keyStart)
            readString()
            expect(':')
            skipValue()
        }
    }

    private suspend fun readValue(
        first: Int,
        builder: StringBuilder?
    ) {
        when (first.toChar()) {
            '{', '[' -> readBalanced(first, builder)
            '"' -> readStringBody(builder?.also { it.append('"') })
            else -> {
                builder?.append(first.toChar())
                while (true) {
                    val char = read()
                    if (char == -1 || char == ','.code || char == '}'.code || char == ']'.code) {
                        if (char != -1) unread(char)
                        return
                    }
                    builder?.append(char.toChar())
                }
            }
        }
    }

    private suspend fun readBalanced(
        first: Int,
        builder: StringBuilder?
    ) {
        var depth = 0
        var inString = false
        var escaped = false
        var char = first
        while (true) {
            check(char != -1) { "Unexpected end of JSON value" }
            builder?.append(char.toChar())
            when {
                escaped -> escaped = false
                inString && char == '\\'.code -> escaped = true
                char == '"'.code -> inString = !inString
                inString -> Unit
                char == '{'.code || char == '['.code -> depth += 1
                char == '}'.code || char == ']'.code -> {
                    depth -= 1
                    if (depth == 0) return
                }
            }
            char = read()
        }
    }

    private suspend fun readString(): String {
        expect('"')
        val builder = StringBuilder()
        readStringBody(builder, appendClosingQuote = false)
        return builder.toString()
    }

    private suspend fun readStringBody(
        builder: StringBuilder?,
        appendClosingQuote: Boolean = true
    ) {
        var escaped = false
        while (true) {
            val char = read()
            check(char != -1) { "Unexpected end of JSON string" }
            if (escaped) {
                when (char.toChar()) {
                    '"', '\\', '/' -> builder?.append(char.toChar())
                    'b' -> builder?.append('\b')
                    'f' -> builder?.append('\u000C')
                    'n' -> builder?.append('\n')
                    'r' -> builder?.append('\r')
                    't' -> builder?.append('\t')
                    'u' -> builder?.append(readUnicodeEscape())
                    else -> builder?.append(char.toChar())
                }
                escaped = false
            } else {
                when (char.toChar()) {
                    '\\' -> escaped = true
                    '"' -> {
                        if (appendClosingQuote) {
                            builder?.append('"')
                        }
                        return
                    }

                    else -> builder?.append(char.toChar())
                }
            }
        }
    }

    private suspend fun readUnicodeEscape(): Char {
        var value = 0
        repeat(4) {
            val char = read()
            check(char != -1) { "Unexpected end of unicode escape" }
            value = value * 16 + char.toChar().digitToInt(16)
        }
        return value.toChar()
    }

    private suspend fun expect(expected: Char) {
        val actual = nextNonWhitespace()
        check(actual == expected.code) { "Expected '$expected' in JSON stream" }
    }

    private suspend fun nextNonWhitespace(): Int {
        while (true) {
            val char = read()
            check(char != -1) { "Unexpected end of JSON stream" }
            if (!char.toChar().isWhitespace()) return char
        }
    }

    private var charsUntilCooperativeCheck = JSON_SCANNER_COOPERATIVE_CHECK_CHARS

    private suspend fun read(): Int {
        val value =
            if (pushedChar != NO_CHAR) {
                pushedChar.also { pushedChar = NO_CHAR }
            } else {
                reader.read()
            }
        if (value != -1) {
            charsUntilCooperativeCheck -= 1
            if (charsUntilCooperativeCheck <= 0) {
                charsUntilCooperativeCheck = JSON_SCANNER_COOPERATIVE_CHECK_CHARS
                coroutineContext.ensureActive()
                yield()
            }
        }
        return value
    }

    private fun unread(char: Int) {
        check(pushedChar == NO_CHAR) { "JSON scanner pushback already occupied" }
        pushedChar = char
    }

    private companion object {
        const val NO_CHAR = -2
    }
}

private enum class JsonObjectScanAction {
    Continue,
    Stop
}
