package com.strive4it.greenmonkeys.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.strive4it.greenmonkeys.logic.Brutality
import com.strive4it.greenmonkeys.logic.CatalogLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * User-configurable settings + the remembered catalogs, mirroring iOS
 * `AppSettings` and `CatalogService`. Exposed as Flows so the UI re-renders
 * the moment the word/noun changes (lesson §6.3).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val insultWord = stringPreferencesKey("insultWord")
        val sessionNoun = stringPreferencesKey("sessionNoun")
        val brutality = intPreferencesKey("brutality")
        val morningAfterHour = intPreferencesKey("morningAfterHour")
        val appLockEnabled = booleanPreferencesKey("appLockEnabled")
        val firstUseDate = longPreferencesKey("firstUseDate")
        val customPromises = stringPreferencesKey("customPromises")
        val customCrimes = stringPreferencesKey("customCrimes")
    }

    companion object {
        // Kept deliberately mild — anyone wanting a truly terrible word can type their own.
        val insultPresets = listOf("idiot", "drunk", "embarrassment", "liability")
        val sessionNounPresets = listOf("Session", "Night Out", "Drink", "Cheeky One")
        private const val LIST_SEPARATOR = '\u001F'
    }

    // MARK: - Settings

    val insultWord: Flow<String> =
        context.settingsStore.data.map { it[Keys.insultWord] ?: "idiot" }

    /** What the user calls a drinking occasion — drives buttons, empty states, titles. */
    val sessionNoun: Flow<String> =
        context.settingsStore.data.map { it[Keys.sessionNoun] ?: "Session" }

    val brutality: Flow<Brutality> =
        context.settingsStore.data.map { prefs ->
            Brutality.entries.firstOrNull { it.value == (prefs[Keys.brutality] ?: 1) }
                ?: Brutality.STANDARD
        }

    val morningAfterHour: Flow<Int> =
        context.settingsStore.data.map { it[Keys.morningAfterHour] ?: 9 }

    /** Default ON: this app holds videos of you drunk (SPEC §5). */
    val appLockEnabled: Flow<Boolean> =
        context.settingsStore.data.map { it[Keys.appLockEnabled] ?: true }

    suspend fun setInsultWord(value: String) = edit { it[Keys.insultWord] = value }
    suspend fun setSessionNoun(value: String) = edit { it[Keys.sessionNoun] = value }
    suspend fun setBrutality(value: Brutality) = edit { it[Keys.brutality] = value.value }
    suspend fun setMorningAfterHour(value: Int) = edit { it[Keys.morningAfterHour] = value }
    suspend fun setAppLockEnabled(value: Boolean) = edit { it[Keys.appLockEnabled] = value }

    /** Anchor for the streak before any idiot verdict exists. Set once on first launch. */
    suspend fun firstUseDate(): Instant {
        val stored = context.settingsStore.data.first()[Keys.firstUseDate]
        if (stored != null) return Instant.ofEpochMilli(stored)
        val now = Instant.now()
        edit { it[Keys.firstUseDate] = now.toEpochMilli() }
        return now
    }

    // MARK: - Catalogs (SPEC §2a — the Monkeys never forget)

    val customPromises: Flow<List<String>> =
        context.settingsStore.data.map { decodeList(it[Keys.customPromises]) }

    val customCrimes: Flow<List<String>> =
        context.settingsStore.data.map { decodeList(it[Keys.customCrimes]) }

    val allCrimes: Flow<List<String>> =
        customCrimes.map { CatalogLogic.builtInCrimes + it }

    suspend fun rememberPromise(text: String) = edit { prefs ->
        prefs[Keys.customPromises] =
            encodeList(CatalogLogic.remember(decodeList(prefs[Keys.customPromises]), text))
    }

    suspend fun removeCustomPromise(text: String) = edit { prefs ->
        prefs[Keys.customPromises] =
            encodeList(decodeList(prefs[Keys.customPromises]).filter { it != text })
    }

    suspend fun rememberCrime(text: String) = edit { prefs ->
        prefs[Keys.customCrimes] =
            encodeList(CatalogLogic.rememberCrime(decodeList(prefs[Keys.customCrimes]), text))
    }

    suspend fun removeCustomCrime(text: String) = edit { prefs ->
        prefs[Keys.customCrimes] =
            encodeList(decodeList(prefs[Keys.customCrimes]).filter { it != text })
    }

    // MARK: - Private

    private suspend fun edit(transform: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsStore.edit(transform)
    }

    private fun decodeList(raw: String?): List<String> =
        raw?.takeIf { it.isNotEmpty() }?.split(LIST_SEPARATOR) ?: emptyList()

    private fun encodeList(values: List<String>): String =
        values.joinToString(LIST_SEPARATOR.toString())
}
