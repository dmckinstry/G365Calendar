package com.g365calendar.auth

import com.g365calendar.BuildConfig
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import dagger.Lazy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthManagerTest {
    private lateinit var msalApp: IMultipleAccountPublicClientApplication
    private lateinit var authManager: AuthManager

    @BeforeEach
    fun setup() {
        msalApp = mockk(relaxed = true)
        authManager =
            AuthManager(
                object : Lazy<IMultipleAccountPublicClientApplication> {
                    override fun get(): IMultipleAccountPublicClientApplication = msalApp
                },
            )
    }

    @Test
    fun `getAccounts returns empty list when no account exists`() =
        runTest {
            every { msalApp.getAccounts(any()) } answers {
                firstArg<IPublicClientApplication.LoadAccountsCallback>().onTaskCompleted(emptyList())
            }

            assertTrue(authManager.getAccounts().isEmpty())
        }

    @Test
    fun `getAccounts returns mapped account details`() =
        runTest {
            val account = mockk<IAccount>()
            every { account.id } returns "account-1"
            every { account.username } returns "user@example.com"
            every { account.claims } returns mapOf("name" to "Test User")
            every { msalApp.getAccounts(any()) } answers {
                firstArg<IPublicClientApplication.LoadAccountsCallback>().onTaskCompleted(listOf(account))
            }

            val accounts = authManager.getAccounts()

            assertEquals(1, accounts.size)
            assertEquals("account-1", accounts[0].id)
            assertEquals("Test User", accounts[0].displayName)
            assertEquals("user@example.com", accounts[0].username)
        }

    @Test
    fun `SCOPES contains Calendars Read`() {
        assertTrue(AuthManager.SCOPES.contains("Calendars.Read"))
    }

    @Test
    fun `SCOPES matches configured graph scopes`() {
        assertTrue(AuthManager.SCOPES.contentEquals(BuildConfig.GRAPH_SCOPES.split(',').map(String::trim).filter(String::isNotEmpty).toTypedArray()))
    }
}
