# 0005: Manual dependency injection

Status: Accepted · 2026-11-01

## Context

The accessibility service, the activity and the background worker share about a dozen long-lived objects: the
store, the clocks, the detector, the block policy.

## Decision

A hand-written `AppContainer`, created once in the `Application` class, constructs these objects and hands them to
the service, the ViewModels and the worker. Classes receive their dependencies through constructors.

Alternatives considered: Hilt (annotation processing and generated code for a small object graph) and Koin (runtime
resolution, so wiring errors appear only when the app runs).

## Consequences

- The whole object graph is visible in one file, and wiring errors are compile errors.
- No extra build time or library.
- Tests construct the classes they need directly, with fakes.
- If the graph grows well beyond its current size, this decision should be revisited.
