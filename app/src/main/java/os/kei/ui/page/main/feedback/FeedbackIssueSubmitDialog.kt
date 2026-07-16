@file:Suppress("FunctionName")

package os.kei.ui.page.main.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.ui.page.main.os.appLucideWarningIcon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.dialog.AppWindowDialogHost
import os.kei.ui.page.main.widget.glass.AppLiquidDialogActionButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val FeedbackChecklistMaxHeight = 320.dp
internal const val FEEDBACK_CONFIRM_CHECKLIST_TEST_TAG = "feedback_confirm_checklist"

@Immutable
internal data class FeedbackSubmitDialogSnapshot(
    val mode: FeedbackSubmitMode,
    val apiTokenAvailable: Boolean,
    val submitting: Boolean,
)

@Stable
internal class FeedbackSubmitDialogExitSnapshot(
    initialValue: FeedbackSubmitDialogSnapshot?,
) {
    var retainedValue: FeedbackSubmitDialogSnapshot? by mutableStateOf(initialValue)
        private set

    fun resolve(currentValue: FeedbackSubmitDialogSnapshot?): FeedbackSubmitDialogSnapshot? = currentValue ?: retainedValue

    fun retain(currentValue: FeedbackSubmitDialogSnapshot?) {
        if (currentValue != null) {
            retainedValue = currentValue
        }
    }

    fun clear() {
        retainedValue = null
    }
}

@Composable
internal fun rememberFeedbackSubmitDialogExitSnapshot(currentValue: FeedbackSubmitDialogSnapshot?): FeedbackSubmitDialogExitSnapshot {
    val snapshot = remember { FeedbackSubmitDialogExitSnapshot(currentValue) }
    SideEffect { snapshot.retain(currentValue) }
    return snapshot
}

@Composable
internal fun FeedbackSubmitConfirmDialog(
    mode: FeedbackSubmitMode?,
    apiTokenAvailable: Boolean,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onConfirmBrowser: () -> Unit,
    onConfirmApi: () -> Unit,
) {
    val currentSnapshot =
        remember(mode, apiTokenAvailable, submitting) {
            mode?.let {
                FeedbackSubmitDialogSnapshot(
                    mode = it,
                    apiTokenAvailable = apiTokenAvailable,
                    submitting = submitting,
                )
            }
        }
    val exitSnapshot = rememberFeedbackSubmitDialogExitSnapshot(currentSnapshot)
    val renderedSnapshot = exitSnapshot.resolve(currentSnapshot)

    AppWindowDialogHost(
        show = mode != null,
        title = stringResource(R.string.feedback_issue_confirm_title),
        summary = renderedSnapshot?.let { feedbackSubmitSummary(it) },
        onDismissRequest = onDismiss,
        dismissible = mode != null && !submitting,
        onDismissFinished = exitSnapshot::clear,
    ) {
        renderedSnapshot?.let { snapshot ->
            FeedbackSubmitConfirmDialogBody(
                snapshot = snapshot,
                actionsEnabled = mode != null,
                onDismiss = onDismiss,
                onConfirmBrowser = onConfirmBrowser,
                onConfirmApi = onConfirmApi,
            )
        }
    }
}

@Composable
internal fun FeedbackSubmitConfirmDialogBody(
    snapshot: FeedbackSubmitDialogSnapshot,
    actionsEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onConfirmBrowser: () -> Unit,
    onConfirmApi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        FeedbackConfirmChecklist(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = FeedbackChecklistMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .testTag(FEEDBACK_CONFIRM_CHECKLIST_TEST_TAG),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppLiquidDialogActionButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                enabled = actionsEnabled && !snapshot.submitting,
            )
            AppLiquidDialogActionButton(
                modifier = Modifier.weight(1f),
                text = feedbackSubmitConfirmText(snapshot.mode),
                enabled = actionsEnabled && !snapshot.submitting,
                containerColor = MiuixTheme.colorScheme.primary,
                variant = GlassVariant.SheetPrimaryAction,
                onClick =
                    when (snapshot.mode) {
                        FeedbackSubmitMode.Browser -> onConfirmBrowser
                        FeedbackSubmitMode.GitHubApi -> onConfirmApi
                    },
            )
        }
    }
}

@Composable
internal fun feedbackSubmitSummary(snapshot: FeedbackSubmitDialogSnapshot): String =
    when (snapshot.mode) {
        FeedbackSubmitMode.Browser -> {
            stringResource(R.string.feedback_issue_confirm_browser_summary)
        }

        FeedbackSubmitMode.GitHubApi -> {
            if (snapshot.apiTokenAvailable) {
                stringResource(R.string.feedback_issue_confirm_api_summary)
            } else {
                stringResource(R.string.feedback_issue_confirm_api_missing_token)
            }
        }
    }

@Composable
private fun feedbackSubmitConfirmText(mode: FeedbackSubmitMode): String =
    when (mode) {
        FeedbackSubmitMode.Browser -> stringResource(R.string.feedback_issue_confirm_browser)
        FeedbackSubmitMode.GitHubApi -> stringResource(R.string.feedback_issue_confirm_api)
    }

@Composable
private fun FeedbackConfirmChecklist(modifier: Modifier = Modifier) {
    val items =
        listOf(
            stringResource(R.string.feedback_issue_confirm_check_public),
            stringResource(R.string.feedback_issue_confirm_check_sensitive),
            stringResource(R.string.feedback_issue_confirm_check_steps),
            stringResource(R.string.feedback_issue_confirm_check_zip),
        )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = appLucideWarningIcon(),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                )
                Text(
                    text = item,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                )
            }
        }
    }
}
