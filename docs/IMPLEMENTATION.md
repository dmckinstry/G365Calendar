# G365Calendar Implementation Summary

This document provides a comprehensive overview of the G365Calendar project implementation.

## Project Overview

**G365Calendar** is a Garmin Connect IQ watch app that displays Microsoft 365 calendar events directly on Garmin wearable devices. The project includes:

- **Watch App**: Monkey C application for Garmin Venu 2/4 series
- **Android Companion App**: Enhanced OAuth and data synchronization
- **iOS Companion App**: Documentation and architecture guide
- **Dev Container**: Complete development environment setup

## Architecture

### Watch App (Monkey C)

#### Core Components

1. **App.mc** - Main application entry point
   - Manages app lifecycle (onStart, onStop)
   - Handles incoming messages from companion apps
   - Provides access to AuthManager and ApiClient

2. **CalendarView.mc** - User interface and interaction
   - Displays calendar events with scrolling
   - Shows event subject, time, location
   - Handles swipe gestures and button presses
   - Loading and empty state screens

3. **AuthManager.mc** - OAuth authentication
   - Microsoft common tenant OAuth 2.0 flow
   - Token storage and refresh logic
   - Handles authorization and token endpoints
   - Expiration tracking

4. **ApiClient.mc** - Microsoft Graph API integration
   - Fetches calendar events from `/me/calendar/events`
   - Configurable time window (DAYS_PAST, DAYS_FUTURE)
   - Event caching with 5-minute TTL
   - Automatic token refresh on 401

#### Data Flow

```
User Action → CalendarView → ApiClient → Graph API
                    ↓              ↓
              UI Update ← Event Processing ← Response
                                    ↓
                            Storage Cache
```

#### Storage

Uses `Application.Storage` for persistent data:
- `access_token`: OAuth access token
- `refresh_token`: OAuth refresh token
- `token_expiry`: Token expiration timestamp
- `cached_events`: Array of calendar events
- `cache_timestamp`: Cache creation time

### Android Companion App

#### Architecture

```
MainActivity
    ├── ConnectIQ SDK
    │   ├── Device Discovery
    │   ├── Message Sending
    │   └── Event Listening
    ├── MSAL
    │   ├── Authentication
    │   └── Token Management
    └── Graph SDK
        └── Calendar Fetching
```

#### Key Features

- Bidirectional messaging with watch
- Enhanced OAuth via MSAL library
- Prefetch calendar data
- Token management and refresh
- Device pairing and connection

### iOS Companion App

Documented architecture for Mac-based development:

- Connect IQ iOS SDK integration
- MSAL for iOS authentication
- SwiftUI/UIKit interface options
- URL scheme handling
- Background Bluetooth communication

## Development Environment

### Devcontainer Configuration

**Base Image**: `mcr.microsoft.com/devcontainers/java:21`

**Installed Tools**:
- Connect IQ SDK 7.3.1 (Linux)
- Android SDK with command-line tools
- Platform tools and build tools
- Java 21 (included in base image)

**Environment Variables**:
- `MB_HOME`: `/opt/connectiq-sdk`
- `ANDROID_HOME`: `/opt/android-sdk`
- Extended PATH with SDK binaries

**VS Code Extensions**:
- Garmin Monkey C
- Java Development Pack
- Gradle support

### Post-Create Script

Automated setup process:
1. Install system dependencies (wget, unzip)
2. Download and install Connect IQ SDK
3. Download and install Android SDK tools
4. Accept Android licenses
5. Install required Android packages
6. Create `.keys` directory structure

## OAuth Flow

### Initial Authentication

```
Watch App → OAuth Request → Microsoft Login
                                    ↓
User Authenticates ← Browser/Companion App
                                    ↓
Authorization Code → Token Exchange
                                    ↓
Access Token + Refresh Token → Storage
```

### Token Refresh

```
API Request → 401 Unauthorized
                    ↓
Refresh Token → Token Endpoint
                    ↓
New Access Token → Storage → Retry Request
```

## API Integration

### Microsoft Graph API

**Endpoint**: `https://graph.microsoft.com/v1.0/me/calendar/events`

**Query Parameters**:
- `$select`: subject, start, end, location
- `$orderby`: start/dateTime
- `$filter`: Date range filtering
- `$top`: Limit results (50)

**Authentication**: Bearer token in Authorization header

**Response Processing**:
1. Extract event array from response
2. Parse dateTime objects
3. Format for display
4. Cache in storage

## Configuration Points

### Watch App

| File | Setting | Purpose |
|------|---------|---------|
| `source/AuthManager.mc` | CLIENT_ID | Azure app ID |
| `source/AuthManager.mc` | REDIRECT_URI | OAuth redirect |
| `source/AuthManager.mc` | SCOPES | API permissions |
| `source/ApiClient.mc` | DAYS_PAST | Past event window |
| `source/ApiClient.mc` | DAYS_FUTURE | Future event window |
| `manifest.xml` | id | Unique app identifier |

