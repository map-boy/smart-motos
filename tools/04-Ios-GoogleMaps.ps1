# 04-Ios-GoogleMaps.ps1  -  replace Mapbox geocoding with Google Maps Platform
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


Write-Host '[04] iOS Google Maps' -ForegroundColor Cyan

Save 'ios\SmartMotos\Services\LocationSearchService.swift' @'
import Foundation

// Google Maps Platform response shapes. Snake_case keys are Google's, decoded as-is
// rather than via CodingKeys to keep the mapping obvious against their API docs.
struct GoogleLatLng: Codable {
    let lat: Double
    let lng: Double
}

struct GoogleGeometry: Codable {
    let location: GoogleLatLng?
}

struct GooglePlace: Codable {
    let name: String?
    let formatted_address: String?
    let geometry: GoogleGeometry?
}

struct GooglePlacesResponse: Codable {
    let results: [GooglePlace]?
    let status: String?
    let error_message: String?
}

struct GoogleGeocodeResult: Codable {
    let formatted_address: String?
}

struct GoogleGeocodeResponse: Codable {
    let results: [GoogleGeocodeResult]?
    let status: String?
    let error_message: String?
}

class LocationSearchService {
    static let shared = LocationSearchService()

    private let kigaliCenter = "-1.9441,30.0619"
    private let searchRadiusMeters = 20000

    private var apiKey: String {
        let key = Bundle.main.object(forInfoDictionaryKey: "GOOGLE_MAPS_API_KEY") as? String ?? ""
        // Google answers a missing key with HTTP 200 and status REQUEST_DENIED, so an
        // unset key would otherwise look identical to "no places matched".
        if key.isEmpty {
            print("LocationSearch: GOOGLE_MAPS_API_KEY is empty. Set it in the build environment before generating the project.")
        }
        return key
    }

    // Google reports failures in the body, not the HTTP status. Anything other than
    // OK or ZERO_RESULTS is a real error and must be logged, not silently swallowed.
    private func isUsable(status: String?, errorMessage: String?) -> Bool {
        switch status {
        case "OK":
            return true
        case "ZERO_RESULTS":
            return false
        default:
            print("LocationSearch: Google returned \(status ?? "no status"): \(errorMessage ?? "no message")")
            return false
        }
    }

    func searchPlaces(query: String) async -> [LocationPoint] {
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty,
              let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "https://maps.googleapis.com/maps/api/place/textsearch/json?query=\(encoded)&location=\(kigaliCenter)&radius=\(searchRadiusMeters)&region=rw&key=\(apiKey)") else {
            return []
        }

        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return [] }
            let res = try JSONDecoder().decode(GooglePlacesResponse.self, from: data)
            guard isUsable(status: res.status, errorMessage: res.error_message) else { return [] }

            return (res.results ?? []).prefix(8).compactMap { place in
                guard let loc = place.geometry?.location else { return nil }
                return LocationPoint(
                    address: place.formatted_address ?? place.name ?? "",
                    latitude: loc.lat,
                    longitude: loc.lng
                )
            }
        } catch {
            print("LocationSearch failed: \(error)")
            return []
        }
    }

    func reverseGeocode(latitude: Double, longitude: Double) async -> LocationPoint {
        let fallback = LocationPoint(address: "Current Location", latitude: latitude, longitude: longitude)
        guard let url = URL(string: "https://maps.googleapis.com/maps/api/geocode/json?latlng=\(latitude),\(longitude)&key=\(apiKey)") else {
            return fallback
        }

        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return fallback }
            let res = try JSONDecoder().decode(GoogleGeocodeResponse.self, from: data)
            guard isUsable(status: res.status, errorMessage: res.error_message),
                  let address = res.results?.first?.formatted_address else {
                return fallback
            }
            // Keep the caller's own coordinates: Google snaps to the nearest civic
            // address, which would silently move the user's actual pickup pin.
            return LocationPoint(address: address, latitude: latitude, longitude: longitude)
        } catch {
            return fallback
        }
    }

    func isWithinKigali(lat: Double, lon: Double) -> Bool {
        return lat >= -2.05 && lat <= -1.85 && lon >= 29.95 && lon <= 30.25
    }
}
'@
Save 'ios\SmartMotos\Resources\Info.plist' @'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>SmartMotos</string>
    <key>CFBundleIdentifier</key>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleExecutable</key>
    <string>$(EXECUTABLE_NAME)</string>
    <key>GOOGLE_MAPS_API_KEY</key>
    <string>$(GOOGLE_MAPS_API_KEY)</string>
    <key>UILaunchScreen</key>
    <dict/>
    <key>UIApplicationSceneManifest</key>
    <dict>
        <key>UIApplicationSupportsMultipleScenes</key>
        <false/>
    </dict>
</dict>
</plist>
'@
Save 'ios\project.yml' @'
name: SmartMotos
options:
  bundleIdPrefix: com.mapboy.smartmotos
  deploymentTarget:
    iOS: "16.0"
configs:
  Debug: debug
targets:
  SmartMotos:
    type: application
    platform: iOS
    sources:
      - path: SmartMotos
        includes:
          - "**/*.swift"
      - path: GoogleService-Info.plist
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.mapboy.smartmotos
        INFOPLIST_FILE: SmartMotos/Resources/Info.plist
        CODE_SIGNING_ALLOWED: NO
        GENERATE_INFOPLIST_FILE: NO
        GOOGLE_MAPS_API_KEY: ${GOOGLE_MAPS_API_KEY}
    dependencies:
      - package: Firebase
        products:
          - FirebaseAuth
          - FirebaseFirestore
          - FirebaseStorage
          - FirebaseFunctions
          - FirebaseMessaging
packages:
  Firebase:
    url: https://github.com/firebase/firebase-ios-sdk
    from: 10.29.0
'@

Write-Host '[04] done' -ForegroundColor Green
