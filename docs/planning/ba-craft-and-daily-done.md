# BA Craft Chamber timers and the daily-done tile

Record of what landed for issue #24 and the daily-done shortcut that grew out of it, kept for the
parts that are **not** recoverable from the code: where the game numbers came from, and which designs
were tried and rejected.

## Craft Chamber: the mechanic, and why one formula covers both halves

The game's 製造 screen has two functions, each with three independent slots — six timers per account.
They look like two mechanics and are one.

| Grade | Per-item duration |
|---|---|
| 下級 / 하급 | 30m |
| 一般 (中級) / 일반 | 1h 30m |
| 上級 / 상급 | 3h |
| 最上級 / 최상급 | 6h |

Source: namu.wiki's Craft Chamber page — *「등급이 높을수록 제작시간이 길어진다. (하급: 30분,
일반: 1시간 30분, 상급: 3시간, 최상급: 6시간)」* — cross-checked against the game8 and kamigame
tables. The same ladder drives both functions.

**A slot's total is the sum of the grades of every item it will produce.**

- **Generate** opens 1–3 nodes ("解"), each producing one item, freely mixed grades. namu.wiki:
  *「3차 노드까지 전부 개방하면 … 총 3개의 아이템을 제작하며, 제작 시간도 3개 분량을, 제조 부스터
  티켓도 3개를 사용한다」* — three items, three items' worth of time, three booster tickets.
- **Fusion** produces 1–5 copies of one recipe. The cap is **5 at every grade** — verified in-game by
  the reporter, not from a guide, because no guide states it.

That is why `BaCraftSlot.grades` is a `List<BaCraftGrade>` rather than a grade plus a count: Fusion
stores its grade repeated N times, and `computedDurationMs()` is `grades.sumOf { it.durationMs }` with
no branches. The difference between the two functions collapses into one validation rule in
`normalized()` — Fusion forces every entry onto the first grade — not a second code path.

### A misreading worth recording

The first pass had the node count *not* affecting the slot total, on the strength of game8 and
kamigame wording. Those sources mean the **per-item** duration is unchanged by node count. The slot
total is the sum. The reporter corrected this twice before it stuck; namu.wiki settles it.

### Deliberate bounds

- `BA_CRAFT_MAX_DURATION_MS = 48h`. The longest reachable real craft is Fusion at 5 × 最上級 = 30h.
  The override is loosened to 48h so a booster ticket spent partway or a clock correction still fits,
  while a corrupt value cannot arm an alarm months out.
- A hand-entered total wins over the sum, because the game only ever displays the slot's total. Blank
  clears the override rather than meaning zero, so emptying the field cannot make a slot unstartable.

### Reminder plumbing

- `startedAtMs` is a wall-clock anchor, never a countdown — same choice AP and the cafe already make,
  so a killed process or a skipped frame cannot make it drift. `startedAtMs == 0L` is the idle
  sentinel, which is why no test may use the epoch as a start.
- `BaCraftNotifiedMarkers` keys on the **completion instant**, not a boolean. Loading a slot with a
  different craft moves its end and re-arms the reminder for free, while re-evaluating the same craft
  stays silent. No explicit reset, and no way for a stale flag to suppress a real completion.
- Those markers live in the reminder runtime, not in `BaCraftSlot`, so posting a notification cannot
  bump `runtimeUpdatedAtMs` — that field arbitrates WebDAV merge, and a reminder must not make one
  device's game state look newer than another's.
- Craft always schedules at `BackgroundAlarmPrecision.Prompt`, never windowed. Android 17's windowed
  alarms slip 10–30 minutes, which is fine for an AP threshold and useless for "your craft is done".
- The completion sweep collects successes and writes them in **one** batched store call. Six separate
  writes per sweep meant six JSON re-encodes and, worse, a partial write on failure would re-fire the
  15-second alarm retry.

## Daily-done: template

`planBaDailyDone` applies the common Sensei routine in one tap:

- Both AP pools to 0.0, anchors re-based to now, notified levels to -1. Rationale from the reporter:
  a teacher who opens the game will collect the cafe AP into the main pool and spend it, so both
  really are zero.
- Headpat and both invite tickets start their cooldowns **only if already elapsed**. Anything still
  on cooldown is left alone — the tap must never look like it un-spent something.
- Generate slots 1 and 2 load one 上級 node (3h) each, only when free. Chosen because one node is the
  best value per booster ticket, and 上級 items and gifts are the common 3h case.

Note that headpat comes back at `min(3h cooldown, next cafe student refresh)`, so the visible number
after a tap is frequently *not* 3h. That rule predates this feature and is intact.

## The quick-settings tile: what the platform actually allows

The reporter pushed back on an early framing that declaring a tile "pollutes" the picker. They were
right, and the corrected facts are the design:

