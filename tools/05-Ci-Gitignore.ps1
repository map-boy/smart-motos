# 05-Ci-Gitignore.ps1  -  CI paths for the new layout, gitignore
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


Write-Host '[05] CI + gitignore' -ForegroundColor Cyan

Save '.github\workflows\ci.yml' @'
name: CI
on:
  push:
    branches: ['**']
  pull_request:
    branches: [main]
jobs:
  build-ios:
    name: Build iOS App
    runs-on: macos-latest
    defaults:
      run:
        working-directory: ios
    steps:
      - uses: actions/checkout@v4
      - uses: maxim-lobanov/setup-xcode@v1
        with:
          xcode-version: 'latest-stable'
      - name: Install XcodeGen
        run: brew install xcodegen
      - name: Restore GoogleService-Info.plist
        run: echo "${{ secrets.GOOGLE_SERVICE_INFO_PLIST_BASE64 }}" | base64 --decode > GoogleService-Info.plist
      - name: Generate Xcode Project
        # XcodeGen expands ${GOOGLE_MAPS_API_KEY} in project.yml at generate time,
        # which is what puts the key into Info.plist. Without it, place search 401s.
        env:
          GOOGLE_MAPS_API_KEY: ${{ secrets.GOOGLE_MAPS_API_KEY }}
        run: xcodegen generate
      - name: Resolve Package Dependencies
        run: xcodebuild -resolvePackageDependencies -project SmartMotos.xcodeproj -scheme SmartMotos
      - name: Build for Simulator
        run: |
          xcodebuild -project SmartMotos.xcodeproj \
            -scheme SmartMotos \
            -sdk iphonesimulator \
            -destination 'generic/platform=iOS Simulator' \
            CODE_SIGNING_ALLOWED=NO \
            build

  build-android:
    name: Build Android App
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: android
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Restore google-services.json
        run: printf '%s' "${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}" | base64 --decode > app/google-services.json
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      - name: Build Debug APK
        run: ./gradlew assembleDebug --stacktrace -Dorg.gradle.jvmargs="-Xmx6g -XX:+UseParallelGC"

  build-functions:
    name: Typecheck Cloud Functions
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: functions
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - name: Install
        run: npm ci
      - name: Build
        run: npm run build
'@
Save '.github\workflows\build-and-release.yml' @'
name: Build and Release
on:
  push:
    tags:
      - 'v*'
jobs:
  build-ios:
    name: Build iOS App
    runs-on: macos-latest
    defaults:
      run:
        working-directory: ios
    steps:
      - uses: actions/checkout@v4
      - uses: maxim-lobanov/setup-xcode@v1
        with:
          xcode-version: 'latest-stable'
      - name: Install XcodeGen
        run: brew install xcodegen
      - name: Restore GoogleService-Info.plist
        run: echo "${{ secrets.GOOGLE_SERVICE_INFO_PLIST_BASE64 }}" | base64 --decode > GoogleService-Info.plist
      - name: Generate Xcode Project
        env:
          GOOGLE_MAPS_API_KEY: ${{ secrets.GOOGLE_MAPS_API_KEY }}
        run: xcodegen generate
      - name: Resolve Package Dependencies
        run: xcodebuild -resolvePackageDependencies -project SmartMotos.xcodeproj -scheme SmartMotos
      - name: Build for Simulator
        run: |
          xcodebuild -project SmartMotos.xcodeproj \
            -scheme SmartMotos \
            -sdk iphonesimulator \
            -destination 'generic/platform=iOS Simulator' \
            -derivedDataPath build \
            CODE_SIGNING_ALLOWED=NO \
            build
      - name: Zip .app for Appetize
        run: |
          cd build/Build/Products/Debug-iphonesimulator
          zip -r SmartMotos-simulator.zip SmartMotos.app
      - name: Upload iOS Build Artifact
        uses: actions/upload-artifact@v4
        with:
          name: ios-release-build
          path: ios/build/Build/Products/Debug-iphonesimulator/SmartMotos-simulator.zip

  build-android:
    name: Build Android App
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: android
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Restore google-services.json
        run: printf '%s' "${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}" | base64 --decode > app/google-services.json
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      - name: Build Debug APK
        run: ./gradlew assembleDebug --stacktrace -Dorg.gradle.jvmargs="-Xmx6g -XX:+UseParallelGC"
      - name: Upload Android Build Artifact
        uses: actions/upload-artifact@v4
        with:
          name: android-release-build
          path: android/app/build/outputs/apk/debug/app-debug.apk

  create-release:
    name: Create GitHub Release
    needs: [build-ios, build-android]
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - name: Download iOS Artifact
        uses: actions/download-artifact@v4
        with:
          name: ios-release-build
          path: ./release-assets
      - name: Download Android Artifact
        uses: actions/download-artifact@v4
        with:
          name: android-release-build
          path: ./release-assets
      - name: Publish GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: |
            ./release-assets/SmartMotos-simulator.zip
            ./release-assets/app-debug.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
'@
Save '.gitignore' @'
# Secrets and generated Firebase config
GoogleService-Info.plist
android/app/google-services.json
.env
*.keystore
*.jks

# Android / Gradle
android/local.properties
android/.gradle/
android/build/
android/app/build/
.gradle/
.kotlin/
*.apk
*.aab

# iOS
ios/*.xcodeproj/
ios/build/
ios/DerivedData/

# Cloud Functions
functions/lib/
functions/node_modules/

# Scratch output
_archive/
*.log
build_log.txt
app_src_listing.txt
kt_files_context.txt
stacktrace_output.txt
origin_version.swift
'@

Write-Host '[05] done' -ForegroundColor Green
