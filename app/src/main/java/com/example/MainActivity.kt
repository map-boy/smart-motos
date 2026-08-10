package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.RoleHeaderBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.common.MapboxOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapboxOptions.accessToken = BuildConfig.VITE_MAPBOX_TOKEN
        setContent {
            SmartMotosTheme(darkTheme = true) {
                SmartMotosApp()
            }
        }
    }
}

@Composable
fun SmartMotosApp() {
    val currentRole by SmartRepository.currentRole.collectAsState()
    val currentUser by SmartRepository.currentUser.collectAsState()
    val isAuthenticated by SmartRepository.isAuthenticated.collectAsState()
    val activeRide by SmartRepository.activeRide.collectAsState()
    val activityContext = LocalContext.current

    var navigationScreen by remember { mutableStateOf("splash") } // splash, onboarding, login, passenger_signup, driver_signup, main, support, topup
    val backStack = remember { mutableStateListOf<String>() }
    var hasCheckedSession by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasCheckedSession) {
            hasCheckedSession = true
            if (FirebaseAuth.getInstance().currentUser != null) {
                navigationScreen = "main"
            }
        }
    }
    var showExitDialog by remember { mutableStateOf(false) }
    var prefillEmail by remember { mutableStateOf("") }
    var selectedPassengerTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Book Ride, 2: Account
    var selectedDriverTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Analytics, 2: Vehicle

    fun navigateTo(screen: String) {
        if (screen != navigationScreen) {
            backStack.add(navigationScreen)
            navigationScreen = screen
        }
    }

    BackHandler(enabled = true) {
        if (selectedPassengerTab != 0 && navigationScreen == "main" && currentRole == UserRole.PASSENGER) {
            selectedPassengerTab = 0
        } else if (selectedDriverTab != 0 && navigationScreen == "main" && currentRole == UserRole.DRIVER) {
            selectedDriverTab = 0
        } else if (backStack.isNotEmpty()) {
            navigationScreen = backStack.removeAt(backStack.lastIndex)
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Smart Motos?") },
            text = { Text("Are you sure you want to close the app?") },
            confirmButton = {
                TextButton(onClick = {
                    (activityContext as? android.app.Activity)?.finish()
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("No") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SmartBlackDark,
        topBar = {
            if (navigationScreen == "main") {
                RoleHeaderBar(
                    currentRole = currentRole,
                    userName = currentUser.name,
                    onRoleChanged = { role ->
                        SmartRepository.setRole(role)
                    }
                )
            }
        },
        bottomBar = {
            val hideNavForInlineLogin = currentRole == UserRole.PASSENGER && !isAuthenticated && selectedPassengerTab != 0
            if (navigationScreen == "main" && currentRole != UserRole.ADMIN && !hideNavForInlineLogin) {
                NavigationBar(
                    containerColor = SmartBlackPaper,
                    contentColor = SmartYellow
                ) {
                    if (currentRole == UserRole.PASSENGER) {
                        NavigationBarItem(
                            selected = selectedPassengerTab == 0,
                            onClick = { selectedPassengerTab = 0 },
                            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SmartYellowDark,
                                selectedTextColor = SmartYellow,
                                indicatorColor = SmartYellowLight,
                                unselectedIconColor = SmartGrayMedium,
                                unselectedTextColor = SmartGrayMedium
                            )
                        )
                        NavigationBarItem(
                            selected = selectedPassengerTab == 1,
                            onClick = { selectedPassengerTab = 1 },
                            icon = { Icon(Icons.Filled.TwoWheeler, contentDescription = "Book Ride") },
                            label = { Text("Book Ride", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SmartYellowDark,
                                selectedTextColor = SmartYellow,
                                indicatorColor = SmartYellowLight,
                                unselectedIconColor = SmartGrayMedium,
                                unselectedTextColor = SmartGrayMedium
                            )
                        )
                        NavigationBarItem(
                            selected = selectedPassengerTab == 2,
                            onClick = { selectedPassengerTab = 2 },
                            icon = { Icon(Icons.Filled.Person, contentDescription = "Account") },
                            label = { Text("Account", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SmartYellowDark,
                                selectedTextColor = SmartYellow,
                                indicatorColor = SmartYellowLight,
                                unselectedIconColor = SmartGrayMedium,
                                unselectedTextColor = SmartGrayMedium
                            )
                        )
                    } else if (currentRole == UserRole.DRIVER) {
                        NavigationBarItem(
                            selected = selectedDriverTab == 0,
                            onClick = { selectedDriverTab = 0 },
                            icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SmartYellowDark,
                                selectedTextColor = SmartYellow,
                                indicatorColor = SmartYellowLight,
                                unselectedIconColor = SmartGrayMedium,
                                unselectedTextColor = SmartGrayMedium
                            )
                        )
                        NavigationBarItem(
                            selected = selectedDriverTab == 1,
                            onClick = { selectedDriverTab = 1 },
                            icon = { Icon(Icons.Filled.BarChart, contentDescription = "Analytics") },
                            label = { Text("Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SmartYellowDark,
                                selectedTextColor = SmartYellow,
                                indicatorColor = SmartYellowLight,
                                unselectedIconColor = SmartGrayMedium,
                                unselectedTextColor = SmartGrayMedium
                            )
                        )
                        NavigationBarItem(
                            selected = selectedDriverTab == 2,
                            onClick = { selectedDriverTab = 2 },
                            icon = { Icon(Icons.Filled.TwoWheeler, contentDescription = "Vehicle") },
                            label = { Text("Vehicle", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SmartYellowDark,
                                selectedTextColor = SmartYellow,
                                indicatorColor = SmartYellowLight,
                                unselectedIconColor = SmartGrayMedium,
                                unselectedTextColor = SmartGrayMedium
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (navigationScreen) {
                "splash" -> SplashScreen(
                    onPassengerClick = {
                        SmartRepository.setRole(UserRole.PASSENGER)
                        navigateTo("login")
                    },
                    onDriverClick = {
                        SmartRepository.setRole(UserRole.DRIVER)
                        navigateTo("login")
                    },
                    onAdminClick = {
                        SmartRepository.setRole(UserRole.ADMIN)
                        navigateTo("login")
                    }
                )

                "onboarding" -> OnboardingScreen(
                    onFinish = { navigateTo("main") }
                )

                "login" -> LoginScreen(
                    role = currentRole,
                    onLoginSuccess = { role ->
                        navigateTo("main")
                    },
                    onNavigateToSignup = { email ->
                        prefillEmail = email
                        navigateTo(if (currentRole == UserRole.DRIVER) "driver_signup" else "passenger_signup")
                    }
                )

                "passenger_signup" -> PassengerSignupScreen(
                    initialEmail = prefillEmail,
                    onSignupSuccess = { navigateTo("main") },
                    onNavigateToLogin = { navigateTo("login") }
                )

                "driver_signup" -> DriverSignupScreen(
                    initialEmail = prefillEmail,
                    onSignupSuccess = { navigateTo("main") },
                    onNavigateToLogin = { navigateTo("login") }
                )

                "topup" -> TopUpModal(
                    onDismiss = { navigateTo("main") }
                )

                "support" -> SupportChatScreen(
                    onNavigateBack = { navigateTo("main") }
                )

                "trip_chat" -> TripChatScreen(
                    tripId = activeRide?.id ?: "",
                    otherPartyName = if (currentRole == UserRole.PASSENGER) (activeRide?.driverName ?: "Driver") else (activeRide?.riderName ?: "Rider"),
                    onNavigateBack = { navigateTo("main") }
                )

                "main" -> {
                    val pendingDriverStatus = currentUser.driverApplicationStatus
                    if (currentRole == UserRole.PASSENGER && pendingDriverStatus != null && pendingDriverStatus != "approved") {
                        PendingApprovalScreen(
                            status = pendingDriverStatus,
                            onSignOut = { navigateTo("splash") }
                        )
                    } else {
                    when (currentRole) {
                        UserRole.PASSENGER -> {
                            when (selectedPassengerTab) {
                                0 -> PassengerHomeScreen(
                                    onBookRideClick = { selectedPassengerTab = 1 },
                                    onTopUpClick = { navigateTo("topup") },
                                    onOpenSupportClick = { navigateTo("support") }
                                )

                                1 -> if (!isAuthenticated) {
                                    LoginScreen(
                                        role = UserRole.PASSENGER,
                                        onLoginSuccess = { },
                                        onNavigateToSignup = { email -> prefillEmail = email; navigationScreen = "passenger_signup" }
                                    )
                                } else {
                                    BookRideScreen(
                                        onNavigateBack = { selectedPassengerTab = 0 },
                                        onOpenChatClick = { navigateTo("trip_chat") }
                                    )
                                }

                                2 -> if (!isAuthenticated) {
                                    LoginScreen(
                                        role = UserRole.PASSENGER,
                                        onLoginSuccess = { },
                                        onNavigateToSignup = { email -> prefillEmail = email; navigationScreen = "passenger_signup" }
                                    )
                                } else {
                                    PassengerProfileScreen(
                                        onTopUpClick = { navigateTo("topup") },
                                        onOpenSupportClick = { navigateTo("support") },
                                        onSignOutClick = {
                                            FirebaseAuth.getInstance().signOut()
                                            selectedPassengerTab = 0
                                        }
                                    )
                                }
                            }
                        }

                        UserRole.DRIVER -> {
                            when (selectedDriverTab) {
                                0 -> DriverHomeScreen(
                                    onNavigateAnalytics = { selectedDriverTab = 1 },
                                    onNavigateVehicle = { selectedDriverTab = 2 },
                                    onOpenChatClick = { navigateTo("trip_chat") }
                                )

                                1 -> DriverAnalyticsScreen(
                                    onNavigateBack = { selectedDriverTab = 0 }
                                )

                                2 -> DriverVehicleDetailsScreen(
                                    onNavigateBack = { selectedDriverTab = 0 }
                                )
                            }
                        }

                        UserRole.ADMIN -> {
                            AdminDashboardScreen()
                        }
                    }
                    }
                }
            }
        }
    }
}
