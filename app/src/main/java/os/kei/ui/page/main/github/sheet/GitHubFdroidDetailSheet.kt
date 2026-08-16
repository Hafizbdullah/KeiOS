@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.core.ext.showToast
import os.kei.feature.github.data.local.fdroid.FdroidMetadataSidecar
import os.kei.feature.github.data.local.fdroid.FdroidVersionMetadataSummary
import os.kei.feature.github.data.remote.fdroid.FdroidAntiFeatureSnapshot
import os.kei.feature.github.model.FdroidIndexFormat
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.FdroidTrackedAppConfig
import os.kei.feature.github.model.GITHUB_FDROID_DEFAULT_REFRESH_INTERVAL_HOURS
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.feature.github.model.buildFdroidRepositoryTrackIdentity
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.github.asset.formatAssetSize
import os.kei.ui.page.main.github.asset.formatReleaseUpdatedAtCompact
import os.kei.ui.page.main.github.page.GitHubFdroidDetailRequest
import os.kei.ui.page.main.os.appLucideCloseIcon
import os.kei.ui.page.main.os.appLucideExternalLinkIcon
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.os.osLucideCopyIcon
import os.kei.ui.page.main.widget.core.AppInfoRow
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.AppLiquidIconButton
import os.kei.ui.page.main.widget.glass.GlassVariant
import os.kei.ui.page.main.widget.sheet.SheetContentColumn
import os.kei.ui.page.main.widget.sheet.SheetDescriptionText
import os.kei.ui.page.main.widget.sheet.SheetSectionCard
import os.kei.ui.page.main.widget.sheet.SheetSectionHeader
import os.kei.ui.page.main.widget.sheet.SheetSummaryCard
import os.kei.ui.page.main.widget.sheet.SnapshotWindowBottomSheet
import os.kei.ui.page.main.widget.status.StatusPill
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

@Composable
internal fun GitHubFdroidDetailSheet(
    request: GitHubFdroidDetailRequest?,
    backdrop: Backdrop,
    onDismissRequest: () -> Unit,
    onRefresh: (GitHubTrackedApp) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
) {
    val detail = request ?: return
    val item = detail.item
    SnapshotWindowBottomSheet(
        show = true,
        preferExportedBackdrop = true,
        title = stringResource(R.string.github_fdroid_detail_title),
        onDismissRequest = onDismissRequest,
        startAction = {
            AppLiquidIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideCloseIcon(),
                contentDescription = stringResource(R.string.common_close),
                onClick = onDismissRequest,
            )
        },
        endAction = {
            AppLiquidIconButton(
                backdrop = backdrop,
                variant = GlassVariant.Bar,
                icon = appLucideRefreshIcon(),
                contentDescription = stringResource(R.string.common_refresh),
                onClick = { onRefresh(item) },
            )
        },
    ) {
        SheetContentColumn(verticalSpacing = 14.dp) {
            when {
                detail.loading -> {
                    FdroidLoadingDetail(item)
                }

                detail.sidecar == null -> {
                    FdroidMissingCacheDetail(
                        item = item,
                        backdrop = backdrop,
                        onOpenExternalUrl = onOpenExternalUrl,
                    )
                }

                else -> {
                    FdroidCachedDetail(
                        item = item,
                        sidecar = detail.sidecar,
                        backdrop = backdrop,
                        onOpenExternalUrl = onOpenExternalUrl,
                    )
                }
            }
        }
    }
}

@Composable
private fun FdroidLoadingDetail(item: GitHubTrackedApp) {
    SheetSummaryCard(
        title = item.fdroidDisplayTitle(),
        badgeLabel = stringResource(R.string.github_track_sheet_source_mode_fdroid),
        badgeColor = GitHubStatusPalette.Active,
    ) {
        GitHubDecisionDetailTextLine(stringResource(R.string.common_loading))
    }
}

@Composable
private fun FdroidMissingCacheDetail(
    item: GitHubTrackedApp,
    backdrop: Backdrop,
    onOpenExternalUrl: (String) -> Unit,
) {
    SheetSummaryCard(
        title = item.fdroidDisplayTitle(),
        badgeLabel = stringResource(R.string.github_track_sheet_source_mode_fdroid),
        badgeColor = GitHubStatusPalette.Cache,
    ) {
        GitHubDecisionDetailTextLine(
            text = stringResource(R.string.github_fdroid_detail_no_cache_title),
            accent = true,
        )
        GitHubDecisionDetailTextLine(
            text = stringResource(R.string.github_fdroid_detail_no_cache_summary),
            maxLines = 4,
        )
    }
    FdroidTrackingConfigSection(
        item = item,
        backdrop = backdrop,
        onOpenExternalUrl = onOpenExternalUrl,
    )
}

