[CmdletBinding()]
param(
    [string]$AvdName = "Pixel_10_Pro"
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $root

Write-Host "=== 1. Kiem tra Docker va khoi dong Backend / DB ===" -ForegroundColor Cyan
$dockerCli = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCli) {
    throw "Docker CLI khong ton tai trong PATH."
}

$dockerRunning = $false
try {
    $null = docker info 2>$null
    if ($LASTEXITCODE -eq 0) { $dockerRunning = $true }
} catch {}

if (-not $dockerRunning) {
    Write-Host "Docker chua chay. Dang bat Docker Desktop..." -ForegroundColor Yellow
    $dockerExe = "C:\\Program Files\\Docker\\Docker\\Docker Desktop.exe"
    if (Test-Path $dockerExe) {
        Start-Process -FilePath $dockerExe -WindowStyle Hidden
    } else {
        throw "Khong tim thay Docker Desktop tai: $dockerExe"
    }

    $attempts = 0
    while (-not $dockerRunning -and $attempts -lt 40) {
        Start-Sleep -Seconds 3
        $attempts++
        try {
            $null = docker info 2>$null
            if ($LASTEXITCODE -eq 0) { $dockerRunning = $true }
        } catch {}
    }
    if (-not $dockerRunning) {
        throw "Qua thoi gian cho Docker khoi dong."
    }
}
Write-Host "Docker engine da san sang." -ForegroundColor Green

Write-Host "Bat containers 'vsat-pg' va 'vsat-api'..." -ForegroundColor Cyan
docker start vsat-pg | Out-Null
docker start vsat-api | Out-Null

Write-Host "Kiem tra backend API tai http://localhost:18080/api/v1/actuator/health..." -ForegroundColor Cyan
$healthy = $false
$healthUrl = "http://localhost:18080/api/v1/actuator/health"
for ($i = 0; $i -lt 30; $i++) {
    try {
        $res = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($res.status -eq "UP") {
            $healthy = $true
            break
        }
    } catch {}
    Start-Sleep -Seconds 2
}
if ($healthy) {
    Write-Host "Backend API da UP (port 18080)." -ForegroundColor Green
} else {
    Write-Warning "Backend API chua bao UP, tiep tuc cac buoc tiep theo..."
}

Write-Host "=== 2. Kiem tra va khoi dong Android Emulator ===" -ForegroundColor Cyan
$sdkPath = Join-Path $env:LOCALAPPDATA "Android\\Sdk"
$adbPath = Join-Path $sdkPath "platform-tools\\adb.exe"
$emulatorPath = Join-Path $sdkPath "emulator\\emulator.exe"

if (-not (Test-Path $adbPath)) {
    throw "Khong tim thay adb tai $adbPath"
}
if (-not (Test-Path $emulatorPath)) {
    throw "Khong tim thay emulator tai $emulatorPath"
}

$devices = & $adbPath devices | Select-String "device$"
$emulatorRunning = $devices.Count -gt 0

if (-not $emulatorRunning) {
    Write-Host "Khoi dong may ao '$AvdName'..." -ForegroundColor Yellow
    Start-Process -FilePath $emulatorPath -ArgumentList "-avd", $AvdName

    Write-Host "Cho may ao khoi dong xong..." -ForegroundColor Cyan
    & $adbPath wait-for-device

    for ($i = 0; $i -lt 60; $i++) {
        $status = (& $adbPath shell getprop sys.boot_completed 2>$null).Trim()
        if ($status -eq "1") { break }
        Start-Sleep -Seconds 2
    }
} else {
    Write-Host "May ao dang chay." -ForegroundColor Green
}

Write-Host "Thiet lap adb reverse tcp:18080 tcp:18080..." -ForegroundColor Cyan
& $adbPath reverse tcp:18080 tcp:18080 | Out-Null

Write-Host "=== 3. Mo App V-SAT Compass tren may ao ===" -ForegroundColor Cyan
$launcherActivity = "com.example.v_sat_compass/.ui.SplashActivity"
& $adbPath shell am start -n $launcherActivity | Out-Null

Write-Host "=== 4. Kiem tra va khoi dong Web Admin (Vite: 5173) ===" -ForegroundColor Cyan
$webListening = $false
try {
    $conn = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
    if ($conn) { $webListening = $true }
} catch {}

if (-not $webListening) {
    Write-Host "Khoi dong Web Admin tai port 5173..." -ForegroundColor Yellow
    $webDir = Resolve-Path (Join-Path $root "web-admin")
    $nodeExe = "C:\\Program Files\\nodejs\\node.exe"
    if (Test-Path $nodeExe) {
        Start-Process -FilePath $nodeExe -ArgumentList "node_modules/vite/bin/vite.js", "--host", "--port", "5173" -WorkingDirectory $webDir -WindowStyle Hidden
    }
    for ($i = 0; $i -lt 15; $i++) {
        Start-Sleep -Seconds 1
        $conn = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
        if ($conn) {
            $webListening = $true
            break
        }
    }
}

if ($webListening) {
    Write-Host "Web Admin da san sang tai: http://localhost:5173" -ForegroundColor Green
} else {
    Write-Warning "Chua ket noi duoc port 5173 cho Web Admin."
}

Write-Host "=== DA KHOI DONG THANH CONG TOAN BO PROJECT! ===" -ForegroundColor Green
Write-Host "  - PostgreSQL: localhost:5432 (container: vsat-pg)"
Write-Host "  - Spring Boot API: localhost:18080 (container: vsat-api)"
Write-Host "  - Android Emulator: $AvdName (adb reverse port 18080)"
Write-Host "  - Mobile App: com.example.v_sat_compass"
Write-Host "  - Web Admin Portal: http://localhost:5173"
