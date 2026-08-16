package os.kei.ui.page.main.ba.support

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Craft Chamber's two functions, each with its own independent [BA_CRAFT_SLOT_COUNT] slots.
 *
 * They are not two mechanics but two shapes of the same one — see [BaCraftSlot.computedDurationMs].
 */
@Serializable
internal enum class BaCraftFunction {
    /** 製造 / ジェネレート — node crafting, one item per unlocked node. */
    @SerialName("generate")
    Generate,

    /** 物質合成 / テイラーメイド — recipe crafting, 1..5 copies of one recipe. */
    @SerialName("fusion")
    Fusion,
}

/**
 * Per-item craft duration, which the game derives purely from the produced item's grade.
 *
 * Confirmed against namu.wiki's Craft Chamber page — *「등급이 높을수록 제작시간이 길어진다.
 * (하급: 30분, 일반: 1시간 30분, 상급: 3시간, 최상급: 6시간)」* — and cross-checked against the
 * game8 and kamigame tables. The same ladder drives both functions.
 *
 * Deliberately NOT keyed on how many nodes a craft opened: the node count multiplies how many items
 * come out, and each item then contributes its own grade's duration. That is the whole reason
 * [BaCraftSlot.grades] is a list rather than a single grade plus a count.
 */
@Serializable
internal enum class BaCraftGrade(val durationMs: Long) {
    /** 下級 / 하급 */
    @SerialName("low")
    Low(30L * 60L * 1000L),

    /** 一般 (中級) / 일반 */
    @SerialName("normal")
    Normal(90L * 60L * 1000L),

    /** 上級 / 상급 */
    @SerialName("high")
    High(180L * 60L * 1000L),

    /** 最上級 / 최상급 */
    @SerialName("highest")
    Highest(360L * 60L * 1000L),
}

/** Both functions expose three slots, and the two sets are separate — six timers per account. */
internal const val BA_CRAFT_SLOT_COUNT = 3

/**
 * A Generate slot crafts one item per unlocked node ("解"), up to three.
 *
 * namu.wiki: *「3차 노드까지 전부 개방하면 1차 노드, 2차 노드, 3차 노드 합해 총 3개의 아이템을
 * 제작하며, 제작 시간도 3개 분량을, 제조 부스터 티켓도 3개를 사용한다」* — three items, three items'
 * worth of time, three booster tickets.
 */
internal const val BA_CRAFT_GENERATE_MAX_ENTRIES = 3

/**
 * A Fusion slot crafts 1..5 copies of one recipe, and the cap is 5 at **every** grade.
 *
 * Verified in-game rather than from a guide: the per-slot maximum does not vary by rarity.
 */
internal const val BA_CRAFT_FUSION_MAX_ENTRIES = 5

/**
 * Ceiling for a hand-entered override.
 *
 * The longest reachable real craft is a Fusion slot at 5 × [BaCraftGrade.Highest] = 30h. The bound is
 * loosened to 48h so a legitimate edge case (a booster ticket spent partway, a clock correction) still
 * fits, while a corrupt value cannot schedule an alarm months out.
 */
internal const val BA_CRAFT_MAX_DURATION_MS = 48L * 60L * 60L * 1000L

/** Trimmed length of a slot's free-text note, matching the account display-name bound. */
private const val BA_CRAFT_LABEL_MAX_LENGTH = 24

/**
 * One loaded slot.
 *
 * [startedAtMs] is a wall-clock anchor, never a countdown: the same choice the AP and cafe timers
 * already make, so a killed process, a backgrounded app or a skipped frame cannot make it drift.
 *
 * A Fusion slot stores its grade repeated [grades].size times rather than a grade plus a count. The
 * redundancy buys a duration formula with no branches, and [normalized] collapses any mixed list back
 * to a single grade so the invariant cannot rot.
 */
@Serializable
internal data class BaCraftSlot(
    val startedAtMs: Long = 0L,
    val grades: List<BaCraftGrade> = emptyList(),
    val customDurationMs: Long = 0L,
    val label: String = "",
)

/** Every slot of one account, per function. Each account owns its own — nothing here is shared. */
@Serializable
internal data class BaCraftState(
    val generate: List<BaCraftSlot> = emptyList(),
    val fusion: List<BaCraftSlot> = emptyList(),
)

internal fun BaCraftFunction.maxEntries(): Int =
    when (this) {
        BaCraftFunction.Generate -> BA_CRAFT_GENERATE_MAX_ENTRIES
        BaCraftFunction.Fusion -> BA_CRAFT_FUSION_MAX_ENTRIES
    }

/**
 * The one formula both functions share: sum the grade of every item the slot will produce.
 *
 * Generate sums 1..3 freely-mixed grades; Fusion sums one grade repeated 1..5 times. The difference
 * between the two mechanics is a validation rule ([normalized]), not a second code path.
 */
internal fun BaCraftSlot.computedDurationMs(): Long = grades.sumOf { it.durationMs }

