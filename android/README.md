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

## Azure app registration

1. Open the Azure portal and create or select an app registration.
2. Add a public client/native redirect URI of `msauth://com.g365calendar/auth`.
3. Grant the app the `Microsoft Graph → Calendars.Read` permission.
4. Copy the application (client) ID.
5. Update the MSAL config at [app/src/main/res/raw/msal_config.json](app/src/main/res/raw/msal_config.json) with the client ID.

## Build and test

From this folder, run:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew ktlintCheck
```

## Project structure

- [app/src/main/java/com/g365calendar/auth](app/src/main/java/com/g365calendar/auth) — authentication state and MSAL integration
- [app/src/main/java/com/g365calendar/data](app/src/main/java/com/g365calendar/data) — Graph API client, models, preferences, and repositories
- [app/src/main/java/com/g365calendar/di](app/src/main/java/com/g365calendar/di) — Hilt dependency injection modules
- [app/src/main/java/com/g365calendar/sync](app/src/main/java/com/g365calendar/sync) — Garmin sync orchestration
- [app/src/main/java/com/g365calendar/ui](app/src/main/java/com/g365calendar/ui) — Compose screens and view models
- [app/src/test](app/src/test) — unit tests (JUnit 5 + MockK)
