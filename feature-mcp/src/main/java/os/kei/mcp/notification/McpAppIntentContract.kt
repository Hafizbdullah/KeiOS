package os.kei.mcp.notification

object McpAppIntentContract {
    const val MAIN_ACTIVITY_CLASS_NAME = "os.kei.MainActivity"
    const val EXTRA_TARGET_BOTTOM_PAGE = "os.kei.extra.TARGET_BOTTOM_PAGE"
    const val EXTRA_TARGET_ROUTE = "os.kei.extra.TARGET_ROUTE"
    const val EXTRA_BA_ACCOUNT_ID = "os.kei.extra.BA_ACCOUNT_ID"
    const val TARGET_BOTTOM_PAGE_MCP = "Mcp"
    const val TARGET_BOTTOM_PAGE_BA = "Ba"
    const val TARGET_ROUTE_WEBDAV_SYNC = "WebDavSync"
}

object McpNotificationMarkReadContract {
    const val ACTION = "os.kei.focus.notification.action.MARK_READ"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_SERVER_NAME = "server_name"
    const val EXTRA_TARGET_BA_ACCOUNT_ID = "target_ba_account_id"
}

object McpNotificationDismissContract {
    const val ACTION = "os.kei.focus.notification.action.DISMISS"
    const val EXTRA_NOTIFICATION_ID = McpNotificationMarkReadContract.EXTRA_NOTIFICATION_ID
    const val EXTRA_SERVER_NAME = McpNotificationMarkReadContract.EXTRA_SERVER_NAME
    const val EXTRA_TARGET_BA_ACCOUNT_ID = McpNotificationMarkReadContract.EXTRA_TARGET_BA_ACCOUNT_ID
}

internal object McpNotificationActionContract {
    const val NOTIFICATION_ACTION_RECEIVER_CLASS_NAME =
        "os.kei.feature.notification.NotificationActionReceiver"
    const val MI_FOCUS_ACTION_RECEIVER_CLASS_NAME =
        "os.kei.feature.notification.MiFocusNotificationActionReceiver"
    const val ACTION_STOP_MCP_SERVER = "os.kei.notification.action.STOP_MCP_SERVER"
    const val ACTION_MI_FOCUS_MARK_READ = McpNotificationMarkReadContract.ACTION
    const val ACTION_MI_FOCUS_DISMISS = McpNotificationDismissContract.ACTION
    const val EXTRA_NOTIFICATION_ID = McpNotificationMarkReadContract.EXTRA_NOTIFICATION_ID
}