/** A hand-entered duration wins, because the game only ever shows the slot's total. */
internal fun BaCraftSlot.effectiveDurationMs(): Long =
    if (customDurationMs > 0L) customDurationMs else computedDurationMs()

internal fun BaCraftSlot.isActive(): Boolean = startedAtMs > 0L && effectiveDurationMs() > 0L

/** Absolute completion instant, or 0 when the slot is idle. */
internal fun BaCraftSlot.endAtMs(): Long = if (isActive()) startedAtMs + effectiveDurationMs() else 0L

internal fun BaCraftSlot.isComplete(nowMs: Long = System.currentTimeMillis()): Boolean =
    isActive() && nowMs >= endAtMs()

internal fun BaCraftSlot.remainingMs(nowMs: Long = System.currentTimeMillis()): Long =
    if (isActive()) (endAtMs() - nowMs).coerceAtLeast(0L) else 0L

internal fun BaCraftSlot.normalized(function: BaCraftFunction): BaCraftSlot {
    val capped = grades.take(function.maxEntries())
    // A Fusion slot repeats one recipe, so a mixed list can only be corruption or a stale write.
    // Keep the count the user chose and force every entry onto the first grade.
    val coherent =
        if (function == BaCraftFunction.Fusion && capped.isNotEmpty()) {
            List(capped.size) { capped.first() }
        } else {
            capped
        }
    return copy(
        startedAtMs = startedAtMs.coerceAtLeast(0L),
        grades = coherent,
        customDurationMs = customDurationMs.coerceIn(0L, BA_CRAFT_MAX_DURATION_MS),
        label = label.trim().take(BA_CRAFT_LABEL_MAX_LENGTH),
    )
}

/**
 * Adds one produced item at [grade], honouring the function's cap.
 *
 * This is the whole editing model: one tap is one item, which is exactly one summand of
 * [computedDurationMs]. Generate appends freely up to three, mixed grades allowed.
 *
 * Fusion is one recipe repeated, so tapping a *different* grade re-bases the whole slot onto it and
 * keeps the count — the teacher changed which recipe they are running, not how many. Appending a
 * different grade instead would need a mixed list, which [normalized] would only throw away.
 */
internal fun BaCraftSlot.withAppendedGrade(
    function: BaCraftFunction,
    grade: BaCraftGrade,
): BaCraftSlot {
    val next =
        when {
            function == BaCraftFunction.Generate ->
                if (grades.size >= BA_CRAFT_GENERATE_MAX_ENTRIES) grades else grades + grade

            grades.isEmpty() -> listOf(grade)
            grades.first() != grade -> List(grades.size) { grade }
            grades.size >= BA_CRAFT_FUSION_MAX_ENTRIES -> grades
            else -> grades + grade
        }
    return copy(grades = next).normalized(function)
}

internal fun BaCraftSlot.withoutLastGrade(function: BaCraftFunction): BaCraftSlot =
    copy(grades = grades.dropLast(1)).normalized(function)

/** Anchors the slot to [nowMs]. A slot with no resolvable duration cannot start. */
internal fun BaCraftSlot.started(nowMs: Long = System.currentTimeMillis()): BaCraftSlot =
    if (effectiveDurationMs() <= 0L) this else copy(startedAtMs = nowMs.coerceAtLeast(1L))

/**
 * Parses the hand-entered override, in minutes.
 *
 * Blank clears the override rather than meaning zero, so emptying the field falls back to the summed
 * total instead of making the slot unstartable.
 */
internal fun baCraftCustomDurationMsFromMinutes(text: String): Long {
    val minutes = text.trim().takeIf { it.isNotEmpty() }?.toLongOrNull() ?: return 0L
    return (minutes.coerceAtLeast(0L) * 60L * 1000L).coerceAtMost(BA_CRAFT_MAX_DURATION_MS)
}

internal fun baCraftCustomDurationMinutesText(customDurationMs: Long): String =
    if (customDurationMs > 0L) (customDurationMs / 60L / 1000L).toString() else ""

internal fun BaCraftState.slots(function: BaCraftFunction): List<BaCraftSlot> =
    when (function) {
        BaCraftFunction.Generate -> generate
        BaCraftFunction.Fusion -> fusion
    }

internal fun BaCraftState.withSlots(
    function: BaCraftFunction,
    slots: List<BaCraftSlot>,
): BaCraftState =
    when (function) {
        BaCraftFunction.Generate -> copy(generate = slots)
        BaCraftFunction.Fusion -> copy(fusion = slots)
    }

internal fun BaCraftState.slotAt(
    function: BaCraftFunction,
    index: Int,
): BaCraftSlot = slots(function).getOrElse(index) { BaCraftSlot() }

