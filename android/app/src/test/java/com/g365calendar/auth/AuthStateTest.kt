package com.g365calendar.auth

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthStateTest {
    private lateinit var authState: AuthState

    @BeforeEach
    fun setup() {
        authState = AuthState()
    }

    @Test
    fun `initial state is empty and idle`() =
        runTest {
            val state = authState.state.first()
            assertTrue(state.accounts.isEmpty())
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
        }

    @Test
    fun `setAccounts updates state`() =
        runTest {
            authState.setAccounts(listOf(AuthAccount(id = "account-1", displayName = "Test User", username = "user@example.com")))

            val state = authState.state.first()

            assertEquals(1, state.accounts.size)
            assertEquals("Test User", state.accounts.first().displayName)
            assertFalse(state.isLoading)
        }

    @Test
    fun `setLoading keeps current accounts and marks loading`() =
        runTest {
            val accounts = listOf(AuthAccount(id = "account-1", displayName = "User", username = "user@example.com"))

            authState.setLoading(accounts)

            val state = authState.state.first()

            assertEquals(accounts, state.accounts)
            assertTrue(state.isLoading)
        }

    @Test
    fun `setError updates state with message`() =
        runTest {
            val accounts = listOf(AuthAccount(id = "account-1", displayName = "User", username = "user@example.com"))

            authState.setError("Something went wrong", accounts)

            val state = authState.state.first()

            assertEquals(accounts, state.accounts)
            assertEquals("Something went wrong", state.errorMessage)
            assertFalse(state.isLoading)
        }
}
