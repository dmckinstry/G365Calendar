# Quick Start Guide

Get up and running with G365Calendar in minutes!

## Prerequisites

- Docker Desktop installed
- Visual Studio Code with Dev Containers extension
- Microsoft 365 or Outlook.com account
- Garmin Venu 2/4 device (for testing on hardware)

## Step 1: Clone the Repository

```bash
git clone https://github.com/dmckinstry/G365Calendar.git
cd G365Calendar
```

## Step 2: Open in Dev Container

```bash
code .
```

When VS Code opens, you'll see a prompt: **"Reopen in Container"**. Click it.

The container will:
- Install Java 21
- Download Connect IQ SDK
- Install Android SDK
- Configure development environment

This may take 5-10 minutes on first run.

## Step 3: Set Up Azure App Registration

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory > App registrations**
3. Click **New registration**
4. Configure:
   - Name: `G365Calendar`
   - Accounts: Select "Personal and organizational"
   - Redirect URI: `https://localhost/oauth/redirect`
5. Add API permissions:
   - Microsoft Graph > Delegated > `Calendars.Read`
   - Microsoft Graph > Delegated > `offline_access`
6. Copy the **Application (client) ID**

See [docs/configuration.md](docs/configuration.md) for detailed steps.

## Step 4: Configure the Watch App

Edit `source/AuthManager.mc`:

```monkey-c
private const CLIENT_ID = "YOUR_CLIENT_ID_HERE";  // Paste your Application ID
```

## Step 5: Generate Developer Key

```bash
mkdir -p .keys
openssl genrsa -out .keys/developer_key.pem 4096
openssl pkcs8 -topk8 -inform PEM -outform DER \
    -in .keys/developer_key.pem \
    -out .keys/developer_key.der \
    -nocrypt
```

## Step 6: Build the Watch App

In VS Code:

1. Open Command Palette (Ctrl+Shift+P / Cmd+Shift+P)
2. Type "Monkey C: Build for Device"
3. Select "Venu 2" (or your target device)

Or using command line:

```bash
monkeyc -d venu2 -f monkey.jungle -o bin/G365Calendar.prg -y .keys/developer_key.der
```

## Step 7: Test in Simulator (Optional)

1. Open Command Palette
2. Type "Monkey C: Run"
3. Select "Venu 2 Simulator"

The Connect IQ simulator will launch with your app.

**Note**: OAuth flows require actual device or companion app testing.

## Step 8: Deploy to Device

### Option A: Using Garmin Express

1. Connect your Garmin device via USB
2. Build the app to create `.prg` file
3. Copy `bin/G365Calendar.prg` to your device
4. Disconnect and launch the app

### Option B: Using Connect IQ App Store (for distribution)

1. Create a developer account at [Connect IQ Store](https://apps.garmin.com/developer)
2. Upload your `.prg` file
3. Complete app submission process

## Step 9: Configure and Build Android App (Optional)

The Android companion app provides enhanced OAuth and data sync.

1. Calculate your app's signature hash:
```bash
keytool -exportcert -alias androiddebugkey \
    -keystore ~/.android/debug.keystore | \
    openssl sha1 -binary | openssl base64
```

2. Update `android/app/src/main/res/raw/auth_config.json`:
```json
{
  "client_id": "YOUR_CLIENT_ID",
  "redirect_uri": "msauth://com.g365calendar/YOUR_SIGNATURE_HASH"
}
```

3. Build:
```bash
cd android
./gradlew assembleDebug
./gradlew installDebug
```

See [android/README.md](android/README.md) for details.

## Step 10: Test the Complete Flow

1. **Install watch app** on your Garmin device
2. **Launch the app** on your watch
3. **Press SELECT** to initiate OAuth
4. **Complete authentication** on your phone
5. **View your calendar events** on the watch!

## Troubleshooting

### "SDK not found" error

Ensure the devcontainer completed setup. Check:
```bash
echo $MB_HOME
ls /opt/connectiq-sdk
```

### "Invalid client ID" error

- Verify you copied the Application (client) ID correctly
- Check for extra spaces
- Ensure it's updated in `source/AuthManager.mc`

### OAuth redirect fails

- Verify redirect URI in Azure matches exactly: `https://localhost/oauth/redirect`
- Check network connectivity
- Ensure permissions are granted

### App won't build

- Check that developer key exists: `ls .keys/`
- Ensure key is in DER format
- Verify manifest.xml syntax

## Next Steps

- 📖 Read [Configuration Guide](docs/configuration.md) for detailed setup
- 🔧 Customize event window in `source/ApiClient.mc`
- 📱 Build iOS companion app (see [docs/ios-setup.md](docs/ios-setup.md))
- 🎨 Customize UI in `source/CalendarView.mc`
- 🤝 Contribute! See [CONTRIBUTING.md](CONTRIBUTING.md)

## Getting Help

- 📚 Check the [full documentation](README.md)
- 🐛 [Report issues](https://github.com/dmckinstry/G365Calendar/issues)
- 💬 Ask questions in [Discussions](https://github.com/dmckinstry/G365Calendar/discussions)

## What's Included?

This quick start covers the watch app. The project also includes:

- ✅ **Watch App**: Monkey C app for Garmin devices
- ✅ **Android App**: Companion app with MSAL
- 📄 **iOS Guide**: Instructions for Mac development
- 🛠️ **Dev Container**: Complete development environment
- 📖 **Documentation**: Configuration and setup guides

Happy coding! 🎉