@Composable
private fun FdroidCachedDetail(
    item: GitHubTrackedApp,
    sidecar: FdroidMetadataSidecar,
    backdrop: Backdrop,
    onOpenExternalUrl: (String) -> Unit,
) {
    val context = LocalContext.current
    val selectedVersion = sidecar.selectedVersion
    val repoName =
        sidecar.repo.repoName
            .ifBlank {
                buildFdroidRepositoryTrackIdentity(item.repoUrl, item.packageName)
                    ?.repoDisplayName
                    .orEmpty()
            }
            .ifBlank { stringResource(R.string.github_track_sheet_source_mode_fdroid) }
    SheetSummaryCard(
        title =
            sidecar.packageInfo.appName
                .ifBlank { item.appLabel }
                .ifBlank { sidecar.packageInfo.packageName }
                .ifBlank { item.packageName },
        badgeLabel = repoName,
        badgeColor = GitHubStatusPalette.Update,
        titleMaxLines = 2,
    ) {
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_package),
            value = sidecar.packageInfo.packageName.ifBlank { item.packageName },
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_version),
            value = selectedVersion?.versionDisplayLabel().orEmpty(),
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_repo),
            value = repoName,
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_fetched_at),
            value =
                formatReleaseUpdatedAtCompact(sidecar.fetchedAtMillis)
                    ?: stringResource(R.string.common_unknown),
        )
    }

    FdroidTrackingConfigSection(
        item = item,
        backdrop = backdrop,
        onOpenExternalUrl = onOpenExternalUrl,
    )
    FdroidPackageSection(
        sidecar = sidecar,
        backdrop = backdrop,
        onOpenExternalUrl = onOpenExternalUrl,
    )
    FdroidVersionSection(
        sidecar = sidecar,
        selectedVersion = selectedVersion,
    )
    FdroidRepositorySection(
        sidecar = sidecar,
        backdrop = backdrop,
        onOpenExternalUrl = onOpenExternalUrl,
    )
    FdroidTrustSection(sidecar)
    FdroidAntiFeaturesSection(sidecar)
}

@Composable
private fun FdroidTrackingConfigSection(
    item: GitHubTrackedApp,
    backdrop: Backdrop,
    onOpenExternalUrl: (String) -> Unit,
) {
    val identity = buildFdroidRepositoryTrackIdentity(item.repoUrl, item.packageName)
    val config = item.fdroidConfig
    val sourceScope = config.fdroidTrackingSourceScopeLabel(
        item = item,
        matchedRepoName =
            FdroidRepositoryPresets.presetForRepoUrl(item.repoUrl)?.displayName
                ?: identity?.repoDisplayName
                ?: item.repoUrl,
    )
    val packagePageUrl =
        config.packagePageUrl
            .ifBlank { identity?.packagePageUrl.orEmpty() }
    SheetSectionHeader(stringResource(R.string.github_fdroid_detail_section_tracking))
    SheetSectionCard(verticalSpacing = 6.dp) {
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_source_scope),
            value = sourceScope,
        )
        FdroidLinkRow(
            label = stringResource(R.string.github_fdroid_detail_label_package_page),
            url = packagePageUrl,
            backdrop = backdrop,
            onOpenExternalUrl = onOpenExternalUrl,
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_update_interval),
            value =
                updateIntervalModeLabel(
                    mode = item.updateIntervalMode,
                    globalRefreshIntervalHours = GITHUB_FDROID_DEFAULT_REFRESH_INTERVAL_HOURS,
                    sourceMode = GitHubTrackedSourceMode.FdroidRepository,
                ),
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_version_selection),
            value = fdroidVersionSelectionModeLabel(config.selectionMode),
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_version_regex),
            value = config.versionNameRegex.ifBlank { stringResource(R.string.common_not_used) },
            valueMaxLines = 2,
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_apk_regex),
            value = config.apkNameRegex.ifBlank { stringResource(R.string.common_not_used) },
            valueMaxLines = 2,
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_anti_feature_policy),
            value = fdroidAntiFeaturePolicyLabel(config.antiFeaturePolicy),
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_blocked_anti_features),
            value =
                config.blockedAntiFeatures
                    .joinToString(", ")
                    .ifBlank { stringResource(R.string.common_not_used) },
            valueMaxLines = 3,
        )
        SheetDescriptionText(stringResource(R.string.github_fdroid_detail_value_sync_schema))
    }
}

