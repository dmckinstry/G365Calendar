package com.g365calendar.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g365calendar.auth.AuthManager
import com.g365calendar.auth.AuthState
import com.g365calendar.sync.SyncManager
import com.g365calendar.sync.SyncResult
import com.g365calendar.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val authState: AuthState,
    private val syncManager: SyncManager,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    val authStatus: StateFlow<AuthState.Status> = authState.state

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
                authState.setAuthenticated(result.account?.username)
                syncScheduler.schedulePeriodicSync()
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
                authState.setAuthenticated(result.account?.username)
                syncScheduler.schedulePeriodicSync()
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
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _syncStatus.value = SyncUiState.Syncing
            val result = syncManager.performSync()
            _syncStatus.value = when (result) {
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
}

sealed class SyncUiState {
    data object Idle : SyncUiState()
    data object Syncing : SyncUiState()
    data class Success(val eventCount: Int) : SyncUiState()
    data class Error(val message: String) : SyncUiState()
}