1. Declaring a `TileService` does **not** put it in the quick-settings panel. Official wording: the
   tile appears only *after the user has added it*. It does appear in the panel's **edit list**.
2. `StatusBarManager.requestAddTileService()` (API 33+) is the sanctioned in-app prompt.
3. Every component here additionally ships `android:enabled="false"`, so it is absent even from the
   edit list until claimed. Claiming enables it via `setComponentEnabledSetting` first — an add
   request for a disabled component returns `TILE_ADD_REQUEST_ERROR_BAD_COMPONENT`.
4. The add request is rate limited **per ComponentName**, and the platform "can choose to auto-deny a
   request if the user has denied that specific request (user, ComponentName) enough times before" —
   permanently. So it fires only from an explicit tap, never on composition or an account change, and
   a slot keeps its component identity rather than being recycled between accounts.
5. The requesting app must be in the foreground (`TILE_ADD_REQUEST_ERROR_APP_NOT_IN_FOREGROUND`).

Points 4 and 5 came from `~/Library/Android/sdk/sources/android-37.1/android/app/StatusBarManager.java`
after web javadoc lookups kept failing. The local SDK sources are the better reference for this API.

### The one hard constraint

A tile is a manifest component and cannot be created at runtime. "One tile per account" is therefore a
**fixed pool of 3** subclasses reading their binding from `BASettingsStore` — which happens to match
`serverIndex`'s own 0..2 range. The UI says so when the pool is full rather than failing quietly.
Launcher shortcuts have no such limit and are generated per account, bounded only by
`getMaxShortcutCountPerActivity`, with truncation logged rather than silent.

Deleting an account frees its slot; **disabling** one does not. Disabling is reversible, and handing
the tile back may not be.

### Three bugs found only on a device

- **No feedback at all.** Android 12+ drops toasts from a background app and a tile click is not
  foreground; and at the icon-only tile size the panel renders neither label nor subtitle. Both cheap
  channels are unavailable, so the tile reports through `BaDailyDoneNotificationDispatcher` — one
  fixed id, replacing rather than stacking, silent when nothing changed. The Shortcut path keeps its
  toast, because there the app really is foreground.
- **Dismissing the dialog was reported as "unavailable".** `TILE_ADD_REQUEST_RESULT_DIALOG_DISMISSED`
  is `3`, is `@hide` (so it cannot be named in app code), is absent from the `@IntDef`, and still
  reaches the callback when the dialog is swiped away. Enumerating the known codes sent it to the
  error branch, telling the teacher their device could not host a tile it had just offered them.
  `baDailyTileAddResultOf` now splits on the documented boundary instead — *"Values greater or equal
  to [1000] indicate an error in the request"* — so anything below it is a decline and any result code
  the platform adds later lands in the right bucket on its own.
- **A declined request left the claim in place**, found in the same session by noticing the settings
  row still said "Remove tile" after cancelling. Claiming *has* to precede the request (an add for a
  disabled component fails), and nothing undid it. Three consequences, worst first: the component
  stayed in the quick-settings editor the teacher had just declined it from, which is the exact
  property this whole design exists to protect; the row lied about a tile that was not there, because
  it derives from the component-enabled state — the only part that survives process death; and a
  declined per-account request permanently burned one of the three pool slots. Both request paths now
  roll back to their pre-request state unless `keepsTile`, and only if they were the ones that changed
  it, so re-requesting an already-added tile cannot disable a working one. Verified on the AVD in both
  directions: cancelling leaves `enabledComponents` without the service and the row offering "Add
  tile"; accepting keeps both.

## Card expansion

Six rows made the Craft Chamber the tallest card on the page, and most of the time all six are idle,
so the header is a disclosure control.

Collapsing is non-lossy by design: a plain hide button would trade the card's meaning for vertical
space, so the header carries a one-line summary. Ready outranks running — it is the only state needing
action — and anything running reports the nearest completion, the single number the rows were scanned
for.

`craftCardExpanded` is **global**, not per-account: it is one card's layout on one page, unlike `craft`
itself. It rides `BaPageSnapshot` the way `showEndedActivities` does, so `withBaAccount` leaves it
alone. The two sit adjacent in the snapshot and are easy to confuse, so a mapper test pins it.

Its store write deliberately skips `notifyChanged()`. That signal re-labels the daily-done tiles and
shortcuts and wakes the home overview; folding a card changes none of that.

Default is expanded — hiding rows an existing install was already using would read as data loss.

## Open

- `BaDailyTileManager` and `BaDailyShortcutSync` have device verification but no unit tests beyond the
  pure parts (`baDailyTileAddResultOf`, and the binding model's own 13 tests). What is left is almost
  entirely `PackageManager` / `StatusBarManager` calls; testing it needs a seam that does not exist
  yet.
- The daily-done template is fixed. There is no entry for "AP left after clearing everything", which
  was floated and not built.
