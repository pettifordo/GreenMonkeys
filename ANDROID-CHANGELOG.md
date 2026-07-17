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
