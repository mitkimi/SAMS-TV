# Android Emulator Bridged Mode Startup Script
# Usage: Modify $AVD_NAME below with your actual AVD name, then run this script

# Set Android SDK path
if ($env:ANDROID_HOME) {
    $ANDROID_SDK_ROOT = $env:ANDROID_HOME
} else {
    # Default path, modify according to your actual installation path
    $ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
}

# Set emulator executable path
$EMULATOR_PATH = Join-Path $ANDROID_SDK_ROOT "emulator\emulator.exe"

# Check if emulator is installed
if (-not (Test-Path $EMULATOR_PATH)) {
    Write-Host "Error: Emulator not found. Please check if ANDROID_SDK_ROOT path is correct" -ForegroundColor Red
    Write-Host "Current path: $EMULATOR_PATH" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Please follow these steps to set up bridged mode:" -ForegroundColor Cyan
    Write-Host "1. Open Android Studio" -ForegroundColor White
    Write-Host "2. Open Tools > Device Manager (or AVD Manager)" -ForegroundColor White
    Write-Host "3. Select your AVD and click Edit (pencil icon)" -ForegroundColor White
    Write-Host "4. Expand 'Show Advanced Settings'" -ForegroundColor White
    Write-Host "5. In 'Network' section, select 'Bridged'" -ForegroundColor White
    Write-Host "6. Select your network adapter" -ForegroundColor White
    Write-Host "7. Click 'Finish' to save settings" -ForegroundColor White
    Read-Host "Press Enter to exit"
    exit 1
}

# List available AVDs
Write-Host "Finding available AVDs..." -ForegroundColor Cyan
$avds = & $EMULATOR_PATH -list-avds

if ($avds.Count -eq 0) {
    Write-Host "No AVDs found. Please create one in Android Studio first." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Display AVDs
Write-Host ""
$index = 1
foreach ($avd in $avds) {
    Write-Host "$index. $avd" -ForegroundColor White
    $index++
}

# Auto-select TV AVD if available, otherwise let user choose
$selectedAvd = $null
foreach ($avd in $avds) {
    if ($avd -like "*Television*" -or $avd -like "*TV*" -or $avd -like "*tv*") {
        $selectedAvd = $avd
        Write-Host ""
        Write-Host "Auto-selected TV AVD: $selectedAvd" -ForegroundColor Green
        break
    }
}

# If no TV AVD found, let user select
if (-not $selectedAvd) {
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
}

# Start emulator in bridged mode
Write-Host ""
Write-Host "Starting emulator in bridged mode: $selectedAvd" -ForegroundColor Green
Write-Host ""
Write-Host "Bridged mode info:" -ForegroundColor Cyan
Write-Host "- Emulator will connect directly to host network interface" -ForegroundColor White
Write-Host "- Emulator will get an independent IP address in the same network segment as host" -ForegroundColor White
Write-Host "- Administrator privileges may be required for bridged mode" -ForegroundColor White
Write-Host ""
Write-Host "Note: Make sure bridged mode is configured in Android Studio AVD Manager first!" -ForegroundColor Yellow
Write-Host ""

# Start emulator
& $EMULATOR_PATH -avd $selectedAvd -netdelay none -netspeed full
