# Configuration Guide

This guide walks through all configuration steps needed to set up G365Calendar.

## Table of Contents

1. [Azure App Registration](#azure-app-registration)
2. [Watch App Configuration](#watch-app-configuration)
3. [Android App Configuration](#android-app-configuration)
4. [iOS App Configuration](#ios-app-configuration)
5. [Developer Key Setup](#developer-key-setup)

## Azure App Registration

### Step 1: Create App Registration

1. Navigate to [Azure Portal](https://portal.azure.com)
2. Go to **Azure Active Directory** > **App registrations**
3. Click **+ New registration**

### Step 2: Configure Basic Settings

- **Name**: `G365Calendar` (or your preferred name)
- **Supported account types**: Select one of:
  - **Accounts in any organizational directory and personal Microsoft accounts** (for both work/school and personal accounts)
  - **Personal Microsoft accounts only** (for Outlook.com only)
- **Redirect URI**: Leave blank for now, we'll add them next

Click **Register**

### Step 3: Note Your Application ID

After registration, you'll see the app overview page. Copy the **Application (client) ID** - you'll need this for all three apps.

Example: `a1b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6`

### Step 4: Add Redirect URIs

Go to **Authentication** section:

1. Click **+ Add a platform**
2. Select **Mobile and desktop applications**
3. Add these redirect URIs:
   ```
   https://localhost/oauth/redirect
   msauth://com.g365calendar/YOUR_SIGNATURE_HASH
   msauth.com.g365calendar://auth
   ```

4. Enable these settings:
   - ✅ Access tokens
   - ✅ ID tokens

5. Click **Configure**

### Step 5: Configure API Permissions

Go to **API permissions** section:

1. Click **+ Add a permission**
2. Select **Microsoft Graph**
3. Select **Delegated permissions**
4. Add these permissions:
   - `Calendars.Read` - Read user calendars
   - `offline_access` - Maintain access to data
   - `User.Read` - Sign in and read user profile (added by default)

5. Click **Add permissions**

### Step 6: (Optional) Admin Consent

For organizational accounts, an admin may need to grant consent:

1. Click **Grant admin consent for [Your Organization]**
2. Confirm the action

## Watch App Configuration

### Update AuthManager.mc

Edit `source/AuthManager.mc`:

```monkey-c
private const CLIENT_ID = "a1b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6";  // Your Application ID
private const REDIRECT_URI = "https://localhost/oauth/redirect";     // Must match Azure
private const SCOPES = "Calendars.Read offline_access";
```

### Update manifest.xml

The app ID in `manifest.xml` should be unique. If needed, generate a new one:

```xml
<iq:application 
    entry="G365CalendarApp" 
    id="YOUR_UNIQUE_APP_ID_HERE"
    ...>
```

You can generate a UUID or use the format: lowercase hex string (32 characters without dashes).

## Android App Configuration

### Step 1: Calculate Signature Hash

Generate your app's signature hash:

```bash
# For debug builds
keytool -exportcert -alias androiddebugkey \
    -keystore ~/.android/debug.keystore | \
    openssl sha1 -binary | openssl base64

# Password is usually: android
```

This will output something like: `AbCdEfGhIjKlMnOpQrStUvWxYz0=`

### Step 2: Update auth_config.json

Edit `android/app/src/main/res/raw/auth_config.json`:

```json
{
  "client_id": "a1b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6",
  "authorization_user_agent": "DEFAULT",
  "redirect_uri": "msauth://com.g365calendar/AbCdEfGhIjKlMnOpQrStUvWxYz0%3D",
  "account_mode": "SINGLE",
  "broker_redirect_uri_registered": true,
  "authorities": [
    {
      "type": "AAD",
      "audience": {
        "type": "AzureADandPersonalMicrosoftAccount",
        "tenant_id": "common"
      }
    }
  ]
}
```

**Note**: URL encode the equals sign: `=` becomes `%3D`

### Step 3: Update AndroidManifest.xml

Edit `android/app/src/main/AndroidManifest.xml`:

Find the BrowserTabActivity intent filter and update:

```xml
<data
    android:scheme="msauth"
    android:host="com.g365calendar"
    android:path="/AbCdEfGhIjKlMnOpQrStUvWxYz0=" />
```

### Step 4: Update MainActivity.java

Edit `android/app/src/main/java/com/g365calendar/MainActivity.java`:

```java
private static final String APP_ID = "YOUR_UNIQUE_APP_ID_HERE";  // Must match watch app
```

## iOS App Configuration

See the detailed [iOS Setup Guide](ios-setup.md) for complete instructions.

### Quick Configuration

1. Update `auth_config.json` in iOS project:
```json
{
  "client_id": "a1b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6",
  "redirect_uri": "msauth.com.g365calendar://auth",
  "authorities": [...]
}
```

2. Update Info.plist with redirect URI scheme:
```xml
<key>CFBundleURLSchemes</key>
<array>
    <string>msauth.com.g365calendar</string>
</array>
```

3. Update Connect IQ app UUID in your Swift code

## Developer Key Setup

### Generate Developer Key

Connect IQ apps must be signed with a developer key:

```bash
# Navigate to project root
cd /path/to/G365Calendar

# Create keys directory
mkdir -p .keys

# Generate private key
openssl genrsa -out .keys/developer_key.pem 4096

# Convert to DER format (required by Connect IQ)
openssl pkcs8 -topk8 -inform PEM -outform DER \
    -in .keys/developer_key.pem \
    -out .keys/developer_key.der \
    -nocrypt
```

### Protect Your Keys

The `.keys/` directory is in `.gitignore`. **Never** commit these files to version control.

### Build with Key

```bash
monkeyc -d venu2 \
    -f monkey.jungle \
    -o bin/G365Calendar.prg \
    -y .keys/developer_key.der
```

## Verification Checklist

Before testing, verify:

- ✅ Azure app registration created
- ✅ Application (client) ID copied
- ✅ Redirect URIs configured in Azure
- ✅ API permissions added (Calendars.Read, offline_access)
- ✅ CLIENT_ID updated in AuthManager.mc
- ✅ REDIRECT_URI matches Azure configuration
- ✅ Android signature hash calculated
- ✅ Android auth_config.json updated
- ✅ AndroidManifest.xml updated with signature hash
- ✅ Developer key generated
- ✅ App builds successfully

## Testing Your Configuration

### Watch App

1. Build the watch app
2. Deploy to simulator or device
3. Launch the app
4. Press SELECT to start OAuth flow
5. Verify authentication redirect works

### Android App

1. Build and install Android app
2. Launch the app
3. Tap "Authenticate with Microsoft"
4. Complete authentication
5. Verify "Sync Calendar to Watch" button works

### iOS App

1. Build iOS app in Xcode
2. Deploy to device (simulator has limitations)
3. Test authentication flow
4. Verify Connect IQ device connection

## Troubleshooting

### "Invalid redirect URI" Error

- Verify the redirect URI in your code exactly matches Azure
- Check for trailing slashes
- Ensure URL encoding is correct (Android)

### "Client ID not found" Error

- Verify you copied the Application (client) ID correctly
- Check for extra spaces or characters
- Ensure you're using the Application ID, not Object ID

### OAuth Timeout

- Check network connectivity
- Verify scopes are correct
- Ensure permissions are granted in Azure

### Android Signature Mismatch

- Recalculate signature hash with correct keystore
- Ensure URL encoding of equals sign (%3D)
- Update both auth_config.json and AndroidManifest.xml

### Watch App Won't Build

- Verify developer key exists in `.keys/` directory
- Check file permissions
- Ensure key is in DER format

## Additional Resources

- [Microsoft Identity Platform Documentation](https://learn.microsoft.com/azure/active-directory/develop/)
- [Connect IQ Developer Guide](https://developer.garmin.com/connect-iq/connect-iq-basics/)
- [Microsoft Graph API Documentation](https://learn.microsoft.com/graph/)

## Getting Help

If you encounter issues:

1. Check the [Troubleshooting](#troubleshooting) section
2. Search [existing issues](https://github.com/dmckinstry/G365Calendar/issues)
3. Create a new issue with detailed information
4. Include relevant logs and error messages (without exposing credentials)
