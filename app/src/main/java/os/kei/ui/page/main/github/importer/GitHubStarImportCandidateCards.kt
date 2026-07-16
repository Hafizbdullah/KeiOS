package os.kei.ui.page.main.github.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import os.kei.R
import os.kei.feature.github.domain.GitHubStarImportClassifier
import os.kei.feature.github.model.GitHubRepositoryImportCandidate
import os.kei.feature.github.model.GitHubStarImportApkVerificationStatus
import os.kei.feature.github.model.GitHubStarImportQuality
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.core.AppStatusPillSize
import os.kei.ui.page.main.widget.core.AppSurfaceCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val StarImportCandidateStatusPillMaxWidth = 112.dp
private val StarImportCandidateMetadataPillMaxWidth = 136.dp
private val StarImportCandidatePackagePillMaxWidth = 180.dp

@Immutable
internal data class StarImportCandidateColors(
    val containerColor: Color,
    val borderColor: Color,
    val titleColor: Color,
)

@Composable
internal fun starImportCandidateColors(
    selected: Boolean,
    disabled: Boolean,
    quality: GitHubStarImportQuality,
    isDark: Boolean,
): StarImportCandidateColors {
    val accent =
        when {
            disabled -> MiuixTheme.colorScheme.onBackgroundVariant
            selected -> GitHubStatusPalette.Update
            quality == GitHubStarImportQuality.LikelyAndroid -> GitHubStatusPalette.Active
            quality == GitHubStarImportQuality.OtherPlatform ->
                MiuixTheme.colorScheme.onBackgroundVariant
            else -> MiuixTheme.colorScheme.primary
        }
    val emphasized = selected || (!disabled && quality == GitHubStarImportQuality.LikelyAndroid)
    return StarImportCandidateColors(
        containerColor = accent.copy(alpha = if (isDark) 0.08f else 0.10f),
        borderColor = accent.copy(alpha = if (emphasized) 0.34f else 0.18f),
        titleColor = MiuixTheme.colorScheme.onBackground,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StarImportCandidateCard(
    candidate: GitHubRepositoryImportCandidate,
    selected: Boolean,
    trackedSelectable: Boolean,
    apkVerificationState: StarImportApkVerificationUiState?,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    val disabled = candidate.alreadyTracked && !trackedSelectable
    val quality = GitHubStarImportClassifier.classify(candidate)
    val colors =
        starImportCandidateColors(
            selected = selected,
            disabled = disabled,
            quality = quality,
            isDark = isAppInDarkTheme(),
        )
    AppSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = colors.containerColor,
        borderColor = colors.borderColor,
        borderWidth = 0.8.dp,
        enabled = !disabled,
        pressSafePadding = 2.dp,
        onClick = onToggle,
        role = Role.Checkbox,
        toggleableState = ToggleableState(selected),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = candidate.repository.fullName,
                        color = colors.titleColor,
                        fontSize = AppTypographyTokens.Body.fontSize,
                        lineHeight = AppTypographyTokens.Body.lineHeight,
                        fontWeight = AppTypographyTokens.Body.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = candidate.repository.description.ifBlank {
                            stringResource(R.string.github_star_import_candidate_no_description)
                        },
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = AppTypographyTokens.Caption.fontSize,
                        lineHeight = AppTypographyTokens.Caption.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StarImportCandidatePill(
                    label = when {
                        disabled -> stringResource(R.string.github_star_import_candidate_tracked)
                        selected -> stringResource(R.string.github_star_import_candidate_selected)
                        else -> stringResource(R.string.github_star_import_candidate_optional)
                    },
                    color = when {
                        disabled -> MiuixTheme.colorScheme.onBackgroundVariant
                        selected -> GitHubStatusPalette.Update
                        else -> GitHubStatusPalette.Active
                    },
                    maxWidth = StarImportCandidateStatusPillMaxWidth,
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StarImportCandidatePill(
                    label = stringResource(quality.labelRes()),
                    color = starImportQualityColor(quality),
                )
                if (candidate.repository.starCount > 0) {
                    StarImportCandidatePill(
                        label = stringResource(
                            R.string.github_star_import_candidate_stars_pill,
                            candidate.repository.starCount.formatStarCount(),
                        ),
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
                when {
                    candidate.repository.archived ->
                        StarImportCandidatePill(
                            label = stringResource(R.string.github_star_import_candidate_archived_pill),
                            color = GitHubStatusPalette.Error,
                        )

                    candidate.repository.fork ->
                        StarImportCandidatePill(
                            label = stringResource(R.string.github_star_import_candidate_fork_pill),
                            color = GitHubStatusPalette.PreRelease,
                        )
                }
                candidate.repository.language.takeIf { it.isNotBlank() }?.let { language ->
                    StarImportCandidatePill(
                        label = language,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
                StarImportApkVerificationPill(state = apkVerificationState)
                candidate.starImportPackageName(apkVerificationState)?.let { packageName ->
                    StarImportCandidatePill(
                        label = packageName,
                        color = GitHubStatusPalette.Active,
                        maxWidth = StarImportCandidatePackagePillMaxWidth,
                    )
                }
            }
        }
    }
}

@Composable
private fun StarImportApkVerificationPill(state: StarImportApkVerificationUiState?) {
    val verification = state?.verification
    val label = when {
        state?.checking == true -> stringResource(R.string.github_star_import_apk_pill_checking)
        verification == null -> stringResource(R.string.github_star_import_apk_pill_unchecked)
        verification.status == GitHubStarImportApkVerificationStatus.HasApk ->
            stringResource(R.string.github_star_import_apk_pill_count, verification.apkAssetCount)

        verification.status == GitHubStarImportApkVerificationStatus.NoApk ->
            stringResource(R.string.github_star_import_apk_pill_none)

        else -> stringResource(R.string.github_star_import_apk_pill_failed)
    }
    val color = when {
        state?.checking == true -> GitHubStatusPalette.Active
        verification?.status == GitHubStarImportApkVerificationStatus.HasApk -> GitHubStatusPalette.Update
        verification?.status == GitHubStarImportApkVerificationStatus.Failed -> GitHubStatusPalette.Error
        else -> MiuixTheme.colorScheme.onBackgroundVariant
    }
    StarImportCandidatePill(
        label = label,
        color = color,
    )
}

@Composable
private fun StarImportCandidatePill(
    label: String,
    color: Color,
    maxWidth: Dp = StarImportCandidateMetadataPillMaxWidth,
) {
    StatusPill(
        label = label,
        color = color,
        modifier = Modifier.widthIn(max = maxWidth),
        size = AppStatusPillSize.Compact,
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun starImportQualityColor(quality: GitHubStarImportQuality): Color {
    return when (quality) {
        GitHubStarImportQuality.LikelyAndroid -> GitHubStatusPalette.Update
        GitHubStarImportQuality.NeedsReview -> GitHubStatusPalette.Active
        GitHubStarImportQuality.OtherPlatform -> MiuixTheme.colorScheme.onBackgroundVariant
        GitHubStarImportQuality.ArchivedOrFork -> GitHubStatusPalette.Error
    }
}

private fun GitHubRepositoryImportCandidate.starImportPackageName(
    state: StarImportApkVerificationUiState?
): String? {
    val verifiedPackage = state
        ?.verification
        ?.takeIf { it.status == GitHubStarImportApkVerificationStatus.HasApk }
        ?.packageName
        ?.trim()
        .orEmpty()
    return trackedApp.packageName.trim()
        .ifBlank { verifiedPackage }
        .takeIf { it.isNotBlank() }
}
