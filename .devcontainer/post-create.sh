#!/bin/bash
set -e

echo "============================================"
echo "Setting up G365Calendar development environment"
echo "============================================"

# -----------------------------------------------------------
# 1. System packages
# -----------------------------------------------------------
sudo apt-get update -qq
sudo apt-get install -y -qq wget unzip

# -----------------------------------------------------------
# 2. Android SDK
# -----------------------------------------------------------
CMDLINE_TOOLS_VERSION="11076708"
ANDROID_SDK_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"

echo ""
echo ">>> Installing Android SDK command-line tools..."
sudo mkdir -p /opt/android-sdk/cmdline-tools
cd /tmp
wget -q "${ANDROID_SDK_URL}" -O commandlinetools.zip
sudo unzip -q commandlinetools.zip -d /opt/android-sdk/cmdline-tools
sudo mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest
sudo chmod -R 755 /opt/android-sdk
rm commandlinetools.zip

# Accept licenses
yes | sudo /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null 2>&1 || true

# Install required SDK packages (matching build.gradle.kts: compileSdk=35, minSdk=26)
echo ">>> Installing Android SDK packages..."
sudo /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --install \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0" \
    "platforms;android-26" \
    > /dev/null 2>&1

# -----------------------------------------------------------
# 3. Gradle wrapper bootstrap (in the android project)
# -----------------------------------------------------------
echo ""
echo ">>> Bootstrapping Gradle wrapper..."
cd /workspaces/G365Calendar/android 2>/dev/null || cd "${CODESPACE_VSCODE_FOLDER:-/workspace}/android" 2>/dev/null || true

if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
    # Download the Gradle wrapper jar directly
    GRADLE_VERSION="8.11.1"
    GRADLE_WRAPPER_URL="https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar"
    echo "    Downloading Gradle wrapper jar (v${GRADLE_VERSION})..."
    wget -q "${GRADLE_WRAPPER_URL}" -O gradle/wrapper/gradle-wrapper.jar 2>/dev/null || true

    # If direct download failed, try via Gradle distribution
    if [ ! -s gradle/wrapper/gradle-wrapper.jar ]; then
        echo "    Downloading full Gradle distribution to extract wrapper..."
        cd /tmp
        wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O gradle-dist.zip
        unzip -q -j gradle-dist.zip "gradle-${GRADLE_VERSION}/lib/gradle-wrapper-*.jar" -d /tmp/gradle-wrapper/ 2>/dev/null || true
        if ls /tmp/gradle-wrapper/*.jar 1>/dev/null 2>&1; then
            cp /tmp/gradle-wrapper/*.jar /workspaces/G365Calendar/android/gradle/wrapper/gradle-wrapper.jar 2>/dev/null || \
            cp /tmp/gradle-wrapper/*.jar "${CODESPACE_VSCODE_FOLDER:-/workspace}/android/gradle/wrapper/gradle-wrapper.jar" 2>/dev/null || true
        fi
        rm -rf gradle-dist.zip /tmp/gradle-wrapper/
    fi
fi

# Create gradlew scripts if missing
cd /workspaces/G365Calendar/android 2>/dev/null || cd "${CODESPACE_VSCODE_FOLDER:-/workspace}/android" 2>/dev/null || true
if [ ! -f gradlew ]; then
    cat > gradlew << 'GRADLEW_EOF'
#!/bin/sh
# Gradle wrapper script
exec java -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
GRADLEW_EOF
    chmod +x gradlew
fi

# -----------------------------------------------------------
# 4. Garmin Connect IQ SDK
# -----------------------------------------------------------
echo ""
echo ">>> Installing Garmin Connect IQ SDK..."
# The Connect IQ SDK requires a Garmin developer account to download.
# Attempt to install; will print instructions if download is unavailable.
SDK_VERSION="7.3.1"
SDK_URL="https://developer.garmin.com/downloads/connect-iq/sdks/connectiq-sdk-lin-${SDK_VERSION}.zip"
sudo mkdir -p /opt/connectiq-sdk
cd /tmp
wget -q "${SDK_URL}" -O connectiq-sdk.zip 2>/dev/null || true
if [ -f connectiq-sdk.zip ] && [ -s connectiq-sdk.zip ]; then
    sudo unzip -q connectiq-sdk.zip -d /opt/connectiq-sdk
    sudo chmod -R 755 /opt/connectiq-sdk
    rm connectiq-sdk.zip
    echo "    Connect IQ SDK ${SDK_VERSION} installed"
else
    rm -f connectiq-sdk.zip
    echo "    ⚠ Connect IQ SDK download unavailable (may require Garmin developer login)."
    echo "    Manual install: download from https://developer.garmin.com/connect-iq/sdk/"
    echo "    and extract to /opt/connectiq-sdk/"
fi

# -----------------------------------------------------------
# 5. Verify installation
# -----------------------------------------------------------
echo ""
echo "============================================"
echo "Environment setup complete!"
echo "============================================"
echo ""
echo "Java:        $(java -version 2>&1 | head -1)"
echo "JAVA_HOME:   ${JAVA_HOME:-/usr/lib/jvm/msopenjdk-current}"
echo "ANDROID_HOME:/opt/android-sdk"

if command -v monkeyc &>/dev/null; then
    echo "Connect IQ:  $(monkeyc --version 2>&1 || echo 'installed')"
else
    echo "Connect IQ:  not installed (see instructions above)"
fi

echo ""
echo "Quick start:"
echo "  cd android && ./gradlew assembleDebug   # Build Android app"
echo "  cd android && ./gradlew test             # Run Android tests"
echo "  cd android && ./gradlew ktlintCheck      # Run Kotlin linter"
echo ""
