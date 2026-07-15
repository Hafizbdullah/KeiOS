@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.student.BaGuideStudentDetailFreshnessTier
import os.kei.ui.page.main.student.page.state.BaStudentGuideCacheStatusUiState
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.AppLiquidTextButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetControlRow
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSummaryCard
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.AppStatusColors
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun BaStudentGuideCacheStatusSheet(
    show: Boolean,
    cacheStatus: BaStudentGuideCacheStatusUiState,
    backdrop: Backdrop?,
    onDismissRequest: () -> Unit,
    onRefreshCurrentStudent: () -> Unit,
    onClearCurrentStudentCache: () -> Unit,
) {
    SnapshotWindowBottomSheet(
        show = show,
        preferExportedBackdrop = true,
        title = stringResource(R.string.guide_cache_status_sheet_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            AppLiquidIconButton(
                backdrop = backdrop,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                variant = GlassVariant.Bar,
                onClick = onDismissRequest,
            )
        },
    ) {
        val nowMs = remember(show, cacheStatus) { System.currentTimeMillis() }
        SheetContentColumn(verticalSpacing = 10.dp) {
            BaStudentGuideCacheStatusSummary(
                cacheStatus = cacheStatus,
                nowMs = nowMs,
            )
            BaStudentGuideCacheStatusActions(
                backdrop = backdrop,
                hasStatus = cacheStatus.hasStatus,
                onRefreshCurrentStudent = onRefreshCurrentStudent,
                onClearCurrentStudentCache = onClearCurrentStudentCache,
            )
            BaStudentGuideCacheStatusPolicy()
        }
    }
}

