# Environment Setup Script for Android TV Signage Player
$ErrorActionPreference = "Stop"

$toolsDir = "C:\Users\User\tools"
$jdkDir = "$toolsDir\jdk-17"
$sdkDir = "$toolsDir\android-sdk"

Write-Host "=== Setting up Build Environment ==="

# 1. Verify Java
if (Test-Path "$jdkDir\bin\java.exe") {
    Write-Host "[OK] Java JDK 17 found at $jdkDir"
} else {
    Write-Error "Java not found at $jdkDir"
}

# Set JAVA_HOME and PATH in current session and persistent user env
$env:JAVA_HOME = $jdkDir
[Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkDir, "User")
$env:PATH = "$jdkDir\bin;" + $env:PATH

# 2. Setup Android SDK
if (-not (Test-Path "$sdkDir\cmdline-tools\latest\bin\sdkmanager.bat")) {
    Write-Host "Setting up Android command-line tools..."
    New-Item -ItemType Directory -Force -Path "$sdkDir\cmdline-tools" | Out-Null
    $zipPath = "$toolsDir\commandlinetools.zip"
    if (-not (Test-Path $zipPath)) {
        Write-Host "Downloading command-line tools..."
        curl.exe -L -o $zipPath "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
    }
    Write-Host "Extracting command-line tools..."
    Expand-Archive -Path $zipPath -DestinationPath "$toolsDir\cmdline_temp" -Force
    if (Test-Path "$sdkDir\cmdline-tools\latest") {
        Remove-Item -Recurse -Force "$sdkDir\cmdline-tools\latest"
    }
    Move-Item -Path "$toolsDir\cmdline_temp\cmdline-tools" -Destination "$sdkDir\cmdline-tools\latest" -Force
    Remove-Item -Recurse -Force "$toolsDir\cmdline_temp", $zipPath
}

# Set ANDROID_HOME and PATH in current session and persistent user env
$env:ANDROID_HOME = $sdkDir
$env:ANDROID_SDK_ROOT = $sdkDir
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkDir, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $sdkDir, "User")
$env:PATH = "$sdkDir\cmdline-tools\latest\bin;$sdkDir\platform-tools;" + $env:PATH

# Add to user PATH permanently
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$additions = @("$jdkDir\bin", "$sdkDir\cmdline-tools\latest\bin", "$sdkDir\platform-tools")
foreach ($add in $additions) {
    if ($userPath -notlike "*$add*") {
        $userPath = "$userPath;$add"
    }
}
[Environment]::SetEnvironmentVariable("Path", $userPath, "User")

# 3. Accept licenses and install platform / build-tools
Write-Host "Accepting Android SDK licenses..."
$yesInput = "y`ny`ny`ny`ny`ny`ny`ny`ny`n"
$yesInput | & "$sdkDir\cmdline-tools\latest\bin\sdkmanager.bat" --licenses --sdk_root="$sdkDir"

Write-Host "Installing platforms;android-34 and build-tools;34.0.0..."
& "$sdkDir\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="$sdkDir" "platforms;android-34" "build-tools;34.0.0" "platform-tools"

Write-Host "=== Setup Environment Complete! ==="
