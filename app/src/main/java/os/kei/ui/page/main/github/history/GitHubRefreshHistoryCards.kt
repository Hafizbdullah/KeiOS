@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import os.kei.feature.github.model.GitHubRefreshHistoryRecord
import os.kei.feature.github.model.GitHubRefreshHistorySlowItem
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.ui.page.main.os.appLucideFilterIcon
import os.kei.ui.page.main.os.appLucideTimeIcon
import os.kei.ui.page.main.github.sheet.trackedSourceModeLabel
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppInfoRow
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GitHubHistoryOverviewCard(
    uiState: GitHubActionsNotificationHistoryUiState,
    modifier: Modifier = Modifier,
) {
    val filterLabel =
        when (uiState.historyMode) {
            GitHubHistoryMode.Refresh -> stringResource(uiState.refreshFilterMode.labelRes)
            GitHubHistoryMode.Actions -> stringResource(uiState.filterMode.labelRes)
            GitHubHistoryMode.Tracking -> stringResource(uiState.trackChangeFilterMode.labelRes)
            GitHubHistoryMode.Apps -> stringResource(uiState.appInstallFilterMode.labelRes)
        }
    val sortLabel =
        when (uiState.historyMode) {
            GitHubHistoryMode.Refresh -> stringResource(uiState.refreshSortMode.labelRes)
            GitHubHistoryMode.Actions -> stringResource(uiState.sortMode.labelRes)
            GitHubHistoryMode.Tracking -> stringResource(uiState.trackChangeSortMode.labelRes)
            GitHubHistoryMode.Apps -> stringResource(uiState.appInstallSortMode.labelRes)
        }
    val sortValue =
        stringResource(
            R.string.github_actions_history_summary_sort_value,
            sortLabel,
            stringResource(uiState.sortDirection.labelRes),
        )
    val shownCount =
        when (uiState.historyMode) {
            GitHubHistoryMode.Refresh -> uiState.refreshRecords.size
            GitHubHistoryMode.Actions -> uiState.records.size
            GitHubHistoryMode.Tracking -> uiState.trackChangeRecords.size
            GitHubHistoryMode.Apps -> uiState.appInstallRecords.size
        }
    val totalCount =
        when (uiState.historyMode) {
            GitHubHistoryMode.Refresh -> uiState.totalRefreshRecordCount
            GitHubHistoryMode.Actions -> uiState.totalRecordCount
            GitHubHistoryMode.Tracking -> uiState.totalTrackChangeRecordCount
            GitHubHistoryMode.Apps -> uiState.totalAppInstallRecordCount
        }
    AppSurfaceCard(
        modifier = modifier,
        showIndication = false,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CardLayoutRhythm.cardHorizontalPadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = appLucideFilterIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MiuixTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.github_history_summary_title),
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = AppTypographyTokens.CompactTitle.fontSize,
                    lineHeight = AppTypographyTokens.CompactTitle.lineHeight,
                    fontWeight = AppTypographyTokens.CompactTitle.fontWeight,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(
                    label =
                        stringResource(
                            R.string.github_actions_history_summary_subtitle,
                            shownCount,
                            totalCount,
                        ),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    size = AppStatusPillSize.Compact,
                    backgroundAlphaOverride = 0.12f,
                    borderAlphaOverride = 0.22f,
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                GitHubHistorySummaryPill(
                    label = stringResource(R.string.github_actions_history_summary_label_filter),
                    value = filterLabel,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                GitHubHistorySummaryPill(
                    label = stringResource(R.string.github_actions_history_summary_label_sort),
                    value = sortValue,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                uiState.lastCleanupRemovedCount?.let { removedCount ->
                    GitHubHistorySummaryPill(
                        label = stringResource(R.string.github_actions_history_summary_label_cleanup),
                        value = stringResource(R.string.github_actions_history_cleanup_removed, removedCount),
                        color = MiuixTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
internal fun GitHubHistorySummaryPill(
    label: String,
    value: String,
    color: Color,
) {
    StatusPill(
        label = stringResource(R.string.github_actions_history_summary_chip_value, label, value),
        color = color,
        size = AppStatusPillSize.Compact,
        backgroundAlphaOverride = 0.16f,
        borderAlphaOverride = 0.28f,
    )
}

@Composable
internal fun GitHubRefreshHistoryRecordCard(
    item: GitHubRefreshHistoryUiRecord,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onExpandedChange: (Boolean) -> Unit,
) {
    val record = item.record
    val finishedAt = rememberGitHubHistoryDateTime(record.finishedAtMillis)
    val title =
        stringResource(
            R.string.github_history_refresh_record_title,
            rememberRefreshSourceLabel(record.source),
            rememberRefreshScopeLabel(record.scope),
        )
    val subtitle =
        stringResource(
            R.string.github_history_refresh_record_summary,
            record.completedCount,
            record.targetCount,
            record.updatableCount,
            record.preReleaseUpdateCount,
            record.failedCount,
        )
    AppFeatureCard(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        eyebrow = stringResource(R.string.github_history_refresh_time_finished, finishedAt),
        sectionStartAction = {
            Icon(
                imageVector = appLucideTimeIcon(),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MiuixTheme.colorScheme.primary,
            )
        },
        collapsible = true,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        headerEndActions = {
            StatusPill(
                label = rememberRefreshOutcomeLabel(record),
                color = refreshOutcomeColor(record),
                size = AppStatusPillSize.Compact,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.compactSectionGap),
        ) {
            AppInfoRow(
                label = stringResource(R.string.github_history_refresh_label_purpose),
                value = rememberRefreshPurposeLabel(record),
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_history_refresh_label_progress),
                value =
                    stringResource(
                        R.string.github_history_refresh_progress_value,
                        record.completedCount,
                        record.targetCount,
                        record.totalTrackedCount,
                    ),
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_history_refresh_label_updates),
                value =
                    stringResource(
                        R.string.github_history_refresh_updates_value,
                        record.updatableCount,
                        record.preReleaseUpdateCount,
                    ),
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_history_refresh_label_failed),
                value = record.failedCount.toString(),
                valueColor = if (record.failedCount > 0) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onBackground,
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
            )
            AppInfoRow(
                label = stringResource(R.string.github_history_refresh_label_elapsed),
                value = rememberDurationLabel(record.elapsedMs),
                valueMaxLines = 1,
                valueOverflow = TextOverflow.Ellipsis,
            )
            if (record.maxConcurrency > 0) {
                AppInfoRow(
                    label = stringResource(R.string.github_history_refresh_label_schedule),
                    value =
                        stringResource(
                            R.string.github_history_refresh_schedule_value,
                            record.maxConcurrency,
                            record.directApkConcurrency,
                            record.fdroidConcurrency,
                        ),
                    valueMaxLines = 1,
                    valueOverflow = TextOverflow.Ellipsis,
                )
            }
            if (record.hasSourceMixDiagnostics()) {
                AppInfoRow(
                    label = stringResource(R.string.github_history_refresh_label_source_mix),
                    value =
                        stringResource(
                            R.string.github_history_refresh_source_mix_value,
                            record.repositoryItemCount,
                            record.directApkItemCount,
                            record.fdroidItemCount,
                            record.otherItemCount,
                        ),
                    stacked = true,
                    valueMaxLines = 2,
                    valueOverflow = TextOverflow.Ellipsis,
                )
            }
            AppInfoRow(
                label = stringResource(R.string.github_history_refresh_label_performance),
                value =
                    stringResource(
                        R.string.github_history_refresh_performance_value,
                        rememberDurationLabel(record.p50ItemMs),
                        rememberDurationLabel(record.p95ItemMs),
                        rememberDurationLabel(record.maxItemMs),
                    ),
                stacked = true,
                valueMaxLines = 2,
                valueOverflow = TextOverflow.Ellipsis,
            )
            record.slowItems.take(5).forEachIndexed { index, slowItem ->
                AppInfoRow(
                    label =
                        stringResource(
                            R.string.github_history_refresh_label_slow_index,
                            index + 1,
                        ),
                    value = rememberSlowRefreshItemValue(slowItem),
                    stacked = true,
                    valueMaxLines = 3,
                    valueOverflow = TextOverflow.Ellipsis,
                )
            }
            if (record.note.isNotBlank()) {
                AppInfoRow(
                    label = stringResource(R.string.github_history_refresh_label_note),
                    value = record.note,
                    stacked = true,
                    valueMaxLines = 3,
                    valueOverflow = TextOverflow.Ellipsis,
                )
            }
            record.failureSummaries.take(4).forEachIndexed { index, failure ->
                val label =
                    stringResource(
                        R.string.github_history_refresh_label_failure_index,
                        index + 1,
                    )
                val value =
                    listOf(
                        failure.appLabel.ifBlank { "${failure.owner}/${failure.repo}" },
                        failure.packageName,
                        failure.message,
                    ).filter { it.isNotBlank() }
                        .joinToString(" · ")
                AppInfoRow(
                    label = label,
                    value = value,
                    stacked = true,
                    valueMaxLines = 3,
                    valueOverflow = TextOverflow.Ellipsis,
                    valueColor = MiuixTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun rememberGitHubHistoryDateTime(millis: Long): String {
    val locale = Locale.getDefault()
    val formatter =
        remember(locale) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", locale)
        }
    return formatter.format(Date(millis))
}

@Composable
private fun rememberRefreshOutcomeLabel(record: GitHubRefreshHistoryRecord): String {
    return when {
        record.outcome == GitHubRefreshHistoryOutcome.Cancelled ->
            stringResource(R.string.github_history_refresh_outcome_cancelled)
        record.outcome == GitHubRefreshHistoryOutcome.Failed ->
            stringResource(R.string.github_actions_history_filter_failed)
        record.failedCount > 0 ->
            stringResource(R.string.github_history_refresh_outcome_partial_failed)
        else ->
            stringResource(R.string.github_actions_history_filter_success)
    }
}

@Composable
private fun refreshOutcomeColor(record: GitHubRefreshHistoryRecord): Color =
    when {
        record.outcome == GitHubRefreshHistoryOutcome.Cancelled -> MiuixTheme.colorScheme.onBackgroundVariant
        record.outcome == GitHubRefreshHistoryOutcome.Failed || record.failedCount > 0 -> MiuixTheme.colorScheme.error
        else -> Color(0xFF22C55E)
    }

@Composable
private fun rememberRefreshScopeLabel(scope: GitHubRefreshScope): String =
    when (scope) {
        GitHubRefreshScope.AllTracked -> stringResource(R.string.github_history_refresh_scope_all)
        GitHubRefreshScope.DueTracked -> stringResource(R.string.github_history_refresh_scope_due)
        GitHubRefreshScope.VisibleTracked -> stringResource(R.string.github_history_refresh_scope_visible)
        GitHubRefreshScope.RequestedTracked -> stringResource(R.string.github_history_refresh_scope_requested)
        GitHubRefreshScope.MissingCache -> stringResource(R.string.github_history_refresh_scope_missing_cache)
        GitHubRefreshScope.SingleTracked -> stringResource(R.string.github_history_refresh_scope_single)
        GitHubRefreshScope.ShortcutAllTracked -> stringResource(R.string.github_history_refresh_scope_shortcut_all)
    }

@Composable
private fun rememberRefreshSourceLabel(source: GitHubRefreshSource): String =
    when (source) {
        GitHubRefreshSource.Page -> stringResource(R.string.github_history_refresh_source_page)
        GitHubRefreshSource.BackgroundTick -> stringResource(R.string.github_history_refresh_source_background)
        GitHubRefreshSource.Shortcut -> stringResource(R.string.github_history_refresh_source_shortcut)
        GitHubRefreshSource.Debug -> stringResource(R.string.github_history_refresh_source_debug)
    }

@Composable
private fun rememberRefreshPurposeLabel(record: GitHubRefreshHistoryRecord): String =
    stringResource(
        R.string.github_history_refresh_purpose_value,
        rememberRefreshSourcePurposeLabel(record.source),
        rememberRefreshScopePurposeLabel(record.scope),
    )

@Composable
private fun rememberRefreshSourcePurposeLabel(source: GitHubRefreshSource): String =
    when (source) {
        GitHubRefreshSource.Page -> stringResource(R.string.github_history_refresh_source_purpose_page)
        GitHubRefreshSource.BackgroundTick -> stringResource(R.string.github_history_refresh_source_purpose_background)
        GitHubRefreshSource.Shortcut -> stringResource(R.string.github_history_refresh_source_purpose_shortcut)
        GitHubRefreshSource.Debug -> stringResource(R.string.github_history_refresh_source_purpose_debug)
    }

@Composable
private fun rememberRefreshScopePurposeLabel(scope: GitHubRefreshScope): String =
    when (scope) {
        GitHubRefreshScope.AllTracked -> stringResource(R.string.github_history_refresh_scope_purpose_all)
        GitHubRefreshScope.DueTracked -> stringResource(R.string.github_history_refresh_scope_purpose_due)
        GitHubRefreshScope.VisibleTracked -> stringResource(R.string.github_history_refresh_scope_purpose_visible)
        GitHubRefreshScope.RequestedTracked -> stringResource(R.string.github_history_refresh_scope_purpose_requested)
        GitHubRefreshScope.MissingCache -> stringResource(R.string.github_history_refresh_scope_purpose_missing_cache)
        GitHubRefreshScope.SingleTracked -> stringResource(R.string.github_history_refresh_scope_purpose_single)
        GitHubRefreshScope.ShortcutAllTracked -> stringResource(R.string.github_history_refresh_scope_purpose_shortcut_all)
    }

@Composable
private fun rememberSlowRefreshItemValue(slowItem: GitHubRefreshHistorySlowItem): String {
    val displayName =
        slowItem.appLabel
            .ifBlank {
                listOf(slowItem.owner, slowItem.repo)
                    .filter { it.isNotBlank() }
                    .joinToString("/")
            }
            .ifBlank { slowItem.packageName }
            .ifBlank { slowItem.trackId }
    val identity = buildSlowRefreshItemIdentity(slowItem, displayName)
    val statusLabel = rememberRefreshStatusLabel(slowItem.status)
    val detail = rememberRefreshMessageDetail(slowItem.message)
    return if (detail.isBlank()) {
        stringResource(
            R.string.github_history_refresh_slow_item_value,
            displayName,
            rememberRefreshSourceModeLabel(slowItem.sourceMode),
            rememberDurationLabel(slowItem.elapsedMs),
            identity,
            statusLabel,
        )
    } else {
        stringResource(
            R.string.github_history_refresh_slow_item_value_with_detail,
            displayName,
            rememberRefreshSourceModeLabel(slowItem.sourceMode),
            rememberDurationLabel(slowItem.elapsedMs),
            identity,
            statusLabel,
            detail,
        )
    }
}

@Composable
private fun rememberRefreshSourceModeLabel(sourceMode: String): String =
    trackedSourceModeLabel(GitHubTrackedSourceMode.fromStorageId(sourceMode))

@Composable
private fun rememberRefreshStatusLabel(status: String): String {
    return when (remember(status) { runCatching { enumValueOf<GitHubTrackedReleaseStatus>(status.trim()) }.getOrNull() }) {
        GitHubTrackedReleaseStatus.UpdateAvailable -> stringResource(R.string.github_status_update_available)
        GitHubTrackedReleaseStatus.PreReleaseUpdateAvailable ->
            stringResource(R.string.github_status_prerelease_update_available)
        GitHubTrackedReleaseStatus.PreReleaseOptional -> stringResource(R.string.github_status_prerelease_optional)
        GitHubTrackedReleaseStatus.PreReleaseTracked -> stringResource(R.string.github_status_prerelease_tracked)
        GitHubTrackedReleaseStatus.UpToDate -> stringResource(R.string.github_status_up_to_date)
        GitHubTrackedReleaseStatus.Ignored -> stringResource(R.string.github_status_ignored)
        GitHubTrackedReleaseStatus.MatchedRelease -> stringResource(R.string.github_status_matched_release)
        GitHubTrackedReleaseStatus.ComparisonUncertain -> stringResource(R.string.github_status_comparison_uncertain)
        GitHubTrackedReleaseStatus.Failed -> stringResource(R.string.github_actions_history_filter_failed)
        null -> status.ifBlank { stringResource(R.string.github_item_value_check_pending) }
    }
}

@Composable
private fun rememberRefreshMessageDetail(message: String): String {
    val raw = message.trim()
    if (raw.isBlank()) return ""
    if (GitHubTrackedReleaseStatus.isOnlyPreReleasesHint(raw)) {
        return stringResource(R.string.github_status_only_prereleases_hint)
    }
    if (GitHubTrackedReleaseStatus.isFailureMessage(raw)) {
        val failed = stringResource(R.string.github_actions_history_filter_failed)
        val localized = GitHubTrackedReleaseStatus.localizedFailureDetail(raw, failed)
        return localized.takeUnless { it == failed }.orEmpty()
    }
    return if (GitHubTrackedReleaseStatus.fromMessage(raw) != null) {
        ""
    } else {
        raw
    }
}

@Composable
private fun rememberDurationLabel(millis: Long): String {
    val safe = millis.coerceAtLeast(0L)
    return when {
        safe >= 60_000L -> {
            val minutes = safe / 60_000L
            val seconds = (safe % 60_000L) / 1_000L
            stringResource(R.string.github_history_duration_minutes_seconds, minutes, seconds)
        }
        safe >= 1_000L -> {
            val seconds = safe / 1_000L
            stringResource(R.string.github_history_duration_seconds, seconds)
        }
        else -> stringResource(R.string.github_history_duration_millis, safe)
    }
}

private fun buildSlowRefreshItemIdentity(
    slowItem: GitHubRefreshHistorySlowItem,
    displayName: String,
): String {
    val repo =
        listOf(slowItem.owner, slowItem.repo)
            .filter { it.isNotBlank() }
            .joinToString("/")
    val identity =
        repo
            .ifBlank { slowItem.packageName }
            .ifBlank { slowItem.trackId }
    return identity.takeUnless { it.equals(displayName, ignoreCase = true) }
        ?: slowItem.packageName.takeUnless { it.equals(displayName, ignoreCase = true) }
        ?: slowItem.trackId
}

private fun GitHubRefreshHistoryRecord.hasSourceMixDiagnostics(): Boolean {
    return repositoryItemCount > 0 ||
        directApkItemCount > 0 ||
        fdroidItemCount > 0 ||
        otherItemCount > 0
}
