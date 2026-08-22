import SwiftUI
import FirebaseFirestore
import FirebaseAuth

struct DriverHomeView: View {
    @ObservedObject var repository = SmartRepository.shared
    
    @State private var isOnline = true
    @State private var currentLat: Double = -1.9515
    @State private var currentLon: Double = 30.1265

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    
                    // Driver Status Header
                    VStack(spacing: 12) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Driver Portal")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                                Text(repository.currentUser.name.isEmpty ? "Driver" : repository.currentUser.name)
                                    .font(.title2)
                                    .fontWeight(.bold)
                            }
                            Spacer()
                            Toggle("", isOn: $isOnline)
                                .labelsHidden()
                                .onChange(of: isOnline) { newValue in
                                    repository.toggleDriverOnline(newValue)
                                }
                        }
                        
                        HStack {
                            Circle()
                                .fill(isOnline ? Color.green : Color.red)
                                .frame(width: 10, height: 10)
                            Text(isOnline ? "Online - Ready for Rides" : "Offline")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(isOnline ? .green : .red)
                            Spacer()
                            Text(repository.currentUser.vehicleType)
                                .font(.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.gray.opacity(0.15))
                                .cornerRadius(8)
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)

                    // Incoming Dispatch Request Alert
                    if let incoming = repository.incomingDriverRequest {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Image(systemName: "bell.badge.fill")
                                    .foregroundColor(.orange)
                                Text("New Trip Offered!")
                                    .font(.headline)
                                Spacer()
                                Text("\(incoming.fareRwf) RWF")
                                    .font(.title3)
                                    .fontWeight(.bold)
                                    .foregroundColor(.green)
                            }

                            Divider()

                            VStack(alignment: .leading, spacing: 6) {
                                Text("Rider: \(incoming.riderName)")
                                    .font(.subheadline)
                                Text("Pickup: \(incoming.pickup.address)")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                                Text("Dropoff: \(incoming.dropoff.address)")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }

                            HStack(spacing: 12) {
                                Button(action: { rejectTrip(incoming.id) }) {
                                    Text("Decline")
                                        .fontWeight(.semibold)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 10)
                                        .background(Color.red.opacity(0.15))
                                        .foregroundColor(.red)
                                        .cornerRadius(8)
                                }

                                Button(action: { acceptTrip(incoming) }) {
                                    Text("Accept Ride")
                                        .fontWeight(.bold)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 10)
                                        .background(Color.green)
                                        .foregroundColor(.white)
                                        .cornerRadius(8)
                                }
                            }
                        }
                        .padding()
                        .background(Color.orange.opacity(0.1))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.orange, lineWidth: 1.5))
                        .cornerRadius(12)
                    }

                    // Active Ongoing Trip Card
                    if let active = repository.activeRide {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Image(systemName: "moped.fill")
                                    .foregroundColor(.green)
                                Text("Active Ride")
                                    .font(.headline)
                                Spacer()
                                Text(active.status.rawValue)
                                    .font(.caption)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.green.opacity(0.2))
                                    .foregroundColor(.green)
                                    .cornerRadius(6)
                            }

                            VStack(alignment: .leading, spacing: 6) {
                                Text("Passenger: \(active.riderName)")
                                    .font(.subheadline)
                                Text("From: \(active.pickup.address)")
                                    .font(.caption)
                                Text("To: \(active.dropoff.address)")
                                    .font(.caption)
                                Text("Fare: \(active.fareRwf) RWF (\(active.paymentMethod))")
                                    .font(.subheadline)
                                    .fontWeight(.bold)
                                    .foregroundColor(.green)
                            }

                            Divider()

                            HStack(spacing: 12) {
                                if active.status == .driverAssigned {
                                    Button(action: { updateTripStatus(active.id, status: .driverArrived) }) {
                                        Text("Arrived at Pickup")
                                            .fontWeight(.bold)
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 10)
                                            .background(Color.blue)
                                            .foregroundColor(.white)
                                            .cornerRadius(8)
                                    }
                                } else if active.status == .driverArrived {
                                    Button(action: { updateTripStatus(active.id, status: .inProgress) }) {
                                        Text("Start Trip")
                                            .fontWeight(.bold)
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 10)
                                            .background(Color.green)
                                            .foregroundColor(.white)
                                            .cornerRadius(8)
                                    }
                                } else if active.status == .inProgress {
                                    Button(action: { updateTripStatus(active.id, status: .completed) }) {
                                        Text("Complete Trip")
                                            .fontWeight(.bold)
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 10)
                                            .background(Color.purple)
                                            .foregroundColor(.white)
                                            .cornerRadius(8)
                                    }
                                }
                            }
                        }
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(12)
                        .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
                    } else if repository.incomingDriverRequest == nil {
                        VStack(spacing: 12) {
                            Image(systemName: "location.circle")
                                .font(.system(size: 40))
                                .foregroundColor(.gray)
                            Text("Searching for nearby ride requests...")
                                .font(.subheadline)
                                .foregroundColor(.gray)
                        }
                        .padding(.vertical, 40)
                    }

                    // Simulated GPS Location Updater Button
                    Button(action: simulateLocationUpdate) {
                        HStack {
                            Image(systemName: "location.fill")
                            Text("Update GPS Position to Kigali Centre")
                        }
                        .font(.caption)
                        .padding(10)
                        .background(Color.gray.opacity(0.15))
                        .cornerRadius(8)
                    }
                }
                .padding()
            }
            .navigationTitle("Driver Dashboard")
            .background(Color(.systemGroupedBackground).ignoresSafeArea())
        }
    }

    private func acceptTrip(_ trip: RideTrip) {
        guard let driverUid = repository.currentUser.id.isEmpty ? Auth.auth().currentUser?.uid : repository.currentUser.id else { return }
        let db = Firestore.firestore()
        db.collection("trips").document(trip.id).updateData([
            "driverId": driverUid,
            "driverName": repository.currentUser.name,
            "vehiclePlate": repository.currentUser.licenseNumber,
            "status": RideStatus.driverAssigned.rawValue,
            // dispatchTimeoutSweep queries offerStatus == "offered" AND
            // offerExpiresAtMs <= now. Left as "offered", the sweep expires this
            // accepted ride ~20s later and re-dispatches it to another driver.
            "offerStatus": "accepted"
        ])
    }

    private func rejectTrip(_ tripId: String) {
        let db = Firestore.firestore()
        // dispatchOnDriverResponse fires on offerStatus going "offered" -> "declined",
        // which is what releases this driver's lock and re-offers to the next candidate.
        // Writing status back to REQUESTED matches nothing: the trip would just sit
        // until the one-minute sweep noticed it.
        db.collection("trips").document(tripId).updateData([
            "offerStatus": "declined"
        ])
    }

    private func updateTripStatus(_ tripId: String, status: RideStatus) {
        let db = Firestore.firestore()
        db.collection("trips").document(tripId).updateData([
            "status": status.rawValue
        ])
    }

    private func simulateLocationUpdate() {
        currentLat = -1.9546
        currentLon = 30.0931
        repository.updateDriverLocation(latitude: currentLat, longitude: currentLon)
    }
}

