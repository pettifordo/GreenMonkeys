package com.strive4it.greenmonkeys.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class CommitmentKindTests {

    @Test
    fun rawValuesRoundTripAndUnknownFallsBackToCustom() {
        for (kind in CommitmentKind.entries) {
            assertEquals(kind, CommitmentKind.fromRaw(kind.rawValue))
        }
        assertEquals(CommitmentKind.CUSTOM, CommitmentKind.fromRaw("somethingFromTheFuture"))
    }

    @Test
    fun builtInsGroupByKindCustomsByTrimmedLowercasedText() {
        assertEquals("leaveBy", patternKey(CommitmentKind.LEAVE_BY, "23:00"))
        assertEquals("no karaoke", patternKey(CommitmentKind.CUSTOM, "  No Karaoke "))
    }

    @Test
    fun patternLabelUsesKindLabelExceptForCustoms() {
        assertEquals("Leave by", patternLabel(CommitmentKind.LEAVE_BY, "23:00"))
        assertEquals("No karaoke", patternLabel(CommitmentKind.CUSTOM, "No karaoke"))
    }

    @Test
    fun displayTextMatchesIOSCopy() {
        assertEquals("No more than 4 drinks", commitmentDisplayText(CommitmentKind.MAX_DRINKS, "4"))
        assertEquals("Leave by 23:00", commitmentDisplayText(CommitmentKind.LEAVE_BY, "23:00"))
        assertEquals("No driving — taxi or lift home", commitmentDisplayText(CommitmentKind.NO_DRIVING, ""))
        assertEquals("Sing zero Wonderwalls", commitmentDisplayText(CommitmentKind.CUSTOM, "Sing zero Wonderwalls"))
    }
}
