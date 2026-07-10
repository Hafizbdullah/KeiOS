# BA AP Dismiss Snooze Design

## Context

BA ordinary AP and cafe AP reminders expose an explicit `Mark read` action.
The current live-notification payload also reuses that action as Android's
`Notification.deleteIntent`. A swipe dismissal therefore writes the same
account-scoped suppression anchor as an explicit button press.

Removing the delete callback entirely would make the next AP increment or
background refresh eligible to recreate the notification immediately. The
notification runtime also caches active state for 1.2 seconds, so a dismissal
must invalidate the cached snapshot before the next delivery decision.

## Goals

- Keep explicit `Mark read` as the only action that records acknowledgement.
- Treat swipe dismissal and `Clear all` as a one-hour temporary snooze.
- Suppress AP increments and refreshes during the temporary snooze.
- Clear acknowledgement and dismissal state after AP falls below threshold.
- Treat a later threshold crossing as a new reminder episode.
- Keep ordinary AP and cafe AP state isolated per BA account.
- Apply the same routing to legacy notifications, Android Live Updates, and
  Xiaomi Magic/Super Island.
- Preserve existing stop-on-dismiss behavior for non-BA MCP notifications.

## Interaction Semantics

| Interaction | Persisted state | Eligibility |
| --- | --- | --- |
| Explicit `Mark read`, setting enabled | read anchor | suppressed until recovery |
| Explicit `Mark read`, setting disabled | read anchor | eligible one hour later |
| Swipe or `Clear all` | dismissed-until timestamp | eligible one hour later |
| AP below threshold or reminder disabled | state cleared | next crossing starts a new episode |

The read anchor and dismissed-until timestamp are separate local fields. An
explicit mark-read action clears any temporary dismissal timestamp before
writing the read anchor. A dismissal leaves the read anchor unchanged.

## Notification Routing

`LiveNotificationPayload` gains `deletePendingIntent`, defaulting to
`stopPendingIntent`. This default preserves existing behavior for every caller
that has not opted into separate dismissal semantics.

For BA AP event payloads, `McpNotificationHelper` builds two immutable broadcast
intents with separate actions and request-code ranges:

- visible secondary action: `ACTION_MARK_READ`
- notification deletion callback: `ACTION_DISMISS`

The legacy, modern Live Updates, and Xiaomi builders continue to use
`stopPendingIntent` for the visible secondary action. Their `deleteIntent`
uses `deletePendingIntent`. Xiaomi Focus JSON therefore keeps the explicit
mark-read action while Android dismissal gets the snooze action.

## Receiver Behavior

`MiFocusNotificationActionReceiver` handles both actions after validating the
notification ID and immutable BA account metadata.

- `ACTION_MARK_READ` cancels the notification, clears runtime state, clears a
  temporary dismissal timestamp, writes the current read anchor, and
  reschedules BA AP work.
- `ACTION_DISMISS` clears the MCP snapshot and active-state cache, writes a
  saturated `now + 1 hour` dismissal timestamp, and reschedules BA AP work.

Unsupported server names, stale account IDs, and mismatched account-scoped
notification IDs can clear notification runtime state but cannot mutate BA AP
suppression data.

## Local State And Policy

`BaApAcknowledgementStore` remains the account-local persistence boundary and
adds a `ba_ap_dismissed_until` key family. The state stays outside account
transfer and WebDAV payloads, matching existing read-anchor behavior.

`BaApAcknowledgementPolicy` evaluates both fields. A permanent read anchor has
highest priority. Active hourly read and dismissal deadlines suppress until
their latest applicable deadline. Expiry bypasses last-level deduplication so
the user can receive one reminder even when AP stayed at the same displayed
level.

Foreground page sync, background scheduling, background delivery, account
snapshot mapping, reconciliation, and account deletion all receive the new
dismissed-until state. Recovery and disabled-reminder reconciliation remove
both local state families.

## Verification

Tests cover:

- separate visible-action and delete-action routing in all three builders
- distinct immutable PendingIntent identities and account metadata
- dismissal runtime-cache invalidation
- explicit read state and dismissal state persistence isolation
- AP `+1` remaining suppressed during a dismissal snooze
- exact one-hour scheduling and same-level re-notification after expiry
- recovery clearing both ordinary and cafe AP state
- account deletion clearing both key families
- ordinary AP and cafe AP background scheduling
- existing mark-read-until-recovery and hourly-read behavior

The final verification runs focused module tests, full app unit tests,
`compileDebugKotlin`, `assembleBenchmark`, APK signature verification, APK
metadata inspection, `git diff --check`, and a Gradle-process audit.

## Commit Boundaries

1. Design and implementation plan.
2. Separate notification dismissal routing from the visible mark-read action.
3. Add account-scoped BA AP dismissal snooze state and policy integration.
