package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

// ----------------------------------------------------
// PASSENGER HOME SCREEN
// ----------------------------------------------------
@Composable
fun PassengerHomeScreen(
    onBookRideClick: () -> Unit,
    onTopUpClick: () -> Unit,
    onOpenSupportClick: () -> Unit
) {
    val currentUser by SmartRepository.currentUser.collectAsState()
    val isAuthenticated by SmartRepository.isAuthenticated.collectAsState()
    val tripHistory by SmartRepository.tripHistory.collectAsState()
    val activeRide by SmartRepository.activeRide.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Wallet Balance Card (only for signed-in users)
            if (isAuthenticated) {
                WalletBalanceCard(
                    balanceRwf = currentUser.walletBalanceRwf,
                    onTopUpClick = onTopUpClick
                )
            }

            // Active Ride Status Banner if any
            if (activeRide != null && activeRide?.status != RideStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    onClick = onBookRideClick,
                    shape = RoundedCornerShape(16.dp),
                    color = SmartYellow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(SmartBlack, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.DirectionsRun, contentDescription = null, tint = SmartYellow)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Active Ride: ${activeRide?.status?.name?.replace('_', ' ')}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${activeRide?.pickup?.address?.take(18)}...  -  ${activeRide?.dropoff?.address?.take(18)}...",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SmartBlack)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // All Transport Section
            Text(
                text = "All Transport",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Motorbike Hailing Card
            Surface(
                onClick = onBookRideClick,
                shape = RoundedCornerShape(18.dp),
                color = SmartBlackCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, SmartGrayDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SmartYellow,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.TwoWheeler,
                                    contentDescription = "Motorbike",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Motorbike Hailing",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Fast, affordable moto taxis across Kigali",
                                color = SmartGrayMedium,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.ArrowForwardIos,
                        contentDescription = "Book",
                        tint = SmartYellow,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Trip History Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trip History",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tripHistory.size} rides",
                    color = SmartYellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tripHistory.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SmartBlackCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SmartGrayDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = SmartGrayMedium, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No completed trips yet", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Book your first moto ride to see history here", color = SmartGrayMedium, fontSize = 12.sp)
                    }
                }
            } else {
                tripHistory.forEach { trip ->
                    TripHistoryCard(trip = trip)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // padding for bottom bar
        }
    }
}

