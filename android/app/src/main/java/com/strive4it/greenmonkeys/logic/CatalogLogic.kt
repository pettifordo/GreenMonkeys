package com.strive4it.greenmonkeys.logic

/**
 * Pure rules for the user-extendable catalogs (custom promises and booze
 * crimes): anything typed once is remembered, deduped case-insensitively,
 * built-ins never duplicated or removed (SPEC §2a). Persistence lives in
 * SettingsRepository; the rules live here so they stay JVM-testable.
 * Ported from iOS `Services/CatalogService.swift`.
 */
object CatalogLogic {

    val builtInCrimes = listOf(
        "Blacked out",
        "Insulted someone",
        "Offended someone",
        "Threw up",
        "Damaged a relationship",
        "Got in a fight",
        "Criminal offence",
        "Drunk texting",
        "Lost phone / wallet / keys",
    )

    /**
     * Returns [existing] with [text] appended, unless it's blank or already
     * present (case-insensitive, trimmed). Order is preserved — first use wins.
     */
    fun remember(existing: List<String>, text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return existing
        if (existing.any { it.equals(trimmed, ignoreCase = true) }) return existing
        return existing + trimmed
    }

    /** Like [remember], but built-in crimes never join the custom list. */
    fun rememberCrime(existingCustoms: List<String>, text: String): List<String> {
        val trimmed = text.trim()
        if (builtInCrimes.any { it.equals(trimmed, ignoreCase = true) }) return existingCustoms
        return remember(existingCustoms, trimmed)
    }
}
