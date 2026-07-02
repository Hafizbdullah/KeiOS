package os.kei.ui.pip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

data class AppPictureInPictureActionEvent(
    val action: String,
    val sessionId: Long,
    val intent: Intent,
)

data class AppPictureInPictureActionIdentity(
    val authority: String,
    val sessionId: Long,
    val action: String,
)

class AppPictureInPictureActionReceiver(
    private val context: Context,
    private val authority: String,
    actions: Collection<String>,
    // SystemUI delivers PiP RemoteAction broadcasts across process boundaries.
    // Package scoping plus session ids keep the action target narrow.
    private val exported: Boolean = true,
    private val currentSessionId: () -> Long,
    private val onAction: (AppPictureInPictureActionEvent) -> Unit,
) {
    private val actionSet = actions.toSet()
    private var registered = false
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                val action = intent?.action?.takeIf(actionSet::contains) ?: return
                val identity =
                    intent.appPictureInPictureActionIdentity(
                        expectedAuthority = authority,
                        expectedAction = action,
                    ) ?: return
                val extraSessionId = intent.appPictureInPictureSessionIdExtra()
                if (extraSessionId != APP_PIP_NO_SESSION_ID && extraSessionId != identity.sessionId) {
                    return
                }
                val sessionId = identity.sessionId
                if (sessionId == APP_PIP_NO_SESSION_ID || sessionId != currentSessionId()) return
                onAction(
                    AppPictureInPictureActionEvent(
                        action = action,
                        sessionId = sessionId,
                        intent = intent,
                    )
                )
            }
        }

    fun register() {
        if (registered) return
        val filter =
            IntentFilter().apply {
                actionSet.forEach(::addAction)
                addDataScheme(APP_PIP_ACTION_URI_SCHEME)
            }
        context.registerReceiver(
            receiver,
            filter,
            if (exported) {
                Context.RECEIVER_EXPORTED
            } else {
                Context.RECEIVER_NOT_EXPORTED
            },
        )
        registered = true
    }

    fun unregister() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }
}

fun Intent?.appPictureInPictureSessionId(): Long {
    return appPictureInPictureSessionIdExtra()
        .takeIf { value -> value != APP_PIP_NO_SESSION_ID }
        ?: appPictureInPictureActionIdentity()?.sessionId
        ?: APP_PIP_NO_SESSION_ID
}

fun Intent?.appPictureInPictureActionIdentity(
    expectedAuthority: String? = null,
    expectedAction: String? = null,
): AppPictureInPictureActionIdentity? {
    val uri = this?.data ?: return null
    if (uri.scheme != APP_PIP_ACTION_URI_SCHEME) return null
    val authority = uri.authority?.takeIf { value -> value.isNotBlank() } ?: return null
    if (expectedAuthority != null && authority != expectedAuthority) return null
    val pathSegments = uri.pathSegments
    val sessionId =
        pathSegments
            .getOrNull(0)
            ?.toLongOrNull()
            ?.takeIf { value -> value != APP_PIP_NO_SESSION_ID }
            ?: return null
    val action = pathSegments.getOrNull(1)?.takeIf { value -> value.isNotBlank() } ?: return null
    if (expectedAction != null && action != expectedAction) return null
    return AppPictureInPictureActionIdentity(
        authority = authority,
        sessionId = sessionId,
        action = action,
    )
}

private fun Intent?.appPictureInPictureSessionIdExtra(): Long {
    return this?.getLongExtra(APP_PIP_EXTRA_SESSION_ID, APP_PIP_NO_SESSION_ID)
        ?: APP_PIP_NO_SESSION_ID
}
