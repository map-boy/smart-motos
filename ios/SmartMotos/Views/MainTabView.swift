import SwiftUI
import FirebaseAuth

struct MainTabView: View {
    @ObservedObject var repository = SmartRepository.shared
    @State private var selectedTab = 0

    var body: some View {
        Group {
            if !repository.isAuthenticated {
                AuthenticationView()
            } else {
                switch repository.currentRole {
                case .passenger:
                    PassengerTabView(selectedTab: $selectedTab)
                case .driver:
                    DriverTabView(selectedTab: $selectedTab)
                case .admin:
                    AdminTabView(selectedTab: $selectedTab)
                }
            }
        }
    }
}

// MARK: - Passenger Tab Navigation

struct PassengerTabView: View {
    @Binding var selectedTab: Int

    var body: some View {
        TabView(selection: $selectedTab) {
            PassengerHomeView()
                .tabItem {
                    Image(systemName: "house.fill")
                    Text("Home")
                }
                .tag(0)

            TripHistoryView()
                .tabItem {
                    Image(systemName: "clock.fill")
                    Text("Trips")
                }
                .tag(1)

            WalletTopupView()
                .tabItem {
                    Image(systemName: "creditcard.fill")
                    Text("Wallet")
                }
                .tag(2)

            UserProfileView()
                .tabItem {
                    Image(systemName: "person.fill")
                    Text("Profile")
                }
                .tag(3)
        }
    }
}

// MARK: - Driver Tab Navigation

struct DriverTabView: View {
    @Binding var selectedTab: Int

    var body: some View {
        TabView(selection: $selectedTab) {
            DriverHomeView()
                .tabItem {
                    Image(systemName: "steeringwheel")
                    Text("Dashboard")
                }
                .tag(0)

            TripHistoryView()
                .tabItem {
                    Image(systemName: "dollarsign.circle.fill")
                    Text("Earnings")
                }
                .tag(1)

            UserProfileView()
                .tabItem {
                    Image(systemName: "person.fill")
                    Text("Profile")
                }
                .tag(2)
        }
    }
}

// MARK: - Admin Tab Navigation

struct AdminTabView: View {
    @Binding var selectedTab: Int

    var body: some View {
        TabView(selection: $selectedTab) {
            AdminHomeView()
                .tabItem {
                    Image(systemName: "shield.checkerboard")
                    Text("Overview")
                }
                .tag(0)

            UserProfileView()
                .tabItem {
                    Image(systemName: "person.fill")
                    Text("Profile")
                }
                .tag(1)
        }
    }
}

// MARK: - Supporting Sub-Views

struct TripHistoryView: View {
    @ObservedObject var repository = SmartRepository.shared

    var body: some View {
        NavigationView {
            List(repository.tripHistory) { trip in
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(trip.orderType)
                            .font(.caption)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.green.opacity(0.15))
                            .foregroundColor(.green)
                            .cornerRadius(4)

                        Spacer()

                        Text("\(trip.fareRwf) RWF")
                            .font(.headline)
                            .foregroundColor(.green)
                    }

                    Text("From: \(trip.pickup.address)")
                        .font(.subheadline)
                    Text("To: \(trip.dropoff.address)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)

                    HStack {
                        Text("Status: \(trip.status.rawValue)")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Spacer()
                        Text(trip.timestamp)
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                }
                .padding(.vertical, 4)
            }
            .navigationTitle("Trip History")
            .overlay(
                Group {
                    if repository.tripHistory.isEmpty {
                        VStack(spacing: 8) {
                            Image(systemName: "clock")
                                .font(.largeTitle)
                                .foregroundColor(.gray)
                            Text("No trips recorded yet.")
                                .foregroundColor(.gray)
                        }
                    }
                }
            )
        }
    }
}

struct WalletTopupView: View {
    @ObservedObject var repository = SmartRepository.shared
    @State private var topupAmount = ""
    @State private var showConfirmation = false

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Current Balance")) {
                    HStack {
                        Text("Wallet Balance")
                        Spacer()
                        Text("\(repository.currentUser.walletBalanceRwf) RWF")
                            .font(.headline)
                            .foregroundColor(.green)
                    }
                }

                Section(header: Text("Request Top-Up (MTN MoMo)")) {
                    TextField("Amount in RWF (e.g. 5000)", text: $topupAmount)
                        .keyboardType(.numberPad)

                    Button(action: {
                        if let amount = Int(topupAmount), amount > 0 {
                            repository.requestWalletTopup(amount: amount)
                            topupAmount = ""
                            showConfirmation = true
                        }
                    }) {
                        Text("Submit Request")
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    }
                }
            }
            .navigationTitle("Wallet & Top-up")
            .alert(isPresented: $showConfirmation) {
                Alert(
                    title: Text("Top-Up Requested"),
                    message: Text("Your request has been sent to admin for approval."),
                    dismissButton: .default(Text("OK"))
                )
            }
        }
    }
}

struct UserProfileView: View {
    @ObservedObject var repository = SmartRepository.shared
    @State private var name = ""
    @State private var phone = ""
    @State private var isEditing = false

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Account Overview")) {
                    HStack {
                        Text("Name")
                        Spacer()
                        Text(repository.currentUser.name)
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Email")
                        Spacer()
                        Text(repository.currentUser.email)
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Phone")
                        Spacer()
                        Text(repository.currentUser.phone)
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Role")
                        Spacer()
                        Text(repository.currentUser.role.rawValue.capitalized)
                            .foregroundColor(.green)
                            .fontWeight(.semibold)
                    }
                }

                Section {
                    Button(action: {
                        clearFcmTokenForCurrentUser {
                            try? FirebaseAuth.Auth.auth().signOut()
                        }
                    }) {
                        HStack {
                            Spacer()
                            Text("Sign Out")
                                .fontWeight(.bold)
                                .foregroundColor(.red)
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("Profile Settings")
        }
    }
}

