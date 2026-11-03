# RemoveReels: MVP plan

Status: M0 to M4 done, M5 next · Last updated: 2026-11-03

## 1. Product

RemoveReels is an Android app that blocks Reels inside the real Instagram app and Shorts inside the real YouTube app.
Everything else in both apps keeps working.

The primary user is the author, on their own phone. The source is public as a portfolio project; the app is not
published on Google Play.

**Goals**

- Block every Reels and Shorts screen, however it is opened.
- Optional daily allowance per app, at most 45 minutes combined.
- Leave everything else in both apps untouched: feed, stories, messages, posting, search, comments, normal videos,
  sign-in, notifications.

**Non-goals for the MVP**

- Other apps (Instagram Lite, Facebook, TikTok) and other platforms (iOS, web).
- Hiding Reels and Shorts previews inside feeds. They stay visible; opening one is blocked. This is the next feature
  after the MVP; see [hiding-plan.md](hiding-plan.md).
- Statistics, streaks, schedules, translations.

## 2. Product rules

| Rule | Behaviour |
|---|---|
| Blocked by default | After installing, both allowances are 0: Reels and Shorts are fully blocked. |
| Allowances | Per app: *block completely*, or 5–45 minutes in 5-minute steps. Instagram + YouTube ≤ 45 minutes. |
| Changes start tomorrow | Every change, up or down, takes effect at the next local midnight and stays until changed again. Today's allowance never changes. |
| No off switch | The app has no setting that pauses or disables blocking, for either app. Uninstalling is the only way to stop it. |
| System switch | Android always lets the user turn an accessibility service off. If that happens, a notification says blocking is off until it is turned back on. |
| Automatic time required | Allowances apply only while Android's *automatic date & time* and *automatic time zone* are on. Otherwise both apps are fully blocked, which closes the "change the date" and "change the time zone" loopholes (ADR 0007). |
| What counts | Time while a Reels or Shorts screen is visible, the screen is on and that app is in front. |

Consequences worth knowing:

- Clearing the app's storage or reinstalling resets both allowances to 0, so neither ever loosens the rules.
- Force stop also cancels the app's scheduled background checks until it is opened again (an Android rule), so the
  "blocking is off" notification cannot appear in that one case.

## 3. How blocking works

```
Instagram / YouTube draws a screen
        │  Android reports screen changes for these two packages only
        ▼
Throttle (≤ 4 checks/s, immediate on window change)
        ▼
SurfaceDetector ── rule list: view ids, minimum screen share, fallbacks
        │
        ├─ not Reels/Shorts → nothing happens
        ▼
BlockPolicy ── allowance left today? automatic time on?
        ├─ yes → count the time, show the timer pill
        ▼ no
Enforcer
  1. take audio focus, which pauses the video
  2. show the block card over the app
  3. press back for the user; re-check; up to 3 times, then tap the app's home tab,
     last resort: go to the launcher
  4. release audio focus once the Reels/Shorts screen is gone
```

**Detection.** Each app builds its screens from views with internal ids (Instagram calls Reels "clips" internally).
The detector looks ids up directly instead of walking the whole screen, which keeps it fast and light on battery. A
match only counts when the view is visible and covers most of the screen, so a thumbnail in the feed never triggers a
block. The rule lists are data
([`detection/src/main/resources/.../rules`](../detection/src/main/resources/io/github/mallma/removereels/detection/rules)),
not code, and are tested against screens recorded on a phone
([`detection/src/test/resources/fixtures`](../detection/src/test/resources/fixtures)). Instagram 453 marks every Reels
viewer with `clips_viewer_view_pager`, and a Reel opened from a chat also shows `sender_profile_pic`; stories use
different `reel_viewer_*` ids and stay untouched. YouTube 21.38 marks the Shorts player with `reel_recycler` and
`reel_watch_player`. Debug builds can record new screens through adb; see `SnapshotRecorder`.

