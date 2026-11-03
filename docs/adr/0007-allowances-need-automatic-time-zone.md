# 0007: Allowances also need the automatic time zone

Status: Accepted · 2026-11-03 · Amends [0003](0003-allowance-changes-start-tomorrow.md)

## Context

ADR 0003 makes allowances depend on Android's automatic date and time, so that moving the clock forward cannot
unlock tomorrow's allowance early. A review found a second way to move the date: with automatic time still on, the
time zone can be chosen by hand. Picking UTC+14 in Vienna in the afternoon makes it tomorrow, with tomorrow's
allowance, on the same real day.

## Decision

Allowances apply only while both automatic date and time and automatic time zone are on. Otherwise both apps are
fully blocked, and the block card and Home say which settings to turn on.

## Consequences

- Travelling still works: the automatic time zone follows the network, and each local day gets its allowance.
- Someone who prefers a manual time zone has no allowances, only full blocking.
- The rest of ADR 0003 stands.
