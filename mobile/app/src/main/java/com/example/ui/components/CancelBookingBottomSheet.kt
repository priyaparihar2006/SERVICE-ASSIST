package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandRed = Color(0xFFDC2626)
private val BrandRedSoft = Color(0xFFFEF2F2)
private val BrandBorder = Color(0xFFE5E7EB)
private val TextMain = Color(0xFF1E2022)
private val TextSub = Color(0xFF6B7280)

val CANCELLATION_REASONS = listOf(
    "Change of plans / No longer needed",
    "Booked for the wrong date or time",
    "Found an alternative / cheaper option",
    "Service professional delayed or unresponsive",
    "Selected the wrong service by mistake",
    "Price or payment concerns",
    "Other reason"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelBookingBottomSheet(
    onDismiss: () -> Unit,
    onConfirmCancel: (reason: String, feedback: String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedReason by remember { mutableStateOf("") }
    var feedbackText by remember { mutableStateOf("") }
    val isOtherSelected = selectedReason == "Other reason"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel Booking",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextMain
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSub
                    )
                }
            }

            Text(
                text = "Please tell us why you want to cancel. Your feedback helps us improve our service.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = TextSub,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Reason Options
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CANCELLATION_REASONS.forEach { reason ->
                    val isSelected = selectedReason == reason
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) BrandRedSoft else Color(0xFFF9FAFB),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) BrandRed else BrandBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BrandRed,
                                    unselectedColor = Color(0xFF9CA3AF)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp
                                ),
                                color = if (isSelected) BrandRed else TextMain
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feedback Text Field
            Text(
                text = if (isOtherSelected) "Tell us more *" else "Additional Feedback (Optional)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = TextMain
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = feedbackText,
                onValueChange = { if (it.length <= 300) feedbackText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp),
                placeholder = {
                    Text(
                        "Share your experience or reasons to help us serve you better...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandRed,
                    unfocusedBorderColor = BrandBorder,
                    focusedContainerColor = Color(0xFFFAFAFA),
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                ),
                supportingText = {
                    Text(
                        text = "${feedbackText.length}/300",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Policy Notice Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Free cancellation before the professional arrives at your location.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFF15803D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Cancel Button (Red)
            val canSubmit = selectedReason.isNotBlank() && (!isOtherSelected || feedbackText.isNotBlank())
            Button(
                onClick = { onConfirmCancel(selectedReason, feedbackText.trim()) },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandRed,
                    disabledContainerColor = Color(0xFFFCA5A5)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Confirm Cancellation",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Keep Booking Button
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Keep My Booking",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextMain
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
