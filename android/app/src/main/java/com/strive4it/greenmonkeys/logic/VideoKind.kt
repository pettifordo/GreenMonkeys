package com.strive4it.greenmonkeys.logic

/** Ported from iOS `Models/SessionVideo.swift`. Raw values are stored — don't rename. */
enum class VideoKind(val rawValue: String, val label: String) {
    PLAN("plan", "The Plan"),
    DRUNK("drunk", "The Evidence"),
    MORNING_AFTER("morningAfter", "The Morning After");

    companion object {
        fun fromRaw(raw: String): VideoKind? = entries.firstOrNull { it.rawValue == raw }
    }
}
