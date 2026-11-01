# 0001: Block Reels and Shorts with an accessibility service

Status: Accepted · 2026-11-01

## Context

Reels and Shorts must be blocked inside the official Instagram and YouTube apps while everything else in those apps
keeps working. Android has no API for changing another app's interface. The options considered:

- **UsageStats** reports which app is in front, not which screen, so it cannot tell Reels from messages.
- **VPN or DNS filtering** cannot separate Reels from the feed: both load video from the same servers.
- **Patched APKs** replace the official apps, break on every update and modify code the project has no rights to.
- **A web wrapper** around the mobile websites (tried in the `spike/webview-wrapper` branch) loses features such as
  posting stories, notifications and Google sign-in.

## Decision

An `AccessibilityService`, limited to the packages `com.instagram.android` and `com.google.android.youtube`, recognises
the Reels and Shorts screens by their internal view ids and blocks them: it takes audio focus to stop playback, draws
an overlay explaining the block, and navigates back.

## Consequences

- The real apps keep every feature, including posting and notifications.
- Detection depends on the apps' internal view ids, which can change with any update. Rules therefore live in data
  files with tests against recorded screens, and canary checks report when they stop matching.
- The user has to enable the service in Android's settings, and Android 13+ adds a "restricted setting" step for apps
  installed from a downloaded file.
- The service can read what those two apps show, which raises the bar for privacy; see
  [0002](0002-no-internet-permission.md).
