import Foundation

struct MapboxFeature: Codable {
    let place_name: String?
    let text: String?
    let center: [Double]?
}

struct MapboxResponse: Codable {
    let features: [MapboxFeature]?
}

class LocationSearchService {
    static let shared = LocationSearchService()
    
    private let kigaliBbox = "29.95,-2.05,30.25,-1.85"
    private let kigaliProximity = "30.0619,-1.9441"
    private var mapboxToken: String {
        Bundle.main.object(forInfoDictionaryKey: "MAPBOX_TOKEN") as? String ?? ""
    }

    func searchPlaces(query: String) async -> [LocationPoint] {
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty,
              let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "https://api.mapbox.com/geocoding/v5/mapbox.places/\(encoded).json?access_token=\(mapboxToken)&bbox=\(kigaliBbox)&proximity=\(kigaliProximity)&country=RW&limit=8") else {
            return []
        }

        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return [] }
            let res = try JSONDecoder().decode(MapboxResponse.self, from: data)
            return res.features?.compactMap { f in
                guard let center = f.center, center.count >= 2 else { return nil }
                return LocationPoint(
                    address: f.place_name ?? f.text ?? "",
                    latitude: center[1],
                    longitude: center[0]
                )
            } ?? []
        } catch {
            print("LocationSearch failed: \(error)")
            return []
        }
    }

    func reverseGeocode(latitude: Double, longitude: Double) async -> LocationPoint {
        let fallback = LocationPoint(address: "Current Location", latitude: latitude, longitude: longitude)
        guard let url = URL(string: "https://api.mapbox.com/geocoding/v5/mapbox.places/\(longitude),\(latitude).json?access_token=\(mapboxToken)&types=address,place,poi") else {
            return fallback
        }

        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return fallback }
            let res = try JSONDecoder().decode(MapboxResponse.self, from: data)
            if let first = res.features?.first {
                return LocationPoint(address: first.place_name ?? "Current Location", latitude: latitude, longitude: longitude)
            }
            return fallback
        } catch {
            return fallback
        }
    }

    func isWithinKigali(lat: Double, lon: Double) -> Bool {
        return lat >= -2.05 && lat <= -1.85 && lon >= 29.95 && lon <= 30.25
    }
}
