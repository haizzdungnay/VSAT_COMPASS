$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) {
    throw "adb not found at $adb"
}

& $adb reverse tcp:18080 tcp:18080
& $adb shell monkey -p com.example.v_sat_compass -c android.intent.category.LAUNCHER 1
