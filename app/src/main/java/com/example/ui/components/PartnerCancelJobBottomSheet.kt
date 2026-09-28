package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartnerCancelReason
import com.example.ui.theme.ServoraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerCancelJobBottomSheet(
    onDismiss: () -> Unit,
    onConfirmCancel: (reasonCode: String, note: String?) -> Unit,
    isCancelling: Boolean = false,
    errorMessage: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedReason by remember { mutableStateOf<PartnerCancelReason?>(null) }
    var reasonNote by remember { mutableStateOf("") }
    val colors = ServoraTheme.colors
    val isDark = colors.isDark

    val isOtherSelected = selectedReason == PartnerCancelReason.OTHER
    val isNoteValid = !isOtherSelected || reasonNote.trim().length >= 10
    val canConfirm = selectedReason != null && isNoteValid && !isCancelling

    ModalBottomSheet(
        onDismissRequest = { if (!isCancelling) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (isDark) colors.danger else Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel Accepted Job",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp
                        ),
                        color = colors.textPrimary
                    )
                }
                IconButton(
                    onClick = { if (!isCancelling) onDismiss() },
                    enabled = !isCancelling
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            Text(
                text = "Please select the reason for cancelling this job. The customer will be notified.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) colors.dangerContainer else Color(0xFFFEE2E2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = if (isDark) colors.danger else Color(0xFFDC2626),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Reason Options List
            PartnerCancelReason.entries.forEach { reason ->
                val isSelected = selectedReason == reason
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        if (isDark) colors.cardBackgroundSubtle else Color(0xFFFEF2F2)
                    } else Color.Transparent,
                    border = if (isSelected) {
                        BorderStroke(1.dp, if (isDark) colors.danger else Color(0xFFFCA5A5))
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable(enabled = !isCancelling) { selectedReason = reason }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { if (!isCancelling) selectedReason = reason },
                            enabled = !isCancelling,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = if (isDark) colors.danger else Color(0xFFDC2626),
                                unselectedColor = colors.textSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                fontSize = 14.sp
                            ),
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )
                    }
                }
            }

            // Note field (Mandatory for OTHER, optional otherwise)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isOtherSelected) "Reason Details (Required, min 10 chars)" else "Additional Note (Optional)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isOtherSelected && reasonNote.trim().length < 10) (if (isDark) colors.danger else Color(0xFFDC2626)) else colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = reasonNote,
                onValueChange = { if (it.length <= 300) reasonNote = it },
                placeholder = {
                    Text(
                        text = if (isOtherSelected) "Please explain the reason in detail..." else "Any note for the dispatch team...",
                        color = colors.textSecondary
                    )
                },
                minLines = 3,
                maxLines = 5,
                enabled = !isCancelling,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) colors.danger else Color(0xFFDC2626),
                    unfocusedBorderColor = colors.cardBorder,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isOtherSelected && reasonNote.trim().length < 10) {
                    Text(
                        text = "At least 10 characters required",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) colors.danger else Color(0xFFDC2626)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Text(
                    text = "${reasonNote.length}/300",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Button(
                onClick = {
                    selectedReason?.let {
                        onConfirmCancel(it.code, reasonNote.trim().ifEmpty { null })
                    }
                },
                enabled = canConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) colors.danger else Color(0xFFDC2626),
                    disabledContainerColor = if (isDark) colors.cardBackgroundSubtle else Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_partner_confirm_cancel")
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White, /* theme-invariant */
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Confirm Cancellation",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = Color.White /* theme-invariant */
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onDismiss,
                enabled = !isCancelling,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Keep Job",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
