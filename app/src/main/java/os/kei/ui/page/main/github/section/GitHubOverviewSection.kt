package os.kei.ui.page.main.github.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubLookupStrategyOption
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.OverviewRefreshState
import os.kei.ui.page.main.github.borderColor
import os.kei.ui.page.main.github.color
import os.kei.ui.page.main.github.formatRefreshAgo
import os.kei.ui.page.main.github.indicatorBackground
import os.kei.ui.page.main.github.overviewLookupPillLabel
import os.kei.ui.page.main.github.surfaceColor
import os.kei.ui.page.main.widget.core.AppOverviewCard
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.AppLiquidDialogActionButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.glass.LiquidCircularProgressBar
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

internal data class GitHubOverviewMetrics(
    val trackedCount: Int,
    val stableUpdateCount: Int,
    val totalUpdatableCount: Int,
    val stableLatestCount: Int,
    val preReleaseCount: Int,
    val preReleaseUpdateCount: Int,
    val failedCount: Int,
    val oldestCheckedAtMillis: Long = 0L,
    val latestCheckedAtMillis: Long = 0L
)

private fun overviewMetricColor(
    color: Color,
    emphasized: Boolean,
    isDark: Boolean
): Color {
    return if (emphasized) {
        color
    } else {
        color.copy(alpha = if (isDark) 0.76f else 0.84f)
    }
}

@Composable
internal fun GitHubOverviewCard(
    backdrop: Backdrop? = null,
    isDark: Boolean,
    lookupConfig: GitHubLookupConfig,
    overviewRefreshState: OverviewRefreshState,
    refreshProgress: Float,
    lastRefreshMs: Long,
    visibleEntries: Set<GitHubOverviewEntry>,
    metrics: GitHubOverviewMetrics,
    failedFilterActive: Boolean,
    onEditVisibleEntries: () -> Unit,
    onRetryFailedTracked: () -> Unit,
    onFailedFilterToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lookupValue = lookupConfig.overviewLookupPillLabel(context)
    val lookupColor =
        when {
            lookupConfig.selectedStrategy != GitHubLookupStrategyOption.GitHubApiToken ->
                GitHubStatusPalette.Active
            lookupConfig.apiToken.isBlank() ->
                GitHubStatusPalette.PreRelease
            else ->
                GitHubStatusPalette.Active
        }
    val displayRefreshState = if (
        overviewRefreshState == OverviewRefreshState.Idle && lastRefreshMs > 0L
    ) {
        OverviewRefreshState.Cached
    } else {
        overviewRefreshState
    }
    val entries = visibleEntries.orDefaultGitHubOverviewEntries()
    AppOverviewCard(
        title = stringResource(R.string.github_overview_title),
        backdrop = backdrop,
        titleColor = MiuixTheme.colorScheme.onBackground,
        subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant,
        containerColor = displayRefreshState.surfaceColor(
            isDark = isDark,
            neutralSurface = MiuixTheme.colorScheme.surface
        ),
        borderColor = displayRefreshState.borderColor(
            isDark = isDark,
            neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
        ),
        contentColor = MiuixTheme.colorScheme.onBackground,
        onLongClick = onEditVisibleEntries,
        titleAccessory = {
            val showLookupMode =
                GitHubOverviewEntry.Strategy in entries || GitHubOverviewEntry.Api in entries
            if (showLookupMode) {
                GitHubOverviewLookupModePill(
                    label = lookupValue,
                    color = lookupColor,
                    backdrop = backdrop,
                )
            }
        },
        headerEndActions = {
            if (displayRefreshState != OverviewRefreshState.Idle) {
                val indicatorColor = displayRefreshState.color(
                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                )
                val indicatorBg = displayRefreshState.indicatorBackground(
                    neutralSurface = MiuixTheme.colorScheme.surface
                )
                val progressValue = when (displayRefreshState) {
                    OverviewRefreshState.Refreshing -> refreshProgress.coerceIn(0f, 1f)
                    OverviewRefreshState.Completed,
                    OverviewRefreshState.Failed,
                    OverviewRefreshState.Cached -> 1f
                    OverviewRefreshState.Idle -> 0f
                }
                LiquidCircularProgressBar(
                    progress = { progressValue },
                    size = 18.dp,
                    strokeWidth = 2.dp,
                    activeColor = indicatorColor,
                    inactiveColor = indicatorBg
                )
            }
            StatusPill(
                label = formatRefreshAgo(context = context, lastRefreshMs = lastRefreshMs),
                color = displayRefreshState.color(
                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                ),
                backgroundAlphaOverride = if (isDark) 0.18f else 0.24f,
                borderAlphaOverride = if (isDark) 0.35f else 0.42f,
                backdrop = backdrop
            )
            StatusPill(
                label = when (displayRefreshState) {
                    OverviewRefreshState.Cached -> stringResource(R.string.common_status_cached)
                    OverviewRefreshState.Refreshing -> stringResource(R.string.common_status_checking)
                    OverviewRefreshState.Completed -> stringResource(R.string.common_status_checked)
                    OverviewRefreshState.Failed -> stringResource(R.string.common_status_failed)
                    OverviewRefreshState.Idle -> stringResource(R.string.common_status_pending_check)
                },
                color = displayRefreshState.color(
                    neutralColor = MiuixTheme.colorScheme.onBackgroundVariant
                ),
                backdrop = backdrop
            )
        }
    ) {
        GitHubOverviewExpandedContent(
            backdrop = backdrop,
            isDark = isDark,
            visibleEntries = entries,
            metrics = metrics,
            failedFilterActive = failedFilterActive,
            onRetryFailedTracked = onRetryFailedTracked,
            onFailedFilterToggle = onFailedFilterToggle
        )
    }
}

