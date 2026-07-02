package os.kei.ui.pip

import android.app.PictureInPictureParams
import android.content.Context
import android.graphics.Rect
import android.util.Rational

data class AppPictureInPictureParamsSpec(
    val title: CharSequence? = null,
    val subtitle: CharSequence? = null,
    val aspectRatio: Rational,
    val expandedAspectRatio: Rational? = null,
    val actionSet: AppPictureInPictureActionSet = AppPictureInPictureActionSet(),
    val sourceRectHint: Rect? = null,
    val autoEnterEnabled: Boolean = false,
    val seamlessResizeEnabled: Boolean = true,
)

fun Context.buildAppPictureInPictureParams(
    spec: AppPictureInPictureParamsSpec,
): PictureInPictureParams {
    val builder =
        PictureInPictureParams.Builder()
            .setAspectRatio(spec.aspectRatio)
            .setSeamlessResizeEnabled(spec.seamlessResizeEnabled)
            .setAutoEnterEnabled(spec.autoEnterEnabled)

    spec.title?.let(builder::setTitle)
    spec.subtitle?.let(builder::setSubtitle)
    spec.actionSet.closeAction?.let(builder::setCloseAction)
    spec.sourceRectHint?.takeUnless { rect -> rect.isEmpty }?.let(builder::setSourceRectHint)
    spec.expandedAspectRatio
        ?.takeIf { supportsAppExpandedPictureInPicture(it) }
        ?.let(builder::setExpandedAspectRatio)
    if (spec.actionSet.actions.isNotEmpty()) {
        builder.setActions(spec.actionSet.actions)
    }
    return builder.build()
}
