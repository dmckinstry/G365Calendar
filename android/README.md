# Android companion app

The Android app is the companion application for G365Calendar. It authenticates with Microsoft 365, reads calendar data from Microsoft Graph, and sends the data to the Garmin watch app over the Garmin Connect Mobile SDK.

## Features

- Microsoft authentication via MSAL (OAuth 2.0 + PKCE)
- Calendar reading from Microsoft Graph
- Calendar selection for sync
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
2. Add a public client/native redirect URI of `msauth://com.g365calendar/auth`.
3. Grant the app the `Microsoft Graph → Calendars.Read` permission.
4. Copy the application (client) ID.
5. Update the MSAL config at [app/src/main/res/raw/msal_config.json](app/src/main/res/raw/msal_config.json) with the client ID.

## Build and test

The Android companion now uses Garmin's Maven Central artifact
`com.garmin.connectiq:ciq-companion-app-sdk` and initializes the SDK in
wireless mode. When the app UI is opened, Garmin's SDK can prompt the user to
install or upgrade Garmin Connect Mobile if required. Background sync paths
reuse the same connector with non-UI initialization.

From the android folder, use `make` as follows:

| Target | Underlying command | Description |
| --- | --- | --- |
| `make build-android` or `make build` | `./gradlew assembleDebug` | Builds the Android debug APK. |
| `make test-android` or `make test` | `./gradlew test` | Runs the Android unit tests. |
| `make dev-android` or `make dev` | `emulator`... and `./gradlew installDebug` | Attempts to launch the `Pixel_8_API_36` emulator (if needed) and then installs the debug build. |

## Project structure

- [app/src/main/java/com/g365calendar/auth](app/src/main/java/com/g365calendar/auth) — authentication state and MSAL integration
- [app/src/main/java/com/g365calendar/data](app/src/main/java/com/g365calendar/data) — Graph API client, models, preferences, and repositories
- [app/src/main/java/com/g365calendar/di](app/src/main/java/com/g365calendar/di) — Hilt dependency injection modules
- [app/src/main/java/com/g365calendar/sync](app/src/main/java/com/g365calendar/sync) — Garmin sync orchestration
- [app/src/main/java/com/g365calendar/ui](app/src/main/java/com/g365calendar/ui) — Compose screens and view models
- [app/src/test](app/src/test) — unit tests (JUnit 5 + MockK)
