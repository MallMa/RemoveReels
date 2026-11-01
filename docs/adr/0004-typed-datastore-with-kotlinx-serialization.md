# 0004: Typed DataStore with kotlinx.serialization

Status: Accepted · 2026-11-01

## Context

The app stores the allowance schedule, seven days of usage per app and a few flags: well under a kilobyte. The
accessibility service writes usage every few seconds while Reels or Shorts are on screen, and the UI observes the
same data.

## Decision

One `@Serializable` state class, persisted with Jetpack DataStore through a custom `Serializer` that encodes JSON with
kotlinx.serialization.

Alternatives considered: Room (a database for data that is never queried), SharedPreferences (no transactions,
synchronous disk access) and Proto DataStore (protobuf schema and code generation for a handful of fields).

## Consequences

- Updates are atomic and typed, and the UI observes them as a `Flow`.
- No annotation processing or code generation beyond the serialization compiler plugin.
- Each update rewrites the whole file, which is negligible at this size.
- Schema changes need a version field and migration code in the serializer.
