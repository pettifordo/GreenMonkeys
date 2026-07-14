package com.strive4it.greenmonkeys.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogLogicTests {

    @Test
    fun rememberTrimsAndAppends() {
        assertEquals(listOf("No karaoke"), CatalogLogic.remember(emptyList(), "  No karaoke "))
    }

    @Test
    fun rememberDedupesCaseInsensitively() {
        val existing = listOf("No karaoke")
        assertEquals(existing, CatalogLogic.remember(existing, "no KARAOKE"))
    }

    @Test
    fun rememberIgnoresBlanks() {
        assertEquals(emptyList<String>(), CatalogLogic.remember(emptyList(), "   "))
    }

    @Test
    fun firstUseOrderIsPreserved() {
        var list = emptyList<String>()
        list = CatalogLogic.remember(list, "No karaoke")
        list = CatalogLogic.remember(list, "Text the dog sitter")
        list = CatalogLogic.remember(list, "no karaoke")
        assertEquals(listOf("No karaoke", "Text the dog sitter"), list)
    }

    @Test
    fun builtInCrimesNeverJoinTheCustomList() {
        assertEquals(emptyList<String>(), CatalogLogic.rememberCrime(emptyList(), "blacked out"))
        assertEquals(listOf("Sang Wonderwall twice"), CatalogLogic.rememberCrime(emptyList(), "Sang Wonderwall twice"))
    }

    @Test
    fun builtInCrimeListMatchesIOS() {
        assertEquals(9, CatalogLogic.builtInCrimes.size)
        assertEquals("Blacked out", CatalogLogic.builtInCrimes.first())
        assertEquals("Lost phone / wallet / keys", CatalogLogic.builtInCrimes.last())
    }
}