@Composable
private fun GitHubOverviewExpandedContent(
    backdrop: Backdrop?,
    isDark: Boolean,
    visibleEntries: Set<GitHubOverviewEntry>,
    metrics: GitHubOverviewMetrics,
    failedFilterActive: Boolean,
    onRetryFailedTracked: () -> Unit,
    onFailedFilterToggle: (Boolean) -> Unit
) {
    val entries = visibleEntries.orDefaultGitHubOverviewEntries()
    val metricPills =
        buildGitHubOverviewExpandedPillPlan(entries).map { pill ->
            pill.toDisplayPill(
                isDark = isDark,
                metrics = metrics
            )
        }
    val pills =
        buildList {
            if (GitHubOverviewEntry.Tracked in entries) {
                add(
                    GitHubOverviewDisplayPill(
                        label = metrics.trackedCount.toString(),
                        color = overviewMetricColor(
                            color = GitHubStatusPalette.Stable,
                            emphasized = metrics.trackedCount > 0,
                            isDark = isDark,
                        ),
                    )
                )
            }
            addAll(metricPills)
        }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CardLayoutRhythm.denseSectionGap)
    ) {
        GitHubOverviewExpandedPillFlow(
            pills = pills,
            backdrop = backdrop,
        )
        if (metrics.failedCount > 0) {
            if (failedFilterActive) {
                StatusPill(
                    label = stringResource(R.string.github_overview_failed_filter_active),
                    color = GitHubStatusPalette.Error,
                    backdrop = backdrop
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CardLayoutRhythm.controlRowGap)
            ) {
                AppLiquidDialogActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        if (failedFilterActive) {
                            R.string.github_overview_action_clear_failed_filter
                        } else {
                            R.string.github_overview_action_show_failed
                        }
                    ),
                    onClick = { onFailedFilterToggle(!failedFilterActive) },
                    containerColor = GitHubStatusPalette.Error,
                    variant = GlassVariant.SheetDangerAction
                )
                AppLiquidDialogActionButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.github_overview_action_retry_failed),
                    onClick = onRetryFailedTracked,
                    containerColor = GitHubStatusPalette.Update
                )
            }
        }
    }
}

internal enum class GitHubOverviewExpandedPillKind {
    Stable,
    StableUpdate,
    StableLatest,
    PreRelease,
    PreReleaseTracked,
    PreReleaseUpdate,
    CheckFailed
}

internal data class GitHubOverviewExpandedPillPlan(
    val kind: GitHubOverviewExpandedPillKind
)

private data class GitHubOverviewDisplayPill(
    val label: String,
    val color: Color
)

private val GitHubOverviewPillHeight = 28.dp
private val GitHubOverviewPillPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)

internal fun buildGitHubOverviewExpandedPillPlan(
    visibleEntries: Set<GitHubOverviewEntry>
): List<GitHubOverviewExpandedPillPlan> {
    val entries = visibleEntries.orDefaultGitHubOverviewEntries()
    return buildList {
        when {
            GitHubOverviewEntry.StableUpdate in entries &&
                    GitHubOverviewEntry.StableLatest in entries ->
                add(GitHubOverviewExpandedPillPlan(GitHubOverviewExpandedPillKind.Stable))

            GitHubOverviewEntry.StableUpdate in entries ->
                add(GitHubOverviewExpandedPillPlan(GitHubOverviewExpandedPillKind.StableUpdate))

            GitHubOverviewEntry.StableLatest in entries ->
                add(GitHubOverviewExpandedPillPlan(GitHubOverviewExpandedPillKind.StableLatest))
        }
        when {
            GitHubOverviewEntry.PreReleaseTracked in entries &&
                    GitHubOverviewEntry.PreReleaseUpdate in entries ->
                add(GitHubOverviewExpandedPillPlan(GitHubOverviewExpandedPillKind.PreRelease))

            GitHubOverviewEntry.PreReleaseTracked in entries ->
                add(GitHubOverviewExpandedPillPlan(GitHubOverviewExpandedPillKind.PreReleaseTracked))

            GitHubOverviewEntry.PreReleaseUpdate in entries ->
                add(GitHubOverviewExpandedPillPlan(GitHubOverviewExpandedPillKind.PreReleaseUpdate))
        }
        if (GitHubOverviewEntry.CheckFailed in entries) {
            add(GitHubOverviewExpandedPillPlan(GitHubOverviewExpandedPillKind.CheckFailed))
        }
    }
}

