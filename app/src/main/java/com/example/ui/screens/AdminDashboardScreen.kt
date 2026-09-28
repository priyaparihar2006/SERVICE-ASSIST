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
import com.example.ui.components.stableStatusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.Professional
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.supabase.SupabaseSyncState
import com.example.ui.components.AdminNavTab
import com.example.ui.components.ThemeToggleButton
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.theme.ServoraTheme

private val AdminIndigo = Color(0xFF4F46E5) // theme-invariant
private val AdminIndigoDark = Color(0xFF3730A3) // theme-invariant
private val AdminAmber = Color(0xFFF59E0B) // theme-invariant

@Composable
fun AdminDashboardScreen(
    currentAdmin: UserProfile,
    currentTab: AdminNavTab,
    allCustomers: List<UserProfile>,
    allPartners: List<Professional>,
    allBookings: List<Booking>,
    syncState: SupabaseSyncState,
    onSwitchToUser: (UserProfile) -> Unit,
    onAdvanceBookingStatus: (Long, BookingStatus) -> Unit,
    onSyncClick: () -> Unit,
    onLogout: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBookingStatusFilter by remember { mutableStateOf<BookingStatus?>(null) }
    var switchTargetUser by remember { mutableStateOf<UserProfile?>(null) }

    val isDark = ServoraTheme.colors.isDark

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .hideStatusBarOnScroll()
    ) {
        // TOP ADMIN CONSOLE HEADER
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = AdminIndigoDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .stableStatusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AdminIndigo),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Console",
                                tint = Color.White, /* theme-invariant */
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Admin Operations Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White, /* theme-invariant */
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentAdmin.name} • Agra Central Ops",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f), /* theme-invariant */
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeToggleButton(
                            isDarkTheme = isDarkTheme,
                            onToggle = onToggleTheme
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)) /* theme-invariant */
                                .clickable { onSyncClick() }
                                .testTag("admin_sync_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Supabase",
                                tint = Color.White, /* theme-invariant */
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.25f)) /* theme-invariant */
                                .clickable { onLogout() }
                                .testTag("admin_logout_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = Color.White, /* theme-invariant */
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cloud Sync Status Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.12f)) /* theme-invariant */
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (syncState.isConnected) Icons.Default.CloudDone else Icons.Default.Refresh,
                        contentDescription = "Sync Status",
                        tint = if (syncState.isConnected) Color(0xFF34D399) /* theme-invariant */ else AdminAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (syncState.isSyncing) "Syncing with Supabase..." else syncState.lastMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f) /* theme-invariant */
                    )
                }
            }
        }

        // CONTENT BASED ON ACTIVE ADMIN TAB
        when (currentTab) {
            AdminNavTab.OVERVIEW -> {
                AdminOverviewTab(
                    allCustomers = allCustomers,
                    allPartners = allPartners,
                    allBookings = allBookings,
                    onSwitchToUser = { switchTargetUser = it },
                    onAdvanceBookingStatus = onAdvanceBookingStatus
                )
            }
            AdminNavTab.CUSTOMERS -> {
                AdminCustomersTab(
                    customers = allCustomers,
                    allBookings = allBookings,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onSwitchToCustomer = { switchTargetUser = it }
                )
            }
            AdminNavTab.PARTNERS -> {
                AdminPartnersTab(
                    partners = allPartners,
                    allBookings = allBookings,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onSwitchToPartner = { pro ->
                        val partnerUser = UserProfile(
                            id = pro.id,
                            name = pro.name,
                            phone = pro.phone,
                            email = "${pro.name.lowercase().replace(" ", "")}@serviceassist.in",
                            city = "Agra",
                            locality = "Fatehabad Road",
                            role = UserRole.PROFESSIONAL
                        )
                        switchTargetUser = partnerUser
                    }
                )
            }
            AdminNavTab.BOOKINGS -> {
                AdminBookingsTab(
                    bookings = allBookings,
                    selectedStatus = selectedBookingStatusFilter,
                    onStatusSelect = { selectedBookingStatusFilter = it },
                    onAdvanceStatus = onAdvanceBookingStatus
                )
            }
            AdminNavTab.PROFILE -> {
                AdminProfileTab(
                    currentAdmin = currentAdmin,
                    syncState = syncState,
                    onSyncClick = onSyncClick,
                    onLogout = onLogout
                )
            }
        }
    }

    // Confirmation Dialog before Switching Accounts
    if (switchTargetUser != null) {
        val target = switchTargetUser!!
        AlertDialog(
            onDismissRequest = { switchTargetUser = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch",
                        tint = AdminIndigo
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Switch Active Account?", color = ServoraTheme.colors.textPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "You are about to switch to the account of:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ServoraTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ServoraTheme.colors.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = target.name,
                                fontWeight = FontWeight.Bold,
                                color = ServoraTheme.colors.textPrimary
                            )
                            Text(
                                text = "Role: ${target.role} • ${target.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ServoraTheme.colors.subtext
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFFEF3C7))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "🛡️ Audit Notice: Viewing user chat conversations is strictly logged to the immutable admin audit trail.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (isDark) ServoraTheme.colors.warning else Color(0xFF92400E)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You will be able to return to the Admin Console anytime via the top banner.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ServoraTheme.colors.subtext
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val u = target
                        switchTargetUser = null
                        onSwitchToUser(u)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminIndigo)
                ) {
                    Text("Confirm & Switch", color = Color.White /* theme-invariant */)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { switchTargetUser = null }) {
                    Text("Cancel", color = ServoraTheme.colors.textPrimary)
                }
            }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    allCustomers: List<UserProfile>,
    allPartners: List<Professional>,
    allBookings: List<Booking>,
    onSwitchToUser: (UserProfile) -> Unit,
    onAdvanceBookingStatus: (Long, BookingStatus) -> Unit
) {
    val isDark = ServoraTheme.colors.isDark
    val totalRevenue = allBookings.filter { it.status == BookingStatus.COMPLETED }.sumOf { it.totalAmount }
    val activeBookings = allBookings.filter { it.status != BookingStatus.COMPLETED && it.status != BookingStatus.CANCELLED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                text = "Key Operations Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ServoraTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 4 KPI Cards in a Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminKpiCard(
                    title = "Total GMV",
                    value = "₹$totalRevenue",
                    icon = Icons.Default.AccountBalanceWallet,
                    tint = if (isDark) ServoraTheme.colors.success else Color(0xFF009051),
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Active Jobs",
                    value = "${activeBookings.size}",
                    icon = Icons.Default.Assignment,
                    tint = if (isDark) ServoraTheme.colors.warning else AdminAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminKpiCard(
                    title = "Customers",
                    value = "${allCustomers.size}",
                    icon = Icons.Default.Group,
                    tint = AdminIndigo,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Agra Partners",
                    value = "${allPartners.size}",
                    icon = Icons.Default.Engineering,
                    tint = if (isDark) ServoraTheme.colors.info else Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = ServoraTheme.colors.cardBorder)
            Spacer(modifier = Modifier.height(14.dp))

            // QUICK MULTI-ACCOUNT SWITCHER CAROUSEL
            Text(
                text = "Quick Account Switcher",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ServoraTheme.colors.textPrimary
            )
            Text(
                text = "Tap to immediately log in and manage services as that user",
                style = MaterialTheme.typography.bodySmall,
                color = ServoraTheme.colors.subtext
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Customer shortcuts
                items(allCustomers) { customer ->
                    QuickUserSwitchCard(
                        name = customer.name,
                        role = "Customer",
                        phone = customer.phone,
                        badgeColor = if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFFEF3C7),
                        textColor = if (isDark) ServoraTheme.colors.warning else Color(0xFFB45309),
                        onClick = { onSwitchToUser(customer) }
                    )
                }
                // Partner shortcuts
                items(allPartners) { partner ->
                    val partnerProfile = UserProfile(
                        id = partner.id,
                        name = partner.name,
                        phone = partner.phone,
                        email = "${partner.name.lowercase().replace(" ", "")}@serviceassist.in",
                        city = "Agra",
                        locality = "Fatehabad Road",
                        role = UserRole.PROFESSIONAL
                    )
                    QuickUserSwitchCard(
                        name = partner.name,
                        role = "Partner Pro",
                        phone = partner.phone,
                        badgeColor = if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFD1FAE5),
                        textColor = if (isDark) ServoraTheme.colors.success else Color(0xFF065F46),
                        onClick = { onSwitchToUser(partnerProfile) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // LIVE ACTIVE DISPATCHES
            Text(
                text = "Live Active Dispatches (${activeBookings.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ServoraTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (activeBookings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ServoraTheme.colors.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "All Done",
                            tint = if (isDark) ServoraTheme.colors.success else Color(0xFF009051),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All caught up! No pending jobs.",
                            fontWeight = FontWeight.SemiBold,
                            color = ServoraTheme.colors.textPrimary
                        )
                    }
                }
            }
        } else {
            items(activeBookings) { booking ->
                AdminBookingCard(
                    booking = booking,
                    onAdvanceStatus = { nextStatus ->
                        onAdvanceBookingStatus(booking.id, nextStatus)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AdminCustomersTab(
    customers: List<UserProfile>,
    allBookings: List<Booking>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSwitchToCustomer: (UserProfile) -> Unit
) {
    val isDark = ServoraTheme.colors.isDark
    val filtered = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.phone.contains(searchQuery, ignoreCase = true) ||
        it.locality.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        item {
            Text(
                text = "Customer Accounts (${customers.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ServoraTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, phone or locality...", color = ServoraTheme.colors.subtext) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ServoraTheme.colors.subtext) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdminIndigo,
                    focusedLabelColor = AdminIndigo,
                    unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                    unfocusedLabelColor = ServoraTheme.colors.subtext,
                    focusedTextColor = ServoraTheme.colors.textPrimary,
                    unfocusedTextColor = ServoraTheme.colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        items(filtered) { customer ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = customer.name.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) ServoraTheme.colors.warning else Color(0xFFB45309)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = customer.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ServoraTheme.colors.textPrimary
                                )
                                Text(
                                    text = "${customer.city} • ${customer.locality}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ServoraTheme.colors.subtext
                                )
                            }
                        }

                        // Verified Status Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFDCFCE7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Active",
                                    tint = if (isDark) ServoraTheme.colors.success else Color(0xFF009051),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) ServoraTheme.colors.success else Color(0xFF166534)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ServoraTheme.colors.cardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = "Phone", tint = ServoraTheme.colors.subtext, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = customer.phone, style = MaterialTheme.typography.bodySmall, color = ServoraTheme.colors.textPrimary)
                            }
                            Text(text = customer.email, style = MaterialTheme.typography.labelSmall, color = ServoraTheme.colors.subtext)
                        }

                        // Switch Button
                        Button(
                            onClick = { onSwitchToCustomer(customer) },
                            colors = ButtonDefaults.buttonColors(containerColor = AdminIndigo),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Switch", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch to User", fontSize = 12.sp, color = Color.White /* theme-invariant */)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPartnersTab(
    partners: List<Professional>,
    allBookings: List<Booking>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSwitchToPartner: (Professional) -> Unit
) {
    val isDark = ServoraTheme.colors.isDark
    val filtered = partners.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.specialty.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        item {
            Text(
                text = "Partner Technicians (${partners.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ServoraTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by partner name or trade...", color = ServoraTheme.colors.subtext) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ServoraTheme.colors.subtext) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdminIndigo,
                    focusedLabelColor = AdminIndigo,
                    unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                    unfocusedLabelColor = ServoraTheme.colors.subtext,
                    focusedTextColor = ServoraTheme.colors.textPrimary,
                    unfocusedTextColor = ServoraTheme.colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        items(filtered) { partner ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFD1FAE5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = partner.avatarInitials,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) ServoraTheme.colors.success else Color(0xFF065F46)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = partner.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ServoraTheme.colors.textPrimary
                                )
                                Text(
                                    text = partner.specialty,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ServoraTheme.colors.subtext
                                )
                            }
                        }

                        // Rating badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFFEF3C7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Rating", tint = if (isDark) ServoraTheme.colors.warning else AdminAmber, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "${partner.rating}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isDark) ServoraTheme.colors.warning else Color(0xFF92400E))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "Jobs: ${partner.completedJobs}", style = MaterialTheme.typography.labelSmall, color = ServoraTheme.colors.textPrimary)
                        Text(text = "Exp: ${partner.experienceYears} yrs", style = MaterialTheme.typography.labelSmall, color = ServoraTheme.colors.textPrimary)
                        Text(text = "Phone: ${partner.phone}", style = MaterialTheme.typography.labelSmall, color = ServoraTheme.colors.subtext)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ServoraTheme.colors.cardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "ON DUTY (ONLINE)",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) ServoraTheme.colors.success else Color(0xFF166534)
                            )
                        }

                        Button(
                            onClick = { onSwitchToPartner(partner) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Engineering, contentDescription = "Switch", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch to Partner", fontSize = 12.sp, color = Color.White /* theme-invariant */)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminBookingsTab(
    bookings: List<Booking>,
    selectedStatus: BookingStatus?,
    onStatusSelect: (BookingStatus?) -> Unit,
    onAdvanceStatus: (Long, BookingStatus) -> Unit
) {
    val filtered = if (selectedStatus == null) bookings else bookings.filter { it.status == selectedStatus }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        item {
            Text(
                text = "All System Bookings (${bookings.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ServoraTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { onStatusSelect(null) },
                        label = { Text("ALL (${bookings.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AdminIndigo,
                            selectedLabelColor = Color.White /* theme-invariant */
                        )
                    )
                }
                items(BookingStatus.entries) { st ->
                    val count = bookings.count { it.status == st }
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { onStatusSelect(st) },
                        label = { Text("${st.name} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AdminIndigo,
                            selectedLabelColor = Color.White /* theme-invariant */
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(filtered) { booking ->
            AdminBookingCard(
                booking = booking,
                onAdvanceStatus = { nextStatus ->
                    onAdvanceStatus(booking.id, nextStatus)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AdminProfileTab(
    currentAdmin: UserProfile,
    syncState: SupabaseSyncState,
    onSyncClick: () -> Unit,
    onLogout: () -> Unit
) {
    val isDark = ServoraTheme.colors.isDark
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AdminIndigo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = Color.White, /* theme-invariant */
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = currentAdmin.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ServoraTheme.colors.textPrimary
                )
                Text(
                    text = "${currentAdmin.email} • ${currentAdmin.phone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ServoraTheme.colors.subtext
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = "SUPER ADMIN • FULL ACCESS",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) ServoraTheme.colors.warning else Color(0xFFB45309)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ServoraTheme.colors.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Lock", tint = AdminIndigo, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Role switching is strictly enforced. Regular users must log out to switch accounts with credentials.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ServoraTheme.colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSyncClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AdminIndigo),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Trigger Cloud Sync Now", color = Color.White /* theme-invariant */)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Red, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out from Admin Console", color = Color.Red)
        }
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = ServoraTheme.colors.subtext
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ServoraTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun QuickUserSwitchCard(
    name: String,
    role: String,
    phone: String,
    badgeColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor
            ) {
                Text(
                    text = role,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = ServoraTheme.colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = phone,
                style = MaterialTheme.typography.labelSmall,
                color = ServoraTheme.colors.subtext,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Switch ➔",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdminIndigo
                )
            }
        }
    }
}

@Composable
private fun AdminBookingCard(
    booking: Booking,
    onAdvanceStatus: (BookingStatus) -> Unit
) {
    val isDark = ServoraTheme.colors.isDark
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.bookingCode,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = ServoraTheme.colors.textPrimary
                    )
                    Text(
                        text = booking.serviceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = ServoraTheme.colors.subtext
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) {
                        ServoraTheme.colors.surfaceVariant
                    } else {
                        when (booking.status) {
                            BookingStatus.COMPLETED -> Color(0xFFDCFCE7)
                            BookingStatus.ON_THE_WAY, BookingStatus.STARTED -> Color(0xFFDBEAFE)
                            BookingStatus.AWAITING_PAYMENT -> Color(0xFFFEF3C7)
                            BookingStatus.CANCELLED -> Color(0xFFFEE2E2)
                            else -> Color(0xFFFEF3C7)
                        }
                    }
                ) {
                    Text(
                        text = booking.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) {
                            when (booking.status) {
                                BookingStatus.COMPLETED -> ServoraTheme.colors.success
                                BookingStatus.ON_THE_WAY, BookingStatus.STARTED -> ServoraTheme.colors.info
                                BookingStatus.AWAITING_PAYMENT -> ServoraTheme.colors.warning
                                BookingStatus.CANCELLED -> ServoraTheme.colors.error
                                else -> ServoraTheme.colors.warning
                            }
                        } else {
                            when (booking.status) {
                                BookingStatus.COMPLETED -> Color(0xFF166534)
                                BookingStatus.ON_THE_WAY, BookingStatus.STARTED -> Color(0xFF1E40AF)
                                BookingStatus.AWAITING_PAYMENT -> Color(0xFF92400E)
                                BookingStatus.CANCELLED -> Color(0xFF991B1B)
                                else -> Color(0xFF92400E)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = ServoraTheme.colors.subtext, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = booking.addressText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ServoraTheme.colors.textPrimary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = "OTP", tint = if (isDark) ServoraTheme.colors.warning else AdminAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Doorstep OTP: ${booking.startOtp}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isDark) ServoraTheme.colors.warning else AdminAmber
                    )
                }

                val payStatusText = if (booking.isPaid) {
                    if (booking.paymentMethod.equals("UPI", ignoreCase = true)) "PAID • UPI" else "PAID • CASH"
                } else if (booking.status == BookingStatus.AWAITING_PAYMENT) {
                    "AWAITING PAYMENT"
                } else {
                    "PENDING (${booking.paymentMethod})"
                }
                Text(
                    text = "₹${booking.totalAmount} ($payStatusText)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ServoraTheme.colors.textPrimary
                )
            }

            if (!booking.paymentReference.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "UPI Ref / UTR: ${booking.paymentReference}",
                    fontSize = 11.sp,
                    color = ServoraTheme.colors.subtext
                )
            }

            // Quick status advancement for Admin
            if (booking.status != BookingStatus.COMPLETED && booking.status != BookingStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = ServoraTheme.colors.cardBorder)
                Spacer(modifier = Modifier.height(8.dp))

                val nextStatus = when (booking.status) {
                    BookingStatus.PENDING, BookingStatus.CONFIRMED -> BookingStatus.ASSIGNED
                    BookingStatus.ASSIGNED -> BookingStatus.ON_THE_WAY
                    BookingStatus.ON_THE_WAY -> BookingStatus.ARRIVED
                    BookingStatus.ARRIVED -> BookingStatus.STARTED
                    BookingStatus.STARTED -> BookingStatus.AWAITING_PAYMENT
                    else -> null
                }

                if (nextStatus != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onAdvanceStatus(nextStatus) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Advance ➔ ${nextStatus.name}", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
