package com.nihonor.smartmotosapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.nihonor.smartmotosapp.data.SmartRepository
import com.nihonor.smartmotosapp.data.UserRole

private data class Tab(val label: String, val icon: ImageVector)

private val passengerTabs = listOf(
    Tab("Home", Icons.Default.Home),
    Tab("Trips", Icons.Default.List),
    Tab("Wallet", Icons.Default.ShoppingCart),
    Tab("Profile", Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {
    val currentUser by SmartRepository.currentUser.collectAsState()
    var selected by remember { mutableIntStateOf(0) }

    // Driver and admin flows exist on iOS but have not been ported yet. Showing a
    // passenger booking form to a driver would be worse than saying so plainly.
    val role = currentUser?.role ?: UserRole.PASSENGER
    if (role != UserRole.PASSENGER) {
        NotPortedYet(role)
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Smart Motos") }) },
        bottomBar = {
            NavigationBar {
                passengerTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        val inner = Modifier.padding(padding)
        when (selected) {
            0 -> PassengerHomeScreen(inner)
            1 -> TripHistoryScreen(inner)
            2 -> WalletScreen(inner)
            else -> ProfileScreen(inner)
        }
    }
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val currentUser by SmartRepository.currentUser.collectAsState()
    val user = currentUser

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                ProfileRow("Name", user?.name.orEmpty().ifEmpty { "-" })
                ProfileRow("Phone", user?.phone.orEmpty().ifEmpty { "-" })
                ProfileRow("Email", user?.email.orEmpty().ifEmpty { "-" })
                ProfileRow("Role", (user?.role ?: UserRole.PASSENGER).name.lowercase().replaceFirstChar { it.uppercase() })
                ProfileRow("Wallet", "${user?.walletBalanceRwf ?: 0} RWF")
            }
        }

        Button(
            onClick = {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    FirebaseAuth.getInstance().signOut()
                } else {
                    // Drop the token first: the write needs this user's credentials, and
                    // firestore.rules rejects it once auth is gone. Otherwise the device
                    // keeps receiving pushes for an account no longer signed in on it.
                    SmartRepository.clearFcmToken(uid) {
                        FirebaseAuth.getInstance().signOut()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Out", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NotPortedYet(role: UserRole) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "${role.name.lowercase().replaceFirstChar { it.uppercase() }} app not available yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This role works on iOS. The Android screens for it have not been built yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) FirebaseAuth.getInstance().signOut()
            else SmartRepository.clearFcmToken(uid) { FirebaseAuth.getInstance().signOut() }
        }) {
            Text("Sign Out")
        }
    }
}
