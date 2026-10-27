[CmdletBinding()]
param(
    [switch]$KeepDocker
)

Write-Host "Dang dung Web Admin tren port 5173..." -ForegroundColor Yellow
try {
    $conns = Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        if ($c.OwningProcess) {
            Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    }
} catch {}

$adb = Join-Path $env:LOCALAPPDATA 'Android\\Sdk\\platform-tools\\adb.exe'
if (Test-Path $adb) {
    Write-Host "Dang tat Android Emulator..." -ForegroundColor Yellow
    & $adb emu kill 2>$null
}

if (-not $KeepDocker) {
    Write-Host "Dang dung Docker containers 'vsat-api' va 'vsat-pg'..." -ForegroundColor Yellow
    docker stop vsat-api vsat-pg 2>$null
}

Write-Host "Da dung toan bo he thong." -ForegroundColor Green
