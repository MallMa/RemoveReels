# Engineering standards

Code quality is this project's first priority. Every rule below is enforced by a tool where one exists, so it holds
without anyone having to remember it.

## Principles

- **Logic in pure Kotlin, Android as thin glue.** Decisions live in `:detection` and `:budget`; `:app` only adapts
  Android APIs to them.
- **Small and explicit.** Short functions, descriptive names, no clever abstractions, no speculative generality.
- **Nothing dead.** No commented-out code, no unused code, no `TODO` without a linked issue.
- **Fakes over mocks.** Dependencies are interfaces with hand-written fakes; no mocking library.
- **Injected time.** Logic never reads the system clock; it receives a `Clock` or `TimeSource`.
- **Warnings are errors.** A warning is either fixed or suppressed with a comment that explains why.

## Toolchain

- JDK 21, Gradle 9.8, Android Gradle Plugin 9.4 and Kotlin 2.4: the latest stable versions when M0 started.
- All versions in `gradle/libs.versions.toml`; no versions in build scripts.
- Shared build configuration lives in convention plugins in `build-logic`.
- The Gradle wrapper pins the SHA-256 checksum of its distribution. Gradle dependency verification is not used
  ([ADR 0006](adr/0006-quality-gates.md)).

## Static analysis

All checks run in CI, all must pass, and none has a baseline: the project starts clean and stays clean.

| Tool | What it checks | Setting |
|---|---|---|
| Kotlin compiler | Types, nullability, deprecated APIs | `allWarningsAsErrors = true`; `explicitApi()` in `:detection` and `:budget` |
| ktlint via Spotless | Formatting of Kotlin and Gradle scripts; trailing whitespace and final newlines elsewhere | `./gradlew spotlessApply` fixes everything it reports |
| detekt 2.0 (type resolution) | Complexity, naming, unused code, likely bugs, forbidden calls (`System.currentTimeMillis`, `Thread.sleep`, `println`) | Defaults plus `config/detekt/detekt.yml` |
| Android Lint | Android-specific problems, accessibility of our own UI | `warningsAsErrors = true`; the "newer version available" checks are disabled because Dependabot handles updates |
| Gradle wrapper validation | The wrapper jar is the official one | GitHub Action |

## Tests

| Where | How |
|---|---|
| `:detection`, `:budget` | JUnit 5 + Truth. Table-driven where it helps. Kover enforces at least 90 % line coverage. |
| Detection rules | Fixture tests: recorded screens (`uiautomator dump`) per app version, with every text and description stripped by a sanitiser script so no personal data is committed. Each rule has screens that must match and screens that must not. |
| `:app` ViewModels | JVM tests with fakes and `kotlinx-coroutines-test`. |
| `:app` UI | Compose UI tests on Robolectric, so CI needs no emulator. The allowance editor's 45-minute logic is covered here. |
| On the phone | A short manual checklist per release: every entry point from the plan, plus messages, posting and normal videos. |

## Commits

- [Conventional Commits](https://www.conventionalcommits.org): `type(scope): summary`.
  Types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`, `perf`.
  Scopes: `detection`, `budget`, `guard`, `ui`, `storage`, `deps`, or none.
- Subject in the imperative, at most 72 characters; the body explains *why* when it isn't obvious.
- One logical change per commit, and every commit builds and passes the checks, so `git bisect` always works.
- Commits written together with Claude carry a `Co-Authored-By` trailer.

## Branches and pull requests

- `main` is always releasable. It is protected: pull request required, all CI checks required.
- Work happens on short-lived branches named `feat/…`, `fix/…`, `docs/…`.
- One pull request per task. The description says what changed, why, and how it was verified (with a screenshot for
  UI changes).
- Pull requests are merged with a merge commit, not squashed, so the branch's commits reach `main` unchanged and
  stay grouped under a merge that carries the pull request's title.

## CI

One GitHub Actions workflow runs on every pull request and every push to `main`:

1. Validate the Gradle wrapper.
2. `./gradlew check`: formatting, detekt, Lint, unit tests and the forbidden-permission check, plus Kover once the
   pure modules exist.
3. `./gradlew assembleRelease`, which also runs R8.

Running `./gradlew check` locally gives the same result as CI. Actions are pinned to commit SHAs. Dependabot opens grouped weekly updates for Gradle dependencies and Actions.

## Releases

- Semantic versioning; tags like `v1.0.0`.
- The release keystore lives outside the repository.
- Each GitHub Release has the APK and short notes generated from the commit subjects.

## Documentation

- `README.md`: what the app does, screenshots, architecture overview, how to build.
- `docs/mvp-plan.md`: the product specification.
- `docs/adr/`: one Architecture Decision Record per significant decision (context, decision, consequences). The
  first ones, written in M0: accessibility service as the mechanism, no internet permission, changes start
  tomorrow, typed DataStore with kotlinx.serialization, manual dependency injection.
- KDoc on the public API of the pure modules.

## Definition of done

A pull request is ready to merge when:

- [ ] CI is green: formatting, detekt, lint, tests and coverage.
- [ ] New logic has tests; detection changes include fixtures.
- [ ] No new warnings; any suppression has a comment that explains it.
- [ ] Docs or an ADR are updated if behaviour or a decision changed.
- [ ] UI changes include a screenshot and were checked with TalkBack and at 200 % font size.
- [ ] The title follows Conventional Commits.
