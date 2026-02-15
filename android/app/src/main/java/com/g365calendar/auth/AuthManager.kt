package com.g365calendar.auth

import android.app.Activity
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages MSAL authentication with OAuth 2.0 Authorization Code + PKCE.
 * Handles login, logout, silent token refresh, and token caching.
 */
@Singleton
class AuthManager @Inject constructor(
    private val msalApp: ISingleAccountPublicClientApplication,
) {
    companion object {
        val SCOPES = arrayOf("Calendars.Read")
    }

    /** Attempts silent token acquisition; returns null if interactive auth is required. */
    suspend fun acquireTokenSilently(): IAuthenticationResult? {
        val account = msalApp.currentAccount?.currentAccount ?: return null
        return try {
            suspendCancellableCoroutine { continuation ->
                val params = AcquireTokenSilentParameters.Builder()
                    .forAccount(account)
                    .fromAuthority(account.authority)
                    .withScopes(SCOPES.toList())
                    .withCallback(object : AuthenticationCallback {
                        override fun onSuccess(result: IAuthenticationResult) {
                            continuation.resume(result)
                        }

                        override fun onError(exception: MsalException) {
                            Timber.w(exception, "Silent token acquisition failed")
                            continuation.resume(null)
                        }

                        override fun onCancel() {
                            continuation.resume(null)
                        }
                    })
                    .build()
                msalApp.acquireTokenSilentAsync(params)
            }
        } catch (e: Exception) {
            Timber.w(e, "Silent token acquisition failed")
            null
        }
    }

    /** Launches interactive login flow. */
    suspend fun acquireTokenInteractively(activity: Activity): IAuthenticationResult {
        return suspendCancellableCoroutine { continuation ->
            val params = AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activity)
                .withScopes(SCOPES.toList())
                .withCallback(object : AuthenticationCallback {
                    override fun onSuccess(result: IAuthenticationResult) {
                        continuation.resume(result)
                    }

                    override fun onError(exception: MsalException) {
                        Timber.e(exception, "Interactive token acquisition failed")
                        continuation.resumeWithException(exception)
                    }

                    override fun onCancel() {
                        continuation.resumeWithException(
                            AuthCancelledException("User cancelled authentication")
                        )
                    }
                })
                .build()
            msalApp.acquireToken(params)
        }
    }

    /** Signs out the current account. */
    suspend fun signOut(): Boolean {
        return try {
            suspendCancellableCoroutine { continuation ->
                msalApp.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                    override fun onSignOut() {
                        continuation.resume(true)
                    }

                    override fun onError(exception: MsalException) {
                        Timber.e(exception, "Sign out failed")
                        continuation.resume(false)
                    }
                })
            }
        } catch (e: Exception) {
            Timber.e(e, "Sign out failed")
            false
        }
    }

    /** Returns true if a cached account exists. */
    fun isSignedIn(): Boolean {
        return msalApp.currentAccount?.currentAccount != null
    }

    /** Returns the current access token, preferring silent refresh. */
    suspend fun getAccessToken(): String? {
        return acquireTokenSilently()?.accessToken
    }
}

class AuthCancelledException(message: String) : Exception(message)
