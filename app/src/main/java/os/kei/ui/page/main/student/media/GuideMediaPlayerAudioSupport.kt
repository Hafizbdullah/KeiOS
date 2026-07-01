package os.kei.ui.page.main.student

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

private val GuideMediaAudioAttributes =
    AudioAttributes
        .Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

internal fun ExoPlayer.configureGuideMediaAudioBehavior(
    handleAudioFocus: Boolean = true,
) {
    setAudioAttributes(GuideMediaAudioAttributes, handleAudioFocus)
    setHandleAudioBecomingNoisy(true)
}
