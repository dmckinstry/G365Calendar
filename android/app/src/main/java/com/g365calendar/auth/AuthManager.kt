package com.g365calendar.auth

import android.app.Activity
import com.g365calendar.BuildConfig
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Base64

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
        @Volatile
        private var cachedAccessToken: String? = null

        companion object {
            private const val MISSING_TOKEN_RESPONSE_VALUE = "Missing from the token response"

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
            val account = getCurrentAccountOrNull(publicClientApplication, "silent token acquisition") ?: return null
            return try {
                suspendCancellableCoroutine { continuation ->
                    val params =
                        AcquireTokenSilentParameters.Builder()
                            .forAccount(account)
                            .fromAuthority(BuildConfig.MSAL_AUTHORITY_URL)
                            .withScopes(SCOPES.toList())
                            .withCallback(
                                object : AuthenticationCallback {
                                    override fun onSuccess(result: IAuthenticationResult) {
                                        cachedAccessToken = result.accessToken
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
                getMsalAppOrThrow("interactive token acquisition")
            return try {
                acquireTokenInteractively(publicClientApplication, activity)
            } catch (exception: MsalClientException) {
                if (!exception.isSingleAccountMismatch()) {
                    throw exception
                }

                Timber.w(exception, "Clearing stale single-account session before retrying interactive sign-in")
                signOut(publicClientApplication)
                acquireTokenInteractively(publicClientApplication, activity)
            }
        }

        /** Signs out the current account. */
        suspend fun signOut(): Boolean {
            val publicClientApplication = getMsalAppOrNull("sign out") ?: return false
            return signOut(publicClientApplication)
        }

        private suspend fun signOut(
            publicClientApplication: ISingleAccountPublicClientApplication,
        ): Boolean {
            return try {
                suspendCancellableCoroutine { continuation ->
                    publicClientApplication.signOut(
                        object : ISingleAccountPublicClientApplication.SignOutCallback {
                            override fun onSignOut() {
                                cachedAccessToken = null
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
        suspend fun isSignedIn(): Boolean {
            val publicClientApplication = getMsalAppOrNull("cached account lookup") ?: return false
            return getCurrentAccountOrNull(publicClientApplication, "cached account lookup") != null
        }

        /** Returns the current access token, preferring silent refresh. */
        suspend fun getAccessToken(): String? {
            return acquireTokenSilently()?.accessToken ?: cachedAccessToken
        }

        fun getDisplayName(result: IAuthenticationResult?): String? {
            val account = result?.account
            return account?.displayNameOrNull()
                ?: result?.accessToken?.displayNameFromAccessToken()
        }

        private suspend fun getMsalAppOrNull(operation: String): ISingleAccountPublicClientApplication? {
            return try {
                withContext(Dispatchers.IO) { msalApp.get() }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "MSAL initialization failed during $operation")
                null
            }
        }

        private suspend fun acquireTokenInteractively(
            publicClientApplication: ISingleAccountPublicClientApplication,
            activity: Activity,
        ): IAuthenticationResult {
            return suspendCancellableCoroutine { continuation ->
                val params =
                    AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withScopes(SCOPES.toList())
                        .withCallback(
                            object : AuthenticationCallback {
                                override fun onSuccess(result: IAuthenticationResult) {
                                    cachedAccessToken = result.accessToken
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

        private fun MsalClientException.isSingleAccountMismatch(): Boolean {
            return message?.contains("does not match with the provided account", ignoreCase = true) == true
        }

        private suspend fun getMsalAppOrThrow(operation: String): ISingleAccountPublicClientApplication {
            return try {
                withContext(Dispatchers.IO) { msalApp.get() }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "MSAL initialization failed during $operation")
                throw AuthUnavailableException(
                    message =
                        buildString {
                            append("Authentication is unavailable on this build. ")
                            append("Verify the Microsoft Entra app uses the redirect URI ")
                            append(BuildConfig.MSAL_REDIRECT_URI)
                            append(" and that the same URI is registered in the app manifest.")
                        },
                    cause = throwable,
                )
            }
        }

        private suspend fun getCurrentAccountOrNull(
            publicClientApplication: ISingleAccountPublicClientApplication,
            operation: String,
        ): IAccount? {
            return try {
                withContext(Dispatchers.IO) {
                    publicClientApplication.currentAccount?.currentAccount
                }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "MSAL current account lookup failed during $operation")
                null
            }
        }

        private fun IAccount.displayNameOrNull(): String? {
            val claimsMap = claims
            val candidates =
                listOf(
                    claimsMap?.get("email") as? String,
                    claimsMap?.get("preferred_username") as? String,
                    username,
                    claimsMap?.get("name") as? String,
                )

            return candidates
                .firstOrNull { candidate ->
                    !candidate.isNullOrBlank() && candidate != MISSING_TOKEN_RESPONSE_VALUE
                }
        }

        private fun String.displayNameFromAccessToken(): String? {
            val payload = split('.').getOrNull(1) ?: return null
            return try {
                val normalizedPayload =
                    payload
                        .replace('-', '+')
                        .replace('_', '/')
                        .let { encoded ->
                            val padding = (4 - encoded.length % 4) % 4
                            encoded + "=".repeat(padding)
                        }
                val json = String(Base64.getDecoder().decode(normalizedPayload), Charsets.UTF_8)
                val tokenClaims = JSONObject(json)
                listOf(
                    tokenClaims.optString("email"),
                    tokenClaims.optString("preferred_username"),
                    tokenClaims.optString("unique_name"),
                    tokenClaims.optString("name"),
                ).firstOrNull { candidate ->
                    candidate.isNotBlank() && candidate != MISSING_TOKEN_RESPONSE_VALUE
                }
            } catch (throwable: Throwable) {
                Timber.w(throwable, "Failed to derive display name from access token")
                null
            }
        }
    }

class AuthCancelledException(message: String) : Exception(message)

class AuthUnavailableException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
