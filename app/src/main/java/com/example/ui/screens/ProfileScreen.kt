package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CustomerReview
import com.example.data.model.SavedAddress
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.prefs.AppTheme
import com.example.data.prefs.RainbowColor
import com.example.data.remote.supabase.SupabaseSyncState
import com.example.ui.components.ThemeToggleButton
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.components.stableStatusBarsPadding
import com.example.ui.theme.ServoraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    savedAddresses: List<SavedAddress>,
    allBookings: List<Booking> = emptyList(),
    allReviews: List<CustomerReview> = emptyList(),
    syncState: SupabaseSyncState = SupabaseSyncState(),
    onSyncClick: () -> Unit = {},
    onUpdateProfile: (String, String, String) -> Unit = { _, _, _ -> },
    onSwitchRole: (UserRole) -> Unit = {},
    onMyBookingsClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onDeleteAddress: (Long) -> Unit = {},
    onAddNewAddress: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onLogout: () -> Unit = {},
    isDarkTheme: Boolean = false,
    selectedColor: RainbowColor = RainbowColor.GREEN,
    appTheme: AppTheme = AppTheme.LIGHT,
    onToggleTheme: () -> Unit = {},
    onSetTheme: (AppTheme) -> Unit = {},
    onSetColor: (RainbowColor) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = ServoraTheme.colors.isDark

    // Dialog / Sheet states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddressesSheet by remember { mutableStateOf(false) }
    var showPassDetailsSheet by remember { mutableStateOf(false) }
    var showWalletSheet by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }
    var showReferSheet by remember { mutableStateOf(false) }
    var showAboutUsSheet by remember { mutableStateOf(false) }
    var showTermsSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showAppearanceSheet by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Add Address Form State
    var showAddAddressForm by remember { mutableStateOf(false) }
    var addrTitle by remember { mutableStateOf("Home") }
    var addrText by remember { mutableStateOf("") }
    var addrLocality by remember { mutableStateOf("Taj Nagri") }
    var addrLandmark by remember { mutableStateOf("") }

    // Edit profile state
    var editName by remember { mutableStateOf(userProfile.name) }
    var editPhone by remember { mutableStateOf(userProfile.phone) }
    var editEmail by remember { mutableStateOf(userProfile.email) }

    // Partner Online State (if partner logged in)
    var isPartnerOnline by remember { mutableStateOf(true) }

    val completedBookingsCount = allBookings.count { it.status == BookingStatus.COMPLETED }
    val activeBookingsCount = allBookings.count {
        it.status != BookingStatus.COMPLETED && it.status != BookingStatus.CANCELLED
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .hideStatusBarOnScroll()
    ) {
        // =========================================================================
        // 1. TOP DYNAMIC THEME PROFILE HEADER
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            ServoraTheme.colors.headerBackgroundStart,
                            ServoraTheme.colors.headerBackgroundEnd
                        )
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .stableStatusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 26.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Avatar + User Info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Photo Container with Camera Badge
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(68.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ServoraTheme.colors.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar",
                                    tint = ServoraTheme.colors.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        // Small camera badge at bottom-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name, Phone & Edit Profile Link
                    Column {
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 21.sp,
                                letterSpacing = 0.sp
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = userProfile.phone,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color.White.copy(alpha = 0.88f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    editName = userProfile.name
                                    editPhone = userProfile.phone
                                    editEmail = userProfile.email
                                    showEditProfileDialog = true
                                }
                        ) {
                            Text(
                                text = "Edit profile",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = Color.White.copy(alpha = 0.95f)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Right: Appearance & Palette Button + Pencil Edit Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Appearance Palette Button
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { showAppearanceSheet = true },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = "Appearance & Theme",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                editName = userProfile.name
                                editPhone = userProfile.phone
                                editEmail = userProfile.email
                                showEditProfileDialog = true
                            },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // =====================================================================
            // 2. SERVICE ASSIST PASS PROMO BANNER (Theme-Adaptive)
            // =====================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPassDetailsSheet = true },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ServoraTheme.colors.brandGradientStart),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    ServoraTheme.colors.brandGradientStart,
                                    ServoraTheme.colors.brandGradientEnd
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Text Block
                        Column(modifier = Modifier.weight(1f)) {
                            // Pill Badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.9f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = ServoraTheme.colors.brandGradientEnd,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SERVICE ASSIST PASS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            letterSpacing = 0.4.sp
                                        ),
                                        color = ServoraTheme.colors.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Get 3 Visits for ₹99 only",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 17.sp
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "More visits. More value.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        // Right 3D Pass Stack Mockup
                        Box(
                            modifier = Modifier
                                .size(width = 86.dp, height = 70.dp)
                                .padding(start = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 58.dp)
                                    .offset(x = (-12).dp, y = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {}

                            Surface(
                                modifier = Modifier
                                    .size(width = 62.dp, height = 64.dp)
                                    .offset(x = 6.dp, y = (-2).dp),
                                shape = RoundedCornerShape(10.dp),
                                color = ServoraTheme.colors.brandGradientEnd,
                                shadowElevation = 4.dp,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "SERVICE ASSIST",
                                        fontSize = 5.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "PASS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        letterSpacing = 0.sp
                                    )
                                    Text(
                                        text = "60 MIN",
                                        fontSize = 6.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =====================================================================
            // 3. THREE QUICK ACTION CARDS (My Bookings, Wallet, Help & Support)
            // =====================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: My Bookings
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(118.dp)
                        .clickable { onMyBookingsClick() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EventAvailable,
                                    contentDescription = "My Bookings",
                                    tint = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = ServoraTheme.colors.subtext.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "My\nBookings",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    lineHeight = 17.sp
                                ),
                                color = ServoraTheme.colors.textPrimary
                            )
                        }
                    }
                }

                // Card 2: Wallet
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(118.dp)
                        .clickable { showWalletSheet = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = "Service Assist Money",
                                    tint = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // ₹0 Balance Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer
                            ) {
                                Text(
                                    text = "₹0",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "Service\nAssist Money",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.5.sp,
                                    lineHeight = 15.sp
                                ),
                                color = ServoraTheme.colors.textPrimary,
                                maxLines = 2
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = ServoraTheme.colors.subtext.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Card 3: Help & Support
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(118.dp)
                        .clickable { showSupportSheet = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HeadsetMic,
                                    contentDescription = "Help & Support",
                                    tint = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = ServoraTheme.colors.subtext.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Help &\nSupport",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    lineHeight = 17.sp
                                ),
                                color = ServoraTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }

            // =====================================================================
            // 4. ROLE & PARTNER / ADMIN CONSOLE (Conditional for Partner)
            // =====================================================================
            if (userProfile.role == UserRole.PROFESSIONAL) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "PARTNER DUTY CONSOLE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer
                                )
                                Text(
                                    text = if (isPartnerOnline) "Status: Online (Accepting Jobs)" else "Status: Offline",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ServoraTheme.colors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = isPartnerOnline,
                                onCheckedChange = { isPartnerOnline = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ServoraTheme.colors.primary,
                                    checkedTrackColor = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("COMPLETED", fontSize = 11.sp, color = ServoraTheme.colors.subtext)
                                Text("$completedBookingsCount Jobs", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ServoraTheme.colors.textPrimary)
                            }
                            Column {
                                Text("ACTIVE IN-FLIGHT", fontSize = 11.sp, color = ServoraTheme.colors.subtext)
                                Text(
                                    "$activeBookingsCount Active",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer
                                )
                            }
                            Column {
                                Text("RATING", fontSize = 11.sp, color = ServoraTheme.colors.subtext)
                                Text("4.9 ★", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) ServoraTheme.colors.warning else Color(0xFFD97706))
                            }
                        }
                    }
                }
            }

            // =====================================================================
            // 5. MENU ITEMS LIST (Including new Appearance Menu)
            // =====================================================================

            // Item 1: Appearance & Theme (New Rainbow + Dark Mode Menu)
            GreenMenuItemCard(
                icon = Icons.Outlined.Palette,
                title = "Appearance & Theme",
                badgeText = selectedColor.displayName,
                onClick = { showAppearanceSheet = true }
            )

            // Item 2: Refer & earn (with ₹100 badge)
            GreenMenuItemCard(
                icon = Icons.Outlined.CardGiftcard,
                title = "Refer & earn",
                badgeText = "₹100",
                onClick = { showReferSheet = true }
            )

            // Item 3: Saved addresses
            GreenMenuItemCard(
                icon = Icons.Outlined.LocationOn,
                title = "Saved addresses",
                onClick = { showAddressesSheet = true }
            )

            // Item 4: About us
            GreenMenuItemCard(
                icon = Icons.Outlined.Info,
                title = "About us",
                onClick = { showAboutUsSheet = true }
            )

            // Item 5: Terms of services
            GreenMenuItemCard(
                icon = Icons.Outlined.Description,
                title = "Terms of services",
                onClick = { showTermsSheet = true }
            )

            // Item 6: Privacy policy
            GreenMenuItemCard(
                icon = Icons.Outlined.Shield,
                title = "Privacy policy",
                onClick = { showPrivacySheet = true }
            )

            // Item 7: Request account deletion
            GreenMenuItemCard(
                icon = Icons.Outlined.PersonRemove,
                title = "Request account deletion",
                onClick = { showDeleteAccountDialog = true }
            )

            // Item 8: Log out
            GreenMenuItemCard(
                icon = Icons.AutoMirrored.Outlined.Logout,
                title = "Log out",
                onClick = { showLogoutConfirmDialog = true },
                isDestructive = true
            )

            // =====================================================================
            // 6. FOOTER: APP VERSION
            // =====================================================================
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "APP VERSION: 1.5.8 (Rainbow Edition)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    color = ServoraTheme.colors.subtext.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ServiceAssist • Agra Smart Services",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = ServoraTheme.colors.subtext.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // =========================================================================
    // MODAL SHEETS & DIALOGS
    // =========================================================================

    // 0. APPEARANCE & THEME BOTTOM SHEET (Rainbow Colors + Dark Mode)
    if (showAppearanceSheet) {
        AppearanceBottomSheet(
            currentTheme = appTheme,
            selectedColor = selectedColor,
            isDark = isDark,
            onThemeChange = { newTheme ->
                onSetTheme(newTheme)
            },
            onColorChange = { newColor ->
                onSetColor(newColor)
            },
            onDismiss = { showAppearanceSheet = false }
        )
    }

    // 1. EDIT PROFILE DIALOG
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = ServoraTheme.colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Update your profile information for Agra doorstep services.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ServoraTheme.colors.subtext
                    )
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ServoraTheme.colors.primary,
                            focusedLabelColor = ServoraTheme.colors.primary,
                            unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                            unfocusedLabelColor = ServoraTheme.colors.subtext,
                            focusedTextColor = ServoraTheme.colors.textPrimary,
                            unfocusedTextColor = ServoraTheme.colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ServoraTheme.colors.primary,
                            focusedLabelColor = ServoraTheme.colors.primary,
                            unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                            unfocusedLabelColor = ServoraTheme.colors.subtext,
                            focusedTextColor = ServoraTheme.colors.textPrimary,
                            unfocusedTextColor = ServoraTheme.colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ServoraTheme.colors.primary,
                            focusedLabelColor = ServoraTheme.colors.primary,
                            unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                            unfocusedLabelColor = ServoraTheme.colors.subtext,
                            focusedTextColor = ServoraTheme.colors.textPrimary,
                            unfocusedTextColor = ServoraTheme.colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateProfile(editName, editPhone, editEmail)
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary)
                ) {
                    Text("Save Changes", color = ServoraTheme.colors.onPrimary)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = ServoraTheme.colors.textPrimary)
                }
            }
        )
    }

    // 2. SAVED ADDRESSES SHEET
    if (showAddressesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddressesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
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
                        text = "Saved Addresses (${savedAddresses.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = ServoraTheme.colors.textPrimary
                    )
                    IconButton(onClick = { showAddAddressForm = !showAddAddressForm }) {
                        Icon(
                            imageVector = if (showAddAddressForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add Address",
                            tint = ServoraTheme.colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (showAddAddressForm) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ServoraTheme.colors.surfaceVariant),
                        border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Add New Address", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = ServoraTheme.colors.textPrimary)
                            OutlinedTextField(
                                value = addrTitle,
                                onValueChange = { addrTitle = it },
                                label = { Text("Title (Home/Office/Parents)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ServoraTheme.colors.primary,
                                    focusedLabelColor = ServoraTheme.colors.primary,
                                    unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                                    unfocusedLabelColor = ServoraTheme.colors.subtext,
                                    focusedTextColor = ServoraTheme.colors.textPrimary,
                                    unfocusedTextColor = ServoraTheme.colors.textPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = addrText,
                                onValueChange = { addrText = it },
                                label = { Text("House / Flat / Building / Road") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ServoraTheme.colors.primary,
                                    focusedLabelColor = ServoraTheme.colors.primary,
                                    unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                                    unfocusedLabelColor = ServoraTheme.colors.subtext,
                                    focusedTextColor = ServoraTheme.colors.textPrimary,
                                    unfocusedTextColor = ServoraTheme.colors.textPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = addrLocality,
                                onValueChange = { addrLocality = it },
                                label = { Text("Locality (e.g. Dayalbagh, Taj Nagri)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ServoraTheme.colors.primary,
                                    focusedLabelColor = ServoraTheme.colors.primary,
                                    unfocusedBorderColor = ServoraTheme.colors.cardBorder,
                                    unfocusedLabelColor = ServoraTheme.colors.subtext,
                                    focusedTextColor = ServoraTheme.colors.textPrimary,
                                    unfocusedTextColor = ServoraTheme.colors.textPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (addrText.isNotBlank()) {
                                        onAddNewAddress(addrTitle, addrText, addrLocality, addrLandmark)
                                        showAddAddressForm = false
                                        addrText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Address", color = ServoraTheme.colors.onPrimary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    savedAddresses.forEach { addr ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = addr.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp,
                                                color = ServoraTheme.colors.textPrimary
                                            )
                                            if (addr.isDefault) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "DEFAULT",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                        Text(text = addr.fullAddress, fontSize = 13.sp, color = ServoraTheme.colors.subtext)
                                        Text(text = "${addr.locality}, Agra", fontSize = 12.sp, color = ServoraTheme.colors.subtext.copy(alpha = 0.7f))
                                    }
                                }

                                IconButton(onClick = { onDeleteAddress(addr.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. PASS DETAILS SHEET
    if (showPassDetailsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPassDetailsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Service Assist Pass",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = ServoraTheme.colors.textPrimary
                )

                Text(
                    text = "Get 3 doorstep visits across Agra for only ₹99. Save up to ₹300 on visiting fees across electrical, AC, cleaning, and plumbing services.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ServoraTheme.colors.subtext
                )

                Spacer(modifier = Modifier.height(16.dp))

                PassPerkRow("Zero Visiting Charges on 3 Bookings")
                Spacer(modifier = Modifier.height(8.dp))
                PassPerkRow("Priority Partner Allocation in 15 Mins")
                Spacer(modifier = Modifier.height(8.dp))
                PassPerkRow("Valid across all Agra localities for 60 days")

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showPassDetailsSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Activate Service Assist Pass for ₹99", fontWeight = FontWeight.SemiBold, color = ServoraTheme.colors.onPrimary)
                }
            }
        }
    }

    // 4. WALLET SHEET
    if (showWalletSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWalletSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Service Assist Wallet",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = ServoraTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer,
                    border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AVAILABLE BALANCE", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = ServoraTheme.colors.subtext)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹0.00", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Use wallet cash on any service in Agra", fontSize = 12.sp, color = ServoraTheme.colors.subtext)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showWalletSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Add Money to Wallet", fontWeight = FontWeight.Medium, color = ServoraTheme.colors.onPrimary)
                }
            }
        }
    }

    // 5. HELP & SUPPORT SHEET
    if (showSupportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSupportSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "24x7 Customer Support",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = ServoraTheme.colors.textPrimary
                )
                Text(
                    text = "Dedicated support team for Agra doorstep services",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ServoraTheme.colors.subtext
                )

                Spacer(modifier = Modifier.height(16.dp))

                SupportActionTile(
                    icon = Icons.Default.Call,
                    title = "Call Support (Toll-Free)",
                    subtitle = "1800-SERVORA-AGRA (9 AM - 9 PM)"
                )
                Spacer(modifier = Modifier.height(10.dp))
                SupportActionTile(
                    icon = Icons.Default.HeadsetMic,
                    title = "WhatsApp Chat Support",
                    subtitle = "Instant technician tracking & query resolution"
                )
                Spacer(modifier = Modifier.height(10.dp))
                SupportActionTile(
                    icon = Icons.Default.Security,
                    title = "₹10,000 Damage Cover",
                    subtitle = "All bookings covered with verified safety assurance"
                )
            }
        }
    }

    // 6. REFER & EARN SHEET
    if (showReferSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReferSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Redeem,
                        contentDescription = null,
                        tint = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Refer Friends & Earn ₹100",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = ServoraTheme.colors.textPrimary
                )

                Text(
                    text = "Share your referral code. When your friend completes their first service in Agra, you both receive ₹100 in your wallet!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ServoraTheme.colors.subtext,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer,
                    border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                ) {
                    Text(
                        text = "YOUR CODE: SERVORA100",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showReferSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = ServoraTheme.colors.onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Referral Link", fontWeight = FontWeight.Medium, color = ServoraTheme.colors.onPrimary)
                }
            }
        }
    }

    // 7. ABOUT US SHEET
    if (showAboutUsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAboutUsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "About ServiceAssist",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = ServoraTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ServiceAssist (Servora) is Agra's premier on-demand doorstep home services platform. We connect verified electricians, AC jet cleaning technicians, plumbers, and salon experts with residents across Taj Nagri, Dayalbagh, Sanjay Place, and all major localities in Agra.\n\nEvery professional undergoes a multi-stage background check, skill verification, and standardized safety training.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ServoraTheme.colors.subtext,
                    lineHeight = 22.sp
                )
            }
        }
    }

    // 8. TERMS SHEET
    if (showTermsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTermsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = ServoraTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Services booked through ServiceAssist are delivered by certified third-party service partners in Agra.\n2. Customers must verify the 4-digit doorstep Start OTP before initiating any job.\n3. Cancellation is completely free up to 30 minutes before the scheduled time slot.\n4. All pricing includes standard taxes and verified service equipment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ServoraTheme.colors.subtext,
                    lineHeight = 22.sp
                )
            }
        }
    }

    // 9. PRIVACY SHEET
    if (showPrivacySheet) {
        ModalBottomSheet(
            onDismissRequest = { showPrivacySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = ServoraTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your privacy is paramount. Your address and contact details are only shared with the assigned professional for the duration of the active booking. All data is encrypted in transit and at rest using industry standard encryption protocols.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ServoraTheme.colors.subtext,
                    lineHeight = 22.sp
                )
            }
        }
    }

    // 10. REQUEST ACCOUNT DELETION DIALOG
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text("Request Account Deletion", fontWeight = FontWeight.Medium, color = ServoraTheme.colors.textPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to request deletion of your ServiceAssist account? All saved addresses, past service invoices, and wallet credits will be permanently removed after verification.",
                    color = ServoraTheme.colors.subtext,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Deletion", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = ServoraTheme.colors.textPrimary)
                }
            }
        )
    }

    // 11. LOGOUT CONFIRMATION DIALOG
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text("Log Out", fontWeight = FontWeight.Medium, color = ServoraTheme.colors.textPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to log out of ${userProfile.name}?",
                    color = ServoraTheme.colors.subtext,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Log Out", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel", color = ServoraTheme.colors.textPrimary)
                }
            }
        )
    }
}

