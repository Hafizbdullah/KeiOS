@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package os.kei.ui.page.main.student

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon as AndroidIcon
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.composables.icons.lucide.R as LucideR
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import os.kei.R
import os.kei.core.platform.PredictiveBackOemCompat
import os.kei.core.prefs.UiPrefs
import os.kei.ui.page.main.back.ProvideBackNavigationRuntime
import os.kei.ui.page.main.back.rememberFullscreenBackNavigationGestureState
import os.kei.ui.page.main.os.appLucideBackIcon
import os.kei.ui.page.main.student.section.gallery.BindGuideVideoForegroundPlaybackGuard
import os.kei.ui.page.main.student.section.gallery.GuideFullscreenMediaRepository
import os.kei.ui.page.main.student.section.gallery.GuideVideoFailureMessage
import os.kei.ui.page.main.student.section.gallery.buildGuideVideoPlayer
import os.kei.ui.page.main.widget.glass.AppInteractiveTokens
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import os.kei.ui.page.main.widget.motion.LocalPredictiveBackAnimationsEnabled
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import java.lang.ref.WeakReference

internal const val GUIDE_VIDEO_ACTION_CLOSE_PIP = "os.kei.action.CLOSE_GUIDE_PIP"
internal const val GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK = "os.kei.action.TOGGLE_GUIDE_PIP_PLAYBACK"
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_CLOSE = 3500
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_PLAYBACK = 3501

class GuideVideoFullscreenActivity : ComponentActivity() {
    private val mediaRepository = GuideFullscreenMediaRepository()
    private val pictureInPictureModeState = mutableStateOf(false)
    private val pictureInPicturePlayWhenReadyState = mutableStateOf(false)
    private var pictureInPictureRequestPending = false
    private var pictureInPictureRuntimeParamsReady = false
    private var launchedIntoPictureInPicture = false
    private var launchedWithPictureInPictureOptions = false
    private var finishRequested = false
    private var pictureInPictureSourceRectHint: Rect? = null
    private var guideVideoPlayer: ExoPlayer? = null
    private var boundVideoPlayer: ExoPlayer? = null
    private var boundPlayerView: PlayerView? = null
    private val pictureInPictureSourceRectLocation = IntArray(2)
    private val enablePictureInPictureRuntimeParamsRunnable =
        Runnable {
            if (finishRequested || isFinishing || isDestroyed || !isInPictureInPictureMode) {
                return@Runnable
            }
            pictureInPictureRuntimeParamsReady = true
            commitGuidePictureInPictureParams(forceRuntime = true)
        }
    private val finishBackgroundedPictureInPictureRunnable =
        Runnable {
            if (finishRequested || isFinishing || isDestroyed) return@Runnable
            if (!launchedIntoPictureInPicture || isInPictureInPictureMode) return@Runnable
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && window.decorView.isShown) {
                return@Runnable
            }
            finishGuideVideoActivity(removeTask = true)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startInPictureInPicture = intent?.getBooleanExtra(EXTRA_START_IN_PIP, false) == true
        launchedWithPictureInPictureOptions =
            intent?.getBooleanExtra(EXTRA_STARTED_WITH_PIP_OPTIONS, false) == true
        val startPositionMs = intent?.getLongExtra(EXTRA_START_POSITION_MS, 0L)?.coerceAtLeast(0L) ?: 0L
        val previewImageUrl = normalizeGuideMediaSource(intent?.getStringExtra(EXTRA_PREVIEW_IMAGE_URL).orEmpty())
        launchedIntoPictureInPicture = startInPictureInPicture
        pictureInPictureSourceRectHint = intent?.getParcelableExtra(EXTRA_SOURCE_RECT_HINT, Rect::class.java)
            ?.takeUnless { rect -> rect.isEmpty }
        if (startInPictureInPicture) {
            registerLaunchedPictureInPictureActivity(this)
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        if (!startInPictureInPicture) {
            lifecycleScope.launch {
                val mediaAdaptiveRotationEnabled = mediaRepository.loadMediaAdaptiveRotationEnabled()
                requestedOrientation = resolveVideoFullscreenOrientation(mediaAdaptiveRotationEnabled)
            }
        }
        enableEdgeToEdge()

        val normalizedUrl = normalizeGuideMediaSource(
            intent?.getStringExtra(EXTRA_MEDIA_URL).orEmpty()
        )
        guideVideoPlayer = createGuideVideoPlayer(normalizedUrl, startPositionMs)
        boundVideoPlayer = guideVideoPlayer
        onGuideVideoPlayerPlayWhenReadyChanged(
            guideVideoPlayer?.let { player ->
                player.playWhenReady && player.playbackState != Player.STATE_ENDED
            } == true
        )
        pictureInPictureRequestPending = startInPictureInPicture && !launchedWithPictureInPictureOptions
        pictureInPictureModeState.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            scheduleGuidePictureInPictureRuntimeParamsCommit()
        } else {
            commitGuidePictureInPictureParams()
        }

