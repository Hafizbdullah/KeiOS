# ITGSA fair running memory (公平运行内存) adaptation

> **Whose standard:** the **金标联盟 / ITGSA** (Mobile Smart Terminal Ecosystem Committee), an industry body
> founded by the major Chinese handset makers. Fair running memory is a **joint** mechanism its members ship —
> announced with **vivo, Xiaomi, OPPO and Honor** named together, adaptation deadline **2026-06-30**. Adapting
> once, against any single member's documentation, covers all of them.
> **Transcribed from:** Xiaomi's *公平运行内存适配：开发者文档*,
> `dev.mi.com/xiaomihyperos/documentation/detail?pId=2304`, page updated **2026-04-28**; read **2026-08-18**.
> That page was simply the one at hand, not the scope.
> **Code:** `app/src/main/java/os/kei/memory/`. **Tests:** `ItgsaFairMemoryTest`.

## The mistake worth reading first

This landed once with registration **gated behind Xiaomi's `ro.mi.os.*` / `ro.miui.*` system properties**, on
the assumption that the `itgsa` prefix was a HyperOS namespace. It is not — it is the alliance's. The gate
meant the app declined to register on vivo, OPPO and Honor builds that send the very same broadcast, *while
logging that it had adapted*. It was the worst shape a bug can take: silent, and indistinguishable from working.

The fix is not a longer property list. An enumeration of member OEMs goes stale the moment the alliance admits
another one, and fails the same silent way. **Registration is unconditional and the broadcast is the gate** —
nothing sends these actions on a device that does not implement the standard. See "Registered everywhere" below
for what that costs and how it is paid for.

## What the mechanism is

The system watches two numbers per app and enforces a budget on both:

- **App physical memory** — the summed **PSS** of the app's *high-priority* processes. A process counts as
  high-priority if `oom_score_adj <= 200`, or it is the UI process while the app is backgrounded, or it is
  bound to one of those, or it keeps being relaunched after low-memory kills.
- **Java heap** — per process, `totalMemory() - freeMemory()` against `maxMemory()`.

As either approaches its limit the app gets a **TRIM** broadcast and is expected to free memory. If it keeps
growing it gets a **KILL** broadcast and is expected to save enough state to resume, because the process is
going to be killed regardless. Both carry a callback `IBinder` and the app has **3 seconds** to answer.

The two paths differ in what the user sees, and it changes how hard we should respond:

| | On exceeding the limit |
|---|---|
| Java heap | The system **notifies the user**, who can close the app or ignore it |
| Physical memory | The system **kills the process first**, then notifies |

So a physical-memory warning has no second chance behind it. That asymmetry is the reason
`releaseLevelFor` treats a *physical* TRIM as hard as a KILL and lets only a *heap* TRIM off lightly.

## The contract, transcribed

Actions: `itgsa.intent.action.TRIM`, `itgsa.intent.action.KILL`.

Intent extras are two nested bundles, `common` and `extra`:

| Bundle | Key | Type | Notes |
|---|---|---|---|
| `common` | `notifyType` | int | `1000` physical memory, `2000` Java heap |
| `common` | `notifyId` | int | Randomly generated; echo it back |
| `common` | `reason` | String | `"Excessive PSS Usage"` / `"Excessive Java Heap Usage"` |
| `common` | `action` | String | `"trim"` / `"kill"` — the intent action restated |
| `common` | `callback` | IBinder | Reply target |
| `extra` | `heapSize` | int, KB | **See the discrepancy below** |
| `extra` | `heapCapacity` | int, KB | |
| `extra` | `pss` | int, KB | |
| `extra` | `pssLimit` | int, KB | |

Reply: `IBinder.transact(FIRST_CALL_TRANSACTION, …, FLAG_ONEWAY)` with the parcel written in this order —
`notifyType`, `notifyId`, `result`, `extra`. `result` is `0` handled / `1` not handled; `extra` carries one
string under `reply`. **There is no AIDL to compile against**, so that field order *is* the contract, and
`the reply parcel carries notifyType notifyId result extra in that order` writes it and reads it back.

