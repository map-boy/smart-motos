# 03-Android-Manifest-Res.ps1  -  manifest, launcher icon, strings
# Part of the SmartMotos apply set. Safe to re-run: every step checks first.
$ErrorActionPreference = 'Stop'
if (-not (Test-Path .git)) { throw 'Run from the repo root.' }

function Save([string]$rel, [string]$text) {
  $full = Join-Path $PWD.Path $rel
  New-Item -ItemType Directory -Force -Path (Split-Path $full -Parent) | Out-Null
  [System.IO.File]::WriteAllText($full, $text)
  Write-Host "  wrote   $rel" -ForegroundColor DarkGreen
}

# Line endings are normalised before comparing: these files are CRLF in a Windows
# checkout but the anchors below are LF, and a raw Contains() would never match.
function Patch([string]$rel, [string]$find, [string]$replace, [string]$guard) {
  $full = Join-Path $PWD.Path $rel
  if (-not (Test-Path $full)) { Write-Host "  SKIP    $rel (not found)" -ForegroundColor Yellow; return }
  $raw  = [System.IO.File]::ReadAllText($full)
  $crlf = $raw.Contains("`r`n")
  $s    = $raw.Replace("`r`n", "`n")
  $f    = $find.Replace("`r`n", "`n")
  $r    = $replace.Replace("`r`n", "`n")
  if ($s.Contains($guard)) { Write-Host "  ok      $rel (already patched)" -ForegroundColor DarkGray; return }
  if (-not $s.Contains($f)) { throw "Anchor not found in $rel - the file differs from what this script expects." }
  $s = $s.Replace($f, $r)
  if ($crlf) { $s = $s.Replace("`n", "`r`n") }
  [System.IO.File]::WriteAllText($full, $s)
  Write-Host "  patched $rel" -ForegroundColor DarkGreen
}


Write-Host '[03] Android manifest + resources' -ForegroundColor Cyan

Save 'android\app\src\main\AndroidManifest.xml' @'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <!-- Required from API 33 on, or FCM notifications are dropped without any error. -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".data.SmartMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>

        <meta-data
            android:name="com.google.firebase.messaging.default_notification_channel_id"
            android:value="smartmotos_trips" />

    </application>

</manifest>
'@
Save 'android\app\src\main\res\values\strings.xml' @'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SmartMotos</string>
</resources>
'@
Save 'android\app\src\main\res\values\colors.xml' @'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#0F172A</color>
    <color name="brand_amber">#F59E0B</color>
</resources>
'@
Save 'android\app\src\main\res\drawable\ic_launcher_foreground.xml' @'
<?xml version="1.0" encoding="utf-8"?>
<!-- Adaptive-icon foreground: 108x108 with content kept inside the 72x72 safe zone. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#00000000"
        android:pathData="M36,68m-13,0a13,13 0 1,0 26,0a13,13 0 1,0 -26,0"
        android:strokeColor="#F59E0B"
        android:strokeWidth="5" />
    <path
        android:fillColor="#00000000"
        android:pathData="M74,68m-13,0a13,13 0 1,0 26,0a13,13 0 1,0 -26,0"
        android:strokeColor="#F59E0B"
        android:strokeWidth="5" />
    <path
        android:fillColor="#00000000"
        android:pathData="M36,68 L50,52 L68,52 L74,68"
        android:strokeColor="#F59E0B"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="5" />
    <path
        android:fillColor="#00000000"
        android:pathData="M66,52 L73,42 L83,42"
        android:strokeColor="#F59E0B"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="5" />
    <path
        android:fillColor="#F59E0B"
        android:pathData="M44,50 L62,50 L60,44 L46,44 z" />
</vector>
'@
Save 'android\app\src\main\res\drawable\ic_launcher_background.xml' @'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#0F172A"
        android:pathData="M0,0 L108,0 L108,108 L0,108 z" />
</vector>
'@
Save 'android\app\src\main\res\mipmap-anydpi-v26\ic_launcher.xml' @'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
'@
Save 'android\app\src\main\res\mipmap-anydpi-v26\ic_launcher_round.xml' @'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
'@
Save 'android\app\src\main\res\mipmap\ic_launcher.xml' @'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#0F172A"
        android:pathData="M16,4 L92,4 A12,12 0 0 1 104,16 L104,92 A12,12 0 0 1 92,104 L16,104 A12,12 0 0 1 4,92 L4,16 A12,12 0 0 1 16,4 z" />
    <path
        android:fillColor="#00000000"
        android:pathData="M36,68m-13,0a13,13 0 1,0 26,0a13,13 0 1,0 -26,0"
        android:strokeColor="#F59E0B"
        android:strokeWidth="5" />
    <path
        android:fillColor="#00000000"
        android:pathData="M74,68m-13,0a13,13 0 1,0 26,0a13,13 0 1,0 -26,0"
        android:strokeColor="#F59E0B"
        android:strokeWidth="5" />
    <path
        android:fillColor="#00000000"
        android:pathData="M36,68 L50,52 L68,52 L74,68"
        android:strokeColor="#F59E0B"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="5" />
    <path
        android:fillColor="#00000000"
        android:pathData="M66,52 L73,42 L83,42"
        android:strokeColor="#F59E0B"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="5" />
    <path
        android:fillColor="#F59E0B"
        android:pathData="M44,50 L62,50 L60,44 L46,44 z" />
</vector>
'@
Save 'android\app\src\main\res\mipmap\ic_launcher_round.xml' @'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#0F172A"
        android:pathData="M16,4 L92,4 A12,12 0 0 1 104,16 L104,92 A12,12 0 0 1 92,104 L16,104 A12,12 0 0 1 4,92 L4,16 A12,12 0 0 1 16,4 z" />
    <path
        android:fillColor="#00000000"
        android:pathData="M36,68m-13,0a13,13 0 1,0 26,0a13,13 0 1,0 -26,0"
        android:strokeColor="#F59E0B"
        android:strokeWidth="5" />
    <path
        android:fillColor="#00000000"
        android:pathData="M74,68m-13,0a13,13 0 1,0 26,0a13,13 0 1,0 -26,0"
        android:strokeColor="#F59E0B"
        android:strokeWidth="5" />
    <path
        android:fillColor="#00000000"
        android:pathData="M36,68 L50,52 L68,52 L74,68"
        android:strokeColor="#F59E0B"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="5" />
    <path
        android:fillColor="#00000000"
        android:pathData="M66,52 L73,42 L83,42"
        android:strokeColor="#F59E0B"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:strokeWidth="5" />
    <path
        android:fillColor="#F59E0B"
        android:pathData="M44,50 L62,50 L60,44 L46,44 z" />
</vector>
'@

Write-Host '[03] done' -ForegroundColor Green