// =============================================================================
// APPEARANCE BOTTOM SHEET (Rainbow Colors + Dark Mode + WCAG AA Legibility)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppearanceBottomSheet(
    currentTheme: AppTheme,
    selectedColor: RainbowColor,
    isDark: Boolean,
    onThemeChange: (AppTheme) -> Unit,
    onColorChange: (RainbowColor) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
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
                            .background(if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 19.sp
                            ),
                            color = ServoraTheme.colors.textPrimary
                        )
                        Text(
                            text = "Customize your app theme color and mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = ServoraTheme.colors.subtext
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ServoraTheme.colors.subtext
                    )
                }
            }

            HorizontalDivider(color = ServoraTheme.colors.divider)

            // Section 1: Dark Mode / Light Mode Segmented Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "THEME MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = ServoraTheme.colors.subtext
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Light Mode Button
                    ThemeModeCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.LightMode,
                        title = "Light",
                        isSelected = currentTheme == AppTheme.LIGHT,
                        onClick = { onThemeChange(AppTheme.LIGHT) }
                    )

                    // Dark Mode Button
                    ThemeModeCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.DarkMode,
                        title = "Dark",
                        isSelected = currentTheme == AppTheme.DARK,
                        onClick = { onThemeChange(AppTheme.DARK) }
                    )

                    // System Mode Button
                    ThemeModeCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.BrightnessAuto,
                        title = "System",
                        isSelected = currentTheme == AppTheme.SYSTEM,
                        onClick = { onThemeChange(AppTheme.SYSTEM) }
                    )
                }
            }

            // Section 2: Rainbow Color Spectrum Palette
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RAINBOW COLOR THEME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        color = ServoraTheme.colors.subtext
                    )

                    Text(
                        text = selectedColor.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ServoraTheme.colors.primary
                    )
                }

                // Grid / FlowRow of Rainbow Color Swatches
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 4
                ) {
                    RainbowColor.entries.forEach { colorOption ->
                        val isSelected = selectedColor == colorOption
                        val colorLight = Color(colorOption.primaryLightHex)
                        val colorDark = Color(colorOption.primaryDarkHex)
                        val activeColor = if (isDark) colorDark else colorLight

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onColorChange(colorOption) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) activeColor else ServoraTheme.colors.cardBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(activeColor)
                                        .shadow(elevation = if (isSelected) 3.dp else 0.dp, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (isDark) Color(0xFF0F1412) else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = colorOption.displayName.replace(" ", "\n"),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ServoraTheme.colors.textPrimary else ServoraTheme.colors.subtext,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 13.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Live Interactive Preview Card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LIVE PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = ServoraTheme.colors.subtext
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${selectedColor.displayName} Theme",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = ServoraTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = if (isDark) "Dark Mode • High Contrast" else "Light Mode • High Contrast",
                                        fontSize = 11.sp,
                                        color = ServoraTheme.colors.subtext
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ServoraTheme.colors.primary
                            ) {
                                Text(
                                    text = "Active",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ServoraTheme.colors.onPrimary
                                )
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = ServoraTheme.colors.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Apply Theme & Close",
                                fontWeight = FontWeight.SemiBold,
                                color = ServoraTheme.colors.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = ServoraTheme.colors.isDark
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) ServoraTheme.colors.primary else ServoraTheme.colors.cardBorder
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) ServoraTheme.colors.primary else ServoraTheme.colors.subtext,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) ServoraTheme.colors.textPrimary else ServoraTheme.colors.subtext
            )
        }
    }
}

// =============================================================================
// SUB-COMPONENTS & HELPERS
// =============================================================================

@Composable
private fun GreenMenuItemCard(
    icon: ImageVector,
    title: String,
    badgeText: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = ServoraTheme.colors.isDark
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDestructive) {
                                if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
                            } else {
                                if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isDestructive) (if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)) else (if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    ),
                    color = if (isDestructive) (if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)) else ServoraTheme.colors.textPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.primary
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ServoraTheme.colors.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = ServoraTheme.colors.subtext.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun PassPerkRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ServoraTheme.colors.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, fontSize = 13.sp, color = ServoraTheme.colors.textPrimary)
    }
}

@Composable
private fun SupportActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val isDark = ServoraTheme.colors.isDark
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ServoraTheme.colors.surfaceVariant,
        border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) MaterialTheme.colorScheme.surface else ServoraTheme.colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDark) ServoraTheme.colors.primary else ServoraTheme.colors.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = ServoraTheme.colors.textPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = ServoraTheme.colors.subtext)
            }
        }
    }
}
