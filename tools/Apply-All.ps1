# Apply-All.ps1  -  runs every step in order, stopping at the first failure.
# Run one step alone to debug it:   .\tools\02-Android-Fcm.ps1
$ErrorActionPreference = 'Stop'
if (-not (Test-Path .git)) { throw 'Run from the repo root.' }

$steps = @(
  '01-Restructure.ps1',
  '02-Android-Fcm.ps1',
  '03-Android-Manifest-Res.ps1',
  '04-Ios-GoogleMaps.ps1',
  '05-Ci-Gitignore.ps1'
)

foreach ($s in $steps) {
  $path = Join-Path $PSScriptRoot $s
  if (-not (Test-Path $path)) { Write-Host "MISSING  $s" -ForegroundColor Red; continue }
  try {
    & $path
  } catch {
    Write-Host "`nFAILED at $s" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Fix it, then re-run just that step: .\tools\$s" -ForegroundColor Yellow
    exit 1
  }
}

Write-Host "`n--- SOURCE ---" -ForegroundColor Cyan
Get-ChildItem -Recurse -File -Include *.kt,*.swift,*.ts |
  Where-Object FullName -notmatch '\\(build|node_modules|\.gradle|_archive)\\' |
  ForEach-Object { $_.FullName.Replace($PWD.Path + '\','') } | Sort-Object

Write-Host "`n--- GIT ---" -ForegroundColor Cyan
git status --short
Write-Host "`nAll steps applied. Nothing was pushed." -ForegroundColor Green
