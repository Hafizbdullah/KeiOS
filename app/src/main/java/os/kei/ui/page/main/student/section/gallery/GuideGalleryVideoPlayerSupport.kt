@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package os.kei.ui.page.main.student.section.gallery

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import os.kei.ui.page.main.student.configureGuideMediaAudioBehavior
import os.kei.ui.page.main.student.createGameKeeMediaSourceFactory

internal fun buildGuideVideoPlayer(context: Context): ExoPlayer {
    val renderersFactory =
        DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .apply {
                if (isRunningOnAndroidEmulator()) {
                    setMediaCodecSelector(EmulatorGuideVideoMediaCodecSelector)
                    forceDisableMediaCodecAsynchronousQueueing()
                }
            }
    return ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .setMediaSourceFactory(createGameKeeMediaSourceFactory(context))
        .build()
        .apply {
            configureGuideMediaAudioBehavior()
        }
}

private val EmulatorGuideVideoMediaCodecSelector =
    MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
        val codecInfos =
            MediaCodecSelector.PREFER_SOFTWARE.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder,
            )
        if (mimeType != MimeTypes.VIDEO_H264) {
            codecInfos
        } else {
            codecInfos
                .filterNot { codecInfo ->
                    codecInfo.name.contains("goldfish", ignoreCase = true)
                }.ifEmpty { codecInfos }
        }
    }

private fun isRunningOnAndroidEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT.orEmpty().lowercase()
    val model = Build.MODEL.orEmpty().lowercase()
    val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
    val brand = Build.BRAND.orEmpty().lowercase()
    val device = Build.DEVICE.orEmpty().lowercase()
    val product = Build.PRODUCT.orEmpty().lowercase()
    val hardware = Build.HARDWARE.orEmpty().lowercase()
    return fingerprint.contains("generic") ||
        fingerprint.contains("emulator") ||
        model.contains("sdk") ||
        model.contains("emulator") ||
        model.contains("android sdk built for") ||
        manufacturer.contains("genymotion") ||
        hardware.contains("ranchu") ||
        hardware.contains("goldfish") ||
        (brand.startsWith("generic") && device.startsWith("generic")) ||
        product.contains("sdk_gphone")
}

@Composable
internal fun rememberGuidePreparedVideoPlayer(
    context: Context,
    mediaUrl: String,
    active: Boolean,
    restartToken: Int = 0,
): ExoPlayer? {
    return remember(context, mediaUrl, active, restartToken) {
        if (!active || mediaUrl.isBlank()) {
            null
        } else {
            buildGuideVideoPlayer(context).apply {
                setMediaItem(MediaItem.fromUri(mediaUrl))
                playWhenReady = true
                prepare()
            }
        }
    }
}

@Composable
internal fun BindGuideVideoForegroundPlaybackGuard(
    player: ExoPlayer?,
    shouldPauseOnStop: () -> Boolean = { true },
    onForegroundStopped: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestShouldPauseOnStop by rememberUpdatedState(shouldPauseOnStop)
    val latestOnForegroundStopped by rememberUpdatedState(onForegroundStopped)
    DisposableEffect(lifecycleOwner, player) {
        val boundPlayer = player ?: return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (latestShouldPauseOnStop()) {
                    runCatching { boundPlayer.pause() }
                    latestOnForegroundStopped()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
internal fun BindGuideVideoPlayerState(
    player: ExoPlayer?,
    onVideoRatioChanged: (Float) -> Unit = {},
    onBufferingChanged: (Boolean) -> Unit = {},
    onIsPlayingChanged: (Boolean) -> Unit = {},
    onPlayerErrorChanged: (String?) -> Unit = {},
    onDispose: () -> Unit = {}
) {
    DisposableEffect(player) {
        val boundPlayer = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    onVideoRatioChanged(videoSize.width.toFloat() / videoSize.height.toFloat())
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                onBufferingChanged(playbackState == Player.STATE_BUFFERING)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onIsPlayingChanged(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                onBufferingChanged(false)
                onIsPlayingChanged(false)
                onPlayerErrorChanged(error.errorCodeName)
            }
        }
        boundPlayer.addListener(listener)
        onDispose {
            boundPlayer.removeListener(listener)
            runCatching { boundPlayer.release() }
            onDispose()
        }
    }
}
