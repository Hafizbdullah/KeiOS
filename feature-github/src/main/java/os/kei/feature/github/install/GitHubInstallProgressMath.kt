package os.kei.feature.github.install

internal fun downloadProgressPercent(
    downloadedBytes: Long,
    totalBytes: Long,
): Int {
    if (totalBytes <= 0L || downloadedBytes <= 0L) return 0
    if (downloadedBytes >= totalBytes) return 100
    val fraction = downloadedBytes.toDouble() / totalBytes.toDouble()
    return (fraction * 100.0).toInt().coerceIn(1, 99)
}
