# Android companion app

The Android app is the companion application for G365Calendar. It authenticates with Microsoft 365, reads calendar data from Microsoft Graph, and sends the data to the Garmin watch app over the Garmin Connect Mobile SDK.

## Features

- Microsoft authentication via MSAL (OAuth 2.0 + PKCE)
- Multiple-account support on the home screen, with add-account and per-account sign-out actions
- Calendar reading from Microsoft Graph for accounts with an Exchange Online mailbox available to Graph calendar APIs
- Account-scoped calendar selection for sync with explicit Save and Cancel actions
- Signed-in home screen summary showing each account email and its currently selected calendars
- Debug builds expose a Debug screen that shows the same canonical event payload sent to Garmin clients for the currently selected calendars
- Background sync every 60 minutes using WorkManager
- Jetpack Compose UI for the companion experience

## Prerequisites

- JDK 17 or later
- Android SDK with API 26+ and compile SDK 35
- A Microsoft Entra app registration with the `Calendars.Read` permission
  - References: [Register an application with the Microsoft identity platform](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app), [How to add a redirect URI for a public client/native app](https://learn.microsoft.com/en-us/entra/identity-platform/reply-url), [Microsoft Graph permissions reference](https://learn.microsoft.com/en-us/graph/permissions-reference), and [MSAL for Android configuration](https://learn.microsoft.com/en-us/entra/msal/android/configure-your-app).
- Garmin Connect Mobile installed on the phone
- The G365Calendar watch app installed on the paired Garmin device

## Azure app registration

1. Open the Azure portal and create or select an app registration.
2. Add a public client/native redirect URI for this Android package in the MSAL Android format: `msauth://com.g365calendar/<url-encoded signing-certificate-signature-hash>`.
3. Set **Supported account types** to support both work or school accounts and personal Microsoft accounts if you want the app to sign in both.
4. Grant the app the `Microsoft Graph → Calendars.Read` permission.
5. Copy the application (client) ID.
6. Provide the client ID to the Android build using one of these inputs:
   - Add `azureAppId=<your-client-id>` to `android/local.properties`
   - Add `azureAppId=<your-client-id>` to `android/gradle.properties`
   - Export `AZURE_APP_ID=<your-client-id>` before running Gradle for CI or other non-local builds

The app now generates its packaged MSAL config from [app/msal_config.json.template](app/msal_config.json.template) at build time, so there is no checked-in client ID to edit.

## Optional build configuration

The Android build resolves calendar-read configuration in this order:

1. Gradle property
2. `local.properties`
3. Environment variable

| Purpose | Gradle or `local.properties` key | Environment variable | Default |
| --- | --- | --- | --- |
| Azure app registration client ID | `azureAppId` | `AZURE_APP_ID` | None; required for sign-in |
| MSAL audience | `azureAuthorityAudience` | `AZURE_AUTHORITY_AUDIENCE` | `AzureADandPersonalMicrosoftAccount` |
| Azure tenant ID for single-tenant sign-in only | `azureTenantId` | `AZURE_TENANT_ID` | Unused unless audience is `AzureADMyOrg` |
| Microsoft Graph scopes | `graphScopes` | `GRAPH_SCOPES` | `Calendars.Read` |
| Microsoft Graph base URL | `graphBaseUrl` | `GRAPH_BASE_URL` | `https://graph.microsoft.com/v1.0/` |

Supported `azureAuthorityAudience` values:

- `AzureADandPersonalMicrosoftAccount` for both Exchange Online work/school accounts and personal Microsoft accounts
- `AzureADMultipleOrgs` for organizational accounts only
- `AzureADMyOrg` for a single tenant; requires `azureTenantId`
- `PersonalMicrosoftAccount` for personal Microsoft accounts only

The default audience is `AzureADandPersonalMicrosoftAccount`, so the app can sign in both account types. Set `azureTenantId` only when you intentionally choose `AzureADMyOrg`.

Important Microsoft Graph calendar limitation:

- Personal Microsoft accounts can authenticate successfully, but `GET /me/calendars` still requires a mailbox that Microsoft Graph can access for the signed-in identity.
- If Graph returns `MailboxNotEnabledForRESTAPI`, the account does not currently expose a mailbox that this app can read. In practice, use a Microsoft 365 work or school account with an active Exchange Online mailbox when you need calendar access.

Example local development config in `android/local.properties`:

```properties
sdk.dir=/Users/you/android-sdk
azureAppId=YOUR_AZURE_APP_ID
azureAuthorityAudience=AzureADandPersonalMicrosoftAccount
graphScopes=Calendars.Read
graphBaseUrl=https\://graph.microsoft.com/v1.0/
```

For debug builds, the app now derives the MSAL redirect URI from the default Android debug signing certificate if you do not supply one explicitly. If you need a custom signing key or release signing, provide one of these optional inputs:

- `azureRedirectUri` or `AZURE_REDIRECT_URI` for the full registered redirect URI
- `azureRedirectSignatureHash` or `AZURE_REDIRECT_SIGNATURE_HASH` for just the base64 signing-certificate SHA-1 hash; the build will compose the final `msauth://com.g365calendar/...` URI

On this machine, you can inspect the debug signing hash with:

```bash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64
```

For CI and other non-local builds, prefer `AZURE_APP_ID`, `AZURE_AUTHORITY_AUDIENCE`, `AZURE_TENANT_ID`, `GRAPH_SCOPES`, and `GRAPH_BASE_URL` instead of checked-in or shared Gradle properties.

## Companion app flow

After one or more accounts are added, the Android home screen shows:

- one card per signed-in account
- the account email address for that card
- the currently selected calendars for that account, including each calendar color indicator
- actions to open the `Calendars` picker, sign out the account, or add another account

In debug builds only, the home screen also shows a `Debug` button at the bottom. It opens a screen that renders the same canonical payload that the companion app sends to Garmin clients for the currently selected calendars across the signed-in accounts.

The event query window starts at the beginning of yesterday and continues 14 days into the future. After the Graph calls complete, the app builds a shared canonical payload for downstream clients. That payload merges events from all selected calendars into a single list, sorts them by start time, omits event IDs, normalizes long descriptions, and trims to the most recent prior event plus the following 16 events when the full JSON would exceed 12 KB. The Debug screen renders that payload directly, so the displayed JSON is identical to the JSON content sent onward through companion-client integrations.

If no calendars are selected for an account, the home card shows how many calendars are available for that account. The Calendars screen loads the available calendars for the selected account, preselects that account's saved calendars, and only persists changes when the user taps Save. Cancel discards any in-progress checkbox changes.

## Build and test

The Android companion now uses Garmin's Maven Central artifact
`com.garmin.connectiq:ciq-companion-app-sdk` and initializes the SDK in
wireless mode on demand during sync work instead of at process start. This
keeps launch resilient when Garmin Connect Mobile is unavailable and produces
clearer startup diagnostics in logcat. Background sync paths reuse the same
connector with non-UI initialization.

From the android folder, use `make` as follows:

| Target | Underlying command | Description |
| --- | --- | --- |
| `make build-android` or `make build` | `./gradlew assembleDebug` | Builds the Android debug APK. |
| `make test-android` or `make test` | `./gradlew test` | Runs the Android unit tests. |
| `make test-integration-android` or `make test-integration` | `./gradlew integrationTest` | Runs the live Microsoft Graph integration tests. Requires `GRAPH_ACCESS_TOKEN` with `Calendars.Read`. |
| `make dev-android` or `make dev` | `emulator`... and `./gradlew installDebug` | Attempts to launch the `Pixel_8_API_36` emulator (if needed) and then installs the debug build. |

The integration tests are tagged separately from the normal unit suite, so they are excluded from `./gradlew test` and `make test` by default.

The current live integration coverage verifies two Microsoft Graph paths:

- Calendar discovery through `GET /me/calendars`
- Event retrieval for the calendar whose display name is exactly `Calendar`

The event retrieval test assumes that the signed-in Microsoft 365 account exposes a calendar named `Calendar`. If that calendar does not exist for the token's user, the test fails by design.

`GRAPH_ACCESS_TOKEN` is required because the integration test injects a mocked auth manager that returns the provided bearer token for the repository call path. It does not launch MSAL, open an interactive sign-in flow, or reuse a cached app session, so there is no other credential source available during the JVM test run.

You can create the token using either of these Microsoft-documented flows:

- Graph Explorer: sign in, consent to `Calendars.Read`, then copy the token from the **Access token** tab. See <https://learn.microsoft.com/en-us/graph/graph-explorer/graph-explorer-overview> and <https://learn.microsoft.com/en-us/graph/graph-explorer/graph-explorer-features>.
- Device code or other delegated user auth flow: follow Microsoft's authentication guidance and request a delegated token that includes `Calendars.Read`. See <https://learn.microsoft.com/en-us/graph/tutorials/java-authentication> and <https://learn.microsoft.com/en-us/graph/sdks/choose-authentication-providers>.

The token is a short-lived bearer token. If the integration test starts failing with `401` or `403`, refresh the token and verify that the delegated scopes include `Calendars.Read`.

If the calendar-specific test fails, first confirm that the token belongs to the intended user and that the account has a calendar named `Calendar`.

Example:

```bash
export GRAPH_ACCESS_TOKEN="<bearer-token-with-Calendars.Read>"
make test-integration
```

## Project structure

- [app/src/main/java/com/g365calendar/auth](app/src/main/java/com/g365calendar/auth) — authentication state and MSAL integration
- [app/src/main/java/com/g365calendar/data](app/src/main/java/com/g365calendar/data) — Graph API client, models, preferences, and repositories
- [app/src/main/java/com/g365calendar/di](app/src/main/java/com/g365calendar/di) — Hilt dependency injection modules
- [app/src/main/java/com/g365calendar/sync](app/src/main/java/com/g365calendar/sync) — Garmin sync orchestration
- [app/src/main/java/com/g365calendar/ui](app/src/main/java/com/g365calendar/ui) — Compose screens and view models
- [app/src/test](app/src/test) — unit tests (JUnit 5 + MockK)
