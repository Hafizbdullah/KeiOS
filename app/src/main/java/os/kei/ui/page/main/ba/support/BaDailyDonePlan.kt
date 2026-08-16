package os.kei.ui.page.main.ba.support

/**
 * How many Generate slots the daily template loads.
 *
 * Two, not three. Sensei's habitual daily opens the first two and leaves the third for whatever the
 * day actually calls for.
 */
internal const val BA_DAILY_DONE_CRAFT_SLOTS = 2

/**
 * The grade the template assumes for each loaded craft.
 *
 * [BaCraftGrade.High] is 3h for a single node, which is what an advanced material or a gift comes out
 * as — the outcome a one-node craft is actually run for, and the best value per keystone. Opening more
 * nodes multiplies the cost far faster than the reward, so the template deliberately stops at one node
 * per slot rather than guessing at a bigger craft the teacher did not ask for.
 */
internal val BA_DAILY_DONE_CRAFT_GRADE = BaCraftGrade.High

/** What a daily-done application actually changed, so the caller can report it honestly. */
internal data class BaDailyDoneOutcome(
    val apCleared: Boolean = false,
    val cafeApCleared: Boolean = false,
    val headpatStarted: Boolean = false,
    val invite1Started: Boolean = false,
    val invite2Started: Boolean = false,
    val craftSlotsStarted: Int = 0,
) {
    val changedAnything: Boolean
        get() = apCleared ||
            cafeApCleared ||
            headpatStarted ||
            invite1Started ||
            invite2Started ||
            craftSlotsStarted > 0
}

/** The new per-account values a daily-done application would write. */
internal data class BaDailyDonePlan(
    val apCurrent: Double,
    val apRegenBaseMs: Long,
    val apSyncMs: Long,
    val apLastNotifiedLevel: Int,
    val cafeStoredAp: Double,
    val cafeLastHourMs: Long,
    val cafeApLastNotifiedLevel: Int,
    val coffeeHeadpatMs: Long,
    val coffeeInvite1UsedMs: Long,
    val coffeeInvite2UsedMs: Long,
    val craft: BaCraftState,
    val outcome: BaDailyDoneOutcome,
)

/**
 * "I did my dailies" — the one-tap template, computed for a single account.
 *
 * Deliberately **idempotent on anything already spent**: only a cooldown that has actually elapsed is
 * restarted, and only a craft slot that is idle or already finished is loaded. Tapping twice in a row
 * therefore does not push a cooldown out or overwrite a craft still in flight, which is what makes it
 * safe to bind to a quick-settings tile where a stray tap costs nothing.
 *
 * Both AP pools go to **zero**, which is not the same as claiming the cafe. Claiming moves cafe AP into
 * the player pool; this template says the teacher already spent both, so each is zeroed independently
 * and each regeneration anchor is re-based to now so the next cycle starts from this moment.
 *
 * The notified levels reset alongside, matching what the in-app cafe claim already does: a reminder
 * that fired at yesterday's level must not dedupe away the next one.
 */
internal fun planBaDailyDone(
    snapshot: BaPageSnapshot,
    nowMs: Long = System.currentTimeMillis(),
): BaDailyDonePlan {
    val apCleared = snapshot.apCurrent > 0.0
    val cafeApCleared = snapshot.cafeStoredAp > 0.0

    val headpatAt = consumeBaHeadpatIfReady(snapshot.coffeeHeadpatMs, snapshot.serverIndex, nowMs)
    val invite1At = consumeBaInviteTicketIfReady(snapshot.coffeeInvite1UsedMs, nowMs)
    val invite2At = consumeBaInviteTicketIfReady(snapshot.coffeeInvite2UsedMs, nowMs)

    var craft = snapshot.craft.normalized()
    var craftStarted = 0
    repeat(BA_DAILY_DONE_CRAFT_SLOTS) { index ->
        val slot = craft.slotAt(BaCraftFunction.Generate, index)
        // Free means idle OR already finished: a completed slot has been collected, so reusing it is
        // exactly what the teacher would do next. A slot still counting down is left alone.
        val free = !slot.isActive() || slot.isComplete(nowMs)
        if (free) {
            craft =
                craft.withSlotAt(
                    function = BaCraftFunction.Generate,
                    index = index,
                    slot =
                        BaCraftSlot(
                            startedAtMs = nowMs,
                            grades = listOf(BA_DAILY_DONE_CRAFT_GRADE),
                        ),
                )
            craftStarted++
        }
    }

    return BaDailyDonePlan(
        apCurrent = 0.0,
        apRegenBaseMs = nowMs,
        apSyncMs = nowMs,
        apLastNotifiedLevel = -1,
        cafeStoredAp = 0.0,
        cafeLastHourMs = floorToHourMs(nowMs),
        cafeApLastNotifiedLevel = -1,
        coffeeHeadpatMs = headpatAt ?: snapshot.coffeeHeadpatMs,
        coffeeInvite1UsedMs = invite1At ?: snapshot.coffeeInvite1UsedMs,
        coffeeInvite2UsedMs = invite2At ?: snapshot.coffeeInvite2UsedMs,
        craft = craft,
        outcome =
            BaDailyDoneOutcome(
                apCleared = apCleared,
                cafeApCleared = cafeApCleared,
                headpatStarted = headpatAt != null,
                invite1Started = invite1At != null,
                invite2Started = invite2At != null,
                craftSlotsStarted = craftStarted,
            ),
    )
}

/**
 * `nowMs` when the headpat cooldown has elapsed, else `null`.
 *
 * Mirrors the in-app `consumeBaHeadpat` rule, including its server dependence: the headpat also frees up
 * at the cafe's student refresh, so the earlier of cooldown-end and refresh wins.
 */
private fun consumeBaHeadpatIfReady(
    coffeeHeadpatMs: Long,
    serverIndex: Int,
    nowMs: Long,
): Long? {
    if (coffeeHeadpatMs <= 0L) return nowMs
    return nowMs.takeIf { calculateNextHeadpatAvailableMs(coffeeHeadpatMs, serverIndex) <= nowMs }
}

private fun consumeBaInviteTicketIfReady(
    usedMs: Long,
    nowMs: Long,
): Long? {
    if (usedMs <= 0L) return nowMs
    return nowMs.takeIf { calculateInviteTicketAvailableMs(usedMs) <= nowMs }
}
