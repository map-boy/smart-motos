package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationPoint
import com.example.data.SmartRepository
import com.example.data.UserRole
import com.example.ui.theme.*
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.delay
import com.example.data.LocationSearchService

@Composable
fun SmartButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) SmartYellow else SmartYellowLight,
            contentColor = if (isPrimary) Color.White else SmartYellowDark,
            disabledContainerColor = SmartGrayDark,
            disabledContentColor = SmartGrayMedium
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = if (isPrimary) Color.White else SmartYellowDark,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SmartTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            color = SmartBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = SmartGrayMedium) },
            readOnly = readOnly,
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = {
                if (isPassword) {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle Password Visibility",
                            tint = SmartYellow
                        )
                    }
                } else if (trailingIcon != null) {
                    trailingIcon()
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SmartBlackCard,
                unfocusedContainerColor = SmartBlackCard,
                disabledContainerColor = SmartBlackCard,
                focusedBorderColor = SmartYellow,
                unfocusedBorderColor = SmartGrayDark,
                focusedTextColor = SmartBlack,
                unfocusedTextColor = SmartBlack
            )
        )
    }
}

@Composable
fun WalletBalanceCard(
    balanceRwf: Int,
    onTopUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SmartBlackCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SmartGrayDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Wallet Balance",
                    color = SmartGrayMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = SmartYellowLight,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "RWF",
                            color = SmartYellowDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "%,d Rwf".format(balanceRwf),
                        color = SmartBlack,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTopUpClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SmartYellow, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = "Top up +", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun RatingStarsRow(
    rating: Float,
    maxStars: Int = 5,
    starSize: Int = 18,
    onStarSelected: ((Int) -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..maxStars) {
            val isFilled = i <= rating
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Star $i",
                tint = SmartYellow,
                modifier = Modifier
                    .size(starSize.dp)
                    .clickable(enabled = onStarSelected != null) {
                        onStarSelected?.invoke(i)
                    }
            )
        }
    }
}

@Composable
fun RoleHeaderBar(
    currentRole: UserRole,
    userName: String,
    onRoleChanged: (UserRole) -> Unit,
    onMenuClick: (() -> Unit)? = null
) {
    Surface(
        color = SmartYellow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onMenuClick != null) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(SmartYellowLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.TwoWheeler, contentDescription = "Logo", tint = SmartYellowDark, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Smart Motos",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Hello, $userName",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SmartYellowLight,
                contentColor = SmartYellowDark
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (currentRole) {
                            UserRole.PASSENGER -> Icons.Filled.Person
                            UserRole.DRIVER -> Icons.Filled.TwoWheeler
                            UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
                        },
                        contentDescription = "Role",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentRole.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

}

@Composable
fun InteractiveKigaliMap(
    pickup: LocationPoint? = null,
    dropoff: LocationPoint? = null,
    onLocationTap: ((LocationPoint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val annotationManager = remember { mutableStateOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager?>(null) }

    fun pinBitmap(colorArgb: Int): Bitmap {
        val size = 64
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)
        val outerPaint = Paint().apply { color = colorArgb; isAntiAlias = true; alpha = 90 }
        val innerPaint = Paint().apply { color = colorArgb; isAntiAlias = true }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, outerPaint)
        canvas.drawCircle(size / 2f, size / 2f, size / 4f, innerPaint)
        return bmp
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        factory = { ctx ->
            MapView(ctx).apply {
                mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                    annotationManager.value = annotations.createPointAnnotationManager()
                }
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(30.0619, -1.9441))
                        .zoom(12.5)
                        .build()
                )
                gestures.addOnMapClickListener { point ->
                    onLocationTap?.invoke(
                        LocationPoint(
                            address = "Selected on map",
                            latitude = point.latitude(),
                            longitude = point.longitude()
                        )
                    )
                    true
                }
            }
        },
        update = { mapView ->
            val manager = annotationManager.value ?: return@AndroidView
            manager.deleteAll()

            pickup?.let { p ->
                manager.create(
                    PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(p.longitude, p.latitude))
                        .withIconImage(pinBitmap(android.graphics.Color.parseColor("#34C759")))
                )
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(p.longitude, p.latitude))
                        .build()
                )
            }

            dropoff?.let { d ->
                manager.create(
                    PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(d.longitude, d.latitude))
                        .withIconImage(pinBitmap(android.graphics.Color.parseColor("#FF3B30")))
                )
            }
        }
    )
}




@Composable
fun LocationSearchDialog(
    title: String,
    onDismiss: () -> Unit,
    onLocationSelected: (LocationPoint) -> Unit,
    onUseCurrentLocation: (() -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<LocationPoint>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.length < 2) {
            results = emptyList()
        } else {
            isSearching = true
            delay(350) // debounce so we don't hit Mapbox on every keystroke
            results = LocationSearchService.searchPlaces(query)
            isSearching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SmartBlackCard,
        title = {
            Text(title, color = SmartYellow, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search a place in Kigali...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SmartBlack,
                        unfocusedTextColor = SmartBlack,
                        focusedBorderColor = SmartYellow,
                        unfocusedBorderColor = SmartGrayMedium
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (onUseCurrentLocation != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUseCurrentLocation()
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = SmartSuccess)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Use my current location", color = SmartSuccess, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (isSearching) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = SmartYellow, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Column(modifier = Modifier.heightIn(max = 260.dp)) {
                        results.forEach { place ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLocationSelected(place)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = SmartYellow, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(place.address, color = SmartBlack, fontSize = 13.sp)
                            }
                        }
                        if (!isSearching && query.length >= 2 && results.isEmpty()) {
                            Text("No places found in Kigali for that search.", color = SmartGrayMedium, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SmartGrayMedium)
            }
        }
    )
}
