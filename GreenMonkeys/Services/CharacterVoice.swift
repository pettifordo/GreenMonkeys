import Foundation

/// Brutality dial. Scales the jokes, never the forward-looking ending (hard rule 5).
enum Brutality: Int, CaseIterable {
    case gentle = 0
    case standard = 1
    case savage = 2

    var label: String {
        switch self {
        case .gentle:   return "Gentle"
        case .standard: return "Standard"
        case .savage:   return "Savage"
        }
    }
}

/// Curated line bank for the two characters. Pure functions — deterministic
/// variants selectable by index so tests can pin output.
enum CharacterVoice {

    // MARK: - Green Monkeys 🐒

    /// Shown on session reminders during the night.
    static func monkeySessionNudge(brutality: Brutality, word: String, variant: Int = Int.random(in: 0..<3)) -> String {
        let lines: [Brutality: [String]] = [
            .gentle: [
                "Quick word from sober you — remember the plan?",
                "The Monkeys are watching. Fondly. Mostly.",
                "Checking in! Sober you had some rather good ideas earlier.",
            ],
            .standard: [
                "Oi. Sober you made promises. Drunk you is on thin ice.",
                "The Green Monkeys have opened a file on you. Stick to the plan.",
                "Remember the plan, or tomorrow's video is going in the archive.",
            ],
            .savage: [
                "The Monkeys are taking notes, you absolute weapon. THE PLAN.",
                "Every drink past the plan is a clip for tomorrow's shame reel.",
                "Sober you would be embarrassed. Don't make him say it on camera. Again.",
            ],
        ]
        return line(from: lines, brutality: brutality, variant: variant, word: word)
    }

    /// Morning-after greeting, before the verdict is given.
    static func monkeyMorningGreeting(brutality: Brutality, word: String, variant: Int = Int.random(in: 0..<3)) -> String {
        let lines: [Brutality: [String]] = [
            .gentle: [
                "Morning. Cup of tea, then let's have an honest look at last night.",
                "The Monkeys come in peace. Ish. Time for the debrief.",
                "Right then. How are we feeling? Let's review.",
            ],
            .standard: [
                "Morning, sunshine. The Monkeys have prepared a little screening for you.",
                "Debrief time. Sober you left a video. Drunk you may also have left a video. Fun!",
                "Time to face the evidence. The Monkeys will allow tea first.",
            ],
            .savage: [
                "Wakey wakey. The Monkeys stayed up all night editing your highlights, you [WORD].",
                "Oh good, you're alive. Shall we watch what you did to yourself?",
                "The tribunal is now in session. Exhibit A awaits, you [WORD].",
            ],
        ]
        return line(from: lines, brutality: brutality, variant: variant, word: word)
    }

    /// After an honest "yes, I was one" verdict. Ends kind — always.
    static func monkeyAfterIdiotVerdict(brutality: Brutality, word: String, variant: Int = Int.random(in: 0..<3)) -> String {
        let lines: [Brutality: [String]] = [
            .gentle: [
                "Honesty counts for a lot. Now — what's the one thing you'll change?",
                "Owning it is the hard part done. One change for next time.",
                "Fair play for saying it straight. Now pick your one change for next time.",
            ],
            .standard: [
                "Counter reset. But respect for the honesty — now pick your one change.",
                "The Monkeys note the confession. Sentence: one concrete change. Choose it.",
                "Back to zero. Honest, though — that's worth more than the streak. One change.",
            ],
            .savage: [
                "Confirmed [WORD]. Counter obliterated. But you said it yourself, so: one change, make it count.",
                "The Monkeys accept your guilty plea. Now stop talking and pick the one change.",
                "Zero days. ZERO. Right — one change, and make it one you'll actually keep, you [WORD].",
            ],
        ]
        return line(from: lines, brutality: brutality, variant: variant, word: word)
    }

    /// After a clean night. Genuinely warm at every brutality level.
    static func monkeyAfterCleanVerdict(brutality: Brutality, word: String, variant: Int = Int.random(in: 0..<3)) -> String {
        let lines: [Brutality: [String]] = [
            .gentle: [
                "Look at you! Plan made, plan kept. The Monkeys are proud.",
                "A clean night. Lovely stuff. Streak intact.",
                "That's how it's done. Sober you and drunk you, finally on the same team.",
            ],
            .standard: [
                "Well well. Stuck to the plan. The Monkeys have nothing. NOTHING.",
                "Streak survives. The Monkeys are begrudgingly impressed.",
                "No material for the shame reel. Disappointing for us, brilliant for you.",
            ],
            .savage: [
                "Unbelievable. Not a [WORD] for once. The Monkeys are stunned into silence.",
                "The shame reel remains empty. Who even ARE you?",
                "Plan kept. The Monkeys retreat to their tree, defeated. Well played.",
            ],
        ]
        return line(from: lines, brutality: brutality, variant: variant, word: word)
    }

    // MARK: - Captain Paranoia 🫣

    /// Morning-after anxious voice — steers into the reflection, not spiralling.
    static func paranoiaMorningLine(brutality: Brutality, variant: Int = Int.random(in: 0..<3)) -> String {
        let lines: [Brutality: [String]] = [
            .gentle: [
                "Captain Paranoia here. Anything nagging at you? Write it down — it's usually smaller than it feels.",
                "Any 3am texts to check? Do it now, then let it go.",
                "Worried about anything you said? Note it in the debrief — sunlight shrinks it.",
            ],
            .standard: [
                "Captain Paranoia reporting. Check the sent folder. Check the group chat. THEN we debrief.",
                "That thing you half-remember saying? Write it down before it grows legs.",
                "The Captain suggests a quick scan: texts, calls, purchases. Then the verdict.",
            ],
            .savage: [
                "The Captain has QUESTIONS. What did you say to Steve? WHAT DID YOU SAY TO STEVE?",
                "Sent messages. Bank app. Camera roll. In that order. The Captain will wait.",
                "The Captain has been up since 4am on your behalf. Check everything, then confess.",
            ],
        ]
        return line(from: lines, brutality: brutality, variant: variant, word: "")
    }

    // MARK: - Pattern callbacks

    /// Thrown at the user when they make a promise they've broken before (SPEC §1.4).
    static func patternCallback(kind: CommitmentKind, timesPromised: Int, timesBroken: Int, brutality: Brutality, word: String) -> String? {
        guard timesPromised > 0, timesBroken > 0 else { return nil }
        let kept = timesPromised - timesBroken
        switch brutality {
        case .gentle:
            return "Gentle note: you've made this promise \(timesPromised) time\(timesPromised == 1 ? "" : "s") and kept it \(kept)."
        case .standard:
            return "You've promised \"\(kind.label)\" \(timesPromised) times now. Kept: \(kept). The Monkeys remember everything."
        case .savage:
            return "\"\(kind.label)\" — promised \(timesPromised) times, broken \(timesBroken). Saying it louder this time, you [WORD]?".replacing("[WORD]", with: word)
        }
    }

    // MARK: - Private

    private static func line(from bank: [Brutality: [String]], brutality: Brutality, variant: Int, word: String) -> String {
        let lines = bank[brutality] ?? []
        guard !lines.isEmpty else { return "" }
        let index = ((variant % lines.count) + lines.count) % lines.count
        return lines[index].replacing("[WORD]", with: word)
    }
}