@Composable
private fun FdroidTrackedAppConfig.fdroidTrackingSourceScopeLabel(
    item: GitHubTrackedApp,
    matchedRepoName: String,
): String {
    val fallbackRepoName =
        matchedRepoName.ifBlank { item.repoUrl }.ifBlank { stringResource(R.string.common_unknown) }
    return when (repoPresetId.trim().lowercase(Locale.ROOT)) {
        FdroidRepositoryPresets.COMMON_ID ->
            stringResource(
                R.string.github_fdroid_detail_value_source_scope_common_format,
                fallbackRepoName,
            )

        FdroidRepositoryPresets.CUSTOM_ID ->
            stringResource(
                R.string.github_fdroid_detail_value_source_scope_custom_format,
                fallbackRepoName,
            )

        else ->
            FdroidRepositoryPresets.presetForId(repoPresetId)?.displayName ?: fallbackRepoName
    }
}

@Composable
private fun FdroidPackageSection(
    sidecar: FdroidMetadataSidecar,
    backdrop: Backdrop,
    onOpenExternalUrl: (String) -> Unit,
) {
    val packageInfo = sidecar.packageInfo
    val hasPackageDetails =
        packageInfo.summary.isNotBlank() ||
            packageInfo.description.isNotBlank() ||
            packageInfo.license.isNotBlank() ||
            packageInfo.categories.isNotEmpty() ||
            packageInfo.sourceCodeUrl.isNotBlank() ||
            packageInfo.webSiteUrl.isNotBlank() ||
            packageInfo.issueTrackerUrl.isNotBlank() ||
            packageInfo.changelogUrl.isNotBlank()
    SheetSectionHeader(stringResource(R.string.github_fdroid_detail_section_package))
    SheetSectionCard(verticalSpacing = 6.dp) {
        if (hasPackageDetails) {
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_summary),
                value = packageInfo.summary,
                valueMaxLines = 3,
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_description),
                value = packageInfo.description,
                valueMaxLines = 4,
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_license),
                value = packageInfo.license,
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_categories),
                value = packageInfo.categories.joinToString(", "),
                valueMaxLines = 2,
            )
            FdroidLinkRow(
                label = stringResource(R.string.github_fdroid_detail_label_source_code),
                url = packageInfo.sourceCodeUrl,
                backdrop = backdrop,
                onOpenExternalUrl = onOpenExternalUrl,
            )
            FdroidLinkRow(
                label = stringResource(R.string.github_fdroid_detail_label_website),
                url = packageInfo.webSiteUrl,
                backdrop = backdrop,
                onOpenExternalUrl = onOpenExternalUrl,
            )
            FdroidLinkRow(
                label = stringResource(R.string.github_fdroid_detail_label_issue_tracker),
                url = packageInfo.issueTrackerUrl,
                backdrop = backdrop,
                onOpenExternalUrl = onOpenExternalUrl,
            )
            FdroidLinkRow(
                label = stringResource(R.string.github_fdroid_detail_label_changelog),
                url = packageInfo.changelogUrl,
                backdrop = backdrop,
                onOpenExternalUrl = onOpenExternalUrl,
            )
        } else {
            SheetDescriptionText(stringResource(R.string.github_fdroid_detail_package_empty))
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FdroidVersionSection(
    sidecar: FdroidMetadataSidecar,
    selectedVersion: FdroidVersionMetadataSummary?,
) {
    val context = LocalContext.current
    SheetSectionHeader(stringResource(R.string.github_fdroid_detail_section_version))
    SheetSectionCard(verticalSpacing = 6.dp) {
        if (selectedVersion == null) {
            SheetDescriptionText(stringResource(R.string.github_fdroid_detail_version_empty))
        } else {
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_version_name),
                value = selectedVersion.versionName,
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_version_code),
                value = selectedVersion.versionCode.takeIf { it > 0L }?.toString().orEmpty(),
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_apk),
                value = selectedVersion.apkName.ifBlank { selectedVersion.apkPath },
                valueMaxLines = 2,
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_size),
                value = formatAssetSize(selectedVersion.apkSizeBytes, context),
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_added_at),
                value = formatReleaseUpdatedAtCompact(selectedVersion.addedAtMillis).orEmpty(),
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_sdk),
                value =
                    stringResource(
                        R.string.github_fdroid_detail_value_sdk,
                        selectedVersion.minSdk?.toString() ?: "-",
                        selectedVersion.targetSdk?.toString() ?: "-",
                    ),
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_native_abis),
                value = selectedVersion.nativeAbis.joinToString(", "),
                valueMaxLines = 2,
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_channels),
                value = selectedVersion.releaseChannels.joinToString(", "),
                valueMaxLines = 2,
            )
            FdroidInfoRow(
                label = stringResource(R.string.github_fdroid_detail_label_release_notes),
                value = selectedVersion.whatsNew,
                valueMaxLines = 5,
            )
        }
        if (sidecar.candidateVersions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sidecar.candidateVersions.take(6).forEach { version ->
                    StatusPill(
                        label = version.versionDisplayLabel(),
                        color =
                            if (selectedVersion?.versionCode == version.versionCode) {
                                GitHubStatusPalette.Update
                            } else {
                                GitHubStatusPalette.Active
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun FdroidRepositorySection(
    sidecar: FdroidMetadataSidecar,
    backdrop: Backdrop,
    onOpenExternalUrl: (String) -> Unit,
) {
    SheetSectionHeader(stringResource(R.string.github_fdroid_detail_section_repository))
    SheetSectionCard(verticalSpacing = 6.dp) {
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_repo_name),
            value = sidecar.repo.repoName,
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_repo_description),
            value = sidecar.repo.repoDescription,
            valueMaxLines = 3,
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_format),
            value = fdroidIndexFormatLabel(sidecar.repo.format),
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_package_count),
            value = sidecar.repo.packageCount.takeIf { it > 0 }?.toString().orEmpty(),
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_repo_timestamp),
            value = formatReleaseUpdatedAtCompact(sidecar.repo.timestampMillis).orEmpty(),
        )
        FdroidLinkRow(
            label = stringResource(R.string.github_fdroid_detail_label_repo_url),
            url = sidecar.repo.repoUrl,
            backdrop = backdrop,
            onOpenExternalUrl = onOpenExternalUrl,
        )
        sidecar.repo.mirrors.take(3).forEachIndexed { index, mirror ->
            FdroidLinkRow(
                label =
                    stringResource(
                        R.string.github_fdroid_detail_label_mirror_format,
                        index + 1,
                    ),
                url = mirror,
                backdrop = backdrop,
                onOpenExternalUrl = onOpenExternalUrl,
            )
        }
    }
}