@Composable
private fun BaStudentGuideCacheStatusSummary(
    cacheStatus: BaStudentGuideCacheStatusUiState,
    nowMs: Long,
) {
    val accent = cacheStatusAccent(cacheStatus)
    val tierLabel =
        cacheStatus.freshnessTier?.let { tier ->
            cacheStatusTierLabel(tier)
        } ?: stringResource(R.string.guide_cache_status_empty)
    SheetSummaryCard(
        title = stringResource(R.string.guide_cache_status_summary_title),
        badgeLabel = tierLabel,
        badgeColor = accent,
        titleMaxLines = 2,
    ) {
        if (!cacheStatus.hasStatus) {
            SheetDescriptionText(stringResource(R.string.guide_cache_status_empty))
            return@SheetSummaryCard
        }
        if (cacheStatus.validatingInBackground) {
            BaStudentGuideCacheStatusLine(
                label = stringResource(R.string.guide_cache_status_state),
                value = stringResource(R.string.guide_cache_status_validating),
                valueColor = AppStatusColors.Refreshing,
            )
        }
        BaStudentGuideCacheStatusLine(
            label = stringResource(R.string.guide_cache_status_cached_at),
            value = cacheStatusTimeLabel(cacheStatus.cachedAtMs, nowMs),
        )
        BaStudentGuideCacheStatusLine(
            label = stringResource(R.string.guide_cache_status_last_validated),
            value = cacheStatusTimeLabel(cacheStatus.lastValidatedAtMs, nowMs),
        )
        val nextLabelRes =
            if (cacheStatus.nextRetryAtMs > 0L) {
                R.string.guide_cache_status_next_retry
            } else {
                R.string.guide_cache_status_next_auto_refresh
            }
        val nextTime =
            cacheStatus.nextRetryAtMs
                .takeIf { it > 0L }
                ?: cacheStatus.nextAutoRefreshAtMs
        BaStudentGuideCacheStatusLine(
            label = stringResource(nextLabelRes),
            value = cacheStatusTimeLabel(nextTime, nowMs),
        )
        if (cacheStatus.failureCount > 0) {
            BaStudentGuideCacheStatusLine(
                label = stringResource(R.string.guide_cache_status_failure_count),
                value = cacheStatus.failureCount.toString(),
                valueColor = MiuixTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun BaStudentGuideCacheStatusActions(
    backdrop: Backdrop?,
    hasStatus: Boolean,
    onRefreshCurrentStudent: () -> Unit,
    onClearCurrentStudentCache: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppLiquidTextButton(
            modifier = Modifier.weight(1f),
            backdrop = backdrop,
            text = stringResource(R.string.guide_cache_status_action_refresh),
            textColor = MiuixTheme.colorScheme.primary,
            containerColor = MiuixTheme.colorScheme.primary,
            variant = GlassVariant.SheetAction,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis,
            onClick = onRefreshCurrentStudent,
        )
        AppLiquidTextButton(
            modifier = Modifier.weight(1f),
            backdrop = backdrop,
            text = stringResource(R.string.guide_cache_status_action_clear),
            textColor = MiuixTheme.colorScheme.error,
            containerColor = MiuixTheme.colorScheme.error,
            variant = GlassVariant.SheetAction,
            enabled = hasStatus,
            textMaxLines = 1,
            textOverflow = TextOverflow.Ellipsis,
            onClick = onClearCurrentStudentCache,
        )
    }
}

@Composable
private fun BaStudentGuideCacheStatusPolicy() {
    SheetSectionCard(verticalSpacing = 8.dp) {
        Text(
            text = stringResource(R.string.guide_cache_status_policy_title),
            color = MiuixTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        SheetDescriptionText(stringResource(R.string.guide_cache_status_policy_summary))
    }
}

@Composable
private fun BaStudentGuideCacheStatusLine(
    label: String,
    value: String,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
) {
    SheetControlRow(label = label) {
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun cacheStatusTierLabel(tier: BaGuideStudentDetailFreshnessTier): String =
    stringResource(
        when (tier) {
            BaGuideStudentDetailFreshnessTier.HotUpdate -> R.string.guide_cache_status_tier_hot_update
            BaGuideStudentDetailFreshnessTier.Completion -> R.string.guide_cache_status_tier_completion
            BaGuideStudentDetailFreshnessTier.Stable -> R.string.guide_cache_status_tier_stable
            BaGuideStudentDetailFreshnessTier.LongTerm -> R.string.guide_cache_status_tier_long_term
            BaGuideStudentDetailFreshnessTier.Archived -> R.string.guide_cache_status_tier_archived
            BaGuideStudentDetailFreshnessTier.Unknown -> R.string.guide_cache_status_tier_unknown
        },
    )

@Composable
private fun cacheStatusAccent(cacheStatus: BaStudentGuideCacheStatusUiState): Color =
    when {
        cacheStatus.failureCount > 0 -> MiuixTheme.colorScheme.error
        cacheStatus.validatingInBackground -> AppStatusColors.Refreshing
        cacheStatus.freshnessTier == BaGuideStudentDetailFreshnessTier.HotUpdate -> AppStatusColors.Refreshing
        cacheStatus.freshnessTier == BaGuideStudentDetailFreshnessTier.Archived -> AppStatusColors.Cached
        cacheStatus.hasStatus -> AppStatusColors.Fresh
        else -> MiuixTheme.colorScheme.onBackgroundVariant
    }

@Composable
private fun cacheStatusTimeLabel(
    timestampMs: Long,
    nowMs: Long,
): String {
    if (timestampMs <= 0L) return stringResource(R.string.guide_cache_status_time_unknown)
    val absolute =
        remember(timestampMs) {
            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestampMs))
        }
    return stringResource(
        R.string.guide_cache_status_time_absolute_relative,
        absolute,
        cacheStatusRelativeTime(timestampMs, nowMs),
    )
}

@Composable
private fun cacheStatusRelativeTime(
    timestampMs: Long,
    nowMs: Long,
): String {
    val diffMs = timestampMs - nowMs
    val absoluteMs = kotlin.math.abs(diffMs)
    val minutes = (absoluteMs / 60_000L).coerceAtLeast(0L)
    if (minutes < 1L) return stringResource(R.string.guide_cache_status_relative_just_now)
    val hours = minutes / 60L
    val days = hours / 24L
    val value =
        when {
            days > 0L -> days
            hours > 0L -> hours
            else -> minutes
        }
    val resId =
        when {
            diffMs >= 0L && days > 0L -> R.string.guide_cache_status_relative_days_later
            diffMs >= 0L && hours > 0L -> R.string.guide_cache_status_relative_hours_later
            diffMs >= 0L -> R.string.guide_cache_status_relative_minutes_later
            days > 0L -> R.string.guide_cache_status_relative_days_ago
            hours > 0L -> R.string.guide_cache_status_relative_hours_ago
            else -> R.string.guide_cache_status_relative_minutes_ago
        }
    return stringResource(resId, value)
}
