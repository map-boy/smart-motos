import SwiftUI
import CoreLocation
import FirebaseFirestore

struct PassengerHomeView: View {
    @ObservedObject var repository = SmartRepository.shared
    
    @State private var pickupAddress = "African Leadership University (ALU)"
    @State private var dropoffAddress = "Kigali Convention Centre"
    @State private var pickupLat = -1.9390
    @State private var pickupLon = 30.1305
    @State private var dropoffLat = -1.9546
    @State private var dropoffLon = 30.0931
    
    @State private var paymentMethod = "MTN Mobile Money"
    @State private var bargainAmountText = ""
    @State private var orderType = "Passenger"
    @State private var isRequesting = false

    private var estimatedDistanceKm: Double {
        let r = 6371.0
        let dLat = (dropoffLat - pickupLat) * .pi / 180.0
        let dLon = (dropoffLon - pickupLon) * .pi / 180.0
        let a = sin(dLat / 2) * sin(dLat / 2) +
                cos(pickupLat * .pi / 180.0) * cos(dropoffLat * .pi / 180.0) *
                sin(dLon / 2) * sin(dLon / 2)
        let c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private var calculatedFareRwf: Int {
        let baseFare = 500
        let cost = baseFare + Int(estimatedDistanceKm * 200)
        return max(cost, 500)
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    
                    // Wallet Balance Summary Header
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Welcome back,")
                                .font(.caption)
                                .foregroundColor(.gray)
                            Text(repository.currentUser.name.isEmpty ? "Passenger" : repository.currentUser.name)
                                .font(.title3)
                                .fontWeight(.bold)
                        }
                        Spacer()
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("Wallet Balance")
                                .font(.caption)
                                .foregroundColor(.gray)
                            Text("\(repository.currentUser.walletBalanceRwf) RWF")
                                .font(.headline)
                                .foregroundColor(.green)
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)

                    // Ride / Cargo Segmented Switch
                    Picker("Order Type", selection: $orderType) {
                        Text("Ride Passenger").tag("Passenger")
                        Text("Send Package").tag("Cargo")
                    }
                    .pickerStyle(SegmentedPickerStyle())

                    // Location Selection Card
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Trip Route")
                            .font(.headline)

                        VStack(spacing: 10) {
                            HStack {
                                Image(systemName: "circle.fill")
                                    .foregroundColor(.green)
                                    .font(.caption)
                                TextField("Pickup location", text: $pickupAddress)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())
                            }

                            HStack {
                                Image(systemName: "square.fill")
                                    .foregroundColor(.red)
                                    .font(.caption)
                                TextField("Drop-off location", text: $dropoffAddress)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())
                            }
                        }

                        Divider()

                        Text("Quick Kigali Locations")
                            .font(.subheadline)
                            .foregroundColor(.gray)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(repository.kigaliLocations, id: \.address) { loc in
                                    Button(action: {
                                        dropoffAddress = loc.address
                                        dropoffLat = loc.latitude
                                        dropoffLon = loc.longitude
                                    }) {
                                        Text(loc.address)
                                            .font(.caption)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(Color.green.opacity(0.15))
                                            .foregroundColor(.green)
                                            .cornerRadius(16)
                                    }
                                }
                            }
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)

                    // Fare & Payment Card
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Fare & Payment")
                            .font(.headline)

                        HStack {
                            Text("Estimated Distance:")
                            Spacer()
                            Text(String(format: "%.1f km", estimatedDistanceKm))
                                .fontWeight(.semibold)
                        }

                        HStack {
                            Text("Standard Fare:")
                            Spacer()
                            Text("\(calculatedFareRwf) RWF")
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundColor(.green)
                        }

                        HStack {
                            Text("Offer Custom Fare (Optional):")
                                .font(.caption)
                            Spacer()
                            TextField("e.g. 1500", text: $bargainAmountText)
                                .keyboardType(.numberPad)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                                .frame(width: 100)
                        }

                        Picker("Payment Method", selection: $paymentMethod) {
                            Text("MTN Mobile Money").tag("MTN Mobile Money")
                            Text("Airtel Money").tag("Airtel Money")
                            Text("Wallet Cash").tag("Wallet")
                        }
                        .pickerStyle(SegmentedPickerStyle())
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)

                    // Request Button
                    Button(action: createRide) {
                        HStack {
                            Spacer()
                            if isRequesting {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Image(systemName: "moped.fill")
                                Text("Request Moto Now")
                                    .fontWeight(.bold)
                            }
                            Spacer()
                        }
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    .disabled(isRequesting)

                    // Nearby Active Drivers List
                    if !repository.nearbyDrivers.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Active Drivers Nearby (\(repository.nearbyDrivers.count))")
                                .font(.headline)

                            ForEach(repository.nearbyDrivers) { driver in
                                HStack {
                                    Image(systemName: "person.circle.fill")
                                        .resizable()
                                        .frame(width: 36, height: 36)
                                        .foregroundColor(.gray)

                                    VStack(alignment: .leading) {
                                        Text(driver.name)
                                            .font(.subheadline)
                                            .fontWeight(.semibold)
                                        Text(driver.plateNumber)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }

                                    Spacer()

                                    HStack(spacing: 2) {
                                        Image(systemName: "star.fill")
                                            .foregroundColor(.yellow)
                                            .font(.caption)
                                        Text(String(format: "%.1f", driver.rating))
                                            .font(.caption)
                                    }
                                }
                                .padding(.vertical, 4)
                            }
                        }
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(12)
                        .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
                    }
                }
                .padding()
            }
            .navigationTitle("Smart Motos")
            .background(Color(.systemGroupedBackground).ignoresSafeArea())
        }
    }

    private func createRide() {
        isRequesting = true
        let bargain = Int(bargainAmountText)
        let pickupLoc = LocationPoint(address: pickupAddress, latitude: pickupLat, longitude: pickupLon)
        let dropoffLoc = LocationPoint(address: dropoffAddress, latitude: dropoffLat, longitude: dropoffLon)
        
        let finalFare = bargain ?? calculatedFareRwf
        let rider = repository.currentUser

        let tripData: [String: Any] = [
            "riderId": rider.id,
            "riderName": rider.name,
            "orderType": orderType,
            "pickup": [
                "address": pickupLoc.address,
                "latitude": pickupLoc.latitude,
                "longitude": pickupLoc.longitude
            ],
            "dropoff": [
                "address": dropoffLoc.address,
                "latitude": dropoffLoc.latitude,
                "longitude": dropoffLoc.longitude
            ],
            "fareRwf": finalFare,
            "baseFareRwf": 500,
            "distanceKm": estimatedDistanceKm,
            "durationMins": Int(estimatedDistanceKm * 3),
            "paymentMethod": paymentMethod,
            "bargainAmountRwf": bargain as Any,
            "status": RideStatus.requested.rawValue,
            "ratingGiven": 0,
            "timestampLabel": "Just now",
            "createdAt": Date()
        ]

        importFirebaseFirestore()
            .collection("trips")
            .addDocument(data: tripData) { _ in
                self.isRequesting = false
                self.bargainAmountText = ""
            }
    }

    private func importFirebaseFirestore() -> FirebaseFirestore.Firestore {
        return FirebaseFirestore.Firestore.firestore()
    }
}

