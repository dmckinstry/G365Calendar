# iOS Companion App Development Setup

This document provides instructions for developing the G365Calendar iOS companion app on macOS. The iOS companion app enables enhanced OAuth handling and bidirectional communication with the Garmin watch app.

## Prerequisites

- macOS 12.0 or later
- Xcode 14.0 or later
- Apple Developer account (for device testing)
- Connect IQ iOS SDK
- CocoaPods or Swift Package Manager

## Connect IQ iOS SDK Integration

### 1. Download Connect IQ iOS SDK

1. Visit the [Garmin Connect IQ Developer Portal](https://developer.garmin.com/connect-iq/sdk/)
2. Download the Connect IQ iOS SDK
3. Extract the SDK to a known location (e.g., `~/ConnectIQ/ios-sdk`)

### 2. Create iOS Project

Create a new iOS project in Xcode:

```bash
# Using Xcode
# File > New > Project > iOS > App
# Product Name: G365Calendar
# Bundle Identifier: com.g365calendar
# Interface: SwiftUI or UIKit
# Language: Swift
```

### 3. Add Connect IQ SDK Framework

1. In Xcode, select your project in the navigator
2. Select the target and go to "General" tab
3. Under "Frameworks, Libraries, and Embedded Content", click "+"
4. Add `ConnectIQ.framework` from the SDK directory
5. Set "Embed & Sign" for the framework

Alternatively, using manual linking:

```bash
# Add framework search path to Build Settings
FRAMEWORK_SEARCH_PATHS = $(inherited) ~/ConnectIQ/ios-sdk
```

### 4. Configure Info.plist

Add required permissions and configurations:

```xml
<key>NSBluetoothAlwaysUsageDescription</key>
<string>Required to connect to Garmin devices</string>

<key>NSBluetoothPeripheralUsageDescription</key>
<string>Required to communicate with Garmin devices</string>

<key>UIBackgroundModes</key>
<array>
    <string>bluetooth-central</string>
</array>
```

## Microsoft Authentication Library (MSAL) Setup

### 1. Add MSAL Dependency

Using CocoaPods, add to `Podfile`:

```ruby
platform :ios, '14.0'

target 'G365Calendar' do
  use_frameworks!
  
  # Microsoft Authentication Library
  pod 'MSAL', '~> 1.3.0'
end
```

Or using Swift Package Manager:

1. File > Add Packages
2. Search for: `https://github.com/AzureAD/microsoft-authentication-library-for-objc`
3. Add MSAL package

### 2. Configure MSAL

Create `auth_config.json` in your project:

```json
{
  "client_id": "YOUR_CLIENT_ID",
  "redirect_uri": "msauth.com.g365calendar://auth",
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

### 3. Configure URL Scheme

In Info.plist, add:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>msauth.com.g365calendar</string>
        </array>
    </dict>
</array>

<key>LSApplicationQueriesSchemes</key>
<array>
    <string>msauthv2</string>
    <string>msauthv3</string>
</array>
```

## Companion App Architecture

### Core Components

1. **ConnectIQManager**: Handles device discovery and communication
2. **AuthenticationManager**: Manages Microsoft OAuth flow via MSAL
3. **CalendarService**: Fetches calendar events from Microsoft Graph API
4. **MessageHandler**: Processes bidirectional messaging with watch

### Sample Implementation Structure

```
G365Calendar/
├── App/
│   ├── G365CalendarApp.swift          # Main app entry
│   └── AppDelegate.swift              # Handle URL schemes
├── Managers/
│   ├── ConnectIQManager.swift         # Connect IQ device management
│   ├── AuthenticationManager.swift    # MSAL authentication
│   └── CalendarService.swift          # Graph API integration
├── Views/
│   ├── MainView.swift                 # Main UI
│   ├── DeviceSelectionView.swift     # Device pairing UI
│   └── CalendarSyncView.swift         # Calendar sync status
├── Models/
│   ├── CalendarEvent.swift            # Event data model
│   └── DeviceConnection.swift         # Device state model
└── Resources/
    ├── auth_config.json               # MSAL configuration
    └── Assets.xcassets                # App assets
```

### Key Implementation Points

#### 1. Initialize Connect IQ

```swift
import ConnectIQ

class ConnectIQManager {
    private var connectIQ: ConnectIQ?
    
    func initialize() {
        connectIQ = ConnectIQ.sharedInstance()
        connectIQ?.initialize(withUrlScheme: "g365calendar", 
                             uiOverrideDelegate: nil)
    }
    
    func findDevices() -> [IQDevice] {
        return connectIQ?.knownDevices ?? []
    }
    
    func sendMessage(to device: IQDevice, message: [String: Any]) {
        let app = IQApp(uuid: "YOUR_APP_UUID")
        connectIQ?.sendMessage(message, 
                              to: app, 
                              on: device) { result in
            // Handle result
        }
    }
}
```

#### 2. Implement MSAL Authentication

```swift
import MSAL

class AuthenticationManager {
    private var msalApp: MSALPublicClientApplication?
    
    func initialize() throws {
        let config = MSALPublicClientApplicationConfig(clientId: "YOUR_CLIENT_ID")
        config.authority = try MSALAuthority(url: URL(string: "https://login.microsoftonline.com/common")!)
        
        msalApp = try MSALPublicClientApplication(configuration: config)
    }
    
    func acquireToken(completion: @escaping (String?, Error?) -> Void) {
        let parameters = MSALInteractiveTokenParameters(scopes: ["Calendars.Read"])
        
        msalApp?.acquireToken(with: parameters) { result, error in
            if let result = result {
                completion(result.accessToken, nil)
            } else {
                completion(nil, error)
            }
        }
    }
}
```

#### 3. Microsoft Graph Integration

```swift
import Foundation

class CalendarService {
    private let graphEndpoint = "https://graph.microsoft.com/v1.0/me/calendar/events"
    
    func fetchEvents(accessToken: String, completion: @escaping ([CalendarEvent]?, Error?) -> Void) {
        var request = URLRequest(url: URL(string: graphEndpoint)!)
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            // Parse and return calendar events
        }.resume()
    }
}
```

## Building and Running

### Development Build

```bash
# Open project
open G365Calendar.xcodeproj

# Or if using workspace (with CocoaPods)
open G365Calendar.xcworkspace

# Build from command line
xcodebuild -scheme G365Calendar -configuration Debug build
```

### Testing on Device

1. Connect iOS device via USB
2. Select device in Xcode
3. Ensure proper signing (Apple Developer account required)
4. Run the app (⌘R)

### Simulator Limitations

Note: The Connect IQ SDK and Bluetooth features are not fully functional in the iOS Simulator. Device testing is required for full functionality.

## Microsoft 365 App Registration

### Register App in Azure Portal

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to Azure Active Directory > App registrations
3. Click "New registration"
4. Configure:
   - Name: G365Calendar
   - Supported account types: Personal Microsoft accounts and organizational accounts
   - Redirect URI: 
     - Type: Public client/native
     - URI: `msauth.com.g365calendar://auth`
5. Under "API permissions", add:
   - Microsoft Graph > Delegated permissions > Calendars.Read
6. Note the Application (client) ID for configuration

## Development Outside Devcontainer

The iOS companion app must be developed on macOS using Xcode, which is outside the Linux-based devcontainer. The development workflow is:

1. **Watch App**: Develop in devcontainer with Monkey C
2. **Android App**: Develop in devcontainer with Android SDK
3. **iOS App**: Develop on Mac host with Xcode

### Syncing Changes

Use git to sync changes between devcontainer and macOS host:

```bash
# In devcontainer
git add .
git commit -m "Update watch app"
git push

# On macOS
git pull
# Continue iOS development
```

## Troubleshooting

### Connect IQ SDK Issues

- Ensure framework is properly embedded
- Check framework search paths in Build Settings
- Verify deployment target matches SDK requirements

### MSAL Issues

- Verify redirect URI matches Azure app registration
- Check URL scheme configuration in Info.plist
- Ensure proper permissions in Info.plist

### Communication Issues

- Verify watch app UUID matches in both watch and iOS apps
- Check Bluetooth permissions
- Ensure devices are properly paired in Garmin Connect app

## Additional Resources

- [Connect IQ iOS SDK Documentation](https://developer.garmin.com/connect-iq/core-topics/mobile-sdk-for-ios/)
- [MSAL iOS Documentation](https://learn.microsoft.com/en-us/azure/active-directory/develop/tutorial-v2-ios)
- [Microsoft Graph API Reference](https://learn.microsoft.com/en-us/graph/api/resources/calendar)
- [Garmin Connect IQ Developer Forums](https://forums.garmin.com/developer/connect-iq/)
