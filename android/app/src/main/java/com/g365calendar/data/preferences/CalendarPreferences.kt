package com.g365calendar.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "g365_prefs")

/** Persists user-selected calendar IDs using Jetpack DataStore. */
@Singleton
class CalendarPreferences
    @Inject
    constructor(
        private val context: Context,
    ) {
        fun selectedCalendarIds(accountId: String): Flow<Set<String>> {
            val key = selectedCalendarsKey(accountId)
            return context.dataStore.data.map { prefs -> prefs[key] ?: emptySet() }
        }

        suspend fun setSelectedCalendars(
            accountId: String,
            ids: Set<String>,
        ) {
            val key = selectedCalendarsKey(accountId)
            context.dataStore.edit { prefs ->
                prefs[key] = ids
            }
        }

        suspend fun clearSelectedCalendars(accountId: String) {
            val key = selectedCalendarsKey(accountId)
            context.dataStore.edit { prefs ->
                prefs.remove(key)
            }
        }

        private fun selectedCalendarsKey(accountId: String): Preferences.Key<Set<String>> {
            val sanitizedAccountId =
                accountId
                    .map { character -> if (character.isLetterOrDigit()) character else '_' }
                    .joinToString(separator = "")
            return stringSetPreferencesKey("selected_calendar_ids_$sanitizedAccountId")
        }
    }
