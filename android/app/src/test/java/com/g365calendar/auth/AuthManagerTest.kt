package com.g365calendar.auth

import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.ICurrentAccountResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthManagerTest {

    private lateinit var msalApp: ISingleAccountPublicClientApplication
    private lateinit var authManager: AuthManager

    @BeforeEach
    fun setup() {
        msalApp = mockk(relaxed = true)
        authManager = AuthManager(msalApp)
    }

    @Test
    fun `isSignedIn returns false when no account`() {
        val accountResult = mockk<ICurrentAccountResult>()
        every { accountResult.currentAccount } returns null
        every { msalApp.currentAccount } returns accountResult

        assertFalse(authManager.isSignedIn())
    }

    @Test
    fun `isSignedIn returns true when account exists`() {
        val account = mockk<IAccount>()
        val accountResult = mockk<ICurrentAccountResult>()
        every { accountResult.currentAccount } returns account
        every { msalApp.currentAccount } returns accountResult

        assertTrue(authManager.isSignedIn())
    }

    @Test
    fun `SCOPES contains Calendars Read`() {
        assertTrue(AuthManager.SCOPES.contains("Calendars.Read"))
    }
}
