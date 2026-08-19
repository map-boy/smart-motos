package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class SmartRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun acceptRideOffer(rideId: String, driverId: String) {
        db.collection("rides").document(rideId)
            .update("status", "ACCEPTED", "driverId", driverId)
    }

    fun declineRideOffer(rideId: String, driverId: String) {
        db.collection("rides").document(rideId)
            .update("status", "DECLINED")
    }

    fun expireRideOfferIfTimedOut(rideId: String) {
        db.collection("rides").document(rideId)
            .update("status", "EXPIRED")
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
