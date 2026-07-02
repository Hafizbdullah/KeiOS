package os.kei.ui.page.main.student

import android.app.PictureInPictureParams
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.Rational
import com.composables.icons.lucide.R as LucideR
import os.kei.R
import os.kei.ui.pip.AppPictureInPictureActionSet
import os.kei.ui.pip.AppPictureInPictureMediaControlActions
import os.kei.ui.pip.AppPictureInPictureParamsSpec
import os.kei.ui.pip.AppPictureInPictureRemoteActionSpec
import os.kei.ui.pip.buildAppPictureInPictureActionSet
import os.kei.ui.pip.buildAppPictureInPictureParams
import os.kei.ui.pip.resolveVisibleActions
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal const val GUIDE_VIDEO_ACTION_CLOSE_PIP = "os.kei.action.CLOSE_GUIDE_PIP"
internal const val GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK = "os.kei.action.TOGGLE_GUIDE_PIP_PLAYBACK"
internal const val GUIDE_VIDEO_ACTION_TOGGLE_PIP_LOOP = "os.kei.action.TOGGLE_GUIDE_PIP_LOOP"
internal const val GUIDE_VIDEO_ACTION_SEEK_BACK_10S = "os.kei.action.SEEK_BACK_GUIDE_PIP_10S"
internal const val GUIDE_VIDEO_ACTION_SEEK_FORWARD_10S = "os.kei.action.SEEK_FORWARD_GUIDE_PIP_10S"

internal val GuideVideoPictureInPictureActions =
    setOf(
        GUIDE_VIDEO_ACTION_CLOSE_PIP,
        GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK,
        GUIDE_VIDEO_ACTION_TOGGLE_PIP_LOOP,
        GUIDE_VIDEO_ACTION_SEEK_BACK_10S,
        GUIDE_VIDEO_ACTION_SEEK_FORWARD_10S,
    )

internal const val GUIDE_VIDEO_PIP_AUTHORITY = "guide-video-pip"
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_CLOSE = 3500
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_PLAYBACK = 3501
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_LOOP = 3502
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_SEEK_BACK_10S = 3503
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_SEEK_FORWARD_10S = 3504

private val GuidePictureInPictureAspectRatio = Rational(16, 9)
private val GuidePictureInPictureExpandedAspectRatio = Rational(12, 5)
private const val GUIDE_VIDEO_PIP_ASPECT_RATIO = 16f / 9f
private const val GUIDE_VIDEO_PIP_TARGET_WIDTH_FRACTION = 0.9f
private const val GUIDE_VIDEO_PIP_TARGET_HEIGHT_FRACTION = 0.72f
private const val GUIDE_VIDEO_PIP_EXISTING_AREA_TOLERANCE = 0.8f
private const val GUIDE_VIDEO_PIP_ASPECT_RATIO_TOLERANCE = 0.08f

internal fun buildGuidePictureInPictureParams(
    context: Context,
    actionSet: AppPictureInPictureActionSet = AppPictureInPictureActionSet(),
    sourceRectHint: Rect? = null,
    autoEnterEnabled: Boolean = false,
): PictureInPictureParams {
    return context.buildAppPictureInPictureParams(
        AppPictureInPictureParamsSpec(
            title = context.getString(R.string.guide_gallery_memorial_lobby_video),
            aspectRatio = GuidePictureInPictureAspectRatio,
            expandedAspectRatio = GuidePictureInPictureExpandedAspectRatio,
            actionSet = actionSet,
            sourceRectHint = sourceRectHint,
            autoEnterEnabled = autoEnterEnabled,
            seamlessResizeEnabled = true,
        )
    )
}

internal fun buildGuidePictureInPictureActionSet(
    context: Context,
    sessionId: Long,
    playWhenReady: Boolean,
    repeatEnabled: Boolean,
    maxActions: Int? = null,
): AppPictureInPictureActionSet {
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
    val repeatTitle =
        context.getString(
            if (repeatEnabled) {
                R.string.guide_gallery_memorial_lobby_pip_loop_disable
            } else {
                R.string.guide_gallery_memorial_lobby_pip_loop_enable
            }
        )
    val repeatIcon =
        if (repeatEnabled) {
            LucideR.drawable.lucide_ic_repeat_1
        } else {
            LucideR.drawable.lucide_ic_repeat
        }
    val closeAction =
        AppPictureInPictureRemoteActionSpec(
            action = GUIDE_VIDEO_ACTION_CLOSE_PIP,
            iconRes = LucideR.drawable.lucide_ic_x,
            title = context.getString(R.string.common_close),
            requestCode = GUIDE_VIDEO_REQUEST_CODE_PIP_CLOSE,
        )
    val seekBackAction =
        AppPictureInPictureRemoteActionSpec(
            action = GUIDE_VIDEO_ACTION_SEEK_BACK_10S,
            iconRes = LucideR.drawable.lucide_ic_rotate_ccw,
            title = context.getString(R.string.guide_gallery_memorial_lobby_pip_seek_back_10s),
            requestCode = GUIDE_VIDEO_REQUEST_CODE_PIP_SEEK_BACK_10S,
        )
    val playbackAction =
        AppPictureInPictureRemoteActionSpec(
            action = GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK,
            iconRes = playbackIcon,
            title = playbackTitle,
            requestCode = GUIDE_VIDEO_REQUEST_CODE_PIP_PLAYBACK,
        )
    val seekForwardAction =
        AppPictureInPictureRemoteActionSpec(
            action = GUIDE_VIDEO_ACTION_SEEK_FORWARD_10S,
            iconRes = LucideR.drawable.lucide_ic_rotate_cw,
            title = context.getString(R.string.guide_gallery_memorial_lobby_pip_seek_forward_10s),
            requestCode = GUIDE_VIDEO_REQUEST_CODE_PIP_SEEK_FORWARD_10S,
        )
    val repeatAction =
        AppPictureInPictureRemoteActionSpec(
            action = GUIDE_VIDEO_ACTION_TOGGLE_PIP_LOOP,
            iconRes = repeatIcon,
            title = repeatTitle,
            requestCode = GUIDE_VIDEO_REQUEST_CODE_PIP_LOOP,
        )
    return context.buildAppPictureInPictureActionSet(
        sessionId = sessionId,
        authority = GUIDE_VIDEO_PIP_AUTHORITY,
        actions = AppPictureInPictureMediaControlActions(
            playbackAction = playbackAction,
            seekForwardAction = seekForwardAction,
            seekBackAction = seekBackAction,
            secondaryAction = repeatAction,
        ).resolveVisibleActions(maxActions),
        closeAction = closeAction,
    )
}

