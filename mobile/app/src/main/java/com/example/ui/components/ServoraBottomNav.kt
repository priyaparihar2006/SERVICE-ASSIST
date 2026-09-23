package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

// ============================================================================
// EXACT REFERENCE COLOR SYSTEM (#009051 Brand)
// ============================================================================
private val BrandGreen = Color(0xFF009051)
private val BrandDarkGreen = Color(0xFF007A45)
private val BrandLightGreen = Color(0xFFE6F5EF)
private val BrandVeryLightGreen = Color(0xFFF2FAF6)
private val BrandWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF172019)
private val TextSecondary = Color(0xFF68736B)
private val BorderColor = Color(0xFFE2EBE6)

enum class ServoraNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    SERVICES("Services", Icons.Filled.GridView, Icons.Outlined.GridView, "nav_services"),
    CHATS("Chats", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_chats"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun ServoraBottomNav(
    currentTab: ServoraNavTab,
    onTabSelected: (ServoraNavTab) -> Unit,
    onCenterActionClick: () -> Unit = { onTabSelected(ServoraNavTab.SERVICES) },
    activeBookingsCount: Int = 0,
    unreadChatsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current
    val density = LocalDensity.current

    var isHolding by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    // 5 slots: 0=Home, 1=Services, 2=CenterPlus, 3=Chats, 4=Profile
    val itemCount = 5
    val navTabs = remember { listOf(ServoraNavTab.HOME, ServoraNavTab.SERVICES, ServoraNavTab.CHATS, ServoraNavTab.PROFILE) }
    val swipeThresholdPx = with(density) { 36.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Pill Capsule Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color(0x24009051),
                    ambientColor = Color(0x0F000000)
                )
                .onGloballyPositioned { coordinates ->
                    barWidthPx = coordinates.size.width.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(currentTab) {
                    val swipeThreshold = 20.dp.toPx()
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
                                val currentIndex = navTabs.indexOf(currentTab)
                                if (totalDeltaX < 0) {
                                    // Swipe Left -> Next Section
                                    if (currentIndex in 0 until navTabs.size - 1) {
                                        onTabSelected(navTabs[currentIndex + 1])
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                } else {
                                    // Swipe Right -> Previous Section
                                    if (currentIndex > 0) {
                                        onTabSelected(navTabs[currentIndex - 1])
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            } else if (isGestureActive && hoveredIndex != null) {
                                when (hoveredIndex) {
                                    0 -> onTabSelected(ServoraNavTab.HOME)
                                    1 -> onTabSelected(ServoraNavTab.SERVICES)
                                    2 -> onCenterActionClick()
                                    3 -> onTabSelected(ServoraNavTab.CHATS)
                                    4 -> onTabSelected(ServoraNavTab.PROFILE)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            isHolding = false
                            hoveredIndex = null
                        }
                    }
                },
            shape = RoundedCornerShape(32.dp),
            color = BrandWhite,
            border = BorderStroke(1.2.dp, BorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. HOME TAB
                FloatingNavItem(
                    tab = ServoraNavTab.HOME,
                    isSelected = currentTab == ServoraNavTab.HOME,
                    isHovered = isHolding && hoveredIndex == 0,
                    badgeCount = 0,
                    onClick = { onTabSelected(ServoraNavTab.HOME) },
                    modifier = Modifier.weight(1f)
                )

                // 2. SERVICES TAB
                FloatingNavItem(
                    tab = ServoraNavTab.SERVICES,
                    isSelected = currentTab == ServoraNavTab.SERVICES,
                    isHovered = isHolding && hoveredIndex == 1,
                    badgeCount = 0,
                    onClick = { onTabSelected(ServoraNavTab.SERVICES) },
                    modifier = Modifier.weight(1f)
                )

                // 3. CENTER FLOATING ACTION BUTTON (+)
                CenterFloatingPlusButton(
                    onClick = onCenterActionClick,
                    isHovered = isHolding && hoveredIndex == 2,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // 4. CHATS TAB (with badge)
                FloatingNavItem(
                    tab = ServoraNavTab.CHATS,
                    isSelected = currentTab == ServoraNavTab.CHATS,
                    isHovered = isHolding && hoveredIndex == 3,
                    badgeCount = unreadChatsCount,
                    onClick = { onTabSelected(ServoraNavTab.CHATS) },
                    modifier = Modifier.weight(1f)
                )

                // 5. PROFILE TAB
                FloatingNavItem(
                    tab = ServoraNavTab.PROFILE,
                    isSelected = currentTab == ServoraNavTab.PROFILE,
                    isHovered = isHolding && hoveredIndex == 4,
                    badgeCount = 0,
                    onClick = { onTabSelected(ServoraNavTab.PROFILE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================================================
// FLOATING NAV ITEM WITH SMOOTH HOVER / SELECTION ANIMATION
// ============================================================================
@Composable
private fun FloatingNavItem(
    tab: ServoraNavTab,
    isSelected: Boolean,
    isHovered: Boolean = false,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth Spring Scale & Color Animation
    val scale by animateFloatAsState(
        targetValue = when {
            isHovered -> 1.24f
            isPressed -> 0.88f
            isSelected -> 1.12f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "NavItemScale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected || isHovered) BrandGreen else TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "NavItemIconColor"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected || isHovered) BrandLightGreen else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "NavItemBgColor"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag(tab.tag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = Color(0xFFEF4444),
                            contentColor = BrandWhite,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Text(
                                text = if (badgeCount > 9) "9+" else "$badgeCount",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSelected || isHovered) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ============================================================================
// CENTER HIGHLIGHTED FLOATING ACTION BUTTON (+)
// ============================================================================
@Composable
private fun CenterFloatingPlusButton(
    onClick: () -> Unit,
    isHovered: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isHovered -> 1.24f
            isPressed -> 0.88f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CenterButtonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(46.dp)
            .shadow(
                elevation = if (isHovered) 12.dp else 6.dp,
                shape = CircleShape,
                spotColor = Color(0x40009051)
            )
            .clip(CircleShape)
            .background(if (isPressed || isHovered) BrandDarkGreen else BrandGreen)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag("center_plus_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Quick Action",
            tint = BrandWhite,
            modifier = Modifier.size(26.dp)
        )
    }
}
