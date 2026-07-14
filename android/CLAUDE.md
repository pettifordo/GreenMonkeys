# Green Monkeys — Android — Claude Code Project Guide

Persistent context for the Android port. Read every session, together with the
repo-root `CLAUDE.md`, `SPEC.md` (product source of truth for both platforms)
and `ANDROID-BRIEF.md` (platform decisions + lessons already paid for).

## What this is

Native Kotlin + Jetpack Compose port of the iOS app. Single Gradle module
(`:app`), applicationId `com.strive4it.greenmonkeys`, minSdk 26, target latest.
The PWA/hybrid route was evaluated and rejected — don't relitigate (brief §1).

## Build & test

The Mac's Homebrew `openjdk` is too new for AGP/Kotlin; use the JDK 21 keg:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
cd android

./gradlew :app:testDebugUnitTest   # unit tests (pure logic lives here)
./gradlew :app:assembleDebug       # debug APK
```

**Android SDK:** installed at `~/Library/Android/sdk` (licenses accepted
2026-07-14, owner-approved) with platform-tools, platforms;android-35 and
build-tools;35.0.0; `android/local.properties` (gitignored) points at it. If
it ever goes missing, reinstall via `sdkmanager --sdk_root=... --licenses`
then the same three packages — sdkmanager comes from the
`android-commandlinetools` brew cask. The `logic/` package has zero Android
imports so it also compiles and tests on plain JVM; keep it that way.

## Layout

```
android/
├── settings.gradle.kts / build.gradle.kts / gradle/libs.versions.toml
└── app/src/main/java/com/strive4it/greenmonkeys/
    ├── MainActivity.kt          # placeholder home
    ├── logic/                   # PURE logic, no Android imports, fully tested
    │   ├── StreakService.kt     # ported from iOS Shared/StreakService.swift
    │   ├── PatternService.kt    # ported from iOS Services/PatternService.swift
    │   └── CharacterVoice.kt    # THE VOICE — see below
    └── ui/theme/Theme.kt
    src/test/java/.../logic/     # 27 tests ported 1:1 from GreenMonkeysTests/
```

Planned packages as features land (brief §4): `data/` (Room entities mirroring
SessionPlan/Commitment/SessionVideo/Verdict — status DERIVED from dates +
verdict, never stored), `notifications/` (AlarmManager exact alarms),
`capture/` (CameraX), `widget/` (Glance).

## Hard rules (same as iOS — root CLAUDE.md rules all apply)

1. Local-only. No analytics, crash SDKs, accounts, or network calls. Ever.
2. Videos never leave the device: `filesDir/videos`, `allowBackup="false"`
   (already set in the manifest — keep it), no share/export, BiometricPrompt
   lock default ON.
3. **`logic/CharacterVoice.kt` is a line-for-line port of iOS
   `GreenMonkeys/Services/CharacterVoice.swift`. The two apps share one voice.
   Never write new copy without the owner; a change to the line banks must land
   on both platforms (or be extracted to shared JSON at the repo root).**
4. Verdict is self-reported; score ≥ 1 resets the streak; roasts/gradings end
   forward-looking. Never delete a video/verdict except explicit user action
   behind the two-stage sarcastic confirmation.
5. No third-party dependency without asking the owner first (includes chart
   libraries — Compose Canvas is the default answer, brief §4).

## Conventions

- Kotlin official style, 4-space indent, one primary type per file.
- Pure logic stays in `logic/` as `object`s with pure functions on value
  types — no Android imports, so it stays JVM-testable. Every service with
  logic gets tests; port behaviour AND tests together.
- Compose state for anything Settings-driven — the home screen must re-render
  the moment the word/noun changes (lesson §6.3).
- All debrief routes live on one nav graph so `popUpTo(home)` always unwinds
  fully (lesson §6.4).
- JUnit4 for unit tests, mirroring the iOS Swift Testing suites' names.

## Lessons already paid for — encode, don't re-learn (brief §6)

Streak: hangover morning is day ZERO (test exists — keep it green). Articles:
every `[WORD]` insertion goes through `CharacterVoice.article()`. Charts: clean
nights are green dots on the axis, not invisible zero-bars; date-based x-axis;
y locked 0–5. Catalogs: customs remembered case-insensitively; built-in crimes
not removable.

## Google Play (when release nears — brief §7)

Alcohol content rating answered honestly (expect Mature 17+). Data safety:
"No data collected, no data shared". `SCHEDULE_EXACT_ALARM` (user-scheduled
reminders rationale), degrade gracefully to inexact. Privacy URL:
https://pettifordo.github.io/GreenMonkeys/ — update its iPhone/Face ID wording
before launch. Verify the Strive4 Play developer account before assuming it.

## User preferences

Owner prefers concise output and honest uncertainty. Avoid filler.
