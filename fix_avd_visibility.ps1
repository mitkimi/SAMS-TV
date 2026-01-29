# Fix AVD visibility in Device Manager
# This script helps troubleshoot why AVDs don't show up in Device Manager

# Get Android SDK path
if ($env:ANDROID_HOME) {
    $sdkPath = $env:ANDROID_HOME
} else {
    $sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
}

$emulatorExe = "$sdkPath\emulator\emulator.exe"
$avdHome = "$env:USERPROFILE\.android\avd"

Write-Host "Checking AVD configuration..." -ForegroundColor Cyan
Write-Host ""

# Check if emulator exists
if (-not (Test-Path $emulatorExe)) {
    Write-Host "Error: Emulator not found at: $emulatorExe" -ForegroundColor Red
    Write-Host "Please set ANDROID_HOME environment variable." -ForegroundColor Yellow
    pause
    exit
}

# List AVDs via command line
Write-Host "AVDs found via command line:" -ForegroundColor Green
$avds = & $emulatorExe -list-avds
if ($avds.Count -eq 0) {
    Write-Host "  No AVDs found!" -ForegroundColor Red
} else {
    foreach ($avd in $avds) {
        Write-Host "  - $avd" -ForegroundColor White
    }
}
Write-Host ""

# Check AVD directory structure
Write-Host "Checking AVD directory structure..." -ForegroundColor Cyan
if (Test-Path $avdHome) {
    $avdDirs = Get-ChildItem -Path $avdHome -Directory -Filter "*.avd"
    Write-Host "Found $($avdDirs.Count) AVD directory(ies):" -ForegroundColor Green
    
    foreach ($avdDir in $avdDirs) {
        $avdName = $avdDir.Name -replace "\.avd$", ""
        Write-Host ""
        Write-Host "  AVD: $avdName" -ForegroundColor Yellow
        
        # Check config.ini
        $configFile = Join-Path $avdDir.FullName "config.ini"
        if (Test-Path $configFile) {
            Write-Host "    Config file: OK" -ForegroundColor Green
            
            # Check key settings
            $configContent = Get-Content $configFile
            $avdId = $configContent | Select-String -Pattern "^avd\.id\s*=\s*(.+)$"
            $target = $configContent | Select-String -Pattern "^target\s*=\s*(.+)$"
            
            if ($avdId) {
                Write-Host "    AVD ID: $($avdId.Matches[0].Groups[1].Value.Trim())" -ForegroundColor Gray
            }
            if ($target) {
                Write-Host "    Target: $($target.Matches[0].Groups[1].Value.Trim())" -ForegroundColor Gray
            }
        } else {
            Write-Host "    Config file: MISSING!" -ForegroundColor Red
        }
        
        # Check .ini file in parent directory
        $iniFile = Join-Path $avdHome "$avdName.ini"
        if (Test-Path $iniFile) {
            Write-Host "    .ini file: OK" -ForegroundColor Green
            $iniContent = Get-Content $iniFile
            $path = $iniContent | Select-String -Pattern "^path\s*=\s*(.+)$"
            if ($path) {
                Write-Host "    Path: $($path.Matches[0].Groups[1].Value.Trim())" -ForegroundColor Gray
            }
        } else {
            Write-Host "    .ini file: MISSING!" -ForegroundColor Red
            Write-Host "    Creating .ini file..." -ForegroundColor Yellow
            $iniContent = @"
avd.ini.encoding=UTF-8
path=$($avdDir.FullName)
"@
            $iniContent | Set-Content $iniFile -Encoding UTF8
            Write-Host "    Created: $iniFile" -ForegroundColor Green
        }
    }
} else {
    Write-Host "AVD directory not found: $avdHome" -ForegroundColor Red
}

Write-Host ""
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "Troubleshooting steps:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. If AVDs show in command line but not in Device Manager:" -ForegroundColor White
Write-Host "   - Close and reopen Android Studio" -ForegroundColor Yellow
Write-Host "   - In Android Studio: File > Invalidate Caches / Restart" -ForegroundColor Yellow
Write-Host ""
Write-Host "2. If .ini files are missing (created above):" -ForegroundColor White
Write-Host "   - Restart Android Studio" -ForegroundColor Yellow
Write-Host "   - The AVDs should now appear in Device Manager" -ForegroundColor Yellow
Write-Host ""
Write-Host "3. To configure bridged mode without Device Manager:" -ForegroundColor White
Write-Host "   - Run: .\configure_avd_bridged.ps1" -ForegroundColor Yellow
Write-Host "   - This will directly modify the config.ini files" -ForegroundColor Yellow
Write-Host ""

pause
