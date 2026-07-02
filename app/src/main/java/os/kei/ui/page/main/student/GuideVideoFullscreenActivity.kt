@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package os.kei.ui.page.main.student

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.provider.Settings
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
import os.kei.ui.pip.APP_PIP_EXTRA_SESSION_ID
import os.kei.ui.pip.APP_PIP_NO_SESSION_ID
import os.kei.ui.pip.AppPictureInPictureActionReceiver
import os.kei.ui.pip.AppPictureInPictureActivityRegistry
import os.kei.ui.pip.AppPictureInPictureSessionIds
import os.kei.ui.pip.appPictureInPictureSessionId
import os.kei.ui.pip.appPictureInPictureSourceRect
import os.kei.ui.pip.findHostActivity
import os.kei.ui.pip.requestAppPictureInPictureFullscreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text

class GuideVideoFullscreenActivity : ComponentActivity() {
    private val mediaRepository = GuideFullscreenMediaRepository()
    private val pictureInPictureModeState = mutableStateOf(false)
    private val pictureInPicturePlayWhenReadyState = mutableStateOf(false)
    private var pictureInPictureRequestPending = false
    private var pictureInPictureRuntimeParamsReady = false
    private var launchedIntoPictureInPicture = false
    private var launchedWithPictureInPictureOptions = false
    private var finishRequested = false
    private var guideVideoMediaUrl: String = ""
    private var guideVideoPreviewImageUrl: String = ""
    private var guideVideoPictureInPictureSessionId = APP_PIP_NO_SESSION_ID
    private var pictureInPictureSourceRectHint: Rect? = null
    private var guideVideoPlayer: ExoPlayer? = null
    private var boundVideoPlayer: ExoPlayer? = null
    private var boundPlayerView: PlayerView? = null
    private val pictureInPictureSourceRectLocation = IntArray(2)
    private lateinit var pictureInPictureActionReceiver: AppPictureInPictureActionReceiver
    private val enablePictureInPictureRuntimeParamsRunnable =
        Runnable {
            if (finishRequested || isFinishing || isDestroyed || !isInPictureInPictureMode) {
                return@Runnable
            }
            pictureInPictureRuntimeParamsReady = true
            commitGuidePictureInPictureParams(forceRuntime = true)
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pictureInPictureActionReceiver =
            AppPictureInPictureActionReceiver(
                context = this,
                authority = GUIDE_VIDEO_PIP_AUTHORITY,
                actions = GuideVideoPictureInPictureActions,
                currentSessionId = { guideVideoPictureInPictureSessionId },
                onAction = { event -> handleGuidePictureInPictureAction(event.action) },
            ).also { receiver ->
                receiver.register()
            }
        val startInPictureInPicture = intent?.getBooleanExtra(EXTRA_START_IN_PIP, false) == true
        launchedWithPictureInPictureOptions =
            intent?.getBooleanExtra(EXTRA_STARTED_WITH_PIP_OPTIONS, false) == true
        val startPositionMs = intent?.getLongExtra(EXTRA_START_POSITION_MS, 0L)?.coerceAtLeast(0L) ?: 0L
        val previewImageUrl = normalizeGuideMediaSource(intent?.getStringExtra(EXTRA_PREVIEW_IMAGE_URL).orEmpty())
        guideVideoPreviewImageUrl = previewImageUrl
        guideVideoPictureInPictureSessionId =
            intent?.appPictureInPictureSessionId()
                ?.takeIf { value -> value != APP_PIP_NO_SESSION_ID }
                ?: nextGuideVideoPictureInPictureSessionId()
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
        guideVideoMediaUrl = normalizedUrl
        val startPlayWhenReady = intent?.getBooleanExtra(EXTRA_PLAY_WHEN_READY, true) != false
        guideVideoPlayer = createGuideVideoPlayer(
            mediaUrl = guideVideoMediaUrl,
            startPositionMs = startPositionMs,
            playWhenReady = startPlayWhenReady,
        )
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
                        previewImageUrl = guideVideoPreviewImageUrl,
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

    private fun createGuideVideoPlayer(
        mediaUrl: String,
        startPositionMs: Long,
        playWhenReady: Boolean,
    ): ExoPlayer? {
        if (mediaUrl.isBlank()) return null
        return buildGuideVideoPlayer(this).apply {
            setMediaItem(MediaItem.fromUri(mediaUrl))
            if (startPositionMs > 0L) {
                seekTo(startPositionMs)
            }
            this.playWhenReady = playWhenReady
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
        if (intent.belongsToCurrentGuidePictureInPictureSession()) {
            handleGuidePictureInPictureAction(intent.action)
        }
    }

    private fun handleGuidePictureInPictureAction(action: String?) {
        when (action) {
            GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK -> {
                toggleGuideVideoPlayback()
            }

            GUIDE_VIDEO_ACTION_REQUEST_FULLSCREEN -> {
                requestGuideVideoFullscreenFromPictureInPictureAction()
            }

            GUIDE_VIDEO_ACTION_CLOSE_PIP -> {
                finishGuideVideoActivity(removeTask = true)
            }
        }
    }

    private fun Intent?.belongsToCurrentGuidePictureInPictureSession(): Boolean {
        val sessionId = appPictureInPictureSessionId()
        return sessionId != APP_PIP_NO_SESSION_ID &&
            sessionId == guideVideoPictureInPictureSessionId
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        window.decorView.removeCallbacks(enablePictureInPictureRuntimeParamsRunnable)
        stopGuideVideoPlayback(release = true)
        boundPlayerView = null
        if (::pictureInPictureActionReceiver.isInitialized) {
            pictureInPictureActionReceiver.unregister()
        }
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
            scheduleGuidePictureInPictureRuntimeParamsCommit()
        } else if (!finishRequested) {
            pictureInPictureRuntimeParamsReady = false
            window.decorView.removeCallbacks(enablePictureInPictureRuntimeParamsRunnable)
            applyVideoFullscreenOrientation()
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
            actionSet = buildGuidePictureInPictureActionSet(),
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

    private fun commitGuidePictureInPictureParams(forceRuntime: Boolean = false) {
        if (finishRequested || isFinishing || isDestroyed) return
        if (launchedWithPictureInPictureOptions && !isInPictureInPictureMode) return
        if (isInPictureInPictureMode && !forceRuntime && !pictureInPictureRuntimeParamsReady) return
        val actionSet = buildGuidePictureInPictureActionSet()
        val autoEnterEnabled = launchedIntoPictureInPicture || pictureInPictureRequestPending
        val params = buildGuidePictureInPictureParams(
            context = this,
            actionSet = actionSet,
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
        return view.appPictureInPictureSourceRect(pictureInPictureSourceRectLocation)
    }

    private fun buildGuidePictureInPictureActionSet() =
        buildGuidePictureInPictureActionSet(
            context = this,
            sessionId = guideVideoPictureInPictureSessionId,
            playWhenReady = pictureInPicturePlayWhenReadyState.value,
            maxActions = getMaxNumPictureInPictureActions(),
        )

    private fun finishGuideVideoActivity(removeTask: Boolean) {
        if (finishRequested && isFinishing) return
        finishRequested = true
        pictureInPictureRequestPending = false
        pictureInPictureRuntimeParamsReady = false
        window.decorView.removeCallbacks(enablePictureInPictureRuntimeParamsRunnable)
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

    private fun requestGuideVideoFullscreenFromPictureInPictureAction() {
        if (finishRequested || isFinishing || isDestroyed) return
        if (!isInPictureInPictureMode) {
            applyVideoFullscreenOrientation()
            return
        }
        requestAppPictureInPictureFullscreen(
            onResult = {
                pictureInPictureRequestPending = false
                pictureInPictureRuntimeParamsReady = false
                window.decorView.removeCallbacks(enablePictureInPictureRuntimeParamsRunnable)
                applyVideoFullscreenOrientation()
            },
            onError = {
                pictureInPictureRequestPending = false
            },
        )
    }

    private fun onGuideVideoPlayerPlayWhenReadyChanged(playWhenReady: Boolean) {
        if (pictureInPicturePlayWhenReadyState.value == playWhenReady) return
        pictureInPicturePlayWhenReadyState.value = playWhenReady
        if (isInPictureInPictureMode || pictureInPictureRequestPending || launchedIntoPictureInPicture) {
            commitGuidePictureInPictureParams(forceRuntime = isInPictureInPictureMode)
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
        private const val EXTRA_PLAY_WHEN_READY = "extra_play_when_ready"
        private const val EXTRA_SOURCE_RECT_HINT = "extra_source_rect_hint"
        private val pictureInPictureActivityRegistry =
            AppPictureInPictureActivityRegistry<GuideVideoFullscreenActivity>()

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
            val sessionId = nextGuideVideoPictureInPictureSessionId()
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
                putExtra(EXTRA_PLAY_WHEN_READY, true)
                putExtra(APP_PIP_EXTRA_SESSION_ID, sessionId)
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
                                actionSet = buildGuidePictureInPictureActionSet(
                                    context = context,
                                    sessionId = sessionId,
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

        private fun nextGuideVideoPictureInPictureSessionId(): Long {
            return AppPictureInPictureSessionIds.next()
        }

        private fun registerLaunchedPictureInPictureActivity(activity: GuideVideoFullscreenActivity) {
            pictureInPictureActivityRegistry.replaceActive(
                activity = activity,
                shouldClose = { current ->
                    current.launchedIntoPictureInPicture &&
                        !current.isFinishing &&
                        !current.isDestroyed
                },
                close = { current ->
                    current.finishGuideVideoActivity(removeTask = true)
                },
            )
        }

        private fun unregisterLaunchedPictureInPictureActivity(activity: GuideVideoFullscreenActivity) {
            pictureInPictureActivityRegistry.clear(activity)
        }

        private fun finishActiveLaunchedPictureInPictureActivity() {
            pictureInPictureActivityRegistry.closeActive(
                shouldClose = { activity ->
                    activity.launchedIntoPictureInPicture &&
                        !activity.isFinishing &&
                        !activity.isDestroyed
                },
                close = { activity ->
                    activity.runOnUiThread {
                        activity.finishGuideVideoActivity(removeTask = true)
                    }
                },
            )
        }
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
