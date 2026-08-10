package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.SmartButton
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen() {
    val context = LocalContext.current
    val pendingTopups by SmartRepository.pendingTopups.collectAsState()
    val pendingDrivers by SmartRepository.pendingDrivers.collectAsState()
    val allUsers by SmartRepository.allUsers.collectAsState()

    var activeTab by remember { mutableStateOf("topups") } // "topups" or "drivers"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
    ) {
        // Header
        Surface(color = SmartBlackPaper, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = SmartYellow, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Admin Operations Portal", color = SmartBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { activeTab = "topups" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == "topups") SmartYellow else SmartBlackCard,
                            contentColor = if (activeTab == "topups") Color.White else SmartBlack
                        )
                    ) {
                        Text("Pending Top-ups (${pendingTopups.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { activeTab = "drivers" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == "drivers") SmartYellow else SmartBlackCard,
                            contentColor = if (activeTab == "drivers") Color.White else SmartBlack
                        )
                    ) {
                        Text("Pending Drivers (${pendingDrivers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { activeTab = "users" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == "users") SmartYellow else SmartBlackCard,
                            contentColor = if (activeTab == "users") Color.White else SmartBlack
                        )
                    ) {
                        Text("All Users (${allUsers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeTab == "topups") {
            if (pendingTopups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SmartSuccess, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No pending top-up requests", color = SmartBlack, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingTopups) { req ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(req.userName, color = SmartBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("%,d Rwf".format(req.amountRwf), color = SmartYellow, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("MoMo Number: ${req.momoNumber} u{2022} ${req.timeAgo}", color = SmartGrayMedium, fontSize = 12.sp)

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            SmartRepository.rejectTopup(req.id)
                                            Toast.makeText(context, "Top-up request rejected", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SmartBlackPaper, contentColor = SmartError)
                                    ) {
                                        Text("Reject", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            SmartRepository.approveTopup(req.id)
                                            Toast.makeText(context, "Top-up approved! User wallet credited with %,d Rwf".format(req.amountRwf), Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SmartYellow, contentColor = SmartBlack)
                                    ) {
                                        Text("Approve", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeTab == "drivers") {
            if (pendingDrivers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = SmartSuccess, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All driver applications verified", color = SmartBlack, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingDrivers) { drv ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(drv.name, color = SmartBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Phone: ${drv.phone} u{2022} Provider: ${drv.serviceProvider}", color = SmartGrayMedium, fontSize = 12.sp)
                                Text("Vehicle: ${drv.vehicleType} Ã¢â‚¬Â¢ License: ${drv.licenseNumber}", color = SmartYellow, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Inspection Code: ${drv.inspectionCode}", color = SmartYellow, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            SmartRepository.rejectDriver(drv.id)
                                            Toast.makeText(context, "Driver application rejected", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SmartBlackPaper, contentColor = SmartError)
                                    ) {
                                        Text("Reject", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            SmartRepository.approveDriver(drv.id)
                                            Toast.makeText(context, "Driver verified successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SmartYellow, contentColor = SmartBlack)
                                    ) {
                                        Text("Approve Driver", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (activeTab == "users") {
            AdminUsersTab(allUsers = allUsers)
        }
    }
}

@Composable
fun AdminUsersTab(allUsers: List<UserProfile>) {
    val context = LocalContext.current
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userPendingDeleteId by remember { mutableStateOf<String?>(null) }
    var newUserResetLink by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (allUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users found", color = SmartBlack, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(allUsers) { user ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SmartYellow.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, color = SmartBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(user.email, color = SmartGrayMedium, fontSize = 12.sp)
                                Text("Role: ${user.role}", color = SmartYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(onClick = { userPendingDeleteId = user.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete user", tint = SmartError)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddUserDialog = true },
            containerColor = SmartYellow,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add user")
        }
    }

    if (userPendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { userPendingDeleteId = null },
            title = { Text("Delete user?") },
            text = { Text("This removes the user's profile AND their login access. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    SmartRepository.deleteUser(userPendingDeleteId!!) { success, error ->
                        if (success) {
                            Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to delete user: ${error ?: "unknown error"}", Toast.LENGTH_LONG).show()
                        }
                    }
                    userPendingDeleteId = null
                }) { Text("Delete", color = SmartError) }
            },
            dismissButton = {
                TextButton(onClick = { userPendingDeleteId = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onConfirm = { name, email, phone, role ->
                SmartRepository.addManualUser(name, email, phone, role) { success, resetLink, error ->
                    if (success) {
                        newUserResetLink = resetLink
                    } else {
                        Toast.makeText(context, "Failed to add user: ${error ?: "unknown error"}", Toast.LENGTH_LONG).show()
                    }
                }
                showAddUserDialog = false
            }
        )
    }

    if (newUserResetLink != null) {
        AlertDialog(
            onDismissRequest = { newUserResetLink = null },
            title = { Text("User created") },
            text = {
                Column {
                    Text("Share this link with the new user so they can set their password:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(newUserResetLink ?: "", color = SmartYellow, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { newUserResetLink = null }) { Text("Done") }
            }
        )
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, UserRole) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.PASSENGER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add user") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.values().forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && email.isNotBlank()) {
                    onConfirm(name.trim(), email.trim(), phone.trim(), role)
                }
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


