@file:Suppress("FunctionName")

package os.kei.ui.page.main.os.shell.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideBackIcon
import os.kei.ui.page.main.os.shell.component.OsShellRunnerInputCard
import os.kei.ui.page.main.os.shell.state.OsShellRunnerTextBundle
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.chrome.AppLiquidNavigationButton
import os.kei.ui.page.main.widget.chrome.AppPageLazyColumn
import os.kei.ui.page.main.widget.chrome.AppPageScaffold
import os.kei.ui.testing.KeiOsTestTags
import os.kei.ui.testing.pageRootTestTag
import os.kei.ui.page.main.widget.chrome.LiquidToolbar
import os.kei.ui.page.main.widget.chrome.LiquidToolbarAction
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun OsShellRunnerContent(
    textBundle: OsShellRunnerTextBundle,
    scrollBehavior: ScrollBehavior,
    topBarBackdrop: LayerBackdrop,
    pageListState: LazyListState,
    actionItems: List<LiquidToolbarAction>,
    commandInput: String,
    runningCommand: Boolean,
    startupFocusRequestToken: Int,
    outputContent: @Composable () -> Unit,
    onRequestClose: () -> Unit,
    onCommandInputChange: (String) -> Unit,
    onRunCommand: () -> Unit,
    onStopCommand: () -> Unit,
    onOpenSaveCommandSheet: () -> Unit,
) {
    AppPageScaffold(
        title = textBundle.shellPageTitle,
        largeTitle = textBundle.shellPageTitle,
        modifier = Modifier.pageRootTestTag(KeiOsTestTags.OsShellRunnerPageRoot),
        scrollBehavior = scrollBehavior,
        titleBackdrop = topBarBackdrop,
        reserveTopEndActionSpace = true,
        navigationIcon = {
            AppLiquidNavigationButton(
                icon = appLucideBackIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onRequestClose,
                backdrop = topBarBackdrop,
            )
        },
        actions = {
            LiquidToolbar(
                backdrop = topBarBackdrop,
                actions = actionItems,
            )
        },
    ) { innerPadding ->
        AppPageLazyColumn(
            innerPadding = innerPadding,
            state = pageListState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .layerBackdrop(topBarBackdrop),
            sectionSpacing = AppChromeTokens.pageSectionGap,
        ) {
            item(key = "shell_input_card", contentType = "shell_input_card") {
                OsShellRunnerInputCard(
                    inputTitle = textBundle.inputTitle,
                    inputHint = textBundle.inputHint,
                    commandInput = commandInput,
                    onCommandInputChange = onCommandInputChange,
                    runningCommand = runningCommand,
                    runActionDescription = textBundle.runActionDescription,
                    stopActionDescription = textBundle.stopActionDescription,
                    saveCommandActionDescription = textBundle.saveCommandActionDescription,
                    focusRequestToken = startupFocusRequestToken,
                    onRunCommand = onRunCommand,
                    onStopCommand = onStopCommand,
                    onOpenSaveCommandSheet = onOpenSaveCommandSheet,
                )
            }
            item(key = "shell_output_card", contentType = "shell_output_card") {
                outputContent()
            }
        }
    }
}