internal fun Activity.resolveGuidePictureInPictureLaunchBounds(
    sourceRectHint: Rect?,
): Rect? {
    val windowBounds = windowManager.currentWindowMetrics.bounds.takeUnless { rect -> rect.isEmpty }
        ?: return sourceRectHint?.takeUnless { rect -> rect.isEmpty }
    return resolveGuidePictureInPictureLaunchBounds(
        windowBounds = windowBounds,
        sourceRectHint = sourceRectHint,
    )
}

internal fun resolveGuidePictureInPictureLaunchBounds(
    windowBounds: Rect,
    sourceRectHint: Rect?,
): Rect? {
    return resolveGuidePictureInPictureLaunchBounds(
        windowBounds = windowBounds.toGuidePictureInPictureLaunchBounds(),
        sourceRectHint = sourceRectHint
            ?.takeUnless { rect -> rect.isEmpty }
            ?.toGuidePictureInPictureLaunchBounds(),
    )?.toRect()
}

internal fun resolveGuidePictureInPictureLaunchBounds(
    windowBounds: GuidePictureInPictureLaunchBounds,
    sourceRectHint: GuidePictureInPictureLaunchBounds?,
): GuidePictureInPictureLaunchBounds? {
    if (windowBounds.isEmpty) return sourceRectHint?.takeUnless { rect -> rect.isEmpty }
    val source = sourceRectHint?.takeUnless { rect -> rect.isEmpty }
    val windowWidth = windowBounds.width().coerceAtLeast(1)
    val windowHeight = windowBounds.height().coerceAtLeast(1)
    val targetWidthByWindow = (windowWidth * GUIDE_VIDEO_PIP_TARGET_WIDTH_FRACTION).roundToInt()
    val targetHeightByWindow = (windowHeight * GUIDE_VIDEO_PIP_TARGET_HEIGHT_FRACTION).roundToInt()
    val targetWidthByHeight = (targetHeightByWindow * GUIDE_VIDEO_PIP_ASPECT_RATIO).roundToInt()
    val targetWidth = min(targetWidthByWindow, targetWidthByHeight).coerceAtLeast(1)
    val targetHeight = (targetWidth / GUIDE_VIDEO_PIP_ASPECT_RATIO).roundToInt()
        .coerceAtLeast(1)

    if (source != null) {
        val sourceArea = source.width().coerceAtLeast(0) * source.height().coerceAtLeast(0)
        val targetArea = targetWidth * targetHeight
        if (sourceArea >= targetArea * GUIDE_VIDEO_PIP_EXISTING_AREA_TOLERANCE &&
            source.matchesGuidePictureInPictureAspectRatio()
        ) {
            return source
        }
    }

    val centerX = source?.centerX() ?: windowBounds.centerX()
    val centerY = source?.centerY() ?: windowBounds.centerY()
    val left = clampInt(
        value = centerX - targetWidth / 2,
        minValue = windowBounds.left,
        maxValue = max(windowBounds.left, windowBounds.right - targetWidth),
    )
    val top = clampInt(
        value = centerY - targetHeight / 2,
        minValue = windowBounds.top,
        maxValue = max(windowBounds.top, windowBounds.bottom - targetHeight),
    )
    return GuidePictureInPictureLaunchBounds(left, top, left + targetWidth, top + targetHeight)
}

private fun clampInt(
    value: Int,
    minValue: Int,
    maxValue: Int,
): Int {
    return value.coerceIn(minValue, maxValue)
}

private fun GuidePictureInPictureLaunchBounds.matchesGuidePictureInPictureAspectRatio(): Boolean {
    if (isEmpty) return false
    val aspectRatio = width().toFloat() / height().coerceAtLeast(1)
    return kotlin.math.abs(aspectRatio - GUIDE_VIDEO_PIP_ASPECT_RATIO) <=
        GUIDE_VIDEO_PIP_ASPECT_RATIO_TOLERANCE
}

internal data class GuidePictureInPictureLaunchBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val isEmpty: Boolean
        get() = left >= right || top >= bottom

    fun width(): Int = right - left

    fun height(): Int = bottom - top

    fun centerX(): Int = (left + right) / 2

    fun centerY(): Int = (top + bottom) / 2

    fun contains(other: GuidePictureInPictureLaunchBounds): Boolean {
        return left <= other.left &&
            top <= other.top &&
            right >= other.right &&
            bottom >= other.bottom
    }
}

private fun Rect.toGuidePictureInPictureLaunchBounds(): GuidePictureInPictureLaunchBounds {
    return GuidePictureInPictureLaunchBounds(left, top, right, bottom)
}

private fun GuidePictureInPictureLaunchBounds.toRect(): Rect {
    return Rect(left, top, right, bottom)
}
