package com.g365calendar.auth

import android.app.Activity
import com.g365calendar.BuildConfig
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
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
        private val msalApp: Lazy<IMultipleAccountPublicClientApplication>,
    ) {
        private val cachedAccessTokens = ConcurrentHashMap<String, String>()

        companion object {
            private const val MISSING_TOKEN_RESPONSE_VALUE = "Missing from the token response"

            val SCOPES =
                BuildConfig.GRAPH_SCOPES
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toTypedArray()
        }

        suspend fun getAccounts(): List<AuthAccount> {
            val publicClientApplication = getMsalAppOrNull("account lookup") ?: return emptyList()
            return getAccountsOrNull(publicClientApplication, "account lookup")
                .orEmpty()
                .map(::toAuthAccount)
        }

        /** Attempts silent token acquisition for a specific account. */
        suspend fun acquireTokenSilently(accountId: String): IAuthenticationResult? {
            val publicClientApplication = getMsalAppOrNull("silent token acquisition") ?: return null
            val account = getAccountOrNull(publicClientApplication, accountId, "silent token acquisition") ?: return null
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
                                        result.account?.let { authenticatedAccount ->
                                            cachedAccessTokens[toAccountId(authenticatedAccount)] = result.accessToken
                                        }
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

        /** Launches interactive login flow to add an account. */
        suspend fun acquireTokenInteractively(activity: Activity): AuthAccount {
            val publicClientApplication = getMsalAppOrThrow("interactive token acquisition")
            val result = acquireTokenInteractively(publicClientApplication, activity)
            val account = result.account
                ?: throw AuthUnavailableException("Authenticated account details were not returned by Microsoft Authentication Library")
            return toAuthAccount(account)
        }

        /** Removes a previously added account. */
        suspend fun removeAccount(accountId: String): Boolean {
            val publicClientApplication = getMsalAppOrNull("account removal") ?: return false
            val account = getAccountOrNull(publicClientApplication, accountId, "account removal") ?: return true
            return try {
                suspendCancellableCoroutine { continuation ->
                    publicClientApplication.removeAccount(
                        account,
                        object : IMultipleAccountPublicClientApplication.RemoveAccountCallback {
                            override fun onRemoved() {
                                cachedAccessTokens.remove(accountId)
                                continuation.resume(true)
                            }

                            override fun onError(exception: MsalException) {
                                Timber.e(exception, "Account removal failed")
                                continuation.resume(false)
                            }
                        },
                    )
                }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Account removal failed")
                false
            }
        }

        /** Returns an access token for the given account, preferring silent refresh. */
        suspend fun getAccessToken(accountId: String): String? {
            return acquireTokenSilently(accountId)?.accessToken ?: cachedAccessTokens[accountId]
        }

        private suspend fun getMsalAppOrNull(operation: String): IMultipleAccountPublicClientApplication? {
            return try {
                withContext(Dispatchers.IO) { msalApp.get() }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "MSAL initialization failed during $operation")
                null
            }
        }

        private suspend fun acquireTokenInteractively(
            publicClientApplication: IMultipleAccountPublicClientApplication,
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
                                    result.account?.let { authenticatedAccount ->
                                        cachedAccessTokens[toAccountId(authenticatedAccount)] = result.accessToken
                                    }
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

        private suspend fun getMsalAppOrThrow(operation: String): IMultipleAccountPublicClientApplication {
            return try {
                withContext(Dispatchers.IO) { msalApp.get() }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "MSAL initialization failed during $operation")
                val causeMessage = throwable.message?.takeIf { it.isNotBlank() }
                throw AuthUnavailableException(
                    message =
                        buildString {
                            append("Authentication is unavailable on this build. ")
                            if (causeMessage != null) {
                                append(causeMessage)
                            } else {
                                append("Verify the Microsoft Entra app uses the redirect URI ")
                                append(BuildConfig.MSAL_REDIRECT_URI)
                                append(" and that the same URI is registered in the app manifest.")
                            }
                        },
                    cause = throwable,
                )
            }
        }

        private suspend fun getAccountOrNull(
            publicClientApplication: IMultipleAccountPublicClientApplication,
            accountId: String,
            operation: String,
        ): IAccount? {
            return getAccountsOrNull(publicClientApplication, operation)
                ?.firstOrNull { account -> toAccountId(account) == accountId }
        }

        private suspend fun getAccountsOrNull(
            publicClientApplication: IMultipleAccountPublicClientApplication,
            operation: String,
        ): List<IAccount>? {
            return try {
                suspendCancellableCoroutine { continuation ->
                    publicClientApplication.getAccounts(
                        object : IPublicClientApplication.LoadAccountsCallback {
                            override fun onTaskCompleted(result: List<IAccount>?) {
                                continuation.resume(result.orEmpty())
                            }

                            override fun onError(exception: MsalException) {
                                Timber.e(exception, "MSAL account lookup failed during $operation")
                                continuation.resume(null)
                            }
                        },
                    )
                }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "MSAL account lookup failed during $operation")
                null
            }
        }

        private fun toAuthAccount(account: IAccount): AuthAccount {
            return AuthAccount(
                id = toAccountId(account),
                displayName = account.displayNameOrNull() ?: account.username?.takeIf { it.isNotBlank() },
                username = account.username?.takeIf { it.isNotBlank() },
            )
        }

        private fun toAccountId(account: IAccount): String {
            return account.id
                ?.takeIf { it.isNotBlank() }
                ?: account.username?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Microsoft account is missing a stable identifier")
        }

        private fun IAccount.displayNameOrNull(): String? {
            val claimsMap = claims
            val candidates =
                listOf(
                    claimsMap?.get("name") as? String,
                    claimsMap?.get("email") as? String,
                    claimsMap?.get("preferred_username") as? String,
                    username,
                )

            return candidates
                .firstOrNull { candidate ->
                    !candidate.isNullOrBlank() && candidate != MISSING_TOKEN_RESPONSE_VALUE
                }
        }
    }

class AuthCancelledException(message: String) : Exception(message)

class AuthUnavailableException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
