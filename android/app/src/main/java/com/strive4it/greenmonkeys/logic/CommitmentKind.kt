package com.strive4it.greenmonkeys.logic

/**
 * The kinds of promise sober-you can make. Raw values are stored — don't rename.
 * Ported from iOS `Models/Commitment.swift`; the pattern-key / display logic
 * lives here so it stays pure and JVM-testable.
 */
enum class CommitmentKind(val rawValue: String, val label: String, val symbol: String) {
    MAX_DRINKS("maxDrinks", "Max drinks", "🍺"),
    WATER_BETWEEN("waterBetween", "Water every other drink", "💧"),
    LEAVE_BY("leaveBy", "Leave by", "🕚"),
    NO_DRIVING("noDriving", "No driving", "🚕"),
    EAT_FIRST("eatFirst", "Eat before drinking", "🍔"),
    NO_SHOTS("noShots", "No shots", "🥃"),
    CUSTOM("custom", "Custom promise", "🤙");

    companion object {
        fun fromRaw(raw: String): CommitmentKind =
            entries.firstOrNull { it.rawValue == raw } ?: CUSTOM
    }
}

/**
 * Stable identity for pattern tracking: built-ins group by kind, customs by
 * their text (trimmed, case-insensitive).
 */
fun patternKey(kind: CommitmentKind, detail: String): String =
    if (kind == CommitmentKind.CUSTOM) detail.trim().lowercase() else kind.rawValue

/** Human name for pattern tracking. */
fun patternLabel(kind: CommitmentKind, detail: String): String =
    if (kind == CommitmentKind.CUSTOM) detail else kind.label

/** How a commitment reads in lists and notification summaries. */
fun commitmentDisplayText(kind: CommitmentKind, detail: String): String = when (kind) {
    CommitmentKind.MAX_DRINKS -> "No more than $detail drinks"
    CommitmentKind.WATER_BETWEEN -> "Water every other drink"
    CommitmentKind.LEAVE_BY -> "Leave by $detail"
    CommitmentKind.NO_DRIVING -> "No driving — taxi or lift home"
    CommitmentKind.EAT_FIRST -> "Eat before drinking"
    CommitmentKind.NO_SHOTS -> "No shots"
    CommitmentKind.CUSTOM -> detail
}
