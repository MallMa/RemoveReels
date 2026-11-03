# 0008: Cover Reels and Shorts entry points with overlay windows

Status: Accepted · 2026-11-03

## Context

The MVP blocks a Reels or Shorts screen once it opens, but the ways in stay visible: the Reels tab, Shorts shelves,
Reels previews in feeds. They invite a tap that the block then undoes. An accessibility service can read another app's
views and draw its own windows over them, but it cannot remove or change them.

## Decision

While an app is blocked, RemoveReels covers each of its Reels or Shorts entry points with a small accessibility-overlay
window that hides it and swallows taps. Entry points are rules in the same data files as the surfaces, found by view id
or by position inside a container, never by text. A rule that does not find exactly the layout it expects covers
nothing, and the full-screen block remains the safety net.

Fixed entry points come first. Covers for entry points that move with scrolling wait for a spike that measures how
closely they can follow ([hiding plan](../hiding-plan.md)).

## Consequences

- No new permission: accessibility overlays come with the service.
- While today's allowance has time left, entry points stay visible so that the allowance can be used.
- A cover in the wrong place would hide normal content, so rules are conservative and tested against recorded screens,
  including screens where nothing may be covered.
- Rules that use positions need recorded screens that keep the view tree, which the snapshot format now does.
