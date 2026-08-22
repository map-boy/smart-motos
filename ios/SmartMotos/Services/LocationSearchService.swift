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