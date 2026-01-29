# Simple Android Emulator Bridged Mode Script
# This script will list available AVDs and let you choose one

# Get Android SDK path
if ($env:ANDROID_HOME) {
    $sdkPath = $env:ANDROID_HOME
} else {
    $sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
}

$emulatorExe = "$sdkPath\emulator\emulator.exe"

# Check if emulator exists
if (-not (Test-Path $emulatorExe)) {
    Write-Host "Error: Emulator not found at: $emulatorExe" -ForegroundColor Red
    Write-Host "Please set ANDROID_HOME environment variable or modify the script." -ForegroundColor Yellow
    pause
    exit
}

# List available AVDs
Write-Host "Available AVDs:" -ForegroundColor Cyan
$avds = & $emulatorExe -list-avds

if ($avds.Count -eq 0) {
    Write-Host "No AVDs found. Please create one in Android Studio first." -ForegroundColor Red
    pause
    exit
}

# Display AVDs with numbers
$index = 1
foreach ($avd in $avds) {
    Write-Host "$index. $avd" -ForegroundColor White
    $index++
}

# Let user select
Write-Host ""
$selection = Read-Host "Enter the number of the AVD to start (or press Enter for first one)"

if ([string]::IsNullOrWhiteSpace($selection)) {
    $selectedAvd = $avds[0]
} else {
    $num = [int]$selection - 1
    if ($num -ge 0 -and $num -lt $avds.Count) {
        $selectedAvd = $avds[$num]
    } else {
        Write-Host "Invalid selection. Using first AVD." -ForegroundColor Yellow
        $selectedAvd = $avds[0]
    }
}

Write-Host ""
Write-Host "Starting emulator: $selectedAvd" -ForegroundColor Green
Write-Host "Note: For bridged mode, configure it in Android Studio AVD Manager first." -ForegroundColor Yellow
Write-Host ""

# Start emulator
& $emulatorExe -avd $selectedAvd -netdelay none -netspeed full
