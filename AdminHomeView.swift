import SwiftUI
import FirebaseFirestore

struct AdminHomeView: View {
    @ObservedObject var repository = SmartRepository.shared
    @State private var showingAddUserModal = false
    
    @State private var newUserName = ""
    @State private var newUserEmail = ""
    @State private var newUserPhone = ""
    @State private var newUserRole: UserRole = .passenger
    @State private var isCreatingUser = false
    @State private var createMessage = ""

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    
                    // Admin Stats Summary Header
                    HStack(spacing: 12) {
                        StatCard(title: "Total Users", value: "\(repository.allUsers.count)", icon: "person.3.fill", color: .blue)
                        StatCard(title: "Pending Topups", value: "\(repository.pendingTopups.count)", icon: "tray.and.arrow.down.fill", color: .orange)
                        StatCard(title: "Pending Drivers", value: "\(repository.pendingDrivers.count)", icon: "person.badge.shield.checkmark.fill", color: .purple)
                    }

                    // Quick Actions
                    HStack {
                        Button(action: { showingAddUserModal = true }) {
                            HStack {
                                Image(systemName: "person.badge.plus")
                                Text("Add Manual User")
                                    .fontWeight(.semibold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                        }
                    }

                    // Pending Driver Approvals
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Pending Driver Applications (\(repository.pendingDrivers.count))")
                            .font(.headline)

                        if repository.pendingDrivers.isEmpty {
                            Text("No pending driver registrations.")
                                .font(.subheadline)
                                .foregroundColor(.gray)
                                .padding(.vertical, 8)
                        } else {
                            ForEach(repository.pendingDrivers) { driver in
                                VStack(alignment: .leading, spacing: 8) {
                                    HStack {
                                        VStack(alignment: .leading) {
                                            Text(driver.name.isEmpty ? "Unnamed Driver" : driver.name)
                                                .font(.headline)
                                            Text("Phone: \(driver.phone) | Vehicle: \(driver.vehicleType)")
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                            Text("License: \(driver.licenseNumber)")
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                        }
                                        Spacer()
                                    }

                                    HStack(spacing: 12) {
                                        Button(action: { repository.rejectDriver(driverId: driver.id) }) {
                                            Text("Reject")
                                                .font(.caption)
                                                .fontWeight(.bold)
                                                .frame(maxWidth: .infinity)
                                                .padding(.vertical, 8)
                                                .background(Color.red.opacity(0.15))
                                                .foregroundColor(.red)
                                                .cornerRadius(6)
                                        }

                                        Button(action: { repository.approveDriver(driverId: driver.id) }) {
                                            Text("Approve Driver")
                                                .font(.caption)
                                                .fontWeight(.bold)
                                                .frame(maxWidth: .infinity)
                                                .padding(.vertical, 8)
                                                .background(Color.green)
                                                .foregroundColor(.white)
                                                .cornerRadius(6)
                                        }
                                    }
                                }
                                .padding()
                                .background(Color(.systemBackground))
                                .cornerRadius(10)
                                .shadow(color: Color.black.opacity(0.04), radius: 3, x: 0, y: 1)
                            }
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)

                    // Pending Topup Requests
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Pending Wallet Topups (\(repository.pendingTopups.count))")
                            .font(.headline)

                        if repository.pendingTopups.isEmpty {
                            Text("No pending top-up requests.")
                                .font(.subheadline)
                                .foregroundColor(.gray)
                                .padding(.vertical, 8)
                        } else {
                            ForEach(repository.pendingTopups) { req in
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(req.userName.isEmpty ? "User \(req.userId.prefix(6))" : req.userName)
                                            .font(.subheadline)
                                            .fontWeight(.bold)
                                        Text("MoMo: \(req.momoNumber)")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }

                                    Spacer()

                                    Text("\(req.amountRwf) RWF")
                                        .font(.callout)
                                        .fontWeight(.bold)
                                        .foregroundColor(.green)

                                    Button(action: { repository.rejectTopup(id: req.id) }) {
                                        Image(systemName: "xmark.circle.fill")
                                            .foregroundColor(.red)
                                    }

                                    Button(action: { repository.approveTopup(id: req.id) }) {
                                        Image(systemName: "checkmark.circle.fill")
                                            .foregroundColor(.green)
                                    }
                                }
                                .padding(.vertical, 6)
                                Divider()
                            }
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
                }
                .padding()
            }
            .navigationTitle("Admin Control")
            .background(Color(.systemGroupedBackground).ignoresSafeArea())
            .sheet(isPresented: $showingAddUserModal) {
                NavigationView {
                    Form {
                        Section(header: Text("User Information")) {
                            TextField("Full Name", text: $newUserName)
                            TextField("Email Address", text: $newUserEmail)
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                            TextField("Phone Number", text: $newUserPhone)
                                .keyboardType(.phonePad)
                            
                            Picker("Role", selection: $newUserRole) {
                                Text("Passenger").tag(UserRole.passenger)
                                Text("Driver").tag(UserRole.driver)
                                Text("Admin").tag(UserRole.admin)
                            }
                        }

                        if !createMessage.isEmpty {
                            Section {
                                Text(createMessage)
                                    .font(.caption)
                                    .foregroundColor(.blue)
                            }
                        }

                        Section {
                            Button(action: handleCreateUser) {
                                HStack {
                                    Spacer()
                                    if isCreatingUser {
                                        ProgressView()
                                    } else {
                                        Text("Create User")
                                            .fontWeight(.bold)
                                    }
                                    Spacer()
                                }
                            }
                            .disabled(isCreatingUser)
                        }
                    }
                    .navigationTitle("Add Manual User")
                    .navigationBarItems(trailing: Button("Close") { showingAddUserModal = false })
                }
            }
        }
    }

    private func handleCreateUser() {
        guard !newUserEmail.isEmpty else { return }
        isCreatingUser = true
        createMessage = ""

        repository.addManualUser(name: newUserName, email: newUserEmail, phone: newUserPhone, role: newUserRole) { success, resetLink, error in
            isCreatingUser = false
            if success {
                createMessage = "User created successfully! Password reset link: \(resetLink ?? "Sent via email")"
                newUserName = ""
                newUserEmail = ""
                newUserPhone = ""
            } else {
                createMessage = "Error: \(error ?? "Failed to create user")"
            }
        }
    }
}

struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color(.systemBackground))
        .cornerRadius(10)
        .shadow(color: Color.black.opacity(0.04), radius: 3, x: 0, y: 1)
    }
}

