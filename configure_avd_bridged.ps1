# Configure All AVDs for Bridged Network Mode
# This script will modify all AVD config.ini files to use bridged network mode permanently

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
Write-Host "Finding all AVDs..." -ForegroundColor Cyan
$avds = & $emulatorExe -list-avds

if ($avds.Count -eq 0) {
    Write-Host "No AVDs found. Please create one in Android Studio first." -ForegroundColor Red
    pause
    exit
}

Write-Host "Found $($avds.Count) AVD(s):" -ForegroundColor Green
foreach ($avd in $avds) {
    Write-Host "  - $avd" -ForegroundColor White
}
Write-Host ""

# Get network adapters (optional)
Write-Host "Available network adapters:" -ForegroundColor Cyan
$adapters = Get-NetAdapter | Where-Object { $_.Status -eq "Up" } | Select-Object Name, InterfaceDescription
if ($adapters.Count -eq 0) {
    Write-Host "  No active network adapters found." -ForegroundColor Yellow
    $selectedAdapter = $null
} else {
    $index = 1
    foreach ($adapter in $adapters) {
        Write-Host "  $index. $($adapter.Name) - $($adapter.InterfaceDescription)" -ForegroundColor White
        $index++
    }
    Write-Host ""
    $adapterSelection = Read-Host "Enter the number of the network adapter to use for all AVDs (or press Enter to use default)"
    
    $selectedAdapter = $null
    if (-not [string]::IsNullOrWhiteSpace($adapterSelection)) {
        $num = [int]$adapterSelection - 1
        if ($num -ge 0 -and $num -lt $adapters.Count) {
            $selectedAdapter = $adapters[$num].Name
            Write-Host "Selected adapter: $selectedAdapter" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "Starting configuration..." -ForegroundColor Cyan
Write-Host ""

# Find AVD home directory
$avdHome = $env:ANDROID_AVD_HOME
if (-not $avdHome) {
    $avdHome = "$env:USERPROFILE\.android\avd"
}

$successCount = 0
$failCount = 0
$skippedCount = 0

# Configure each AVD
foreach ($avd in $avds) {
    Write-Host "Configuring: $avd" -ForegroundColor Yellow
    
    $configFile = Join-Path $avdHome "$avd.avd\config.ini"
    
    if (-not (Test-Path $configFile)) {
        Write-Host "  Error: Config file not found: $configFile" -ForegroundColor Red
        $failCount++
        continue
    }
    
    # Backup config file
    $backupFile = "$configFile.backup"
    if (-not (Test-Path $backupFile)) {
        Copy-Item $configFile $backupFile -Force
        Write-Host "  Backup created: $backupFile" -ForegroundColor Gray
    }
    
    # Read config file
    try {
        $configContent = Get-Content $configFile -ErrorAction Stop
        
        # Prepare new config lines
        $newConfig = @()
        
        # Track which settings we've set
        $netModeSet = $false
        $netAdapterSet = $false
        $netSpeedSet = $false
        $netDelaySet = $false
        
        # Process existing lines
        foreach ($line in $configContent) {
            if ($line -match "^hw\.network\s*=") {
                # Update existing network mode
                $newConfig += "hw.network = bridged"
                $netModeSet = $true
            } elseif ($line -match "^net\.if\s*=") {
                # Update existing adapter
                if ($selectedAdapter) {
                    $newConfig += "net.if = $selectedAdapter"
                    $netAdapterSet = $true
                } else {
                    # Keep existing adapter if no new one selected
                    $newConfig += $line
                    $netAdapterSet = $true
                }
            } elseif ($line -match "^net\.speed\s*=") {
                # Update speed
                $newConfig += "net.speed = full"
                $netSpeedSet = $true
            } elseif ($line -match "^net\.delay\s*=") {
                # Update delay
                $newConfig += "net.delay = none"
                $netDelaySet = $true
            } else {
                $newConfig += $line
            }
        }
        
        # Add network settings if they don't exist
        if (-not $netModeSet) {
            $newConfig += "hw.network = bridged"
        }
        
        if ($selectedAdapter -and -not $netAdapterSet) {
            $newConfig += "net.if = $selectedAdapter"
        }
        
        if (-not $netSpeedSet) {
            $newConfig += "net.speed = full"
        }
        
        if (-not $netDelaySet) {
            $newConfig += "net.delay = none"
        }
        
        # Write config file
        $newConfig | Set-Content $configFile -Encoding UTF8 -ErrorAction Stop
        
        Write-Host "  Success: Configured bridged network mode" -ForegroundColor Green
        if ($selectedAdapter) {
            Write-Host "    Adapter: $selectedAdapter" -ForegroundColor Gray
        }
        $successCount++
        
    } catch {
        Write-Host "  Error: Failed to configure - $($_.Exception.Message)" -ForegroundColor Red
        $failCount++
    }
    
    Write-Host ""
}

# Summary
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "Configuration Summary:" -ForegroundColor Cyan
Write-Host "  Success: $successCount AVD(s)" -ForegroundColor Green
if ($failCount -gt 0) {
    Write-Host "  Failed: $failCount AVD(s)" -ForegroundColor Red
}
Write-Host ""
Write-Host "All configured AVDs will use bridged network mode permanently." -ForegroundColor Green
Write-Host "Restart any running emulators for changes to take effect." -ForegroundColor Yellow
Write-Host ""

pause
