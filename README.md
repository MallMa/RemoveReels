# RemoveReels (code name)

A free, open-source way to use Instagram and YouTube without Reels, Shorts and the endless recommendation feeds,
while keeping messages, profiles, search and posting. The plan is an Android app first, then iOS and a browser
extension, all sharing one rule engine and one set of community-maintained rule lists. No accounts, no servers, no
tracking.

**Status: feasibility spike.** The question this spike has to answer: *how often do the rules break when Instagram and
YouTube change their websites?* That number decides whether a free, volunteer-maintained project can keep up.

## How it works

The app loads each platform's own mobile website in a WebView and injects [engine/engine.js](engine/engine.js) at
document start. The engine reads a rule list ([rules/](rules/)) and applies rules in layers, most robust first:

1. **Route rules** look at the URL. `/shorts/<id>` becomes `/watch?v=<id>` (one video, no swiping), the Instagram Reels
   feed shows a block screen, and moving from one reel to the next is blocked so a reel someone sent you still plays.
   URLs change far less often than page markup.
2. **Hide rules** hide elements with CSS, using stable hooks (`href` patterns, custom element names, `aria` attributes)
   rather than the sites' scrambled class names.
3. **hideClosest / hideText rules** hide the card around a matching link, or a post containing a phrase such as
   "Suggested for you".

Videos never autoplay (`mediaPlaybackRequiresUserGesture`), which also saves mobile data.

Every rule reports on-device how often it matched. Rules marked `canary` should match on every page where they apply
(e.g. the Shorts tab button), so when one stops matching, the **Rules** screen flags it as possibly broken. Nothing is
sent anywhere.

## Repository layout

| Path | What |
|---|---|
| [rules/](rules/) | Rule lists, one JSON file per platform. Shared by every app. |
| [engine/](engine/) | The rule engine (one dependency-free JS file) and its tests, which also validate every rule list. |
| [android/](android/) | Android app (Kotlin, one WebView per platform). Bundles `engine.js` and `rules/*.json` as assets at build time. |
| [scripts/setup-android-toolchain.ps1](scripts/setup-android-toolchain.ps1) | Installs JDK 21, the Android SDK and Gradle under `%USERPROFILE%\tools` (no admin rights). |

## Build and run

```powershell
powershell -ExecutionPolicy Bypass -File scripts\setup-android-toolchain.ps1   # once; accepts the Android SDK license
cd android
.\gradlew.bat assembleDebug
```

The APK lands in `android/app/build/outputs/apk/debug/app-debug.apk`. To install it, enable USB debugging on the phone,
connect it, and run:

```powershell
& "$env:USERPROFILE\tools\android-sdk\platform-tools\adb.exe" install -r android\app\build\outputs\apk\debug\app-debug.apk
```

Or open the `android/` folder in Android Studio.

Engine tests (Node 20+):

```powershell
cd engine
npm install
npm test
```

## Rule format (schema 0)

Each `rules/<platform>.json` has `platform`, `name`, `version`, `startUrl`, `homePath`, `origins` (where the engine is
injected) and `hosts` (domains that stay inside the app; other links open in the browser). Each rule has `id`, `name`,
`type`, optional `default` (on unless `false`) and optional `routes` (a regex on path + query that limits where it applies).

| `type` | Fields | Effect |
|---|---|---|
| `route` | `match` (regex), `action` (`block` / `redirect`), `to` (path with `$1`…), `from` (regex on the previous URL), `on` (`any` / `load` / `spa`), `message` | Blocks or redirects matching URLs, on full loads and in-app navigation |
| `hide` | `selector`, `canary`, `canaryWhen` (selector that must exist for the canary check to count) | Hides matching elements with CSS |
| `hideClosest` | `match`, `closest` | Hides the nearest `closest` ancestor of each `match` (or the element itself) |
| `hideText` | `container`, `text` (list) | Hides containers whose text contains any of the phrases |

`npm test` checks every list: unique ids, valid regexes and selectors, required fields.

### Fixing a rule

Debug builds allow remote debugging: connect the phone, open `chrome://inspect` in desktop Chrome, inspect the app's
WebView, find a stable hook for the element, update the rule list, run `npm test`, rebuild.

## Spike test plan

Use the debug build daily for 2–3 weeks with your own accounts and record:

- [ ] Instagram login works in the WebView, and stays logged in after restarting the app
- [ ] DMs: reading, replying, and opening a reel someone sent (plays; swiping to the next shows the block screen)
- [ ] Posting a photo (file picker) and a story
- [ ] Reels tab, Explore grid and suggested posts are gone; search still works
- [ ] YouTube: Shorts tab and Shorts in search are gone, a Shorts link opens as a normal video
- [ ] **Rules screen once a week: which rules show "possibly broken", and the date each broke.** This is the main result.

## Known limitations of the spike

- **No Google sign-in for YouTube.** Google blocks sign-in inside embedded WebViews. The app does not disguise itself
  to get around that, so YouTube runs signed out (search and watching work, subscriptions don't).
- **No notifications.** A WebView only runs while the app is open.
- **Selectors for logged-in Instagram are unverified.** Instagram's logged-in pages could not be inspected while
  building this, so `ig-reels-nav`, `ig-explore-grid` and `ig-suggested-posts` are educated guesses based on links and
  text. The Rules screen will show whether they match. `ig-suggested-posts` only knows English phrases.
- YouTube's Shorts tab (`.pivot-shorts`) and Shorts-in-search (`ytm-video-with-context-renderer`) hooks were confirmed
  on the live mobile site on 2026-10-31; the home-feed and watch-page hooks were not (YouTube showed a bot check).
- The engine is tested in jsdom against small copies of the sites' structure, not yet inside a real WebView.
- Rule lists are bundled in the APK; there is no remote update yet.
- No fullscreen video, no per-account profiles, no localization, placeholder icon, code-name package
  `org.removereels.app`.

## Next steps

1. Run the spike test plan above.
2. Remote rule updates: signed rule lists on GitHub Pages with mirrors, fetched daily, falling back to the bundled copy.
3. One-tap "this rule is broken" report that opens a pre-filled GitHub issue (no telemetry server).
4. Native guard mode (AccessibilityService) so the real Instagram and YouTube apps can be used with their Reels and
   Shorts screens blocked, which brings back posting and notifications.
5. Pick the real name, a license and the package id before the first public release.
