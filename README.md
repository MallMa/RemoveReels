# RemoveReels

[![CI](https://github.com/MallMa/RemoveReels/actions/workflows/ci.yml/badge.svg)](https://github.com/MallMa/RemoveReels/actions/workflows/ci.yml)

An Android app that blocks Reels inside the real Instagram app and Shorts inside the real YouTube app, while feed,
stories, messages, posting, search and normal videos keep working. An optional daily allowance per app, at most 45
minutes combined, lets some through.

**Status:** foundation (milestone M0). The app builds and every quality gate runs; nothing is blocked yet.

- [MVP plan](docs/mvp-plan.md): product rules, how blocking works, tech stack, architecture, UI/UX, milestones.
- [Engineering standards](docs/engineering.md): code quality rules, tests, commits, CI.
- [Architecture decisions](docs/adr/README.md): why the project is built the way it is.

## How it works

RemoveReels uses Android's Accessibility API to recognise the Reels and Shorts screens inside the two apps. When one
opens without allowance left, it pauses the video, shows a short notice and returns to the previous screen. It only
looks at Instagram and YouTube, saves nothing it sees, and has no internet permission.

There is no off switch: uninstalling is the only way to stop it.

## Building

Requirements: JDK 21 and the Android SDK (`ANDROID_HOME`). The Gradle wrapper downloads everything else.

```sh
./gradlew check           # formatting, detekt, Lint, unit tests, permission check: the same as CI
./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
./gradlew spotlessApply   # fixes formatting
```

| Path | Contents |
|---|---|
| `app/` | The Android app |
| `build-logic/` | Gradle convention plugins with the shared build configuration and checks |
| `config/detekt/` | detekt settings on top of the defaults |
| `docs/` | Plan, engineering standards, architecture decisions |
