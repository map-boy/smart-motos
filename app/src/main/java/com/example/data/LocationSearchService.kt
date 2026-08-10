package com.example.data

import com.example.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Real place search + reverse geocoding via Mapbox Geocoding API,
 * hard-bounded to the Kigali city bounding box so results never
 * leave the coverage area.
 */
object LocationSearchService {

    private val client = OkHttpClient()

    // Kigali bounding box: minLon,minLat,maxLon,maxLat
    private const val KIGALI_BBOX = "29.95,-2.05,30.25,-1.85"
    private const val KIGALI_PROXIMITY = "30.0619,-1.9441"

    /**
     * Forward search: user types "Kimironko" -> returns matching places,
     * restricted to the Kigali bbox only.
     */
    suspend fun searchPlaces(query: String): List<LocationPoint> {
        if (query.isBlank()) return emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encoded.json" +
            "?access_token=${BuildConfig.VITE_MAPBOX_TOKEN}" +
            "&bbox=$KIGALI_BBOX" +
            "&proximity=$KIGALI_PROXIMITY" +
            "&country=RW" +
            "&limit=8"

        return runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string() ?: return@use emptyList()
                val json = JSONObject(body)
                val features = json.optJSONArray("features") ?: return@use emptyList()
                (0 until features.length()).mapNotNull { i ->
                    val f = features.optJSONObject(i) ?: return@mapNotNull null
                    val center = f.optJSONArray("center") ?: return@mapNotNull null
                    val placeName = f.optString("place_name", f.optString("text", ""))
                    LocationPoint(
                        address = placeName,
                        latitude = center.optDouble(1),
                        longitude = center.optDouble(0)
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    /**
     * Reverse geocode: turn a raw GPS lat/lon (e.g. from FusedLocationProvider)
     * into a human-readable address for the "Use my current location" pickup.
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): LocationPoint {
        val fallback = LocationPoint("Current Location", latitude, longitude)
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$longitude,$latitude.json" +
            "?access_token=${BuildConfig.VITE_MAPBOX_TOKEN}" +
            "&types=address,place,poi"

        return runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use fallback
                val body = response.body?.string() ?: return@use fallback
                val json = JSONObject(body)
                val features = json.optJSONArray("features")
                if (features == null || features.length() == 0) return@use fallback
                val first = features.optJSONObject(0) ?: return@use fallback
                LocationPoint(
                    address = first.optString("place_name", "Current Location"),
                    latitude = latitude,
                    longitude = longitude
                )
            }
        }.getOrElse { fallback }
    }

    /** True if the point actually falls inside Kigali's bbox. */
    fun isWithinKigali(lat: Double, lon: Double): Boolean {
        return lat in -2.05..-1.85 && lon in 29.95..30.25
    }
}