@Composable
private fun FdroidTrustSection(sidecar: FdroidMetadataSidecar) {
    SheetSectionHeader(stringResource(R.string.github_fdroid_detail_section_trust))
    SheetSectionCard(verticalSpacing = 6.dp) {
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_trust_policy),
            value = fdroidTrustPolicyLabel(sidecar.trust.trustPolicy),
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_repo_fingerprint),
            value = sidecar.trust.repoFingerprint,
            valueMaxLines = 3,
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_apk_sha256),
            value = sidecar.trust.apkSha256,
            valueMaxLines = 3,
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_signer_sha256),
            value = sidecar.trust.signerSha256.joinToString("\n"),
            valueMaxLines = 4,
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_hash_status),
            value =
                stringResource(
                    if (sidecar.trust.hashAvailable) {
                        R.string.common_available
                    } else {
                        R.string.common_unavailable
                    },
                ),
        )
        FdroidInfoRow(
            label = stringResource(R.string.github_fdroid_detail_label_signer_status),
            value =
                stringResource(
                    if (sidecar.trust.signerAvailable) {
                        R.string.common_available
                    } else {
                        R.string.common_unavailable
                    },
                ),
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FdroidAntiFeaturesSection(sidecar: FdroidMetadataSidecar) {
    SheetSectionHeader(stringResource(R.string.github_fdroid_detail_section_anti_features))
    SheetSectionCard(verticalSpacing = 6.dp) {
        if (sidecar.antiFeatures.isEmpty()) {
            SheetDescriptionText(stringResource(R.string.github_fdroid_detail_antifeatures_empty))
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sidecar.antiFeatures.forEach { antiFeature ->
                    StatusPill(
                        label = antiFeature.displayLabelText(),
                        color = antiFeature.statusColor(),
                    )
                }
            }
            sidecar.antiFeatures
                .filter { it.description.isNotBlank() }
                .take(4)
                .forEach { antiFeature ->
                    GitHubDecisionDetailTextLine(
                        text = "${antiFeature.displayLabelText()}: ${antiFeature.description}",
                        maxLines = 3,
                    )
                }
        }
    }
}