@Composable
fun TripHistoryCard(trip: RideTrip) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SmartGrayDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = SmartYellowLight,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Driver Avatar",
                        tint = SmartYellowDark,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.vehiclePlate,
                    color = SmartYellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = trip.driverName ?: "Verified Moto Driver",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${trip.pickup.address.take(15)}  -  ${trip.dropoff.address.take(15)}",
                    color = SmartGrayMedium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                RatingStarsRow(rating = if (trip.ratingGiven > 0) trip.ratingGiven.toFloat() else trip.driverRating, starSize = 14)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%,d Rwf".format(trip.fareRwf),
                    color = SmartYellowDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${trip.distanceKm} km",
                    color = SmartGrayMedium,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// BOOK RIDE & ACTIVE TRIP FLOW
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookRideScreen(
    onNavigateBack: () -> Unit,
    onOpenChatClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val nearbyDrivers by SmartRepository.nearbyDrivers.collectAsState()
    val activeRide by SmartRepository.activeRide.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var bookingMode by remember { mutableStateOf("map") } // "map" or "form"
    var selectedPickup by remember { mutableStateOf<LocationPoint?>(null) }
    var selectedDropoff by remember { mutableStateOf<LocationPoint?>(null) }
    var showPickupSearch by remember { mutableStateOf(false) }
    var showDropoffSearch by remember { mutableStateOf(false) }

    fun fetchGpsAsPickup() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        if (LocationSearchService.isWithinKigali(loc.latitude, loc.longitude)) {
                            coroutineScope.launch {
                                selectedPickup = LocationSearchService.reverseGeocode(loc.latitude, loc.longitude)
                            }
                        } else {
                            Toast.makeText(context, "You are outside Kigali, please pick a location manually", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        } else {
            Toast.makeText(context, "Location permission needed to use current location", Toast.LENGTH_LONG).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchGpsAsPickup()
    }
    var paymentMethod by remember { mutableStateOf("Mobile Money") }
    var bargainAmount by remember { mutableStateOf("") }
    var orderType by remember { mutableStateOf("Passenger") } // "Passenger" or "Cargo"
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var cargoDescription by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf("input") } // "input", "searching", "active", "invoice", "rating"

    var selectedRating by remember { mutableStateOf(5) }
    var ratingComment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Surface(color = SmartBlackPaper, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SmartYellow)
                    }
                    Text(
                        text = when (currentStep) {
                            "input" -> "BOOK MOTO RIDE"
                            "searching" -> "FINDING DRIVER..."
                            "active" -> "TRIP IN PROGRESS"
                            "invoice" -> "TRIP INVOICE"
                            "rating" -> "RATE YOUR DRIVER"
                            else -> "BOOK RIDE"
                        },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = SmartBlackCard,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = SmartYellow)
                    }
                }
            }
        }

        // STEP 1: INPUT / MAP BOOKING
        if (currentStep == "input") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Booking Mode Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SmartBlackCard)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (bookingMode == "map") SmartYellow else Color.Transparent)
                            .clickable { bookingMode = "map" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = if (bookingMode == "map") SmartBlack else SmartGrayMedium, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Book by Map", color = if (bookingMode == "map") SmartBlack else SmartGrayMedium, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (bookingMode == "form") SmartYellow else Color.Transparent)
                            .clickable { bookingMode = "form" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FormatListBulleted, contentDescription = null, tint = if (bookingMode == "form") SmartBlack else SmartGrayMedium, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Book by Form", color = if (bookingMode == "form") SmartBlack else SmartGrayMedium, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Map View
                InteractiveKigaliMap(
                    pickup = selectedPickup,
                    dropoff = selectedDropoff,
                    onLocationTap = { loc ->
                        selectedDropoff = loc
                        Toast.makeText(context, "Dropoff set to: ${loc.address}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(200.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Pickup Selector
                Text("Pickup Location", color = SmartYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SmartBlackCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .clickable { showPickupSearch = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = SmartSuccess)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            selectedPickup?.address ?: "Tap to set pickup",
                            color = if (selectedPickup != null) SmartBlack else SmartGrayMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = SmartYellow)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dropoff Selector
                Text("Drop-off Location", color = SmartYellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SmartBlackCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SmartError),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .clickable { showDropoffSearch = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = SmartError)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            selectedDropoff?.address ?: "Tap to set drop-off",
                            color = if (selectedDropoff != null) SmartBlack else SmartGrayMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = SmartYellow)
                    }
                }

                if (showPickupSearch) {
                    LocationSearchDialog(
                        title = "Set Pickup Location",
                        onDismiss = { showPickupSearch = false },
                        onLocationSelected = { selectedPickup = it },
                        onUseCurrentLocation = {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                            fetchGpsAsPickup()
                        }
                    )
                }

                if (showDropoffSearch) {
                    LocationSearchDialog(
                        title = "Set Drop-off Location",
                        onDismiss = { showDropoffSearch = false },
                        onLocationSelected = { selectedDropoff = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                // Order Type Toggle
                Text("Order Type", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SmartBlackCard)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (orderType == "Passenger") SmartYellow else Color.Transparent)
                            .clickable { orderType = "Passenger" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Passenger Ride", color = if (orderType == "Passenger") SmartBlack else SmartGrayMedium, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (orderType == "Cargo") SmartYellow else Color.Transparent)
                            .clickable { orderType = "Cargo" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cargo Delivery", color = if (orderType == "Cargo") SmartBlack else SmartGrayMedium, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (orderType == "Cargo") {
                    SmartTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = "Recipient Name",
                        placeholder = "Who receives the package",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SmartTextField(
                        value = recipientPhone,
                        onValueChange = { recipientPhone = it },
                        label = "Recipient Phone",
                        placeholder = "+250 78...",
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SmartTextField(
                        value = cargoDescription,
                        onValueChange = { cargoDescription = it },
                        label = "Package Description (Optional)",
                        placeholder = "e.g. Documents, food, electronics",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }


                // Payment Method
                Text("Mode of Payment", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cash", "Mobile Money", "Wallet").forEach { method ->
                        val isSelected = paymentMethod == method
                        Surface(
                            onClick = { paymentMethod = method },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) SmartYellow else SmartBlackCard
                        ) {
                            Text(
                                text = method,
                                color = if (isSelected) SmartBlack else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bargain Amount Input
                SmartTextField(
                    value = bargainAmount,
                    onValueChange = { bargainAmount = it },
                    label = "Bargain Amount in RWF (Optional)",
                    placeholder = "e.g. 1200",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Estimated Fare Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SmartBlackPaper),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Estimated Fare", color = SmartGrayMedium, fontSize = 12.sp)
                            Text("6.4 km  -  16 mins", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = if (bargainAmount.isNotBlank()) "${bargainAmount} Rwf" else "1,500 Rwf",
                            color = SmartYellow,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SmartButton(
                    text = if (orderType == "Cargo") "Confirm Cargo Delivery" else "Confirm Booking",
                    onClick = {
                        val pickup = selectedPickup
                        val dropoff = selectedDropoff
                        if (pickup == null || dropoff == null) {
                            Toast.makeText(context, "Please set both pickup and drop-off locations", Toast.LENGTH_SHORT).show()
                        } else if (orderType == "Cargo" && (recipientName.isBlank() || recipientPhone.isBlank())) {
                            Toast.makeText(context, "Please enter recipient name and phone", Toast.LENGTH_SHORT).show()
                        } else {
                            val amount = bargainAmount.toIntOrNull()
                            SmartRepository.createRideRequest(
                                pickup, dropoff, paymentMethod, amount,
                                orderType = orderType,
                                recipientName = if (orderType == "Cargo") recipientName else null,
                                recipientPhone = if (orderType == "Cargo") recipientPhone else null,
                                cargoDescription = if (orderType == "Cargo") cargoDescription else null
                            )
                            currentStep = "searching"
                        }
                    }
                )
            }
        }

        // STEP 2: SEARCHING / DRIVER SELECTION
        if (currentStep == "searching") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                InteractiveKigaliMap(
                    pickup = selectedPickup,
                    dropoff = selectedDropoff,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Available Drivers Nearby", color = SmartYellow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SmartYellow, strokeWidth = 2.dp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val driver = selectedPickup?.let { pickup ->
                            nearbyDrivers.minByOrNull {
                                val dLat = it.currentLocation.latitude - pickup.latitude
                                val dLon = it.currentLocation.longitude - pickup.longitude
                                (dLat * dLat) + (dLon * dLon)
                            }
                        }

                        if (driver == null) {
                            Text(
                                "No drivers online nearby right now. Please wait...",
                                color = SmartGrayMedium,
                                fontSize = 13.sp
                            )
                        } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SmartBlackPaper,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = SmartYellow, modifier = Modifier.size(46.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = SmartBlack)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(driver.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${driver.vehicleType}  -  ${driver.plateNumber}", color = SmartYellow, fontSize = 12.sp)
                                    RatingStarsRow(rating = driver.rating, starSize = 12)
                                }

                                Text("Nearby", color = SmartSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SmartButton(
                                text = "Cancel",
                                onClick = { currentStep = "input" },
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            )
                            SmartButton(
                                text = "Book Driver",
                                onClick = {
                                    SmartRepository.updateActiveRideStatus(RideStatus.DRIVER_ASSIGNED)
                                    currentStep = "active"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // STEP 3: ACTIVE RIDE
        if (currentStep == "active") {
            val trip = activeRide ?: SmartRepository.tripHistory.value.first()
            var isPaused by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Map
                InteractiveKigaliMap(
                    pickup = trip.pickup,
                    dropoff = trip.dropoff,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Active Ride Controls Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPaused) SmartError else SmartYellow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isPaused) "TRIP ON HOLD / PAUSED" else "THE DRIVER HAS ARRIVED",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = SmartYellow, modifier = Modifier.size(50.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = SmartBlack, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trip.driverName ?: "Kamana Emmanuel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(trip.vehiclePlate, color = SmartYellow, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                RatingStarsRow(rating = if (trip.ratingGiven > 0) trip.ratingGiven.toFloat() else trip.driverRating, starSize = 14)
                            }
                            IconButton(onClick = onOpenChatClick) {
                                Icon(Icons.Filled.Chat, contentDescription = "Chat with driver", tint = SmartYellow)
                            }
                            IconButton(
                                onClick = { Toast.makeText(context, "Calling driver at +250 789 001 122", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier
                                    .background(SmartBlackPaper, CircleShape)
                                    .border(1.dp, SmartYellow, CircleShape)
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = "Call", tint = SmartYellow)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Pickup Point", color = SmartGrayMedium, fontSize = 11.sp)
                                Text(trip.pickup.address, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Dropoff Point", color = SmartGrayMedium, fontSize = 11.sp)
                                Text(trip.dropoff.address, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SmartButton(
                                text = if (isPaused) "Resume" else "Pause",
                                onClick = {
                                    isPaused = !isPaused
                                    SmartRepository.updateActiveRideStatus(if (isPaused) RideStatus.PAUSED else RideStatus.IN_PROGRESS)
                                },
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            )
                            SmartButton(
                                text = "Complete Ride",
                                onClick = {
                                    SmartRepository.updateActiveRideStatus(RideStatus.COMPLETED)
                                    currentStep = "invoice"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // STEP 4: INVOICE SCREEN
        if (currentStep == "invoice") {
            val trip = activeRide ?: SmartRepository.tripHistory.value.first()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape,
                        color = SmartYellow
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Receipt, contentDescription = null, tint = SmartBlack, modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("TRIP INVOICE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Payment summary for your recent trip", color = SmartGrayMedium, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pickup Location:", color = SmartGrayMedium, fontSize = 13.sp)
                                Text(trip.pickup.address, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Dropoff Location:", color = SmartGrayMedium, fontSize = 13.sp)
                                Text(trip.dropoff.address, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment Method:", color = SmartGrayMedium, fontSize = 13.sp)
                                Text(trip.paymentMethod, color = SmartYellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SmartGrayDark)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Trip Fare", color = Color.White, fontSize = 14.sp)
                                Text("%,d Rwf".format(trip.fareRwf), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Waiting Fee", color = Color.White, fontSize = 14.sp)
                                Text("0 Rwf", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SmartYellow)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Amount", color = SmartYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text("%,d Rwf".format(trip.fareRwf), color = SmartYellow, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                SmartButton(
                    text = "Pay Now & Rate Driver",
                    onClick = { currentStep = "rating" }
                )
            }
        }

        // STEP 5: RATING & FEEDBACK
        if (currentStep == "rating") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("How was your trip?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Your feedback keeps Smart Motos safe and reliable", color = SmartGrayMedium, fontSize = 13.sp, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(24.dp))

                    RatingStarsRow(
                        rating = selectedRating.toFloat(),
                        starSize = 36,
                        onStarSelected = { selectedRating = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("What went well?", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    val tags = listOf("Safe Driver", "Clean Helmet", "On Time", "Friendly", "Smooth Ride")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            val isSelected = selectedTags.contains(tag)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                                },
                                label = { Text(tag) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SmartYellow,
                                    selectedLabelColor = SmartBlack,
                                    containerColor = SmartBlackCard,
                                    labelColor = SmartBlack
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    SmartTextField(
                        value = ratingComment,
                        onValueChange = { ratingComment = it },
                        label = "Add a comment (optional)",
                        placeholder = "Write a review for your driver...",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SmartButton(
                    text = "Submit Rating",
                    onClick = {
                        SmartRepository.submitRating(selectedRating, ratingComment)
                        Toast.makeText(context, "Thank you for rating your trip!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// PASSENGER ACCOUNT & PROFILE SCREEN
// ----------------------------------------------------
@Composable
fun PassengerProfileScreen(
    onTopUpClick: () -> Unit,
    onOpenSupportClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by SmartRepository.currentUser.collectAsState()

    var name by remember { mutableStateOf(currentUser.name) }
    var email by remember { mutableStateOf(currentUser.email) }
    var phone by remember { mutableStateOf(currentUser.phone) }
    var showReferDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Yellow Header Banner
        Surface(
            color = SmartYellow,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = SmartBlack,
                    border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Avatar",
                            tint = SmartYellow,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentUser.name,
                    color = SmartBlack,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = currentUser.email,
                    color = SmartBlack.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Wallet Card
            WalletBalanceCard(
                balanceRwf = currentUser.walletBalanceRwf,
                onTopUpClick = onTopUpClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Account Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))

            SmartTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SmartTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SmartTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone Number",
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            SmartButton(
                text = "Save Profile Changes",
                onClick = {
                    SmartRepository.updateUserProfile(name, email, phone)
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Options List
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SmartBlackCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ProfileOptionItem(
                        icon = Icons.Filled.Share,
                        title = "Refer a Friend",
                        onClick = { showReferDialog = true }
                    )
                    HorizontalDivider(color = SmartBlackPaper)
                    ProfileOptionItem(
                        icon = Icons.Filled.CreditCard,
                        title = "Card and Bank Settings",
                        onClick = { Toast.makeText(context, "Bank cards configured: MASTERCARD **** 657", Toast.LENGTH_SHORT).show() }
                    )
                    HorizontalDivider(color = SmartBlackPaper)
                    ProfileOptionItem(
                        icon = Icons.Filled.HeadsetMic,
                        title = "Customer Support",
                        onClick = onOpenSupportClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSignOutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmartError.copy(alpha = 0.2f),
                    contentColor = SmartError
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Logout, contentDescription = null, tint = SmartError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showReferDialog) {
        AlertDialog(
            onDismissRequest = { showReferDialog = false },
            title = { Text("Refer a Friend via", color = SmartBlack, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("WhatsApp", "Facebook", "SMS", "Copy Invite Link").forEach { channel ->
                        Surface(
                            onClick = {
                                Toast.makeText(context, "Referral link generated & shared to $channel!", Toast.LENGTH_SHORT).show()
                                showReferDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = SmartBlackCard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = channel,
                                color = SmartYellow,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = SmartBlackPaper
        )
    }
}

@Composable
fun ProfileOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SmartYellow, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SmartGrayMedium)
    }
}

// ----------------------------------------------------
// WALLET TOP UP MODAL / SCREEN
// ----------------------------------------------------
@Composable
fun TopUpModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf("5000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Top Up Wallet", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = SmartYellow)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mobile Money (MoMo) Instructions:", color = SmartYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("1. Send amount to MoMo Number: 0790246983", color = Color.White, fontSize = 13.sp)
                Text("2. Enter the exact RWF amount below", color = Color.White, fontSize = 13.sp)
                Text("3. Admin will verify and credit your wallet", color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SmartTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = "Amount in RWF",
            placeholder = "5000",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        SmartButton(
            text = "Send Screenshot on WhatsApp",
            isPrimary = false,
            onClick = {
                Toast.makeText(context, "Opening WhatsApp confirmation...", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SmartButton(
            text = "I've Sent Payment",
            onClick = {
                val amt = amountText.toIntOrNull() ?: 5000
                SmartRepository.requestWalletTopup(amt)
                Toast.makeText(context, "Top-up request sent to Admin for verification!", Toast.LENGTH_LONG).show()
                onDismiss()
            }
        )
    }
}

// ----------------------------------------------------
// CUSTOMER SUPPORT LIVE CHAT
// ----------------------------------------------------
@Composable
fun SupportChatScreen(
    onNavigateBack: () -> Unit
) {
    val messages by SmartRepository.supportMessages.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TopBar
        Surface(color = SmartBlackPaper, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SmartYellow)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Customer Support", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Online  -  Typically replies instantly", color = SmartSuccess, fontSize = 11.sp)
                }
            }
        }

        // Chat List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.isFromUser
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 16.dp
                        ),
                        color = if (isUser) SmartYellow else SmartBlackCard,
                        contentColor = if (isUser) SmartBlack else SmartGrayMedium,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(msg.text, fontSize = 14.sp)
                            Text(
                                text = msg.time,
                                fontSize = 10.sp,
                                color = if (isUser) SmartBlack.copy(alpha = 0.7f) else SmartGrayMedium,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Input row
        Surface(color = SmartBlackPaper, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type your message...", color = SmartGrayMedium) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SmartBlackCard,
                        unfocusedContainerColor = SmartBlackCard,
                        focusedBorderColor = SmartYellow,
                        unfocusedBorderColor = SmartGrayDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            SmartRepository.addSupportMessage(inputText)
                            inputText = ""
                        }
                    },
                    containerColor = SmartYellow,
                    contentColor = SmartBlack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}




