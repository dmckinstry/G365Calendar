package com.g365calendar.sync

import com.g365calendar.auth.AuthManager
import com.g365calendar.data.model.DisplayEvent
import com.g365calendar.data.preferences.CalendarPreferences
import com.g365calendar.data.repository.CalendarRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedEventsProvider
    @Inject
    constructor(
        private val authManager: AuthManager,
        private val calendarRepository: CalendarRepository,
        private val calendarPreferences: CalendarPreferences,
    ) {
        suspend fun loadSelectedEvents(): SelectedEventsLoadResult {
            val accounts = authManager.getAccounts()
            if (accounts.isEmpty()) {
                return SelectedEventsLoadResult(events = emptyList(), hasAvailableSelectedCalendars = false)
            }

            var hasAvailableSelectedCalendars = false
            val events = mutableListOf<DisplayEvent>()

            for (account in accounts) {
                val selectedIds = calendarPreferences.selectedCalendarIds(account.id).first()
                if (selectedIds.isEmpty()) {
                    continue
                }

                val allCalendars = calendarRepository.getCalendars(account.id)
                val selectedCalendars = allCalendars.filter { it.id in selectedIds }
                if (selectedCalendars.isEmpty()) {
                    continue
                }

                hasAvailableSelectedCalendars = true
                events += calendarRepository.getEvents(account.id, selectedCalendars)
            }

            return SelectedEventsLoadResult(
                events = events,
                hasAvailableSelectedCalendars = hasAvailableSelectedCalendars,
            )
        }
    }

data class SelectedEventsLoadResult(
    val events: List<DisplayEvent>,
    val hasAvailableSelectedCalendars: Boolean,
)
