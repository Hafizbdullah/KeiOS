@file:Suppress("PropertyName")

package os.kei.ui.testing

object KeiOsTestTags {
    const val MainBottomTabHome = "main_bottom_tab_home"
    const val MainBottomTabOs = "main_bottom_tab_os"
    const val MainBottomTabMcp = "main_bottom_tab_mcp"
    const val MainBottomTabGitHub = "main_bottom_tab_github"
    const val MainBottomTabBa = "main_bottom_tab_ba"

    /**
     * The button that converts the tab bar into a sidebar, and back.
     *
     * Tagged so the baseline profile can reach the sidebar at all: it is the only entry point, and the rail's
     * composables would otherwise never be compiled into the profile even on a tablet-shaped window.
     */
    const val MainSidebarToggle = "main_sidebar_toggle"
    const val MainSidebarRowHome = "main_sidebar_row_home"
    const val MainSidebarRowOs = "main_sidebar_row_os"
    const val MainSidebarRowMcp = "main_sidebar_row_mcp"
    const val MainSidebarRowGitHub = "main_sidebar_row_github"
    const val MainSidebarRowBa = "main_sidebar_row_ba"
    const val MainPagerSettledHome = "main_pager_settled_home"
    const val MainPagerSettledOs = "main_pager_settled_os"
    const val MainPagerSettledMcp = "main_pager_settled_mcp"
    const val MainPagerSettledGitHub = "main_pager_settled_github"
    const val MainPagerSettledBa = "main_pager_settled_ba"
    const val HomePageRoot = "home_page_root"
    const val HomeSettingsButton = "home_settings_button"
    const val HomeAboutButton = "home_about_button"
    const val HomeWebDavCard = "home_webdav_card"
    const val SettingsPageRoot = "settings_page_root"
    const val AboutPageRoot = "about_page_root"
    const val WebDavSyncPageRoot = "webdav_sync_page_root"
    const val OsPageRoot = "os_page_root"
    const val OsShellRunnerButton = "os_shell_runner_button"
    const val OsShellRunnerPageRoot = "os_shell_runner_page_root"
    const val McpPageRoot = "mcp_page_root"
    const val McpSkillButton = "mcp_skill_button"
    const val McpSkillPageRoot = "mcp_skill_page_root"
    const val BaPageRoot = "ba_page_root"
    const val BaAccountManagementButton = "ba_account_management_button"
    const val BaCraftCardHeader = "ba_craft_card_header"
    const val BaCraftSlotFirst = "ba_craft_slot_first"
    const val BaDockOpenCalendar = "ba_dock_open_calendar"
    const val BaDockOpenPool = "ba_dock_open_pool"
    const val GitHubPageRoot = "github_page_root"
    const val GitHubImportMenuButton = "github_import_menu_button"
    const val GitHubImportTracks = "github_import_tracks"
    const val GitHubImportStars = "github_import_stars"
    const val GitHubShareImportCancel = "github_share_import_cancel"
    const val GitHubShareImportConfirm = "github_share_import_confirm"
    const val GitHubShareImportPendingClose = "github_share_import_pending_close"
    const val GitHubShareImportPendingCancel = "github_share_import_pending_cancel"
    const val GitHubShareImportAttachClose = "github_share_import_attach_close"
    const val GitHubShareImportAttachCancel = "github_share_import_attach_cancel"
    const val GitHubShareImportAttachConfirm = "github_share_import_attach_confirm"
    const val GitHubActionsHistoryButton = "github_actions_history_button"
    const val GitHubActionsHistoryPageRoot = "github_actions_history_page_root"
    const val GitHubShareImportAttachConfirmOpenGitHub =
        "github_share_import_attach_confirm_open_github"
}
