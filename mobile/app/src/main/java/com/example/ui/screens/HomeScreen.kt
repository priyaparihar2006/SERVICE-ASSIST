package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Sanitizer
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.CustomerReview
import com.example.data.model.ServiceCategory
import com.example.data.model.ServiceItem
import com.example.ui.components.hideStatusBarOnScroll
import kotlinx.coroutines.delay

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
private val GoldStar = Color(0xFFF59E0B)

data class GridCategoryItem(
    val title: String,
    val icon: ImageVector? = null,
    val imageDrawableRes: Int? = null,
    val categoryId: String? = null
)

data class HeroSlideData(
    val id: Int,
    val imageRes: Int,
    val badge: String,
    val title: String,
    val subtitle: String,
    val ctaText: String,
    val categoryId: String? = null
)

@Composable
fun HomeScreen(
    categories: List<ServiceCategory>,
    popularServices: List<ServiceItem>,
    reviews: List<CustomerReview>,
    activeBooking: Booking? = null,
    selectedCity: String = "Agra",
    selectedLocality: String = "Taj Nagri Phase 2",
    onCategoryClick: (ServiceCategory) -> Unit,
    onSeeAllServicesClick: () -> Unit = {},
    onServiceClick: (ServiceItem) -> Unit,
    onBookService: (ServiceItem) -> Unit,
    onTrackBookingClick: (Long) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onBecomePartnerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 8 Core Categories Grid with high quality imagery (matching reference screenshot)
    val categoryGridItems = listOf(
        GridCategoryItem("Plumbing", Icons.Default.Build, R.drawable.img_plumber_work, "cat_plumber"),
        GridCategoryItem("Cleaning", Icons.Default.CleaningServices, R.drawable.img_cleaning_pro, "cat_cleaning"),
        GridCategoryItem("AC Repair", Icons.Default.AcUnit, R.drawable.img_ac_repair, "cat_ac"),
        GridCategoryItem("Electrical", Icons.Default.Bolt, R.drawable.img_electrician_work, "cat_electrician"),
        GridCategoryItem("Painting", Icons.Default.FormatPaint, R.drawable.img_painter_work, "cat_painting"),
        GridCategoryItem("Carpentry", Icons.Default.Carpenter, R.drawable.img_carpenter_work, "cat_carpenter"),
        GridCategoryItem("Salon & Spa", Icons.Default.Spa, R.drawable.img_salon_wellness, "cat_salon_w"),
        GridCategoryItem("All Services", Icons.Default.MoreHoriz, R.drawable.img_home_makeover, null)
    )

    // Dynamic AI Curated Imagery Slide Pool
    val heroSlides = remember {
        listOf(
            HeroSlideData(
                id = 1,
                imageRes = R.drawable.img_hero_service,
                badge = "EXPERT CARE",
                title = "Home Services\nat Doorstep",
                subtitle = "",
                ctaText = "Book Now"
            ),
            HeroSlideData(
                id = 2,
                imageRes = R.drawable.img_ac_repair,
                badge = "AC CARE",
                title = "AC Foam Jet\nDeep Service",
                subtitle = "",
                ctaText = "Book AC Clean",
                categoryId = "cat_ac"
            ),
            HeroSlideData(
                id = 3,
                imageRes = R.drawable.img_cleaning_pro,
                badge = "CLEANING",
                title = "Full Home Deep\nCleaning",
                subtitle = "",
                ctaText = "Deep Clean",
                categoryId = "cat_cleaning"
            ),
            HeroSlideData(
                id = 4,
                imageRes = R.drawable.img_cozy_living,
                badge = "FABRIC CARE",
                title = "Sofa & Cushion\nRevival",
                subtitle = "",
                ctaText = "Shampoo Sofa",
                categoryId = "cat_sofa"
            ),
            HeroSlideData(
                id = 5,
                imageRes = R.drawable.img_home_makeover,
                badge = "PAINTING",
                title = "Designer Wall\nMakeover",
                subtitle = "",
                ctaText = "Free Survey",
                categoryId = "cat_painting"
            ),
            HeroSlideData(
                id = 6,
                imageRes = R.drawable.img_salon_wellness,
                badge = "SALON",
                title = "Luxury Spa &\nSalon at Home",
                subtitle = "",
                ctaText = "Explore Salon",
                categoryId = "cat_salon_w"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandWhite)
            .verticalScroll(rememberScrollState())
            .hideStatusBarOnScroll()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // =================================================================
            // 1. DYNAMIC AI IMAGE HERO CAROUSEL (Uniqueness Engine + Auto-play)
            // =================================================================
            DynamicHeroCarousel(
                slides = heroSlides,
                onSlideCtaClick = { slide ->
                    if (slide.categoryId != null) {
                        val cat = categories.find { it.id == slide.categoryId }
                        if (cat != null) onCategoryClick(cat) else onSeeAllServicesClick()
                    } else {
                        onSeeAllServicesClick()
                    }
                }
            )

            // =================================================================
            // 2. MY ACTIVE JOB CARD (Live Tracking Card if active)
            // =================================================================
            activeBooking?.let { job ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x14009051))
                        .clickable { onTrackBookingClick(job.id) }
                        .testTag("my_active_job_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandWhite),
                    border = BorderStroke(1.5.dp, BrandLightGreen)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BrandLightGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Handyman,
                                    contentDescription = "Active Job",
                                    tint = BrandGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(BrandGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE ACTIVE JOB",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = BrandGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = job.serviceName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = job.addressText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(BrandGreen)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Track",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = BrandWhite
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = BrandWhite,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 4. CATEGORIES 2x4 GRID
            // =================================================================
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Verified professionals on demand",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onSeeAllServicesClick() }
                            .testTag("categories_see_all")
                    ) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = BrandGreen
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "See All",
                            tint = BrandGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val row1 = categoryGridItems.take(4)
                val row2 = categoryGridItems.drop(4)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row1.forEach { item ->
                        CategoryGridTile(
                            item = item,
                            onClick = {
                                if (item.categoryId == null) {
                                    onSeeAllServicesClick()
                                } else {
                                    val cat = categories.find { it.id == item.categoryId }
                                    if (cat != null) onCategoryClick(cat) else onSeeAllServicesClick()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row2.forEach { item ->
                        CategoryGridTile(
                            item = item,
                            onClick = {
                                if (item.categoryId == null) {
                                    onSeeAllServicesClick()
                                } else {
                                    val cat = categories.find { it.id == item.categoryId }
                                    if (cat != null) onCategoryClick(cat) else onSeeAllServicesClick()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // =================================================================
            // 5. TRENDING / MOST POPULAR SERVICES (Visual Showcase Cards)
            // =================================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Popular Services",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Most booked in $selectedCity this week",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onSeeAllServicesClick() }
                            .testTag("home_services_see_all")
                    ) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = BrandGreen
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View All",
                            tint = BrandGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                popularServices.take(5).forEach { service ->
                    PopularServiceCard(
                        service = service,
                        onClick = { onServiceClick(service) },
                        onBookClick = { onBookService(service) }
                    )
                }
            }

            // =================================================================
            // 6. PROMO CODE / SAVINGS BANNER (Clean Green Outline)
            // =================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandVeryLightGreen),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SPECIAL WELCOME OFFER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = BrandGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Flat ₹150 OFF on 1st Service",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Use code: SERVORA150 at checkout",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandGreen)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SERVORA150",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = BrandWhite
                        )
                    }
                }
            }

            // Bottom Spacing for smooth navigation clearance
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ============================================================================
// DYNAMIC HERO CAROUSEL COMPONENT WITH UNIQUENESS ENGINE
// ============================================================================
@Composable
private fun DynamicHeroCarousel(
    slides: List<HeroSlideData>,
    onSlideCtaClick: (HeroSlideData) -> Unit,
    modifier: Modifier = Modifier
) {
    if (slides.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // History queue to prevent consecutive image repetitions
    val history = remember { mutableStateListOf<Int>() }

    fun pickNextUniqueSlide() {
        if (slides.size <= 1) return
        val currentSlide = slides[currentIndex]
        
        // Filter out slides recently seen in history
        val candidates = slides.indices.filter { idx ->
            idx != currentIndex && !history.takeLast(2).contains(idx)
        }
        
        val nextIndex = if (candidates.isNotEmpty()) {
            candidates.first() // Pick smoothly through pool
        } else {
            (currentIndex + 1) % slides.size
        }

        history.add(currentIndex)
        if (history.size > 10) history.removeAt(0)
        currentIndex = nextIndex
    }

    fun pickPreviousSlide() {
        if (slides.size <= 1) return
        currentIndex = if (currentIndex > 0) currentIndex - 1 else slides.size - 1
    }

    // Auto-rotation timer (every 4.5s, pauses on user interaction)
    LaunchedEffect(isUserInteracting, currentIndex) {
        if (!isUserInteracting) {
            delay(4500L)
            pickNextUniqueSlide()
        }
    }

    val currentSlide = slides[currentIndex]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isUserInteracting = true },
                    onDragEnd = { isUserInteracting = false },
                    onDragCancel = { isUserInteracting = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.x < -35f) {
                            pickNextUniqueSlide()
                        } else if (dragAmount.x > 35f) {
                            pickPreviousSlide()
                        }
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = Color(0x18009051)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BrandWhite),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Smooth Crossfade + Subtle Scale Image Transition
                Crossfade(
                    targetState = currentSlide,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    label = "HeroImageTransition"
                ) { slide ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = slide.imageRes),
                            contentDescription = slide.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.02f),
                            contentScale = ContentScale.Crop
                        )

                        // Dark Gradient Overlay for optimal text legibility
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xE6051E11),
                                            Color(0x99051E11),
                                            Color(0x20000000)
                                        )
                                    )
                                )
                        )
                    }
                }

                // Text Content & CTA on top of image
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Badge Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(BrandGreen)
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentSlide.badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = BrandWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currentSlide.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                lineHeight = 22.sp
                            ),
                            color = BrandWhite
                        )
                    }

                    // CTA Button in #009051 Brand Green
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSlideCtaClick(currentSlide) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGreen,
                                contentColor = BrandWhite
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = currentSlide.ctaText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        // Dot / Bar Indicators
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            slides.indices.forEach { index ->
                                val isSelected = index == currentIndex
                                val indicatorWidth by animateDpAsState(
                                    targetValue = if (isSelected) 18.dp else 6.dp,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "IndicatorWidth"
                                )

                                Box(
                                    modifier = Modifier
                                        .height(5.dp)
                                        .width(indicatorWidth)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isSelected) BrandGreen else Color(0x66FFFFFF))
                                        .clickable {
                                             isUserInteracting = true
                                             currentIndex = index
                                             isUserInteracting = false
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// CATEGORY GRID TILE COMPONENT (Soft Mint Rounded Square + Visual Imagery)
// Exact match to reference screenshot: rounded container + centered item + label underneath
// ============================================================================
@Composable
private fun CategoryGridTile(
    item: GridCategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Soft Mint Rounded Square Container (Full bleed AI imagery)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isPressed) Color(0xFFD6EDE0) else Color(0xFFEBF5F0))
                .border(1.dp, Color(0xFFE0EEE5), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageDrawableRes != null) {
                Image(
                    painter = painterResource(id = item.imageDrawableRes),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (item.icon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFEBF5F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = BrandGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Title Label Underneath
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 15.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// POPULAR SERVICE CARD COMPONENT (Clean Minimal Card with #009051 Accent)
// ============================================================================
@Composable
private fun PopularServiceCard(
    service: ServiceItem,
    onClick: () -> Unit,
    onBookClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPressed) BrandVeryLightGreen else BrandWhite),
        border = BorderStroke(1.dp, BorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Service Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                // Service Title
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        lineHeight = 20.sp
                    ),
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Rating & Duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = BrandGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "%.2f".format(service.rating),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "(${service.reviewsCount})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price & View Action
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${service.startingPrice}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = BrandGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${service.duration}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }
            }

            // Service Image & Add Button
            Box(
                modifier = Modifier.size(width = 104.dp, height = 104.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandVeryLightGreen)
                ) {
                    Image(
                        painter = painterResource(id = service.imageDrawableRes),
                        contentDescription = service.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Floating "+ Add" pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(BrandGreen)
                        .clickable { onBookClick() }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
