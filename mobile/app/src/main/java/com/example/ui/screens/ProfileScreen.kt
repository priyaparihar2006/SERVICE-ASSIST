package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
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
import com.example.data.remote.supabase.SupabaseSyncState
import com.example.ui.components.hideStatusBarOnScroll

// Primary Green Palette matching #009051 brand theme
private val DeepForestGreen = Color(0xFF009051)
private val ForestGreenGradient = listOf(
    Color(0xFF009051),
    Color(0xFF008249),
    Color(0xFF007542)
)
private val MintLightBg = Color(0xFFE6F5EE)
private val MintBadgeBg = Color(0xFFDCF4E9)
private val MintBadgeText = Color(0xFF00703E)
private val VibrantMint = Color(0xFF009051)
private val MintLinkText = Color(0xFF86EFAC)
private val CardBorderColor = Color(0xFFF1F5F9)
private val SlateDarkText = Color(0xFF1E293B)
private val SlateMutedText = Color(0xFF64748B)

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
    modifier: Modifier = Modifier
) {
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
            .background(Color(0xFFF7FBF9))
            .verticalScroll(rememberScrollState())
            .hideStatusBarOnScroll()
    ) {
        // =========================================================================
        // 1. TOP GREEN WAVE PROFILE HEADER
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(ForestGreenGradient),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .statusBarsPadding()
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
                                    .background(Color(0xFFE2F5EC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar",
                                    tint = DeepForestGreen,
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
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                letterSpacing = (-0.3).sp
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
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = MintLinkText
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MintLinkText,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Right: Circular Pencil Edit Button
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
                    color = Color.White.copy(alpha = 0.18f)
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

        Spacer(modifier = Modifier.height(14.dp))

        // Content Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // =====================================================================
            // 2. SERVICE ASSIST PASS PROMO BANNER
            // =====================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPassDetailsSheet = true },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF00A45C)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF00A45C), Color(0xFF008E50))
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
                                color = Color(0xFFDCFCE7).copy(alpha = 0.95f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SERVICE ASSIST PASS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.5.sp,
                                            letterSpacing = 0.4.sp
                                        ),
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Get 3 Visits for ₹99 only",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
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
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // Right 3D Green Pass Stack Mockup
                        Box(
                            modifier = Modifier
                                .size(width = 86.dp, height = 70.dp)
                                .padding(start = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Back card
                            Surface(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 58.dp)
                                    .offset(x = (-12).dp, y = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF22C55E).copy(alpha = 0.5f)
                            ) {}

                            // Front Pass Card
                            Surface(
                                modifier = Modifier
                                    .size(width = 62.dp, height = 64.dp)
                                    .offset(x = 6.dp, y = (-2).dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF16A34A),
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
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "PASS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        text = "60 MIN",
                                        fontSize = 6.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFDCFCE7)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =====================================================================
            // 3. THREE QUICK ACTION CARDS (My Bookings, Service Assist Money, Help & Support)
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor),
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
                                    .background(MintLightBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EventAvailable,
                                    contentDescription = "My Bookings",
                                    tint = DeepForestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = SlateMutedText.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "My\nBookings",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    lineHeight = 17.sp
                                ),
                                color = SlateDarkText
                            )
                        }
                    }
                }

                // Card 2: Service Assist Money (Wallet)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(118.dp)
                        .clickable { showWalletSheet = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor),
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
                                    .background(MintLightBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = "Service Assist Money",
                                    tint = DeepForestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // ₹0 Balance Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MintBadgeBg
                            ) {
                                Text(
                                    text = "₹0",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintBadgeText
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
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    lineHeight = 15.sp
                                ),
                                color = SlateDarkText,
                                maxLines = 2
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = SlateMutedText.copy(alpha = 0.6f),
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor),
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
                                    .background(MintLightBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HeadsetMic,
                                    contentDescription = "Help & Support",
                                    tint = DeepForestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = SlateMutedText.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Help &\nSupport",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    lineHeight = 17.sp
                                ),
                                color = SlateDarkText
                            )
                        }
                    }
                }
            }


            // =====================================================================
            // 5. ROLE & PARTNER / ADMIN CONSOLE (Conditional for Partner / Admin)
            // =====================================================================
            if (userProfile.role == UserRole.PROFESSIONAL) {
                // Partner Dashboard Quick Widget
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PARTNER DUTY CONSOLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepForestGreen)
                                Text(
                                    text = if (isPartnerOnline) "Status: Online (Accepting Jobs)" else "Status: Offline",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateDarkText
                                )
                            }
                            Switch(
                                checked = isPartnerOnline,
                                onCheckedChange = { isPartnerOnline = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = VibrantMint, checkedTrackColor = MintBadgeBg)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("COMPLETED", fontSize = 10.sp, color = SlateMutedText)
                                Text("$completedBookingsCount Jobs", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateDarkText)
                            }
                            Column {
                                Text("ACTIVE IN-FLIGHT", fontSize = 10.sp, color = SlateMutedText)
                                Text("$activeBookingsCount Active", fontSize = 16.sp, fontWeight = FontWeight.Black, color = DeepForestGreen)
                            }
                            Column {
                                Text("RATING", fontSize = 10.sp, color = SlateMutedText)
                                Text("4.9 ★", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFD97706))
                            }
                        }
                    }
                }
            }

            // =====================================================================
            // 6. MENU ITEMS LIST (MATCHING EXACT GREEN THEME SCREENSHOT)
            // =====================================================================

            // Item 1: My bookings
            GreenMenuItemCard(
                icon = Icons.Outlined.CalendarMonth,
                title = "My bookings",
                badgeText = if (activeBookingsCount > 0) "$activeBookingsCount active" else null,
                onClick = { onMyBookingsClick() }
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

            // Item 3: About us
            GreenMenuItemCard(
                icon = Icons.Outlined.Info,
                title = "About us",
                onClick = { showAboutUsSheet = true }
            )

            // Item 4: Terms of services
            GreenMenuItemCard(
                icon = Icons.Outlined.Article,
                title = "Terms of services",
                onClick = { showTermsSheet = true }
            )

            // Item 5: Privacy policy
            GreenMenuItemCard(
                icon = Icons.Outlined.Shield,
                title = "Privacy policy",
                onClick = { showPrivacySheet = true }
            )

            // Item 6: Request account deletion
            GreenMenuItemCard(
                icon = Icons.Outlined.PersonRemove,
                title = "Request account deletion",
                onClick = { showDeleteAccountDialog = true }
            )

            // Item 7: Log out
            GreenMenuItemCard(
                icon = Icons.AutoMirrored.Outlined.Logout,
                title = "Log out",
                onClick = { showLogoutConfirmDialog = true },
                isDestructive = true
            )

            // =====================================================================
            // 7. FOOTER: APP VERSION & ORGANIC DECORATION
            // =====================================================================
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "APP VERSION: 1.5.8 (8a48)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    color = SlateMutedText.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ServiceAssist • Agra Smart Services",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = SlateMutedText.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // =========================================================================
    // MODAL SHEETS & DIALOGS
    // =========================================================================

    // 1. EDIT PROFILE DIALOG
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateDarkText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Update your profile information for Agra doorstep services.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMutedText
                    )
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
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
                    colors = ButtonDefaults.buttonColors(containerColor = DeepForestGreen)
                ) {
                    Text("Save Changes", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. SAVED ADDRESSES SHEET
    if (showAddressesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddressesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
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
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SlateDarkText
                    )
                    IconButton(onClick = { showAddAddressForm = !showAddAddressForm }) {
                        Icon(
                            imageVector = if (showAddAddressForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add Address",
                            tint = DeepForestGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (showAddAddressForm) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Add New Address", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            OutlinedTextField(
                                value = addrTitle,
                                onValueChange = { addrTitle = it },
                                label = { Text("Title (Home/Office/Parents)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = addrText,
                                onValueChange = { addrText = it },
                                label = { Text("House / Flat / Building / Road") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = addrLocality,
                                onValueChange = { addrLocality = it },
                                label = { Text("Locality (e.g. Dayalbagh, Taj Nagri)") },
                                singleLine = true,
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
                                colors = ButtonDefaults.buttonColors(containerColor = DeepForestGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Address", color = Color.White)
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
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                                            .background(MintLightBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = DeepForestGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = addr.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = SlateDarkText
                                            )
                                            if (addr.isDefault) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MintBadgeBg
                                                ) {
                                                    Text(
                                                        text = "DEFAULT",
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MintBadgeText
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${addr.fullAddress}, ${addr.locality}, ${addr.city}",
                                            fontSize = 12.sp,
                                            color = SlateMutedText
                                        )
                                    }
                                }

                                IconButton(onClick = { onDeleteAddress(addr.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. SERVICE ASSIST PASS DETAILS SHEET
    if (showPassDetailsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPassDetailsSheet = false },
            containerColor = Color.White
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
                        .background(MintLightBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = DeepForestGreen,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Service Assist Pass",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateDarkText
                )

                Text(
                    text = "Unlock 3 Doorstep Visits across Agra for only ₹99",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMutedText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PassPerkRow("₹0 Visiting Charges on AC, Electrical & Cleaning")
                    PassPerkRow("Guaranteed 44-minute Priority Partner Arrival")
                    PassPerkRow("Valid for 60 days across all localities in Agra")
                    PassPerkRow("100% Satisfaction or full refund guarantee")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showPassDetailsSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Activate Service Assist Pass for ₹99", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // 4. SERVICE ASSIST MONEY / WALLET SHEET
    if (showWalletSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWalletSheet = false },
            containerColor = Color.White
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
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateDarkText
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MintLightBg,
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AVAILABLE BALANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateMutedText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹0.00", fontSize = 32.sp, fontWeight = FontWeight.Black, color = DeepForestGreen)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Use wallet cash on any service in Agra", fontSize = 12.sp, color = SlateMutedText)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showWalletSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Add Money to Wallet", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // 5. HELP & SUPPORT SHEET
    if (showSupportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSupportSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "24x7 Customer Support",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateDarkText
                )
                Text(
                    text = "Dedicated support team for Agra doorstep services",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMutedText
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
            containerColor = Color.White
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
                        .background(MintLightBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Redeem,
                        contentDescription = null,
                        tint = DeepForestGreen,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Refer Friends & Earn ₹100",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateDarkText
                )

                Text(
                    text = "Share your referral code. When your friend completes their first service in Agra, you both receive ₹100 in your wallet!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMutedText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = "YOUR CODE: SERVORA100",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = DeepForestGreen
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showReferSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Referral Link", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // 7. ABOUT US SHEET
    if (showAboutUsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAboutUsSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "About ServiceAssist",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateDarkText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ServiceAssist (Servora) is Agra's premier on-demand doorstep home services platform. We connect verified electricians, AC jet cleaning technicians, plumbers, and salon experts with residents across Taj Nagri, Dayalbagh, Sanjay Place, and all major localities in Agra.\n\nEvery professional undergoes a multi-stage background check, skill verification, and standardized safety training.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMutedText,
                    lineHeight = 22.sp
                )
            }
        }
    }

    // 8. TERMS SHEET
    if (showTermsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTermsSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateDarkText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Services booked through ServiceAssist are delivered by certified third-party service partners in Agra.\n2. Customers must verify the 4-digit doorstep Start OTP before initiating any job.\n3. Cancellation is completely free up to 30 minutes before the scheduled time slot.\n4. All pricing includes standard taxes and verified service equipment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMutedText,
                    lineHeight = 22.sp
                )
            }
        }
    }

    // 9. PRIVACY SHEET
    if (showPrivacySheet) {
        ModalBottomSheet(
            onDismissRequest = { showPrivacySheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SlateDarkText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your privacy is paramount. Your address and contact details are only shared with the assigned professional for the duration of the active booking. All data is encrypted in transit and at rest using industry standard encryption protocols.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMutedText,
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
                Text("Request Account Deletion", fontWeight = FontWeight.Bold, color = SlateDarkText)
            },
            text = {
                Text(
                    "Are you sure you want to request deletion of your ServiceAssist account? All saved addresses, past service invoices, and wallet credits will be permanently removed after verification.",
                    color = SlateMutedText,
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
                    Text("Cancel")
                }
            }
        )
    }

    // 11. LOGOUT CONFIRMATION DIALOG
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text("Log Out", fontWeight = FontWeight.Bold, color = SlateDarkText)
            },
            text = {
                Text(
                    "Are you sure you want to log out of ${userProfile.name}?",
                    color = SlateMutedText,
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
                    Text("Cancel")
                }
            }
        )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorderColor),
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
                // Soft Mint Icon Container
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDestructive) Color(0xFFFEE2E2) else MintLightBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isDestructive) Color(0xFFDC2626) else DeepForestGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = if (isDestructive) Color(0xFFDC2626) else SlateDarkText
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VibrantMint
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = SlateMutedText.copy(alpha = 0.6f),
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
            tint = VibrantMint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, fontSize = 13.sp, color = SlateDarkText)
    }
}

@Composable
private fun SupportActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                    .background(MintLightBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepForestGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateDarkText)
                Text(text = subtitle, fontSize = 12.sp, color = SlateMutedText)
            }
        }
    }
}

