package com.g365calendar.auth

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `initial state is Unknown`() = runTest {
        val state = authState.state.first()
        assertTrue(state is AuthState.Status.Unknown)
    }

    @Test
    fun `setAuthenticated updates state`() = runTest {
        authState.setAuthenticated("Test User")
        val state = authState.state.first()
        assertTrue(state is AuthState.Status.Authenticated)
        assertEquals("Test User", (state as AuthState.Status.Authenticated).displayName)
    }

    @Test
    fun `setUnauthenticated updates state`() = runTest {
        authState.setAuthenticated("User")
        authState.setUnauthenticated()
        val state = authState.state.first()
        assertTrue(state is AuthState.Status.Unauthenticated)
    }

    @Test
    fun `setLoading updates state`() = runTest {
        authState.setLoading()
        val state = authState.state.first()
        assertTrue(state is AuthState.Status.Loading)
    }

    @Test
    fun `setError updates state with message`() = runTest {
        authState.setError("Something went wrong")
        val state = authState.state.first()
        assertTrue(state is AuthState.Status.Error)
        assertEquals("Something went wrong", (state as AuthState.Status.Error).message)
    }
}
