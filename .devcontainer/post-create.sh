#!/bin/bash
set -e

echo "Setting up G365Calendar development environment..."

# Install required packages
sudo apt-get update
sudo apt-get install -y wget unzip

# Install Connect IQ SDK
SDK_VERSION="7.3.1"
SDK_URL="https://developer.garmin.com/downloads/connect-iq/sdks/connectiq-sdk-lin-${SDK_VERSION}.zip"
echo "Installing Connect IQ SDK ${SDK_VERSION}..."
sudo mkdir -p /opt/connectiq-sdk
cd /tmp
wget -q "${SDK_URL}" -O connectiq-sdk.zip || echo "Note: SDK download may require manual installation"
if [ -f connectiq-sdk.zip ]; then
    sudo unzip -q connectiq-sdk.zip -d /opt/connectiq-sdk
    sudo chmod -R 755 /opt/connectiq-sdk
    rm connectiq-sdk.zip
fi

# Install Android SDK command-line tools
CMDLINE_TOOLS_VERSION="11076708"
ANDROID_SDK_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
echo "Installing Android SDK command-line tools..."
sudo mkdir -p /opt/android-sdk/cmdline-tools
cd /tmp
wget -q "${ANDROID_SDK_URL}" -O commandlinetools.zip
sudo unzip -q commandlinetools.zip -d /opt/android-sdk/cmdline-tools
sudo mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest
sudo chmod -R 755 /opt/android-sdk
rm commandlinetools.zip

# Accept Android SDK licenses
yes | sudo /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses || true

# Install required Android SDK packages
echo "Installing Android SDK packages..."
sudo /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0"

# Create .keys directory if it doesn't exist
mkdir -p /workspace/.keys

# Display setup information
echo ""
echo "======================================"
echo "Setup complete!"
echo "======================================"
echo "MB_HOME: /opt/connectiq-sdk"
echo "ANDROID_HOME: /opt/android-sdk"
echo ""
echo "Note: To generate a developer key for Garmin, run:"
echo "  openssl genrsa -out /workspace/.keys/developer_key.pem 4096"
echo "  openssl pkcs8 -topk8 -inform PEM -outform DER -in /workspace/.keys/developer_key.pem -out /workspace/.keys/developer_key.der -nocrypt"
echo ""