@Composable
private fun GitHubOverviewExpandedPillPlan.toDisplayPill(
    isDark: Boolean,
    metrics: GitHubOverviewMetrics
): GitHubOverviewDisplayPill {
    val stableTotal = metrics.stableUpdateCount + metrics.stableLatestCount
    val label = when (kind) {
        GitHubOverviewExpandedPillKind.Stable ->
            stringResource(
                R.string.github_overview_pill_stable_pair,
                metrics.stableUpdateCount,
                stableTotal
            )

        GitHubOverviewExpandedPillKind.StableUpdate ->
            stringResource(R.string.github_overview_pill_stable_update, metrics.stableUpdateCount)

        GitHubOverviewExpandedPillKind.StableLatest ->
            stringResource(R.string.github_overview_pill_stable_latest, metrics.stableLatestCount)

        GitHubOverviewExpandedPillKind.PreRelease ->
            stringResource(
                R.string.github_overview_pill_prerelease_pair,
                metrics.preReleaseUpdateCount,
                metrics.preReleaseCount
            )

        GitHubOverviewExpandedPillKind.PreReleaseTracked ->
            stringResource(R.string.github_overview_pill_prerelease_tracked, metrics.preReleaseCount)

        GitHubOverviewExpandedPillKind.PreReleaseUpdate ->
            stringResource(
                R.string.github_overview_pill_prerelease_update,
                metrics.preReleaseUpdateCount
            )

        GitHubOverviewExpandedPillKind.CheckFailed ->
            stringResource(R.string.github_overview_pill_failed, metrics.failedCount)
    }
    val color = when (kind) {
        GitHubOverviewExpandedPillKind.Stable,
        GitHubOverviewExpandedPillKind.StableUpdate ->
            overviewMetricColor(
                color = GitHubStatusPalette.Update,
                emphasized = metrics.stableUpdateCount > 0,
                isDark = isDark
            )

        GitHubOverviewExpandedPillKind.StableLatest ->
            overviewMetricColor(
                color = GitHubStatusPalette.Stable,
                emphasized = metrics.stableLatestCount > 0,
                isDark = isDark
            )

        GitHubOverviewExpandedPillKind.PreRelease,
        GitHubOverviewExpandedPillKind.PreReleaseTracked,
        GitHubOverviewExpandedPillKind.PreReleaseUpdate ->
            overviewMetricColor(
                color = GitHubStatusPalette.PreRelease,
                emphasized = metrics.preReleaseCount > 0 || metrics.preReleaseUpdateCount > 0,
                isDark = isDark
            )

        GitHubOverviewExpandedPillKind.CheckFailed ->
            overviewMetricColor(
                color = GitHubStatusPalette.Error,
                emphasized = metrics.failedCount > 0,
                isDark = isDark
            )
    }
    return GitHubOverviewDisplayPill(label = label, color = color)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GitHubOverviewExpandedPillFlow(
    pills: List<GitHubOverviewDisplayPill>,
    backdrop: Backdrop?
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pills.forEach { pill ->
            GitHubOverviewExpandedPill(
                pill = pill,
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.Bottom)
            )
        }
    }
}

@Composable
private fun GitHubOverviewLookupModePill(
    label: String,
    color: Color,
    backdrop: Backdrop?,
) {
    StatusPill(
        label = label,
        color = color,
        size = AppStatusPillSize.Compact,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
        backdrop = backdrop,
    )
}

@Composable
private fun GitHubOverviewExpandedPill(
    pill: GitHubOverviewDisplayPill,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    StatusPill(
        label = pill.label,
        color = pill.color,
        modifier = modifier.height(GitHubOverviewPillHeight),
        size = AppStatusPillSize.Compact,
        contentPadding = GitHubOverviewPillPadding,
        backdrop = backdrop
    )
}

@Preview(name = "GitHub Overview Light", showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun GitHubOverviewCardPreview() {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
        GitHubOverviewCard(
            isDark = false,
            lookupConfig = GitHubLookupConfig(
                selectedStrategy = GitHubLookupStrategyOption.GitHubApiToken,
                apiToken = "github_pat_preview_token"
            ),
            overviewRefreshState = OverviewRefreshState.Completed,
            refreshProgress = 1f,
            lastRefreshMs = System.currentTimeMillis() - 180_000L,
            visibleEntries = defaultGitHubOverviewEntries(),
            metrics = GitHubOverviewMetrics(
                trackedCount = 18,
                stableUpdateCount = 4,
                totalUpdatableCount = 6,
                stableLatestCount = 11,
                preReleaseCount = 3,
                preReleaseUpdateCount = 2,
                failedCount = 1,
                oldestCheckedAtMillis = System.currentTimeMillis() - 7_200_000L,
                latestCheckedAtMillis = System.currentTimeMillis() - 180_000L
            ),
            failedFilterActive = false,
            onEditVisibleEntries = {},
            onRetryFailedTracked = {},
            onFailedFilterToggle = {}
        )
    }
}
