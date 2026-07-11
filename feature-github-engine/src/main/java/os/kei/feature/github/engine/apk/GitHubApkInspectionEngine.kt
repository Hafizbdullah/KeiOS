package os.kei.feature.github.engine.apk

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import os.kei.core.io.cancellableResult
import os.kei.core.io.resultPreservingCancellation
import os.kei.feature.github.data.apk.AndroidBinaryXmlPackageNameParser
import os.kei.feature.github.data.apk.RemoteZipEntryReader
import os.kei.feature.github.data.apk.RemoteZipSelectedEntries
import os.kei.feature.github.model.GitHubApkManifestInfo
import os.kei.feature.github.model.GitHubApkSignatureInfo

data class GitHubApkReadTarget(
    val url: String,
    val token: String = "",
    val source: String = "",
)

class GitHubApkInspectionEngine(
    private val zipEntryReader: RemoteZipEntryReader = RemoteZipEntryReader(),
) {
    suspend fun inspect(
        assetName: String,
        targets: List<GitHubApkReadTarget>,
        nestedArtifactArchive: Boolean = false,
    ): Result<GitHubApkManifestInfo> = cancellableResult {
        val inspectPayload = readInspectPayload(
            targets = targets,
            nestedArtifactArchive = nestedArtifactArchive,
        ).getOrThrow()
        inspectPayload.toManifestInfo(assetName).getOrThrow()
    }

    suspend fun readPackageName(
        targets: List<GitHubApkReadTarget>,
    ): Result<String> {
        return readAndroidManifestBytes(targets).mapCatching { manifestBytes ->
            parsePackageName(manifestBytes).getOrThrow()
        }
    }

    suspend fun readNestedApkPackageName(
        targets: List<GitHubApkReadTarget>,
        nestedApkEntryName: String,
    ): Result<String> {
        return readWithFallback(targets) { target ->
            zipEntryReader.readNestedStoredZipEntry(
                url = target.url,
                outerEntryName = nestedApkEntryName,
                innerEntryName = ANDROID_MANIFEST_ENTRY,
                apiToken = target.token,
            )
        }.mapCatching { payload ->
            parsePackageName(payload.value).getOrThrow()
        }
    }

    suspend fun readSelectedNestedApkPackageName(
        targets: List<GitHubApkReadTarget>,
        selectNestedApkEntryNames: (List<String>) -> List<String>,
    ): Result<String> {
        return readWithFallback(targets) { target ->
            zipEntryReader.readSelectedNestedStoredZipEntry(
                url = target.url,
                innerEntryName = ANDROID_MANIFEST_ENTRY,
                apiToken = target.token,
                selectOuterEntryNames = selectNestedApkEntryNames,
            )
        }.mapCatching { payload ->
            parsePackageName(payload.value).getOrThrow()
        }
    }

    fun parsePackageName(manifestBytes: ByteArray): Result<String> {
        return AndroidBinaryXmlPackageNameParser.parsePackageName(manifestBytes)
    }

    suspend fun readAndroidManifestBytes(
        targets: List<GitHubApkReadTarget>,
    ): Result<ByteArray> {
        return readWithFallback(targets) { target ->
            zipEntryReader.readEntry(
                url = target.url,
                entryName = ANDROID_MANIFEST_ENTRY,
                apiToken = target.token,
            )
        }.map { payload -> payload.value }
    }

    suspend fun listEntryNames(
        targets: List<GitHubApkReadTarget>,
    ): Result<List<String>> {
        return readWithFallback(targets) { target ->
            zipEntryReader.listEntryNames(
                url = target.url,
                apiToken = target.token,
            )
        }.map { payload -> payload.value }
    }

    private suspend fun readInspectPayload(
        targets: List<GitHubApkReadTarget>,
        nestedArtifactArchive: Boolean,
    ): Result<ManifestReadPayload<RemoteZipSelectedEntries>> {
        if (nestedArtifactArchive) {
            readNestedInspectPayload(targets).getOrNull()?.let { nestedPayload ->
                return Result.success(nestedPayload)
            }
        }
        return readWithFallback(targets) { target ->
            zipEntryReader.readSelectedEntries(
                url = target.url,
                apiToken = target.token,
                selectEntryNames = ::selectInspectEntryNames,
            )
        }
    }

    private suspend fun readNestedInspectPayload(
        targets: List<GitHubApkReadTarget>,
    ): Result<ManifestReadPayload<RemoteZipSelectedEntries>> {
        return readWithFallback(targets) { target ->
            zipEntryReader.readSelectedNestedStoredZipEntries(
                url = target.url,
                apiToken = target.token,
                selectOuterEntryNames = ::selectNestedApkEntryNames,
                selectInnerEntryNames = ::selectInspectEntryNames,
            )
        }
    }

    private fun selectInspectEntryNames(entryNames: List<String>): List<String> {
        return buildList {
            add(ANDROID_MANIFEST_ENTRY)
            entryNames.firstSignatureEntry()?.let(::add)
        }
    }

    private fun parseSignatureInfo(
        signatureEntry: String,
        certBytes: ByteArray,
    ): Result<GitHubApkSignatureInfo?> = resultPreservingCancellation {
        val certificates = CertificateFactory.getInstance("X.509")
            .generateCertificates(ByteArrayInputStream(certBytes))
        val certificate = certificates.firstOrNull() as? X509Certificate
            ?: return@resultPreservingCancellation null
        GitHubApkSignatureInfo(
            entryName = signatureEntry,
            subject = certificate.subjectX500Principal.name,
            issuer = certificate.issuerX500Principal.name,
            serialNumber = certificate.serialNumber.toString(16),
            algorithm = certificate.sigAlgName,
            notBeforeMillis = certificate.notBefore.time,
            notAfterMillis = certificate.notAfter.time,
            sha256 = certBytes.sha256Hex(),
        )
    }

    private fun ManifestReadPayload<RemoteZipSelectedEntries>.toManifestInfo(
        assetName: String,
    ): Result<GitHubApkManifestInfo> = resultPreservingCancellation {
        val entries = value
        val manifest = entries.entries[ANDROID_MANIFEST_ENTRY]
            ?: error("$ANDROID_MANIFEST_ENTRY was not found in APK")
        val signatureEntry = entries.entryNames.firstSignatureEntry()
        val signatureInfo = signatureEntry?.let { entryName ->
            entries.entries[entryName]?.let { certBytes ->
                parseSignatureInfo(entryName, certBytes).getOrNull()
            }
        }
        AndroidBinaryXmlPackageNameParser.parseManifestInfo(manifest).getOrThrow()
            .copy(
                assetName = assetName,
                fetchSource = source,
                nativeAbis = entries.entryNames.extractNativeAbis(),
                signatureInfo = signatureInfo,
            )
    }

    private suspend fun <T> readWithFallback(
        targets: List<GitHubApkReadTarget>,
        read: suspend (GitHubApkReadTarget) -> Result<T>,
    ): Result<ManifestReadPayload<T>> {
        var firstFailure: Result<T>? = null
        targets.forEach { target ->
            val result = read(target)
            if (result.isSuccess) {
                return result.map { value ->
                    ManifestReadPayload(value = value, source = target.source)
                }
            }
            if (firstFailure == null) firstFailure = result
        }
        return Result.failure(
            firstFailure?.exceptionOrNull()
                ?: IllegalStateException("APK manifest read target missing"),
        )
    }

    private fun List<String>.extractNativeAbis(): List<String> {
        return asSequence()
            .mapNotNull { entry ->
                val parts = entry.split('/')
                if (parts.size >= 3 && parts[0] == "lib" && parts.last().endsWith(".so")) {
                    parts[1]
                } else {
                    null
                }
            }
            .distinct()
            .sorted()
            .toList()
    }

    private fun List<String>.firstSignatureEntry(): String? {
        return firstOrNull { entry ->
            entry.startsWith("META-INF/", ignoreCase = true) &&
                (entry.endsWith(".RSA", ignoreCase = true) ||
                    entry.endsWith(".DSA", ignoreCase = true) ||
                    entry.endsWith(".EC", ignoreCase = true))
        }
    }

    private fun nestedApkEntryScore(entryName: String): Int {
        val name = entryName.substringAfterLast('/').lowercase()
        return when {
            "universal" in name && "release" in name -> 0
            "universal" in name -> 1
            "release" in name -> 2
            "debug" in name -> 4
            else -> 3
        }
    }

    private fun selectNestedApkEntryNames(entryNames: List<String>): List<String> {
        return entryNames
            .asSequence()
            .filter { it.endsWith(".apk", ignoreCase = true) }
            .sortedWith(
                compareBy<String> { nestedApkEntryScore(it) }
                    .thenBy { it.length }
                    .thenBy { it.lowercase() },
            )
            .take(MAX_NESTED_APK_SCAN_CANDIDATES)
            .toList()
    }

    private data class ManifestReadPayload<T>(
        val value: T,
        val source: String,
    )

    companion object {
        private const val ANDROID_MANIFEST_ENTRY = "AndroidManifest.xml"
        private const val MAX_NESTED_APK_SCAN_CANDIDATES = 4
    }
}

private fun ByteArray.sha256Hex(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { byte -> "%02x".format(byte) }
}
