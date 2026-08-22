package com.nihonor.smartmotosapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nihonor.smartmotosapp.data.RideStatus
import com.nihonor.smartmotosapp.data.RideTrip
import com.nihonor.smartmotosapp.data.SmartRepository

@Composable
fun DriverHomeScreen(modifier: Modifier = Modifier) {
    val currentUser by SmartRepository.currentUser.collectAsState()
    val isOnline by SmartRepository.isDriverOnline.collectAsState()
    val incoming by SmartRepository.incomingDriverRequest.collectAsState()
    val active by SmartRepository.driverActiveRide.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(title = "Driver Portal") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        currentUser?.name.orEmpty().ifEmpty { "Driver" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isOnline) "Online - ready for rides" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOnline) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
                Switch(
                    checked = isOnline,
                    onCheckedChange = { SmartRepository.toggleDriverOnline(it) }
                )
            }
            LabelledRow("Vehicle", currentUser?.vehicleType.orEmpty().ifEmpty { "Bike" })
            LabelledRow("Plate", currentUser?.licenseNumber.orEmpty().ifEmpty { "-" })
        }

        val offer = incoming
        if (offer != null) {
            IncomingOfferCard(
                trip = offer,
                onAccept = { SmartRepository.acceptTrip(offer.id) },
                onDecline = { SmartRepository.declineTrip(offer.id) }
            )
        }

        val ride = active
        if (ride != null) {
            ActiveDriverRideCard(ride)
        } else if (offer == null) {
            EmptyState(
                Modifier.padding(vertical = 32.dp),
                if (isOnline) "Waiting for ride requests" else "You are offline",
                if (isOnline) "Offers appear here the moment dispatch picks you."
                else "Go online to start receiving ride offers."
            )
        }

        OutlinedButton(
            onClick = { SmartRepository.updateDriverLocation(-1.9546, 30.0931) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update GPS position to Kigali Centre")
        }
    }
}

@Composable
private fun IncomingOfferCard(trip: RideTrip, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "New trip offered",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${trip.fareRwf} RWF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
            LabelledRow("Rider", trip.riderName.ifEmpty { "Passenger" })
            LabelledRow("Pickup", trip.pickup.address)
            LabelledRow("Drop-off", trip.dropoff.address)
            LabelledRow("Payment", trip.paymentMethod)

            // dispatch.ts gives a driver 20s before the sweep re-offers the trip.
            Text(
                "This offer expires shortly if you do not respond.",
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                    Text("Decline")
                }
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                    Text("Accept Ride", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ActiveDriverRideCard(ride: RideTrip) {
    SectionCard(title = "Active Ride") {
        LabelledRow("Status", ride.status.name.replace('_', ' '))
        LabelledRow("Passenger", ride.riderName.ifEmpty { "Passenger" })
        LabelledRow("From", ride.pickup.address)
        LabelledRow("To", ride.dropoff.address)
        LabelledRow("Fare", "${ride.fareRwf} RWF (${ride.paymentMethod})")

        HorizontalDivider()

        // One button per state, so a driver can only ever take the next legal step.
        when (ride.status) {
            RideStatus.DRIVER_ASSIGNED -> AdvanceButton("Arrived at Pickup") {
                SmartRepository.updateTripStatus(ride.id, RideStatus.DRIVER_ARRIVED)
            }
            RideStatus.DRIVER_ARRIVED -> AdvanceButton("Start Trip") {
                SmartRepository.updateTripStatus(ride.id, RideStatus.IN_PROGRESS)
            }
            RideStatus.IN_PROGRESS -> AdvanceButton("Complete Trip") {
                SmartRepository.updateTripStatus(ride.id, RideStatus.COMPLETED)
            }
            else -> Unit
        }
    }
}

@Composable
private fun AdvanceButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}
