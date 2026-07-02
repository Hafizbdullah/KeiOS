package os.kei.ui.page.main.student

import android.app.PictureInPictureParams
import android.content.Context
import android.graphics.Rect
import android.util.Rational
import com.composables.icons.lucide.R as LucideR
import os.kei.R
import os.kei.ui.pip.AppPictureInPictureActionSet
import os.kei.ui.pip.AppPictureInPictureParamsSpec
import os.kei.ui.pip.AppPictureInPictureRemoteActionSpec
import os.kei.ui.pip.buildAppPictureInPictureActionSet
import os.kei.ui.pip.buildAppPictureInPictureParams
import os.kei.ui.pip.supportsAppPictureInPicture

internal const val GUIDE_VIDEO_ACTION_CLOSE_PIP = "os.kei.action.CLOSE_GUIDE_PIP"
internal const val GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK = "os.kei.action.TOGGLE_GUIDE_PIP_PLAYBACK"
internal const val GUIDE_VIDEO_ACTION_REQUEST_FULLSCREEN =
    "os.kei.action.REQUEST_GUIDE_PIP_FULLSCREEN"

internal val GuideVideoPictureInPictureActions =
    setOf(
        GUIDE_VIDEO_ACTION_CLOSE_PIP,
        GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK,
        GUIDE_VIDEO_ACTION_REQUEST_FULLSCREEN,
    )

internal const val GUIDE_VIDEO_PIP_AUTHORITY = "guide-video-pip"
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_CLOSE = 3500
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_PLAYBACK = 3501
private const val GUIDE_VIDEO_REQUEST_CODE_PIP_FULLSCREEN = 3502

private val GuidePictureInPictureAspectRatio = Rational(16, 9)

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
    return context.buildAppPictureInPictureActionSet(
        sessionId = sessionId,
        authority = GUIDE_VIDEO_PIP_AUTHORITY,
        actions =
            buildList {
                add(
                    AppPictureInPictureRemoteActionSpec(
                        action = GUIDE_VIDEO_ACTION_TOGGLE_PIP_PLAYBACK,
                        iconRes = playbackIcon,
                        title = playbackTitle,
                        requestCode = GUIDE_VIDEO_REQUEST_CODE_PIP_PLAYBACK,
                    )
                )
                add(
                    AppPictureInPictureRemoteActionSpec(
                        action = GUIDE_VIDEO_ACTION_REQUEST_FULLSCREEN,
                        iconRes = LucideR.drawable.lucide_ic_external_link,
                        title = context.getString(R.string.guide_gallery_memorial_lobby_pip_fullscreen),
                        requestCode = GUIDE_VIDEO_REQUEST_CODE_PIP_FULLSCREEN,
                    )
                )
            },
        closeAction =
            AppPictureInPictureRemoteActionSpec(
                action = GUIDE_VIDEO_ACTION_CLOSE_PIP,
                iconRes = LucideR.drawable.lucide_ic_x,
                title = context.getString(R.string.common_close),
                requestCode = GUIDE_VIDEO_REQUEST_CODE_PIP_CLOSE,
            ),
        maxActions = maxActions,
    )
}

internal fun Context.supportsGuidePictureInPicture(): Boolean {
    return supportsAppPictureInPicture()
}
