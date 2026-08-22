package com.nihonor.smartmotosapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Kotlin port of SmartRepository.swift. Field names/types must match SCHEMA.md exactly â€”
// update SCHEMA.md first if the shape ever changes, then this file, not the other way around.
object SmartRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance("africa-south1")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var userListener: ListenerRegistration? = null
    private var tripsListener: ListenerRegistration? = null
    private var nearbyDriversListener: ListenerRegistration? = null
    private var driverTripsListener: ListenerRegistration? = null
    private var topupsListener: ListenerRegistration? = null
    private var pendingDriversListener: ListenerRegistration? = null
    private var allUsersListener: ListenerRegistration? = null

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _trips = MutableStateFlow<List<RideTrip>>(emptyList())
    val trips: StateFlow<List<RideTrip>> = _trips.asStateFlow()

    private val _nearbyDrivers = MutableStateFlow<List<DriverInfo>>(emptyList())
    val nearbyDrivers: StateFlow<List<DriverInfo>> = _nearbyDrivers.asStateFlow()

    private val _pendingTopups = MutableStateFlow<List<TopupRequest>>(emptyList())
    val pendingTopups: StateFlow<List<TopupRequest>> = _pendingTopups.asStateFlow()

    private val _pendingDrivers = MutableStateFlow<List<UserProfile>>(emptyList())
    val pendingDrivers: StateFlow<List<UserProfile>> = _pendingDrivers.asStateFlow()

    private val _isDriverOnline = MutableStateFlow(false)
    val isDriverOnline: StateFlow<Boolean> = _isDriverOnline.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    // The rider's current in-flight trip, derived from the trips listener rather than
    // fetched separately, so it can never disagree with tripHistory.
    private val _activeRide = MutableStateFlow<RideTrip?>(null)
    val activeRide: StateFlow<RideTrip?> = _activeRide.asStateFlow()

    // Driver-side flows kept separate from the rider's activeRide: both listeners are
    // attached at once, and a rider listener finding nothing would otherwise null out
    // the driver's in-progress trip.
    private val _incomingDriverRequest = MutableStateFlow<RideTrip?>(null)
    val incomingDriverRequest: StateFlow<RideTrip?> = _incomingDriverRequest.asStateFlow()

    private val _driverActiveRide = MutableStateFlow<RideTrip?>(null)
    val driverActiveRide: StateFlow<RideTrip?> = _driverActiveRide.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    private val terminalStatuses = setOf(
        RideStatus.COMPLETED, RideStatus.CANCELLED,
        RideStatus.NO_DRIVER_FOUND, RideStatus.DISPATCH_ERROR
    )

    // Mirrors kigaliLocations in SmartRepository.swift.
    val kigaliLocations: List<LocationPoint> = listOf(
        LocationPoint("African Leadership University (ALU)", -1.9390, 30.1305),
        LocationPoint("Kimironko Market", -1.9515, 30.1265),
        LocationPoint("Kigali Convention Centre", -1.9546, 30.0931),
        LocationPoint("Rwanda Art Museum, Kanombe", -1.9688, 30.1558),
        LocationPoint("Nyabugogo Bus Park", -1.9385, 30.0460),
        LocationPoint("KG 18 Ave, Remera", -1.9580, 30.1120)
    )

    private fun logListenerError(context: String, error: com.google.firebase.firestore.FirebaseFirestoreException?) {
        if (error == null) return
        android.util.Log.e("SmartRepository", "[$context] listener error", error)
        _lastError.value = "$context: ${error.message}"
    }

    @Volatile private var ensureInFlight = false

    // Creates users/{uid} with role=passenger if it doesn't exist yet (must match
    // firestore.rules create condition exactly, or the write is rejected).
    suspend fun ensureUserProfile(uid: String, phone: String) {
        if (ensureInFlight) return
        ensureInFlight = true
        try {
            ensureUserProfileInner(uid, phone)
        } finally {
            ensureInFlight = false
        }
    }

    private suspend fun ensureUserProfileInner(uid: String, phone: String) {
        // Source.SERVER, not the default: an offline-cache miss must never be treated as
        // "no profile exists", or a reconnect would clobber the real doc with blank defaults.
        // If the server is unreachable this throws and we simply retry on the next snapshot.
        val doc = db.collection("users").document(uid)
            .get(com.google.firebase.firestore.Source.SERVER).await()
        if (doc.exists()) return
        val newUser = mapOf(
            "name" to "New User",
            "email" to "",
            "phone" to phone,
            "role" to "passenger",
            "walletBalanceRwf" to 0,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).set(newUser).await()
    }

    // onNewToken only fires when the token is first created or rotates. A user who
    // installs, gets a token, then signs in later would never hit that callback while
    // authenticated, so the token has to be fetched explicitly after login too.
    fun registerFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> SmartMessagingService.persistToken(token) }
            .addOnFailureListener { e ->
                android.util.Log.e("SmartRepository", "Failed to fetch FCM token", e)
            }
    }

    // Clear the token on sign-out so pushes for this account stop landing on a device
    // that is no longer signed in to it.
    fun clearFcmToken(uid: String) {
        db.collection("users").document(uid)
            .update("fcmToken", FieldValue.delete())
            .addOnFailureListener { e ->
                android.util.Log.e("SmartRepository", "Failed to clear fcmToken", e)
            }
    }

    // ---- mapping (mirrors SmartRepository.swift line-for-line) ----

    private fun mapUserProfile(id: String, data: Map<String, Any?>): UserProfile {
        @Suppress("UNCHECKED_CAST")
        val driverApp = data["driverApplication"] as? Map<String, Any?>
        val roleStr = data["role"] as? String
        val role = when (roleStr) {
            "driver" -> UserRole.DRIVER
            "admin" -> UserRole.ADMIN
            else -> UserRole.PASSENGER
        }
        return UserProfile(
            id = id,
            name = data["name"] as? String ?: "",
            email = data["email"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            role = role,
            photoUrl = data["photoUrl"] as? String,
            walletBalanceRwf = (data["walletBalanceRwf"] as? Long)?.toInt() ?: 0,
            serviceProvider = data["serviceProvider"] as? String ?: "MTN",
            vehicleType = data["vehicleType"] as? String ?: "Bike",
            licenseNumber = data["licenseNumber"] as? String ?: "",
            verificationStatus = data["verificationStatus"] as? String ?: "verified",
            isAvailableOnline = data["isAvailableOnline"] as? Boolean ?: true,
            driverApplicationStatus = driverApp?.get("status") as? String ?: "",
            inspectionCode = driverApp?.get("inspectionCode") as? String ?: "",
            latitude = data["latitude"] as? Double,
            longitude = data["longitude"] as? Double
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapLocationPoint(m: Map<String, Any?>?): LocationPoint {
        if (m == null) return LocationPoint()
        return LocationPoint(
            address = m["address"] as? String ?: "",
            latitude = (m["latitude"] as? Double) ?: 0.0,
            longitude = (m["longitude"] as? Double) ?: 0.0
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapRideTrip(id: String, d: Map<String, Any?>): RideTrip? {
        val pickupMap = d["pickup"] as? Map<String, Any?> ?: return null
        val dropoffMap = d["dropoff"] as? Map<String, Any?> ?: return null
        val statusStr = d["status"] as? String ?: "REQUESTED"
        val status = try { RideStatus.valueOf(statusStr) } catch (e: Exception) { RideStatus.REQUESTED }

        return RideTrip(
            id = id,
            riderId = d["riderId"] as? String ?: "",
            orderType = d["orderType"] as? String ?: "",
            recipientName = d["recipientName"] as? String,
            recipientPhone = d["recipientPhone"] as? String,
            cargoDescription = d["cargoDescription"] as? String,
            riderName = d["riderName"] as? String ?: "",
            driverId = d["driverId"] as? String,
            driverName = d["driverName"] as? String,
            driverAvatar = d["driverAvatar"] as? String,
            driverRating = (d["driverRating"] as? Double)?.toFloat() ?: 0f,
            vehiclePlate = d["vehiclePlate"] as? String ?: "",
            pickup = mapLocationPoint(pickupMap),
            dropoff = mapLocationPoint(dropoffMap),
            status = status,
            fareRwf = (d["fareRwf"] as? Long)?.toInt() ?: 0,
            baseFareRwf = (d["baseFareRwf"] as? Long)?.toInt() ?: 0,
            distanceKm = d["distanceKm"] as? Double ?: 0.0,
            durationMins = (d["durationMins"] as? Long)?.toInt() ?: 0,
            paymentMethod = d["paymentMethod"] as? String ?: "",
            bargainAmountRwf = (d["bargainAmountRwf"] as? Long)?.toInt(),
            ratingGiven = (d["ratingGiven"] as? Long)?.toInt() ?: 0,
            ratingFeedback = d["ratingFeedback"] as? String,
            timestamp = d["timestamp"] as? String ?: "",
            offeredDriverId = d["offeredDriverId"] as? String
        )
    }

    // ---- listeners ----

    fun attachListeners(uid: String) {
        userListener?.remove()
        userListener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            logListenerError("user profile", error)
            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data ?: return@addSnapshotListener
                val profile = mapUserProfile(uid, data)
                _currentUser.value = profile
                _isDriverOnline.value = profile.isAvailableOnline
            } else if (snapshot != null && !snapshot.exists()) {
                // Signed in with no profile doc - e.g. the post-signup write was lost to a
                // network blip. Self-heal so the app never stays stuck on a blank profile.
                // Deliberately not gated on metadata.isFromCache: on a poor connection the
                // server snapshot may never arrive, and ensureUserProfile re-checks against
                // the server itself before writing anything.
                scope.launch {
                    try {
                        ensureUserProfile(uid, auth.currentUser?.phoneNumber ?: "")
                    } catch (e: Exception) {
                        android.util.Log.e("SmartRepository", "self-heal ensureUserProfile failed", e)
                    }
                }
            }
        }

        tripsListener?.remove()
        tripsListener = db.collection("trips")
            .whereEqualTo("riderId", uid)
            .addSnapshotListener { snapshot, error ->
                logListenerError("rider trips", error)
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { mapRideTrip(doc.id, it) }
                } ?: emptyList()
                _trips.value = list
            }

        nearbyDriversListener?.remove()
        nearbyDriversListener = db.collection("users")
            .whereEqualTo("role", "driver")
            .whereEqualTo("isAvailableOnline", true)
            .addSnapshotListener { snapshot, error ->
                logListenerError("nearby drivers", error)
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val profile = mapUserProfile(doc.id, data)
                    DriverInfo(
                        id = profile.id,
                        name = profile.name,
                        phone = profile.phone,
                        rating = 0f,
                        plateNumber = "",
                        avatarUrl = profile.photoUrl ?: "",
                        currentLocation = LocationPoint(
                            latitude = profile.latitude ?: 0.0,
                            longitude = profile.longitude ?: 0.0
                        ),
                        vehicleType = profile.vehicleType
                    )
                } ?: emptyList()
                _nearbyDrivers.value = list
            }

        driverTripsListener?.remove()
        driverTripsListener = db.collection("trips")
            .whereEqualTo("driverId", uid)
            .addSnapshotListener { snapshot, error ->
                logListenerError("driver trips", error)
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { mapRideTrip(doc.id, it) }
                } ?: emptyList()
                // dispatch.ts writes driverId at offer time, not on accept, so an offered
                // trip is already visible to this query.
                _incomingDriverRequest.value = list.firstOrNull {
                    it.status == RideStatus.OFFERED && it.offeredDriverId == uid
                }
                _driverActiveRide.value = list.firstOrNull {
                    it.status == RideStatus.DRIVER_ASSIGNED ||
                        it.status == RideStatus.DRIVER_ARRIVED ||
                        it.status == RideStatus.IN_PROGRESS
                }
            }
    }

    fun detachListeners() {
        userListener?.remove(); userListener = null
        tripsListener?.remove(); tripsListener = null
        nearbyDriversListener?.remove(); nearbyDriversListener = null
        driverTripsListener?.remove(); driverTripsListener = null
        _incomingDriverRequest.value = null
        _driverActiveRide.value = null
        detachAdminListeners()
    }

    fun attachAdminListeners() {
        if (topupsListener != null || pendingDriversListener != null) return

        topupsListener = db.collection("topupRequests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                logListenerError("pending topups", error)
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    TopupRequest(
                        id = doc.id,
                        userId = d["userId"] as? String ?: "",
                        userName = d["userName"] as? String ?: "",
                        amountRwf = (d["amountRwf"] as? Long)?.toInt() ?: 0,
                        momoNumber = d["momoNumber"] as? String ?: "",
                        status = d["status"] as? String ?: "pending",
                        timeAgo = d["timeAgo"] as? String ?: ""
                    )
                } ?: emptyList()
                _pendingTopups.value = list
            }

        pendingDriversListener = db.collection("users")
            .whereEqualTo("driverApplication.status", "pending")
            .addSnapshotListener { snapshot, error ->
                logListenerError("pending drivers", error)
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { mapUserProfile(doc.id, it) }
                } ?: emptyList()
                _pendingDrivers.value = list
            }

        allUsersListener?.remove()
        allUsersListener = db.collection("users").addSnapshotListener { snapshot, error ->
            logListenerError("all users", error)
            _allUsers.value = snapshot?.documents?.mapNotNull { doc ->
                doc.data?.let { mapUserProfile(doc.id, it) }
            } ?: emptyList()
        }
    }

    fun detachAdminListeners() {
        topupsListener?.remove(); topupsListener = null
        pendingDriversListener?.remove(); pendingDriversListener = null
        allUsersListener?.remove(); allUsersListener = null
        _pendingTopups.value = emptyList()
        _pendingDrivers.value = emptyList()
        _allUsers.value = emptyList()
    }

    // ---- writes ----

    // The iOS build writes this document straight from PassengerHomeView. Keeping it in
    // the repository means the driver and admin flows read one definition of a trip's
    // shape, and firestore.rules only ever sees fields SCHEMA.md documents.
    suspend fun createRide(
        pickup: LocationPoint,
        dropoff: LocationPoint,
        orderType: String,
        paymentMethod: String,
        bargainAmountRwf: Int?
    ): Result<String> {
        val rider = _currentUser.value
        if (rider == null || rider.id.isEmpty()) {
            return Result.failure(IllegalStateException("No signed-in user profile yet"))
        }

        val distanceKm = Fare.distanceKm(pickup, dropoff)
        val data = hashMapOf<String, Any>(
            "riderId" to rider.id,
            "riderName" to rider.name,
            "orderType" to orderType,
            "pickup" to mapOf(
                "address" to pickup.address,
                "latitude" to pickup.latitude,
                "longitude" to pickup.longitude
            ),
            "dropoff" to mapOf(
                "address" to dropoff.address,
                "latitude" to dropoff.latitude,
                "longitude" to dropoff.longitude
            ),
            "fareRwf" to (bargainAmountRwf ?: Fare.quoteRwf(distanceKm)),
            "baseFareRwf" to Fare.BASE_RWF,
            "distanceKm" to distanceKm,
            "durationMins" to Fare.durationMins(distanceKm),
            "paymentMethod" to paymentMethod,
            "status" to RideStatus.REQUESTED.name,
            "ratingGiven" to 0,
            "timestamp" to "Just now",
            "createdAt" to FieldValue.serverTimestamp()
        )
        // Written only when the rider actually offered a price. A present-but-null field
        // is not the same as an absent one to either the mapper or dispatch.ts.
        if (bargainAmountRwf != null) data["bargainAmountRwf"] = bargainAmountRwf

        return try {
            val ref = db.collection("trips").add(data).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            android.util.Log.e("SmartRepository", "createRide failed", e)
            Result.failure(e)
        }
    }

    fun cancelRide(tripId: String) {
        db.collection("trips").document(tripId)
            .update("status", RideStatus.CANCELLED.name)
            .addOnFailureListener { e ->
                android.util.Log.e("SmartRepository", "cancelRide failed", e)
            }
    }

    // Accepting must clear offerStatus. dispatchTimeoutSweep queries
    // offerStatus == "offered" AND offerExpiresAtMs <= now; leaving it as "offered"
    // means the sweep expires an already-accepted ride ~20s later and re-dispatches it
    // to another driver. DriverHomeView.swift has this bug.
    fun acceptTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        val me = _currentUser.value
        db.collection("trips").document(tripId).update(
            mapOf(
                "driverId" to uid,
                "driverName" to (me?.name ?: ""),
                "vehiclePlate" to (me?.licenseNumber ?: ""),
                "status" to RideStatus.DRIVER_ASSIGNED.name,
                "offerStatus" to "accepted"
            )
        ).addOnFailureListener { e ->
            android.util.Log.e("SmartRepository", "acceptTrip failed", e)
        }
    }

    // dispatchOnDriverResponse fires on offerStatus going "offered" -> "declined", then
    // releases this driver's lock and re-offers to the next candidate. Writing status
    // back to REQUESTED instead, as DriverHomeView.swift does, triggers nothing: the
    // trip just waits for the one-minute sweep.
    fun declineTrip(tripId: String) {
        db.collection("trips").document(tripId)
            .update("offerStatus", "declined")
            .addOnFailureListener { e ->
                android.util.Log.e("SmartRepository", "declineTrip failed", e)
            }
    }

    fun updateTripStatus(tripId: String, status: RideStatus) {
        db.collection("trips").document(tripId)
            .update("status", status.name)
            .addOnFailureListener { e ->
                android.util.Log.e("SmartRepository", "updateTripStatus failed", e)
            }
    }

    fun toggleDriverOnline(online: Boolean) {
        _isDriverOnline.value = online
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("isAvailableOnline", online)
    }

    fun updateDriverLocation(latitude: Double, longitude: Double) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update(mapOf("latitude" to latitude, "longitude" to longitude))
    }

    fun updateUserProfile(name: String, email: String, phone: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update(mapOf("name" to name, "email" to email, "phone" to phone))
    }

    fun requestWalletTopup(amount: Int) {
        val user = _currentUser.value ?: return
        if (user.id.isEmpty()) return
        val newReq = mapOf(
            "userId" to user.id,
            "userName" to user.name,
            "amountRwf" to amount,
            "momoNumber" to user.phone,
            "status" to "pending",
            "timeAgo" to "just now"
        )
        db.collection("topupRequests").add(newReq)
    }

    fun approveTopup(id: String) {
        db.collection("topupRequests").document(id).get()
            .addOnSuccessListener { doc ->
                val data = doc.data ?: return@addOnSuccessListener
                val userId = data["userId"] as? String ?: return@addOnSuccessListener
                val amount = (data["amountRwf"] as? Long) ?: 0L
                db.collection("topupRequests").document(id).update("status", "approved")
                db.collection("users").document(userId)
                    .update("walletBalanceRwf", FieldValue.increment(amount))
            }
    }

    fun rejectTopup(id: String) {
        db.collection("topupRequests").document(id).update("status", "rejected")
    }

    fun approveDriver(driverId: String) {
        db.collection("users").document(driverId).update(
            mapOf(
                "role" to "driver",
                "driverApplication.status" to "approved"
            )
        )
    }

    fun rejectDriver(driverId: String) {
        db.collection("users").document(driverId)
            .update("driverApplication.status", "rejected")
    }

    // Matches the callable adminCreateUser function in functions/src/index.ts.
    suspend fun addManualUser(name: String, email: String, phone: String, role: UserRole): Result<Unit> {
        return try {
            functions.getHttpsCallable("adminCreateUser").call(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "role" to role.name.lowercase()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

