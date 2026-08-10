package com.example.ui.screens
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*

// ----------------------------------------------------
// DRIVER MAIN DASHBOARD
// ----------------------------------------------------
@Composable
fun DriverHomeScreen(
    onNavigateAnalytics: () -> Unit,
    onNavigateVehicle: () -> Unit,
    onOpenChatClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    fun fetchAndPushDriverLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        SmartRepository.updateDriverLocation(location.latitude, location.longitude)
                    }
                }
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchAndPushDriverLocation()
        } else {
            Toast.makeText(context, "Location permission is needed to receive nearby ride requests", Toast.LENGTH_LONG).show()
        }
    }

    val isOnline by SmartRepository.isDriverOnline.collectAsState()
    LaunchedEffect(isOnline) {
        while (isOnline) {
            fetchAndPushDriverLocation()
            delay(15000L)
        }
    }
    val incomingRequest by SmartRepository.incomingDriverRequest.collectAsState()
    val activeRide by SmartRepository.activeRide.collectAsState()

    var selectedFilter by remember { mutableStateOf("Today") }
    var driverStep by remember { mutableStateOf("home") } // "home", "request_popup", "nav_pickup", "in_trip"
    var isPaused by remember { mutableStateOf(false) }

    val stats = DriverEarningsSummary()

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

            // Online / Offline Header Bar
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isOnline) SmartSuccess else SmartGrayDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) SmartSuccess else SmartError)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isOnline) "You're Online" else "You're Offline",
                                color = SmartBlack,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isOnline) "Ready to receive nearby ride requests" else "Go online to start earning",
                                color = SmartGrayMedium,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isOnline,
                        onCheckedChange = { online ->
                            SmartRepository.toggleDriverOnline(online)
                            if (online) {
                                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    fetchAndPushDriverLocation()
                                } else {
                                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                }
                            }
                            Toast.makeText(context, if (online) "Driver status set to ONLINE" else "Driver status set to OFFLINE", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SmartYellow,
                            uncheckedThumbColor = SmartGrayMedium,
                            uncheckedTrackColor = SmartGrayDark
                        )
                    )
                }
            }

            // Incoming Ride Request Alert Overlay
            if (isOnline && (incomingRequest != null || driverStep == "request_popup")) {
                val trip = incomingRequest ?: SmartRepository.tripHistory.value.first()
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SmartYellow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = SmartBlack)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("INCOMING RIDE REQUEST", color = SmartBlack, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Surface(
                                shape = CircleShape,
                                color = SmartBlack,
                                contentColor = SmartYellow
                            ) {
                                Text("30s", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Rider: ${trip.riderName}", color = SmartBlack, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Pickup: ${trip.pickup.address}", color = SmartBlack.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Dropoff: ${trip.dropoff.address}", color = SmartBlack.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Estimated Fare:", color = SmartBlack, fontSize = 13.sp)
                            Text("%,d Rwf".format(trip.fareRwf), color = SmartBlack, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { SmartRepository.updateActiveRideStatus(RideStatus.CANCELLED) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SmartBlackCard, contentColor = SmartBlack)
                            ) {
                                Text("Reject", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    SmartRepository.updateActiveRideStatus(RideStatus.DRIVER_ASSIGNED)
                                    driverStep = "nav_pickup"
                                    Toast.makeText(context, "Ride accepted! Navigating to passenger...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SmartBlack, contentColor = SmartYellow)
                            ) {
                                Text("Accept Ride", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Driver Navigation View
            if (driverStep == "nav_pickup" || driverStep == "in_trip") {
                val trip = activeRide ?: SmartRepository.tripHistory.value.first()
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (driverStep == "nav_pickup") "NAVIGATING TO PICKUP" else "TRIP TO DESTINATION",
                            color = SmartYellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        InteractiveKigaliMap(
                            pickup = trip.pickup,
                            dropoff = trip.dropoff,
                            modifier = Modifier.height(180.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Rider: ${trip.riderName}", color = SmartBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Destination: ${trip.dropoff.address}", color = SmartGrayMedium, fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(10.dp))
                        SmartButton(
                            text = "Chat with Rider",
                            onClick = onOpenChatClick,
                            isPrimary = false
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (driverStep == "nav_pickup") {
                            SmartButton(
                                text = "Arrived at Pickup",
                                onClick = {
                                    SmartRepository.updateActiveRideStatus(RideStatus.DRIVER_ARRIVED)
                                    driverStep = "in_trip"
                                    Toast.makeText(context, "Passenger notified: Driver has arrived!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SmartButton(
                                    text = if (isPaused) "Resume" else "Pause",
                                    onClick = { isPaused = !isPaused },
                                    isPrimary = false,
                                    modifier = Modifier.weight(1f)
                                )
                                SmartButton(
                                    text = "Complete Trip",
                                    onClick = {
                                        SmartRepository.updateActiveRideStatus(RideStatus.COMPLETED)
                                        driverStep = "home"
                                        Toast.makeText(context, "Trip completed! Fare added to earnings.", Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Earnings & Time Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Earnings & Performance", color = SmartBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNavigateAnalytics) {
                    Icon(Icons.Filled.BarChart, contentDescription = "Analytics", tint = SmartYellow)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SmartBlackCard)
                    .padding(4.dp)
            ) {
                listOf("Today", "Week", "Month", "All time").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SmartYellow else Color.Transparent)
                            .clickable { selectedFilter = filter }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) SmartBlack else SmartGrayMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Driver Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox("Trips", "${stats.totalTrips}", Modifier.weight(1f))
                StatBox("Online", "${stats.hoursOnline} hrs", Modifier.weight(1f))
                StatBox(
                    "Earned",
                    when (selectedFilter) {
                        "Today" -> "%,d Rwf".format(stats.todayRwf)
                        "Week" -> "%,d Rwf".format(stats.weekRwf)
                        else -> "%,d Rwf".format(stats.monthRwf)
                    },
                    Modifier.weight(1.3f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Shortcut Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = onNavigateAnalytics,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = SmartBlackCard
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ShowChart, contentDescription = null, tint = SmartYellow)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Demand Analytics", color = SmartBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Surface(
                    onClick = onNavigateVehicle,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = SmartBlackCard
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.TwoWheeler, contentDescription = null, tint = SmartYellow)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Vehicle Details", color = SmartBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = SmartYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = SmartGrayMedium, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ----------------------------------------------------
// DRIVER ANALYTICS & DEMAND HEATMAP
// ----------------------------------------------------
@Composable
fun DriverAnalyticsScreen(
    onNavigateBack: () -> Unit
) {
    var period by remember { mutableStateOf("Week") }

    val hotspots = listOf(
        DemandZone("Kimironko Market & Bus Park", -1.9515, 30.1265, 0.95f, 142),
        DemandZone("Nyabugogo Transport Hub", -1.9385, 30.0460, 0.88f, 120),
        DemandZone("Kigali Heights & Convention Centre", -1.9546, 30.0931, 0.75f, 98),
        DemandZone("African Leadership University (ALU)", -1.9390, 30.1305, 0.65f, 74)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(color = SmartBlackPaper, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SmartYellow)
                }
                Text("Demand & Earnings Analytics", color = SmartBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Period Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SmartBlackCard)
                    .padding(4.dp)
            ) {
                listOf("Day", "Week", "Month").forEach { p ->
                    val isSelected = period == p
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SmartYellow else Color.Transparent)
                            .clickable { period = p }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p,
                            color = if (isSelected) SmartBlack else SmartGrayMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chart Visualizer Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Hourly Demand & Peak Hours", color = SmartBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Highest passenger requests occur around 07:30 AM & 17:30 PM", color = SmartGrayMedium, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Custom Bar Chart Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val barValues = listOf(0.3f, 0.85f, 0.95f, 0.5f, 0.4f, 0.9f, 0.75f, 0.35f)
                        val barWidth = width / (barValues.size * 1.8f)

                        barValues.forEachIndexed { i, ratio ->
                            val x = i * (barWidth * 1.8f) + barWidth * 0.4f
                            val barHeight = height * ratio
                            val y = height - barHeight

                            drawRect(
                                color = if (ratio > 0.8f) SmartYellow else SmartYellow.copy(alpha = 0.4f),
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("06:00", color = SmartGrayMedium, fontSize = 10.sp)
                        Text("09:00", color = SmartYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("12:00", color = SmartGrayMedium, fontSize = 10.sp)
                        Text("18:00", color = SmartYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("22:00", color = SmartGrayMedium, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Popular Pickup Hotspots in Kigali", color = SmartBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))

            hotspots.forEachIndexed { index, zone ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${index + 1}",
                                color = SmartYellow,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(zone.name, color = SmartBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("High Demand ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ ${zone.bookingCount} bookings today", color = SmartGrayMedium, fontSize = 12.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SmartYellow.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${(zone.demandLevel * 100).toInt()}% Peak",
                                color = SmartYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ----------------------------------------------------
// DRIVER VEHICLE & LICENSE DETAILS
// ----------------------------------------------------
@Composable
fun DriverVehicleDetailsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var make by remember { mutableStateOf("TVS HLX") }
    var model by remember { mutableStateOf("125 Moto") }
    var year by remember { mutableStateOf("2023") }
    var plateNumber by remember { mutableStateOf("JDM PL8S") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(color = SmartBlackPaper, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SmartYellow)
                }
                Text("Vehicle & License Details", color = SmartBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SmartSuccess)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = SmartSuccess, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Status: VERIFIED DRIVER", color = SmartSuccess, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("License and vehicle documents approved by Admin", color = SmartGrayMedium, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SmartTextField(
                value = make,
                onValueChange = { make = it },
                label = "Vehicle Make",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SmartTextField(
                value = model,
                onValueChange = { model = it },
                label = "Model",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SmartTextField(
                value = year,
                onValueChange = { year = it },
                label = "Year",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SmartTextField(
                value = plateNumber,
                onValueChange = { plateNumber = it },
                label = "Plate Number",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            SmartButton(
                text = "Save Vehicle Details",
                onClick = {
                    Toast.makeText(context, "Vehicle details updated!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
            )
        }
    }
}
@Composable
fun DriverOfferCountdownRow(
    tripId: String,
    offerExpiresAtMs: Long,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var secondsLeft by remember(tripId) { mutableStateOf(((offerExpiresAtMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)) }
    LaunchedEffect(tripId) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft = ((offerExpiresAtMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        }
        if (secondsLeft <= 0L) {
            SmartRepository.expireRideOfferIfTimedOut(tripId, offerExpiresAtMs)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { SmartRepository.declineRideOffer(tripId); onDecline() }) { Text("Decline (${secondsLeft}s)") }
        Button(onClick = { SmartRepository.acceptRideOffer(tripId); onAccept() }) { Text("Accept") }
    }
}
