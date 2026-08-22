package com.nihonor.smartmotosapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nihonor.smartmotosapp.data.SmartRepository
import com.nihonor.smartmotosapp.data.UserRole
import kotlinx.coroutines.launch

private val creatableRoles = listOf("passenger", "driver", "admin")

@Composable
fun AdminHomeScreen(modifier: Modifier = Modifier) {
    val allUsers by SmartRepository.allUsers.collectAsState()
    val pendingTopups by SmartRepository.pendingTopups.collectAsState()
    val pendingDrivers by SmartRepository.pendingDrivers.collectAsState()

    // Admin listeners are separate from the per-user ones and are not attached at login,
    // so the screen owns their lifetime.
    DisposableEffect(Unit) {
        SmartRepository.attachAdminListeners()
        onDispose { SmartRepository.detachAdminListeners() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), "Total Users", allUsers.size.toString())
            StatCard(Modifier.weight(1f), "Pending Topups", pendingTopups.size.toString())
            StatCard(Modifier.weight(1f), "Pending Drivers", pendingDrivers.size.toString())
        }

        AddUserCard()

        SectionCard(title = "Pending Driver Applications (${pendingDrivers.size})") {
            if (pendingDrivers.isEmpty()) {
                Text(
                    "No pending driver registrations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                pendingDrivers.forEach { driver ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            driver.name.ifEmpty { "Unnamed Driver" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        LabelledRow("Phone", driver.phone.ifEmpty { "-" })
                        LabelledRow("Vehicle", driver.vehicleType)
                        LabelledRow("License", driver.licenseNumber.ifEmpty { "-" })
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { SmartRepository.rejectDriver(driver.id) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Reject") }
                            Button(
                                onClick = { SmartRepository.approveDriver(driver.id) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Approve") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        SectionCard(title = "Pending Wallet Topups (${pendingTopups.size})") {
            if (pendingTopups.isEmpty()) {
                Text(
                    "No pending top-up requests.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                pendingTopups.forEach { req ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            req.userName.ifEmpty { "User ${req.userId.take(6)}" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        LabelledRow("MoMo", req.momoNumber.ifEmpty { "-" })
                        LabelledRow(
                            "Amount",
                            "${req.amountRwf} RWF",
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { SmartRepository.rejectTopup(req.id) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Reject") }
                            Button(
                                onClick = { SmartRepository.approveTopup(req.id) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Approve") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun AddUserCard() {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(creatableRoles[0]) }
    var isCreating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    SectionCard(title = "Add Manual User") {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ChipRow(
            label = "Role",
            options = creatableRoles,
            selected = role,
            onSelect = { role = it },
            display = { it.replaceFirstChar { c -> c.uppercase() } }
        )

        message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                isCreating = true
                message = null
                scope.launch {
                    // adminCreateUser in functions/src/index.ts creates the Auth account
                    // as well as the profile doc, and rejects the call unless the caller
                    // is an admin.
                    val result = SmartRepository.addManualUser(
                        name = name,
                        email = email,
                        phone = phone,
                        role = UserRole.valueOf(role.uppercase())
                    )
                    isCreating = false
                    result.onSuccess {
                        message = "User created. A password reset link was generated for them."
                        name = ""; email = ""; phone = ""
                    }
                    result.onFailure { message = "Error: ${it.message ?: "could not create user"}" }
                }
            },
            enabled = !isCreating && email.isNotBlank() && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create User", fontWeight = FontWeight.Bold)
            }
        }
    }
}