internal fun BaCraftState.withSlotAt(
    function: BaCraftFunction,
    index: Int,
    slot: BaCraftSlot,
): BaCraftState {
    if (index !in 0 until BA_CRAFT_SLOT_COUNT) return this
    val next = normalizedSlots(slots(function), function).toMutableList()
    next[index] = slot.normalized(function)
    return withSlots(function, next)
}

/**
 * Pads or truncates to exactly [BA_CRAFT_SLOT_COUNT] so every reader can index by slot without a
 * bounds check, and so a shorter persisted list from an older build fills in as idle.
 */
private fun normalizedSlots(
    slots: List<BaCraftSlot>,
    function: BaCraftFunction,
): List<BaCraftSlot> =
    List(BA_CRAFT_SLOT_COUNT) { index ->
        slots.getOrElse(index) { BaCraftSlot() }.normalized(function)
    }

internal fun BaCraftState.normalized(): BaCraftState =
    BaCraftState(
        generate = normalizedSlots(generate, BaCraftFunction.Generate),
        fusion = normalizedSlots(fusion, BaCraftFunction.Fusion),
    )

internal fun BaCraftState.hasActiveSlot(): Boolean =
    generate.any { it.isActive() } || fusion.any { it.isActive() }

/**
 * Earliest completion still in the future, or `null` when nothing is pending.
 *
 * This is what the alarm scheduler wants: one absolute instant, earliest-wins across all six slots.
 * Already-elapsed slots are excluded — they need a notification now, not an alarm later.
 */
internal fun BaCraftState.nextCompletionAtMs(nowMs: Long = System.currentTimeMillis()): Long? =
    BaCraftFunction.entries
        .asSequence()
        .flatMap { function -> slots(function).asSequence() }
        .filter { it.isActive() }
        .map { it.endAtMs() }
        .filter { it > nowMs }
        .minOrNull()

/** A slot that has elapsed, addressed so a caller can key a notification and a read marker to it. */
internal data class BaCraftCompletion(
    val function: BaCraftFunction,
    val index: Int,
    val slot: BaCraftSlot,
) {
    val endAtMs: Long get() = slot.endAtMs()
}

internal fun BaCraftState.completions(nowMs: Long = System.currentTimeMillis()): List<BaCraftCompletion> =
    BaCraftFunction.entries.flatMap { function ->
        slots(function).mapIndexedNotNull { index, slot ->
            if (slot.isComplete(nowMs)) BaCraftCompletion(function, index, slot) else null
        }
    }

/**
 * Which craft each slot has already been announced for, stored as that craft's completion instant.
 *
 * Keyed on the instant rather than a boolean on purpose: loading a slot with a different craft moves
 * its end, which re-arms the reminder for free, while a re-evaluation of the *same* craft stays
 * silent. No explicit reset, and no way for a stale flag to suppress a real completion.
 *
 * Lives in the reminder runtime rather than in [BaCraftSlot] so that posting a notification does not
 * bump `runtimeUpdatedAtMs` — that field arbitrates WebDAV merge, and a reminder must not make one
 * device's game state look newer than another's.
 */
@Serializable
internal data class BaCraftNotifiedMarkers(
    val generate: List<Long> = emptyList(),
    val fusion: List<Long> = emptyList(),
)

private fun BaCraftNotifiedMarkers.markers(function: BaCraftFunction): List<Long> =
    when (function) {
        BaCraftFunction.Generate -> generate
        BaCraftFunction.Fusion -> fusion
    }

internal fun BaCraftNotifiedMarkers.markerAt(
    function: BaCraftFunction,
    index: Int,
): Long = markers(function).getOrElse(index) { 0L }

internal fun BaCraftNotifiedMarkers.withMarkerAt(
    function: BaCraftFunction,
    index: Int,
    endAtMs: Long,
): BaCraftNotifiedMarkers {
    if (index !in 0 until BA_CRAFT_SLOT_COUNT) return this
    val next =
        List(BA_CRAFT_SLOT_COUNT) { slot ->
            if (slot == index) endAtMs.coerceAtLeast(0L) else markerAt(function, slot)
        }
    return when (function) {
        BaCraftFunction.Generate -> copy(generate = next)
        BaCraftFunction.Fusion -> copy(fusion = next)
    }
}

internal fun BaCraftNotifiedMarkers.normalized(): BaCraftNotifiedMarkers =
    BaCraftNotifiedMarkers(
        generate = List(BA_CRAFT_SLOT_COUNT) { markerAt(BaCraftFunction.Generate, it).coerceAtLeast(0L) },
        fusion = List(BA_CRAFT_SLOT_COUNT) { markerAt(BaCraftFunction.Fusion, it).coerceAtLeast(0L) },
    )

/** Elapsed slots the teacher has not been told about yet. */
internal fun BaCraftState.pendingCraftReminders(
    notified: BaCraftNotifiedMarkers,
    nowMs: Long = System.currentTimeMillis(),
): List<BaCraftCompletion> =
    completions(nowMs).filter { completion ->
        notified.markerAt(completion.function, completion.index) != completion.endAtMs
    }