**Canaries.** A rule list can also name an element that should always appear with the app's main screen, such as the
Reels tab button next to Instagram's tab bar. If the main screen appears without it, the app's layout has probably
changed and the rules are stale, and RemoveReels says so instead of failing silently. YouTube's tab bar items have no
view ids, so YouTube has no canary; its fixture tests are the safety net.

| | Instagram | YouTube |
|---|---|---|
| Keeps working | Feed, stories, messages, posting (music, close friends), profiles, search, live, notifications | Home, subscriptions, search, normal videos, sign-in, comments, library, notifications |
| Blocked | Reels tab; Reels viewer opened from anywhere: feed, Explore, profile, messages, links | Shorts tab; Shorts player opened from anywhere: home shelf, search, subscriptions, channel, links |
| Visible but not playable | Reels previews in the feed | Shorts shelves |

## 4. Tech stack

| Layer | Choice | Why | Not chosen |
|---|---|---|---|
| Platform | Native Android | The only mobile OS that lets an app recognise a screen inside another app. | iOS: no API for it, and Screen Time can only lock whole apps. |
| Language | Kotlin 2.x | The standard Android language; null safety and coroutines. The core logic lives in pure Kotlin modules testable on any JVM. | Java (verbose); Flutter / React Native (no second platform to share with, and an extra runtime inside an always-on service). |
| Blocking mechanism | `AccessibilityService` limited to the two packages | The only official API that can tell the Reels viewer apart from messages inside another app, and act on it (back, tap, overlay). | UsageStats (knows the app, not the screen); VPN/DNS (Reels and feed videos share servers); patched APKs (fragile, legally risky). |
| Overlays | `TYPE_ACCESSIBILITY_OVERLAY` windows | Come with the accessibility service, so no "draw over other apps" permission. | `SYSTEM_ALERT_WINDOW` (extra permission); a block Activity (fights the app's back stack). |
| UI | Jetpack Compose + Material 3 | Less code, state-driven, good defaults for TalkBack, large fonts and right-to-left. Also renders the overlays. | XML views (more code, older patterns). |
| State | ViewModel + `StateFlow`, unidirectional data flow | One source of truth per screen; easy to test with fakes. | MVI frameworks (machinery not needed for six screens). |
| Async | Coroutines + Flow | Shared by the service, storage and UI. | RxJava. |
| Storage | Jetpack DataStore with a kotlinx.serialization serializer | One typed, atomic state file (schedule, usage, flags); safe for frequent writes from the service; no code generation. | Room (a database without queries); SharedPreferences (no transactions, synchronous I/O); Proto DataStore (protobuf code generation). |
| Rule lists | JSON parsed with kotlinx.serialization | Rules stay data; parsing is compile-time and reflection-free. | Hard-coded ids; Gson / Moshi. |
| Background check | WorkManager, every 15 minutes | Notices when the accessibility service is off and posts the notification; survives reboots. | A foreground service (permanent notification for no benefit). |
| Dependency injection | A small hand-written `AppContainer` | About a dozen objects; manual wiring is explicit and has no build cost. | Hilt, Koin. |
| Navigation | Navigation Compose with type-safe routes | Standard, small. | Hand-rolled. |
| Time | `java.time.Clock` (wall time) and `kotlin.time.TimeSource` (monotonic), both injected | Correct midnights and time zones; sessions immune to clock changes; deterministic tests. | `System.currentTimeMillis()`. |
| SDK levels | minSdk 33, target/compile 37 (Android 17) | Android 13+ removes compatibility branches (node recycling, notification permission checks, install-source API, per-app language). The author's phone runs Android 13 or newer. | minSdk 26 (more legacy code paths for users the project does not target). |
| Permissions | Accessibility binding, `POST_NOTIFICATIONS`, `<queries>` for the two packages. **No `INTERNET`.** | An app that can see screens should be unable to send anything; Android enforces it. | |
| Distribution | Release APK on GitHub Releases, installed with `adb` or Android Studio | Personal use; no store review. | Google Play, F-Droid. |

## 5. Architecture

```
:app (Android)  ──►  :detection (pure Kotlin)
     │
     └─────────────►  :budget (pure Kotlin)
```

| Module | Contents | Depends on |
|---|---|---|
| `:detection` | Rule model and parser, `ScreenTree` interface, `SurfaceDetector`, canary checks | kotlinx.serialization |
| `:budget` | `Allowance`, `AllowanceSchedule`, `UsageLedger`, `BlockPolicy`, day boundaries | nothing |
| `:app` | Accessibility service and `ScreenTree` adapter, enforcer, overlays, DataStore, WorkManager, Compose UI, `AppContainer` | both modules |

The two pure modules cannot import Android classes, which the build enforces. All decisions live there and are tested
on the JVM in milliseconds; `:app` translates between Android and those modules. Each pure module is created together
with its first code, in M1 and M2. Shared build settings live in convention plugins in `build-logic`.

Core model (`:budget`):

- `Allowance`: blocked, or 5–45 minutes in steps of 5.
- `Allowances`: one per app; construction fails if the sum exceeds 45 minutes.
- `AllowanceSchedule`: today's allowances plus an optional change effective from a given date. `scheduleChange(new, today)` always sets the effective date to `today + 1`.
- `UsageLedger`: time watched per app per date; keeps seven days.
- `BlockPolicy.decide(app, today, schedule, ledger, automaticTimeOn)` → `Allow(remaining)` or `Block(reason)`.
- `WatchTimer`: measures watching time on a monotonic clock, so changing the wall clock adds or removes nothing.

## 6. UI/UX

### Principles

1. **Calm, never shaming.** Copy states facts ("20 min used today"), never judgements.
2. **Say what still works.** Every block message mentions that feed and messages are unaffected.
3. **No surprises.** Stop the video, explain, let the user continue. Never jump screens without showing why.
4. **One main screen.** Daily information is on Home; everything else is rare.
5. **Accessible.** Icons with text, 48 dp touch targets, readable at 200 % font size, TalkBack labels, respects
   reduced motion.
6. **No borrowed branding.** App icons are read from the installed apps at runtime; no logos ship in the APK.

### Screen map

```
First run:  Welcome → How it works → Notifications → Turn on → [Phone maker] → Try it → Home
Home ──► Daily allowance
  └────► Settings ──► Keep running on this phone · Test blocking · About
Over Instagram / YouTube:  block card · timer pill
System:  "Blocking is off" notification
```

### Onboarding

```
Welcome                          How it works
┌──────────────────────────────┐ ┌──────────────────────────────┐
│ Keep Instagram and YouTube.  │ │ RemoveReels uses Android's   │
│ Lose Reels and Shorts.       │ │ Accessibility feature to     │
│                              │ │ notice when Instagram or     │
│ ✓ Feed, messages, stories,   │ │ YouTube opens Reels or       │
│   posting and search work    │ │ Shorts.                      │
│ ✕ Reels and Shorts: blocked  │ │                              │
│ ⏱ Optional daily allowance   │ │ • It only looks at those two │
│                              │ │   apps                       │
│ There is no off switch.      │ │ • It saves nothing you see   │
│ Uninstalling is the only way │ │   or type                    │
│ to stop it.                  │ │ • It has no internet access  │
│                              │ │                              │
│         [ Get started ]      │ │       [ Continue ]           │
└──────────────────────────────┘ └──────────────────────────────┘

Turn on                          Try it
┌──────────────────────────────┐ ┌──────────────────────────────┐
│ Turn on RemoveReels          │ │ Check that it works          │
│                              │ │                              │
│ ① Tap Open settings          │ │ Open Instagram and tap the   │
│ ② Tap RemoveReels            │ │ Reels button.                │
│ ③ Switch it on               │ │                              │
│                              │ │     [ Open Instagram ]       │
│      [ Open settings ]       │ │          Skip                │
└──────────────────────────────┘ └──────────────────────────────┘
 Returns by itself and shows       After the block card: "✓ It works.
 "✓ Blocking is on".               Your feed and messages are untouched."
```

- **Welcome** states the no-off-switch rule up front, so it is a choice, not a surprise.
- **Notifications** asks for the notification permission and explains its single use: telling you when blocking is off.
- **Turn on** opens Android's Accessibility settings; apps cannot open their own entry there directly. If the APK was installed from a downloaded file, Android 13+
  greys out the switch ("restricted setting"); the app detects the install source and shows the extra step first:
  App info → ⋮ → Allow restricted settings.
- **Phone maker** appears only on brands known to stop background services (Xiaomi, Oppo, Vivo, Huawei, Samsung) and
  shows that brand's steps.
- **Try it** makes the first block one the user triggered on purpose.

### Home

```
┌──────────────────────────────────┐
│ RemoveReels                    ⚙ │
│ ┌──────────────────────────────┐ │
│ │ ● Blocking is on             │ │   red when off: "● Blocking is off  [Turn on]"
│ └──────────────────────────────┘ │
│ ┌──────────────────────────────┐ │
│ │ [icon] Instagram             │ │
│ │ Reels: blocked               │ │
│ └──────────────────────────────┘ │
│ ┌──────────────────────────────┐ │
│ │ [icon] YouTube               │ │
│ │ Shorts: 7 of 15 min left     │ │
│ │ ███████████░░░░░░░░░         │ │
│ │ From tomorrow: 20 min        │ │
│ └──────────────────────────────┘ │
│                                  │
│ Today: 8 of 15 min · resets at   │
│ midnight                         │
│                                  │
│       [ Daily allowance ]        │
└──────────────────────────────────┘
```

Card states: *blocked* · *X of Y min left* · *used up today* · *not installed* · *rules may be out of date*
(canary failed). A banner explains when automatic date, time or time zone is off: "Allowances need automatic date, time and time zone. Until
it's on, Reels and Shorts stay blocked."

### Daily allowance

```
┌──────────────────────────────────┐
│ ←  Daily allowance               │
│                                  │
│ Today                            │
│ Instagram Reels      blocked     │
│ YouTube Shorts       15 min      │
│                                  │
│ From tomorrow                    │
│ [icon] Instagram Reels           │
│  (●) Block completely            │
│  ( ) Allow  ──────●────── 20 min │
│ [icon] YouTube Shorts            │
│  ( ) Block completely            │
│  (●) Allow  ────●──────── 15 min │
│                                  │
│ Shared daily maximum: 45 min     │
│ ┌───────────┬────────┬─────────┐ │
│ │ IG 20     │ YT 15  │ free 10 │ │
│ └───────────┴────────┴─────────┘ │
│                                  │
│ Changes start tomorrow (Mon 2    │
│ Nov) and stay until you change   │
│ them again.                      │
│                       [ Save ]   │
└──────────────────────────────────┘
```

- Today's values are read-only text, not disabled controls.
- Each slider runs from 5 to `45 − other app`. If the other app already has 45, *Allow* is disabled with the hint
  "Lower YouTube's allowance first."
- *Block completely* is its own option, not slider position 0, so the strictest choice is always one tap.
- Saving shows a confirmation with the date the change starts. A saved change can be edited again until midnight.
- TalkBack reads a slider as "Instagram Reels from tomorrow, 20 minutes, maximum 30."

### Block card

```
┌──────────────────────────────────┐
│                                  │
│                ⏸                 │
│        Reels are blocked         │
│                                  │
│    Your feed, messages and       │
│    stories still work.           │
│                                  │
│    [ Continue in Instagram ]     │
│                                  │
└──────────────────────────────────┘
 Allowance used up: "You've used today's 20 min of Reels. They're back tomorrow."
 Opened from messages: adds "Reels in messages are blocked too."
```

- Audio stops and the card appears within ~150 ms of detection (target); underneath, the app is already leaving the
  Reels screen.
- The card covers the screen and catches touches, so swiping underneath is impossible during the back-out.
- *Continue in Instagram* dismisses it; the user is on the screen they came from.
- No link to the allowance editor: changes only apply tomorrow, so it would not help in this moment.
- Announced by TalkBack; light haptic tick.

### Timer pill

```
          ┌──────────────┐
          │ ⏱ 4:32 left  │     top centre, below the status bar
          └──────────────┘     amber from 1:00; tap to hide for this session
```

Shown only while watching within an allowance. The overlay window is the size of the pill, so it never blocks taps
on the app.

### Settings

```
Daily allowance                 ›
Show time left while watching   ●
Test blocking                   ›   same flow as onboarding "Try it"
Keep running on this phone      ›   phone-maker guide
About                           ›   version, source code, licences,
                                    tested app versions (Instagram 4xx ✓)
```

Debug builds can record the current screen through adb (`SnapshotRecorder`), with view ids, classes and bounds only,
to update the rules and their fixtures.

### Notification

One notification only: **"Blocking is off. Tap to turn it back on."** Posted by the 15-minute check while the
accessibility service is off; it opens the *Turn on* screen.

## 7. Acceptance criteria

| Area | Target |
|---|---|
| Speed | Audio stopped and card shown within 300 ms of a Reels/Shorts screen appearing (95th percentile) |
| False positives | None on feed, stories, messages, posting, profiles, search, comments or normal videos |
| Coverage | Every entry point in section 3, on the installed Instagram and YouTube versions |
| Time accounting | Within 5 s per session; correct across midnight, screen off, app switches and time zone changes |
| Battery | Under 1 % per day from the service with normal use |
| Size | APK under 6 MB; service memory under 30 MB |
| Accessibility | TalkBack pass; 200 % font size; right-to-left pseudo-locale |
| Privacy | Works with no internet permission |

## 8. Milestones

Each milestone is a series of small commits; see [engineering.md](engineering.md). M1 to M4 landed together in one
pull request.

| Milestone | Scope | Done when |
|---|---|---|
| M0 Foundation | Gradle project with build conventions and the app module, version catalog, formatting, detekt, Android Lint, the no-internet check, CI, ADRs | An empty app builds and every check runs green in CI |
| M1 Detection | Record screens from the phone, sanitise them into fixtures, rule format, `SurfaceDetector`, a logging-only service | A day of normal use logs every entry point with no false hits |
| M2 Budget | Allowances, schedule, ledger, `BlockPolicy`, time handling | Every rule in section 2 has tests |
| M3 Enforcement | Audio focus, block card, back-out sequence, timer pill, usage recording | Every entry point is blocked within the speed target |
| M4 Interface | Onboarding, Home, Daily allowance, Settings, notification | Install to "it works" in under two minutes |
| M5 Release | Device checks, battery, accessibility pass, README screenshots, v1.0.0 | Every row in section 7 passes |

M1 to M4 were checked on the author's phone: every recorded entry point is detected, the Reels and Shorts tabs are
blocked, the timer pill counts down, and onboarding runs to a working block. Two "done when" targets are measured in
M5 with the other device checks: a day of normal use without false hits, and the speed target.

## 9. Risks

| Risk | Mitigation |
|---|---|
| Instagram or YouTube changes internal view ids | Several signals per screen, canaries, fixture tests per app version, fast release of a fixed APK |
| A/B tests show different layouts | Rules allow alternative signals; new fixtures when a variant appears |
| The phone stops the service | Phone-maker guide, 15-minute check and notification |
| Accessibility API behaviour changes in a new Android version | Target the newest SDK, test on Android betas before they ship |
