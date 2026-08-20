import SwiftUI
import FirebaseAuth
import FirebaseFirestore

struct AuthenticationView: View {
    @ObservedObject var repository = SmartRepository.shared
    @State private var isSignUp = false
    @State private var email = ""
    @State private var password = ""
    @State private var fullName = ""
    @State private var phoneNumber = ""
    @State private var selectedRole: UserRole = .passenger
    @State private var errorMessage = ""
    @State private var isLoading = false

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Spacer()

                VStack(spacing: 8) {
                    Image(systemName: "moped.fill")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 70, height: 70)
                        .foregroundColor(.green)
                    
                    Text("Smart Motos")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    
                    Text(isSignUp ? "Create a new account" : "Sign in to continue")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                }

                Picker("Account Type", selection: $selectedRole) {
                    Text("Passenger").tag(UserRole.passenger)
                    Text("Driver").tag(UserRole.driver)
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding(.horizontal)

                VStack(spacing: 12) {
                    if isSignUp {
                        TextField("Full Name", text: $fullName)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                        
                        TextField("Phone Number (e.g. 078...)", text: $phoneNumber)
                            .keyboardType(.phonePad)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }

                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .textFieldStyle(RoundedBorderTextFieldStyle())

                    SecureField("Password", text: $password)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                }
                .padding(.horizontal)

                if !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }

                Button(action: handleAuthAction) {
                    HStack {
                        Spacer()
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text(isSignUp ? "Sign Up" : "Sign In")
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                        }
                        Spacer()
                    }
                    .padding()
                    .background(Color.green)
                    .cornerRadius(10)
                }
                .padding(.horizontal)
                .disabled(isLoading)

                Button(action: {
                    isSignUp.toggle()
                    errorMessage = ""
                }) {
                    Text(isSignUp ? "Already have an account? Sign In" : "Don't have an account? Sign Up")
                        .font(.footnote)
                        .foregroundColor(.blue)
                }

                Spacer()
            }
            .navigationBarHidden(true)
        }
    }

    private func handleAuthAction() {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = "Please enter both email and password."
            return
        }

        isLoading = true
        errorMessage = ""

        if isSignUp {
            Auth.auth().createUser(withEmail: email, password: password) { result, error in
                if let error = error {
                    self.isLoading = false
                    self.errorMessage = error.localizedDescription
                    return
                }

                guard let uid = result?.user.uid else { return }
                let userData: [String: Any] = [
                    "name": fullName.isEmpty ? "New User" : fullName,
                    "email": email,
                    "phone": phoneNumber,
                    "role": selectedRole.rawValue,
                    "walletBalanceRwf": 0,
                    "createdAt": FieldValue.serverTimestamp()
                ]

                Firestore.firestore().collection("users").document(uid).setData(userData) { error in
                    self.isLoading = false
                    if let error = error {
                        self.errorMessage = error.localizedDescription
                    }
                }
            }
        } else {
            Auth.auth().signIn(withEmail: email, password: password) { _, error in
                self.isLoading = false
                if let error = error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
}

