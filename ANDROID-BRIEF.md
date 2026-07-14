# Green Monkeys — Android Porting Brief

Written 2026-07-14 at the end of the iOS v1.0 build, to hand context to the
Android effort. Read `SPEC.md` first — it is the product source of truth for
both platforms. This brief covers what the spec doesn't: platform decisions,
the personality contract, hard-won lessons from the iOS build, and Google
Play specifics.

## 1. Platform decision (already made — don't relitigate)

**Native Kotlin + Jetpack Compose.** A PWA was evaluated and rejected: web
cannot schedule local notifications (Notification Triggers API is dead; Web
Push needs a server, which violates local-only), has no home-screen widget,
and offers no OS-grade file protection for the videos. A hybrid wrapper
(Capacitor) was the runner-up but the four make-or-break features — exact
alarms, widget, camera, biometrics — are exactly what wrappers do worst.

- Package / applicationId: `com.strive4it.greenmonkeys`
- Location: `android/` directory in this repo (monorepo, like PacePartner).
  Shared truth stays at the root: SPEC.md, docs/ site, this brief.
- Min SDK: 26+ is fine; target latest.

## 2. Non-negotiable product rules (same as iOS, see SPEC + CLAUDE.md)

1. Local-only. No analytics, crash SDKs, accounts, or network calls. Ever.
2. Videos never leave the device: app-private storage, excluded from backup
   (`android:allowBackup="false"` or explicit backup rules; consider
   `EncryptedFile`), no share/export function, biometric lock **on by default**.
3. The verdict (0–5) is self-reported; the app never accuses. Score ≥ 1 resets
   the streak. The 0–5 gradings and roast must end forward-looking.
4. Never delete/overwrite a recorded video or verdict except by explicit user
   action, behind the two-stage sarcastic confirmation.
5. Insult-word presets stay mild (idiot / drunk / embarrassment / liability);
   custom free text for anything worse. Session noun configurable
   (Session / Night Out / Drink / Cheeky One / custom).
6. No third-party dependency without asking the owner first. (This includes
   chart libraries — see §5.)

## 3. The personality contract (single source: iOS `CharacterVoice.swift`)

The two apps must share one voice. Port `GreenMonkeys/Services/CharacterVoice.swift`
line-for-line; do not write new copy without the owner. Its API surface:

- `Brutality` dial: gentle / standard / savage. Scales the jokes, never the
  sincere closer.
- `[WORD]` placeholder substitution + `article(for:)` ("a"/"an" — yes, this
  was a shipped bug once; see §6).
- Line banks: session nudges, morning greetings, post-verdict (idiot + clean),
  Captain Paranoia morning lines — 3 variants × 3 brutality levels each.
- `scoreGrading(0...5, word:)` — the verdict scale wording.
- `roast(score:crimes:brokenPromises:brutality:word:)` → opener (references
  the numeric score), one bespoke charge line per built-in booze crime
  (customs get "added to your permanent record"), a broken-promises line, and
  the monkey-free closer. Closer escalates to the honest "is alcohol giving
  you more than it takes?" nudge when score ≥ 3 or crimes ≥ 3; clean nights
  (0, no crimes) get praise instead.
- `patternCallback(...)` — quotes the user's own record when they re-make a
  broken promise. `deleteWarnings` — the two-stage delete sarcasm.

If the banks ever need to change, extract them to a shared JSON at the repo
root and generate both platforms from it.

## 4. Feature → Android mapping

