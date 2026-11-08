# OTC — Outwork the Challenge

Android workout app, English UI, dark theme, starting at Level 2.

## Build an APK with Codemagic

1. Sign in to [Codemagic](https://codemagic.io/).
2. Add application → connect GitHub → select `Increate-AD/OTC-android`.
3. Select the Android/native project if asked.
4. Ensure the `main` branch is selected and Codemagic detects the root `codemagic.yaml`.
5. Start the `OTC Android APK` workflow.
6. When the build succeeds, open Artifacts and download the APK.
7. Transfer the APK to your Android phone and install it. Android may ask you to allow installation from that source.

## Current MVP scope

- Level 2 training days and guided recovery days.
- Actual set-result entry and local set counters.
- Basic rest timer with skip.
- Workout completion count and simple streak counter.
- Level 2 test evaluation.
- Local storage using Android SharedPreferences.

## Not yet included

- Custom illustrated Roman Reigns-inspired logo and exercise GIF library.
- Full edit/delete workout history, export/import backup.
- Timer pause/manual adjustment and per-set detailed history.
- Complete Levels 3–6 and Master plans/unlock flow.
- Fine-grained recovery-day streak rules and calendar rescheduling.

This is an initial functional MVP, not a finished feature-complete release. Debug APKs are suitable for testing; prepare a signed release build before wider distribution.
