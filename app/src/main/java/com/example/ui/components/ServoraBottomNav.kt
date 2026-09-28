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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
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

enum class ServoraNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    SERVICES("Services", Icons.Filled.GridView, Icons.Outlined.GridView, "nav_services"),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    BOOKINGS("Bookings", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_bookings"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun ServoraBottomNav(
    currentTab: ServoraNavTab,
    onTabSelected: (ServoraNavTab) -> Unit,
    onCenterActionClick: () -> Unit = { onTabSelected(ServoraNavTab.SEARCH) },
    activeBookingsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current

    var isHolding by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    // 5 slots: 0=Home, 1=Services, 2=CenterSearch, 3=Bookings, 4=Profile
    val itemCount = 5

    // Determine currently active slot index (0..4)
    val activeIndex = when (currentTab) {
        ServoraNavTab.HOME -> 0
        ServoraNavTab.SERVICES -> 1
        ServoraNavTab.SEARCH -> 2
        ServoraNavTab.BOOKINGS -> 3
        ServoraNavTab.PROFILE -> 4
    }

    val displayIndex = if (isHolding && hoveredIndex != null) hoveredIndex!! else activeIndex

    fun selectSlotByIndex(index: Int) {
        when (index) {
            0 -> onTabSelected(ServoraNavTab.HOME)
            1 -> onTabSelected(ServoraNavTab.SERVICES)
            2 -> {
                onTabSelected(ServoraNavTab.SEARCH)
                onCenterActionClick()
            }
            3 -> onTabSelected(ServoraNavTab.BOOKINGS)
            4 -> onTabSelected(ServoraNavTab.PROFILE)
        }
    }

    val navColors = ServoraTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Pill Capsule Container - Translucent Glassmorphism for Scroll-Behind Effect
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (navColors.isDark) 0.dp else 8.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color(0x22009051), // theme-invariant
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

                            // Commit on touch release
                            val finalIndex = if (barWidthPx > 0f) {
                                ((lastPos.x / barWidthPx) * itemCount).toInt().coerceIn(0, itemCount - 1)
                            } else {
                                hoveredIndex ?: activeIndex
                            }

                            selectSlotByIndex(finalIndex)
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
                    .padding(horizontal = 6.dp)
            ) {
                val totalWidth = maxWidth
                val slotWidth = totalWidth / itemCount
                val indicatorSize = 44.dp

                // Sliding Indicator Background Pill with fluid spring physics
                val targetCenterOffsetX = (slotWidth * displayIndex) + (slotWidth / 2) - (indicatorSize / 2)
                val animatedOffsetX by animateDpAsState(
                    targetValue = targetCenterOffsetX,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 520f
                    ),
                    label = "SlidingNavIndicator"
                )

                val indicatorAlpha by animateFloatAsState(
                    targetValue = if (displayIndex == 2) 0f else 1.0f,
                    animationSpec = tween(150),
                    label = "IndicatorAlpha"
                )

                if (indicatorAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .offset(x = animatedOffsetX)
                            .size(indicatorSize)
                            .align(Alignment.CenterStart)
                            .clip(CircleShape)
                            .background(navColors.navBarSelectedIndicator.copy(alpha = indicatorAlpha))
                            .border(1.dp, navColors.cardBorder.copy(alpha = indicatorAlpha * 0.70f), CircleShape)
                    )
                }

                // 5 Nav Slots (Home, Services, Center Search, Bookings, Profile)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. HOME TAB (Index 0)
                    NavSlotContainer(
                        isSelected = currentTab == ServoraNavTab.HOME,
                        isHovered = isHolding && hoveredIndex == 0,
                        onClick = { onTabSelected(ServoraNavTab.HOME) },
                        testTag = ServoraNavTab.HOME.tag,
                        modifier = Modifier.weight(1f)
                    ) { isSelected, isHovered ->
                        HomeOutlineIcon(
                            isSelected = isSelected || isHovered
                        )
                    }

                    NavVerticalDivider()

                    // 2. SERVICES / CATEGORIES TAB (Index 1)
                    NavSlotContainer(
                        isSelected = currentTab == ServoraNavTab.SERVICES,
                        isHovered = isHolding && hoveredIndex == 1,
                        onClick = { onTabSelected(ServoraNavTab.SERVICES) },
                        testTag = ServoraNavTab.SERVICES.tag,
                        modifier = Modifier.weight(1f)
                    ) { isSelected, isHovered ->
                        CategoryGridOutlineIcon(
                            isSelected = isSelected || isHovered
                        )
                    }

                    NavVerticalDivider()

                    // 3. CENTER SEARCH BUTTON (Index 2 - Elevated Solid Green Circle)
                    CenterSearchSolidButton(
                        isSelected = currentTab == ServoraNavTab.SEARCH,
                        isHovered = isHolding && hoveredIndex == 2,
                        onClick = {
                            onTabSelected(ServoraNavTab.SEARCH)
                            onCenterActionClick()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    NavVerticalDivider()

                    // 4. MY BOOKINGS TAB (Index 3 - Calendar with Checkmark)
                    NavSlotContainer(
                        isSelected = currentTab == ServoraNavTab.BOOKINGS,
                        isHovered = isHolding && hoveredIndex == 3,
                        onClick = { onTabSelected(ServoraNavTab.BOOKINGS) },
                        testTag = ServoraNavTab.BOOKINGS.tag,
                        modifier = Modifier.weight(1f)
                    ) { isSelected, isHovered ->
                        BookingsCalendarOutlineIcon(
                            isSelected = isSelected || isHovered,
                            activeCount = activeBookingsCount
                        )
                    }

                    NavVerticalDivider()

                    // 5. PROFILE TAB (Index 4)
                    NavSlotContainer(
                        isSelected = currentTab == ServoraNavTab.PROFILE,
                        isHovered = isHolding && hoveredIndex == 4,
                        onClick = { onTabSelected(ServoraNavTab.PROFILE) },
                        testTag = ServoraNavTab.PROFILE.tag,
                        modifier = Modifier.weight(1f)
                    ) { isSelected, isHovered ->
                        ProfileOutlineIcon(
                            isSelected = isSelected || isHovered
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// SLOT WRAPPER WITH SPRING SCALE ANIMATION
// ============================================================================
@Composable
private fun NavSlotContainer(
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
        label = "NavSlotScale"
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
// 1. HOME OUTLINE ICON (Line Art House with Arched Doorway)
// ============================================================================
@Composable
private fun HomeOutlineIcon(
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

        val housePath = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.92f, h * 0.46f)
            lineTo(w * 0.84f, h * 0.46f)
            lineTo(w * 0.84f, h * 0.86f)
            lineTo(w * 0.62f, h * 0.86f)
            lineTo(w * 0.62f, h * 0.60f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = w * 0.38f,
                    top = h * 0.48f,
                    right = w * 0.62f,
                    bottom = h * 0.72f
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(w * 0.38f, h * 0.86f)
            lineTo(w * 0.16f, h * 0.86f)
            lineTo(w * 0.16f, h * 0.46f)
            lineTo(w * 0.08f, h * 0.46f)
            close()
        }

        drawPath(
            path = housePath,
            color = iconColor,
            style = stroke
        )
    }
}

// ============================================================================
// 2. CATEGORY / SERVICES GRID OUTLINE ICON (4 Rounded Squares)
// ============================================================================
@Composable
private fun CategoryGridOutlineIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isSelected) ServoraTheme.colors.navBarSelected else ServoraTheme.colors.navBarUnselected
    Canvas(modifier = modifier.size(24.dp)) {
        val totalSize = size.width
        val gap = totalSize * 0.16f
        val tileSize = (totalSize - gap) / 2f
        val cornerRadius = CornerRadius(tileSize * 0.38f, tileSize * 0.38f)
        val strokeWidth = (if (isSelected) 2.6.dp else 2.0.dp).toPx()

        val stroke = Stroke(
            width = strokeWidth,
            join = StrokeJoin.Round
        )

        val offsetCorrection = strokeWidth / 2f
        val adjustedSize = tileSize - strokeWidth

        // Top-Left Tile
        drawRoundRect(
            color = iconColor,
            topLeft = Offset(offsetCorrection, offsetCorrection),
            size = Size(adjustedSize, adjustedSize),
            cornerRadius = cornerRadius,
            style = stroke
        )

        // Top-Right Tile
        drawRoundRect(
            color = iconColor,
            topLeft = Offset(tileSize + gap + offsetCorrection, offsetCorrection),
            size = Size(adjustedSize, adjustedSize),
            cornerRadius = cornerRadius,
            style = stroke
        )

        // Bottom-Left Tile
        drawRoundRect(
            color = iconColor,
            topLeft = Offset(offsetCorrection, tileSize + gap + offsetCorrection),
            size = Size(adjustedSize, adjustedSize),
            cornerRadius = cornerRadius,
            style = stroke
        )

        // Bottom-Right Tile
        drawRoundRect(
            color = iconColor,
            topLeft = Offset(tileSize + gap + offsetCorrection, tileSize + gap + offsetCorrection),
            size = Size(adjustedSize, adjustedSize),
            cornerRadius = cornerRadius,
            style = stroke
        )
    }
}

// ============================================================================
// 3. CENTER SEARCH BUTTON (Elevated Solid Emerald / Dark Green Circle)
// ============================================================================
@Composable
private fun CenterSearchSolidButton(
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
        label = "CenterSearchScale"
    )

    val navColors = ServoraTheme.colors

    Box(
        modifier = modifier
            .height(52.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag("center_search_button"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(46.dp)
                .shadow(
                    elevation = if (navColors.isDark) 0.dp else if (isSelected || isHovered) 8.dp else 4.dp,
                    shape = CircleShape,
                    spotColor = Color(0x50009051) // theme-invariant
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
                imageVector = Icons.Default.Search,
                contentDescription = "Search Services",
                tint = Color.White, // theme-invariant
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================================================
// 4. BOOKINGS / CALENDAR OUTLINE ICON (Calendar with Checkmark & Top Hooks)
// ============================================================================
@Composable
private fun BookingsCalendarOutlineIcon(
    isSelected: Boolean,
    activeCount: Int = 0,
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
                contentDescription = "Bookings has updates"
            )
        }
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidth = (if (isSelected) 2.6.dp else 2.0.dp).toPx()

            val stroke = Stroke(
                width = strokeWidth,
                join = StrokeJoin.Round,
                cap = StrokeCap.Round
            )

            // 1. Top Hooks / Binder Rings
            val hookTop = h * 0.08f
            val hookBottom = h * 0.28f
            val leftHookX = w * 0.30f
            val rightHookX = w * 0.70f

            drawLine(
                color = iconColor,
                start = Offset(leftHookX, hookTop),
                end = Offset(leftHookX, hookBottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = iconColor,
                start = Offset(rightHookX, hookTop),
                end = Offset(rightHookX, hookBottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // 2. Calendar Main Body (Rounded Rectangle)
            val calLeft = strokeWidth / 2 + w * 0.06f
            val calTop = h * 0.18f
            val calRight = w * 0.94f - strokeWidth / 2
            val calBottom = h * 0.92f - strokeWidth / 2
            val cornerRadius = CornerRadius(w * 0.20f, h * 0.20f)

            val calPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = calLeft,
                        top = calTop,
                        right = calRight,
                        bottom = calBottom,
                        cornerRadius = cornerRadius
                    )
                )
            }

            drawPath(
                path = calPath,
                color = iconColor,
                style = stroke
            )

            // 3. Center Checkmark inside calendar
            val checkPath = Path().apply {
                moveTo(w * 0.32f, h * 0.55f)
                lineTo(w * 0.46f, h * 0.70f)
                lineTo(w * 0.68f, h * 0.44f)
            }

            drawPath(
                path = checkPath,
                color = iconColor,
                style = stroke
            )
        }
    }
}

// ============================================================================
// 5. PROFILE OUTLINE ICON (Line Art Head + Torso)
// ============================================================================
@Composable
private fun ProfileOutlineIcon(
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

        // Head (Outline Circle)
        drawCircle(
            color = iconColor,
            radius = w * 0.20f - strokeWidth / 2,
            center = Offset(w * 0.50f, h * 0.26f),
            style = stroke
        )

        // Torso / Shoulders (Outline Curved Arch)
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
