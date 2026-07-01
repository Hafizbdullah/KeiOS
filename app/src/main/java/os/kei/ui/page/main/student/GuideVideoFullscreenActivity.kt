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
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.pip.VideoPlaybackPictureInPicture
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.composables.icons.lucide.R as LucideR
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
import java.util.concurrent.Executor

private const val GUIDE_VIDEO_ACTION_CLOSE_PIP = "os.kei.action.CLOSE_GUIDE_PIP"
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_CLOSE = 3503

class GuideVideoFullscreenActivity : ComponentActivity() {
    private val mediaRepository = GuideFullscreenMediaRepository()
    private val pictureInPictureModeState = mutableStateOf(false)
    private val widePictureInPictureState = mutableStateOf(false)
    private var pictureInPictureRequestPending = false
    private var launchedIntoPictureInPicture = false
    private var pictureInPictureFullscreenRequestPending = false
    private var pictureInPictureSourceRectHint: Rect? = null
    private var boundVideoPlayer: Player? = null
    private val pictureInPictureExecutor = Executor { runnable ->
        if (isFinishing || isDestroyed) return@Executor
        runOnUiThread(runnable)
    }
    private val videoPictureInPicture by lazy(LazyThreadSafetyMode.NONE) {
        VideoPlaybackPictureInPicture(this, pictureInPictureExecutor)
    }
    private var boundPlayerView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startInPictureInPicture = intent?.getBooleanExtra(EXTRA_START_IN_PIP, false) == true
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
        pictureInPictureRequestPending = startInPictureInPicture
        pictureInPictureModeState.value = isInPictureInPictureMode
        commitGuidePictureInPictureParams()

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
                        mediaUrl = normalizedUrl,
                        previewImageUrl = previewImageUrl,
                        startPositionMs = startPositionMs,
                        startInPictureInPicture = startInPictureInPicture,
                        pictureInPictureMode = pictureInPictureMode,
                        shouldPauseOnStop = { !isInPictureInPictureMode },
                        onRequestEnterPictureInPicture = ::requestEnterGuidePictureInPicture,
                        onPlayerBound = ::onGuideVideoPlayerBound,
                        onPlayerReleased = ::onGuideVideoPlayerReleased,
                        onPlayerViewBound = ::onGuideVideoPlayerViewBound,
                        onPlayerViewReleased = ::onGuideVideoPlayerViewReleased,
                        onClose = { finishGuideVideoActivity(removeTask = launchedIntoPictureInPicture) }
                    )
                }
            }
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
        pictureInPictureFullscreenRequestPending = false
        tryEnterGuidePictureInPicture()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        when (intent.action) {
            ACTION_SET_PIP_SIZE -> {
                widePictureInPictureState.value =
                    intent.getBooleanExtra(EXTRA_PIP_WIDE_MODE, !widePictureInPictureState.value)
                commitGuidePictureInPictureParams()
            }

            ACTION_OPEN_FULLSCREEN -> {
                requestGuideFullscreenFromPictureInPicture()
            }

            GUIDE_VIDEO_ACTION_CLOSE_PIP -> {
                finishGuideVideoActivity(removeTask = true)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (
            launchedIntoPictureInPicture &&
            !isInPictureInPictureMode &&
            !pictureInPictureFullscreenRequestPending &&
            !isFinishing
        ) {
            finishGuideVideoActivity(removeTask = true)
        }
    }

    override fun onDestroy() {
        if (boundVideoPlayer != null) {
            stopGuideVideoPlayback()
            boundVideoPlayer = null
        }
        boundPlayerView = null
        runCatching { videoPictureInPicture.close() }
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
            commitGuidePictureInPictureParams()
        } else {
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
            wideMode = widePictureInPictureState.value,
            actions = buildGuidePictureInPictureActions(),
            sourceRectHint = pictureInPictureSourceRectHint,
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

    private fun commitGuidePictureInPictureParams() {
        val actions = buildGuidePictureInPictureActions()
        val aspectRatio = guidePictureInPictureAspectRatio(wideMode = widePictureInPictureState.value)
        val autoEnterEnabled = launchedIntoPictureInPicture || pictureInPictureRequestPending
        runCatching {
            val pip = videoPictureInPicture
            pip.setAspectRatio(aspectRatio)
            pip.setActions(actions)
            pip.setEnabled(autoEnterEnabled)
            if (shouldTrackGuidePictureInPicturePlayerView()) {
                val playerView = boundPlayerView
                if (playerView != null) {
                    pip.setPlayerView(playerView)
                } else {
                    pip.setPlayerView(null)
                }
            } else {
                pip.setPlayerView(null)
            }
            pip.commit()
        }
        // core-pip handles player bounds and compatibility bookkeeping. Platform-only
        // fields such as closeAction/title/expandedAspectRatio still need a final
        // framework params commit so PIP close reliably stops playback.
        val params = buildGuidePictureInPictureParams(
            context = this,
            wideMode = widePictureInPictureState.value,
            actions = actions,
            sourceRectHint = pictureInPictureSourceRectHint,
            autoEnterEnabled = autoEnterEnabled,
        )
        runCatching { setPictureInPictureParams(params) }
    }

    private fun shouldTrackGuidePictureInPicturePlayerView(): Boolean {
        return !launchedIntoPictureInPicture && !isInPictureInPictureMode
    }

    private fun buildGuidePictureInPictureActions(): List<RemoteAction> {
        val nextWideMode = !widePictureInPictureState.value
        val titleRes =
            if (nextWideMode) {
                R.string.guide_gallery_memorial_lobby_pip_size_wide
            } else {
                R.string.guide_gallery_memorial_lobby_pip_size_standard
            }
        val iconRes =
            if (nextWideMode) {
                LucideR.drawable.lucide_ic_maximize_2
            } else {
                LucideR.drawable.lucide_ic_minimize_2
            }
        val actionIntent =
            Intent(this, GuideVideoFullscreenActivity::class.java).apply {
                action = ACTION_SET_PIP_SIZE
                putExtra(EXTRA_PIP_WIDE_MODE, nextWideMode)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                if (nextWideMode) REQUEST_CODE_PIP_WIDE else REQUEST_CODE_PIP_STANDARD,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val title = getString(titleRes)
        return listOf(
            RemoteAction(
                AndroidIcon.createWithResource(this, LucideR.drawable.lucide_ic_fullscreen),
                getString(R.string.guide_gallery_memorial_lobby_pip_fullscreen),
                getString(R.string.guide_gallery_memorial_lobby_pip_fullscreen),
                PendingIntent.getActivity(
                    this,
                    REQUEST_CODE_PIP_FULLSCREEN,
                    Intent(this, GuideVideoFullscreenActivity::class.java).apply {
                        action = ACTION_OPEN_FULLSCREEN
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            ),
            RemoteAction(
                AndroidIcon.createWithResource(this, iconRes),
                title,
                title,
                pendingIntent,
            )
        )
    }

    private fun finishGuideVideoActivity(removeTask: Boolean) {
        pictureInPictureRequestPending = false
        pictureInPictureFullscreenRequestPending = false
        stopGuideVideoPlayback()
        if (removeTask) {
            runCatching { finishAndRemoveTask() }
                .onFailure { finish() }
        } else {
            finish()
        }
    }

    private fun stopGuideVideoPlayback() {
        boundVideoPlayer?.let { player ->
            runCatching { player.pause() }
            runCatching { player.stop() }
        }
    }

    private fun onGuideVideoPlayerBound(player: Player) {
        boundVideoPlayer = player
    }

    private fun onGuideVideoPlayerReleased(player: Player) {
        if (boundVideoPlayer === player) {
            boundVideoPlayer = null
        }
    }

    private fun onGuideVideoPlayerViewBound(view: View) {
        if (boundPlayerView === view) return
        boundPlayerView = view
        commitGuidePictureInPictureParams()
    }

    private fun onGuideVideoPlayerViewReleased(view: View) {
        if (boundPlayerView === view) {
            boundPlayerView = null
            if (!isFinishing && !isDestroyed) {
                commitGuidePictureInPictureParams()
            }
        }
    }

    private fun requestGuideFullscreenFromPictureInPicture() {
        pictureInPictureFullscreenRequestPending = true
        applyVideoFullscreenOrientation()
        runCatching {
            requestFullscreenMode(Activity.FULLSCREEN_MODE_REQUEST_ENTER, null)
        }.onFailure {
            pictureInPictureFullscreenRequestPending = false
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
        private const val EXTRA_START_POSITION_MS = "extra_start_position_ms"
        private const val EXTRA_SOURCE_RECT_HINT = "extra_source_rect_hint"
        private const val EXTRA_PIP_WIDE_MODE = "extra_pip_wide_mode"
        private const val ACTION_SET_PIP_SIZE = "os.kei.action.SET_GUIDE_PIP_SIZE"
        private const val ACTION_OPEN_FULLSCREEN = "os.kei.action.OPEN_GUIDE_FULLSCREEN"
        private const val REQUEST_CODE_PIP_FULLSCREEN = 3500
        private const val REQUEST_CODE_PIP_STANDARD = 3501
        private const val REQUEST_CODE_PIP_WIDE = 3502
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
            val intent = Intent(context, GuideVideoFullscreenActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_URL, mediaUrl)
                putExtra(EXTRA_PREVIEW_IMAGE_URL, previewImageUrl)
                putExtra(EXTRA_START_IN_PIP, startInPictureInPicture)
                putExtra(EXTRA_START_POSITION_MS, startPositionMs.coerceAtLeast(0L))
                sourceRectHint?.takeUnless { rect -> rect.isEmpty }?.let { rect ->
                    putExtra(EXTRA_SOURCE_RECT_HINT, rect)
                }
                if (startInPictureInPicture) addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                if (hostActivity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val launchOptions =
                if (startInPictureInPicture && hostActivity != null && context.supportsGuidePictureInPicture()) {
                    ActivityOptions
                        .makeLaunchIntoPip(
                            buildGuidePictureInPictureParams(
                                context = context,
                                wideMode = false,
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
    wideMode: Boolean,
    actions: List<RemoteAction> = emptyList(),
    sourceRectHint: Rect? = null,
    autoEnterEnabled: Boolean = false,
): PictureInPictureParams {
    val aspectRatio = guidePictureInPictureAspectRatio(wideMode = wideMode)
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
    if (wideMode && context.supportsGuideExpandedPictureInPicture()) {
        builder.setExpandedAspectRatio(aspectRatio)
    }
    if (actions.isNotEmpty()) {
        builder.setActions(actions)
    }
    return builder.build()
}

private fun guidePictureInPictureAspectRatio(wideMode: Boolean): Rational {
    return if (wideMode) Rational(21, 9) else Rational(16, 9)
}

private fun buildGuidePictureInPictureCloseAction(context: Context): RemoteAction {
    val title = context.getString(R.string.common_close)
    return RemoteAction(
        AndroidIcon.createWithResource(context, LucideR.drawable.lucide_ic_x),
        title,
        title,
        PendingIntent.getActivity(
            context,
            GUIDE_VIDEO_REQUEST_CODE_PIP_CLOSE,
            Intent(context, GuideVideoFullscreenActivity::class.java).apply {
                action = GUIDE_VIDEO_ACTION_CLOSE_PIP
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
    )
}

private fun Context.supportsGuidePictureInPicture(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}

private fun Context.supportsGuideExpandedPictureInPicture(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_EXPANDED_PICTURE_IN_PICTURE)
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
    mediaUrl: String,
    previewImageUrl: String,
    startPositionMs: Long,
    startInPictureInPicture: Boolean,
    pictureInPictureMode: Boolean,
    shouldPauseOnStop: () -> Boolean,
    onRequestEnterPictureInPicture: () -> Unit,
    onPlayerBound: (Player) -> Unit,
    onPlayerReleased: (Player) -> Unit,
    onPlayerViewBound: (View) -> Unit,
    onPlayerViewReleased: (View) -> Unit,
    onClose: () -> Unit
) {
    val backGestureState = rememberFullscreenBackNavigationGestureState(
        allowActivityFrameworkFinish = true,
        onBack = onClose
    )

    val context = LocalContext.current
    var loadError by remember(mediaUrl) { mutableStateOf<String?>(null) }
    var firstFrameRendered by remember(mediaUrl, startPositionMs) { mutableStateOf(false) }

    val player = remember(context, mediaUrl, startPositionMs) {
        if (mediaUrl.isBlank()) {
            null
        } else {
            buildGuideVideoPlayer(context).apply {
                setMediaItem(MediaItem.fromUri(mediaUrl))
                if (startPositionMs > 0L) {
                    seekTo(startPositionMs)
                }
                playWhenReady = true
                prepare()
            }
        }
    }

    LaunchedEffect(startInPictureInPicture, player) {
        if (startInPictureInPicture && player != null) {
            onRequestEnterPictureInPicture()
        }
    }

    DisposableEffect(player) {
        val boundPlayer = player ?: return@DisposableEffect onDispose { }
        onPlayerBound(boundPlayer)
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (startInPictureInPicture && playbackState == Player.STATE_READY) {
                    onRequestEnterPictureInPicture()
                }
            }

            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
            }

            override fun onPlayerError(error: PlaybackException) {
                loadError = error.errorCodeName
            }
        }
        boundPlayer.addListener(listener)
        onDispose {
            boundPlayer.removeListener(listener)
            onPlayerReleased(boundPlayer)
            runCatching { boundPlayer.release() }
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
                    mediaUrl = mediaUrl,
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
