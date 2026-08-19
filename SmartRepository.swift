import Foundation
import FirebaseAuth
import FirebaseFirestore
import FirebaseStorage
import FirebaseFunctions
import Combine

class SmartRepository: ObservableObject {
    static let shared = SmartRepository()

    private let auth = Auth.auth()
    private let db = Firestore.firestore()
    private let functions = Functions.functions(region: "africa-south1")

    private var userListener: ListenerRegistration?
    private var tripsListener: ListenerRegistration?
    private var topupsListener: ListenerRegistration?
    private var pendingDriversListener: ListenerRegistration?
    private var nearbyDriversListener: ListenerRegistration?
    private var driverTripsListener: ListenerRegistration?
    private var allUsersListener: ListenerRegistration?

    let kigaliLocations: [LocationPoint] = [
        LocationPoint(address: "African Leadership University (ALU)", latitude: -1.9390, longitude: 30.1305),
        LocationPoint(address: "Kimironko Market", latitude: -1.9515, longitude: 30.1265),
        LocationPoint(address: "Kigali Convention Centre", latitude: -1.9546, longitude: 30.0931),
        LocationPoint(address: "Rwanda Art Museum, Kanombe", latitude: -1.9688, longitude: 30.1558),
        LocationPoint(address: "Nyabugogo Bus Park", latitude: -1.9385, longitude: 30.0460),
        LocationPoint(address: "KG 18 Ave, Remera", latitude: -1.9580, longitude: 30.1120)
    ]

    @Published var currentUser: UserProfile = UserProfile(id: "", name: "", email: "", phone: "", role: .passenger)
    @Published var currentRole: UserRole = .passenger
    @Published var isAuthenticated: Bool = false
    @Published var tripHistory: [RideTrip] = []
    @Published var activeRide: RideTrip? = nil
    @Published var nearbyDrivers: [DriverInfo] = []
    @Published var incomingDriverRequest: RideTrip? = nil
    @Published var isDriverOnline: Bool = true
    @Published var pendingTopups: [TopupRequest] = []
    @Published var pendingDrivers: [UserProfile] = []
    @Published var allUsers: [UserProfile] = []
    @Published var supportMessages: [SupportMessage] = [
        SupportMessage(id: "msg-1", sender: "Support Bot", text: "Hi there! Welcome to Smart Motos support. How can we help you today?", isUser: false, timeAgo: "09:00 AM")
    ]

    private init() {
        self.isAuthenticated = auth.currentUser != nil
        auth.addStateDidChangeListener { [weak self] _, user in
            guard let self = self else { return }
            self.isAuthenticated = user != nil
            if let uid = user?.uid {
                self.attachListeners(uid: uid)
            } else {
                self.detachListeners()
                self.currentUser = UserProfile(id: "", name: "", email: "", phone: "", role: .passenger)
                self.tripHistory = []
                self.pendingTopups = []
                self.pendingDrivers = []
                self.allUsers = []
            }
        }
    }

    private func attachListeners(uid: String) {
        userListener?.remove()
        userListener = db.collection("users").document(uid).addSnapshotListener { [weak self] snapshot, _ in
            guard let self = self, let snapshot = snapshot, snapshot.exists, let data = snapshot.data() else { return }
            let profile = self.mapUserProfile(id: uid, data: data)
            self.currentUser = profile
            self.currentRole = profile.role

            if profile.role == .admin {
                self.attachAdminListeners()
            } else {
                self.detachAdminListeners()
            }
        }

        tripsListener?.remove()
        tripsListener = db.collection("trips")
            .whereField("riderId", isEqualTo: uid)
            .order(by: "createdAt", descending: true)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let self = self, let documents = snapshot?.documents else { return }
                self.tripHistory = documents.compactMap { doc in self.mapRideTrip(id: doc.documentID, d: doc.data()) }
            }

