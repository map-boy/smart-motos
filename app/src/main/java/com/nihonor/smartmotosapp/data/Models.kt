package com.nihonor.smartmotosapp.data

enum class UserRole { PASSENGER, DRIVER, ADMIN }

enum class RideStatus {
    REQUESTED, OFFERED, DRIVER_ASSIGNED, DRIVER_ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED
}

data class LocationPoint(
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

// Firestore doc = users/{uid}. driverApplication is stored nested ({status, inspectionCode})
// and flattened here on read, matching SmartRepository.swift's mapUserProfile exactly.
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.PASSENGER,
    val photoUrl: String? = null,
    val walletBalanceRwf: Int = 0,
    val serviceProvider: String = "MTN",
    val vehicleType: String = "Bike",
    val licenseNumber: String = "",
    val verificationStatus: String = "verified",
    val isAvailableOnline: Boolean = true,
    val driverApplicationStatus: String = "",
    val inspectionCode: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class RideTrip(
    val id: String = "",
    val riderId: String = "",
    val orderType: String = "",
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val cargoDescription: String? = null,
    val riderName: String = "",
    val driverId: String? = null,
    val driverName: String? = null,
    val driverAvatar: String? = null,
    val driverRating: Float = 0f,
    val vehiclePlate: String = "",
    val pickup: LocationPoint = LocationPoint(),
    val dropoff: LocationPoint = LocationPoint(),
    val status: RideStatus = RideStatus.REQUESTED,
    val fareRwf: Int = 0,
    val baseFareRwf: Int = 0,
    val distanceKm: Double = 0.0,
    val durationMins: Int = 0,
    val paymentMethod: String = "",
    val bargainAmountRwf: Int? = null,
    val ratingGiven: Int = 0,
    val ratingFeedback: String? = null,
    val timestamp: String = "",
    val offeredDriverId: String? = null
)

// Not its own collection: written to topupRequests, matches SCHEMA.md exactly.
data class TopupRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amountRwf: Int = 0,
    val momoNumber: String = "",
    val status: String = "pending",
    val timeAgo: String = ""
)

// Denormalized read-model projected from users where role == "driver" — not its own collection.
data class DriverInfo(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val rating: Float = 0f,
    val plateNumber: String = "",
    val avatarUrl: String = "",
    val currentLocation: LocationPoint = LocationPoint(),
    val vehicleType: String = ""
)