### The one contradiction in the source

The field table names the Java-heap-in-use key **`heapSize`**. The same document's example code reads
**`heapAlloc`**:

```java
int heapAlloc = extraData.getInt("heapAlloc");
```

Both cannot be right, and nothing on the page says which one ships. `readHeapUsedKb` therefore tries the
table's spelling first and the sample's second — one absent-key lookup, and no guess. If a real device ever
shows which is live, this is the note to delete.

## What KeiOS did about it

### The gap this exposed first

The app had **no memory-pressure handling at all**: nothing in the tree overrode `onTrimMemory`, implemented
`ComponentCallbacks2`, or handled `onLowMemory`. Every cache it held survived until the process died. So the
adaptation is two layers, and the OEM one is the smaller:

1. **`AppMemoryRelease`** — one release path, wired to Android's own `onTrimMemory` / `onLowMemory`. Portable,
   and the part that helps on every device including AOSP.
2. **`ItgsaFairMemoryReceiver`** — the `itgsa` broadcasts, calling that same path. The alliance mechanism
   contributes a *trigger*, not a second policy.

Worth being clear about why layer 2 earns its keep given layer 1 exists: the alliance trigger arrives
**earlier and better informed**. `onTrimMemory` says "the system is under pressure" with no numbers and, at the
levels that fire while the app is visible, nothing safe to do. A fair-memory TRIM says "*your* PSS is 600MB
against a 800MB limit" before anything has gone wrong, which is both an earlier warning and the only one that
can be logged into something actionable.

### Two levels, not five

`AppMemoryReleaseLevel` is `Moderate` / `Critical`. Android's five `TRIM_MEMORY_*` constants describe the
process's standing in the LRU list rather than how much to drop, and `levelForTrimMemory` maps them:

| Android level | Response | Why |
|---|---|---|
| `RUNNING_MODERATE`, `RUNNING_LOW` | **nothing** | These arrive while the app is *visible*. Emptying the decoded bitmaps of the list under the user's eyes causes a re-decode they can see. Bounded caches are the answer to these, not empty ones |
| `UI_HIDDEN` | **nothing** | Says the UI went away, not that memory is short |
| `BACKGROUND`, `MODERATE` | `Moderate` | Halve the Coil memory cache, evict the bitmap caches |
| `RUNNING_CRITICAL`, `COMPLETE` | `Critical` | The alternative is being killed |

### Never disk

Both bitmap caches expose a clear that evicts memory **and `deleteRecursively()`s the disk cache**. Calling
one from a memory-pressure handler is wrong twice over: disk is not the resource under pressure, and deleting
it converts a memory problem into a network one the moment the user scrolls back. So `BaGuideImageCache`
gained `evictMemory()`, the icon cache is cleared through its existing `clear(context = null)` path, and
`release never touches disk caches` asserts it on the source — the mistake is a one-word edit, passing a
context where `null` belongs.

That test strips comments before matching, because the file's own notes explain what `clearAll(context)`
would do wrong and a naive search finds the explanation. Same trap as the `vibrancy()` assertion in the
Liquid campaign.

### Coil is the prize

Coil's memory cache is configured at **25% of the heap**, which makes it the largest reclaimable allocation
in the process by a wide margin. `Moderate` halves it — the on-screen images are the most recently used, so
trimming to half keeps them and drops the scrollback — and `Critical` empties it.

### Registered everywhere, and defended at the handler

Registration is unconditional — see "The mistake worth reading first". The receiver must be **exported** for
the system to reach it, so on every device, including ones where nothing will ever broadcast, any local app can
trigger it.

