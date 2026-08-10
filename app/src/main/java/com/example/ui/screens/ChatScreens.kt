package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun TripChatScreen(
    tripId: String,
    otherPartyName: String,
    onNavigateBack: () -> Unit
) {
    val messages by SmartRepository.tripChatMessages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid
    val listState = rememberLazyListState()

    LaunchedEffect(tripId) {
        if (tripId.isNotBlank()) {
            SmartRepository.listenToTripChat(tripId)
        }
    }
    DisposableEffect(tripId) {
        onDispose { SmartRepository.detachTripChat() }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartBlackDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Surface(color = SmartBlackPaper, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SmartYellow)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = CircleShape, color = SmartYellow, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = SmartBlack, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(otherPartyName, color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Trip chat", color = SmartGrayMedium, fontSize = 11.sp)
                }
            }
        }

        if (tripId.isBlank()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No active trip", color = SmartGrayMedium)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isMine = msg.senderId == myUid
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMine) 16.dp else 2.dp,
                                bottomEnd = if (isMine) 2.dp else 16.dp
                            ),
                            color = if (isMine) SmartYellow else SmartBlackCard,
                            contentColor = if (isMine) SmartBlack else SmartGrayMedium,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (!isMine) {
                                    Text(
                                        text = msg.senderName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SmartYellow
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(msg.text, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        Surface(color = SmartBlackPaper, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type a message...", color = SmartGrayMedium) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SmartBlackCard,
                        unfocusedContainerColor = SmartBlackCard,
                        focusedBorderColor = SmartYellow,
                        unfocusedBorderColor = SmartGrayDark,
                        focusedTextColor = androidx.compose.ui.graphics.Color.White,
                        unfocusedTextColor = androidx.compose.ui.graphics.Color.White
                    ),
                    enabled = tripId.isNotBlank()
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank() && tripId.isNotBlank()) {
                            SmartRepository.sendTripChatMessage(tripId, inputText)
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
