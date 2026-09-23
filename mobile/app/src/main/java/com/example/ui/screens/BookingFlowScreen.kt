package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Offer
import com.example.data.model.SavedAddress
import com.example.data.model.ServiceItem
import com.example.data.model.ServicePackage
import com.example.data.model.UserProfile
import com.example.ui.components.hideStatusBarOnScroll
import java.util.Locale

enum class CartBookingMode(val label: String) {
    INSTANT("Instant"),
    SCHEDULED("Scheduled"),
    RECURRING("Recurring")
}

data class CartDateOption(
    val dayName: String,
    val dateNumber: String,
    val fullDateString: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFlowScreen(
    service: ServiceItem,
    selectedPackage: ServicePackage?,
    userProfile: UserProfile = UserProfile(),
    savedAddresses: List<SavedAddress>,
    availableOffers: List<Offer>,
    appliedOffer: Offer?,
    selectedAddress: SavedAddress?,
    selectedDate: String,
    selectedTimeSlot: String,
    paymentMethod: String,
    onDateChange: (String) -> Unit,
    onTimeSlotChange: (String) -> Unit,
    onAddressChange: (SavedAddress) -> Unit,
    onAddNewAddress: (String, String, String, String) -> Unit,
    onApplyPromo: (String) -> Boolean,
    onRemovePromo: () -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onConfirmBooking: (finalAmount: Int, discountAmount: Int, packageName: String, durationNotes: String) -> Unit,
    onBackClick: () -> Unit,
    onAddMoreServices: () -> Unit = onBackClick,
    modifier: Modifier = Modifier
) {
    // Mode State: Instant, Scheduled, Recurring
    var currentMode by remember { mutableStateOf(CartBookingMode.INSTANT) }

    // Quantity / Stepper State
    var quantity by remember { mutableIntStateOf(1) }
    var recurringVisits by remember { mutableIntStateOf(4) } // 4 visits/month for weekly

    // Expansion & Sheet states
    var isBillDetailsExpanded by remember { mutableStateOf(true) }
    var showCouponsSheet by remember { mutableStateOf(false) }
    var showAddressSheet by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showSlotSheet by remember { mutableStateOf(false) }
    var showGstInfoDialog by remember { mutableStateOf(false) }

    // Editable Contact State
    var contactName by remember(userProfile.name) {
        mutableStateOf(if (userProfile.name.isNotBlank()) userProfile.name else "Siddharth Kaushik")
    }
    var contactPhone by remember(userProfile.phone) {
        mutableStateOf(if (userProfile.phone.isNotBlank()) userProfile.phone else "+91 9105830551")
    }

    // New Address Form State
    var newAddressTitle by remember { mutableStateOf("Home") }
    var newAddressFull by remember { mutableStateOf("") }
    var newAddressLocality by remember { mutableStateOf("Taj Nagri Phase 2") }
    var newAddressLandmark by remember { mutableStateOf("") }

    // Custom Promo state
    var customPromoInput by remember { mutableStateOf("") }
    var promoMessage by remember { mutableStateOf<String?>(null) }

    // Date options for Scheduled / Recurring
    val dateSlotOptions = remember {
        listOf(
            CartDateOption("Today", "19", "Today, 19 Sep"),
            CartDateOption("Sun", "20", "Sun, 20 Sep"),
            CartDateOption("Mon", "21", "Mon, 21 Sep"),
            CartDateOption("Tue", "22", "Tue, 22 Sep"),
            CartDateOption("Wed", "23", "Wed, 23 Sep")
        )
    }

    val standardSlots = listOf("09:00 AM", "10:30 AM", "12:00 PM", "02:00 PM", "03:30 PM", "04:30 PM")
    val peakSlots = listOf("05:30 PM", "06:30 PM", "07:30 PM")

    // Price Calculations
    val unitPrice = selectedPackage?.price ?: service.startingPrice
    val baseItemTotal = unitPrice * quantity
    val visitsCount = if (currentMode == CartBookingMode.RECURRING) recurringVisits else 1
    val grossItemTotal = baseItemTotal * visitsCount

    val recurringDiscount = if (currentMode == CartBookingMode.RECURRING) (grossItemTotal * 15) / 100 else 0
    val couponDiscount = appliedOffer?.let { off ->
        if (off.percentageDiscount > 0) (grossItemTotal * off.percentageDiscount) / 100
        else off.flatDiscount
    } ?: 0

    val totalDiscount = recurringDiscount + couponDiscount
    val discountedSubtotal = (grossItemTotal - totalDiscount).coerceAtLeast(49)

    // GST @ 5% + platform fee
    val gstAndFeesDouble = (discountedSubtotal * 0.05)
    val gstFormatted = String.format(Locale.US, "%.2f", gstAndFeesDouble)
    val finalTotalDouble = discountedSubtotal + gstAndFeesDouble
    val finalTotalFormatted = String.format(Locale.US, "%.2f", finalTotalDouble)

    // Current resolved address
    val currentAddress = selectedAddress ?: savedAddresses.firstOrNull() ?: SavedAddress(
        id = 1,
        title = "Location",
        fullAddress = "987, Pocket 25, Subhash Place, Rohini",
        locality = "Rohini",
        city = "Delhi",
        landmark = "Near Metro Station"
    )

    // Duration display (e.g. 90 minutes)
    val isDurationService = service.name.contains("Help", ignoreCase = true) ||
            service.name.contains("Clean", ignoreCase = true) ||
            service.name.contains("Repair", ignoreCase = true)
    val stepperDisplayNumber = if (isDurationService) 90 * quantity else quantity
    val stepperUnitLabel = if (isDurationService) "Minutes" else "Quantity"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .hideStatusBarOnScroll()
                .padding(bottom = 120.dp)
        ) {
            // ================= 1. TOP HEADER =================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E2022),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "My Cart",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF1E2022)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= 2. 3-WAY SEGMENTED PILL SWITCHER =================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(50.dp),
                color = Color(0xFFECEEF2),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CartBookingMode.entries.forEach { mode ->
                        val isSelected = currentMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50.dp))
                                .background(
                                    if (isSelected) Color(0xFF123E2A)
                                    else Color.Transparent
                                )
                                .clickable { currentMode = mode }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = if (isSelected) Color.White else Color(0xFF4B5563)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= 3. RECURRING HERO BANNER (Only in Recurring Mode) =================
            if (currentMode == CartBookingMode.RECURRING) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD3EBE0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Set up recurring service.",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = Color(0xFF1E2022)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pick your days and time once. We'll schedule every visit automatically.",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = Color(0xFF6B7280)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF009051),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Choose days that work for you",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF374151)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF009051),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Same slot every time",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF374151)
                                    )
                                }
                            }

                            // Calendar Graphic Box
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE8F6F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Recurring Calendar",
                                    tint = Color(0xFF009051),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F4F6))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Frequency selection pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(2, "Bi-weekly", "2 visits"),
                                Triple(4, "Weekly", "4 visits"),
                                Triple(8, "Twice/wk", "8 visits")
                            ).forEach { (vCount, title, subtitle) ->
                                val isVSelected = recurringVisits == vCount
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isVSelected) Color(0xFFE8F8F0) else Color(0xFFF9FAFB))
                                        .border(
                                            1.dp,
                                            if (isVSelected) Color(0xFF009051) else Color(0xFFE5E7EB),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { recurringVisits = vCount }
                                        .padding(horizontal = 4.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isVSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            letterSpacing = (-0.1).sp
                                        ),
                                        color = if (isVSelected) Color(0xFF009051) else Color(0xFF1F2937),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 10.5.sp
                                        ),
                                        color = if (isVSelected) Color(0xFF065F46) else Color(0xFF6B7280),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ================= 4. REVIEW BOOKING SECTION =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Review booking",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF1E2022)
                )
                Text(
                    text = if (currentMode == CartBookingMode.RECURRING) "1 service per visit" else "1 service",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Service row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Service Icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEBF7EE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Spa,
                                    contentDescription = null,
                                    tint = Color(0xFF009051),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    ),
                                    color = Color(0xFF1E2022)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedPackage?.name ?: "Standard Package",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "₹$unitPrice",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF1E2022)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Green Stepper [- 90 +]
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.2.dp, Color(0xFF009051), RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = Color(0xFF009051),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { if (quantity > 1) quantity-- }
                                    )
                                    Text(
                                        text = "$stepperDisplayNumber",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color(0xFF009051)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = Color(0xFF009051),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { quantity++ }
                                    )
                                }
                                Text(
                                    text = stepperUnitLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF009051)
                                )
                            }
                        }
                    }

                    // Scheduled Info Banner (Screenshot 2)
                    if (currentMode == CartBookingMode.SCHEDULED) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF3F4F8))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Slots may vary based on partner availability and the selected service.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                    color = Color(0xFF4B5563)
                                )
                            }
                        }
                    }

                    // Schedule Slot row (for Scheduled & Recurring modes)
                    if (currentMode != CartBookingMode.INSTANT) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF9FAFB))
                                .clickable { showSlotSheet = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color(0xFF009051),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Slot: $selectedDate at $selectedTimeSlot",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF1F2937)
                                )
                            }
                            Text(
                                text = "Change >",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF009051)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF1F4F6))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Missed something? Add more services
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Missed something? ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = "Add more services.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF009051),
                            modifier = Modifier.clickable { onAddMoreServices() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= 5. VIEW ALL COUPONS =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showCouponsSheet = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (appliedOffer != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ConfirmationNumber,
                                contentDescription = null,
                                tint = Color(0xFF009051),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Coupon Applied: ${appliedOffer.code}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF009051)
                                )
                                Text(
                                    text = "₹$couponDiscount extra discount applied",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                        Text(
                            text = "Remove",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Red,
                            modifier = Modifier.clickable { onRemovePromo() }
                        )
                    } else {
                        Text(
                            text = "View all coupons",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = Color(0xFF1E2022)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "View coupons",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= 6. BOOKING DETAILS =================
            Text(
                text = "Booking details",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1E2022),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Location Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddressSheet = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Location",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF1E2022)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentAddress.fullAddress}, ${currentAddress.locality}, ${currentAddress.city}",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                color = Color(0xFF6B7280),
                                maxLines = 2
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Change location",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF1F4F6))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Contact Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showContactDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Contact",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contactName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF1E2022)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = contactPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Change contact",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= 7. BILL DETAILS ACCORDION =================
            Text(
                text = "Bill details",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1E2022),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with dropdown toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBillDetailsExpanded = !isBillDetailsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "To pay ₹$finalTotalFormatted",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF1E2022)
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBillDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle bill details",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isBillDetailsExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFFF1F4F6))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Item total
                            CartBillRow(
                                label = if (currentMode == CartBookingMode.RECURRING) "Item total ($recurringVisits visits)" else "Item total",
                                value = "₹$grossItemTotal"
                            )

                            // Recurring Discount
                            if (recurringDiscount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CartBillRow(
                                    label = "Recurring Plan Discount (15%)",
                                    value = "-₹$recurringDiscount",
                                    valueColor = Color(0xFF009051)
                                )
                            }

                            // Coupon Discount
                            if (couponDiscount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CartBillRow(
                                    label = "Coupon Discount (${appliedOffer?.code})",
                                    value = "-₹$couponDiscount",
                                    valueColor = Color(0xFF009051)
                                )
                            }

                            // GST & Service Fees
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showGstInfoDialog = true }
                                ) {
                                    Text(
                                        text = "GST & Service Fees",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF6B7280)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "GST Info",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "₹$gstFormatted",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF1E2022)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Dotted Divider
                            DottedDivider()

                            Spacer(modifier = Modifier.height(14.dp))

                            // Final To Pay
                            CartBillRow(
                                label = "To pay",
                                value = "₹$finalTotalFormatted",
                                isBold = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // ================= 8. STICKY BOTTOM ACTION BAR =================
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val buttonText = when {
                    currentAddress.fullAddress.isBlank() -> "Add address to proceed"
                    currentMode == CartBookingMode.INSTANT -> "Book Instant Service • ₹$finalTotalFormatted"
                    currentMode == CartBookingMode.RECURRING -> "Set up recurring service • ₹$finalTotalFormatted"
                    else -> "Proceed to pay • ₹$finalTotalFormatted"
                }

                Button(
                    onClick = {
                        if (currentAddress.fullAddress.isBlank()) {
                            showAddressSheet = true
                        } else {
                            val packageDetails = if (currentMode == CartBookingMode.RECURRING) {
                                "${selectedPackage?.name ?: "Standard Service"} • $recurringVisits Visits"
                            } else if (isDurationService) {
                                "${selectedPackage?.name ?: "Standard Service"} • ${90 * quantity} Mins"
                            } else if (quantity > 1) {
                                "${selectedPackage?.name ?: "Standard Service"} (Qty: $quantity)"
                            } else {
                                selectedPackage?.name ?: "Standard Service"
                            }

                            val durationNotes = if (isDurationService) {
                                "Duration: ${90 * quantity} Minutes (${quantity}x slot)"
                            } else if (currentMode == CartBookingMode.RECURRING) {
                                "Recurring Plan: $recurringVisits visits scheduled"
                            } else if (quantity > 1) {
                                "Quantity: $quantity"
                            } else {
                                ""
                            }

                            val finalTotalInt = (discountedSubtotal + gstAndFeesDouble).toInt()
                            onConfirmBooking(finalTotalInt, totalDiscount, packageDetails, durationNotes)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009051)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_booking_btn")
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }

        // ================= 9. MODALS & BOTTOM SHEETS =================

        // A. Coupons Bottom Sheet
        if (showCouponsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCouponsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Apply Coupon Code",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E2022)
                        )
                        IconButton(onClick = { showCouponsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF4B5563))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom Promo Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPromoInput,
                            onValueChange = { customPromoInput = it.uppercase() },
                            placeholder = { Text("Enter coupon code") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF009051),
                                focusedLabelColor = Color(0xFF009051)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (customPromoInput.isNotBlank()) {
                                    val success = onApplyPromo(customPromoInput)
                                    if (success) {
                                        promoMessage = "Coupon applied successfully!"
                                        showCouponsSheet = false
                                    } else {
                                        promoMessage = "Invalid coupon code"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009051)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (promoMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = promoMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (promoMessage!!.contains("successfully")) Color(0xFF009051) else Color.Red
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Available Offers",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E2022)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    availableOffers.forEach { offer ->
                        val isCurrentApplied = appliedOffer?.code == offer.code
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isCurrentApplied) Color(0xFFE8F6F0) else Color(0xFFF9FAFB)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrentApplied) Color(0xFF009051) else Color(0xFFE5E7EB))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = offer.code,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF009051)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = offer.title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF1E2022)
                                    )
                                    Text(
                                        text = offer.discountDescription,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (isCurrentApplied) {
                                            onRemovePromo()
                                        } else {
                                            onApplyPromo(offer.code)
                                            showCouponsSheet = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCurrentApplied) Color(0xFFEF4444) else Color(0xFF009051)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isCurrentApplied) "Remove" else "Apply")
                                }
                            }
                        }
                    }
                }
            }
        }

        // B. Address Selector & Add Address Bottom Sheet
        if (showAddressSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddressSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Delivery Address",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E2022)
                        )
                        IconButton(onClick = { showAddressSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    savedAddresses.forEach { addr ->
                        val isAddrSelected = currentAddress.id == addr.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onAddressChange(addr)
                                    showAddressSheet = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAddrSelected) Color(0xFFE8F6F0) else Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAddrSelected) Color(0xFF009051) else Color(0xFFE5E7EB)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isAddrSelected,
                                    onClick = {
                                        onAddressChange(addr)
                                        showAddressSheet = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF009051))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = addr.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1E2022)
                                    )
                                    Text(
                                        text = "${addr.fullAddress}, ${addr.locality}, ${addr.city}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            showAddressSheet = false
                            showAddAddressDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF009051))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add New Address", color = Color(0xFF009051), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // C. Add New Address Dialog
        if (showAddAddressDialog) {
            AlertDialog(
                onDismissRequest = { showAddAddressDialog = false },
                title = { Text("Add New Address", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newAddressTitle,
                            onValueChange = { newAddressTitle = it },
                            label = { Text("Address Tag (Home/Office)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newAddressFull,
                            onValueChange = { newAddressFull = it },
                            label = { Text("Flat / House No / Road") }
                        )
                        OutlinedTextField(
                            value = newAddressLocality,
                            onValueChange = { newAddressLocality = it },
                            label = { Text("Locality / Area") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newAddressLandmark,
                            onValueChange = { newAddressLandmark = it },
                            label = { Text("Landmark (Optional)") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newAddressFull.isNotBlank()) {
                                onAddNewAddress(newAddressTitle, newAddressFull, newAddressLocality, newAddressLandmark)
                                showAddAddressDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009051))
                    ) {
                        Text("Save Address")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAddAddressDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // D. Edit Contact Dialog
        if (showContactDialog) {
            AlertDialog(
                onDismissRequest = { showContactDialog = false },
                title = { Text("Recipient Contact Details", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Phone Number") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showContactDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009051))
                    ) {
                        Text("Done")
                    }
                }
            )
        }

        // E. Slot Selector Bottom Sheet (Scheduled / Recurring)
        if (showSlotSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSlotSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Service Date & Slot",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E2022)
                        )
                        IconButton(onClick = { showSlotSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(dateSlotOptions) { dateOpt ->
                            val isSelected = selectedDate.contains(dateOpt.dateNumber) || selectedDate == dateOpt.fullDateString
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFE8F6F0) else Color(0xFFF9FAFB))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF009051) else Color(0xFFE5E7EB),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onDateChange(dateOpt.fullDateString) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dateOpt.dayName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = if (isSelected) Color(0xFF009051) else Color(0xFF6B7280)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dateOpt.dateNumber,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color(0xFF009051) else Color(0xFF1E2022)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Select start time of service",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E2022)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val allSlots = standardSlots + peakSlots
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(allSlots) { slot ->
                            val isSlotSelected = selectedTimeSlot == slot
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSlotSelected) Color(0xFFE8F6F0) else Color(0xFFF9FAFB))
                                    .border(
                                        width = if (isSlotSelected) 1.5.dp else 1.dp,
                                        color = if (isSlotSelected) Color(0xFF009051) else Color(0xFFE5E7EB),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onTimeSlotChange(slot)
                                        showSlotSheet = false
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slot,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSlotSelected) Color(0xFF009051) else Color(0xFF374151)
                                )
                            }
                        }
                    }
                }
            }
        }

        // F. GST Info Dialog
        if (showGstInfoDialog) {
            AlertDialog(
                onDismissRequest = { showGstInfoDialog = false },
                title = { Text("GST & Service Fees Breakdown", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "• Government GST (5%): Applied on the discounted service subtotal according to local tax regulations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4B5563)
                        )
                        Text(
                            text = "• Service Quality & Protection: Covers partner background verification, doorstep hygiene kits, and 30-day service warranty.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4B5563)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showGstInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009051))
                    ) {
                        Text("Understood")
                    }
                }
            )
        }
    }
}

@Composable
fun CartBillRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isBold: Boolean = false,
    valueColor: Color = Color(0xFF1E2022)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyMedium,
            color = if (isBold) Color(0xFF1E2022) else Color(0xFF6B7280)
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isBold) Color(0xFF1E2022) else valueColor
        )
    }
}

@Composable
fun DottedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFD1D5DB)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = pathEffect,
            strokeWidth = 1.5f
        )
    }
}
