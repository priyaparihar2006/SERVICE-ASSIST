package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ServoraTheme

enum class AdminNavTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    OVERVIEW("Ops", Icons.Default.Dashboard, "admin_tab_overview"),
    CUSTOMERS("Customers", Icons.Default.Group, "admin_tab_customers"),
    PARTNERS("Partners", Icons.Default.Engineering, "admin_tab_partners"),
    BOOKINGS("Bookings", Icons.AutoMirrored.Filled.Assignment, "admin_tab_bookings"),
    PROFILE("Admin", Icons.Default.AdminPanelSettings, "admin_tab_profile")
}

@Composable
fun AdminBottomNav(
    currentTab: AdminNavTab,
    onTabSelected: (AdminNavTab) -> Unit,
    pendingBookingsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current

    var isHolding by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    val tabs = AdminNavTab.entries
    val itemCount = tabs.size
    val activeIndex = tabs.indexOf(currentTab).coerceAtLeast(0)
    val displayIndex = if (isHolding && hoveredIndex != null) hoveredIndex!! else activeIndex
    val navColors = ServoraTheme.colors

    val adminActiveColor = if (navColors.isDark) Color(0xFF7AB8FF) else Color(0xFF4F46E5)
    val adminIndicatorColor = if (navColors.isDark) Color(0xFF0E2742) else Color(0xFFEEF2FF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = if (navColors.isDark) 0.dp else 16.dp,
                    shape = RoundedCornerShape(36.dp),
                    spotColor = Color(0x304F46E5), // theme-invariant
                    ambientColor = Color(0x12000000) // theme-invariant
                )
                .onGloballyPositioned { coordinates ->
                    barWidthPx = coordinates.size.width.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(currentTab) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val downTime = System.currentTimeMillis()
                            val downPos = down.position
                            var lastPos = downPos
                            var isSliding = false

                            if (barWidthPx > 0f) {
                                val initialIdx = ((downPos.x / barWidthPx) * itemCount).toInt().coerceIn(0, itemCount - 1)
                                hoveredIndex = initialIdx
                            }

                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                lastPos = pointer.position
                                val distance = (lastPos - downPos).getDistance()
                                val elapsed = System.currentTimeMillis() - downTime

                                if (!pointer.pressed) {
                                    break
                                }

                                if (!isSliding && (elapsed > 60 || distance > viewConfig.touchSlop)) {
                                    isSliding = true
                                    isHolding = true
                                }

                                if (barWidthPx > 0f) {
                                    val rawIndex = ((lastPos.x / barWidthPx) * itemCount).toInt()
                                    val index = rawIndex.coerceIn(0, itemCount - 1)
                                    if (index != hoveredIndex) {
                                        hoveredIndex = index
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }

                            val finalIndex = if (barWidthPx > 0f) {
                                ((lastPos.x / barWidthPx) * itemCount).toInt().coerceIn(0, itemCount - 1)
                            } else {
                                hoveredIndex ?: activeIndex
                            }

                            tabs.getOrNull(finalIndex)?.let { onTabSelected(it) }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            isHolding = false
                            hoveredIndex = null
                        }
                    }
                },
            shape = RoundedCornerShape(36.dp),
            color = navColors.navBarBackground,
            border = BorderStroke(1.2.dp, navColors.cardBorder)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                val totalWidth = maxWidth
                val slotWidth = totalWidth / itemCount
                val indicatorSize = 52.dp

                // Smooth Sliding Indicator
                val targetCenterOffsetX = (slotWidth * displayIndex) + (slotWidth / 2) - (indicatorSize / 2)
                val animatedOffsetX by animateDpAsState(
                    targetValue = targetCenterOffsetX,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 500f
                    ),
                    label = "AdminSlidingNavIndicator"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffsetX)
                        .size(indicatorSize)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(adminIndicatorColor)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = currentTab == tab
                        val isHovered = isHolding && hoveredIndex == index
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        val scale by animateFloatAsState(
                            targetValue = when {
                                isHovered -> 1.25f
                                isPressed -> 0.88f
                                isSelected -> 1.12f
                                else -> 1.0f
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "AdminNavItemScale"
                        )

                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected || isHovered) adminActiveColor else navColors.textSecondary,
                            animationSpec = tween(durationMillis = 200),
                            label = "AdminNavItemIconColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { onTabSelected(tab) }
                                .testTag(tab.testTag),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.scale(scale)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (tab == AdminNavTab.BOOKINGS && pendingBookingsCount > 0) {
                                            NotificationDot(
                                                modifier = Modifier.offset(x = 2.dp, y = (-2).dp),
                                                ringColor = navColors.navBarBackground.copy(alpha = 1f),
                                                contentDescription = "Pending bookings"
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
