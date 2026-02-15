# G365Calendar

Calendar application to expose the M365 calendar directly on Garmin Wearables.

## Architecture

```
┌─────────────────┐     ┌─────────────────────┐     ┌───────────────────┐
│  M365 Graph API │◄───►│ Android Companion    │◄───►│ Garmin Watch App  │
│  (Calendars)    │     │ (Kotlin/Compose)     │     │ (Monkey C / CIQ)  │
│                 │     │                      │     │                   │
│ • Calendar list │     │ • MSAL auth (PKCE)   │     │ • Event list view │
│ • Event data    │     │ • Graph API client   │     │ • Event detail    │
│                 │     │ • Calendar selection  │     │ • Local storage   │
│                 │     │ • 60-min WorkManager  │     │ • Color-coded     │
│                 │     │ • Garmin Connect SDK  │     │   calendar labels │
└─────────────────┘     └─────────────────────┘     └───────────────────┘
                              Bluetooth / Garmin Connect Mobile SDK
```

- **Android Companion App** (`android/`): Kotlin + Jetpack Compose. Authenticates with M365 via MSAL (OAuth 2.0 + PKCE), fetches calendar events from Microsoft Graph API (24h past → 7 days future), and pushes them to the watch via the Garmin Connect Mobile SDK. Background sync every 60 minutes via WorkManager.
- **Garmin Watch App** (`garmin/`): Monkey C Connect IQ app targeting Venu 2, 3, and 4 series. Displays calendar events with title, time, location, and calendar color. Supports scrollable event list and detail view.

## Features

- **Multi-calendar support**: User selects which M365 calendars to sync
- **Color-coded events**: Each calendar's events are visually distinguished by color
- **Automatic sync**: Background sync every 60 minutes
- **Manual sync**: One-tap sync from the companion app
- **Offline viewing**: Events cached on watch for offline access
- **Compact display**: Optimized for Garmin AMOLED touchscreens

## Target Devices

| Device       | Connect IQ ID | Screen |
|-------------|---------------|--------|
| Venu 2      | `venu2`       | 416×416 |
| Venu 2S     | `venu2s`      | 360×360 |
| Venu 2 Plus | `venu2plus`   | 416×416 |
| Venu 3      | `venu3`       | 390×390 |
| Venu 3S     | `venu3s`      | 360×360 |
| Venu 4      | `venu4`       | TBD |
| Venu 4S     | `venu4s`      | TBD |

## Prerequisites

- **JDK 17** or later
- **Android SDK** (API 26+, compile SDK 35)
- **Garmin Connect IQ SDK** (4.0.0+)
- **Azure AD App Registration** with `Calendars.Read` permission
- **Garmin Developer Account** (for Connect IQ Store distribution)

## Getting Started

### 1. Azure AD App Registration

1. Go to [Azure AD App Registrations](https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationsListBlade)
2. Click **New registration**
3. Set redirect URI: `msauth://com.g365calendar/auth` (type: Public client/native)
4. Under **API permissions**, add `Microsoft Graph → Calendars.Read`
5. Copy the **Application (client) ID**
6. Update `android/app/src/main/res/raw/msal_config.json`:
   ```json
   {
     "client_id": "YOUR_CLIENT_ID_HERE"
   }
   ```

### 2. Android Companion App

```bash
cd android

# Bootstrap Gradle wrapper (first time only — requires Gradle installed)
gradle wrapper --gradle-version=8.11.1

# Build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run Android lint
./gradlew lint

# Run code style check (ktlint)
./gradlew ktlintCheck
```

### 3. Garmin Watch App

```bash
# Build using Connect IQ SDK (requires monkeyc on PATH)
monkeyc -f garmin/monkey.jungle -d venu3 -o garmin/bin/G365Calendar.prg

# Run tests
monkeyc -f garmin/monkey.jungle -d venu3 -t -o garmin/bin/G365Calendar-test.prg
connectiq && monkeydo garmin/bin/G365Calendar-test.prg venu3
```

## Project Structure

```
G365Calendar/
├── android/                         # Android companion app
│   ├── app/src/main/java/com/g365calendar/
│   │   ├── auth/                    # MSAL authentication (AuthManager, AuthState)
│   │   ├── data/
│   │   │   ├── api/                 # Graph API client (Retrofit) + AuthInterceptor
│   │   │   ├── model/               # Data models (GraphModels, DisplayEvent)
│   │   │   ├── preferences/         # DataStore calendar preferences
│   │   │   └── repository/          # CalendarRepository
│   │   ├── di/                      # Hilt dependency injection modules
│   │   ├── sync/                    # Garmin sync (EventSerializer, GarminConnector, SyncManager)
│   │   └── ui/                      # Jetpack Compose screens & ViewModels
│   ├── app/src/test/                # Unit tests (JUnit 5 + MockK)
│   ├── gradle/libs.versions.toml    # Version catalog
│   └── build.gradle.kts             # Root build config
├── garmin/                          # Garmin Connect IQ watch app
│   ├── source/
│   │   ├── G365CalendarApp.mc       # App entry point
│   │   ├── G365CalendarView.mc      # Event list view (scrollable)
│   │   ├── G365CalendarDelegate.mc  # Input handling (scroll, select)
│   │   ├── EventDetailView.mc       # Event detail view
│   │   ├── DataReceiver.mc          # Companion app communication
│   │   └── EventStore.mc            # Local event storage
│   ├── test/                        # Connect IQ unit tests
│   ├── resources/                   # Strings, drawables, layouts
│   └── manifest.xml                 # Device targeting & permissions
├── docs/
│   └── integration-test-plan.md     # Manual integration test procedures
├── .github/
│   ├── workflows/
│   │   ├── android-ci.yml           # Android CI (build/lint/test/ktlint)
│   │   └── garmin-ci.yml            # Garmin CI (validation)
│   └── copilot-instructions.md
├── .connect-iq/                     # Connect IQ SDK manager
├── .gitignore
├── LICENSE                          # MIT
└── README.md
```

## CI/CD

GitHub Actions workflows run on push/PR to `main`:

- **`android-ci.yml`**: Builds debug APK, runs unit tests, Android lint, and ktlint
- **`garmin-ci.yml`**: Validates manifest.xml, source files, and resource files

## Known Limitations

- **Android Doze mode**: The 60-minute WorkManager sync may be delayed when the device is in Doze mode. The actual sync interval may vary.
- **Watch storage**: Garmin apps have ~128KB storage limit. Events are capped at 50 per sync to stay within bounds.
- **Garmin Connect Mobile SDK**: Must be downloaded separately from the [Garmin Developer Portal](https://developer.garmin.com/connect-iq/sdk/) — it is not available on Maven Central.
- **iOS**: Deferred — only Android companion app is implemented.
- **Venu 4 series**: Device IDs (`venu4`, `venu4s`) are provisional — verify against the Connect IQ SDK when hardware is available.

## Testing

- **Unit tests**: `android/app/src/test/` — JUnit 5 + MockK (13+ test cases covering auth, Graph API, sync)
- **Garmin tests**: `garmin/test/` — Connect IQ test framework (6 test cases for EventStore)
- **Integration tests**: See `docs/integration-test-plan.md` for manual E2E test procedures

## License

MIT — see [LICENSE](LICENSE) for details.

