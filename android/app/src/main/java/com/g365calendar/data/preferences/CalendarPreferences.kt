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
class CalendarPreferences @Inject constructor(
    private val context: Context,
) {
    companion object {
        private val SELECTED_CALENDARS = stringSetPreferencesKey("selected_calendar_ids")
    }

    val selectedCalendarIds: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[SELECTED_CALENDARS] ?: emptySet() }

    suspend fun setSelectedCalendars(ids: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_CALENDARS] = ids
        }
    }

    suspend fun toggleCalendar(calendarId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SELECTED_CALENDARS] ?: emptySet()
            prefs[SELECTED_CALENDARS] = if (calendarId in current) {
                current - calendarId
            } else {
                current + calendarId
            }
        }
    }
}
