package com.example.ui.screens

import android.widget.Toast
import com.example.data.registerFcmTokenForCurrentUser
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SmartRepository
import com.example.data.UserRole
import com.example.ui.components.SmartButton
import com.example.ui.components.SmartTextField
import com.example.ui.theme.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage

private fun roleStringToUserRole(role: String?): UserRole = when (role) {
    "driver" -> UserRole.DRIVER
    "admin" -> UserRole.ADMIN
    else -> UserRole.PASSENGER
}

private fun performLogin(
    email: String,
    password: String,
    onResult: (Result<UserRole>) -> Unit
) {
    // Uses SmartRepository.signInWithEmail internally: coroutine + 15s timeout,
    // so a flaky connection fails cleanly instead of hanging the spinner forever.
    kotlinx.coroutines.MainScope().launch {
        val result = com.example.data.SmartRepository.signInWithEmail(email, password)
        result.onSuccess {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                onResult(Result.failure(Exception("No user returned after sign-in")))
                return@launch
            }
            try {
                val doc = kotlinx.coroutines.withTimeout(15_000L) {
                    FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                }
                onResult(Result.success(roleStringToUserRole(doc.getString("role"))))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }.onFailure { e ->
            onResult(Result.failure(e))
        }
    }
}

private const val GOOGLE_WEB_CLIENT_ID = "532787193673-mav1ei5nhqsf2neg0gjeammvnb873i88.apps.googleusercontent.com"
// admin credentials removed - admin role is now verified server-side via actualRole

private suspend fun performGoogleSignIn(
    context: android.content.Context,
    defaultRole: UserRole,
    onResult: (Result<UserRole>) -> Unit
) {
    try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val signInResult = com.example.data.SmartRepository.signInWithGoogle(idToken, defaultRole)
            signInResult.onSuccess {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    onResult(Result.failure(Exception("No user returned after sign-in")))
                } else {
                    try {
                        val doc = kotlinx.coroutines.withTimeout(15_000L) {
                            FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                        }
                        val role = if (doc.exists()) roleStringToUserRole(doc.getString("role")) else defaultRole
                        onResult(Result.success(role))
                    } catch (e: Exception) {
                        onResult(Result.failure(e))
                    }
                }
            }.onFailure { e ->
                onResult(Result.failure(e))
            }
        } else {
            onResult(Result.failure(Exception("Unexpected credential type returned")))
        }
    } catch (e: NoCredentialException) {
        onResult(Result.failure(Exception("No Google account found on this device, or none available yet. Please try again, or add a Google account in device Settings.")))
    } catch (e: GetCredentialException) {
        onResult(Result.failure(Exception(e.localizedMessage ?: "Google sign-in failed, please try again")))
    }
}

