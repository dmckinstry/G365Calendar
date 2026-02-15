package com.g365calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g365calendar.data.model.Calendar
import com.g365calendar.data.preferences.CalendarPreferences
import com.g365calendar.data.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CalendarSelectionViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val calendarPreferences: CalendarPreferences,
) : ViewModel() {

    private val _calendars = MutableStateFlow<List<Calendar>>(emptyList())
    val calendars: StateFlow<List<Calendar>> = _calendars.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val selectedCalendarIds: StateFlow<Set<String>> = calendarPreferences.selectedCalendarIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun loadCalendars() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _calendars.value = calendarRepository.getCalendars()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load calendars")
                _error.value = e.message ?: "Failed to load calendars"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleCalendar(calendarId: String) {
        viewModelScope.launch {
            calendarPreferences.toggleCalendar(calendarId)
        }
    }
}
