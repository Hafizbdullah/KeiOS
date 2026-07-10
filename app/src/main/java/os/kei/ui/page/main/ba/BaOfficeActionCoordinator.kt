package os.kei.ui.page.main.ba

import android.content.Context
import androidx.compose.ui.unit.IntRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import os.kei.core.background.AppBackgroundScheduler
import os.kei.ui.page.main.ba.support.BA_AP_MAX
import os.kei.ui.page.main.ba.support.BaAccountId

internal suspend fun persistBaApMutationAndReschedule(
    persist: suspend () -> Unit,
    schedule: () -> Unit,
) {
    persist()
    schedule()
}

internal class BaOfficeActionCoordinator(
    private val context: Context,
    private val office: BaOfficeController,
    private val scope: CoroutineScope,
    private val serverIndexProvider: () -> Int,
    private val accountIdProvider: () -> BaAccountId?,
    private val onSettingsCafeLevelChange: (Int) -> Unit,
    private val onCafeLevelPopupAnchorBoundsChange: (IntRect?) -> Unit,
    private val onCafeLevelPopupChange: (Boolean) -> Unit,
    private val onOpenApLimitTools: () -> Unit,
    private val onOpenCafeApTools: () -> Unit,
    private val onOpenCafeCooldownEditSheet: (BaCafeCooldownEditTarget) -> Unit,
    private val onAccountSelected: (BaAccountId) -> Unit,
    private val onEditAccount: (BaAccountId) -> Unit,
    private val onRefreshCalendar: () -> Unit,
    private val onRefreshPool: () -> Unit,
    private val onOpenCalendarLink: (String) -> Unit,
    private val onOpenPoolStudentGuide: (String) -> Unit,
    private val persistRuntimeUpdate: suspend (BaRuntimePersistenceUpdate) -> Unit =
        { update -> update.persistAsync() },
    private val scheduleBaApThreshold: () -> Unit =
        { AppBackgroundScheduler.scheduleBaApThreshold(context) },
) {
    fun buildContentActions(): BaPageContentActions =
        BaPageContentActions(
            onApCurrentInputChange = { office.apCurrentInput = it },
            onApCurrentDone = ::saveApCurrentInput,
            onOpenApLimitTools = onOpenApLimitTools,
            onCafeStoredApInputChange = { office.cafeStoredApInput = normalizeCafeStoredApInput(it) },
            onCafeStoredApDone = ::saveCafeStoredApInput,
            onOpenCafeApTools = onOpenCafeApTools,
            onCafeLevelPopupAnchorBoundsChange = onCafeLevelPopupAnchorBoundsChange,
            onCafeLevelPopupChange = onCafeLevelPopupChange,
            onAccountSelected = onAccountSelected,
            onEditAccount = onEditAccount,
            onCafeLevelChange = ::selectCafeLevel,
            onClaimCafeStoredAp = ::claimCafeStoredAp,
            onTouchHead = { persistCooldown(office.touchHead(serverIndexProvider())) },
            onEditHeadpatCooldown = {
                onOpenCafeCooldownEditSheet(BaCafeCooldownEditTarget.Headpat)
            },
            onUseInviteTicket1 = { persistCooldown(office.useInviteTicket1()) },
            onEditInviteTicket1Cooldown = {
                onOpenCafeCooldownEditSheet(BaCafeCooldownEditTarget.InviteTicket1)
            },
            onUseInviteTicket2 = { persistCooldown(office.useInviteTicket2()) },
            onEditInviteTicket2Cooldown = {
                onOpenCafeCooldownEditSheet(BaCafeCooldownEditTarget.InviteTicket2)
            },
            onRefreshCalendar = onRefreshCalendar,
            onOpenCalendarLink = onOpenCalendarLink,
            onRefreshPool = onRefreshPool,
            onOpenPoolStudentGuide = onOpenPoolStudentGuide,
        )

    private fun saveApCurrentInput() {
        val finalValue = office.apCurrentInput.toIntOrNull()?.coerceIn(0, BA_AP_MAX) ?: 0
        persistRuntimeAndReschedule(office.updateCurrentAp(finalValue, markSync = true))
        office.apCurrentInput = finalValue.toString()
    }

    private fun saveCafeStoredApInput() {
        val finalValue = office.cafeStoredApInput.replace(',', '.').toDoubleOrNull() ?: 0.0
        persistRuntimeAndReschedule(office.updateCafeStoredAp(finalValue))
        office.cafeStoredApInput = office.displayCafeStoredApInputText()
    }

    private fun selectCafeLevel(level: Int) {
        val normalized = level.coerceIn(1, 10)
        val storageUpdate = office.applyCafeStorageUpdate()
        office.cafeLevel = normalized
        val clampUpdate = office.clampCafeStoredToCapUpdate()
        scope.launch {
            storageUpdate?.withCurrentAccount()?.persistAsync()
            BaOfficeRepository.saveCafeLevelAsync(normalized)
            clampUpdate?.withCurrentAccount()?.persistAsync()
        }
        onSettingsCafeLevelChange(normalized)
        onCafeLevelPopupChange(false)
    }

    private fun claimCafeStoredAp() {
        persistRuntimeAndReschedule(office.claimCafeStoredAp(context))
    }

    private fun persistRuntimeAndReschedule(update: BaRuntimePersistenceUpdate?) {
        if (update == null) return
        scope.launch {
            persistBaApMutationAndReschedule(
                persist = { persistRuntimeUpdate(update.withCurrentAccount()) },
                schedule = scheduleBaApThreshold,
            )
        }
    }

    private fun BaRuntimePersistenceUpdate.withCurrentAccount(): BaRuntimePersistenceUpdate =
        withAccountId(accountIdProvider())

    private fun persistCooldown(update: BaOfficeCooldownPersistenceUpdate?) {
        if (update == null) return
        scope.launch {
            update.persistAsync()
        }
    }
}

private fun normalizeCafeStoredApInput(input: String): String {
    var hasDecimalSeparator = false
    return buildString {
        for (char in input.trim().take(8)) {
            when {
                char.isDigit() -> append(char)
                (char == '.' || char == ',') && !hasDecimalSeparator -> {
                    append(char)
                    hasDecimalSeparator = true
                }
            }
        }
    }
}