@Composable
fun LoginScreen(
    role: UserRole,
    onLoginSuccess: (UserRole) -> Unit,
    onNavigateToSignup: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selectedRole = role
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = SmartYellow
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.TwoWheeler,
                        contentDescription = "Smart Motos",
                        tint = SmartBlack,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "${selectedRole.name.lowercase().replaceFirstChar { it.uppercase() }} Sign In",
                color = SmartBlack,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Enter your account details to access Smart Motos",
                color = SmartGrayMedium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(8.dp))

            if (role != UserRole.ADMIN) {
                SmartTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email or Phone Number",
                    placeholder = "you@example.com / +250 78...",
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            SmartTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "Password",
                isPassword = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (role != UserRole.ADMIN) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot password?",
                        color = SmartYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (email.isBlank()) {
                                Toast.makeText(context, "Enter your email first", Toast.LENGTH_SHORT).show()
                            } else {
                                FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim())
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Password reset link sent to $email", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, e.localizedMessage ?: "Could not send reset email", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SmartButton(
                text = "Log In",
                isLoading = isLoading,
                onClick = {
                    if (role == UserRole.ADMIN) {
                        isLoading = true
                        performLogin(email, password) { result ->
                            isLoading = false
                            result.onSuccess { actualRole ->
                                if (actualRole == UserRole.ADMIN) {
                                    SmartRepository.setRole(UserRole.ADMIN)
                                    Toast.makeText(context, "Welcome, Admin!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(UserRole.ADMIN)
                                } else {
                                    Toast.makeText(context, "This account does not have admin access", Toast.LENGTH_LONG).show()
                                }
                            }.onFailure { e ->
                                Toast.makeText(context, e.localizedMessage ?: "Admin login failed - check the account exists in Firebase", Toast.LENGTH_LONG).show()
                            }
                        }
                        return@SmartButton
                    }
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Enter your email and password", Toast.LENGTH_SHORT).show()
                        return@SmartButton
                    }
                    isLoading = true
                    performLogin(email, password) { result ->
                        isLoading = false
                        result.onSuccess { actualRole ->
                            SmartRepository.setRole(actualRole)
                            Toast.makeText(context, "Welcome back to Smart Motos!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(actualRole)
                        }.onFailure { e ->
                            if (e is FirebaseAuthInvalidUserException) {
                                Toast.makeText(context, "No account found, please sign up", Toast.LENGTH_SHORT).show()
                                onNavigateToSignup(email)
                            } else {
                                Toast.makeText(context, e.localizedMessage ?: "Login failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )

            if (role != UserRole.ADMIN) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SmartGrayDark)
                    Text(
                        text = "  or continue with  ",
                        color = SmartGrayMedium,
                        fontSize = 12.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SmartGrayDark)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            performGoogleSignIn(context, selectedRole) { result ->
                                isLoading = false
                                result.onSuccess { role ->
                                    SmartRepository.setRole(role)
                                    Toast.makeText(context, "Welcome to Smart Motos!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(role)
                                }.onFailure { e ->
                                    Toast.makeText(context, e.localizedMessage ?: "Google sign-in failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmartBlackCard,
                        contentColor = SmartBlack
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = SmartYellow)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Sign in with Google", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        if (role != UserRole.ADMIN) {
            Row(
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have an account? ", color = SmartGrayMedium, fontSize = 14.sp)
                Text(
                    text = "Sign Up",
                    color = SmartYellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToSignup(email) }
                )
            }
        }
    }
}

@Composable
fun PassengerSignupScreen(
    initialEmail: String = "",
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(initialEmail) }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Create Passenger Account",
            color = SmartBlack,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Sign up to start hailing moto rides in Kigali",
            color = SmartGrayMedium,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 24.dp)
        )

        SmartTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name",
            placeholder = "John Doe",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        SmartTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            placeholder = "your@email.com",
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        SmartTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number",
            placeholder = "+250 78...",
            keyboardType = KeyboardType.Phone,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        SmartTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            placeholder = "At least 6 characters",
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        SmartButton(
            text = "Create Account",
            isLoading = isLoading,
            onClick = {
                if (email.isBlank() || password.length < 6) {
                    Toast.makeText(context, "Enter a valid email and a password of at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@SmartButton
                }
                isLoading = true
                kotlinx.coroutines.MainScope().launch {
                    val result = com.example.data.SmartRepository.signUpPassenger(email, password, fullName, phone)
                    isLoading = false
                    result.onSuccess {
                        Toast.makeText(context, "Passenger Account Created!", Toast.LENGTH_SHORT).show()
                        onSignupSuccess()
                    }.onFailure { e ->
                        Toast.makeText(context, e.localizedMessage ?: "Sign-up failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = SmartGrayMedium, fontSize = 14.sp)
            Text(
                text = "Login",
                color = SmartYellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}

@Composable
fun DriverSignupScreen(
    initialEmail: String = "",
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(initialEmail) }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var serviceProvider by remember { mutableStateOf("MTN") }
    var vehicleType by remember { mutableStateOf("Bike") }
    var licenseNumber by remember { mutableStateOf("") }
    var inspectionCode by remember { mutableStateOf("") }
    var licensePhotoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> licensePhotoUri = uri }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Driver Registration",
            color = SmartBlack,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Join Smart Motos as a verified driver",
            color = SmartGrayMedium,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp)
        )

        SmartTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name",
            placeholder = "Kamana Emmanuel",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SmartTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            placeholder = "driver@example.com",
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SmartTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number",
            placeholder = "+250 78...",
            keyboardType = KeyboardType.Phone,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SmartTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            placeholder = "Password",
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Mobile Service Provider",
            color = SmartBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("MTN", "Airtel").forEach { provider ->
                val isSelected = serviceProvider == provider
                Surface(
                    onClick = { serviceProvider = provider },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SmartYellow else SmartBlackCard
                ) {
                    Text(
                        text = provider,
                        color = if (isSelected) SmartBlack else SmartGrayMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Vehicle Type",
            color = SmartBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Bike", "Car").forEach { vType ->
                val isSelected = vehicleType == vType
                Surface(
                    onClick = { vehicleType = vType },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SmartYellow else SmartBlackCard
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (vType == "Bike") Icons.Filled.TwoWheeler else Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = if (isSelected) SmartBlack else SmartYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = vType,
                            color = if (isSelected) SmartBlack else SmartGrayMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SmartTextField(
            value = licenseNumber,
            onValueChange = { licenseNumber = it },
            label = "Driving License Number",
            placeholder = "RA-88392",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SmartTextField(
            value = inspectionCode,
            onValueChange = { inspectionCode = it },
            label = "Vehicle Inspection Code",
            placeholder = "RIC-2026-00000",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Driving License Photo (Required)",
            color = SmartBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable { photoPickerLauncher.launch("image/*") },
            shape = RoundedCornerShape(12.dp),
            color = SmartBlackCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (licensePhotoUri != null) SmartSuccess else SmartYellow)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (licensePhotoUri != null) {
                    AsyncImage(
                        model = licensePhotoUri,
                        contentDescription = "License photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "Camera", tint = SmartYellow)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tap to Select Photo of License", color = SmartYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SmartButton(
            text = "Submit Application",
            isLoading = isLoading,
            onClick = {
                if (fullName.isBlank() || phone.isBlank()) {
                    Toast.makeText(context, "Enter your full name and phone number", Toast.LENGTH_SHORT).show()
                    return@SmartButton
                }
                if (email.isBlank() || password.length < 6) {
                    Toast.makeText(context, "Enter a valid email and a password of at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@SmartButton
                }
                if (licenseNumber.isBlank()) {
                    Toast.makeText(context, "Enter your driving license number", Toast.LENGTH_SHORT).show()
                    return@SmartButton
                }
                if (inspectionCode.isBlank()) {
                    Toast.makeText(context, "Enter your vehicle inspection code", Toast.LENGTH_SHORT).show()
                    return@SmartButton
                }
                if (licensePhotoUri == null) {
                    Toast.makeText(context, "Please attach your license photo", Toast.LENGTH_SHORT).show()
                    return@SmartButton
                }
                isLoading = true
                val photoUri = licensePhotoUri!!
                kotlinx.coroutines.MainScope().launch {
                    val result = com.example.data.SmartRepository.signUpDriver(
                        email, password, fullName, phone, serviceProvider, vehicleType, licenseNumber, inspectionCode, photoUri
                    )
                    isLoading = false
                    result.onSuccess {
                        Toast.makeText(context, "Application received - uploading your license...", Toast.LENGTH_LONG).show()
                        onSignupSuccess()
                    }.onFailure { e ->
                        Toast.makeText(context, e.localizedMessage ?: "Sign-up failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already registered? ", color = SmartGrayMedium, fontSize = 14.sp)
            Text(
                text = "Login",
                color = SmartYellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}
















