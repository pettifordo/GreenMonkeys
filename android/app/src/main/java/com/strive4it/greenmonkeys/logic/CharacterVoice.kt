package com.strive4it.greenmonkeys.logic

import kotlin.random.Random

/** Brutality dial. Scales the jokes, never the forward-looking ending (hard rule 5). */
enum class Brutality(val value: Int, val label: String) {
    GENTLE(0, "Gentle"),
    STANDARD(1, "Standard"),
    SAVAGE(2, "Savage"),
}

/**
 * Curated line bank for the two characters. Pure functions — deterministic
 * variants selectable by index so tests can pin output.
 *
 * SINGLE SOURCE OF VOICE: ported line-for-line from iOS
 * `GreenMonkeys/Services/CharacterVoice.swift` (brief §3). Do not write new
 * copy here without the owner; if the banks change, change both platforms.
 */
object CharacterVoice {

    // MARK: - Green Monkeys 🐒

    /** Shown on session reminders during the night. */
    fun monkeySessionNudge(brutality: Brutality, word: String, variant: Int = Random.nextInt(3)): String {
        val lines = mapOf(
            Brutality.GENTLE to listOf(
                "Quick word from sober you — remember the plan?",
                "The Monkeys are watching. Fondly. Mostly.",
                "Checking in! Sober you had some rather good ideas earlier.",
            ),
            Brutality.STANDARD to listOf(
                "Oi. Sober you made promises. Drunk you is on thin ice.",
                "The Green Monkeys have opened a file on you. Stick to the plan.",
                "Remember the plan, or tomorrow's video is going in the archive.",
            ),
            Brutality.SAVAGE to listOf(
                "The Monkeys are taking notes, you absolute weapon. THE PLAN.",
                "Every drink past the plan is a clip for tomorrow's shame reel.",
                "Sober you would be embarrassed. Don't make him say it on camera. Again.",
            ),
        )
        return line(lines, brutality, variant, word)
    }

    /** Morning-after greeting, before the verdict is given. */
    fun monkeyMorningGreeting(brutality: Brutality, word: String, variant: Int = Random.nextInt(3)): String {
        val lines = mapOf(
            Brutality.GENTLE to listOf(
                "Morning. Cup of tea, then let's have an honest look at last night.",
                "The Monkeys come in peace. Ish. Time for the debrief.",
                "Right then. How are we feeling? Let's review.",
            ),
            Brutality.STANDARD to listOf(
                "Morning, sunshine. The Monkeys have prepared a little screening for you.",
                "Debrief time. Sober you left a video. Drunk you may also have left a video. Fun!",
                "Time to face the evidence. The Monkeys will allow tea first.",
            ),
            Brutality.SAVAGE to listOf(
                "Wakey wakey. The Monkeys stayed up all night editing your highlights, you [WORD].",
                "Oh good, you're alive. Shall we watch what you did to yourself?",
                "The tribunal is now in session. Exhibit A awaits, you [WORD].",
            ),
        )
        return line(lines, brutality, variant, word)
    }

    /** After an honest "yes, I was one" verdict. Ends kind — always. */
    fun monkeyAfterIdiotVerdict(brutality: Brutality, word: String, variant: Int = Random.nextInt(3)): String {
        val lines = mapOf(
            Brutality.GENTLE to listOf(
                "Honesty counts for a lot. Now — what's the one thing you'll change?",
                "Owning it is the hard part done. One change for next time.",
                "Fair play for saying it straight. Now pick your one change for next time.",
            ),
            Brutality.STANDARD to listOf(
                "Counter reset. But respect for the honesty — now pick your one change.",
                "The Monkeys note the confession. Sentence: one concrete change. Choose it.",
                "Back to zero. Honest, though — that's worth more than the streak. One change.",
            ),
            Brutality.SAVAGE to listOf(
                "Confirmed [WORD]. Counter obliterated. But you said it yourself, so: one change, make it count.",
                "The Monkeys accept your guilty plea. Now stop talking and pick the one change.",
                "Zero days. ZERO. Right — one change, and make it one you'll actually keep, you [WORD].",
            ),
        )
        return line(lines, brutality, variant, word)
    }

