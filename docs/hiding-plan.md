# Hiding Reels and Shorts entry points

Status: H1 done, H0 next · Last updated: 2026-11-03

The MVP blocks a Reels or Shorts screen once it opens. This feature goes further: the ways in, such as the Reels
tab or a Shorts shelf, disappear from Instagram and YouTube, so they cannot be seen or tapped.

## What Android allows

An accessibility service can read another app's screen, act on it (back, tap, scroll) and draw its own windows on top.
It cannot remove or change a view inside another app. Hiding therefore means **covering** each entry point with a small
window of our own while it is on screen; the cover hides it and swallows taps on it
([ADR 0008](adr/0008-cover-entry-points-with-overlays.md)).

| Alternative | Why not |
|---|---|
| Tap "Not interested" or "Show fewer Shorts" automatically | Changes the user's account and recommendations; YouTube's lasts 30 days; Instagram has nothing like it for the Reels tab |
| Scroll past previews automatically | The feed would jump under the user's thumb |
| Patched apps | Rejected in [ADR 0001](adr/0001-block-with-an-accessibility-service.md) |

The full-screen block stays as the safety net for anything a cover misses.

## Rules

- **Covered only while the app is blocked.** While today's allowance has time left, the entry points stay visible, so
  the allowance can be used.
- **Never cover by guess.** Covering a normal post is worse than missing a Reel. A rule that does not find exactly the
  layout it expects covers nothing.
- **No text.** Entry points are found by view id, class, bounds and position in the view tree; text and descriptions
  are never read, as in the rest of the app.

## Entry points

What the screens recorded for the MVP (Instagram 453, YouTube 21.38) show:

| App | Entry point | How it is found | Moves |
|---|---|---|---|
| Instagram | Reels tab in the bottom bar | `clips_tab` | No |
| Instagram | Reels tiles in Explore | `preview_clip_play_count` inside each `grid_card_layout_container` | With the grid |
| Instagram | Reels tab on profiles | second item of `profile_tab_layout` | With the profile |
| Instagram | Reels in the home feed | No distinct id yet; needs new recordings | With the feed |
| Instagram | Reels shared in chats, suggested-Reels carousels | Not recorded yet | With the list |
| YouTube | Shorts tab in the bottom bar | second labelled item of `pivot_bar`; its items have no ids | No |
| YouTube | Shorts shelves on Home, Subscriptions, search and watch pages | No ids at all; a horizontal row of portrait tiles | With the feed |
| YouTube | Shorts tab and grid on channels | Not recorded yet | With the channel |

## Design

1. **Rules stay data.** Each rule file gains `entryPoints`: an entry point is found by view id, or as an item at a
   position inside a container with a fixed number of items. Later phases add a shape rule (portrait tiles in a
   horizontal row) for YouTube's shelves, which have no ids.
2. **Snapshots keep the tree.** Format 2 records every view, with or without an id, and its parent, so rules that use
   positions can be tested against recorded screens.
3. **Covers** are small accessibility-overlay windows of plain Views, coloured like the app bar they cover, clipped to
   the screen, and hidden while the block card shows.
4. **Moving entry points** need covers that follow scrolling: shift by each scroll event's delta at once, then read the
   real bounds again. A cover that swallows touches also stops the feed from scrolling where the thumb lands, so drags
   either pass through or are replayed into the app. A spike decides both.
5. **Battery.** Scroll events are far more frequent than window changes. Lookups stay inside the containers the rules
   name. The budget stays the MVP's: under 1 % per day.

## Phases

| Phase | Scope | Done when |
|---|---|---|
| **H1 Fixed tabs** (done) | Rule format, snapshot format 2, covers; Instagram's Reels tab and YouTube's Shorts tab | Neither tab can be seen or tapped on the phone while the app is blocked; fixture tests |
| H0 Spike | Moving covers on Explore tiles and the profile tab row: lag while scrolling at 120 Hz, battery, drags over covers, audio of covered videos | A recorded decision on moving covers |
| H2 Instagram, moving | Profile Reels tab, Explore and profile grids, feed Reels, chats, carousels | Every recorded Reel is covered and no normal post is |
| H3 YouTube, moving | Channel Shorts tab, Shorts shelves, search tiles, channel grid | The same for YouTube |
| H4 Polish | TalkBack, landscape, split screen, large fonts; About lists what is hidden | The MVP's acceptance criteria still pass |

## H1 on the phone

- Both tabs disappear: each cover takes the colour of its bar (`#0C1014` for Instagram, `#0F0F0F` for YouTube in the
  dark theme), and YouTube's 1 dp divider along the top of its bar stays visible.
- Tapping where a tab was does nothing; no block card appears.
- Leaving either app removes the covers: both apps send an event as they go to the background, and the guard also
  looks again every second while covers show.
- Not checked yet: the light theme's colours (assumed white for both bars), landscape, and the profile and channel tabs,
  which move with scrolling and belong to H2 and H3.

## Risks

| Risk | Mitigation |
|---|---|
| Covers trail fast scrolling | Delta shifts and margins; otherwise cover only while still (H0) |
| A layout change makes a rule cover the wrong view | Exact item counts, fixtures that must not be covered, canaries |
| A covered Reel in the feed still plays sound | Decided in H0; holding audio focus would also mute normal videos |
| Replaying drags needs Android's "perform gestures" capability | Decided in H0; the consent screen would then list it |
