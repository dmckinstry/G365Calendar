# G365Calendar

Garmin Connect IQ device app that displays Microsoft 365 calendar events directly on compatible Garmin wearables. Supports both Entra ID (Azure AD) and personal Outlook.com accounts via OAuth authentication.

## Features

- 📅 View Microsoft 365 calendar events on your Garmin watch
- 🔐 OAuth 2.0 authentication for secure access
- 📱 Companion apps for Android and iOS
- ⚡ Configurable event window (past/future days)
- 💾 Local caching for offline viewing
- 🔄 Automatic token refresh

## Supported Devices

- Garmin Venu 2
- Garmin Venu 2 Plus
- Garmin Venu 2S
- Garmin Venu Sq 2
- Garmin Venu Sq 2 Music Edition

## Project Structure

```
G365Calendar/
├── .devcontainer/          # Development container configuration
├── source/                 # Monkey C source files
│   ├── App.mc             # Main application
│   ├── CalendarView.mc    # UI view and delegate
│   ├── AuthManager.mc     # OAuth authentication
│   └── ApiClient.mc       # Microsoft Graph API client
├── resources/             # App resources
│   ├── drawables/        # Images and icons
│   ├── layouts/          # UI layouts
│   └── strings/          # Localized strings
├── android/              # Android companion app
├── docs/                 # Documentation
│   └── ios-setup.md     # iOS development guide
└── manifest.xml          # Connect IQ manifest

```

## Getting Started

### Prerequisites

- [Connect IQ SDK](https://developer.garmin.com/connect-iq/sdk/)
- [Visual Studio Code](https://code.visualstudio.com/) with [Monkey C extension](https://marketplace.visualstudio.com/items?itemName=garmin.monkey-c)
- [Docker](https://www.docker.com/) (for devcontainer)
- Microsoft 365 account or Outlook.com account
- Azure AD app registration (see Configuration section)

### Development with Devcontainer

This project includes a devcontainer configuration for a consistent development environment:

1. **Open in VS Code with Dev Containers extension**:
   ```bash
   code .
   # When prompted, click "Reopen in Container"
   ```

2. **The container will automatically**:
   - Install Java 21
   - Download Connect IQ SDK
   - Install Android SDK command-line tools
   - Configure environment variables

3. **Generate developer key** (first time only):
   ```bash
   openssl genrsa -out .keys/developer_key.pem 4096
   openssl pkcs8 -topk8 -inform PEM -outform DER \
     -in .keys/developer_key.pem -out .keys/developer_key.der -nocrypt
   ```

### Building the Watch App

```bash
# Using Connect IQ SDK command-line tools
monkeyc -d venu2 -f monkey.jungle -o bin/G365Calendar.prg -y .keys/developer_key.der
```

Or use the VS Code Monkey C extension to build and run in the simulator.

## Configuration

### Microsoft 365 App Registration

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory > App registrations**
3. Click **New registration**
4. Configure:
   - **Name**: G365Calendar
   - **Supported account types**: Personal and organizational accounts
   - **Redirect URI**: `https://localhost/oauth/redirect` (adjust as needed)
5. Under **API permissions**, add:
   - Microsoft Graph > Delegated > `Calendars.Read`
   - Microsoft Graph > Delegated > `offline_access`
6. Note the **Application (client) ID**

### Update Configuration

Update the following files with your app registration details:

**source/AuthManager.mc**:
```monkey-c
private const CLIENT_ID = "your-client-id-here";
private const REDIRECT_URI = "your-redirect-uri-here";
```

**android/app/src/main/res/raw/auth_config.json**:
```json
{
  "client_id": "your-client-id-here",
  "redirect_uri": "msauth://com.g365calendar/YOUR_SIGNATURE_HASH"
}
```

## Android Companion App

The Android companion app provides enhanced OAuth handling and data prefetch capabilities.

### Building Android App

```bash
cd android
./gradlew build
```

### Installing

```bash
./gradlew installDebug
```

See [android/README.md](android/README.md) for more details.

## iOS Companion App

The iOS companion app is developed separately on macOS. See the detailed setup guide:

📖 [iOS Development Setup Guide](docs/ios-setup.md)

## Usage

### First Time Setup

1. Install the app on your Garmin device
2. Open the app on your watch
3. Press SELECT to initiate OAuth flow
4. Complete authentication on your phone
5. Calendar events will sync automatically

### Navigation

- **Swipe Up**: Next event
- **Swipe Down**: Previous event
- **SELECT**: Refresh events

### Configuring Event Window

In `source/ApiClient.mc`, adjust:

```monkey-c
private const DAYS_PAST = 1;    // Events from past day
private const DAYS_FUTURE = 7;  // Events for next week
```

## Development Considerations

### SDK Installation Strategy

The devcontainer uses a post-create script to download SDKs. Alternative approaches:

1. **Download during setup** (current): Smaller image, slower startup
2. **Pre-baked image**: Larger image, faster startup
3. **Host mount**: Share SDKs from host machine

### Simulator Usage

The Connect IQ simulator can be used for testing, but OAuth flows require actual device or companion app testing.

### Developer Key Management

Developer keys are stored in `.keys/` directory (gitignored). Generate keys as shown in the Getting Started section.

## Contributing

Contributions are welcome! Please ensure:

- Code follows Monkey C best practices
- OAuth credentials are never committed
- Changes are tested on actual devices

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Garmin Connect IQ](https://developer.garmin.com/connect-iq/)
- [Microsoft Graph API](https://developer.microsoft.com/graph)
- [Microsoft Authentication Library (MSAL)](https://learn.microsoft.com/azure/active-directory/develop/msal-overview)

## Support

For issues and questions:

- [GitHub Issues](https://github.com/dmckinstry/G365Calendar/issues)
- [Connect IQ Forums](https://forums.garmin.com/developer/connect-iq/)

## Roadmap

- [ ] Add support for more Garmin devices
- [ ] Implement event details view
- [ ] Add calendar event notifications
- [ ] Support for multiple calendars
- [ ] Offline calendar management 
