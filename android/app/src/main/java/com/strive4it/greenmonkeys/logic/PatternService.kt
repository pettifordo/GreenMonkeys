package com.strive4it.greenmonkeys.logic

import java.time.Instant

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

/** One night's chartable facts, flattened from a judged plan. */
data class NightRecord(
    val date: Instant,
    val score: Int,
    val crimes: Int,
    val brokenPromises: Int,
)

/** Chart aggregation buckets. */
enum class ChartPeriod(val label: String) {
    NIGHT("Night"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
}

/** A period bucket for the charts: mean score plus summed misdeeds. */
data class PeriodPoint(
    val periodStart: Instant,
    val averageScore: Double,
    val crimes: Int,
    val brokenPromises: Int,
    val nights: Int,
)

/** Simple least-squares trend over time: y = slope × days-since-first + intercept. */
data class TrendLine(
    val slope: Double,
    val intercept: Double,
    val firstDate: Instant,
) {
    /** Projected value at a date, clamped to the 0–5 score scale. */
    fun valueAt(date: Instant): Double {
        val days = (date.epochSecond - firstDate.epochSecond) / 86_400.0
        return (slope * days + intercept).coerceIn(0.0, 5.0)
    }
}

/**
 * Computes the user's track record: the composite idiot score, which promises
 * they break repeatedly, and the booze-crime charge sheet (SPEC §1.4).
 * Operates on value types only. Ported from iOS `Services/PatternService.swift`.
 */
object PatternService {

    // MARK: - Chart aggregation

    /**
     * Groups nights into period buckets (mean score, summed misdeeds),
     * sorted chronologically. NIGHT passes nights through unchanged.
     */
    fun aggregate(
        nights: List<NightRecord>,
        period: ChartPeriod,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): List<PeriodPoint> {
        if (period == ChartPeriod.NIGHT) {
            return nights
                .sortedBy { it.date }
                .map {
                    PeriodPoint(
                        periodStart = it.date,
                        averageScore = it.score.toDouble(),
                        crimes = it.crimes,
                        brokenPromises = it.brokenPromises,
                        nights = 1,
                    )
                }
        }

        fun bucketStart(date: Instant): Instant {
            val local = date.atZone(zone).toLocalDate()
            val start = when (period) {
                ChartPeriod.WEEK -> local.with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1)
                ChartPeriod.MONTH -> local.withDayOfMonth(1)
                ChartPeriod.YEAR -> local.withDayOfYear(1)
                ChartPeriod.NIGHT -> local
            }
            return start.atStartOfDay(zone).toInstant()
        }

        return nights
            .groupBy { bucketStart(it.date) }
            .map { (start, group) ->
                PeriodPoint(
                    periodStart = start,
                    averageScore = group.sumOf { it.score }.toDouble() / group.size,
                    crimes = group.sumOf { it.crimes },
                    brokenPromises = group.sumOf { it.brokenPromises },
                    nights = group.size,
                )
            }
            .sortedBy { it.periodStart }
    }

    /**
     * Least-squares fit over (date, value). null with fewer than 3 points or a
     * zero time spread — predicting from two nights is astrology.
     */
    fun trend(dates: List<Instant>, values: List<Double>): TrendLine? {
        if (dates.size < 3 || dates.size != values.size) return null
        val first = dates.minOrNull() ?: return null
        val last = dates.maxOrNull() ?: return null
        if (last <= first) return null

        val xs = dates.map { (it.epochSecond - first.epochSecond) / 86_400.0 }
        val n = xs.size.toDouble()
        val sumX = xs.sum()
        val sumY = values.sum()
        val sumXY = xs.zip(values).sumOf { it.first * it.second }
        val sumXX = xs.sumOf { it * it }
        val denominator = n * sumXX - sumX * sumX
        if (denominator == 0.0) return null

        val slope = (n * sumXY - sumX * sumY) / denominator
        val intercept = (sumY - slope * sumX) / n
        return TrendLine(slope = slope, intercept = intercept, firstDate = first)
    }

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