@Composable
private fun FdroidInfoRow(
    label: String,
    value: String,
    valueMaxLines: Int = 2,
) {
    if (value.isBlank()) return
    AppInfoRow(
        label = label,
        value = value,
        labelWeight = 0.36f,
        valueWeight = 0.64f,
        horizontalSpacing = 8.dp,
        rowVerticalPadding = 0.dp,
        verticalAlignment = Alignment.Top,
        valueTextAlign = TextAlign.Start,
        labelMaxLines = 1,
        valueMaxLines = valueMaxLines.coerceAtLeast(1),
        labelOverflow = TextOverflow.Ellipsis,
        valueOverflow = TextOverflow.Ellipsis,
        labelFontSize = AppTypographyTokens.Supporting.fontSize,
        labelLineHeight = AppTypographyTokens.Supporting.lineHeight,
        valueFontSize = AppTypographyTokens.Supporting.fontSize,
        valueLineHeight = AppTypographyTokens.Supporting.lineHeight,
        emphasizedValue = false,
    )
}

@Composable
private fun FdroidLinkRow(
    label: String,
    url: String,
    backdrop: Backdrop,
    onOpenExternalUrl: (String) -> Unit,
) {
    if (url.isBlank()) return
    val context = LocalContext.current
    val copiedToast = stringResource(R.string.github_fdroid_detail_toast_link_copied)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.34f),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        GitHubDecisionDetailTextLine(
            text = url,
            maxLines = 2,
            modifier = Modifier.weight(0.54f),
        )
        AppLiquidIconButton(
            backdrop = backdrop,
            icon = osLucideCopyIcon(),
            contentDescription = stringResource(R.string.common_copy),
            width = 34.dp,
            height = 34.dp,
            variant = GlassVariant.Content,
            onClick = {
                copyTextToClipboard(context, label, url)
                context.showToast(copiedToast)
            },
        )
        AppLiquidIconButton(
            backdrop = backdrop,
            icon = appLucideExternalLinkIcon(),
            contentDescription = stringResource(R.string.common_open),
            width = 34.dp,
            height = 34.dp,
            variant = GlassVariant.Content,
            onClick = { onOpenExternalUrl(url) },
        )
    }
}

private fun GitHubTrackedApp.fdroidDisplayTitle(): String =
    appLabel.ifBlank { packageName }.ifBlank { repoUrl }

private fun FdroidVersionMetadataSummary.versionDisplayLabel(): String =
    buildList {
        versionName.takeIf { it.isNotBlank() }?.let(::add)
        versionCode.takeIf { it > 0L }?.let { add(it.toString()) }
    }.joinToString(" / ")

@Composable
private fun FdroidAntiFeatureSnapshot.displayLabelText(): String =
    label.ifBlank { id }.ifBlank { stringResource(R.string.github_fdroid_detail_antifeature_fallback) }

private fun FdroidAntiFeatureSnapshot.statusColor(): Color {
    val normalized = id.lowercase(Locale.ROOT)
    return when {
        "vuln" in normalized ||
            "disabledalgorithm" in normalized ||
            "nosourcesince" in normalized -> GitHubStatusPalette.Error

        "tracking" in normalized ||
            "nonfree" in normalized -> GitHubStatusPalette.PreRelease

        else -> GitHubStatusPalette.Cache
    }
}

@Composable
private fun fdroidIndexFormatLabel(format: FdroidIndexFormat): String =
    when (format) {
        FdroidIndexFormat.V2 -> stringResource(R.string.github_fdroid_detail_format_v2)
        FdroidIndexFormat.V1 -> stringResource(R.string.github_fdroid_detail_format_v1)
        FdroidIndexFormat.PackageApi -> stringResource(R.string.github_fdroid_detail_format_package_api)
        FdroidIndexFormat.Unknown -> stringResource(R.string.common_unknown)
    }
