package com.nihonor.smartmotosapp.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// Pricing and distance live here rather than inside a screen so the driver and admin
// flows quote the same number as the passenger. PassengerHomeView.swift computes this
// inline; keep the constants in sync if that ever changes.
object Fare {
    const val BASE_RWF = 500
    const val PER_KM_RWF = 200

    private const val EARTH_RADIUS_KM = 6371.0

    fun distanceKm(from: LocationPoint, to: LocationPoint): Double {
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
            sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun quoteRwf(distanceKm: Double): Int =
        max(BASE_RWF + (distanceKm * PER_KM_RWF).toInt(), BASE_RWF)

    fun durationMins(distanceKm: Double): Int = (distanceKm * 3).toInt()
}
