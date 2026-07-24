package com.g365calendar.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g365calendar.auth.AuthAccount
import com.g365calendar.auth.AuthManager
import com.g365calendar.auth.AuthState
import com.g365calendar.data.model.Calendar
import com.g365calendar.data.preferences.CalendarPreferences
import com.g365calendar.data.repository.CalendarRepository
import com.g365calendar.sync.SyncManager
import com.g365calendar.sync.SyncResult
import com.g365calendar.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authManager: AuthManager,
        private val authState: AuthState,
        private val calendarPreferences: CalendarPreferences,
        private val calendarRepository: CalendarRepository,
        private val syncManager: SyncManager,
        private val syncScheduler: SyncScheduler,
    ) : ViewModel() {
        val authStatus: StateFlow<AuthState.Status> = authState.state

        private val _calendarStates = MutableStateFlow<Map<String, CalendarListUiState>>(emptyMap())
        private val _selectedCalendarIdsByAccount = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

        val accounts: StateFlow<List<AccountHomeUiState>> =
            combine(authStatus, _calendarStates, _selectedCalendarIdsByAccount) { authStatus, calendarStates, selectedIdsByAccount ->
                authStatus.accounts.map { account ->
                    val calendarState = calendarStates[account.id] ?: CalendarListUiState.Idle
                    val selectedIds = selectedIdsByAccount[account.id].orEmpty()
                    val selectedCalendars =
                        when (calendarState) {
                            is CalendarListUiState.Success -> calendarState.calendars.filter { calendar -> calendar.id in selectedIds }
                            else -> emptyList()
                        }

                    AccountHomeUiState(
                        account = account,
                        selectedCalendars = selectedCalendars,
                        calendarState = calendarState,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val _syncStatus = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
        val syncStatus: StateFlow<SyncUiState> = _syncStatus.asStateFlow()

        private val _lastSyncTime = MutableStateFlow<Long?>(null)
        val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

        init {
            refreshAccounts()
        }

        fun addAccount(activity: Activity) {
            viewModelScope.launch {
                authState.setLoading()
                try {
                    authManager.acquireTokenInteractively(activity)
                    refreshAccounts()
                    triggerSync()
                } catch (e: Exception) {
                    Timber.e(e, "Add account failed")
                    authState.setError(e.message ?: "Add account failed")
                }
            }
        }

        fun clearAuthError() {
            authState.clearError(authStatus.value.accounts)
        }

        fun signOut(accountId: String) {
            viewModelScope.launch {
                val removed = authManager.removeAccount(accountId)
                if (!removed) {
                    authState.setError("Failed to sign out account")
                    return@launch
                }

                calendarPreferences.clearSelectedCalendars(accountId)
                _calendarStates.update { current -> current - accountId }
                _selectedCalendarIdsByAccount.update { current -> current - accountId }

                refreshAccounts(triggerCalendarRefresh = false)

                if (authStatus.value.accounts.isEmpty()) {
                    _syncStatus.value = SyncUiState.Idle
                    _lastSyncTime.value = null
                }
            }
        }

        fun refreshAccount(accountId: String) {
            viewModelScope.launch {
                refreshSelectedCalendarIds(accountId)
                loadCalendars(accountId)
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

        private fun refreshAccounts(triggerCalendarRefresh: Boolean = true) {
            viewModelScope.launch {
                val existingAccounts = authStatus.value.accounts
                authState.setLoading(existingAccounts)

                val accounts = authManager.getAccounts()
                authState.setAccounts(accounts)

                val accountIds = accounts.map(AuthAccount::id).toSet()
                _calendarStates.update { current -> current.filterKeys(accountIds::contains) }
                _selectedCalendarIdsByAccount.value =
                    accounts.associate { account ->
                        account.id to calendarPreferences.selectedCalendarIds(account.id).first()
                    }

                if (accounts.isEmpty()) {
                    syncScheduler.cancelPeriodicSync()
                    return@launch
                }

                syncScheduler.schedulePeriodicSync()

                if (triggerCalendarRefresh) {
                    accounts.forEach { account ->
                        loadCalendars(account.id)
                    }
                }
            }
        }

        private suspend fun refreshSelectedCalendarIds(accountId: String) {
            _selectedCalendarIdsByAccount.update { current ->
                current + (accountId to calendarPreferences.selectedCalendarIds(accountId).first())
            }
        }

        private suspend fun loadCalendars(accountId: String) {
            _calendarStates.update { current -> current + (accountId to CalendarListUiState.Loading) }

            val calendarState =
                try {
                    refreshSelectedCalendarIds(accountId)
                    CalendarListUiState.Success(calendarRepository.getCalendars(accountId))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load calendars for account $accountId")
                    CalendarListUiState.Error(e.message ?: "Failed to load calendars")
                }

            _calendarStates.update { current -> current + (accountId to calendarState) }
        }
    }

data class AccountHomeUiState(
    val account: AuthAccount,
    val selectedCalendars: List<Calendar>,
    val calendarState: CalendarListUiState,
)

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
