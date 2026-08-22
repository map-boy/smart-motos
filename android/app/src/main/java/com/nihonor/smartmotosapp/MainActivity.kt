package com.nihonor.smartmotosapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.nihonor.smartmotosapp.auth.AuthViewModel
import com.nihonor.smartmotosapp.auth.OtpState
import com.nihonor.smartmotosapp.data.SmartMessagingService
import com.nihonor.smartmotosapp.data.SmartRepository
import com.nihonor.smartmotosapp.ui.MainScaffold

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    // Result is ignored on purpose: a declined prompt just means no notifications,
    // which must not block the app from starting.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SmartMessagingService.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(authViewModel)
                }
            }
        }
    }
}

@Composable
fun AppRoot(authViewModel: AuthViewModel) {
    val auth = FirebaseAuth.getInstance()
    var uid by remember { mutableStateOf(auth.currentUser?.uid) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            uid = fa.currentUser?.uid
            if (uid != null) {
                SmartRepository.attachListeners(uid!!)
                SmartRepository.registerFcmToken()
            } else {
                SmartRepository.detachListeners()
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (uid == null) {
        LoginScreen(authViewModel)
    } else {
        MainScaffold()
    }
}

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val state by authViewModel.otpState.collectAsState()
    val activity = androidx.compose.ui.platform.LocalContext.current as android.app.Activity

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SmartMotos", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        when (state) {
            is OtpState.CodeSent -> {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Enter code") })
                Spacer(Modifier.height(12.dp))
                Button(onClick = { authViewModel.verifyOtp(code) }) { Text("Verify") }
            }
            is OtpState.Error -> {
                Text((state as OtpState.Error).message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (+250...)") })
                Spacer(Modifier.height(12.dp))
                Button(onClick = { authViewModel.sendOtp(phone, activity) }) { Text("Send code") }
            }
            else -> {
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (+250...)") })
                Spacer(Modifier.height(12.dp))
                Button(onClick = { authViewModel.sendOtp(phone, activity) }) { Text("Send code") }
            }
        }
    }
}
