package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerModal(
    currentCity: String,
    currentLocality: String,
    supportedCities: List<String>,
    agraLocalities: List<String>,
    onDismiss: () -> Unit,
    onLocationSelected: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCity by remember { mutableStateOf(currentCity) }
    var selectedLocality by remember { mutableStateOf(currentLocality) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var isDetectingLocation by remember { mutableStateOf(false) }
    var detectedAddressLine by remember { mutableStateOf<String?>(null) }
    var locationErrorMessage by remember { mutableStateOf<String?>(null) }
    var isGpsActive by remember { mutableStateOf(false) }

    fun processLocation(location: Location) {
        coroutineScope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val detectedCityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Agra"
                    val subLoc = addr.subLocality ?: addr.thoroughfare ?: addr.featureName ?: "Taj Nagri Phase 2"
                    val fullLine = addr.getAddressLine(0) ?: "$subLoc, $detectedCityName"

                    selectedCity = if (supportedCities.any { it.equals(detectedCityName, ignoreCase = true) }) {
                        supportedCities.first { it.equals(detectedCityName, ignoreCase = true) }
                    } else {
                        "Agra"
                    }
                    selectedLocality = subLoc
                    detectedAddressLine = fullLine
                    isGpsActive = true
                    locationErrorMessage = null
                } else {
                    selectedCity = "Agra"
                    selectedLocality = "Taj Nagri Phase 2"
                    detectedAddressLine = "Pinpoint: ${String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)}"
                    isGpsActive = true
                    locationErrorMessage = null
                }
            } catch (e: Exception) {
                selectedCity = "Agra"
                selectedLocality = "Taj Nagri Phase 2"
                detectedAddressLine = "GPS coordinates locked: ${String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)}"
                isGpsActive = true
                locationErrorMessage = null
            } finally {
                isDetectingLocation = false
            }
        }
    }

    fun fetchGpsLocation() {
        isDetectingLocation = true
        locationErrorMessage = null

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        processLocation(location)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                processLocation(lastLoc)
                            } else {
                                isDetectingLocation = false
                                locationErrorMessage = "GPS signal weak. Defaulting to Agra pinpoint."
                                selectedCity = "Agra"
                                selectedLocality = "Taj Nagri Phase 2"
                                isGpsActive = true
                            }
                        }.addOnFailureListener {
                            isDetectingLocation = false
                            locationErrorMessage = "Could not fetch GPS. Using Agra center."
                        }
                    }
                }
                .addOnFailureListener { e ->
                    isDetectingLocation = false
                    locationErrorMessage = "Location error: ${e.localizedMessage ?: "GPS unavailable"}"
                }
        } catch (e: SecurityException) {
            isDetectingLocation = false
            locationErrorMessage = "Location permission required to access GPS."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineLocationGranted || coarseLocationGranted) {
            fetchGpsLocation()
        } else {
            isDetectingLocation = false
            locationErrorMessage = "Location permission denied. Please select locality manually."
        }
    }

    fun onUseCurrentLocationClick() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            fetchGpsLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val emeraldGreen = Color(0xFF009051)
    val darkForest = Color(0xFF0B5433)
    val textPrimary = Color(0xFF1E2022)
    val textSecondary = Color(0xFF6B7280)
    val bgMint = Color(0xFFF7FCF9)
    val cardBorder = Color(0xFFE5E7EB)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgMint,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgMint)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
            ) {
                // ================= 1. TOP HEADER & MONUMENT BADGE =================
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        // Back circular button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2F5EC))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = darkForest,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Title + Subtitle + Top-right Illustration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Choose Your Service City",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Service Assist is currently live with 1,000+ verified pros in Agra",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = textSecondary
                                )
                            }

                            // Agra Monument Illustration Badge
                            AgraLocationBadge(modifier = Modifier.size(width = 85.dp, height = 75.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ================= GPS CURRENT LOCATION BUTTON =================
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isDetectingLocation) { onUseCurrentLocationClick() }
                            .testTag("use_current_location_btn"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGpsActive) Color(0xFFE8F8F0) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isGpsActive) emeraldGreen else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isGpsActive) emeraldGreen else Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDetectingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = emeraldGreen,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isGpsActive) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                                        contentDescription = "My Location GPS",
                                        tint = if (isGpsActive) Color.White else emeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isGpsActive) "Current Live Location" else "Use Current Location",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isGpsActive) darkForest else textPrimary
                                )
                                if (isDetectingLocation || detectedAddressLine != null) {
                                    Text(
                                        text = if (isDetectingLocation) "Detecting device GPS..." else detectedAddressLine!!,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = if (isGpsActive) emeraldGreen else textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "Using device GPS",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp
                                        ),
                                        color = textSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (!isDetectingLocation) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isGpsActive) emeraldGreen else Color(0xFFE2E8F0)
                                ) {
                                    Text(
                                        text = if (isGpsActive) "Active" else "Enable",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGpsActive) Color.White else textSecondary
                                    )
                                }
                            }
                        }
                    }

                    if (locationErrorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Text(
                                text = locationErrorMessage ?: "",
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ================= 2. AVAILABLE CITIES =================
                item {
                    Text(
                        text = "AVAILABLE CITIES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = darkForest
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(supportedCities.take(3)) { city ->
                            val isCitySelected = selectedCity == city
                            val isLiveCity = city == "Agra"

                            Card(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedCity = city
                                        if (city == "Agra") {
                                            if (selectedLocality.isBlank()) selectedLocality = agraLocalities.first()
                                        } else {
                                            selectedLocality = "Central $city"
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCitySelected) Color(0xFFE8F8F0) else Color.White
                                ),
                                border = BorderStroke(
                                    width = if (isCitySelected) 1.5.dp else 1.dp,
                                    color = if (isCitySelected) emeraldGreen else cardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationCity,
                                        contentDescription = null,
                                        tint = if (isCitySelected) emeraldGreen else textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = city,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isCitySelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        ),
                                        color = if (isCitySelected) darkForest else textPrimary
                                    )

                                    if (isLiveCity) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(emeraldGreen)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "LIVE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ================= 3. LOCALITY SELECTION =================
                if (selectedCity == "Agra") {
                    item {
                        Text(
                            text = "SELECT AGRA LOCALITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = darkForest
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(agraLocalities) { locality ->
                        val isLocalitySelected = selectedLocality == locality

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedLocality = locality }
                                .testTag("locality_$locality"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLocalitySelected) Color(0xFFE6F7ED) else Color.White
                            ),
                            border = BorderStroke(
                                width = if (isLocalitySelected) 1.2.dp else 1.dp,
                                color = if (isLocalitySelected) Color(0xFF86EFAC) else cardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Circular Navigation Icon
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isLocalitySelected) Color(0xFFD1FAE5) else Color(0xFFE8F6EE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = emeraldGreen,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Text(
                                        text = locality,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isLocalitySelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.5.sp
                                        ),
                                        color = if (isLocalitySelected) darkForest else textPrimary
                                    )
                                }

                                if (isLocalitySelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(emeraldGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Expanding Soon to $selectedCity",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = darkForest
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Service Assist is expanding quickly to $selectedCity! We will notify you as soon as on-demand 30-min booking unlocks for your neighborhood.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // ================= 4. BOTTOM FIXED CONFIRM BUTTON =================
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = bgMint.copy(alpha = 0.96f),
                shadowElevation = 0.dp
            ) {
                Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Button(
                        onClick = {
                            onLocationSelected(selectedCity, selectedLocality)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("confirm_location_btn"),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = darkForest),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Confirm Location ($selectedCity • $selectedLocality)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Agra Monument & Location Pin Illustration matching the reference design
 */
@Composable
private fun AgraLocationBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Soft organic mint background bubble
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFDCFCE7).copy(alpha = 0.7f)
        ) {}

        // Canvas for radiating dash marks and monument silhouette
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeGreen = Color(0xFF135334).copy(alpha = 0.4f)
            val strokeWidth = 1.8.dp.toPx()

            // Radiating sparkles
            drawLine(strokeGreen, Offset(size.width * 0.18f, size.height * 0.22f), Offset(size.width * 0.24f, size.height * 0.28f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawLine(strokeGreen, Offset(size.width * 0.88f, size.height * 0.2f), Offset(size.width * 0.82f, size.height * 0.27f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawLine(strokeGreen, Offset(size.width * 0.15f, size.height * 0.65f), Offset(size.width * 0.22f, size.height * 0.62f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawLine(strokeGreen, Offset(size.width * 0.85f, size.height * 0.72f), Offset(size.width * 0.78f, size.height * 0.68f), strokeWidth = strokeWidth, cap = StrokeCap.Round)

            // Taj Mahal dome / minarets line art at bottom
            val monumentGreen = Color(0xFF135334).copy(alpha = 0.55f)
            val baseY = size.height * 0.88f
            val monumentPath = Path().apply {
                // Left minaret
                moveTo(size.width * 0.55f, baseY)
                lineTo(size.width * 0.55f, baseY - 20f)
                lineTo(size.width * 0.58f, baseY - 20f)
                lineTo(size.width * 0.58f, baseY)

                // Central dome
                moveTo(size.width * 0.62f, baseY)
                lineTo(size.width * 0.62f, baseY - 22f)
                cubicTo(
                    size.width * 0.62f, baseY - 34f,
                    size.width * 0.78f, baseY - 34f,
                    size.width * 0.78f, baseY - 22f
                )
                lineTo(size.width * 0.78f, baseY)

                // Right minaret
                moveTo(size.width * 0.82f, baseY)
                lineTo(size.width * 0.82f, baseY - 20f)
                lineTo(size.width * 0.85f, baseY - 20f)
                lineTo(size.width * 0.85f, baseY)
            }
            drawPath(monumentPath, monumentGreen, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
        }

        // Dark Green Location Pin Icon with white center
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 18.dp, y = (-6).dp)
                .size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF0F4A2F),
                modifier = Modifier.size(34.dp)
            )
            // White dot inside pin
            Box(
                modifier = Modifier
                    .offset(y = (-4).dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
