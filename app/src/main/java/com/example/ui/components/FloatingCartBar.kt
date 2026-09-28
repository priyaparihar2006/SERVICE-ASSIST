package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.ui.theme.ServoraTheme

@Composable
fun FloatingCartBar(
    itemCount: Int,
    totalPrice: Int,
    totalSavings: Int = 120,
    deliveryText: String = "Free Delivery",
    onViewCartClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ServoraTheme.colors.isDark
    val primaryColor = ServoraTheme.colors.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Floating Card Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 4.dp else 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = if (isDark) Color.Black else Color(0x33000000),
                    ambientColor = Color(0x12000000)
                )
                .clickable { onViewCartClick() }
                .testTag("floating_cart_bar"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, ServoraTheme.colors.cardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: 3D Service Bucket / Basket Illustration Box
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isDark) ServoraTheme.colors.surfaceVariant
                            else ServoraTheme.colors.primaryContainer.copy(alpha = 0.75f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ServiceBasketGraphic(
                        primaryColor = primaryColor,
                        containerColor = ServoraTheme.colors.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Center: Text & Badges
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Line 1: Single non-breaking text line "1 Item • ₹349"
                    val titleAnnotated = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = ServoraTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        ) {
                            append("$itemCount ${if (itemCount == 1) "Item" else "Items"}")
                        }
                        withStyle(
                            SpanStyle(
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        ) {
                            append(" • ")
                        }
                        withStyle(
                            SpanStyle(
                                color = if (isDark) primaryColor else Color(0xFF009051),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            )
                        ) {
                            append("₹$totalPrice")
                        }
                    }

                    Text(
                        text = titleAnnotated,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Line 2: Save ₹120 with Service Assist
                    Text(
                        text = if (totalSavings > 0) "Save ₹$totalSavings with Service Assist" else "Best doorstep price guaranteed",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = ServoraTheme.colors.subtext,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Line 3: Free Delivery Pill Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = if (isDark) primaryColor else ServoraTheme.colors.onPrimaryContainer,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = deliveryText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                color = if (isDark) primaryColor else ServoraTheme.colors.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(ServoraTheme.colors.divider)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Pill Button "View Cart ➔"
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onViewCartClick() }
                        .testTag("view_cart_cta_btn"),
                    shape = RoundedCornerShape(20.dp),
                    color = primaryColor,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "View Cart",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            color = ServoraTheme.colors.onPrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = ServoraTheme.colors.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Small Close / Dismiss Button at Top Right Corner
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 3.dp, y = (-3).dp)
                .size(22.dp)
                .clip(CircleShape)
                .clickable { onDismiss() }
                .testTag("dismiss_cart_btn"),
            shape = CircleShape,
            color = if (isDark) ServoraTheme.colors.surfaceVariant else ServoraTheme.colors.primaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Cart Bar",
                    tint = if (isDark) primaryColor else ServoraTheme.colors.onPrimaryContainer,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * High quality vector illustration of a Service Basket with cleaning spray, paint roller, wrench & brush
 */
@Composable
private fun ServiceBasketGraphic(
    primaryColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Paint Roller (top-right sticking out)
        val rollerPath = Path().apply {
            moveTo(w * 0.62f, h * 0.38f)
            lineTo(w * 0.76f, h * 0.18f)
            lineTo(w * 0.88f, h * 0.22f)
        }
        drawPath(
            path = rollerPath,
            color = Color(0xFF64748B),
            style = Stroke(width = w * 0.05f, cap = StrokeCap.Round)
        )
        // Roller cylinder
        drawRoundRect(
            color = Color(0xFFFFFFFF),
            topLeft = Offset(w * 0.72f, h * 0.14f),
            size = Size(w * 0.24f, h * 0.12f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )
        drawRoundRect(
            color = primaryColor.copy(alpha = 0.75f),
            topLeft = Offset(w * 0.72f, h * 0.14f),
            size = Size(w * 0.08f, h * 0.12f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        // 2. Screwdriver / Tool handle (middle sticking out)
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(w * 0.44f, h * 0.22f),
            size = Size(w * 0.12f, h * 0.26f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(w * 0.50f, h * 0.22f),
            end = Offset(w * 0.50f, h * 0.10f),
            strokeWidth = w * 0.04f,
            cap = StrokeCap.Round
        )

        // 3. Spray Bottle (left sticking out)
        drawRoundRect(
            color = primaryColor.copy(alpha = 0.9f),
            topLeft = Offset(w * 0.22f, h * 0.20f),
            size = Size(w * 0.20f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        // Trigger / cap
        val sprayCap = Path().apply {
            moveTo(w * 0.14f, h * 0.18f)
            lineTo(w * 0.32f, h * 0.18f)
            lineTo(w * 0.28f, h * 0.12f)
            lineTo(w * 0.18f, h * 0.12f)
            close()
        }
        drawPath(sprayCap, color = Color(0xFFE2E8F0))

        // 4. Main Service Basket Body
        val basketPath = Path().apply {
            moveTo(w * 0.10f, h * 0.48f)
            lineTo(w * 0.90f, h * 0.48f)
            lineTo(w * 0.82f, h * 0.88f)
            lineTo(w * 0.18f, h * 0.88f)
            close()
        }
        drawPath(
            path = basketPath,
            color = primaryColor
        )

        // Basket Top Rim
        drawRoundRect(
            color = primaryColor.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.06f, h * 0.44f),
            size = Size(w * 0.88f, h * 0.10f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )

        // Basket Slits / Texture
        val slitColor = Color.White.copy(alpha = 0.4f)
        drawLine(slitColor, Offset(w * 0.32f, h * 0.58f), Offset(w * 0.30f, h * 0.80f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
        drawLine(slitColor, Offset(w * 0.50f, h * 0.58f), Offset(w * 0.50f, h * 0.80f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
        drawLine(slitColor, Offset(w * 0.68f, h * 0.58f), Offset(w * 0.70f, h * 0.80f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)

        // Basket Center Leaf Badge
        drawCircle(
            color = Color.White,
            radius = w * 0.10f,
            center = Offset(w * 0.50f, h * 0.70f)
        )
        drawCircle(
            color = primaryColor,
            radius = w * 0.06f,
            center = Offset(w * 0.50f, h * 0.70f)
        )
    }
}
