# 01-Restructure.ps1  -  move Android and iOS into their own folders
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


Write-Host '[01] restructure' -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path android, ios | Out-Null

foreach ($f in 'app','gradle','gradlew','gradlew.bat','gradle.properties','settings.gradle.kts','build.gradle.kts') {
  if (Test-Path $f) { git mv $f android/ ; Write-Host "  moved   $f -> android\" -ForegroundColor DarkGray }
  else { Write-Host "  ok      $f (already moved)" -ForegroundColor DarkGray }
}
foreach ($f in 'SmartMotos','project.yml') {
  if (Test-Path $f) { git mv $f ios/ ; Write-Host "  moved   $f -> ios\" -ForegroundColor DarkGray }
  else { Write-Host "  ok      $f (already moved)" -ForegroundColor DarkGray }
}

# Committed debug dumps - login_capture.txt alone is 2.65 MB.
foreach ($f in 'login_capture.txt','driver_login_logcat.txt') {
  if (Test-Path $f) { git rm -q --cached $f; Remove-Item $f -Force; Write-Host "  removed $f" -ForegroundColor DarkGray }
}
Write-Host '[01] done' -ForegroundColor Green
