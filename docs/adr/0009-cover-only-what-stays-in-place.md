# 0009: Cover only what stays in place

Status: Accepted · 2026-11-09

## Context

[ADR 0008](0008-cover-entry-points-with-overlays.md) covers Reels and Shorts entry points with overlay windows and left
open whether covers can follow entry points that scroll: Reels tiles in Explore, the Reels tab on profiles, Reels in the
home feed, YouTube's Shorts shelves. The H0 spike measured that on the Galaxy S23 Ultra (120 Hz) with Instagram 453
and 454. A debug-only probe covered the Reels tiles in Explore and the Reels tab on a profile while adb scrolled them,
and logged how far each cover was from its target; it lives on the unmerged branch
[`spike/moving-covers`](https://github.com/MallMa/RemoveReels/tree/spike/moving-covers).

| What | Result |
|---|---|
| Scroll updates from Instagram | One every 107 ms (median over five runs); Android sends them at most every 100 ms. The profile page sends none, only content changes |
| Covers moved on updates alone | 29 px off (median), 354 px (95th percentile), up to 541 px |
| Covers moved on every frame | 5–9 px off (median), 88–102 px (95th percentile), up to 389 px when a scroll starts, before the first update |
| Asking for one position | 2–3.7 ms (median), 9–10.5 ms (95th percentile), answered on Instagram's UI thread; a frame lasts 8.3 ms |
| Finding the Reels tiles again | 23–40 ms (median), five lookups by view id |
| Drawing every cover in one overlay | 9 ms per frame (median), 40 ms (90th percentile) |
| One window per cover, as in H1 | 69 ms per frame (median); covers fell up to 1,247 px behind |
| Instagram's CPU time while scrolling | 44–67 % more than without the probe; its share of janky frames did not change measurably |
| Watched on the phone | Covers visibly trailed the tiles during flings, with either way of drawing |
| Swipes that start on a cover that swallows touches | Nothing scrolls |
| Explore's grid | Every visible tile was a Reel: 42 tiles in three recordings over six days. Its moving previews play without sound |

Covers that follow scrolling are always at least one position request behind, one request in twenty takes longer than
a frame, and the start of every scroll goes unnoticed for up to 100 ms.
A Reel shows at the edge of its cover during every fling. A cover that swallows touches would also stop the feed from
scrolling; one that lets touches through no longer stops a tap on the Reel under it.

## Decision

RemoveReels never moves a cover to follow scrolling. While an app is blocked:

- **Fixed entry points are covered**, as in H1: the Reels and Shorts tabs in the bottom bars.
- **A region that shows only Reels is covered as a whole**, with one cover that swallows touches and therefore cannot
  scroll: Instagram's Explore grid, below its search bar. Explore keeps working for search.
- **A tab that lists only Reels is left at once**: when the Reels tab on an Instagram profile is selected, the guard
  selects the first tab again; the Shorts tab on YouTube channels will work the same way.
- **Everything else stays visible**, such as Reels in the home feed and in chats, and YouTube's Shorts shelves; opening
  one is blocked as in the MVP.

## Consequences

- The service keeps its event types and does no work per frame, so the MVP's battery budget holds.
- Reels in feeds and Shorts shelves can still be seen, though not watched. The hiding plan says so instead of promising
  that every entry point disappears.
- Explore's few normal posts are hidden along with its Reels while the app is blocked. The cover follows a rule that
  must match Explore's layout exactly, so it still covers nothing by guess.
- Selecting the first tab is the guard's first action inside Instagram other than backing out of a blocked screen. It
  uses the accessibility click on that tab, so no gesture permission is needed.
- The probe is not merged; its branch keeps the code and scripts that produced the numbers above.
