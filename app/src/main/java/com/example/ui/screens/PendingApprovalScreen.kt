package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PendingApprovalScreen(
    status: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val isRejected = status == "rejected"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                color = if (isRejected) SmartError.copy(alpha = 0.15f) else SmartYellow.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isRejected) Icons.Filled.Cancel else Icons.Filled.HourglassTop,
                        contentDescription = null,
                        tint = if (isRejected) SmartError else SmartYellow,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isRejected) "Application Not Approved" else "Application Under Review",
                color = SmartBlack,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isRejected)
                    "Your driver application was not approved. Contact support for more details, or reach out to re-apply."
                else
                    "Our team is reviewing your driver application and license photo. You'll get access to the driver dashboard as soon as you're approved.",
                color = SmartGrayMedium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                    onSignOut()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmartBlackCard,
                    contentColor = SmartYellow
                )
            ) {
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}
