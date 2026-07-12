package os.kei.ui.pip

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Rational
import android.view.View

private const val APP_EXPANDED_PIP_MIN_RATIO = 2.39f

fun Context.supportsAppPictureInPicture(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}

fun Context.supportsAppExpandedPictureInPicture(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_EXPANDED_PICTURE_IN_PICTURE)
}

fun Context.supportsAppExpandedPictureInPicture(
    expandedAspectRatio: Rational?,
): Boolean {
    return expandedAspectRatio != null &&
        supportsAppExpandedPictureInPicture() &&
        expandedAspectRatio.isValidAppExpandedPictureInPictureAspectRatio()
}

fun Rational.isValidAppExpandedPictureInPictureAspectRatio(): Boolean {
    val value = toFloat()
    return value > APP_EXPANDED_PIP_MIN_RATIO ||
        value < 1f / APP_EXPANDED_PIP_MIN_RATIO
}

tailrec fun Context.findHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.findHostActivity()
        else -> null
    }
}

fun View.appPictureInPictureSourceRect(): Rect? {
    if (width <= 0 || height <= 0) return null
    return Rect()
        .takeIf(::getGlobalVisibleRect)
        ?.takeUnless { rect -> rect.isEmpty }
}
