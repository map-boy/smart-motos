import Foundation

enum UserRole: String, Codable {
    case passenger = "passenger"
    case driver = "driver"
    case admin = "admin"
}

enum RideStatus: String, Codable {
    case requested = "REQUESTED"
    case searching = "SEARCHING"
    case offered = "OFFERED"
    case driverAssigned = "DRIVER_ASSIGNED"
    case driverArrived = "DRIVER_ARRIVED"
    case inProgress = "IN_PROGRESS"
    case completed = "COMPLETED"
    case cancelled = "CANCELLED"
    case noDriverFound = "NO_DRIVER_FOUND"
    case dispatchError = "DISPATCH_ERROR"
}

struct LocationPoint: Codable, Equatable {
    var address: String
    var latitude: Double
    var longitude: Double
}

struct UserProfile: Identifiable, Codable {
    var id: String
    var name: String
    var email: String
    var phone: String
    var role: UserRole
    var photoUrl: String?
    var walletBalanceRwf: Int = 0
    var serviceProvider: String = "MTN"
    var vehicleType: String = "Bike"
    var licenseNumber: String = ""
    var inspectionCode: String = ""
    var verificationStatus: String = "verified"
    var isAvailableOnline: Bool = true
    var driverApplicationStatus: String?
    var latitude: Double?
    var longitude: Double?
}

struct DriverInfo: Identifiable, Codable {
    var id: String
    var name: String
    var phone: String
    var rating: Float
    var plateNumber: String
    var avatarUrl: String
    var currentLocation: LocationPoint
    var vehicleType: String
}

struct RideTrip: Identifiable, Codable {
    var id: String
    var riderId: String
    var orderType: String
    var recipientName: String?
    var recipientPhone: String?
    var cargoDescription: String?
    var riderName: String
    var driverId: String?
    var driverName: String?
    var driverAvatar: String?
    var driverRating: Float
    var vehiclePlate: String
    var pickup: LocationPoint
    var dropoff: LocationPoint
    var status: RideStatus
    var fareRwf: Int
    var baseFareRwf: Int
    var distanceKm: Double
    var durationMins: Int
    var paymentMethod: String
    var bargainAmountRwf: Int?
    var ratingGiven: Int
    var ratingFeedback: String?
    var timestamp: String
    var offeredDriverId: String?
}

struct TopupRequest: Identifiable, Codable {
    var id: String
    var userId: String
    var userName: String
    var amountRwf: Int
    var momoNumber: String
    var status: String
    var timeAgo: String
}

struct SupportMessage: Identifiable, Codable {
    var id: String
    var sender: String
    var text: String
    var isUser: Bool
    var timeAgo: String
}
