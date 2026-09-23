package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.ui.components.hideStatusBarOnScroll

private val PartnerRevenueGreen = Color(0xFF009051)
private val PartnerRevenueDarkGreen = Color(0xFF0B5433)
private val PartnerMintBg = Color(0xFFE6F5EE)
private val PartnerBorderColor = Color(0xFFCCEBDC)

private enum class SettlementFilter(val title: String) {
    ALL("All Settlements"),
    ONLINE("Online Prepaid"),
    CASH("Cash Collected")
}

@Composable
fun PartnerSettlementHistoryScreen(
    completedBookings: List<Booking>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(SettlementFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedBookingId by remember { mutableStateOf<Long?>(null) }

    val filteredBookings = completedBookings
        .filter { booking ->
            when (selectedFilter) {
                SettlementFilter.ALL -> true
                SettlementFilter.ONLINE -> booking.paymentMethod.contains("UPI", ignoreCase = true)
                SettlementFilter.CASH -> booking.paymentMethod.equals("CASH", ignoreCase = true) || (!booking.paymentMethod.contains("UPI", ignoreCase = true) && !booking.isPaid)
            }
        }
        .filter { booking ->
            if (searchQuery.isBlank()) true
            else {
                booking.serviceName.contains(searchQuery, ignoreCase = true) ||
                        booking.bookingCode.contains(searchQuery, ignoreCase = true) ||
                        booking.locality.contains(searchQuery, ignoreCase = true)
            }
        }

    val totalSettled = completedBookings.sumOf { it.totalAmount }
    val onlineTotal = completedBookings.filter { it.paymentMethod.contains("UPI", ignoreCase = true) }.sumOf { it.totalAmount }
    val cashTotal = completedBookings.filter { it.paymentMethod.equals("CASH", ignoreCase = true) || (!it.paymentMethod.contains("UPI", ignoreCase = true) && !it.isPaid) }.sumOf { it.totalAmount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFBFDFB))
    ) {
        // 1. TOP HEADER BAR WITH THEME GREEN (#009051) EXTENDING BEHIND STATUS BAR
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PartnerRevenueGreen,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Job Settlement History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "${completedBookings.size} completed jobs settled to wallet & UPI",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hideStatusBarOnScroll()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 2. HERO SUMMARY CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PartnerRevenueGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PartnerRevenueGreen)
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = "TOTAL SETTLED EARNINGS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹$totalSettled",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 28.sp
                                        ),
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "100% Settled",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Online UPI Settled",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "₹$onlineTotal",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Direct Cash in Hand",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "₹$cashTotal",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. SEARCH FIELD & FILTER CHIPS
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by service, locality, or code...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PartnerRevenueGreen,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SettlementFilter.entries) { filter ->
                        val isSelected = selectedFilter == filter
                        val count = when (filter) {
                            SettlementFilter.ALL -> completedBookings.size
                            SettlementFilter.ONLINE -> completedBookings.count { it.isPaid }
                            SettlementFilter.CASH -> completedBookings.count { !it.isPaid }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) PartnerRevenueGreen else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) PartnerRevenueGreen else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = "${filter.title} ($count)",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            // 4. SETTLED JOBS LIST
            if (filteredBookings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No settlement records found",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                items(filteredBookings, key = { it.id }) { booking ->
                    val isExpanded = expandedBookingId == booking.id
                    val serviceIcon = getSettlementServiceIcon(booking.serviceId, booking.serviceName)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedBookingId = if (isExpanded) null else booking.id
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isExpanded) PartnerRevenueGreen else Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Icon in circular mint chip
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(PartnerMintBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = serviceIcon,
                                        contentDescription = null,
                                        tint = PartnerRevenueGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = booking.serviceName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${booking.scheduledDate} • ${booking.locality}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Code: ${booking.bookingCode} • ${booking.paymentMethod}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Price & Settled Badge
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+₹${booking.totalAmount}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = PartnerRevenueGreen
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PartnerMintBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SETTLED",
                                            color = PartnerRevenueGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Expanded Breakdown Details
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Settlement Breakdown",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Gross Service Amount", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("₹${booking.totalAmount}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Partner Commission Share (100%)", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("₹${booking.totalAmount}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PartnerRevenueGreen)
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Payment Channel", fontSize = 11.sp, color = Color(0xFF64748B))
                                        val channelText = if (booking.paymentMethod.contains("UPI", ignoreCase = true)) "Online UPI (QR Paid)" else "Cash Collected at Doorstep"
                                        Text(channelText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                    }
                                    if (!booking.paymentReference.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("UPI Ref (UTR)", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text(booking.paymentReference, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Payout Status", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("Instant Settled to Wallet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PartnerRevenueGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private fun getSettlementServiceIcon(serviceId: String, serviceName: String): ImageVector {
    val nameLower = serviceName.lowercase()
    val idLower = serviceId.lowercase()
    return when {
        "ac" in idLower || "ac" in nameLower -> Icons.Default.AcUnit
        "clean" in idLower || "clean" in nameLower || "home" in nameLower -> Icons.Default.Home
        "salon" in idLower || "facial" in nameLower || "spa" in nameLower -> Icons.Default.Spa
        "short circuit" in nameLower || "wiring" in nameLower || "plug" in nameLower -> Icons.Default.Power
        "electric" in idLower || "electric" in nameLower -> Icons.Default.ElectricBolt
        "plumb" in idLower || "pipe" in nameLower || "leak" in nameLower || "tap" in nameLower -> Icons.Default.Build
        else -> Icons.Default.Home
    }
}
