package com.strive4it.greenmonkeys.logic

/**
 * A judged commitment, reduced to a value type so pattern logic stays pure and
 * testable. `key` groups built-ins by kind and custom promises by their text.
 */
data class CommitmentRecord(
    val key: String,
    val label: String,
    val wasBroken: Boolean,
)

data class PromiseHistory(
    val key: String,
    val label: String,
    val timesPromised: Int,
    val timesBroken: Int,
) {
    val timesKept: Int get() = timesPromised - timesBroken
}

data class CrimeCount(val crime: String, val count: Int)

/**
 * Computes the user's track record: the composite idiot score, which promises
 * they break repeatedly, and the booze-crime charge sheet (SPEC §1.4).
 * Operates on value types only. Ported from iOS `Services/PatternService.swift`.
 */
object PatternService {

    /** History for one promise (built-in kind or custom text) across all judged sessions. */
    fun history(forKey: String, records: List<CommitmentRecord>): PromiseHistory {
        val matching = records.filter { it.key == forKey }
        return PromiseHistory(
            key = forKey,
            label = matching.firstOrNull()?.label ?: forKey,
            timesPromised = matching.size,
            timesBroken = matching.count { it.wasBroken },
        )
    }

    /** Promises broken more than once, worst first — the repeat offenders. */
    fun repeatOffenders(records: List<CommitmentRecord>): List<PromiseHistory> {
        val keys = records.map { it.key }.distinct()
        return keys
            .map { history(it, records) }
            .filter { it.timesBroken >= 2 }
            .sortedByDescending { it.timesBroken }
    }

    /** Fraction of judged sessions that scored >= 1. null when no history. */
    fun idiotRate(scores: List<Int>): Double? {
        if (scores.isEmpty()) return null
        return scores.count { it > 0 }.toDouble() / scores.size.toDouble()
    }

    /** Mean idiot score across judged sessions. null when no history. */
    fun averageScore(scores: List<Int>): Double? {
        if (scores.isEmpty()) return null
        return scores.sum().toDouble() / scores.size.toDouble()
    }

    /** Every crime confessed to, tallied, most-committed first (ties alphabetical). */
    fun crimeCounts(crimeLists: List<List<String>>): List<CrimeCount> {
        val counts = mutableMapOf<String, Int>()
        for (crimes in crimeLists) {
            for (crime in crimes) {
                counts[crime] = (counts[crime] ?: 0) + 1
            }
        }
        return counts
            .map { CrimeCount(crime = it.key, count = it.value) }
            .sortedWith(compareByDescending<CrimeCount> { it.count }.thenBy { it.crime })
    }
}
