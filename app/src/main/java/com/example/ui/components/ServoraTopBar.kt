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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
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
import com.example.ui.theme.ServoraTheme

@Composable
fun ServoraTopBar(
    selectedCity: String,
    selectedLocality: String,
    userRole: UserRole,
    onLocationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    unreadNotificationsCount: Int = 0,
    isImpersonating: Boolean = false,
    onReturnToAdminClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCustomer = userRole == UserRole.CUSTOMER

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isCustomer) ServoraTheme.colors.headerBackgroundStart else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isCustomer) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .stableStatusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (isCustomer) {
                // ==================== GREEN CUSTOMER HEADER ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location: White Pin + City + Locality Dropdown
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLocationClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)), // theme-invariant
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color.White, // theme-invariant
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = selectedCity.ifBlank { "Agra" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 17.sp,
                                    letterSpacing = 0.sp
                                ),
                                color = Color.White // theme-invariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedLocality.ifBlank { "Fatehabad Road" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.9f), // theme-invariant
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Dropdown",
                                    tint = Color.White.copy(alpha = 0.9f), // theme-invariant
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
                                .background(Color.White) // theme-invariant
                                .clickable { onReturnToAdminClick() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("return_admin_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Return to Admin",
                                tint = ServoraTheme.colors.success,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Admin Mode ➔",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = ServoraTheme.colors.success
                            )
                        }
                    } else {
                        // White Notification Bell Icon Button with translucent background
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f)) // theme-invariant
                                .clickable { onNotificationsClick() }
                                .testTag("customer_notif_bell_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White, // theme-invariant
                                modifier = Modifier.size(22.dp)
                            )
                            if (unreadNotificationsCount > 0) {
                                NotificationDot(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp),
                                    ringColor = ServoraTheme.colors.headerBackgroundStart,
                                    contentDescription = "Unread notifications"
                                )
                            }
                        }
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
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.sp,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Location Selector Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onLocationClick() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("location_pill"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$selectedCity • $selectedLocality",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select City",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
                                tint = MaterialTheme.colorScheme.onSurface,
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
                                        UserRole.PROFESSIONAL -> ServoraTheme.colors.successContainer
                                        UserRole.ADMIN -> ServoraTheme.colors.infoContainer
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
                                    UserRole.CUSTOMER -> MaterialTheme.colorScheme.onSurfaceVariant
                                    UserRole.PROFESSIONAL -> ServoraTheme.colors.onSuccessContainer
                                    UserRole.ADMIN -> ServoraTheme.colors.onInfoContainer
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
                                fontWeight = FontWeight.Medium,
                                color = when (userRole) {
                                    UserRole.CUSTOMER -> MaterialTheme.colorScheme.onSurfaceVariant
                                    UserRole.PROFESSIONAL -> ServoraTheme.colors.onSuccessContainer
                                    UserRole.ADMIN -> ServoraTheme.colors.onInfoContainer
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