    /** After a clean night. Genuinely warm at every brutality level. */
    fun monkeyAfterCleanVerdict(brutality: Brutality, word: String, variant: Int = Random.nextInt(3)): String {
        val lines = mapOf(
            Brutality.GENTLE to listOf(
                "Look at you! Plan made, plan kept. The Monkeys are proud.",
                "A clean night. Lovely stuff. Streak intact.",
                "That's how it's done. Sober you and drunk you, finally on the same team.",
            ),
            Brutality.STANDARD to listOf(
                "Well well. Stuck to the plan. The Monkeys have nothing. NOTHING.",
                "Streak survives. The Monkeys are begrudgingly impressed.",
                "No material for the shame reel. Disappointing for us, brilliant for you.",
            ),
            Brutality.SAVAGE to listOf(
                "Unbelievable. Not a [WORD] for once. The Monkeys are stunned into silence.",
                "The shame reel remains empty. Who even ARE you?",
                "Plan kept. The Monkeys retreat to their tree, defeated. Well played.",
            ),
        )
        return line(lines, brutality, variant, word)
    }

    // MARK: - Captain Paranoia 🫣

    /** Morning-after anxious voice — steers into the reflection, not spiralling. */
    fun paranoiaMorningLine(brutality: Brutality, variant: Int = Random.nextInt(3)): String {
        val lines = mapOf(
            Brutality.GENTLE to listOf(
                "Captain Paranoia here. Anything nagging at you? Write it down — it's usually smaller than it feels.",
                "Any 3am texts to check? Do it now, then let it go.",
                "Worried about anything you said? Note it in the debrief — sunlight shrinks it.",
            ),
            Brutality.STANDARD to listOf(
                "Captain Paranoia reporting. Check the sent folder. Check the group chat. THEN we debrief.",
                "That thing you half-remember saying? Write it down before it grows legs.",
                "The Captain suggests a quick scan: texts, calls, purchases. Then the verdict.",
            ),
            Brutality.SAVAGE to listOf(
                "The Captain has QUESTIONS. What did you say to Steve? WHAT DID YOU SAY TO STEVE?",
                "Sent messages. Bank app. Camera roll. In that order. The Captain will wait.",
                "The Captain has been up since 4am on your behalf. Check everything, then confess.",
            ),
        )
        return line(lines, brutality, variant, word = "")
    }

    // MARK: - Score gradings

    /** "a" or "an" for the configured word. (Yes, "a idiot" was a shipped bug once.) */
    fun article(word: String): String {
        val first = word.lowercase().firstOrNull() ?: ' '
        return if (first in "aeiou") "an" else "a"
    }

    /** The 0–5 verdict scale, worded for maximum honesty (SPEC §2). */
    fun scoreGrading(score: Int, word: String): String {
        val a = article(word)
        return when (score) {
            0 -> "Behaved like a normal person. Who even are you?"
            1 -> "A bit of $a $word. Recoverable."
            2 -> "A proper $word. People noticed."
            3 -> "Weapons-grade $word. Apologies are owed."
            4 -> "${a.replaceFirstChar { it.uppercase() }} $word for the ages. The group chat has screenshots."
            else -> "Can't show your face again. The Monkeys are impressed, frankly."
        }
    }

    // MARK: - Pattern callbacks

    /** Thrown at the user when they make a promise they've broken before (SPEC §1.4). */
    fun patternCallback(label: String, timesPromised: Int, timesBroken: Int, brutality: Brutality, word: String): String? {
        if (timesPromised <= 0 || timesBroken <= 0) return null
        val kept = timesPromised - timesBroken
        return when (brutality) {
            Brutality.GENTLE ->
                "Gentle note: you've made this promise $timesPromised time${if (timesPromised == 1) "" else "s"} and kept it $kept."
            Brutality.STANDARD ->
                "You've promised \"$label\" $timesPromised times now. Kept: $kept. The Monkeys remember everything."
            Brutality.SAVAGE ->
                "\"$label\" — promised $timesPromised times, broken $timesBroken. Saying it louder this time, you [WORD]?"
                    .replace("[WORD]", word)
        }
    }

    // MARK: - The Roast

    /** The post-verdict debrief: shaming with receipts, then a sincere exit line. */
    data class Roast(
        val opener: String,
        /** One line per confessed booze crime, in confession order. */
        val charges: List<String>,
        val promisesLine: String?,
        /**
         * Monkey-free. Escalates from "could do better" to the honest
         * "is alcohol giving more than it takes?" question on bad mornings.
         */
        val closer: String,
    )

