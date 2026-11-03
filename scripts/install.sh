#!/usr/bin/env bash
# Sideload and permission grant script for Android TV / Google TV
set -e

DEVICE_IP="$1"
APK_PATH="${2:-app/build/outputs/apk/debug/app-debug.apk}"
PACKAGE_NAME="com.example.tvsignage"

if [ -z "$DEVICE_IP" ]; then
    echo "Usage: ./scripts/install.sh <TV_IP_ADDRESS> [APK_PATH]"
    echo "Example: ./scripts/install.sh 192.168.1.50"
    exit 1
fi

if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at $APK_PATH"
    echo "Please build the project first with: ./gradlew assembleDebug"
    exit 1
fi

echo "Connecting to Android TV at $DEVICE_IP:5555..."
adb connect "$DEVICE_IP:5555"

echo "Waiting for device connection..."
adb -s "$DEVICE_IP:5555" wait-for-device

echo "Installing APK: $APK_PATH..."
adb -s "$DEVICE_IP:5555" install -r -g "$APK_PATH"

echo "Granting special permissions for background launch & kiosk mode..."
adb -s "$DEVICE_IP:5555" shell appops set "$PACKAGE_NAME" SYSTEM_ALERT_WINDOW allow || true
adb -s "$DEVICE_IP:5555" shell dumpsys deviceidle whitelist +"$PACKAGE_NAME" || true

echo "Starting TV Signage Player..."
adb -s "$DEVICE_IP:5555" shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1

echo "Installation complete!"
