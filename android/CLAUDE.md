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

**Emulator** (installed; AVD `gm-test` = Pixel 7 / API 35 arm64):

```bash
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
"$ANDROID_SDK_ROOT/emulator/emulator" -avd gm-test -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect &
"$ANDROID_SDK_ROOT/platform-tools/adb" wait-for-device   # then poll sys.boot_completed
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.strive4it.greenmonkeys/.MainActivity
adb exec-out screencap -p > /tmp/screen.png             # verify visually
adb shell "dumpsys alarm | grep -A3 greenmonkeys"       # verify scheduled nudges
adb emu kill
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
    ├── MainActivity.kt          # nav host + notification-tap routing
    ├── GreenMonkeysApp.kt       # composition root (no DI framework)
    ├── logic/                   # PURE logic, no Android imports, fully tested
    │   ├── StreakService.kt     # ported from iOS Shared/StreakService.swift
    │   ├── PatternService.kt    # ported from iOS Services/PatternService.swift
    │   ├── CharacterVoice.kt    # THE VOICE — see below
    │   ├── PlanStatus.kt        # derived status + 6h grace (SPEC §4)
    │   ├── CommitmentKind.kt / VideoKind.kt / ReminderTimes.kt / CatalogLogic.kt
    ├── data/                    # Room: 4 entities, PlanWithDetails, insert-only verdicts
    ├── settings/                # DataStore: word/noun/brutality/hour/lock + catalogs
    ├── notifications/           # NudgeScheduler (AlarmManager) + Nudge/Boot receivers
    └── ui/                      # AppNavHost (ONE graph), home/, editor/, detail/, theme/
    src/test/java/.../logic/     # 66 JVM tests (all pure logic; ported 1:1 from iOS)
```

**At parity with iOS 1.1 (2026-07-19), all screens live:** Home, plan editor,
session-live, morning-after debrief, verdict/roast, unplanned confession,
pattern, settings, CameraX front-camera recorder (120s cap) +
`capture/VideoStore` (filesDir/videos) + VideoView playback, BiometricPrompt
lock (default ON; fails OPEN when no PIN/biometric enrolled — never brick the
app), Glance streak widget (anchor-date-only snapshot, WorkManager midnight
refresh), adaptive launcher icon (from `Tools/draw_icon.swift` geometry).

iOS 1.1 delta features present: `StreakService.longestStreak` +
seedLongestStreak ("record before the app") on Home card / pattern / widget;
pattern chart rework (Night/Week/Month/Year period picker, `PatternService`
`aggregate`/`trend` with dashed forecast + Monkey commentary, plain blue line);
Home swipe-to-delete with status-aware confirmation; library video import on
the debrief; debug `DemoSeeder` (mirrors iOS `ScreenshotRig`) via
`am start ... --ez demoData true --es screen <home|editor|session|morning|roast|pattern|settings>`.

Not yet done: Play listing store assets (feature graphic, screenshots),
docs/ wording update for Android, real-device camera test (emulator uses the
fake camera).

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

## Cross-platform sync

Product-behaviour changes on either platform get a PENDING entry in
`../ANDROID-CHANGELOG.md` until ported to the other. Check it at the start of
every session — pending iOS entries are your work queue. Flip entries to
APPLIED (with your commit hash) when done.
