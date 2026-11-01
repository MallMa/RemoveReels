# 0006: Quality gates and their tools

Status: Accepted · 2026-11-01

## Context

Code quality is the project's first priority, so the standards in [engineering.md](../engineering.md) have to be
enforced by tools rather than by memory. The project uses Kotlin 2.4.

## Decision

`./gradlew check` runs every gate, and none has a baseline:

- **Kotlin compiler** with warnings as errors.
- **Spotless with ktlint** (official code style, 120 columns) for Kotlin and Gradle scripts; whitespace rules for
  other text files.
- **detekt 2.0** with type resolution, so rules such as the ban on `System.currentTimeMillis` work. The 2.0 line is
  still in alpha, but detekt 1.x stopped at Kotlin 2.0 and cannot analyse this code base. The version is pinned
  exactly.
- **Android Lint** with warnings as errors. Only the "newer version available" checks are disabled, because Dependabot
  proposes updates as pull requests.

Gradle dependency verification is not used: Dependabot cannot update its metadata file, so every dependency update
would fail. The Gradle wrapper's distribution checksum is pinned, and CI validates the wrapper.

## Consequences

- A change cannot reach `main` with formatting, style, Lint or compiler warnings.
- The detekt alpha may change its configuration format between releases; the pinned version moves only in deliberate
  updates, and the project switches to detekt 2.0 stable once it is released.
- Supply-chain protection relies on pinned versions, the wrapper checksum and reviewed Dependabot updates.
