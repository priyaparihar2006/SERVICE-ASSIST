package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

private val AdminIndigo = Color(0xFF4F46E5)
private val AdminIndigoLight = Color(0xFFEEF2FF)
private val TextSecondary = Color(0xFF68736B)
private val TextPrimary = Color(0xFF172019)
private val BorderColor = Color(0xFFE2EBE6)

@Composable
fun AdminBottomNav(
    currentTab: AdminNavTab,
    onTabSelected: (AdminNavTab) -> Unit,
    pendingBookingsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current
    val density = LocalDensity.current

    var isHolding by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    val tabs = AdminNavTab.entries
    val itemCount = tabs.size
    val swipeThreshold = with(density) { 20.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color(0x244F46E5),
                    ambientColor = Color(0x0F000000)
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
                            var isGestureActive = false

                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                lastPos = pointer.position
                                val distance = (lastPos - downPos).getDistance()
                                val elapsed = System.currentTimeMillis() - downTime

                                if (!pointer.pressed) {
                                    break
                                }

                                if (!isGestureActive && (elapsed > 100 || distance > viewConfig.touchSlop)) {
                                    isGestureActive = true
                                    isHolding = true
                                }

                                if (isGestureActive && barWidthPx > 0f) {
                                    val rawIndex = ((lastPos.x / barWidthPx) * itemCount).toInt()
                                    val index = rawIndex.coerceIn(0, itemCount - 1)
                                    if (index != hoveredIndex) {
                                        hoveredIndex = index
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }

                            val totalDeltaX = lastPos.x - downPos.x
                            val totalDeltaY = lastPos.y - downPos.y
                            val absX = kotlin.math.abs(totalDeltaX)
                            val absY = kotlin.math.abs(totalDeltaY)

                            if (absX > swipeThreshold && absX > absY * 0.9f) {
                                val currentIndex = tabs.indexOf(currentTab)
                                if (totalDeltaX < 0) {
                                    // Swipe Left -> Next tab
                                    if (currentIndex in 0 until tabs.size - 1) {
                                        onTabSelected(tabs[currentIndex + 1])
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                } else {
                                    // Swipe Right -> Previous tab
                                    if (currentIndex > 0) {
                                        onTabSelected(tabs[currentIndex - 1])
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            } else if (isGestureActive && hoveredIndex != null) {
                                val targetTab = tabs.getOrNull(hoveredIndex!!)
                                targetTab?.let { onTabSelected(it) }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            isHolding = false
                            hoveredIndex = null
                        }
                    }
                },
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            border = BorderStroke(1.2.dp, BorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
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
                            isHovered -> 1.24f
                            isPressed -> 0.88f
                            isSelected -> 1.10f
                            else -> 1.0f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "AdminNavItemScale"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected || isHovered) AdminIndigo else TextSecondary,
                        animationSpec = tween(durationMillis = 200),
                        label = "AdminNavItemIconColor"
                    )

                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected || isHovered) AdminIndigoLight else Color.Transparent,
                        animationSpec = tween(durationMillis = 220),
                        label = "AdminNavItemBgColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(bgColor)
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
                                        Badge(
                                            containerColor = Color(0xFFF59E0B),
                                            contentColor = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Text(
                                                text = if (pendingBookingsCount > 9) "9+" else "$pendingBookingsCount",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = iconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
