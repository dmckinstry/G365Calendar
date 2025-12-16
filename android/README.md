# Android Companion App

Android companion app for G365Calendar that provides enhanced OAuth handling and bidirectional communication with the Garmin watch app.

## Features

- Connect to Garmin devices via Connect IQ SDK
- Microsoft Authentication Library (MSAL) integration
- Fetch calendar events from Microsoft Graph API
- Send events to watch app
- Token management and refresh

## Building

```bash
# Build debug version
./gradlew assembleDebug

# Build release version
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## Configuration

### 1. Update Client ID

Edit `app/src/main/res/raw/auth_config.json`:

```json
{
  "client_id": "YOUR_CLIENT_ID",
  "redirect_uri": "msauth://com.g365calendar/YOUR_SIGNATURE_HASH"
}
```

### 2. Calculate Signature Hash

Generate your app's signature hash for the redirect URI:

```bash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64
```

The password for debug keystore is typically `android`.

### 3. Update AndroidManifest.xml

Replace `YOUR_SIGNATURE_HASH` in `AndroidManifest.xml` with the calculated hash.

### 4. Update App ID

In `MainActivity.java`, update the Connect IQ app ID:

```java
private static final String APP_ID = "your-actual-app-id";
```

## Testing

1. Install the app on your Android device
2. Pair your Garmin device with Garmin Connect app
3. Launch G365Calendar companion app
4. Tap "Authenticate with Microsoft"
5. Complete OAuth flow
6. Tap "Sync Calendar to Watch"

## Dependencies

- Connect IQ Android SDK: 2.0.3
- MSAL: 5.1.0
- Microsoft Graph SDK: 6.5.0
- AndroidX libraries

## Requirements

- Android SDK 26 (Android 8.0) or higher
- Java 17
- Gradle 8.2

## Known Issues

- First sync may take longer due to initial authentication
- Requires Garmin Connect app to be installed and device paired

## License

MIT License
