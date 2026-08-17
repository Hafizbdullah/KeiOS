# HyperOS fair running memory (公平运行内存) adaptation

> Source: *公平运行内存适配：开发者文档*, `dev.mi.com/xiaomihyperos/documentation/detail?pId=2304`,
> page updated **2026-04-28**. Read and transcribed **2026-08-18**.
> Code: `app/src/main/java/os/kei/memory/`. Tests: `HyperOsFairMemoryTest`.

## What the mechanism is

Xiaomi watches two numbers per app and enforces a budget on both:

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
   and the part that helps on every device.
2. **`HyperOsFairMemoryReceiver`** — the `itgsa` broadcasts, calling that same path. The OEM mechanism
   contributes a *trigger*, not a second policy.

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

### Inert off HyperOS

`register` is gated on the Xiaomi system properties, using the same list `BaGuideBgmMediaOemCompat` already
uses (two different answers to "is this a Xiaomi build" in one app would be a bug waiting to happen).
That matters beyond tidiness: the receiver has to be registered **exported** for the system to reach it, and
gating means that surface does not exist on devices that will never send the broadcast. The worst it buys an
attacker on a Xiaomi device is making the app drop caches it can rebuild.

### The 3-second budget

The receiver runs on its own `HandlerThread`, as the doc's sample does, because `Debug.getPss()` alone can
cost tens of milliseconds. The reply is sent from that callback as soon as the work returns rather than being
posted for later, and the handler logs a warning if it took longer than the budget — "we replied at 2.9s" is
the warning that the next cache added here will push it over.

## Verified, and not

**Verified on the API 37 AVD** (AOSP, no Xiaomi properties):

- The gate works: `not HyperOS, fair-memory receiver not registered`.
- The portable path works end to end. `adb shell am send-trim-memory <pid> COMPLETE` on a backgrounded
  process produced:
  `released level=Critical caches=3 pssBeforeKb=157818 pssAfterKb=153977 freedKb=3841`
  — **3.8 MB recovered** on a near-idle app.

**Not verified, and it cannot be here:**

- The `itgsa` broadcasts. Only a HyperOS build sends them, and the AVD is AOSP. The parsing, the level policy
  and the reply parcel are unit-tested; that the *system* accepts the reply is not.
- The visible-level ignore, live. `am send-trim-memory` refuses to raise a trim level once lowered
  (`Unable to set a higher trim level than current level`) and refuses background levels on a foreground
  process, so the running-level cases are covered by unit test rather than by the shell.

### Owed on the phone

Add to the physical-device pass:

1. Install on a HyperOS device and confirm the log line is `fair-memory receiver registered` rather than the
   "not HyperOS" branch — that alone proves the property gate is right on real hardware.
2. If a TRIM or KILL ever arrives, the log carries `pss=…/…`, `heap=…/…` and `usage=…`. **Capture it**: it is
   the only way to learn which of `heapSize` / `heapAlloc` the shipped system actually sends, and whether the
   reply is accepted.
3. Watch for the system's own notification appearing — that means the app exceeded a limit and the response
   was too slow or too small.
