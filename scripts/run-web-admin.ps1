[CmdletBinding()]
param()

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location (Join-Path $root "web-admin")
Write-Host "Dang khoi chay Web Admin V-SAT Compass..." -ForegroundColor Cyan
npm run dev -- --host --port 5173
