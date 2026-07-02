package os.kei.ui.pip

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri

data class AppPictureInPictureRemoteActionSpec(
    val action: String,
    val iconRes: Int,
    val title: CharSequence,
    val requestCode: Int,
)

data class AppPictureInPictureActionSet(
    val actions: List<RemoteAction> = emptyList(),
    val closeAction: RemoteAction? = null,
)

fun Context.buildAppPictureInPictureActionSet(
    sessionId: Long,
    authority: String,
    actions: List<AppPictureInPictureRemoteActionSpec>,
    closeAction: AppPictureInPictureRemoteActionSpec? = null,
    maxActions: Int? = null,
): AppPictureInPictureActionSet {
    val visibleActions =
        maxActions
            ?.takeIf { value -> value >= 0 }
            ?.let { value -> actions.take(value) }
            ?: actions
    val actionRemoteActions =
        visibleActions.associateWith { spec ->
            buildAppPictureInPictureRemoteAction(
                sessionId = sessionId,
                authority = authority,
                spec = spec,
            )
        }
    return AppPictureInPictureActionSet(
        actions = visibleActions.mapNotNull(actionRemoteActions::get),
        closeAction = closeAction?.let { spec ->
            actionRemoteActions[spec]
                ?: buildAppPictureInPictureRemoteAction(
                    sessionId = sessionId,
                    authority = authority,
                    spec = spec,
                )
        },
    )
}

fun Context.buildAppPictureInPictureRemoteAction(
    sessionId: Long,
    authority: String,
    spec: AppPictureInPictureRemoteActionSpec,
): RemoteAction {
    return RemoteAction(
        Icon.createWithResource(this, spec.iconRes),
        spec.title,
        spec.title,
        PendingIntent.getBroadcast(
            this,
            spec.pendingIntentRequestCode(sessionId),
            Intent(spec.action)
                .setPackage(packageName)
                .setData(buildAppPictureInPictureActionUri(authority, spec.action, sessionId))
                .putExtra(APP_PIP_EXTRA_SESSION_ID, sessionId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
    )
}

internal fun buildAppPictureInPictureActionUri(
    authority: String,
    action: String,
    sessionId: Long,
): Uri {
    return Uri.Builder()
        .scheme(APP_PIP_ACTION_URI_SCHEME)
        .authority(authority)
        .appendPath(sessionId.toString())
        .appendPath(action)
        .build()
}

private fun AppPictureInPictureRemoteActionSpec.pendingIntentRequestCode(sessionId: Long): Int {
    val sessionHash = (sessionId xor (sessionId ushr 32)).toInt()
    return requestCode * 31 + sessionHash
}
