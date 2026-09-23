package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.theme.ServoraBorder
import com.example.ui.theme.ServoraCharcoal
import com.example.ui.theme.ServoraCoral
import com.example.ui.theme.ServoraCoralDark
import com.example.ui.theme.ServoraGreen
import com.example.ui.theme.ServoraHoney
import com.example.ui.theme.ServoraPeach
import com.example.ui.theme.ServoraPeachLight
import com.example.ui.theme.ServoraSubtext

@Composable
fun LoginScreen(
    onLoginWithProfile: (UserProfile) -> Unit,
    onLoginWithCredentials: (UserRole, String, String?) -> Unit,
    demoCustomer: UserProfile,
    demoPartner: UserProfile,
    demoAdmin: UserProfile,
    modifier: Modifier = Modifier
) {
    var selectedRole by remember { mutableStateOf(UserRole.CUSTOMER) }
    var inputIdentifier by remember { mutableStateOf("") }
    var inputSecret by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }

    // Colors matching role
    val rolePrimaryColor = when (selectedRole) {
        UserRole.CUSTOMER -> ServoraCoral
        UserRole.PROFESSIONAL -> ServoraGreen
        UserRole.ADMIN -> Color(0xFFD97706)
    }

    val roleBgColor = when (selectedRole) {
        UserRole.CUSTOMER -> ServoraPeachLight
        UserRole.PROFESSIONAL -> Color(0xFFF0FDF4)
        UserRole.ADMIN -> Color(0xFFFFFBEB)
    }

    val roleBorderColor = when (selectedRole) {
        UserRole.CUSTOMER -> ServoraPeach
        UserRole.PROFESSIONAL -> Color(0xFFBBF7D0)
        UserRole.ADMIN -> Color(0xFFFDE68A)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .hideStatusBarOnScroll()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // BRAND HERO HEADER
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ServoraPeach, ServoraCoral.copy(alpha = 0.2f))
                    )
                )
                .border(2.dp, ServoraCoral, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = ServoraCoral,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "SERVICE ASSIST",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = ServoraCharcoal
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = ServoraCoral,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Doorstep Services • Agra, UP",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = ServoraSubtext
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ROLE SELECTION TABS
        Text(
            text = "SELECT YOUR ACCOUNT TYPE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = ServoraSubtext,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, ServoraBorder, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            UserRole.entries.forEach { role ->
                val isSelected = selectedRole == role
                val tabColor = when (role) {
                    UserRole.CUSTOMER -> ServoraCoral
                    UserRole.PROFESSIONAL -> ServoraGreen
                    UserRole.ADMIN -> Color(0xFFD97706)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) tabColor else Color.Transparent)
                        .clickable {
                            selectedRole = role
                            inputIdentifier = ""
                            inputSecret = ""
                            showOtpField = false
                        }
                        .padding(vertical = 12.dp)
                        .testTag("login_role_${role.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (role) {
                                UserRole.CUSTOMER -> Icons.Default.Person
                                UserRole.PROFESSIONAL -> Icons.Default.Handyman
                                UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                            },
                            contentDescription = null,
                            tint = if (isSelected) Color.White else ServoraSubtext,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (role) {
                                UserRole.CUSTOMER -> "Customer"
                                UserRole.PROFESSIONAL -> "Partner Pro"
                                UserRole.ADMIN -> "Admin"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else ServoraCharcoal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ROLE CONTEXT BANNER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = roleBgColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, roleBorderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(rolePrimaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (selectedRole) {
                            UserRole.CUSTOMER -> Icons.Default.CheckCircle
                            UserRole.PROFESSIONAL -> Icons.Default.VerifiedUser
                            UserRole.ADMIN -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = rolePrimaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = when (selectedRole) {
                            UserRole.CUSTOMER -> "Customer Portal"
                            UserRole.PROFESSIONAL -> "Service Partner Portal"
                            UserRole.ADMIN -> "Agra Ops Management Console"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ServoraCharcoal
                        )
                    )
                    Text(
                        text = when (selectedRole) {
                            UserRole.CUSTOMER -> "Book AC repair, deep cleaning & spa at home"
                            UserRole.PROFESSIONAL -> "Manage customer appointments & track daily earnings"
                            UserRole.ADMIN -> "Monitor real-time bookings, technician dispatch & revenue"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = ServoraSubtext
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // MANUAL LOGIN FORM
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ServoraBorder),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = when (selectedRole) {
                        UserRole.CUSTOMER -> "Sign In to Your Account"
                        UserRole.PROFESSIONAL -> "Partner Login"
                        UserRole.ADMIN -> "Admin Authentication"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ServoraCharcoal
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Identifier Field (Phone / Email)
                OutlinedTextField(
                    value = inputIdentifier,
                    onValueChange = { inputIdentifier = it },
                    label = {
                        Text(
                            when (selectedRole) {
                                UserRole.CUSTOMER -> "Mobile Number or Email"
                                UserRole.PROFESSIONAL -> "Partner Mobile / Pro ID"
                                UserRole.ADMIN -> "Admin Email ID"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (selectedRole) {
                                UserRole.CUSTOMER -> Icons.Default.Phone
                                UserRole.PROFESSIONAL -> Icons.Default.Handyman
                                UserRole.ADMIN -> Icons.Default.Person
                            },
                            contentDescription = null,
                            tint = rolePrimaryColor
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (selectedRole == UserRole.ADMIN) KeyboardType.Email else KeyboardType.Phone
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = rolePrimaryColor,
                        focusedLabelColor = rolePrimaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Name Field for new users
                if (selectedRole == UserRole.CUSTOMER && inputIdentifier.isNotBlank() && !showOtpField) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Full Name (Optional for new users)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = rolePrimaryColor,
                            focusedLabelColor = rolePrimaryColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Password / OTP Field
                OutlinedTextField(
                    value = inputSecret,
                    onValueChange = { inputSecret = it },
                    label = {
                        Text(
                            when (selectedRole) {
                                UserRole.CUSTOMER -> if (showOtpField) "Enter 4-Digit OTP" else "Password / OTP (or leave blank)"
                                UserRole.PROFESSIONAL -> "Passcode / OTP (Default: 1234)"
                                UserRole.ADMIN -> "Admin Security PIN / Key"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = rolePrimaryColor
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = rolePrimaryColor,
                        focusedLabelColor = rolePrimaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Login Button
                Button(
                    onClick = {
                        val idVal = inputIdentifier.ifBlank {
                            when (selectedRole) {
                                UserRole.CUSTOMER -> demoCustomer.phone
                                UserRole.PROFESSIONAL -> demoPartner.phone
                                UserRole.ADMIN -> demoAdmin.email
                            }
                        }
                        onLoginWithCredentials(selectedRole, idVal, inputName.ifBlank { null })
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = rolePrimaryColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_submit_login")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when (selectedRole) {
                                UserRole.CUSTOMER -> "Log In as Customer"
                                UserRole.PROFESSIONAL -> "Access Partner Dashboard"
                                UserRole.ADMIN -> "Enter Operations Console"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QUICK DEMO PERSONA LOGIN SHORTCUTS
        Text(
            text = "⚡ QUICK ONE-TAP DEMO LOGINS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = ServoraSubtext,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Demo Customer Card
            DemoLoginChip(
                name = demoCustomer.name,
                roleTitle = "Customer • Taj Nagri Phase 2, Agra",
                icon = Icons.Default.Person,
                accentColor = ServoraCoral,
                bgColor = ServoraPeachLight,
                onClick = { onLoginWithProfile(demoCustomer) },
                testTag = "quick_login_customer"
            )

            // Demo Partner Pro Card
            DemoLoginChip(
                name = demoPartner.name,
                roleTitle = "AC & Appliance Partner Pro • Fatehabad Rd",
                icon = Icons.Default.Handyman,
                accentColor = ServoraGreen,
                bgColor = Color(0xFFF0FDF4),
                onClick = { onLoginWithProfile(demoPartner) },
                testTag = "quick_login_partner"
            )

            // Demo Admin Card
            DemoLoginChip(
                name = demoAdmin.name,
                roleTitle = "Agra City Operations Lead • Sanjay Place",
                icon = Icons.Default.AdminPanelSettings,
                accentColor = Color(0xFFD97706),
                bgColor = Color(0xFFFFFBEB),
                onClick = { onLoginWithProfile(demoAdmin) },
                testTag = "quick_login_admin"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // TRUST BADGES FOOTER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8FAFC))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ServoraGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Verified Pros", style = MaterialTheme.typography.labelSmall, color = ServoraCharcoal)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ServoraCoral,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Transparent Rates", style = MaterialTheme.typography.labelSmall, color = ServoraCharcoal)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("₹10K Insurance", style = MaterialTheme.typography.labelSmall, color = ServoraCharcoal)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DemoLoginChip(
    name: String,
    roleTitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    bgColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ServoraCharcoal
                        )
                    )
                    Text(
                        text = roleTitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = ServoraSubtext
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}