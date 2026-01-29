# Check if Android emulator is using bridged network mode
# This script verifies the AVD configuration and running emulator's network status

# Get Android SDK path
if ($env:ANDROID_HOME) {
    $sdkPath = $env:ANDROID_HOME
} else {
    $sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
}

$adbExe = "$sdkPath\platform-tools\adb.exe"
$emulatorExe = "$sdkPath\emulator\emulator.exe"

if (-not (Test-Path $adbExe)) {
    Write-Host "Error: ADB not found at: $adbExe" -ForegroundColor Red
    Write-Host "Please set ANDROID_HOME environment variable." -ForegroundColor Yellow
    pause
    exit
}

Write-Host "Checking emulator network configuration..." -ForegroundColor Cyan
Write-Host ""

# Check running emulators
$devices = & $adbExe devices 2>&1 | Where-Object { $_ -match "emulator-\d+" }
if ($devices.Count -eq 0) {
    Write-Host "No running emulators detected." -ForegroundColor Yellow
    Write-Host "Please start an emulator first." -ForegroundColor Yellow
    pause
    exit
}

Write-Host "Found running emulator(s):" -ForegroundColor Green
foreach ($device in $devices) {
    if ($device -match "(emulator-\d+)") {
        $serial = $matches[1]
        Write-Host "  - $serial" -ForegroundColor White
        
        # Get IP address
        Write-Host "    Checking IP address..." -ForegroundColor Cyan
        $ipInfo = & $adbExe -s $serial shell "ip addr show" 2>&1
        
        # Extract IPv4 addresses
        $ipAddresses = $ipInfo | Select-String -Pattern "inet\s+(\d+\.\d+\.\d+\.\d+)" | ForEach-Object {
            if ($_.Matches[0].Groups[1].Value) {
                $_.Matches[0].Groups[1].Value
            }
        }
        
        Write-Host "    IP Addresses:" -ForegroundColor Cyan
        $isBridged = $false
        foreach ($ip in $ipAddresses) {
            if ($ip -match "^10\.0\.2\.\d+$") {
                Write-Host "      $ip (NAT Mode - NOT Bridged)" -ForegroundColor Red
            } elseif ($ip -match "^192\.168\.\d+\.\d+$" -or $ip -match "^172\.(1[6-9]|2[0-9]|3[0-1])\.\d+\.\d+$" -or $ip -match "^10\.\d+\.\d+\.\d+$") {
                Write-Host "      $ip (Bridged Mode - OK)" -ForegroundColor Green
                $isBridged = $true
            } else {
                Write-Host "      $ip" -ForegroundColor Yellow
            }
        }
        
        if (-not $isBridged) {
            Write-Host ""
            Write-Host "    WARNING: Emulator is NOT using bridged mode!" -ForegroundColor Red
            Write-Host "    The IP address 10.0.2.x indicates NAT mode." -ForegroundColor Yellow
            Write-Host ""
            Write-Host "    To fix this:" -ForegroundColor Cyan
            Write-Host "    1. Close the emulator" -ForegroundColor White
            Write-Host "    2. Run: .\configure_avd_bridged.ps1" -ForegroundColor White
            Write-Host "    3. Restart the emulator" -ForegroundColor White
        } else {
            Write-Host ""
            Write-Host "    SUCCESS: Emulator is using bridged mode!" -ForegroundColor Green
        }
        
        Write-Host ""
    }
}

# Check AVD configuration files
Write-Host "Checking AVD configuration files..." -ForegroundColor Cyan
$avdHome = "$env:USERPROFILE\.android\avd"
if (Test-Path $avdHome) {
    $avdDirs = Get-ChildItem -Path $avdHome -Directory -Filter "*.avd"
    foreach ($avdDir in $avdDirs) {
        $configFile = Join-Path $avdDir.FullName "config.ini"
        if (Test-Path $configFile) {
            $configContent = Get-Content $configFile
            $networkMode = $configContent | Select-String -Pattern "^hw\.network\s*=\s*(.+)$"
            $netAdapter = $configContent | Select-String -Pattern "^net\.if\s*=\s*(.+)$"
            
            Write-Host "  AVD: $($avdDir.Name)" -ForegroundColor Yellow
            if ($networkMode) {
                $mode = $networkMode.Matches[0].Groups[1].Value.Trim()
                if ($mode -eq "bridged") {
                    Write-Host "    Network Mode: $mode (OK)" -ForegroundColor Green
                } else {
                    Write-Host "    Network Mode: $mode (Should be 'bridged')" -ForegroundColor Red
                }
            } else {
                Write-Host "    Network Mode: Not set (Default NAT mode)" -ForegroundColor Red
            }
            
            if ($netAdapter) {
                Write-Host "    Network Adapter: $($netAdapter.Matches[0].Groups[1].Value.Trim())" -ForegroundColor Gray
            }
            Write-Host ""
        }
    }
}

Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "If you see 10.0.2.x IP addresses, the emulator is using NAT mode." -ForegroundColor Yellow
Write-Host "Bridged mode IP addresses should be in the same range as your host (e.g., 192.168.x.x)" -ForegroundColor Green
Write-Host ""

pause
