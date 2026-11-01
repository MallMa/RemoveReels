# RemoveReels

An Android app that blocks Reels inside the real Instagram app and Shorts inside the real YouTube app, while feed,
stories, messages, posting, search and normal videos keep working. An optional daily allowance per app, at most 45
minutes combined, lets some through.

**Status:** planning. Nothing is built yet.

- [MVP plan](docs/mvp-plan.md): product rules, how blocking works, tech stack, architecture, UI/UX, milestones.
- [Engineering standards](docs/engineering.md): code quality rules, tests, commits, CI.

## How it works

RemoveReels uses Android's Accessibility API to recognise the Reels and Shorts screens inside the two apps. When one
opens without allowance left, it pauses the video, shows a short notice and returns to the previous screen. It only
looks at Instagram and YouTube, saves nothing it sees, and has no internet permission.

There is no off switch: uninstalling is the only way to stop it.
