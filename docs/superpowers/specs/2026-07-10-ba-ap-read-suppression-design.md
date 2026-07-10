# BA AP Read Suppression Design

## Context

The ordinary Blue Archive AP threshold notification and the cafe stored-AP
threshold notification currently deduplicate only by the last displayed AP
integer. Tapping `Mark read` cancels the current notification, while AP
regeneration soon produces another integer and makes the notification eligible
again. This turns a user acknowledgement into a short dismissal rather than a
durable read state.

Both reminders can render through Android Live Updates, the legacy Android
notification fallback, or Xiaomi Magic and Super Island. The suppression
behavior must live above those renderers so every supported device receives the
same semantics.

The BA account model and WebDAV synchronization also carry reminder settings
and runtime data. A notification acknowledgement is device-local interaction.
Synchronizing every acknowledgement would increase WebDAV writes and could
consume request quotas on services such as Jianguoyun.

## Goals

- Add one shared setting for ordinary AP and cafe stored-AP read behavior.
- Apply the setting through the existing global and per-account custom reminder
  setting hierarchy.
- Default the setting to persistent read suppression.
- Track ordinary AP and cafe AP acknowledgements independently per account.
- Keep acknowledgement runtime local to the current device.
- Suppress reminders until AP falls below the active threshold when the setting
  is enabled.
- Limit repeated reminders to at most once per hour after acknowledgement when
  the setting is disabled.
- Keep foreground synchronization, background ticks, and Alarm scheduling on
  one policy.
- Support Android Live Updates, the legacy notification fallback, Xiaomi Magic,
  and Super Island through the same action and state path.
- Preserve current notification templates, Xiaomi Focus JSON, notification IDs,
  and renderer selection.

## Non-goals

- Synchronize read acknowledgements between devices.
- Change AP regeneration or cafe AP accumulation rules.
- Change the behavior of arena, cafe-visit, calendar, or pool reminders.
- Treat opening the notification as acknowledgement.
- Add a configurable cooldown duration in this iteration.
- Redesign the shared notification framework or Xiaomi templates.

## Synchronized Setting

`BaGlobalReminderSettings` and `BaAccountReminderOverride` gain:

```kotlin
val keepApRemindersReadUntilBelowThreshold: Boolean = true
```

The default preserves the approved upgrade behavior for existing serialized
data. The field follows the existing global/custom account resolution rules and
is included in BA account export, import, normalization, and WebDAV merge.

The BA WebDAV fingerprint revision is incremented when the serialized setting
shape changes. Existing remote documents decode the missing field as `true`.

## Device-local Runtime

A dedicated `BaApAcknowledgementStore` persists one suppression anchor for each
account and AP kind:

```kotlin
enum class BaApReminderKind {
    Ap,
    CafeAp,
}

data class BaApAcknowledgement(
    val suppressionAnchorAtMs: Long = 0L,
)
```

The store uses independent MMKV keys derived from `BaAccountId` and
`BaApReminderKind`. These keys remain outside `BaAccountRecord`, BA transfer
JSON, WebDAV fingerprints, and conflict merge state.

The store exposes focused operations to load, acknowledge, advance after a
successful hourly repeat, clear one kind, and clear an entire account. All
timestamps are normalized to non-negative values.

`BaPageSnapshot` receives the effective setting and the two local suppression
anchors needed by pure policy evaluation. `BASettingsStore.loadSnapshot()` and
`loadReminderSnapshots()` enrich account snapshots from the local store after
the synchronized account model has been resolved.

## Shared Eligibility Policy

A pure `BaApAcknowledgementPolicy` evaluates these inputs:

- notification enabled state
- current displayed AP
- effective threshold
- persistent-read setting
- local suppression anchor
- current time

It returns an eligibility decision plus any required local reset:

1. A disabled notification clears its suppression anchor and last-notified
   level.
2. AP below the threshold clears its suppression anchor and last-notified
   level.
3. An empty suppression anchor delegates to the current last-level dedupe.
4. A populated anchor with persistent-read enabled suppresses indefinitely for
   the current threshold episode.
5. A populated anchor with persistent-read disabled suppresses until
   `anchor + 1 hour`.
6. An expired hourly anchor makes the reminder eligible even when the displayed
   AP equals the last-notified level.
7. Successful delivery of an hourly repeat advances the anchor to the delivery
   time, enforcing a maximum frequency of one repeat per hour.
8. Failed delivery leaves the anchor unchanged so a later valid notification
   attempt can recover.

Ordinary AP and cafe AP use identical policy semantics with separate state.

## Notification Action Routing

`McpNotificationHelper` already builds one mark-read `PendingIntent` before the
notification renderer is selected. Its private mark-read intent adds:

- target BA account ID
- BA notification server name, which identifies ordinary AP or cafe AP
- notification ID

The intent remains explicit, immutable, and attached as
`LiveNotificationPayload.stopPendingIntent`.

`MiFocusNotificationActionReceiver` performs these steps:

1. Validate the action and notification ID.
2. Read the account ID and AP kind.
3. Cancel the notification immediately through `McpNotificationHelper`.
4. Validate that the account exists and that the notification ID matches
   `BaAccountNotificationKind` for that account and kind.
