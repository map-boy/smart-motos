package com.nihonor.smartmotosapp.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.nihonor.smartmotosapp.data.Fare
import com.nihonor.smartmotosapp.data.RideStatus
import com.nihonor.smartmotosapp.data.RideTrip
import com.nihonor.smartmotosapp.data.SmartRepository
import kotlinx.coroutines.launch
import java.util.Locale

private val orderTypes = listOf("Passenger", "Cargo")
private val paymentMethods = listOf("MTN Mobile Money", "Airtel Money", "Wallet")

@Composable
fun PassengerHomeScreen(modifier: Modifier = Modifier) {
    val currentUser by SmartRepository.currentUser.collectAsState()
    val nearbyDrivers by SmartRepository.nearbyDrivers.collectAsState()
    val activeRide by SmartRepository.activeRide.collectAsState()
    val scope = rememberCoroutineScope()

    var pickup by remember { mutableStateOf(SmartRepository.kigaliLocations[0]) }
    var dropoff by remember { mutableStateOf(SmartRepository.kigaliLocations[2]) }
    var orderType by remember { mutableStateOf(orderTypes[0]) }
    var paymentMethod by remember { mutableStateOf(paymentMethods[0]) }
    var bargainText by remember { mutableStateOf("") }
    var isRequesting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val distanceKm = Fare.distanceKm(pickup, dropoff)
    val quotedFare = Fare.quoteRwf(distanceKm)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WalletHeader(
            name = currentUser?.name.orEmpty().ifEmpty { "Passenger" },
            balanceRwf = currentUser?.walletBalanceRwf ?: 0
        )

        val ride = activeRide
        if (ride != null) {
            ActiveRideCard(ride = ride, onCancel = { SmartRepository.cancelRide(ride.id) })
            return@Column
        }

        ChipRow(
            label = "Order Type",
            options = orderTypes,
            selected = orderType,
            onSelect = { orderType = it },
            display = { if (it == "Passenger") "Ride Passenger" else "Send Package" }
        )

        SectionCard(title = "Trip Route") {
            OutlinedTextField(
                value = pickup.address,
                onValueChange = { pickup = pickup.copy(address = it) },
                label = { Text("Pickup location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dropoff.address,
                onValueChange = { dropoff = dropoff.copy(address = it) },
                label = { Text("Drop-off location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()
            Text("Quick Kigali Locations", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmartRepository.kigaliLocations.forEach { loc ->
                    AssistChip(
                        onClick = { dropoff = loc },
                        label = { Text(loc.address, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        SectionCard(title = "Fare & Payment") {
            LabelledRow("Estimated Distance", String.format(Locale.US, "%.1f km", distanceKm))
            LabelledRow(
                "Standard Fare",
                "$quotedFare RWF",
                valueColor = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = bargainText,
                onValueChange = { input -> bargainText = input.filter { it.isDigit() } },
                label = { Text("Offer custom fare (optional)") },
                placeholder = { Text("e.g. 1500") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ChipRow(
                label = "Payment Method",
                options = paymentMethods,
                selected = paymentMethod,
                onSelect = { paymentMethod = it }
            )
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                isRequesting = true
                errorMessage = null
                scope.launch {
                    val result = SmartRepository.createRide(
                        pickup = pickup,
                        dropoff = dropoff,
                        orderType = orderType,
                        paymentMethod = paymentMethod,
                        bargainAmountRwf = bargainText.toIntOrNull()
                    )
                    isRequesting = false
                    result.onSuccess { bargainText = "" }
                    result.onFailure { errorMessage = it.message ?: "Could not request a ride" }
                }
            },
            enabled = !isRequesting,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isRequesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Request Moto Now", fontWeight = FontWeight.Bold)
            }
        }

        if (nearbyDrivers.isNotEmpty()) {
            SectionCard(title = "Active Drivers Nearby (${nearbyDrivers.size})") {
                nearbyDrivers.forEach { driver ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(driver.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                driver.plateNumber.ifEmpty { driver.vehicleType },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(String.format(Locale.US, "%.1f", driver.rating), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveRideCard(ride: RideTrip, onCancel: () -> Unit) {
    SectionCard(title = "Your Ride") {
        LabelledRow("Status", ride.status.name.replace('_', ' '))
        LabelledRow("From", ride.pickup.address)
        LabelledRow("To", ride.dropoff.address)
        LabelledRow("Fare", "${ride.fareRwf} RWF")
        if (!ride.driverName.isNullOrEmpty()) {
            LabelledRow("Driver", ride.driverName!!)
            LabelledRow("Plate", ride.vehiclePlate)
        }
        if (ride.status == RideStatus.REQUESTED ||
            ride.status == RideStatus.SEARCHING ||
            ride.status == RideStatus.OFFERED
        ) {
            Text(
                "Looking for a nearby driver...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        // COMPLETED and CANCELLED never reach here: activeRide excludes terminal statuses.
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel Ride")
        }
    }
}

@Composable
fun TripHistoryScreen(modifier: Modifier = Modifier) {
    val trips by SmartRepository.trips.collectAsState()

    if (trips.isEmpty()) {
        EmptyState(modifier, "No trips yet", "Your completed rides will show up here.")
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(trips, key = { it.id }) { trip ->
            SectionCard(title = trip.status.name.replace('_', ' ')) {
                LabelledRow("From", trip.pickup.address)
                LabelledRow("To", trip.dropoff.address)
                LabelledRow("Fare", "${trip.fareRwf} RWF")
                LabelledRow("Payment", trip.paymentMethod)
            }
        }
    }
}

@Composable
fun WalletScreen(modifier: Modifier = Modifier) {
    val currentUser by SmartRepository.currentUser.collectAsState()
    var amountText by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WalletHeader(
            name = currentUser?.name.orEmpty().ifEmpty { "Passenger" },
            balanceRwf = currentUser?.walletBalanceRwf ?: 0
        )

        SectionCard(title = "Top Up Wallet") {
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() } },
                label = { Text("Amount (RWF)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "An admin reviews every top-up before the balance changes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    val amount = amountText.toIntOrNull() ?: return@Button
                    SmartRepository.requestWalletTopup(amount)
                    amountText = ""
                    confirmation = "Top-up request sent for review."
                },
                enabled = (amountText.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request Top Up")
            }
            confirmation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
