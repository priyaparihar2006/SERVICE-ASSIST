package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.theme.ServiceAssistGreen
import com.example.ui.theme.ServoraBorder
import com.example.ui.theme.ServoraCharcoal
import com.example.ui.theme.ServoraGreen
import com.example.ui.theme.ServoraSubtext

@Composable
fun PartnerToolkitScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val checkedItems = remember {
        mutableStateMapOf(
            "ac_pump" to true,
            "ac_jacket" to true,
            "ac_gauge" to true,
            "ac_foam" to true,
            "clean_scrubber" to true,
            "clean_vacuum" to true,
            "shoe_covers" to true,
            "clean_microfiber" to true,
            "plumb_wrench" to true,
            "plumb_voltage" to true,
            "plumb_pressure" to false
        )
    }

    val totalItems = checkedItems.size
    val completedCount = checkedItems.values.count { it }
    val progressFraction = if (totalItems > 0) completedCount.toFloat() / totalItems.toFloat() else 1f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .hideStatusBarOnScroll()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 24.dp)
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, ServoraBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ServoraCharcoal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Equipment & Pro Toolkit",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = ServoraCharcoal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pre-visit equipment readiness & doorstep safety guidelines",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = ServoraSubtext
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // READINESS PROGRESS CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = ServoraGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Toolkit Readiness Score",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = ServoraCharcoal
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (progressFraction >= 1f) Color(0xFFDCFCE7) else Color(0xFFE6F5EE)
                    ) {
                        Text(
                            text = "$completedCount / $totalItems Ready",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ServoraGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ServoraGreen,
                    trackColor = Color(0xFFE2E8F0),
                    strokeCap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 24x7 AGRA PARTNER SOS & DISPATCH HELPLINE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ServoraGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Agra Partner Dispatch SOS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = ServoraCharcoal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Instant assistance for address issues, client delays, or extra spares approval.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        ),
                        color = Color(0xFF4B5563)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:18007378672"))
                            context.startActivity(dialIntent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ServoraGreen),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Call Dispatch Desk",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1: AC & APPLIANCE REPAIR KIT
        ToolkitSectionHeader(
            icon = Icons.Default.AcUnit,
            title = "AC & APPLIANCE REPAIR KIT"
        )
        Spacer(modifier = Modifier.height(8.dp))

        ToolkitCardWithImage(
            imageRes = R.drawable.img_ac_repair,
            imageContentDescription = "AC Repair Toolkit"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolkitCheckRow(
                    label = "High-Pressure Jet Wash Pump",
                    isChecked = checkedItems["ac_pump"] ?: false,
                    onToggle = { checkedItems["ac_pump"] = !(checkedItems["ac_pump"] ?: false) }
                )
                ToolkitCheckRow(
                    label = "Spill-Proof Wall Jacket",
                    isChecked = checkedItems["ac_jacket"] ?: false,
                    onToggle = { checkedItems["ac_jacket"] = !(checkedItems["ac_jacket"] ?: false) }
                )
                ToolkitCheckRow(
                    label = "Digital Multimeter & Manifold",
                    isChecked = checkedItems["ac_gauge"] ?: false,
                    onToggle = { checkedItems["ac_gauge"] = !(checkedItems["ac_gauge"] ?: false) }
                )
                ToolkitCheckRow(
                    label = "Eco-Foam Jet Coil Cleaner",
                    isChecked = checkedItems["ac_foam"] ?: false,
                    onToggle = { checkedItems["ac_foam"] = !(checkedItems["ac_foam"] ?: false) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 2: HOME DEEP CLEANING & HYGIENE KIT
        ToolkitSectionHeader(
            icon = Icons.Default.CleaningServices,
            title = "HOME DEEP CLEANING & HYGIENE KIT"
        )
        Spacer(modifier = Modifier.height(8.dp))

        ToolkitCardWithImage(
            imageRes = R.drawable.img_cleaning_pro,
            imageContentDescription = "Deep Cleaning Toolkit"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolkitCheckRow(
                    label = "Single-Disc Floor Rotary Scrubber",
                    isChecked = checkedItems["clean_scrubber"] ?: false,
                    onToggle = { checkedItems["clean_scrubber"] = !(checkedItems["clean_scrubber"] ?: false) }
                )
                ToolkitCheckRow(
                    label = "Wet & Dry Extraction Vacuum",
                    isChecked = checkedItems["clean_vacuum"] ?: false,
                    onToggle = { checkedItems["clean_vacuum"] = !(checkedItems["clean_vacuum"] ?: false) }
                )
                ToolkitCheckRow(
                    label = "Disposable Shoe Covers & Sanitizer",
                    isChecked = checkedItems["shoe_covers"] ?: false,
                    onToggle = { checkedItems["shoe_covers"] = !(checkedItems["shoe_covers"] ?: false) }
                )
                ToolkitCheckRow(
                    label = "Microfiber Multi-Surface Cloths",
                    isChecked = checkedItems["clean_microfiber"] ?: false,
                    onToggle = { checkedItems["clean_microfiber"] = !(checkedItems["clean_microfiber"] ?: false) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 3: PLUMBING & ELECTRICAL ESSENTIALS
        ToolkitSectionHeader(
            icon = Icons.Default.Handyman,
            title = "PLUMBING & ELECTRICAL ESSENTIALS"
        )
        Spacer(modifier = Modifier.height(8.dp))

        ToolkitCardWithImage(
            imageRes = R.drawable.img_electrician_work,
            imageContentDescription = "Plumbing and Electrical Kit"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolkitCheckRow(
                    label = "Pipe Wrench & Sealant Tape Set",
                    isChecked = checkedItems["plumb_wrench"] ?: false,
                    onToggle = { checkedItems["plumb_wrench"] = !(checkedItems["plumb_wrench"] ?: false) }
                )
                ToolkitCheckRow(
                    label = "Voltage Detector & Insulated Pliers",
                    isChecked = checkedItems["plumb_voltage"] ?: false,
                    onToggle = { checkedItems["plumb_voltage"] = !(checkedItems["plumb_voltage"] ?: false) }
                )
                ToolkitCheckRow(
                    label = "Pipe Pressure Gauge & Cutter",
                    isChecked = checkedItems["plumb_pressure"] ?: false,
                    onToggle = { checkedItems["plumb_pressure"] = !(checkedItems["plumb_pressure"] ?: false) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // DOORSTEP SOP RULES CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE6F5EE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ServoraGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Doorstep Quality Standards",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = ServoraCharcoal
                        )
                        Text(
                            text = "Mandatory Service Assist partner guidelines",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = ServoraSubtext
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SopStepRow(number = 1, text = "Always wear Servora ID badge & shoe covers before entering client premises.")
                Spacer(modifier = Modifier.height(8.dp))
                SopStepRow(number = 2, text = "Collect the 4-digit start OTP from the client before starting any repair work.")
                Spacer(modifier = Modifier.height(8.dp))
                SopStepRow(number = 3, text = "Lay down the waterproof floor sheet under work area to prevent dust or spill.")
                Spacer(modifier = Modifier.height(8.dp))
                SopStepRow(number = 4, text = "Hand over transparent digital invoice and ensure work space is left spotless.")
            }
        }
    }
}

@Composable
private fun ToolkitSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ServoraGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
            ),
            color = Color(0xFF4B5563)
        )
    }
}

@Composable
private fun ToolkitCardWithImage(
    imageRes: Int,
    imageContentDescription: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checklist Items Column
            Box(modifier = Modifier.weight(1f)) {
                content()
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Visual Container
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 118.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF3FAF6))
                    .border(1.dp, Color(0xFFD1FAE5), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = imageContentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun ToolkitCheckRow(
    label: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom Circular Checkbox
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isChecked) ServoraGreen else Color.Transparent)
                .border(
                    width = if (isChecked) 0.dp else 1.5.dp,
                    color = if (isChecked) Color.Transparent else Color(0xFFD1D5DB),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 16.sp
            ),
            color = if (isChecked) ServoraCharcoal else Color(0xFF6B7280)
        )
    }
}

@Composable
private fun SopStepRow(
    number: Int,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFFE6F5EE)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = ServoraGreen
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 17.sp
            ),
            color = Color(0xFF374151)
        )
    }
}

