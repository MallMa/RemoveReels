# Hiding Reels and Shorts entry points

Status: H2 built, its phone check open · Last updated: 2026-11-11

The MVP blocks a Reels or Shorts screen once it opens. This feature goes further: while an app is blocked, the ways in
that stay in place, such as the Reels tab or Explore's grid, disappear from Instagram and YouTube, and tabs that list
only Reels are left at once. Reels inside feeds stay visible but are blocked when opened, because covers cannot follow
scrolling closely enough ([ADR 0009](adr/0009-cover-only-what-stays-in-place.md)).

## What Android allows

An accessibility service can read another app's screen, act on it (back, tap, scroll) and draw its own windows on top.
It cannot remove or change a view inside another app. Hiding therefore means **covering** each entry point with a small
window of our own while it is on screen; the cover hides it and swallows taps on it
([ADR 0008](adr/0008-cover-entry-points-with-overlays.md)).

| Alternative | Why not |
|---|---|
| Tap "Not interested" or "Show fewer Shorts" automatically | Changes the user's account and recommendations; YouTube's lasts 30 days; Instagram has nothing like it for the Reels tab |
| Scroll past previews automatically | The feed would jump under the user's thumb |
| Covers that follow scrolling | They trail the content by up to hundreds of pixels during flings ([ADR 0009](adr/0009-cover-only-what-stays-in-place.md)) |
| Patched apps | Rejected in [ADR 0001](adr/0001-block-with-an-accessibility-service.md) |

The full-screen block stays as the safety net for anything a cover misses.

## Rules

- **Covered only while the app is blocked.** While today's allowance has time left, the entry points stay visible, so
  the allowance can be used.
- **Nothing follows scrolling.** A cover hides an entry point that stays in place, or a whole region that shows only
  Reels and is not scrolled while covered.
- **Never cover by guess.** Covering a normal post is worse than missing a Reel. A rule that does not find exactly the
  layout it expects covers nothing.
- **No text.** Entry points are found by view id, class, bounds and position in the view tree; text and descriptions
  are never read, as in the rest of the app.

## Entry points

What the recorded screens show (Instagram 453 and 454, YouTube 21.38), and what happens to each while the app is
blocked:

| App | Entry point | How it is found | While blocked |
|---|---|---|---|
| Instagram | Reels tab in the bottom bar | `clips_tab` | Covered (H1) |
| Instagram | Explore grid | `recycler_view` holding a `preview_clip_play_count`, below the search bar `explore_action_bar`; at the top of Explore 10 of 12 tiles were Reels in 454, all in 453 | Covered as a whole (H2) |
| Instagram | Reels tab on profiles | `clips_grid_recyclerview` showing inside `profile_viewpager`; other people's profiles open on it | The pager turns back to the tab before (H2) |
| Instagram | Reels in the home feed | No distinct id yet | Visible; blocked when opened |
| Instagram | Search screen before a search | `recycler_view` without play counts and without the Explore bar | Not covered |
| Instagram | Reels shared in chats, suggested-Reels carousels, search results | Not recorded yet | Visible; blocked when opened, until recorded (H2b) |
| YouTube | Shorts tab in the bottom bar | Second labelled item of `pivot_bar`; its items have no ids | Covered (H1) |
| YouTube | Shorts shelves on Home, Subscriptions, search and watch pages | No ids at all; a horizontal row of portrait tiles | Visible; blocked when opened |
| YouTube | Shorts tab on channels | Not recorded yet | The first tab is selected again (H3) |

## Design

1. **Rules stay data.** Each rule file has `entryPoints`: an entry point is found by view id, or as an item at a
   position inside a container with a fixed number of items. A region counts only while it holds a given view, and its
   cover may start below another view (H2). `reelsPages` name lists that show only Reels and the pager they sit in.
   Views that a rule depends on are looked up only once the rule's own view is on screen, since every lookup is a
   request the app answers on its UI thread.
2. **Snapshots keep the tree.** Format 2 records every view, with or without an id, and its parent, so rules that use
   positions can be tested against recorded screens.
3. **Covers** are small accessibility-overlay windows of plain Views, coloured like what they cover, clipped to the
   screen, and hidden while the block card shows. They change only when the screen does, never per frame.
4. **Leaving a tab** turns its pager back a page with the accessibility scroll action, as a swipe would, once each
   time the page comes into view. ADR 0009 expected a click on the first tab; the pager also works when the tab row
   has scrolled away and when a profile has four tabs instead of three, and needs no more permission.
5. **Battery.** No new event types and no work per frame: covers and tab changes ride on the checks the guard already
   makes, within the MVP's budget of under 1 % per day.

## Phases

| Phase | Scope | Done when |
|---|---|---|
| **H1 Fixed tabs** (done) | Rule format, snapshot format 2, covers; Instagram's Reels tab and YouTube's Shorts tab | Neither tab can be seen or tapped on the phone while the app is blocked; fixture tests |
| **H0 Spike** (done) | Covers that follow scrolling, on Explore tiles and the profile tab row | Decided in [ADR 0009](adr/0009-cover-only-what-stays-in-place.md): no cover follows scrolling |
| **H2 Instagram** (built) | Explore's grid covered as a whole; a profile's Reels page left; Instagram 454 recorded | Neither Explore's grid nor a profile's Reels can be seen on the phone while blocked, and no other screen changes |
| H2b Instagram, recorded with the user | Chats with shared Reels, suggested-Reels carousels and search results, which need the user's own chats and searches, recorded and handled by the same rules | The same for those screens |
| H3 YouTube | A channel's Shorts tab left for the first tab; channels and search recorded | The same for YouTube |
| H4 Polish | TalkBack, landscape, split screen, large fonts; About lists what is hidden | The MVP's acceptance criteria still pass |

## H1 on the phone

- Both tabs disappear: each cover takes the colour of its bar (`#0C1014` for Instagram, `#0F0F0F` for YouTube in the
  dark theme), and YouTube's 1 dp divider along the top of its bar stays visible.
- Tapping where a tab was does nothing; no block card appears.
- Leaving either app removes the covers: both apps send an event as they go to the background, and the guard also
  looks again every second while covers show.
- Not checked yet: the light theme's colours (assumed white for both bars) and landscape.

## H0 on the phone

A debug-only probe on the branch
[`spike/moving-covers`](https://github.com/MallMa/RemoveReels/tree/spike/moving-covers) covered Explore's Reels tiles
and a profile's Reels tab while adb scrolled them. [ADR 0009](adr/0009-cover-only-what-stays-in-place.md) lists the
numbers; in short:

- Instagram reports scrolling at most every 100 ms. Asking for positions on every frame brings the typical error down
  to 5–9 px, but covers still trailed by up to 389 px when a scroll started, and visibly during every fling.
- Each request for a position takes 2–10 ms, and Instagram's CPU time grew by about half.
- Covers that swallow touches stop the grid from scrolling; covers that let touches through no longer stop taps.

## Risks

| Risk | Mitigation |
|---|---|
| A layout change makes a rule cover the wrong view | Exact item counts, fixtures that must not be covered, canaries |
| Explore starts showing many normal posts | Recordings of each new app version count Explore's Reels tiles; the rule is revisited when they drop |
| Explore's search bar is collapsed when the cover appears, because the grid was scrolled before | Checked in H2; the cover starts below the bar, wherever its edge is |
| Leaving the Reels tab fights a user who keeps tapping it | One click per selection, only while blocked; the guard never repeats it in a loop |
