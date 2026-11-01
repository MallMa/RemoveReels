# 0003: Allowance changes start tomorrow; no off switch

Status: Accepted · 2026-11-01

## Context

The app is a commitment device. The moment someone most wants more Reels is exactly the moment they would raise a
limit or pause blocking, so any setting that loosens the rules immediately defeats the purpose.

## Decision

- Reels and Shorts are blocked by default. Each app can have a daily allowance of 5 to 45 minutes; together at most 45.
- Every change to an allowance, up or down, takes effect at the next local midnight. Today's allowance never changes.
- The app has no setting that pauses or disables blocking. Uninstalling is the only way to stop it.
- Allowances apply only while Android's automatic date and time is on; otherwise both apps are fully blocked.
- If the accessibility service is switched off in Android's settings, the app shows a notification until it is back on.

## Consequences

- There is no in-app escape in a weak moment, by design.
- Moving the clock forward cannot unlock tomorrow's allowance early.
- Android always lets the user disable an accessibility service, force-stop the app or clear its data. The app does
  not fight this. Clearing data resets allowances to "blocked", so it never loosens the rules. Force stop also cancels
  the background check, so the notification cannot appear until the app is opened again.
- The domain model needs a schedule (today's allowances plus a pending change), not a single current value.
