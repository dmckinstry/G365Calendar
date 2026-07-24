package com.g365calendar.auth

import android.app.Activity
import com.g365calendar.BuildConfig
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import dagger.Lazy
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
class AuthManager
    @Inject
    constructor(
        private val msalApp: Lazy<ISingleAccountPublicClientApplication>,
    ) {
        companion object {
            val SCOPES =
                BuildConfig.GRAPH_SCOPES
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toTypedArray()
        }

        /** Attempts silent token acquisition; returns null if interactive auth is required. */
        suspend fun acquireTokenSilently(): IAuthenticationResult? {
            val publicClientApplication = getMsalAppOrNull("silent token acquisition") ?: return null
            val account = publicClientApplication.currentAccount?.currentAccount ?: return null
            return try {
                suspendCancellableCoroutine { continuation ->
                    val params =
                        AcquireTokenSilentParameters.Builder()
                            .forAccount(account)
                            .fromAuthority(account.authority)
                            .withScopes(SCOPES.toList())
                            .withCallback(
                                object : AuthenticationCallback {
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
                                },
                            )
                            .build()
                    publicClientApplication.acquireTokenSilentAsync(params)
                }
            } catch (throwable: Throwable) {
                Timber.w(throwable, "Silent token acquisition failed")
                null
            }
        }

        /** Launches interactive login flow. */
        suspend fun acquireTokenInteractively(activity: Activity): IAuthenticationResult {
            val publicClientApplication =
                getMsalAppOrNull("interactive token acquisition")
                    ?: throw AuthUnavailableException("Authentication is unavailable on this build")

            return suspendCancellableCoroutine { continuation ->
                val params =
                    AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withScopes(SCOPES.toList())
                        .withCallback(
                            object : AuthenticationCallback {
                                override fun onSuccess(result: IAuthenticationResult) {
                                    continuation.resume(result)
                                }

                                override fun onError(exception: MsalException) {
                                    Timber.e(exception, "Interactive token acquisition failed")
                                    continuation.resumeWithException(exception)
                                }

                                override fun onCancel() {
                                    continuation.resumeWithException(
                                        AuthCancelledException("User cancelled authentication"),
                                    )
                                }
                            },
                        )
                        .build()
                publicClientApplication.acquireToken(params)
            }
        }

        /** Signs out the current account. */
        suspend fun signOut(): Boolean {
            val publicClientApplication = getMsalAppOrNull("sign out") ?: return false
            return try {
                suspendCancellableCoroutine { continuation ->
                    publicClientApplication.signOut(
                        object : ISingleAccountPublicClientApplication.SignOutCallback {
                            override fun onSignOut() {
                                continuation.resume(true)
                            }

                            override fun onError(exception: MsalException) {
                                Timber.e(exception, "Sign out failed")
                                continuation.resume(false)
                            }
                        },
                    )
                }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Sign out failed")
                false
            }
        }

        /** Returns true if a cached account exists. */
        fun isSignedIn(): Boolean {
            val publicClientApplication = getMsalAppOrNull("cached account lookup") ?: return false
            return publicClientApplication.currentAccount?.currentAccount != null
        }

        /** Returns the current access token, preferring silent refresh. */
        suspend fun getAccessToken(): String? {
            return acquireTokenSilently()?.accessToken
        }

        private fun getMsalAppOrNull(operation: String): ISingleAccountPublicClientApplication? {
            return try {
                msalApp.get()
            } catch (throwable: Throwable) {
                Timber.e(throwable, "MSAL initialization failed during $operation")
                null
            }
        }
    }

class AuthCancelledException(message: String) : Exception(message)

class AuthUnavailableException(message: String) : Exception(message)
