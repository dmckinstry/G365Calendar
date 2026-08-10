package com.g365calendar.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Observable authentication state for UI consumption. */
@Singleton
class AuthState
    @Inject
    constructor() {
        private val _state = MutableStateFlow(Status())
        val state: StateFlow<Status> = _state.asStateFlow()

        fun setAccounts(accounts: List<AuthAccount>) {
            _state.value = Status(accounts = accounts)
        }

        fun setLoading(accounts: List<AuthAccount> = _state.value.accounts) {
            _state.value = Status(accounts = accounts, isLoading = true)
        }

        fun setError(
            message: String,
            accounts: List<AuthAccount> = _state.value.accounts,
        ) {
            _state.value = Status(accounts = accounts, errorMessage = message)
        }

        fun clearError(accounts: List<AuthAccount> = _state.value.accounts) {
            _state.value = Status(accounts = accounts)
        }

        data class Status(
            val accounts: List<AuthAccount> = emptyList(),
            val isLoading: Boolean = false,
            val errorMessage: String? = null,
        )
    }
