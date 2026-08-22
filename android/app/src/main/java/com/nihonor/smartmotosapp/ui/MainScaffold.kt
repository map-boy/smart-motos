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
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.nihonor.smartmotosapp.data.SmartRepository
import com.nihonor.smartmotosapp.data.UserRole

private data class Tab(
    val label: String,
    val icon: ImageVector,
    val content: @Composable (Modifier) -> Unit
)

private fun tabsFor(role: UserRole): List<Tab> = when (role) {
    UserRole.PASSENGER -> listOf(
        Tab("Home", Icons.Default.Home) { PassengerHomeScreen(it) },
        Tab("Trips", Icons.Default.List) { TripHistoryScreen(it) },
        Tab("Wallet", Icons.Default.ShoppingCart) { WalletScreen(it) },
        Tab("Profile", Icons.Default.Person) { ProfileScreen(it) }
    )
    UserRole.DRIVER -> listOf(
        Tab("Dashboard", Icons.Default.Home) { DriverHomeScreen(it) },
        Tab("Profile", Icons.Default.Person) { ProfileScreen(it) }
    )
    UserRole.ADMIN -> listOf(
        Tab("Control", Icons.Default.Home) { AdminHomeScreen(it) },
        Tab("Profile", Icons.Default.Person) { ProfileScreen(it) }
    )
}

private fun titleFor(role: UserRole): String = when (role) {
    UserRole.PASSENGER -> "Smart Motos"
    UserRole.DRIVER -> "Driver Dashboard"
    UserRole.ADMIN -> "Admin Control"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {
    val currentUser by SmartRepository.currentUser.collectAsState()
    val role = currentUser?.role ?: UserRole.PASSENGER
    val tabs = tabsFor(role)

    var selected by remember { mutableIntStateOf(0) }
    // An admin promoting themselves mid-session changes the tab count under us; clamp
    // rather than let a stale index run off the end of the new list.
    val index = selected.coerceIn(0, tabs.lastIndex)

    Scaffold(
        topBar = { TopAppBar(title = { Text(titleFor(role)) }) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = index == i,
                        onClick = { selected = i },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        tabs[index].content(Modifier.padding(padding))
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
        SectionCard(title = "Profile") {
            ProfileRow("Name", user?.name.orEmpty().ifEmpty { "-" })
            ProfileRow("Phone", user?.phone.orEmpty().ifEmpty { "-" })
            ProfileRow("Email", user?.email.orEmpty().ifEmpty { "-" })
            ProfileRow(
                "Role",
                (user?.role ?: UserRole.PASSENGER).name.lowercase().replaceFirstChar { it.uppercase() }
            )
            ProfileRow("Wallet", "${user?.walletBalanceRwf ?: 0} RWF")
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
