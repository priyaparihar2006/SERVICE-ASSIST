package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ServoraTheme

enum class PartnerNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DUTY_JOBS("Duty & Jobs", Icons.Filled.Build, Icons.Outlined.Build, "partner_nav_jobs"),
    BOOKINGS("Bookings", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "partner_nav_bookings"),
    EARNINGS("Earnings", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "partner_nav_earnings"),
    TOOLKIT("Toolkit", Icons.Filled.Checklist, Icons.Outlined.Checklist, "partner_nav_toolkit"),
    PROFILE("Pro Profile", Icons.Filled.Person, Icons.Outlined.Person, "partner_nav_profile")
}

@Composable
fun PartnerBottomNav(
    currentTab: PartnerNavTab,
    onTabSelected: (PartnerNavTab) -> Unit,
    activeJobsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current

    var isHolding by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    val tabs = PartnerNavTab.entries
    val itemCount = tabs.size
    val activeIndex = tabs.indexOf(currentTab).coerceAtLeast(0)
    val displayIndex = if (isHolding && hoveredIndex != null) hoveredIndex!! else activeIndex
    val navColors = ServoraTheme.colors

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
                .height(64.dp)
                .shadow(
                    elevation = if (navColors.isDark) 0.dp else 6.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color(0x18009051), // theme-invariant
                    ambientColor = Color(0x0A000000) // theme-invariant
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

                            onTabSelected(tabs[finalIndex])
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            isHolding = false
                            hoveredIndex = null
                        }
                    }
                },
            shape = RoundedCornerShape(32.dp),
            color = navColors.navBarBackground,
            border = BorderStroke(1.dp, navColors.cardBorder)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                val totalWidth = maxWidth
                val slotWidth = totalWidth / itemCount
                val indicatorSize = 46.dp

                val targetCenterOffsetX = (slotWidth * displayIndex) + (slotWidth / 2) - (indicatorSize / 2)
                val animatedOffsetX by animateDpAsState(
                    targetValue = targetCenterOffsetX,
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = 550f
                    ),
                    label = "PartnerSlidingNavIndicator"
                )

                val indicatorAlpha by animateFloatAsState(
                    targetValue = 1.0f,
                    animationSpec = tween(150),
                    label = "PartnerIndicatorAlpha"
                )

                if (indicatorAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .offset(x = animatedOffsetX)
                            .size(indicatorSize)
                            .align(Alignment.CenterStart)
                            .clip(CircleShape)
                            .background(navColors.navBarSelectedIndicator.copy(alpha = indicatorAlpha))
                            .border(1.dp, navColors.cardBorder.copy(alpha = indicatorAlpha * 0.75f), CircleShape)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slot 0: Duty & Jobs
                    PartnerNavSlot(
                        isSelected = currentTab == PartnerNavTab.DUTY_JOBS,
                        isHovered = isHolding && hoveredIndex == 0,
                        onClick = { onTabSelected(PartnerNavTab.DUTY_JOBS) },
                        testTag = PartnerNavTab.DUTY_JOBS.tag,
                        modifier = Modifier.weight(1f)
                    ) { isSelected, isHovered ->
                        PartnerJobsOutlineIcon(
                            isSelected = isSelected || isHovered,
                            activeCount = activeJobsCount
                        )
                    }

                    NavVerticalDivider()

                    // Slot 1: Bookings (My Bookings)
                    PartnerNavSlot(
                        isSelected = currentTab == PartnerNavTab.BOOKINGS,
                        isHovered = isHolding && hoveredIndex == 1,
                        onClick = { onTabSelected(PartnerNavTab.BOOKINGS) },
                        testTag = PartnerNavTab.BOOKINGS.tag,
                        modifier = Modifier.weight(1f)
                    ) { isSelected, isHovered ->
                        PartnerBookingsOutlineIcon(
                            isSelected = isSelected || isHovered
                        )
                    }

                    NavVerticalDivider()

                    // Slot 2: Earnings (Center Action)
                    PartnerCenterEarningsButton(
                        isSelected = currentTab == PartnerNavTab.EARNINGS,
                        isHovered = isHolding && hoveredIndex == 2,
                        onClick = { onTabSelected(PartnerNavTab.EARNINGS) },
                        modifier = Modifier.weight(1f)
                    )

                    NavVerticalDivider()

                    // Slot 3: Toolkit
                    PartnerNavSlot(
                        isSelected = currentTab == PartnerNavTab.TOOLKIT,
                        isHovered = isHolding && hoveredIndex == 3,
                        onClick = { onTabSelected(PartnerNavTab.TOOLKIT) },
                        testTag = PartnerNavTab.TOOLKIT.tag,
                        modifier = Modifier.weight(1f)
                    ) { isSelected, isHovered ->
                        PartnerToolkitOutlineIcon(
                            isSelected = isSelected || isHovered
                        )
                    }

                    NavVerticalDivider()

                    // Slot 4: Pro Profile
                    PartnerNavSlot(
                        isSelected = currentTab == PartnerNavTab.PROFILE,
                        isHovered = isHolding && hoveredIndex == 4,
                        onClick = { onTabSelected(PartnerNavTab.PROFILE) },
                        testTag = PartnerNavTab.PROFILE.tag,
                        modifier = Modifier.weight(1f)
                    ) { isSelected, isHovered ->
                        PartnerProfileOutlineIcon(
                            isSelected = isSelected || isHovered
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerNavSlot(
    isSelected: Boolean,
    isHovered: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable (isSelected: Boolean, isHovered: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isHovered -> 1.18f
            isPressed -> 0.88f
            isSelected -> 1.06f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "PartnerNavSlotScale"
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.scale(scale),
            contentAlignment = Alignment.Center
        ) {
            content(isSelected, isHovered)
        }
    }
}

// ============================================================================
// PARTNER OUTLINE ICONS
// ============================================================================
@Composable
private fun PartnerJobsOutlineIcon(
    isSelected: Boolean,
    activeCount: Int,
    modifier: Modifier = Modifier
) {
    val isBadgeVisible = activeCount > 0
    val iconColor = if (isSelected) ServoraTheme.colors.navBarSelected else ServoraTheme.colors.navBarUnselected
    BadgedBox(
        modifier = modifier,
        badge = {
            if (isBadgeVisible) NotificationDot(
                modifier = Modifier.offset(x = 2.dp, y = (-2).dp),
                ringColor = ServoraTheme.colors.navBarBackground.copy(alpha = 1f),
                contentDescription = "Jobs has updates"
            )
        }
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.Build else Icons.Outlined.Build,
            contentDescription = "Jobs",
            tint = iconColor,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
private fun PartnerBookingsOutlineIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isSelected) ServoraTheme.colors.navBarSelected else ServoraTheme.colors.navBarUnselected
    Icon(
        imageVector = if (isSelected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
        contentDescription = "Bookings",
        tint = iconColor,
        modifier = modifier.size(25.dp)
    )
}

@Composable
private fun PartnerCenterEarningsButton(
    isSelected: Boolean,
    isHovered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isHovered -> 1.18f
            isPressed -> 0.88f
            isSelected -> 1.08f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PartnerCenterScale"
    )

    val navColors = ServoraTheme.colors

    Box(
        modifier = modifier
            .height(52.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag("partner_center_earnings"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(44.dp)
                .shadow(
                    elevation = if (navColors.isDark) 0.dp else if (isSelected || isHovered) 8.dp else 3.dp,
                    shape = CircleShape,
                    spotColor = Color(0x40009051) // theme-invariant
                )
                .clip(CircleShape)
                .background(if (isSelected || isPressed || isHovered) navColors.navBarSelected else MaterialTheme.colorScheme.primary)
                .border(
                    width = if (isSelected) 1.5.dp else 0.dp,
                    color = if (isSelected) navColors.cardBorder else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Earnings",
                tint = Color.White, // theme-invariant
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PartnerToolkitOutlineIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isSelected) ServoraTheme.colors.navBarSelected else ServoraTheme.colors.navBarUnselected
    Icon(
        imageVector = if (isSelected) Icons.Filled.Checklist else Icons.Outlined.Checklist,
        contentDescription = "Toolkit",
        tint = iconColor,
        modifier = modifier.size(25.dp)
    )
}

@Composable
private fun PartnerProfileOutlineIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isSelected) ServoraTheme.colors.navBarSelected else ServoraTheme.colors.navBarUnselected
    Canvas(modifier = modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = (if (isSelected) 2.6.dp else 2.0.dp).toPx()

        val stroke = Stroke(
            width = strokeWidth,
            join = StrokeJoin.Round,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = iconColor,
            radius = w * 0.20f - strokeWidth / 2,
            center = Offset(w * 0.50f, h * 0.26f),
            style = stroke
        )

        val bodyPath = Path().apply {
            moveTo(w * 0.12f, h * 0.88f)
            cubicTo(
                w * 0.14f, h * 0.58f,
                w * 0.28f, h * 0.50f,
                w * 0.50f, h * 0.50f
            )
            cubicTo(
                w * 0.72f, h * 0.50f,
                w * 0.86f, h * 0.58f,
                w * 0.88f, h * 0.88f
            )
        }

        drawPath(
            path = bodyPath,
            color = iconColor,
            style = stroke
        )
    }
}

// ============================================================================
// VERTICAL DIVIDER BETWEEN BOTTOM NAV SLOTS
// ============================================================================
@Composable
private fun NavVerticalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 1.dp, height = 24.dp)
            .background(ServoraTheme.colors.divider, shape = RoundedCornerShape(0.5.dp))
    )
}
