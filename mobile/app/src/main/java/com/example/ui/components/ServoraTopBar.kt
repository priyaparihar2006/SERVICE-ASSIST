package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.ServoraCharcoal
import com.example.ui.theme.ServoraCoral
import com.example.ui.theme.ServoraPeach

@Composable
fun ServoraTopBar(
    selectedCity: String,
    selectedLocality: String,
    userRole: UserRole,
    onLocationClick: () -> Unit,
    onSearchClick: () -> Unit,
    isImpersonating: Boolean = false,
    onReturnToAdminClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCustomer = userRole == UserRole.CUSTOMER

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = if (isCustomer) 10.dp else 12.dp)
        ) {
            if (isCustomer) {
                // ==================== REDESIGNED CUSTOMER HEADER ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location: Green Pin + City + Subtitle Dropdown
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLocationClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF009051),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedCity.ifBlank { "Agra" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = Color(0xFF172019)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedLocality.ifBlank { "Fatehabad Road" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = Color(0xFF68736B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Dropdown",
                                    tint = Color(0xFF68736B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right action: Admin Return or Notification Bell Icon
                    if (isImpersonating && onReturnToAdminClick != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF4F46E5))
                                .clickable { onReturnToAdminClick() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("return_admin_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Return to Admin",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Admin Mode ➔",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        // Green Notification Bell Icon Button
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF009051),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar matching exact reference
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSearchClick() }
                        .testTag("top_search_bar"),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2EBE6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF009051),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Search services, home repairs...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF68736B)
                            )
                        )
                    }
                }
            } else {
                // ==================== PARTNER / ADMIN HEADER ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brand Logo & City
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Service Assist",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFF0B5433)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(ServoraCoral)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Location Selector Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(ServoraPeach)
                                .clickable { onLocationClick() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("location_pill"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = ServoraCoral,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$selectedCity • $selectedLocality",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                color = ServoraCharcoal,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select City",
                                tint = ServoraCharcoal,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Action Icons: Quick Search + Role / Admin Return Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("top_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Services",
                                tint = ServoraCharcoal,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Role Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    when (userRole) {
                                        UserRole.CUSTOMER -> MaterialTheme.colorScheme.surfaceVariant
                                        UserRole.PROFESSIONAL -> Color(0xFFD1FAE5)
                                        UserRole.ADMIN -> Color(0xFFEEF2FF)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("role_badge"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (userRole) {
                                    UserRole.CUSTOMER -> Icons.Outlined.Person
                                    UserRole.PROFESSIONAL -> Icons.Default.Handyman
                                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                },
                                contentDescription = "User Mode",
                                tint = when (userRole) {
                                    UserRole.CUSTOMER -> ServoraCharcoal
                                    UserRole.PROFESSIONAL -> Color(0xFF065F46)
                                    UserRole.ADMIN -> Color(0xFF4F46E5)
                                },
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (userRole) {
                                    UserRole.CUSTOMER -> "Customer"
                                    UserRole.PROFESSIONAL -> "Partner"
                                    UserRole.ADMIN -> "Admin Console"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (userRole) {
                                    UserRole.CUSTOMER -> ServoraCharcoal
                                    UserRole.PROFESSIONAL -> Color(0xFF065F46)
                                    UserRole.ADMIN -> Color(0xFF4F46E5)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

