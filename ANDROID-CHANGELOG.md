# Cross-Platform Change Log

The sync ledger between the iOS and Android apps. **Any product-behaviour
change on either platform gets an entry here until it has been applied to the
other platform.** Release engineering (signing, versions, store assets) and
site-only changes are noted for context but need no port.

How to use: when you change product behaviour, append an entry (date, source
commit, what/why, port notes) marked **PENDING**. Whoever ports it flips the
status to **APPLIED** with the applying commit. Entries stay in the file —
this is also the product's decision history.

Parity baseline: Android reached iOS v1 parity at `e1ebe6c`
("Complete Android v1 parity", 2026-07-16), including the verdict-screen flow,
roast, charts, catalogs, widget, and app lock.

---

## PENDING → Android

### 2026-07-17 · First-run onboarding + existing-streak start (owner-requested)

1. **Onboarding** — shown once on first launch (gated by `hasOnboarded`),
   re-openable from Settings → "How it works". Four pages: Welcome (the two
   characters), How it works (Plan/Session/Morning/Pattern), Owen's story
   (short quote + link to the site story page), and "Already on a streak?".
   Skippable except the last page. **Notification permission moved here** —
   requested at the END of onboarding, not cold on launch (do the same on
   Android: don't request POST_NOTIFICATIONS until onboarding completes).
   Screenshot rig skips onboarding.
2. **Existing-streak start** — a self-declared `streakStartDate` (stored as
   epoch seconds; 0 = unset). New computed anchor
   `AppSettings.streakAnchorDate = streakStartDate ?? firstUseDate` — EVERY
   streak calc (home count, longest streak, widget snapshot's firstUseDate)
   now uses the anchor, so a declared start flows everywhere with no new
   convention. Self-declared start counts plainly (no hangover shift); a later
   incident becomes the anchor and resets normally.
   - Onboarding date picker with live "That's N days clean. Respect." Input is
     a DATE (decided with owner), not a day count.
   - Editable in Settings with **escalating snark** (decided with owner, not
     one-off-locked): `CharacterVoice.streakStartMoveWarning(edits:)` — 3
     tiers by `streakStartEdits` count. Onboarding only SETS when unset;
     replay never overwrites — moving it is Settings' snark-gated job.

**Status:** PENDING

### 2026-07-17 · iOS 1.1 feature batch (owner-requested)

One release's worth of changes; port as a set. iOS is now marketing 1.1,
build 1 — bump Android versionName to 1.1 when ported.

1. **Longest streak on the Home card.** Under the tagline: `Longest streak: N
   days`, switching to `🏆 Personal best — beat yesterday's you again
   tomorrow` when current ≥ longest and > 0.
2. **Seeded personal record (Settings).** New setting "Longest streak to beat"
   (`seedLongestStreak`, non-negative Int, numeric field): a pre-app record to
   challenge yourself against. Effective longest = `max(computed, seed)`
   everywhere (home, pattern, widget snapshot). App never claims a personal
   best until the seed is beaten.
3. **"Risk Event" added to the session-noun presets** (after Cheeky One).
4. **Pattern charts reworked** (owner feedback: green dot was unclear, average
   RuleMark collided with bars):
   - Score chart is now a **curvy line** (Catmull-Rom), plain BLUE line and
     points — no severity/green colour-coding (owner reviewed on device: the
     green dot was unexplained and red read as scolding). Zero nights are
     naturally visible at y=0; footer: "Lower is better; zero means you
     behaved." Average rule REMOVED.
   - **Trend forecast**: least-squares fit over the plotted points (≥ 3 points
     required — "predicting from two nights is astrology"), drawn as a dashed
     secondary line from the last bucket to one horizon ahead (night→+7 d,
     week→+2 w, month→+2 m, year→+1 y), values clamped 0–5. Footer commentary
     by slope: improving / heading the wrong way / flat.
   - **Period picker** (segmented: Night / Week / Month / Year) feeding both
     charts: buckets by calendar period with MEAN score and SUMMED misdeeds.
     See `PatternService.aggregate` + `trend` and the 6 new tests
     (week/month bucketing, trend slopes, clamping).
5. **Unplanned debrief video import.** Retro sessions (no commitments AND no
   reminders) must NOT say "No drunk video recorded" — drunk-you couldn't
   have. Instead: "Add a video from last night" via the system photo picker
   (no permission prompt needed on either platform: PHPicker / Android Photo
   Picker), imported copy stored in the protected video store as a `drunk`
   video. Planned sessions keep the snark and also get the import option.
6. **Swipe-to-cancel/delete on ALL home session rows** — upcoming, live, and
   debrief-due (owner found part-completed sessions un-swipeable). Wording
   adapts: planned → "Cancel this X?" / "Are you sure? … the Monkeys are
   still watching. They saw you plan it." / "It was created by mistake —
   delete it"; started → "Delete this X?" / "Was it created by mistake, or
   did it just go badly? … deleting it won't delete the memory of it." /
   "Delete it — evidence and all". Deletes videos + notifications + record.
7. **Plan editor buttons moved to the bottom** (owner preference): prominent
   "Save the plan" + bordered "Cancel" at the end of the form; top-bar
   Save/Cancel removed.

**Status:** PENDING

### 2026-07-16 · Longest clean streak (iOS `c290c7a`)

**What:** "Longest clean streak" surfaced in two places:
- Pattern screen, The Score section: `Longest clean streak — N days` row.
- Widget: home-screen size shows `🐒 best: N` under the count, switching to
  `🐒 personal best!` when the current run ≥ stored best and > 0; Lock Screen
  rectangular shows `best N` beside the number.

**Algorithm** (`Shared/StreakService.swift → longestStreak`), same day
conventions as the counter:
- No incidents ever → current run from first-use date (plain count).
- Segment 1: startOfDay(firstUse) → startOfDay(first incident), plain days,
  floored at 0 (guards firstUse AFTER old incidents, e.g. reinstall).
- Between consecutive incident days: `gap_days - 1` (hangover shift).
- Current run (daysSince last incident) is included — the record may be live.
- Incidents on the same calendar day collapse to one.

**Snapshot:** `StreakSnapshot.longestStreak: Int?` — optional so pre-existing
stored snapshots still decode; app writes it on Home appear and on verdict
save (which re-fetches all plans to recompute). Widget displays
`max(stored, currentRun)` so a record broken between app opens shows without
waiting for the app.

**Android port notes:** add `longestStreak` to the Kotlin StreakService with
the 5 new tests (see `GreenMonkeysTests/StreakServiceTests.swift`, the
"Longest streak" suite — includes the same-day-collapse and
firstUse-after-incident edge cases); extend the DataStore widget snapshot
(default null for existing installs); pattern row + Glance widget line with
the personal-best wording.

**Status:** PENDING

---

## No port needed (context only)

- 2026-07-14 · `docs/story.html` + homepage teaser — founder's story with live
  TDE counter (site serves both platforms). Play listing copy can quote it.
- 2026-07-15 · iOS 6.5" screenshot set for App Store Connect (`c67ddc1`).
- 2026-07-16 · Android release signing from gitignored keystore (`c06917c`).
- 2026-07-16 · iOS version alignment: marketing 1.0, build 2 (upload fix).

## APPLIED (historical)

- Everything up to and including the charts + verdict-flow rework is covered
  by the parity baseline `e1ebe6c`; see `ANDROID-BRIEF.md` for the decisions
  and lessons that shaped it.