5. Use `BackgroundAsyncReceiverRunner` to persist the local suppression anchor.
6. Re-run `AppBackgroundScheduler.scheduleBaApThreshold()`.

Legacy notifications created before this change carry no BA metadata. Their
mark-read action continues to cancel the notification and skips local state
mutation. Unsupported server names and invalid account/ID combinations are
logged and ignored after cancellation.

## Renderer Coverage

The renderer contract remains unchanged:

- `ModernNotificationBuilder` adds the mark-read action from
  `state.stopPendingIntent` for Android Live Updates.
- `LegacyNotificationBuilder` adds the same action for the compatibility
  notification path.
- `MiIslandNotificationBuilder` uses the same action for Xiaomi Magic and Super
  Island.

The account metadata is therefore present before renderer selection, and every
renderer reaches the same receiver and acknowledgement store.

## Foreground and Background Integration

`BaReminderCoordinator` uses the shared eligibility policy for ordinary AP and
cafe AP background ticks. Reminder plans carry local reset intent and whether a
successful notification should advance an expired hourly anchor.

`BaApNotificationSyncCoordinator` uses the same policy for the foreground BA
page. It suppresses threshold sends while read state is active, clears local
state below threshold, and bypasses last-level dedupe for an expired hourly
repeat.

`AppBackgroundSchedulePolicy` applies the same decisions:

- persistent suppression contributes no AP Alarm candidate
- hourly suppression contributes `anchor + 1 hour`
- expired hourly suppression contributes a prompt reminder
- an unacknowledged reminder retains current threshold and AP-point scheduling

Other BA reminder candidates continue to participate in the minimum-time
selection.

## Reset and Reconciliation

Local acknowledgement reconciliation runs before BA Alarm scheduling. It
projects ordinary AP and cafe AP to the current time with existing regeneration
functions, resolves the effective account threshold, and clears local anchors
when a notification is disabled or the projected value is below threshold.

This reconciliation is reached after:

- ordinary AP manual edits and synchronization
- cafe AP edits, clearing, filling, and claiming
- notification enable or threshold changes
- account custom-setting changes
- WebDAV account merge
- process startup and Alarm rescheduling
- normal foreground and background reminder evaluation

Deleting an account clears both local keys. Changing from persistent mode to
hourly mode retains the existing anchor; an anchor older than one hour becomes
immediately eligible. Changing from hourly mode to persistent mode keeps the
current threshold episode acknowledged.

## Settings UI

The BA notification sheet adds a shared switch after the ordinary AP and cafe
AP controls:

- Label: `Keep read AP reminders silent`
- Enabled summary: read reminders remain silent until the corresponding AP
  drops below its threshold.
- Disabled summary: an acknowledged reminder may repeat at most once per hour.

The per-account custom reminder editor adds the same control. Both use existing
`SheetControlRow` and `AppSwitch` conventions. Chinese, English, and Japanese
strings are resource-backed. Existing row and switch components provide the
required touch target and accessibility semantics.

## Error Handling and Safety

- Notification cancellation completes even when BA metadata is absent or
  invalid.
- Local persistence and Alarm rescheduling run through the existing async
  receiver runner.
- Account existence and deterministic notification-ID checks constrain writes
  received by the exported Focus action receiver.
- Timestamp arithmetic uses overflow-safe addition and non-negative values.
- A successful hourly notification dispatch advances the anchor; permission or
  dispatch failures preserve the previous anchor.
- Local acknowledgement writes do not notify WebDAV sync or change BA transfer
  fingerprints.

## Verification

Focused unit and Robolectric coverage includes:

- default synchronized setting value and legacy JSON decoding
- global and custom account setting resolution
- BA transfer and WebDAV fingerprint revision behavior
- per-account and per-kind local store isolation
- persistent suppression above threshold
- ordinary AP and cafe AP reset below threshold
- hourly suppression before expiry
- hourly eligibility at expiry with an unchanged AP value
- hourly anchor advancement only after successful delivery
- independent ordinary AP and cafe AP acknowledgement
- foreground notification synchronization policy
- background reminder plan policy
- Alarm scheduling at the hourly boundary and no AP candidate during persistent
  suppression
- manual AP mutation, cafe claim, settings change, WebDAV merge, and account
  deletion reconciliation
- mark-read intent metadata and receiver validation
- compatibility behavior for old mark-read intents
- Modern Live Updates, Legacy, and Xiaomi Magic action routing

Final verification uses bounded Gradle invocations with output redirected to a
temporary log file:

1. Focused policy, store, receiver, scheduler, and serialization tests.
2. Full `:app:testDebugUnitTest`.
3. Benchmark or release-like minified APK build.
4. Physical Xiaomi API 36 validation for Super Island and the generic Android
   notification path.
5. A non-Xiaomi or renderer-forced validation of Modern Live Updates.

Device acceptance covers both AP kinds, both setting modes, process restart,
one-hour reappearance, below-threshold reset, and confirmation that mark-read
interactions create no BA WebDAV dirty state.

## Commit Boundaries

1. Design and implementation plan.
2. Synchronized setting and device-local acknowledgement store.
3. Shared eligibility policy, foreground integration, and background
   scheduling.
4. Mark-read receiver metadata and renderer-wide behavior tests.
5. Settings UI, localization, reconciliation coverage, and final verification.
