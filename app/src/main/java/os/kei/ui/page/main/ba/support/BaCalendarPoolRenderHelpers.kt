package os.kei.ui.page.main.ba.support

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.ba.data.remote.GameKeeNetworkClient
import os.kei.feature.ba.data.remote.GameKeeNetworkResult
import os.kei.ui.page.main.widget.shape.appSquircleClip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val GAMEKEE_COVER_MEMORY_CACHE_KIB = 64 * 1024
private const val GAMEKEE_COVER_DEFAULT_DECODE_DIMENSION = 960
private const val GAMEKEE_COVER_MIN_DECODE_DIMENSION = 128
private const val GAMEKEE_COVER_MAX_DECODE_DIMENSION = 2048

private val gameKeeCoverBitmapCache =
    object : LruCache<String, Bitmap>(GAMEKEE_COVER_MEMORY_CACHE_KIB) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

internal fun normalizeGameKeeCoverDecodeDimension(requested: Int): Int =
    requested.coerceIn(
        GAMEKEE_COVER_MIN_DECODE_DIMENSION,
        GAMEKEE_COVER_MAX_DECODE_DIMENSION,
    )

internal fun gameKeeCoverBitmapCacheKey(
    url: String,
    maxDecodeDimension: Int,
): String {
    val normalizedUrl = url.trim()
    if (normalizedUrl.isBlank()) return ""
    return "${normalizeGameKeeCoverDecodeDimension(maxDecodeDimension)}:$normalizedUrl"
}

private fun cachedGameKeeCoverBitmap(
    url: String,
    maxDecodeDimension: Int,
): Bitmap? {
    val key = gameKeeCoverBitmapCacheKey(url, maxDecodeDimension)
    if (key.isBlank()) return null
    return synchronized(gameKeeCoverBitmapCache) { gameKeeCoverBitmapCache.get(key) }
}

private fun cacheGameKeeCoverBitmap(
    url: String,
    maxDecodeDimension: Int,
    bitmap: Bitmap,
) {
    val key = gameKeeCoverBitmapCacheKey(url, maxDecodeDimension)
    if (key.isBlank()) return
    synchronized(gameKeeCoverBitmapCache) { gameKeeCoverBitmapCache.put(key, bitmap) }
}

private fun decodeSampledLocalBitmap(
    localPath: String,
    maxDecodeDimension: Int = 1280
): Bitmap? {
    val safeMax = maxDecodeDimension.coerceAtLeast(512)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(localPath, bounds)
    val srcWidth = bounds.outWidth
    val srcHeight = bounds.outHeight
    if (srcWidth <= 0 || srcHeight <= 0) {
        return BitmapFactory.decodeFile(localPath)
    }
    var sample = 1
    while ((srcWidth / sample) > safeMax || (srcHeight / sample) > safeMax) {
        sample *= 2
    }
    return BitmapFactory.decodeFile(
        localPath,
        BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    )
}

@Composable
internal fun GameKeeCoverImage(
    imageUrl: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    aspectRatioRange: ClosedFloatingPointRange<Float> = 1.0f..2.4f,
    loadEnabled: Boolean = true,
    maxDecodeDimension: Int = GAMEKEE_COVER_DEFAULT_DECODE_DIMENSION,
) {
    if (!enabled) return
    val normalizedUrl = remember(imageUrl) { normalizeGameKeeImageLink(imageUrl) }
    if (normalizedUrl.isBlank()) return
    val resolvedDecodeDimension =
        remember(maxDecodeDimension) {
            normalizeGameKeeCoverDecodeDimension(maxDecodeDimension)
        }

    var bitmap by remember(normalizedUrl, resolvedDecodeDimension) {
        mutableStateOf(
            cachedGameKeeCoverBitmap(
                url = normalizedUrl,
                maxDecodeDimension = resolvedDecodeDimension,
            ),
        )
    }
    LaunchedEffect(normalizedUrl, loadEnabled, resolvedDecodeDimension) {
        cachedGameKeeCoverBitmap(normalizedUrl, resolvedDecodeDimension)?.let { cached ->
            bitmap = cached
            return@LaunchedEffect
        }
        if (!loadEnabled) return@LaunchedEffect
        if (normalizedUrl.startsWith("file://")) {
            val localPath = normalizedUrl.toUri().path.orEmpty()
            if (localPath.isBlank()) {
                return@LaunchedEffect
            }
            val decoded =
                withContext(AppDispatchers.media) {
                    decodeSampledLocalBitmap(localPath, resolvedDecodeDimension)
                }
            if (decoded != null) {
                bitmap = decoded
                cacheGameKeeCoverBitmap(
                    url = normalizedUrl,
                    maxDecodeDimension = resolvedDecodeDimension,
                    bitmap = decoded,
                )
            }
            return@LaunchedEffect
        }
        val loaded = withContext(AppDispatchers.media) {
            when (val result = GameKeeNetworkClient.fetchImage(
                imageUrl = normalizedUrl,
                maxDecodeDimension = resolvedDecodeDimension,
            )) {
                is GameKeeNetworkResult.Success -> result.value
                is GameKeeNetworkResult.Failure -> null
            }
        }
        if (loaded != null) {
            bitmap = loaded
            cacheGameKeeCoverBitmap(
                url = normalizedUrl,
                maxDecodeDimension = resolvedDecodeDimension,
                bitmap = loaded,
            )
        }
    }

    val rendered = bitmap ?: return
    val imageBitmap = remember(rendered) { rendered.asImageBitmap() }
    val minRatio = aspectRatioRange.start.coerceAtLeast(0.2f)
    val maxRatio = aspectRatioRange.endInclusive.coerceAtLeast(minRatio + 0.01f)
    val aspectRatioValue = remember(rendered.width, rendered.height, minRatio, maxRatio) {
        val w = rendered.width.coerceAtLeast(1)
        val h = rendered.height.coerceAtLeast(1)
        (w.toFloat() / h.toFloat()).coerceIn(minRatio, maxRatio)
    }
    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatioValue)
            .appSquircleClip(12.dp)
    )
}

internal fun formatBaDateTimeNoYearInTimeZone(epochMillis: Long, timeZone: TimeZone): String {
    if (epochMillis <= 0L) return "-"
    return runCatching {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }.format(Date(epochMillis))
    }.getOrDefault("-")
}
