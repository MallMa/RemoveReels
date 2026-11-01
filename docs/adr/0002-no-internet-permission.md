# 0002: No internet permission

Status: Accepted · 2026-11-01

## Context

The accessibility service ([0001](0001-block-with-an-accessibility-service.md)) can see what Instagram and YouTube
display, including messages. Nothing in the product needs a server: rules ship with the app, and settings and usage
stay on the phone.

## Decision

The app never requests `android.permission.INTERNET`. Cloud backup and device-to-device transfer of app data are
disabled as well. A build check fails if the merged manifest of any variant requests the permission, including through
a library.

## Consequences

- Android itself guarantees that nothing the service sees can be sent anywhere.
- No analytics or automatic crash reports. Problems are diagnosed with `adb logcat` and the debug build's detection log.
- Rule fixes for changed Instagram or YouTube layouts ship as app updates, not as downloaded rule lists.
- Every new dependency has to work without network access, or the build fails.
