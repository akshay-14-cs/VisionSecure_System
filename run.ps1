# VisionSecure One-Click Startup Script
$env:ANDROID_HOME = "E:\ANDROID_APP\SDK"
$env:ANDROID_AVD_HOME = "E:\ANDROID_APP\AVD"

# Find a valid JDK that explicitly contains bin\jlink.exe
$validJdk = $null
$candidatePaths = @(
    "C:\Program Files\Java\latest\jdk-26",
    "C:\Program Files\Android\Android Studio\jbr",
    "C:\Program Files\Android\Android Studio\jre",
    "E:\ANDROID_APP\jbr",
    "E:\ANDROID_APP\jre",
    "E:\Android\jbr",
    "E:\Android\jre",
    "C:\Program Files\Java\jdk-17",
    "C:\Program Files\Java\jdk-21"
)

foreach ($path in $candidatePaths) {
    if (Test-Path "$path\bin\jlink.exe") {
        $validJdk = $path
        break
    }
}

if (-not $validJdk) {
    # Search drive C: and E: for any jlink.exe
    $foundJlink = Get-ChildItem -Path "C:\Program Files\Java", "C:\Program Files\Android", "E:\ANDROID_APP", "E:\Android" -Filter "jlink.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($foundJlink) {
        $validJdk = $foundJlink.Directory.Parent.FullName
    }
}

if ($validJdk) {
    $env:JAVA_HOME = $validJdk
    Write-Host "Using valid JDK at: $env:JAVA_HOME" -ForegroundColor Green
} else {
    Write-Host "Current JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Yellow
}

$emulatorExe = "E:\ANDROID_APP\SDK\emulator\emulator.exe"
$adbExe = "E:\ANDROID_APP\SDK\platform-tools\adb.exe"

# 1. Check if emulator is running
$runningDevices = & $adbExe devices
if ($runningDevices -notmatch "device\b") {
    Write-Host "Starting Pixel_6a emulator..." -ForegroundColor Yellow
    Start-Process -FilePath $emulatorExe -ArgumentList "-avd Pixel_6a -gpu swiftshader_indirect" -WindowStyle Hidden
    
    Write-Host "Waiting for emulator to boot up..." -ForegroundColor Yellow
    & $adbExe wait-for-device
    Start-Sleep -Seconds 5
} else {
    Write-Host "Emulator is already running!" -ForegroundColor Green
}

# 2. Stop old broken Gradle daemons & Build
Write-Host "Stopping cached Gradle daemons..." -ForegroundColor Cyan
.\gradlew --stop

Write-Host "Building and deploying VisionSecure app..." -ForegroundColor Cyan
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jlink.exe")) {
    .\gradlew installDebug "-Dorg.gradle.java.home=$env:JAVA_HOME"
} else {
    .\gradlew installDebug
}

# 3. Launch Login Screen
if ($LASTEXITCODE -eq 0) {
    Write-Host "App launched successfully!" -ForegroundColor Green
    & $adbExe shell am start -n com.example.vs_project/.ui.auth.GetStartedActivity
} else {
    Write-Host "Build failed! Please check output above." -ForegroundColor Red
}
