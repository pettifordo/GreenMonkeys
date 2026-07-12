# Green Monkeys 🐒

The morning-after accountability app. Record sober-you's plan before a night
out, let the Green Monkeys nag you (phone + Apple Watch) during it, film the
evidence, and face Captain Paranoia's debrief the next morning — ending, always,
with one thing you'll change.

**Days since you were a(n) [your word here]: 0** — let's fix that.

All data stays on your phone. No accounts, no cloud, no analytics. Videos are
stored with full file protection, excluded from backups, behind Face ID.

## Setup

```bash
brew install xcodegen          # once
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
xcodegen generate
open GreenMonkeys.xcodeproj
```

See `SPEC.md` for the product spec and `CLAUDE.md` for development conventions.

Green Monkeys is a self-awareness tool for social drinkers, not a treatment for
alcohol dependence. If drinking is worrying you: https://www.nhs.uk/live-well/alcohol-advice/