    fun roast(score: Int, crimes: List<String>, brokenPromises: Int, brutality: Brutality, word: String): Roast =
        Roast(
            opener = roastOpener(score, brutality, word),
            charges = crimes.map { chargeLine(it) },
            promisesLine = promisesLine(brokenPromises),
            closer = roastCloser(score, crimes.size),
        )

    private fun roastOpener(score: Int, brutality: Brutality, word: String): String {
        // Gentle keeps the plain grading; the Monkeys save the theatrics for the dial.
        if (brutality == Brutality.GENTLE) {
            return "$score out of 5. ${scoreGrading(score, word)}"
        }
        val line = when (score) {
            0 -> "Zero. The Monkeys checked twice. Nothing. Who ARE you?"
            1 -> "One out of five. A starter [WORD]. The Monkeys barely looked up from their tea."
            2 -> "Two out of five. A solid, professional [WORD] performance. Marks for consistency."
            3 -> "Three out of five. Weapons-grade. The Monkeys have opened a commemorative file."
            4 -> "Four out of five. The Monkeys are considering a documentary."
            else -> "Five. Out of five. A perfect score. The Monkeys are on their feet applauding, you magnificent [WORD]."
        }
        return line.replace("[WORD]", word)
    }

    /** Bespoke jibe per built-in crime; customs get filed to the permanent record. */
    private fun chargeLine(crime: String): String = when (crime.lowercase()) {
        "blacked out" ->
            "Blacked out: you lost actual hours of your life. The Monkeys saw all of it. They're not telling."
        "insulted someone" ->
            "Insulted someone: the insults were not as witty as they felt at the time. They never are."
        "offended someone" ->
            "Offended someone: offence given freely and generously. An apology tour may be required."
        "threw up" ->
            "Threw up: your stomach filed a formal complaint. So, possibly, did someone's shoes."
        "damaged a relationship" ->
            "Damaged a relationship: that one isn't funny, and you know it. Fix it today, not 'at some point'."
        "got in a fight" ->
            "Got in a fight: you're not in a film. You're a person with a back that hurts."
        "criminal offence" ->
            "Criminal offence: the Monkeys are a comedy act, not a legal defence."
        "drunk texting" ->
            "Drunk texting: the sent folder is a crime scene. Captain Paranoia has already read it twice."
        "lost phone / wallet / keys" ->
            "Lost property: you paid good money to misplace your own possessions. Bold strategy."
        else ->
            "\"$crime\": a new one for the collection. Added to your permanent record."
    }

    private fun promisesLine(brokenPromises: Int): String? {
        if (brokenPromises <= 0) return null
        return if (brokenPromises == 1)
            "And one of sober-you's promises didn't survive the night. He keeps score, you know."
        else
            "And $brokenPromises of sober-you's promises didn't survive the night. He keeps score, you know."
    }

    private fun roastCloser(score: Int, crimeCount: Int): String {
        if (score == 0 && crimeCount == 0) {
            return "Whatever you did last night — bottle that instead."
        }
        if (score >= 3 || crimeCount >= 3) {
            return "Could do better — you know that.\n\n" +
                "A word without the monkeys: if mornings like this are becoming a pattern, " +
                "it's fair to ask whether alcohol is giving you more than it takes these days. " +
                "Plenty of people try a month off and never look back. No pressure, no badge — " +
                "just a door, open. There's a judgement-free place to start in Settings."
        }
        return "Could do better. Next time, the plan is the boss.\n\n" +
            "Quiet question while the kettle's on: did the drinks past the plan actually add anything?"
    }

    // MARK: - Deletion friction

    /** Two-stage sarcasm for anyone trying to erase history (SPEC hard rule 3). */
    data class DeleteWarnings(val first: String, val second: String)

    val deleteWarnings = DeleteWarnings(
        first = "Deleting this won't delete the memory of it. Everyone who was there still knows.",
        second = "Last chance. The videos, the verdict, the evidence — gone. The Monkeys will still remember. They always remember.",
    )

    // MARK: - Private

    private fun line(bank: Map<Brutality, List<String>>, brutality: Brutality, variant: Int, word: String): String {
        val lines = bank[brutality] ?: emptyList()
        if (lines.isEmpty()) return ""
        val index = ((variant % lines.size) + lines.size) % lines.size
        return lines[index].replace("[WORD]", word)
    }
}