### Android App

| File | Setting | Purpose |
|------|---------|---------|
| `auth_config.json` | client_id | Azure app ID |
| `auth_config.json` | redirect_uri | MSAL redirect |
| `AndroidManifest.xml` | signature hash | App signing hash |
| `MainActivity.java` | APP_ID | Watch app ID |

### iOS App

| File | Setting | Purpose |
|------|---------|---------|
| `auth_config.json` | client_id | Azure app ID |
| `Info.plist` | URL schemes | MSAL redirect |
| `ConnectIQManager` | App UUID | Watch app ID |

## Security Considerations

### Credentials

**Never commit**:
- OAuth client secrets
- Developer keys (.pem, .der)
- Access tokens
- API keys

**Protection**:
- `.gitignore` excludes sensitive files
- `.keys/` directory for local keys
- Environment variables for secrets
- Documentation for manual setup

### Token Management

- Tokens stored securely in device storage
- Automatic expiration checking
- Refresh token rotation
- HTTPS for all API calls

## Build Process

### Watch App

```bash
monkeyc \
  -d venu2 \                    # Target device
  -f monkey.jungle \            # Project file
  -o bin/G365Calendar.prg \     # Output file
  -y .keys/developer_key.der    # Signing key
```

### Android App

```bash
cd android
./gradlew assembleDebug        # Debug build
./gradlew assembleRelease      # Release build
./gradlew installDebug         # Install to device
```

## Testing Strategy

### Watch App

1. **Simulator Testing**: Basic UI and logic
2. **Device Testing**: OAuth, networking, real data
3. **Manual Testing**: User interactions, edge cases

### Companion Apps

1. **Unit Tests**: Business logic (future enhancement)
2. **Integration Tests**: API communication
3. **Device Tests**: Bluetooth, messaging, pairing

## Future Enhancements

### Planned Features

- [ ] Additional device support (Fenix, Forerunner series)
- [ ] Event details view with notes
- [ ] Calendar event notifications
- [ ] Multiple calendar support
- [ ] Meeting response actions (Accept/Decline)
- [ ] All-day event handling
- [ ] Timezone support

### Technical Improvements

- [ ] Unit test coverage
- [ ] CI/CD pipeline
- [ ] Automated releases
- [ ] Localization (i18n)
- [ ] Performance optimization
- [ ] Battery usage profiling

## File Structure Summary

```
G365Calendar/
├── .devcontainer/              # Dev container config
│   ├── devcontainer.json       # Container specification
│   └── post-create.sh          # Setup automation
├── source/                     # Watch app source
│   ├── App.mc                  # Main application
│   ├── CalendarView.mc         # UI view
│   ├── AuthManager.mc          # OAuth logic
│   └── ApiClient.mc            # API client
├── resources/                  # App resources
│   ├── drawables/              # Images/icons
│   ├── layouts/                # UI layouts
│   └── strings/                # Localized text
├── android/                    # Android companion
│   ├── app/src/main/           # Java source
│   └── build.gradle            # Build config
├── docs/                       # Documentation
│   ├── configuration.md        # Setup guide
│   └── ios-setup.md           # iOS guide
├── manifest.xml                # Connect IQ manifest
├── monkey.jungle               # Build configuration
├── README.md                   # Main documentation
├── QUICKSTART.md              # Quick start guide
└── CONTRIBUTING.md            # Contribution guide
```

## Dependencies

### Watch App (Monkey C)

- Connect IQ SDK 3.2.0+
- Supported on API Level 3.2.0+

### Android App

- Connect IQ SDK: 2.0.3
- MSAL: 5.1.0
- Microsoft Graph: 6.5.0
- AndroidX AppCompat: 1.6.1
- Material Components: 1.11.0
- Min SDK: 26 (Android 8.0)
- Target SDK: 34

### iOS App

- Connect IQ iOS SDK
- MSAL for iOS: ~1.3.0
- iOS 14.0+
- Xcode 14.0+

## Resources

### Official Documentation

- [Garmin Connect IQ](https://developer.garmin.com/connect-iq/)
- [Microsoft Graph API](https://learn.microsoft.com/graph/)
- [MSAL Documentation](https://learn.microsoft.com/azure/active-directory/develop/msal-overview)

### Community

- [Connect IQ Forums](https://forums.garmin.com/developer/connect-iq/)
- [GitHub Repository](https://github.com/dmckinstry/G365Calendar)

## License

MIT License - See [LICENSE](LICENSE) file

## Contributors

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

---

**Last Updated**: Initial implementation
**Version**: 0.1.0