| Feature | iOS implementation | Android approach |
|---|---|---|
| Data | SwiftData models: SessionPlan, Commitment, SessionVideo, Verdict | Room (entities mirror the four models; status is DERIVED from dates+verdict, never stored) |
| Session nudges | UNUserNotificationCenter, offsets from sessionStart, time-sensitive, "Record video" action | AlarmManager `setExactAndAllowWhileIdle` + POST_NOTIFICATIONS; notification action deep-links straight into the front camera (one tap — drunk users don't navigate) |
| Morning-after summons | Next morning at configurable hour (default 09:00) | Same via AlarmManager; full-screen intent not needed |
| Video capture | UIImagePickerController, front camera, 120 s cap | CameraX, front lens default, same cap |
| Video storage | App Support + complete file protection + backup-excluded | `filesDir/videos`, backup-excluded; consider Jetpack `EncryptedFile` |
| App lock | LocalAuthentication, default ON | BiometricPrompt (`DEVICE_CREDENTIAL` fallback), default ON |
| Widget | WidgetKit, App Group snapshot; widget computes days itself with midnight timeline entries | Glance widget; snapshot via DataStore; schedule midnight refresh (WorkManager) — same principle: share the ANCHOR DATE, not a cached count |
| Watch | WatchConnectivity slim app | Skip in v1 — notification bridging to Wear is automatic |
| Charts | Swift Charts (first-party) | No first-party equivalent. Either Compose Canvas (no dependency) or ASK OWNER before adding Vico/similar |
| Screenshot rig | DEBUG launch args seed demo data + route to screens | Same idea: debug-only intent extras; reuse the demo dataset from `ScreenshotRig.swift` verbatim (5 judged nights, scores 4/2/0/3/0, plus pending + upcoming) |
| App icon | `Tools/draw_icon.swift` renders 1024 px master | Reuse the master for Play listing; build adaptive icon layers (monkey foreground / purple gradient background) from the same geometry |

## 5. Screens (parity checklist)

Home (streak headline → tappable to Pattern; face-the-music; coming up;
"No [noun] planned" tappable; labelled "Plan a [noun]" button; unplanned-night
confess entry) · Plan editor (promises catalog incl. remembered customs,
pattern callbacks, check-in times, plan video) · Session live (plan, nudge
line, big record button, skip to outcome) · Morning after (EVIDENCE LINKS AT
TOP, committee greeting, promises kept/broken, charge sheet with custom-crime
confess field, 0–5 scale — the only mandatory field, optional video + one
change) · **Verdict screen — its own destination** (roast, closer, one change,
"Finish — go face the day" → all the way Home) · Pattern (score stats, two
charts, repeat-offender promises, lifetime charge sheet, record with per-night
score + "Relive the roast") · Settings (word, noun, brutality, debrief hour,
catalogs management, lock toggle, support/privacy/NHS links).

## 6. Lessons already paid for (do not re-learn on Android)

1. **Streak arithmetic**: the hangover morning is day ZERO; the first full
   clean day is 1. Anchor on the incident night's `sessionStart`, subtract one
   day, floor at 0. First-use fallback counts plainly (no shift). Encode the
   owner's scenario as a test: idiot night two days ago + clean night = 1.
2. **"a idiot"**: every string that inserts the word needs the article helper.
3. **Live settings**: the home screen must re-render the moment the word/noun
   changes (Compose state, not a launch-time read).
4. **Verdict flow**: deliver-verdict navigates to the dedicated verdict screen;
   Finish must unwind EVERY entry point to Home (we shipped a bug where the
   unplanned-night flow didn't unwind — keep all debrief routes on one nav
   graph / back-stack so popUpTo(home) always works).
5. **Charts**: a zero-score night is invisible as a bar — draw clean nights as
   green dots on the axis. Date-based x-axis (gaps meaningful), y locked 0–5,
   dashed lifetime-average line.
6. **Catalogs**: custom promises/crimes are remembered case-insensitively at
   the moment of use and offered forever after; built-in crimes not removable.

## 7. Google Play specifics

- **Content rating questionnaire**: answer the alcohol section honestly
  (frequent/intense alcohol references) — expect Mature 17+ / PEGI 18-ish.
  The app never encourages drinking; the store listing copy should lead with
  accountability, matching the docs/ site tone.
- **Data safety form**: "No data collected, no data shared" — true, and must
  stay true (rule 1). Videos = "data stored on device only", not collected.
- **Exact alarms**: `USE_EXACT_ALARM` is policy-restricted; Green Monkeys'
  nudges are user-scheduled reminders, which fits `SCHEDULE_EXACT_ALARM` +
  in-app rationale. Degrade gracefully to inexact if denied.
- **Site**: support/privacy pages already live at
  https://pettifordo.github.io/GreenMonkeys/ — Play requires the privacy URL.
  Before launch, update `docs/privacy.html` wording that says "iPhone" /
  "Face ID" to cover Android ("your phone", "biometric lock"), and add Play
  availability to `docs/index.html`.
- **Signing/account**: owner has a Play developer account under Strive4
  (verify before assuming).

## 8. Suggested first session plan

1. `android/` Gradle scaffold (Kotlin, Compose, Room, single module).
2. Port the value-type logic first WITH TESTS: StreakService, PatternService,
   CharacterVoice (these are pure functions in iOS — translate tests too;
   iOS has 27, most cover exactly this logic).
3. Data layer, then Home + Plan editor + notifications, then debrief/verdict,
   then widget/charts/lock.
4. Add an `android/CLAUDE.md` mirroring the root one (build commands,
   conventions) so future sessions bootstrap instantly.
