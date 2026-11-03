<#
.SYNOPSIS
    Installs TV Signage Player APK to Android TV / Google TV via network ADB.
.PARAMETER DeviceIp
    The IP address of the Android TV device.
.PARAMETER ApkPath
    Path to the APK file (defaults to debug APK).
.EXAMPLE
    .\scripts\install.ps1 -DeviceIp 192.168.1.50
#>
param(
    [Parameter(Mandatory=$true)]
    [string]$DeviceIp,

    [Parameter(Mandatory=$false)]
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk"
)

$packageName = "com.example.tvsignage"

if (-not (Test-Path $ApkPath)) {
    # Check alternate release path
    $releasePath = "app\build\outputs\apk\release\app-release.apk"
    if (Test-Path $releasePath) {
        $ApkPath = $releasePath
    } else {
        Write-Error "APK not found at $ApkPath or $releasePath. Please build with .\gradlew assembleDebug first."
        exit 1
    }
}

Write-Host "Connecting to Android TV at $DeviceIp:5555..." -ForegroundColor Cyan
adb connect "$DeviceIp:5555"

Write-Host "Waiting for device..." -ForegroundColor Cyan
adb -s "$DeviceIp:5555" wait-for-device

Write-Host "Installing APK: $ApkPath..." -ForegroundColor Cyan
adb -s "$DeviceIp:5555" install -r -g $ApkPath

Write-Host "Granting background launch (overlay) and battery whitelist..." -ForegroundColor Cyan
adb -s "$DeviceIp:5555" shell appops set $packageName SYSTEM_ALERT_WINDOW allow
adb -s "$DeviceIp:5555" shell dumpsys deviceidle whitelist +$packageName

Write-Host "Starting TV Signage Player..." -ForegroundColor Green
adb -s "$DeviceIp:5555" shell monkey -p $packageName -c android.intent.category.LAUNCHER 1

Write-Host "Success! Application installed and started." -ForegroundColor Green
