package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ServoraTheme

/**
 * Small red "something new" dot. No number by design.
 * ringColor = the background the icon sits on, so the dot separates cleanly from the icon.
 */
@Composable
fun NotificationDot(
    modifier: Modifier = Modifier,
    size: Dp = 9.dp,
    ringColor: Color? = null,
    contentDescription: String = "New updates"
) {
    val base = modifier
        .size(size)
        .clip(CircleShape)
        .semantics { this.contentDescription = contentDescription }
    Box(
        modifier = (if (ringColor != null) base.border(1.5.dp, ringColor, CircleShape) else base)
            .background(ServoraTheme.colors.badgeRed)
    )
}