        setContent {
            val transitionAnimationsEnabled = UiPrefs.isTransitionAnimationsEnabled()
            val predictiveBackPolicy = PredictiveBackOemCompat.currentPolicy(
                transitionAnimationsEnabled = transitionAnimationsEnabled,
                predictiveBackAnimationsEnabled = UiPrefs.isPredictiveBackAnimationsEnabled()
            )
            ProvideBackNavigationRuntime(policy = predictiveBackPolicy) {
                CompositionLocalProvider(
                    LocalTransitionAnimationsEnabled provides transitionAnimationsEnabled,
                    LocalPredictiveBackAnimationsEnabled provides predictiveBackPolicy.localPredictiveBackEnabled
                ) {
                    val pictureInPictureMode = pictureInPictureModeState.value
                    GuideVideoFullscreenScreen(
                        player = guideVideoPlayer,
                        previewImageUrl = previewImageUrl,
                        requestEnterPictureInPictureOnReady =
                            startInPictureInPicture && !launchedWithPictureInPictureOptions,
                        pictureInPictureMode = pictureInPictureMode,
                        shouldPauseOnStop = { !isInPictureInPictureMode },
                        onRequestEnterPictureInPicture = ::requestEnterGuidePictureInPicture,
                        onPlayerPlayWhenReadyChanged = ::onGuideVideoPlayerPlayWhenReadyChanged,
                        onPlayerViewBound = ::onGuideVideoPlayerViewBound,
                        onPlayerViewReleased = ::onGuideVideoPlayerViewReleased,
                        onClose = { finishGuideVideoActivity(removeTask = launchedIntoPictureInPicture) }
                    )
                }
            }
        }
    }

    private fun createGuideVideoPlayer(mediaUrl: String, startPositionMs: Long): ExoPlayer? {
        if (mediaUrl.isBlank()) return null
        return buildGuideVideoPlayer(this).apply {
            setMediaItem(MediaItem.fromUri(mediaUrl))
            if (startPositionMs > 0L) {
                seekTo(startPositionMs)
            }
            playWhenReady = true
            prepare()
        }
    }

    private fun resolveVideoFullscreenOrientation(mediaAdaptiveRotationEnabled: Boolean): Int {
        if (mediaAdaptiveRotationEnabled) {
            return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        val systemAutoRotateEnabled = runCatching {
            Settings.System.getInt(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        }.getOrDefault(false)
        return if (systemAutoRotateEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
    }

    override fun onResume() {
        super.onResume()
        tryEnterGuidePictureInPicture()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleGuidePictureInPictureAction(intent)
    }

    private fun handleGuidePictureInPictureAction(intent: Intent?) {
        when (intent?.action) {
            GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK -> {
                toggleGuideVideoPlayback()
            }

            GUIDE_VIDEO_ACTION_CLOSE_PIP -> {
                finishGuideVideoActivity(removeTask = true)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        scheduleFinishBackgroundedPictureInPicture()
    }

    override fun onDestroy() {
        window.decorView.removeCallbacks(enablePictureInPictureRuntimeParamsRunnable)
        window.decorView.removeCallbacks(finishBackgroundedPictureInPictureRunnable)
        stopGuideVideoPlayback(release = true)
        boundPlayerView = null
        unregisterLaunchedPictureInPictureActivity(this)
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pictureInPictureModeState.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            pictureInPictureRequestPending = false
            pictureInPictureSourceRectHint = null
            window.decorView.removeCallbacks(finishBackgroundedPictureInPictureRunnable)
            scheduleGuidePictureInPictureRuntimeParamsCommit()
        } else if (!finishRequested) {
            pictureInPictureRuntimeParamsReady = false
            window.decorView.removeCallbacks(enablePictureInPictureRuntimeParamsRunnable)
            applyVideoFullscreenOrientation()
            scheduleFinishBackgroundedPictureInPicture()
        }
    }

    private fun requestEnterGuidePictureInPicture() {
        pictureInPictureRequestPending = true
        tryEnterGuidePictureInPicture()
    }

    private fun tryEnterGuidePictureInPicture() {
        if (!pictureInPictureRequestPending) return
        if (isInPictureInPictureMode || isFinishing || isDestroyed) return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val params = buildGuidePictureInPictureParams(
            context = this,
            actions = buildGuidePictureInPictureActions(),
            sourceRectHint = runtimeGuidePictureInPictureSourceRectHint(),
            autoEnterEnabled = launchedIntoPictureInPicture,
        )
        runCatching {
            setPictureInPictureParams(params)
            enterPictureInPictureMode(params)
        }.onSuccess { entered ->
            if (entered) {
                pictureInPictureRequestPending = false
            }
        }
    }

    private fun scheduleGuidePictureInPictureRuntimeParamsCommit() {
        pictureInPictureRuntimeParamsReady = false
        window.decorView.removeCallbacks(enablePictureInPictureRuntimeParamsRunnable)
        window.decorView.postDelayed(enablePictureInPictureRuntimeParamsRunnable, 180L)
    }

    private fun scheduleFinishBackgroundedPictureInPicture() {
        if (finishRequested || isFinishing || isDestroyed) return
        if (!launchedIntoPictureInPicture) return
        window.decorView.removeCallbacks(finishBackgroundedPictureInPictureRunnable)
        window.decorView.postDelayed(finishBackgroundedPictureInPictureRunnable, 360L)
    }

    private fun commitGuidePictureInPictureParams(forceRuntime: Boolean = false) {
        if (finishRequested || isFinishing || isDestroyed) return
        if (launchedWithPictureInPictureOptions && !isInPictureInPictureMode) return
        if (isInPictureInPictureMode && !forceRuntime && !pictureInPictureRuntimeParamsReady) return
        val actions = buildGuidePictureInPictureActions()
        val autoEnterEnabled = launchedIntoPictureInPicture || pictureInPictureRequestPending
        val params = buildGuidePictureInPictureParams(
            context = this,
            actions = actions,
            sourceRectHint = runtimeGuidePictureInPictureSourceRectHint(),
            autoEnterEnabled = autoEnterEnabled,
        )
        runCatching { setPictureInPictureParams(params) }
    }

    private fun runtimeGuidePictureInPictureSourceRectHint(): Rect? {
        if (isInPictureInPictureMode) {
            return currentGuidePictureInPictureContentRectHint()
        }
        if (pictureInPictureRequestPending) {
            return pictureInPictureSourceRectHint
        }
        if (launchedIntoPictureInPicture) return null
        return pictureInPictureSourceRectHint
    }

    private fun currentGuidePictureInPictureContentRectHint(): Rect? {
        val view =
            boundPlayerView
                ?.takeIf { playerView -> playerView.width > 0 && playerView.height > 0 }
                ?: window.decorView.takeIf { decorView ->
                    decorView.width > 0 && decorView.height > 0
                }
                ?: return null
        view.getLocationInWindow(pictureInPictureSourceRectLocation)
        val left = pictureInPictureSourceRectLocation[0]
        val top = pictureInPictureSourceRectLocation[1]
        return Rect(left, top, left + view.width, top + view.height)
            .takeUnless { rect -> rect.isEmpty }
    }

    private fun buildGuidePictureInPictureActions(): List<RemoteAction> {
        return buildGuidePictureInPictureActions(
            context = this,
            playWhenReady = pictureInPicturePlayWhenReadyState.value,
            maxActions = getMaxNumPictureInPictureActions(),
        )
    }

    private fun finishGuideVideoActivity(removeTask: Boolean) {
        if (finishRequested && isFinishing) return
        finishRequested = true
        pictureInPictureRequestPending = false
        pictureInPictureRuntimeParamsReady = false
        window.decorView.removeCallbacks(enablePictureInPictureRuntimeParamsRunnable)
        window.decorView.removeCallbacks(finishBackgroundedPictureInPictureRunnable)
        stopGuideVideoPlayback(release = true)
        if (isInPictureInPictureMode) {
            finish()
        } else if (removeTask) {
            runCatching { finishAndRemoveTask() }
                .onFailure { finish() }
        } else {
            finish()
        }
    }

    private fun stopGuideVideoPlayback(release: Boolean = false) {
        val player = boundVideoPlayer ?: guideVideoPlayer
        player?.let {
            runCatching { player.playWhenReady = false }
            runCatching { player.pause() }
            runCatching { player.stop() }
            if (release) {
                runCatching { boundPlayerView?.player = null }
                runCatching { player.clearVideoSurface() }
                runCatching { player.clearMediaItems() }
                runCatching { player.release() }
                boundVideoPlayer = null
                if (guideVideoPlayer === player) {
                    guideVideoPlayer = null
                }
            }
        }
        if (pictureInPicturePlayWhenReadyState.value) {
            pictureInPicturePlayWhenReadyState.value = false
        }
    }

    private fun toggleGuideVideoPlayback() {
        val player = boundVideoPlayer ?: guideVideoPlayer ?: return
        val playbackEnded = player.playbackState == Player.STATE_ENDED
        if (!playbackEnded && (player.isPlaying || player.playWhenReady)) {
            runCatching { player.pause() }
            onGuideVideoPlayerPlayWhenReadyChanged(false)
        } else {
            runCatching {
                if (playbackEnded) {
                    player.seekTo(0L)
                }
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.play()
            }
            onGuideVideoPlayerPlayWhenReadyChanged(true)
        }
    }

    private fun onGuideVideoPlayerPlayWhenReadyChanged(playWhenReady: Boolean) {
        if (pictureInPicturePlayWhenReadyState.value == playWhenReady) return
        pictureInPicturePlayWhenReadyState.value = playWhenReady
        if (isInPictureInPictureMode || pictureInPictureRequestPending || launchedIntoPictureInPicture) {
            commitGuidePictureInPictureParams()
        }
    }

    private fun onGuideVideoPlayerViewBound(view: PlayerView) {
        if (boundPlayerView === view) return
        boundPlayerView = view
    }

    private fun onGuideVideoPlayerViewReleased(view: PlayerView) {
        if (boundPlayerView === view) {
            boundPlayerView = null
        }
    }

    private fun applyVideoFullscreenOrientation() {
        lifecycleScope.launch {
            val mediaAdaptiveRotationEnabled = mediaRepository.loadMediaAdaptiveRotationEnabled()
            requestedOrientation = resolveVideoFullscreenOrientation(mediaAdaptiveRotationEnabled)
        }
    }

    companion object {
        private const val EXTRA_MEDIA_URL = "extra_media_url"
        private const val EXTRA_PREVIEW_IMAGE_URL = "extra_preview_image_url"
        private const val EXTRA_START_IN_PIP = "extra_start_in_pip"
        private const val EXTRA_STARTED_WITH_PIP_OPTIONS = "extra_started_with_pip_options"
        private const val EXTRA_START_POSITION_MS = "extra_start_position_ms"
        private const val EXTRA_SOURCE_RECT_HINT = "extra_source_rect_hint"
        private var activeLaunchedPictureInPictureActivity: WeakReference<GuideVideoFullscreenActivity>? = null

        fun launch(
            context: Context,
            mediaUrl: String,
            previewImageUrl: String = "",
        ) {
            launchInternal(
                context = context,
                mediaUrl = mediaUrl,
                previewImageUrl = previewImageUrl,
                startInPictureInPicture = false,
                startPositionMs = 0L,
                sourceRectHint = null,
            )
        }

        fun launchInPictureInPicture(
            context: Context,
            mediaUrl: String,
            previewImageUrl: String = "",
            startPositionMs: Long = 0L,
            sourceRectHint: Rect? = null,
        ) {
            launchInternal(
                context = context,
                mediaUrl = mediaUrl,
                previewImageUrl = previewImageUrl,
                startInPictureInPicture = true,
                startPositionMs = startPositionMs,
                sourceRectHint = sourceRectHint,
            )
        }

        private fun launchInternal(
            context: Context,
            mediaUrl: String,
            previewImageUrl: String,
            startInPictureInPicture: Boolean,
            startPositionMs: Long,
            sourceRectHint: Rect?,
        ) {
            val hostActivity = context.findHostActivity()
            if (startInPictureInPicture) {
                finishActiveLaunchedPictureInPictureActivity()
            }
            val canUseLaunchIntoPictureInPicture =
                startInPictureInPicture && hostActivity != null && context.supportsGuidePictureInPicture()
            val intent = Intent(context, GuideVideoFullscreenActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_URL, mediaUrl)
                putExtra(EXTRA_PREVIEW_IMAGE_URL, previewImageUrl)
                putExtra(EXTRA_START_IN_PIP, startInPictureInPicture)
                putExtra(EXTRA_STARTED_WITH_PIP_OPTIONS, canUseLaunchIntoPictureInPicture)
                putExtra(EXTRA_START_POSITION_MS, startPositionMs.coerceAtLeast(0L))
                sourceRectHint?.takeUnless { rect -> rect.isEmpty }?.let { rect ->
                    putExtra(EXTRA_SOURCE_RECT_HINT, rect)
                }
                if (startInPictureInPicture) addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                if (hostActivity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val launchOptions =
                if (canUseLaunchIntoPictureInPicture) {
                    ActivityOptions
                        .makeLaunchIntoPip(
                            buildGuidePictureInPictureParams(
                                context = context,
                                actions = buildGuidePictureInPictureActions(
                                    context = context,
                                    playWhenReady = true,
                                ),
                                sourceRectHint = sourceRectHint,
                                autoEnterEnabled = true,
                            )
                        )
                        .toBundle()
                } else {
                    null
                }
            if (hostActivity != null && launchOptions != null) {
                hostActivity.startActivity(intent, launchOptions)
            } else if (hostActivity != null) {
                hostActivity.startActivity(intent)
            } else {
                context.startActivity(intent)
            }
        }

        private fun registerLaunchedPictureInPictureActivity(activity: GuideVideoFullscreenActivity) {
            activeLaunchedPictureInPictureActivity
                ?.get()
                ?.takeIf { current ->
                    current !== activity &&
                        current.launchedIntoPictureInPicture &&
                        !current.isFinishing &&
                        !current.isDestroyed
                }
                ?.finishGuideVideoActivity(removeTask = true)
            activeLaunchedPictureInPictureActivity = WeakReference(activity)
        }

        private fun unregisterLaunchedPictureInPictureActivity(activity: GuideVideoFullscreenActivity) {
            if (activeLaunchedPictureInPictureActivity?.get() === activity) {
                activeLaunchedPictureInPictureActivity = null
            }
        }

        internal fun dispatchPictureInPictureAction(action: String) {
            val activity =
                activeLaunchedPictureInPictureActivity
                    ?.get()
                    ?.takeIf { current -> !current.isFinishing && !current.isDestroyed }
                    ?: return
            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.handleGuidePictureInPictureAction(Intent(action))
                }
            }
        }

        private fun finishActiveLaunchedPictureInPictureActivity() {
            activeLaunchedPictureInPictureActivity
                ?.get()
                ?.takeIf { activity ->
                    activity.launchedIntoPictureInPicture &&
                        !activity.isFinishing &&
                        !activity.isDestroyed
                }
                ?.let { activity ->
                    activity.runOnUiThread {
                        activity.finishGuideVideoActivity(removeTask = true)
                    }
                }
        }
    }
}

private fun buildGuidePictureInPictureParams(
    context: Context,
    actions: List<RemoteAction> = emptyList(),
    sourceRectHint: Rect? = null,
    autoEnterEnabled: Boolean = false,
): PictureInPictureParams {
    val aspectRatio = guidePictureInPictureAspectRatio()
    val builder =
        PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setSeamlessResizeEnabled(true)
            .setAutoEnterEnabled(autoEnterEnabled)
            .setTitle(context.getString(R.string.guide_gallery_memorial_lobby_video))
            .setCloseAction(buildGuidePictureInPictureCloseAction(context))
    sourceRectHint?.takeUnless { rect -> rect.isEmpty }?.let { rect ->
        builder.setSourceRectHint(rect)
    }
    if (actions.isNotEmpty()) {
        builder.setActions(actions)
    }
    return builder.build()
}

private fun guidePictureInPictureAspectRatio(): Rational {
    return Rational(16, 9)
}

private fun buildGuidePictureInPictureActions(
    context: Context,
    playWhenReady: Boolean,
    maxActions: Int? = null,
): List<RemoteAction> {
    val playbackTitle =
        context.getString(
            if (playWhenReady) {
                R.string.guide_gallery_memorial_lobby_pip_pause
            } else {
                R.string.guide_gallery_memorial_lobby_pip_resume
            }
        )
    val playbackIcon =
        if (playWhenReady) {
            LucideR.drawable.lucide_ic_pause
        } else {
            LucideR.drawable.lucide_ic_play
        }
    val actions =
        listOf(
            buildGuidePictureInPictureRemoteAction(
                context = context,
                iconRes = playbackIcon,
                title = playbackTitle,
                requestCode = GUIDE_VIDEO_REQUEST_CODE_PIP_PLAYBACK,
                action = GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK,
            ),
        )
    return maxActions
        ?.takeIf { value -> value >= 0 }
        ?.let { value -> actions.take(value) }
        ?: actions
}

private fun buildGuidePictureInPictureCloseAction(context: Context): RemoteAction {
    return buildGuidePictureInPictureRemoteAction(
        context = context,
        iconRes = LucideR.drawable.lucide_ic_x,
        title = context.getString(R.string.common_close),
        requestCode = GUIDE_VIDEO_REQUEST_CODE_PIP_CLOSE,
        action = GUIDE_VIDEO_ACTION_CLOSE_PIP,
    )
}

private fun buildGuidePictureInPictureRemoteAction(
    context: Context,
    iconRes: Int,
    title: String,
    requestCode: Int,
    action: String,
    configureIntent: Intent.() -> Unit = {},
): RemoteAction {
    return RemoteAction(
        AndroidIcon.createWithResource(context, iconRes),
        title,
        title,
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, GuideVideoPictureInPictureActionReceiver::class.java).apply {
                this.action = action
                configureIntent()
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
    )
}

private fun Context.supportsGuidePictureInPicture(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}

private tailrec fun Context.findHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.findHostActivity()
        else -> null
    }
}
@Composable
private fun GuideVideoFullscreenScreen(
    player: ExoPlayer?,
    previewImageUrl: String,
    requestEnterPictureInPictureOnReady: Boolean,
    pictureInPictureMode: Boolean,
    shouldPauseOnStop: () -> Boolean,
    onRequestEnterPictureInPicture: () -> Unit,
    onPlayerPlayWhenReadyChanged: (Boolean) -> Unit,
    onPlayerViewBound: (PlayerView) -> Unit,
    onPlayerViewReleased: (PlayerView) -> Unit,
    onClose: () -> Unit
) {
    val backGestureState = rememberFullscreenBackNavigationGestureState(
        allowActivityFrameworkFinish = true,
        onBack = onClose
    )

    var loadError by remember(player) { mutableStateOf<String?>(null) }
    var firstFrameRendered by remember(player) { mutableStateOf(false) }

    LaunchedEffect(requestEnterPictureInPictureOnReady, player) {
        if (requestEnterPictureInPictureOnReady && player != null) {
            onRequestEnterPictureInPicture()
        }
    }

    DisposableEffect(player) {
        val boundPlayer = player ?: return@DisposableEffect onDispose { }
        fun notifyPlayWhenReady() {
            onPlayerPlayWhenReadyChanged(
                boundPlayer.playWhenReady && boundPlayer.playbackState != Player.STATE_ENDED
            )
        }
        notifyPlayWhenReady()
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (requestEnterPictureInPictureOnReady && playbackState == Player.STATE_READY) {
                    onRequestEnterPictureInPicture()
                }
                notifyPlayWhenReady()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                notifyPlayWhenReady()
            }

            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
            }

            override fun onPlayerError(error: PlaybackException) {
                loadError = error.errorCodeName
                onPlayerPlayWhenReadyChanged(false)
            }
        }
        boundPlayer.addListener(listener)
        onDispose {
            boundPlayer.removeListener(listener)
            onPlayerPlayWhenReadyChanged(false)
        }
    }
    BindGuideVideoForegroundPlaybackGuard(
        player = player,
        shouldPauseOnStop = shouldPauseOnStop,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                backGestureState.onContainerSizeChanged(size.width, size.height)
            }
            .graphicsLayer {
                val backMotion = backGestureState.motionValues()
                transformOrigin = TransformOrigin(backMotion.pivotX, backMotion.pivotY)
                translationX = backMotion.translationX
                scaleX = backMotion.scale
                scaleY = backMotion.scale
                alpha = backMotion.contentAlpha
            }
            .background(Color.Black)
    ) {
        val activePlayer = player
        if (activePlayer != null) {
            var currentPlayerView by remember(activePlayer) { mutableStateOf<PlayerView?>(null) }
            DisposableEffect(currentPlayerView) {
                val boundView = currentPlayerView ?: return@DisposableEffect onDispose { }
                onPlayerViewBound(boundView)
                onDispose {
                    onPlayerViewReleased(boundView)
                }
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    (LayoutInflater.from(ctx)
                        .inflate(R.layout.guide_video_fullscreen_player_view, null) as PlayerView).apply {
                        useController = !pictureInPictureMode
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setKeepContentOnPlayerReset(true)
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        this.player = activePlayer
                        currentPlayerView = this
                    }
                },
                update = { view ->
                    view.player = activePlayer
                    view.useController = !pictureInPictureMode
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    currentPlayerView = view
                }
            )
        } else {
            Text(
                text = stringResource(R.string.guide_media_video_url_invalid),
                color = Color(0xFFBFDBFE),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (!firstFrameRendered && previewImageUrl.isNotBlank()) {
            AsyncImage(
                model = previewImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        loadError?.takeIf { it.isNotBlank() }?.let { err ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth()
                    .background(Color(0xD9000000), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                GuideVideoFailureMessage(
                    loadError = err,
                    mediaUrl = player?.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty(),
                    errorColor = Color(0xFFFCA5A5),
                    supportingColor = Color(0xFFBFDBFE),
                    buttonTextColor = Color(0xFFBFDBFE),
                    horizontalAlignment = Alignment.CenterHorizontally,
                )
            }
        }

        if (!pictureInPictureMode) {
            GuideVideoFullscreenCloseButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun GuideVideoFullscreenCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topPadding = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState =
        appMotionFloatState(
            targetValue = if (isPressed) AppInteractiveTokens.pressedScale else 1f,
            durationMillis = 110,
            label = "guide_video_fullscreen_close_scale",
        )
    val scaleProvider = remember(scaleState) { { scaleState.value } }
    Box(
        modifier =
            modifier
                .padding(start = 14.dp, top = topPadding + 12.dp)
                .size(48.dp)
                .graphicsLayer {
                    val scale = scaleProvider()
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x99000000))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = appLucideBackIcon(),
            contentDescription = stringResource(R.string.common_close),
            tint = Color.White,
        )
    }
}
