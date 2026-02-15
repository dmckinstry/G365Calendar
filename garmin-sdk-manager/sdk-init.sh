#!/bin/bash
set -e

SDK_VERSION="8.1.1"
MANIFEST_FILE="manifest.xml"

echo "🔧 Checking if SDK Manager CLI is installed..."
if ! command -v connect-iq-sdk-manager &> /dev/null; then
    echo "❌ 'connect-iq-sdk-manager' is not installed. Please run ./install-cli.sh first."
    exit 1
fi

echo ""
echo "📜 Viewing Garmin SDK license..."
connect-iq-sdk-manager agreement view

read -p "Paste the agreement hash from above: " AGREEMENT_HASH
connect-iq-sdk-manager agreement accept --agreement-hash "$AGREEMENT_HASH"

# Use environment variables if defined, else prompt
GARMIN_USERNAME="${GARMIN_USERNAME:-}"
GARMIN_PASSWORD="${GARMIN_PASSWORD:-}"

if [ -z "$GARMIN_USERNAME" ]; then
  read -p "Enter your Garmin username (email): " GARMIN_USERNAME
fi

if [ -z "$GARMIN_PASSWORD" ]; then
  read -s -p "Enter your Garmin password: " GARMIN_PASSWORD
  echo ""
fi

echo ""
echo "🔐 Logging into Garmin developer account..."
export GARMIN_USERNAME
export GARMIN_PASSWORD
connect-iq-sdk-manager login

echo ""
echo "⬇️ Downloading SDK version $SDK_VERSION..."
connect-iq-sdk-manager sdk set "$SDK_VERSION"

echo ""
echo "📂 Checking for manifest: $MANIFEST_FILE"
if [ -f "$MANIFEST_FILE" ]; then
    echo "📦 Downloading device files for manifest..."
    connect-iq-sdk-manager device download --manifest="$MANIFEST_FILE"
else
    echo "⚠️ '$MANIFEST_FILE' not found. Skipping device download."
    echo "You can run this manually later:"
    echo "  connect-iq-sdk-manager device download --manifest=manifest.xml"
fi

echo ""
echo "✅ SDK setup complete! SDK path:"
connect-iq-sdk-manager sdk current-path
