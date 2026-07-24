package com.g365calendar.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
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
class CalendarSelectionViewModel
    @Inject
    constructor(
        private val calendarRepository: CalendarRepository,
        private val calendarPreferences: CalendarPreferences,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val accountId: String =
            requireNotNull(savedStateHandle.get<String>(ACCOUNT_ID_ARGUMENT)) { "Missing account ID" }
                .let(Uri::decode)

        private val _calendars = MutableStateFlow<List<Calendar>>(emptyList())
        val calendars: StateFlow<List<Calendar>> = _calendars.asStateFlow()

        private val _draftSelectedCalendarIds = MutableStateFlow<Set<String>>(emptySet())
        val draftSelectedCalendarIds: StateFlow<Set<String>> = _draftSelectedCalendarIds.asStateFlow()

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error.asStateFlow()

        val selectedCalendarIds: StateFlow<Set<String>> =
            calendarPreferences.selectedCalendarIds(accountId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

        private var lastPersistedSelection: Set<String>? = null

        fun initializeSelectionDraft(selectedIds: Set<String>) {
            if (lastPersistedSelection == selectedIds) {
                return
            }

            val shouldRefreshDraft =
                lastPersistedSelection == null || _draftSelectedCalendarIds.value == lastPersistedSelection

            lastPersistedSelection = selectedIds

            if (!shouldRefreshDraft) {
                return
            }

            _draftSelectedCalendarIds.value = selectedIds
        }

        fun loadCalendars() {
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                try {
                    _calendars.value = calendarRepository.getCalendars(accountId)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load calendars")
                    _error.value = e.message ?: "Failed to load calendars"
                } finally {
                    _isLoading.value = false
                }
            }
        }

        fun toggleCalendar(calendarId: String) {
            _draftSelectedCalendarIds.value =
                _draftSelectedCalendarIds.value.let { current ->
                    if (calendarId in current) current - calendarId else current + calendarId
                }
        }

        fun saveSelection(onSaved: () -> Unit) {
            viewModelScope.launch {
                calendarPreferences.setSelectedCalendars(accountId, _draftSelectedCalendarIds.value)
                lastPersistedSelection = _draftSelectedCalendarIds.value
                onSaved()
            }
        }

        fun discardSelectionChanges() {
            _draftSelectedCalendarIds.value = selectedCalendarIds.value
        }

        companion object {
            const val ACCOUNT_ID_ARGUMENT = "accountId"
        }
    }
