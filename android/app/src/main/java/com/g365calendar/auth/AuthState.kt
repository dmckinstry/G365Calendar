package com.g365calendar.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Observable authentication state for UI consumption. */
@Singleton
class AuthState @Inject constructor() {

    private val _state = MutableStateFlow<Status>(Status.Unknown)
    val state: StateFlow<Status> = _state.asStateFlow()

    fun setAuthenticated(displayName: String?) {
        _state.value = Status.Authenticated(displayName)
    }

    fun setUnauthenticated() {
        _state.value = Status.Unauthenticated
    }

    fun setLoading() {
        _state.value = Status.Loading
    }

    fun setError(message: String) {
        _state.value = Status.Error(message)
    }

    sealed class Status {
        data object Unknown : Status()
        data object Loading : Status()
        data object Unauthenticated : Status()
        data class Authenticated(val displayName: String?) : Status()
        data class Error(val message: String) : Status()
    }
}