That surface is paid for at the handler rather than at the registration. `MIN_RELEASE_INTERVAL_MS` is a **4
second** floor on how often a broadcast may actually cause a release: far below any plausible cadence for a
genuine memory warning, far above what an abuser needs to be made useless. A release is cheap and safe but not
free — it evicts the Coil memory cache and calls `Debug.getPss()` twice — so without the floor a caller in a
loop could turn it into a re-decode treadmill.

Two deliberate exceptions, both tested:

- **A KILL is never suppressed.** It is the last chance to save state; a rate limit there would trade a real
  save for an abuse that cannot do meaningful harm anyway.
- **The reply is never rate-limited.** A suppressed release still answers the system inside its 3-second
  window, because a missing reply is what gets the process killed.

`shouldRunItgsaRelease` also treats a **backwards clock** as "long enough ago". `System.currentTimeMillis()` is
not monotonic, and an NTP correction that puts "now" before the last release would otherwise wedge the limit
shut until the clock caught up — which on the physical-memory path means being killed instead.

### The 3-second budget

The receiver runs on its own `HandlerThread`, as the doc's sample does, because `Debug.getPss()` alone can
cost tens of milliseconds. The reply is sent from that callback as soon as the work returns rather than being
posted for later, and the handler logs a warning if it took longer than the budget — "we replied at 2.9s" is
the warning that the next cache added here will push it over.

## Verified, and not

**Verified on the API 37 AVD** (AOSP):

- The receiver registers: `ITGSA fair-memory receiver registered`. Under the old vendor gate this same AVD
  logged `not HyperOS, fair-memory receiver not registered` — which is exactly the silent decline that would
  have happened on three quarters of the alliance.
- It is reachable and rejects malformed input, on its own thread. `am broadcast` of the TRIM action logged
  `ignored itgsa.intent.action.TRIM: not a fair-memory notification` from a non-main tid.
- The portable path works end to end. `adb shell am send-trim-memory <pid> COMPLETE` on a backgrounded
  process produced:
  `released level=Critical caches=3 pssBeforeKb=157818 pssAfterKb=153977 freedKb=3841`
  — **3.8 MB recovered** on a near-idle app.

**Not verified, and it cannot be here:**

- The `itgsa` broadcasts. Only an alliance member's build sends them, and the AVD is AOSP. The parsing, the
  level policy, the rate limit and the reply parcel are unit-tested; that the *system* accepts the reply is not.
  Note that the receiver being exported means a **local** broadcast can be used to exercise the whole handler
  path on any device — see the phone list below.
- The visible-level ignore, live. `am send-trim-memory` refuses to raise a trim level once lowered
  (`Unable to set a higher trim level than current level`) and refuses background levels on a foreground
  process, so the running-level cases are covered by unit test rather than by the shell.

### Owed on the phone

Add to the physical-device pass:

1. **Confirm registration** on the alliance device: the log line should be
   `ITGSA fair-memory receiver registered`. There is no vendor branch left to get wrong, so this is a smoke
   check rather than the test it used to be.
2. **Exercise the handler with a local broadcast**, which needs no memory pressure and works on any device:

   ```bash
   adb shell am broadcast -a itgsa.intent.action.TRIM --ei notifyType 1000 --ei notifyId 1
   ```

   `am` cannot build the nested `common` bundle, so the correct outcome is the parser **rejecting** it. Expect
   exactly:

   ```
   I ItgsaFairMemory: ignored itgsa.intent.action.TRIM: not a fair-memory notification
   ```

   That line exists only so this check means something — a rejected broadcast and a broadcast that never
   arrived are otherwise indistinguishable. Seeing it proves the receiver is registered, exported, reachable,
   and running on its own thread (the tid in the log line is not the main thread's). Verified on the AVD.
3. **If a real TRIM or KILL arrives, capture the log.** It carries `pss=…/…`, `heap=…/…` and `usage=…`, and it
   is the only way to learn which of `heapSize` / `heapAlloc` the shipped system actually sends and whether the
   reply is accepted.
4. **Watch for the system's own notification.** If it appears, the app exceeded a limit and the response was
   too slow or too small.
