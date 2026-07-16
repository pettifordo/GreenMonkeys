# Green Monkeys — Product Specification

A brutally honest (but ultimately kind) drinking-accountability app for people who
drink more than they intend to and regret it the next day. The **Green Monkeys**
sit on your shoulder the morning after telling you what an idiot you were;
**Captain Paranoia** worries about what you said and did. The app weaponises both
characters *for* you instead of against you.

iPhone + Apple Watch. **All data local. Nothing ever leaves the device.**

## 1. Core loop

1. **The Plan (sober you, before the session)** — record a short video and a typed
   list of commitments for a scheduled drinking session: max drinks, water every
   other drink, leave by HH:MM, no driving, custom promises. Reminder times are
   chosen when the plan is made.
2. **The Session (during)** — at the pre-scheduled reminder times, notifications
   (mirrored to the Watch) resurface the plan and sober-you's video. One tap from
   the notification opens the camera to record drunk-you.
3. **The Morning After** — a notification the next morning walks you through:
   watch the drunk video, rewatch the plan video, mark each commitment kept or
   broken, confess any **booze crimes** (charge-sheet multi-select, see §2a),
   deliver the honest self-verdict on the **0–5 scale** (the only mandatory
   step), optionally record a regret/reflection video and name one thing to
   change next time. The flow still ends on the forward-looking step, but it
   no longer blocks saving.
   The flow is reachable at any time from the session screen ("Record the
   outcome") — no waiting for status transitions.
4. **The Pattern** — the composite record: idiot rate, average 0–5 score,
   per-promise broken/kept tallies (built-ins and customs alike), and lifetime
   booze-crime counts. The app throws this history back at you when you make
   the same promise again ("You've promised to leave by 23:00 four times.
   You've managed it once."). Reached from the patterns screen or by tapping
   the counter on the home screen.

## 2. The counter

Home screen headline: **"Days since you were a(n) [WORD]"**.

- `WORD` is configurable. Presets are deliberately mild — idiot / drunk /
  embarrassment / liability — and a custom free-text field lets users supply
  their own terrible word. Nothing harsher ships as a preset.
- The verdict is a **0–5 scale**, self-reported — the app suggests nothing and
  never accuses. 0 = behaved like a normal person; 1 = a bit of a [word];
  5 = can't show my face again. Each score has an amusing grading line.
- Any score ≥ 1 resets the counter.
- **Day convention (owner-confirmed):** the hangover morning is day ZERO. The
  incident night anchors the streak, and the first full clean day after it is
  day 1. (Confess Saturday morning → 0; clean Saturday night → 1 on Sunday.)
- If no idiot verdict has ever been recorded, count plainly from first app use
  (no hangover-day shift — nothing happened).
- **Longest clean streak** (best run ever, including the current one) is shown
  on the pattern screen and the widget. Computed with the same day
  conventions; the widget receives it via the snapshot and shows
  "personal best!" when the current run is the record.

## 2a. Configurable vocabulary & catalogs

- **Session noun**: what the user calls a drinking occasion — presets
  Session / Night Out / Drink / Cheeky One, plus custom. Drives the
  "Plan a [noun]" button, "No [noun] planned" empty state, and editor titles.
- **Promise list**: built-in kinds plus user-added custom promises. Any custom
  promise used in a plan is remembered and offered again next time. Managed
  (removable) in Settings.
- **Booze-crime list**: built-ins (Blacked out, Insulted someone, Offended
  someone, Threw up, Damaged a relationship, Got in a fight, Criminal offence,
  Drunk texting, Lost phone/wallet/keys) plus user-added customs, which are
  remembered for future mornings. Customs removable in Settings; built-ins are
  not ("the law is the law").

## 3. Characters and tone

- **Green Monkeys 🐒** — mocking-but-affectionate piss-take. Delivers session
  nudges and morning-after commentary.
- **Captain Paranoia 🫣** — anxious second-guessing voice. Appears in the
  morning-after flow ("Any texts you need to check…?").
- **Brutality dial** (Settings): 0 gentle → 1 standard → 2 savage. All character
  lines scale with it. The insult word is injected into lines where relevant.
- Regardless of brutality, the morning-after flow ends on the forward-looking
  "one change" step (encouraged, not mandatory — only the 0–5 score is
  required). Shame is the hook; self-correction is the product.
- **Deleting a session is deliberately hard**: two sarcastic confirmations
  ("Deleting this won't delete the memory of it…") before anything is removed.

## 4. Sessions

- Fully scheduled in advance: a plan has a start date/time and a planned end.
- Status is **derived, never stored**:
  - `planned` — now < start
  - `active` — start ≤ now ≤ plannedEnd + 6 h grace
  - `awaitingVerdict` — past grace, no verdict yet
  - `completed` — verdict recorded
- Reminders are minute-offsets from session start, chosen at plan time
  (defaults 60/120/180 min). Each fires a notification with the commitments
  summary, a Green Monkeys line, and a **Record video** action that deep-links
  straight into the front camera.
- Morning-after notification fires the morning after `plannedEnd` at a
  configurable hour (default 09:00).
- **Unplanned nights**: a morning-after debrief can be started with no
  pre-existing plan. The app creates a retrospective session (user picks which
  night, optional occasion, no commitments, no reminders) and goes straight to
  the debrief — the verdict, videos, streak, and pattern all work the same.
  Regret shouldn't need a booking.

## 5. Videos

Three kinds per session: `plan`, `drunk`, `morningAfter`.

- Stored **only** in Application Support with complete file protection,
  excluded from iCloud/iTunes backup. Deletable individually and with the plan.
- Never exported, shared, or uploaded by the app. No share sheet in v1 —
  deliberate: this footage is radioactive.
- Optional Face ID/passcode lock on the whole app (Settings, default ON).

## 6. Watch (v1: slim)

- Session reminders arrive via standard iPhone notification mirroring.
- Watch app shows: the streak counter, tonight's plan commitments while a
  session is planned/active. Data via WatchConnectivity application context.
- v2 candidates: complication with the counter, on-wrist drink tally.

## 7. Streak widget

Home-screen (systemSmall) and Lock Screen (circular / rectangular / inline)
widget showing the counter. The app shares only the streak anchor date, first
use date, and the configured word via the App Group
(`group.com.strive4it.greenmonkeys`) — never any video or session content. The
widget computes the day count from the anchor itself, with timeline entries at
each midnight, so it ticks over without the app being opened.

## 8. Out of scope for v1

- Any cloud sync, accounts, or sharing. No analytics or telemetry, ever.
- Drink logging/BAC estimation, location, contact blocking ("drunk text guard").
- Watch complication, Health integration.
- AI-generated character lines (v1 uses a curated line bank).

## 9. Safety framing

This is a self-awareness tool for social drinkers, **not** treatment for alcohol
dependence and it must never claim to be. Settings contains a quiet
"If this feels bigger than morning-after regret" link to NHS alcohol support
(https://www.nhs.uk/live-well/alcohol-advice/). No medical claims anywhere in
UI or App Store copy. Expected App Store rating 17+.
