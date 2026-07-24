package com.g365calendar.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g365calendar.auth.AuthManager
import com.g365calendar.auth.AuthState
import com.g365calendar.data.model.Calendar
import com.g365calendar.data.preferences.CalendarPreferences
import com.g365calendar.data.repository.CalendarRepository
import com.g365calendar.sync.SyncManager
import com.g365calendar.sync.SyncResult
import com.g365calendar.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authManager: AuthManager,
        private val authState: AuthState,
        calendarPreferences: CalendarPreferences,
        private val calendarRepository: CalendarRepository,
        private val syncManager: SyncManager,
        private val syncScheduler: SyncScheduler,
    ) : ViewModel() {
        val authStatus: StateFlow<AuthState.Status> = authState.state

        private val selectedCalendarIds: StateFlow<Set<String>> =
            calendarPreferences.selectedCalendarIds
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

        private val _calendarState = MutableStateFlow<CalendarListUiState>(CalendarListUiState.Idle)
        val calendarState: StateFlow<CalendarListUiState> = _calendarState.asStateFlow()

        val selectedCalendars: StateFlow<List<Calendar>> =
            combine(selectedCalendarIds, authStatus, _calendarState) { selectedIds, _, calendarState ->
                when (calendarState) {
                    is CalendarListUiState.Success -> {
                        calendarState.calendars
                            .filter { calendar -> calendar.id in selectedIds }
                    }
                    else -> emptyList()
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val _syncStatus = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
        val syncStatus: StateFlow<SyncUiState> = _syncStatus.asStateFlow()

        private val _lastSyncTime = MutableStateFlow<Long?>(null)
        val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

        init {
            checkAuthStatus()
        }

        private fun checkAuthStatus() {
            viewModelScope.launch {
                authState.setLoading()
                val result = authManager.acquireTokenSilently()
                if (result != null) {
                    authState.setAuthenticated(authManager.getDisplayName(result))
                    syncScheduler.schedulePeriodicSync()
                    loadCalendars()
                } else {
                    authState.setUnauthenticated()
                }
            }
        }

        fun signIn(activity: Activity) {
            viewModelScope.launch {
                authState.setLoading()
                try {
                    val result = authManager.acquireTokenInteractively(activity)
                    authState.setAuthenticated(authManager.getDisplayName(result))
                    syncScheduler.schedulePeriodicSync()
                    loadCalendars()
                    triggerSync()
                } catch (e: Exception) {
                    Timber.e(e, "Sign in failed")
                    authState.setError(e.message ?: "Sign in failed")
                }
            }
        }

        fun signOut() {
            viewModelScope.launch {
                authManager.signOut()
                authState.setUnauthenticated()
                syncScheduler.cancelPeriodicSync()
                _syncStatus.value = SyncUiState.Idle
                _lastSyncTime.value = null
                _calendarState.value = CalendarListUiState.Idle
            }
        }

        fun refreshCalendars() {
            viewModelScope.launch {
                loadCalendars()
            }
        }

        fun triggerSync() {
            viewModelScope.launch {
                _syncStatus.value = SyncUiState.Syncing
                val result = syncManager.performSync()
                _syncStatus.value =
                    when (result) {
                        is SyncResult.Success -> {
                            _lastSyncTime.value = System.currentTimeMillis()
                            SyncUiState.Success(result.eventCount)
                        }
                        is SyncResult.NoCalendarsSelected -> SyncUiState.Error("No calendars selected")
                        is SyncResult.WatchNotConnected -> SyncUiState.Error("Watch not connected")
                        is SyncResult.Error -> SyncUiState.Error(result.message)
                    }
            }
        }

        private suspend fun loadCalendars() {
            _calendarState.value = CalendarListUiState.Loading
            _calendarState.value =
                try {
                    CalendarListUiState.Success(calendarRepository.getCalendars())
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load calendars for authenticated user")
                    CalendarListUiState.Error(e.message ?: "Failed to load calendars")
                }
        }
    }

sealed class SyncUiState {
    data object Idle : SyncUiState()

    data object Syncing : SyncUiState()

    data class Success(val eventCount: Int) : SyncUiState()

    data class Error(val message: String) : SyncUiState()
}

sealed class CalendarListUiState {
    data object Idle : CalendarListUiState()

    data object Loading : CalendarListUiState()

    data class Success(val calendars: List<Calendar>) : CalendarListUiState()

    data class Error(val message: String) : CalendarListUiState()
}