        nearbyDriversListener?.remove()
        nearbyDriversListener = db.collection("users")
            .whereField("role", isEqualTo: "driver")
            .whereField("isAvailableOnline", isEqualTo: true)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let self = self, let documents = snapshot?.documents else { return }
                self.nearbyDrivers = documents.compactMap { doc in
                    let data = doc.data()
                    guard let lat = data["latitude"] as? Double, let lon = data["longitude"] as? Double else { return nil }
                    return DriverInfo(
                        id: doc.documentID,
                        name: data["name"] as? String ?? "",
                        phone: data["phone"] as? String ?? "",
                        rating: 4.8,
                        plateNumber: data["licenseNumber"] as? String ?? "",
                        avatarUrl: data["photoUrl"] as? String ?? "",
                        currentLocation: LocationPoint(address: "", latitude: lat, longitude: lon),
                        vehicleType: data["vehicleType"] as? String ?? "Moto"
                    )
                }
            }

        driverTripsListener?.remove()
        driverTripsListener = db.collection("trips")
            .whereField("driverId", isEqualTo: uid)
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let self = self, let documents = snapshot?.documents else { return }
                let trips = documents.compactMap { doc in self.mapRideTrip(id: doc.documentID, d: doc.data()) }
                self.incomingDriverRequest = trips.first(where: { $0.status == .offered && $0.offeredDriverId == uid })
                self.activeRide = trips.first(where: { $0.status == .driverAssigned || $0.status == .driverArrived || $0.status == .inProgress })
            }
    }

    private func attachAdminListeners() {
        if topupsListener != nil || pendingDriversListener != nil { return }

        topupsListener = db.collection("topupRequests")
            .whereField("status", isEqualTo: "pending")
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let self = self, let documents = snapshot?.documents else { return }
                self.pendingTopups = documents.compactMap { doc in
                    let d = doc.data()
                    return TopupRequest(
                        id: doc.documentID,
                        userId: d["userId"] as? String ?? "",
                        userName: d["userName"] as? String ?? "",
                        amountRwf: d["amountRwf"] as? Int ?? 0,
                        momoNumber: d["momoNumber"] as? String ?? "",
                        status: d["status"] as? String ?? "pending",
                        timeAgo: "Recently"
                    )
                }
            }

        allUsersListener?.remove()
        allUsersListener = db.collection("users").addSnapshotListener { [weak self] snapshot, _ in
            guard let self = self, let documents = snapshot?.documents else { return }
            self.allUsers = documents.map { doc in self.mapUserProfile(id: doc.documentID, data: doc.data()) }
        }

        pendingDriversListener = db.collection("users")
            .whereField("driverApplication.status", isEqualTo: "pending")
            .addSnapshotListener { [weak self] snapshot, _ in
                guard let self = self, let documents = snapshot?.documents else { return }
                self.pendingDrivers = documents.compactMap { doc in
                    let d = doc.data()
                    let app = d["driverApplication"] as? [String: Any] ?? [:]
                    return UserProfile(
                        id: doc.documentID,
                        name: d["name"] as? String ?? "",
                        email: d["email"] as? String ?? "",
                        phone: d["phone"] as? String ?? "",
                        role: .driver,
                        serviceProvider: app["serviceProvider"] as? String ?? "MTN",
                        vehicleType: app["vehicleType"] as? String ?? "Bike",
                        licenseNumber: app["licenseNumber"] as? String ?? "",
                        inspectionCode: app["inspectionCode"] as? String ?? "",
                        verificationStatus: "pending_verification"
                    )
                }
            }
    }

    private func detachAdminListeners() {
        topupsListener?.remove(); topupsListener = nil
        pendingDriversListener?.remove(); pendingDriversListener = nil
        allUsersListener?.remove(); allUsersListener = nil
        pendingTopups = []
        pendingDrivers = []
    }

    private func detachListeners() {
        userListener?.remove(); userListener = nil
        tripsListener?.remove(); tripsListener = nil
        nearbyDriversListener?.remove(); nearbyDriversListener = nil
        driverTripsListener?.remove(); driverTripsListener = nil
        detachAdminListeners()
    }

    private func mapUserProfile(id: String, data: [String: Any]) -> UserProfile {
        let driverApp = data["driverApplication"] as? [String: Any]
        let roleStr = data["role"] as? String
        let role: UserRole = roleStr == "driver" ? .driver : (roleStr == "admin" ? .admin : .passenger)
        return UserProfile(
            id: id,
            name: data["name"] as? String ?? "",
            email: data["email"] as? String ?? "",
            phone: data["phone"] as? String ?? "",
            role: role,
            photoUrl: data["photoUrl"] as? String,
            walletBalanceRwf: data["walletBalanceRwf"] as? Int ?? 0,
            serviceProvider: data["serviceProvider"] as? String ?? "MTN",
            vehicleType: data["vehicleType"] as? String ?? "Bike",
            licenseNumber: data["licenseNumber"] as? String ?? "",
            inspectionCode: driverApp?["inspectionCode"] as? String ?? "",
            verificationStatus: data["verificationStatus"] as? String ?? "verified",
            isAvailableOnline: data["isAvailableOnline"] as? Bool ?? true,
            driverApplicationStatus: driverApp?["status"] as? String,
            latitude: data["latitude"] as? Double,
            longitude: data["longitude"] as? Double
        )
    }

    private func mapRideTrip(id: String, d: [String: Any]) -> RideTrip? {
        guard let pickupMap = d["pickup"] as? [String: Any],
              let dropoffMap = d["dropoff"] as? [String: Any] else { return nil }
        
        let statusStr = d["status"] as? String ?? "REQUESTED"
        let status = RideStatus(rawValue: statusStr) ?? .requested

        return RideTrip(
            id: id,
            riderId: d["riderId"] as? String ?? "",
            orderType: d["orderType"] as? String ?? "Passenger",
            recipientName: d["recipientName"] as? String,
            recipientPhone: d["recipientPhone"] as? String,
            cargoDescription: d["cargoDescription"] as? String,
            riderName: d["riderName"] as? String ?? "",
            driverId: d["driverId"] as? String,
            driverName: d["driverName"] as? String,
            driverAvatar: d["driverAvatar"] as? String,
            driverRating: Float(d["driverRating"] as? Double ?? 4.8),
            vehiclePlate: d["vehiclePlate"] as? String ?? "",
            pickup: LocationPoint(
                address: pickupMap["address"] as? String ?? "",
                latitude: pickupMap["latitude"] as? Double ?? 0.0,
                longitude: pickupMap["longitude"] as? Double ?? 0.0
            ),
            dropoff: LocationPoint(
                address: dropoffMap["address"] as? String ?? "",
                latitude: dropoffMap["latitude"] as? Double ?? 0.0,
                longitude: dropoffMap["longitude"] as? Double ?? 0.0
            ),
            status: status,
            fareRwf: d["fareRwf"] as? Int ?? 0,
            baseFareRwf: d["baseFareRwf"] as? Int ?? 0,
            distanceKm: d["distanceKm"] as? Double ?? 0.0,
            durationMins: d["durationMins"] as? Int ?? 0,
            paymentMethod: d["paymentMethod"] as? String ?? "",
            bargainAmountRwf: d["bargainAmountRwf"] as? Int,
            ratingGiven: d["ratingGiven"] as? Int ?? 0,
            ratingFeedback: d["ratingFeedback"] as? String,
            timestamp: d["timestampLabel"] as? String ?? "",
            offeredDriverId: d["offeredDriverId"] as? String
        )
    }

    func setRole(_ role: UserRole) {
        currentRole = role
    }

    func toggleDriverOnline(_ online: Bool) {
        isDriverOnline = online
        guard let uid = auth.currentUser?.uid else { return }
        db.collection("users").document(uid).updateData(["isAvailableOnline": online])
    }

    func updateDriverLocation(latitude: Double, longitude: Double) {
        guard let uid = auth.currentUser?.uid else { return }
        db.collection("users").document(uid).updateData(["latitude": latitude, "longitude": longitude])
    }

    func updateUserProfile(name: String, email: String, phone: String) {
        guard let uid = auth.currentUser?.uid else { return }
        db.collection("users").document(uid).updateData(["name": name, "email": email, "phone": phone])
    }

    func requestWalletTopup(amount: Int) {
        guard !currentUser.id.isEmpty else { return }
        let newReq: [String: Any] = [
            "userId": currentUser.id,
            "userName": currentUser.name,
            "amountRwf": amount,
            "momoNumber": "0790246983",
            "status": "pending",
            "createdAt": FieldValue.serverTimestamp()
        ]
        db.collection("topupRequests").addDocument(data: newReq)
    }

    func approveTopup(id: String) {
        db.collection("topupRequests").document(id).getDocument { [weak self] doc, _ in
            guard let doc = doc, let data = doc.data(), let userId = data["userId"] as? String else { return }
            let amount = data["amountRwf"] as? Int ?? 0
            self?.db.collection("topupRequests").document(id).updateData(["status": "approved"])
            self?.db.collection("users").document(userId).updateData(["walletBalanceRwf": FieldValue.increment(Int64(amount))])
        }
    }

    func rejectTopup(id: String) {
        db.collection("topupRequests").document(id).updateData(["status": "rejected"])
    }

    func approveDriver(driverId: String) {
        db.collection("users").document(driverId).updateData([
            "role": "driver",
            "driverApplication.status": "approved"
        ])
    }

    func rejectDriver(driverId: String) {
        db.collection("users").document(driverId).updateData(["driverApplication.status": "rejected"])
    }
    func addManualUser(name: String, email: String, phone: String, role: UserRole, completion: @escaping (Bool, String?, String?) -> Void) {
        functions.httpsCallable("createUserByAdmin").call([
            "name": name,
            "email": email,
            "phone": phone,
            "role": role.rawValue
        ]) { result, error in
            if let error = error {
                completion(false, nil, error.localizedDescription)
                return
            }
            let data = result?.data as? [String: Any]
            let resetLink = data?["resetLink"] as? String
            completion(true, resetLink, nil)
        }
    }
}

