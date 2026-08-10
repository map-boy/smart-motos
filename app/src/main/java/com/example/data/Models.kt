package com.example.data

enum class UserRole {
    PASSENGER, DRIVER, ADMIN
}

enum class RideStatus {
    IDLE, SEARCHING, REQUESTED, OFFERED, DRIVER_ASSIGNED, DRIVER_ARRIVED, IN_PROGRESS, PAUSED, COMPLETED, CANCELLED, NO_DRIVER_FOUND
}

data class LocationPoint(
    val address: String,
    val latitude: Double,
    val longitude: Double
)

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val photoUrl: String? = null,
    val walletBalanceRwf: Int = 24000,
    val serviceProvider: String = "MTN",
    val vehicleType: String = "Bike",
    val licenseNumber: String = "RA-88392",
    val inspectionCode: String = "",
    val verificationStatus: String = "verified", // "verified", "pending_verification", "rejected"
    val isAvailableOnline: Boolean = true,
    val driverApplicationStatus: String? = null, // null, "pending_upload", "pending", "approved", "rejected"
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class DriverInfo(
    val id: String,
    val name: String,
    val phone: String,
    val rating: Float,
    val plateNumber: String,
    val avatarUrl: String,
    val currentLocation: LocationPoint,
    val vehicleType: String = "Moto"
)

data class RideTrip(
    val id: String,
    val riderId: String,
    val offerStatus: String = "none", // "none", "offered", "accepted", "declined", "expired"
    val offerExpiresAtMs: Long = 0L,
    val offeredDriverId: String? = null,
    val orderType: String = "Passenger", // "Passenger" or "Cargo"
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val cargoDescription: String? = null,
    val riderName: String,
    val driverId: String?,
    val driverName: String?,
    val driverAvatar: String?,
    val driverRating: Float = 4.8f,
    val vehiclePlate: String = "JDM PL8S",
    val pickup: LocationPoint,
    val dropoff: LocationPoint,
    val status: RideStatus = RideStatus.REQUESTED,
    val fareRwf: Int = 1500,
    val baseFareRwf: Int = 500,
    val waitingFeeRwf: Int = 0,
    val distanceKm: Double = 5.5,
    val durationMins: Int = 15,
    val paymentMethod: String = "Mobile Money",
    val bargainAmountRwf: Int? = null,
    val ratingGiven: Int = 0,
    val ratingFeedback: String? = null,
    val timestamp: String = "7:15 AM"
)

data class TopupRequest(
    val id: String,
    val userId: String,
    val userName: String,
    val amountRwf: Int,
    val momoNumber: String,
    val status: String = "pending", // "pending", "approved", "rejected"
    val timeAgo: String = "Just now"
)

data class DriverEarningsSummary(
    val todayRwf: Int = 18500,
    val weekRwf: Int = 124000,
    val monthRwf: Int = 480000,
    val totalTrips: Int = 22,
    val hoursOnline: Int = 11,
    val acceptanceRate: Float = 95.0f,
    val rating: Float = 4.85f,
    val cancellationRate: Float = 2.0f
)

data class DemandZone(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val demandLevel: Float, // 0.0 to 1.0
    val bookingCount: Int
)

data class TripChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val time: String = ""
)

data class SupportMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val isFromUser: Boolean,
    val time: String
)


