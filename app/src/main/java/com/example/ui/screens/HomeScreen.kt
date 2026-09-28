package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Sanitizer
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.CustomerReview
import com.example.data.model.ServiceCategory
import com.example.data.model.ServiceItem
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.theme.ServoraTheme
import kotlinx.coroutines.delay

// ============================================================================
// EXACT REFERENCE COLOR SYSTEM (#009051 Brand)
// ============================================================================
private val BrandGreen = Color(0xFF009051)
private val BrandLightGreen = Color(0xFFE6F5EF)
private val BrandVeryLightGreen = Color(0xFFF2FAF6)

data class GridCategoryItem(
    val title: String,
    val icon: ImageVector? = null,
    val imageDrawableRes: Int? = null,
    val categoryId: String? = null
)

data class HeroSlideData(
    val id: Int,
    val imageRes: Int,
    val ctaText: String = "Book Now",
    val categoryId: String? = null
)

data class AiReelItem(
    val id: String,
    val title: String,
    val tag: String,
    val views: String,
    val likes: String,
    val author: String,
    val authorRole: String,
    val coverImageRes: Int,
    val storyImages: List<Int>,
    val durationText: String,
    val serviceName: String,
    val priceText: String,
    val categoryId: String? = null
)

data class AiTransformationStudioItem(
    val id: String,
    val startingPrice: String,
    val ratingText: String,
    val verificationText: String = "Verified Professionals",
    val imageRes: Int,
    val badgeTitle: String,
    val badgeSubtitle: String,
    val serviceName: String,
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
    val isDark = ServoraTheme.colors.isDark
    val emeraldGreen = if (isDark) ServoraTheme.colors.primary else BrandGreen
    val brandGreen = emeraldGreen
    val brandLightGreen = if (isDark) ServoraTheme.colors.surfaceVariant else BrandLightGreen
    val brandVeryLightGreen = if (isDark) ServoraTheme.colors.surfaceVariant else BrandVeryLightGreen
    val textPrimary = ServoraTheme.colors.textPrimary
    val textSecondary = ServoraTheme.colors.subtext
    val borderColor = ServoraTheme.colors.cardBorder

    // 9 Core Categories for Clean 3x3 Grid
    val categoryGridItems = listOf(
        GridCategoryItem("Cleaning", Icons.Default.CleaningServices, R.drawable.img_cleaning_pro, "cat_cleaning"),
        GridCategoryItem("AC Repair", Icons.Default.AcUnit, R.drawable.img_ac_repair, "cat_ac"),
        GridCategoryItem("Plumbing", Icons.Default.Build, R.drawable.img_plumber_work, "cat_plumber"),
        GridCategoryItem("Electrical", Icons.Default.Bolt, R.drawable.img_electrician_work, "cat_electrician"),
        GridCategoryItem("Painting", Icons.Default.FormatPaint, R.drawable.img_painter_work, "cat_painting"),
        GridCategoryItem("Carpentry", Icons.Default.Carpenter, R.drawable.img_carpenter_work, "cat_carpenter"),
        GridCategoryItem("Salon & Spa", Icons.Default.Spa, R.drawable.img_salon_wellness, "cat_salon_w"),
        GridCategoryItem("Sofa Clean", Icons.Default.Handyman, R.drawable.img_sofa_clean_work, "cat_sofa"),
        GridCategoryItem("All Services", Icons.Default.MoreHoriz, null, null)
    )

    // Dynamic AI Curated Imagery Slide Pool (Clean images without text overlay)
    val heroSlides = remember {
        listOf(
            HeroSlideData(
                id = 1,
                imageRes = R.drawable.img_hero_service,
                ctaText = "Explore Services"
            ),
            HeroSlideData(
                id = 2,
                imageRes = R.drawable.img_ac_repair,
                ctaText = "Book AC Clean",
                categoryId = "cat_ac"
            ),
            HeroSlideData(
                id = 3,
                imageRes = R.drawable.img_cozy_living,
                ctaText = "Revive Sofa",
                categoryId = "cat_sofa"
            ),
            HeroSlideData(
                id = 4,
                imageRes = R.drawable.img_painter_work,
                ctaText = "Book Painting",
                categoryId = "cat_painting"
            ),
            HeroSlideData(
                id = 5,
                imageRes = R.drawable.img_salon_wellness,
                ctaText = "Explore Salon",
                categoryId = "cat_salon_w"
            ),
            HeroSlideData(
                id = 6,
                imageRes = R.drawable.img_kitchen_clean_work,
                ctaText = "Book Deep Clean",
                categoryId = "cat_cleaning"
            )
        )
    }

    // AI Reels Data List
    val aiReels = remember {
        listOf(
            AiReelItem(
                id = "reel_1",
                title = "Deep Sofa Foam Extraction in 60s",
                tag = "✨ Transformation",
                views = "48.2K",
                likes = "3.9K",
                author = "Priya Sharma",
                authorRole = "Master Cleaner",
                coverImageRes = R.drawable.img_sofa_clean_work,
                storyImages = listOf(
                    R.drawable.img_sofa_clean_work,
                    R.drawable.img_cozy_living,
                    R.drawable.img_cleaning_pro
                ),
                durationText = "0:45",
                serviceName = "Sofa Foam Shampooing",
                priceText = "₹499",
                categoryId = "cat_sofa"
            ),
            AiReelItem(
                id = "reel_2",
                title = "Hydro-Jet AC Cleaning Process",
                tag = "⚡ Pro Demo",
                views = "62.4K",
                likes = "5.1K",
                author = "Rahul Verma",
                authorRole = "AC Specialist",
                coverImageRes = R.drawable.img_ac_repair,
                storyImages = listOf(
                    R.drawable.img_ac_repair,
                    R.drawable.img_washing_machine,
                    R.drawable.img_hero_service
                ),
                durationText = "0:55",
                serviceName = "Power Jet AC Service",
                priceText = "₹599",
                categoryId = "cat_ac"
            ),
            AiReelItem(
                id = "reel_3",
                title = "Living Room Accent Wall Transformation",
                tag = "🎨 Designer Look",
                views = "35.8K",
                likes = "4.2K",
                author = "Amit Kumar",
                authorRole = "Paint Stylist",
                coverImageRes = R.drawable.img_painter_work,
                storyImages = listOf(
                    R.drawable.img_painter_work,
                    R.drawable.img_home_makeover,
                    R.drawable.img_carpenter_work
                ),
                durationText = "0:50",
                serviceName = "Accent Wall Painting",
                priceText = "₹1,499",
                categoryId = "cat_painting"
            ),
            AiReelItem(
                id = "reel_4",
                title = "Hydra Gold Facial & Instant Glow",
                tag = "💆 Salon Spa",
                views = "54.1K",
                likes = "6.3K",
                author = "Neha Singh",
                authorRole = "Beauty Expert",
                coverImageRes = R.drawable.img_facial_cleanup,
                storyImages = listOf(
                    R.drawable.img_facial_cleanup,
                    R.drawable.img_salon_wellness,
                    R.drawable.img_nail_art,
                    R.drawable.img_threading
                ),
                durationText = "1:00",
                serviceName = "Hydra Glow Facial",
                priceText = "₹899",
                categoryId = "cat_salon_w"
            ),
            AiReelItem(
                id = "reel_5",
                title = "Kitchen Oil & Chimney Degreasing",
                tag = "🔥 Deep Scrub",
                views = "41.9K",
                likes = "3.5K",
                author = "Sunil Rao",
                authorRole = "Sanitation Lead",
                coverImageRes = R.drawable.img_kitchen_clean_work,
                storyImages = listOf(
                    R.drawable.img_kitchen_clean_work,
                    R.drawable.img_kitchen_essentials,
                    R.drawable.img_dishwash_liquid
                ),
                durationText = "0:40",
                serviceName = "Kitchen Deep Degreasing",
                priceText = "₹999",
                categoryId = "cat_cleaning"
            ),
            AiReelItem(
                id = "reel_6",
                title = "Smart Electric Switchboard Fix",
                tag = "💡 Fast Fix",
                views = "27.3K",
                likes = "2.1K",
                author = "Vikas Patel",
                authorRole = "Lead Electrician",
                coverImageRes = R.drawable.img_electrician_work,
                storyImages = listOf(
                    R.drawable.img_electrician_work,
                    R.drawable.img_plumber_work,
                    R.drawable.img_carpenter_work
                ),
                durationText = "0:35",
                serviceName = "Electrical Safety Check",
                priceText = "₹299",
                categoryId = "cat_electrician"
            )
        )
    }

    // AI Transformation Studio Auto-Cycling Showcase Pool (Exact Reference Design)
    val studioItems = remember {
        listOf(
            AiTransformationStudioItem(
                id = "studio_kitchen",
                startingPrice = "₹ 499",
                ratingText = "4.93 (50 mins)",
                verificationText = "Verified Professionals",
                imageRes = R.drawable.img_kitchen_clean_work,
                badgeTitle = "Spotless Finish",
                badgeSubtitle = "Every Time",
                serviceName = "Kitchen Deep Cleaning",
                categoryId = "cat_cleaning"
            ),
            AiTransformationStudioItem(
                id = "studio_sofa",
                startingPrice = "₹ 449",
                ratingText = "4.98 (45 mins)",
                verificationText = "Verified Professionals",
                imageRes = R.drawable.img_sofa_clean_work,
                badgeTitle = "Velvet Revival",
                badgeSubtitle = "99% Stain Removal",
                serviceName = "Sofa Foam Shampooing",
                categoryId = "cat_sofa"
            ),
            AiTransformationStudioItem(
                id = "studio_ac",
                startingPrice = "₹ 599",
                ratingText = "4.96 (35 mins)",
                verificationText = "Verified Professionals",
                imageRes = R.drawable.img_ac_repair,
                badgeTitle = "Hydro Jet Clean",
                badgeSubtitle = "2X Fast Cooling",
                serviceName = "Power Jet AC Service",
                categoryId = "cat_ac"
            ),
            AiTransformationStudioItem(
                id = "studio_paint",
                startingPrice = "₹ 1,299",
                ratingText = "4.95 (3.5 hrs)",
                verificationText = "Verified Professionals",
                imageRes = R.drawable.img_painter_work,
                badgeTitle = "Royal Finish",
                badgeSubtitle = "Zero Odor Walls",
                serviceName = "Accent Wall Painting",
                categoryId = "cat_painting"
            ),
            AiTransformationStudioItem(
                id = "studio_facial",
                startingPrice = "₹ 799",
                ratingText = "4.97 (60 mins)",
                verificationText = "Verified Professionals",
                imageRes = R.drawable.img_facial_cleanup,
                badgeTitle = "Studio Radiance",
                badgeSubtitle = "Monodose Kits",
                serviceName = "Hydra Glow Facial",
                categoryId = "cat_salon_w"
            ),
            AiTransformationStudioItem(
                id = "studio_bathroom",
                startingPrice = "₹ 399",
                ratingText = "4.92 (40 mins)",
                verificationText = "Verified Professionals",
                imageRes = R.drawable.img_bathroom_cleaner,
                badgeTitle = "Mirror Chrome",
                badgeSubtitle = "100% Germ Shield",
                serviceName = "Bathroom Deep Clean",
                categoryId = "cat_cleaning"
            )
        )
    }

    var selectedReelForPlayback by remember { mutableStateOf<AiReelItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .hideStatusBarOnScroll()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // =================================================================
            // 1. DYNAMIC AI IMAGE HERO CAROUSEL (Clean Images Without Text Overlays)
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
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x14009051) /* theme-invariant */)
                        .clickable { onTrackBookingClick(job.id) }
                        .testTag("my_active_job_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, if (isDark) borderColor else BrandLightGreen)
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
                                    .background(brandLightGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Handyman,
                                    contentDescription = "Active Job",
                                    tint = brandGreen,
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
                                            .background(brandGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE ACTIVE JOB",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = brandGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = job.serviceName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
                                    ),
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = job.addressText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(brandGreen)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Track",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = Color.White /* theme-invariant */
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White, /* theme-invariant */
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 3. CATEGORIES 2x4 GRID (Exact Match to Reference Screenshot)
            // =================================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Green Top Accent Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(brandGreen)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = "Verified professionals on demand",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onSeeAllServicesClick() }
                            .padding(bottom = 2.dp)
                            .testTag("categories_see_all")
                    ) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = brandGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "See All",
                            tint = brandGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                val row1 = categoryGridItems.take(3)
                val row2 = categoryGridItems.drop(3).take(3)
                val row3 = categoryGridItems.drop(6).take(3)

                // Row 1 (3 items)
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

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2 (3 items)
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

                Spacer(modifier = Modifier.height(8.dp))

                // Row 3 (3 items)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row3.forEach { item ->
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
            // 4. ✨ AI REELS & SHORT VIDEOS SECTION
            // =================================================================
            AiReelsSection(
                reels = aiReels,
                onReelClick = { reel -> selectedReelForPlayback = reel },
                onExploreAll = { onSeeAllServicesClick() }
            )

            // =================================================================
            // 5. 🌟 REDESIGNED AI TRANSFORMATION STUDIO (Exact Reference Design)
            // =================================================================
            AiTransformationShowcaseSection(
                items = studioItems,
                onBookServiceClick = { studioItem ->
                    val matchedService = popularServices.find { it.name.contains(studioItem.serviceName, ignoreCase = true) }
                        ?: popularServices.firstOrNull()
                    if (matchedService != null) {
                        onBookService(matchedService)
                    } else {
                        onSeeAllServicesClick()
                    }
                }
            )

            // =================================================================
            // 6. TRENDING / MOST POPULAR SERVICES (Visual Showcase Cards)
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
                            color = textPrimary
                        )
                        Text(
                            text = "Most booked in $selectedCity this week",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
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
                            color = brandGreen
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View All",
                            tint = brandGreen,
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
            // 7. PROMO CODE / SAVINGS BANNER (Clean Green Outline)
            // =================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) ServoraTheme.colors.surfaceVariant else BrandVeryLightGreen),
                border = BorderStroke(1.dp, borderColor)
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
                            color = brandGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Flat ₹150 OFF on 1st Service",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Use code: SERVORA150 at checkout",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(brandGreen)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SERVORA150",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color.White /* theme-invariant */
                        )
                    }
                }
            }

            // Bottom Spacing for smooth navigation clearance above floating transparent dock
            Spacer(modifier = Modifier.height(84.dp))
        }
    }

    // =========================================================================
    // FULLSCREEN INTERACTIVE AI REEL STORY PLAYER DIALOG
    // =========================================================================
    selectedReelForPlayback?.let { reel ->
        AiReelStoryPlayerDialog(
            reel = reel,
            onDismiss = { selectedReelForPlayback = null },
            onBookService = {
                selectedReelForPlayback = null
                val matchedService = popularServices.find { it.name.contains(reel.serviceName, ignoreCase = true) }
                    ?: popularServices.firstOrNull()
                if (matchedService != null) {
                    onBookService(matchedService)
                } else {
                    onSeeAllServicesClick()
                }
            }
        )
    }
}

// ============================================================================
// DYNAMIC HERO CAROUSEL COMPONENT (Clean Visual Images Without Text Overlays)
// ============================================================================
@Composable
private fun DynamicHeroCarousel(
    slides: List<HeroSlideData>,
    onSlideCtaClick: (HeroSlideData) -> Unit,
    modifier: Modifier = Modifier
) {
    if (slides.isEmpty()) return

    val isDark = ServoraTheme.colors.isDark
    val brandGreen = if (isDark) ServoraTheme.colors.primary else BrandGreen
    val borderColor = ServoraTheme.colors.cardBorder

    var currentIndex by remember { mutableIntStateOf(0) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // History queue to prevent consecutive image repetitions
    val history = remember { mutableStateListOf<Int>() }

    fun pickNextUniqueSlide() {
        if (slides.size <= 1) return
        val candidates = slides.indices.filter { idx ->
            idx != currentIndex && !history.takeLast(2).contains(idx)
        }
        val nextIndex = if (candidates.isNotEmpty()) candidates.first() else (currentIndex + 1) % slides.size
        history.add(currentIndex)
        if (history.size > 10) history.removeAt(0)
        currentIndex = nextIndex
    }

    fun pickPreviousSlide() {
        if (slides.size <= 1) return
        currentIndex = if (currentIndex > 0) currentIndex - 1 else slides.size - 1
    }

    // Auto-rotation timer (every 4.5s)
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
                .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x18009051) /* theme-invariant */)
                .clickable { onSlideCtaClick(currentSlide) },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Smooth Crossfade Image Transition (100% clean image without text overlays)
                Crossfade(
                    targetState = currentSlide.imageRes,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    label = "HeroImageTransition"
                ) { imageRes ->
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "Service Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Subtle bottom shadow gradient for CTA button and pagination dots visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x88000000) /* theme-invariant */
                                )
                            )
                        )
                )

                // Bottom Controls: CTA Button on Left + Pagination Dots on Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // CTA Button in Brand Green
                    Button(
                        onClick = { onSlideCtaClick(currentSlide) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandGreen,
                            contentColor = Color.White /* theme-invariant */
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = currentSlide.ctaText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color.White /* theme-invariant */
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White /* theme-invariant */,
                            modifier = Modifier.size(13.dp)
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
                                    .background(if (isSelected) brandGreen else Color(0xAAFFFFFF) /* theme-invariant */)
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

// ============================================================================
// ✨ AI REELS & SHORT VIDEOS SECTION
// ============================================================================
@Composable
private fun AiReelsSection(
    reels: List<AiReelItem>,
    onReelClick: (AiReelItem) -> Unit,
    onExploreAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ServoraTheme.colors.isDark
    val brandGreen = if (isDark) ServoraTheme.colors.primary else BrandGreen
    val brandLightGreen = if (isDark) ServoraTheme.colors.surfaceVariant else BrandLightGreen
    val textPrimary = ServoraTheme.colors.textPrimary
    val textSecondary = ServoraTheme.colors.subtext

    val infiniteTransition = rememberInfiniteTransition(label = "ReelsGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(brandLightGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reels",
                    tint = brandGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Reels & Shorts",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF3366) /* theme-invariant */)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White /* theme-invariant */,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Text(
                    text = "Real service transformations in 60s",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textSecondary
                )
            }
        }

        // Horizontal Reels Strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(reels) { reel ->
                AiReelCard(
                    reel = reel,
                    pulseAlpha = pulseAlpha,
                    onClick = { onReelClick(reel) }
                )
            }
        }
    }
}

// ============================================================================
// AI REEL CARD COMPONENT
// ============================================================================
@Composable
private fun AiReelCard(
    reel: AiReelItem,
    pulseAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ServoraTheme.colors.isDark
    val brandGreen = if (isDark) ServoraTheme.colors.primary else BrandGreen

    Card(
        modifier = modifier
            .width(148.dp)
            .height(230.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = Color(0x18009051) /* theme-invariant */)
            .clickable { onClick() }
            .testTag("reel_${reel.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, brandGreen.copy(alpha = pulseAlpha * 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Reel Cover Image
            Image(
                painter = painterResource(id = reel.coverImageRes),
                contentDescription = reel.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dynamic Dark Vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x80000000) /* theme-invariant */,
                                Color(0x10000000) /* theme-invariant */,
                                Color(0xE0051E11) /* theme-invariant */
                            )
                        )
                    )
            )

            // Top Header: Tag + Views
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC009051) /* theme-invariant */)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = reel.tag,
                        color = Color.White /* theme-invariant */,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x99000000) /* theme-invariant */)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White /* theme-invariant */,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = reel.views,
                        color = Color.White /* theme-invariant */,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Center Play Icon Button with Glowing Ring
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000) /* theme-invariant */)
                    .border(1.5.dp, Color(0xFF55E6A5) /* theme-invariant */, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White /* theme-invariant */,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Bottom Info: Title, Author, Price
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
            ) {
                Text(
                    text = reel.title,
                    color = Color.White /* theme-invariant */,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reel.author,
                        color = Color(0xFFBCE7D3) /* theme-invariant */,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Text(
                        text = reel.priceText,
                        color = Color(0xFF55E6A5) /* theme-invariant */,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// ============================================================================
// 🌟 REDESIGNED AI TRANSFORMATION STUDIO COMPONENT (Exact Match to Reference Image)
// Features: Sparkle Header, Starting Price Pill, Rating & Verified Badges,
// Auto-cycling Image with Crossfade Transition, Glassmorphism Badge, Pagination Dots, Next Button, Full CTA Button
// ============================================================================
@Composable
private fun AiTransformationShowcaseSection(
    items: List<AiTransformationStudioItem>,
    onBookServiceClick: (AiTransformationStudioItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val isDark = ServoraTheme.colors.isDark
    val brandGreen = if (isDark) ServoraTheme.colors.primary else BrandGreen
    val textPrimary = ServoraTheme.colors.textPrimary
    val textSecondary = ServoraTheme.colors.subtext
    val borderColor = ServoraTheme.colors.cardBorder
    val surfaceVariant = ServoraTheme.colors.surfaceVariant
    val pillBg = if (isDark) surfaceVariant else Color(0xFFEAF7F0)
    val pillText = if (isDark) brandGreen else Color(0xFF1B633D)
    val pillSubtext = if (isDark) textSecondary else Color(0xFF507E63)
    val circleIconBg = if (isDark) surfaceVariant else Color(0xFFE2F7ED)
    val glassBg = if (isDark) Color(0xCC1E2922) else Color(0xCCFFFFFF)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF)
    val nextBtnBg = if (isDark) surfaceVariant else Color.White

    var currentIndex by remember { mutableIntStateOf(0) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // Auto-cycling timer: Automatically changes image every 3.2 seconds
    LaunchedEffect(currentIndex, isUserInteracting) {
        if (!isUserInteracting) {
            delay(3200L)
            currentIndex = (currentIndex + 1) % items.size
        }
    }

    val currentItem = items[currentIndex]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = Color(0x18009051) /* theme-invariant */,
                ambientColor = Color(0x0C000000) /* theme-invariant */
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =========================================================
            // 1. TOP HEADER: Sparkle Icon + Title + Starting Price Pill
            // =========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Circular Mint Icon + Title & Subtitle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(circleIconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Studio",
                            tint = brandGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Works",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                lineHeight = 21.sp
                            ),
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Multi-Photo Proof of Work",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right: Starting Price Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillBg)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Starting",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = pillSubtext
                        )
                        Text(
                            text = currentItem.startingPrice,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = brandGreen
                        )
                        Text(
                            text = "/ session",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = pillSubtext
                        )
                    }
                }
            }

            // =========================================================
            // 2. SUB-HEADER PILLS: Rating & Verification
            // =========================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill 1: Rating & Duration
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(pillBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = brandGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = currentItem.ratingText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = pillText
                        )
                    }
                }

                // Pill 2: Verified Professionals
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(pillBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = brandGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = currentItem.verificationText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = pillText
                        )
                    }
                }
            }

            // =========================================================
            // 3. CENTER IMAGE CONTAINER WITH AUTO-CYCLING MULTI-PHOTOS
            // =========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F2218) /* theme-invariant */)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isUserInteracting = true },
                            onDragEnd = { isUserInteracting = false },
                            onDragCancel = { isUserInteracting = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.x < -30f) {
                                    currentIndex = (currentIndex + 1) % items.size
                                } else if (dragAmount.x > 30f) {
                                    currentIndex = if (currentIndex > 0) currentIndex - 1 else items.size - 1
                                }
                            }
                        )
                    }
            ) {
                // Auto-cycling Image with Crossfade Transition
                Crossfade(
                    targetState = currentItem.imageRes,
                    animationSpec = tween(650, easing = FastOutSlowInEasing),
                    label = "StudioImageCrossfade"
                ) { imgRes ->
                    Image(
                        painter = painterResource(id = imgRes),
                        contentDescription = currentItem.badgeTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.02f),
                        contentScale = ContentScale.Crop
                    )
                }

                // Subtle Bottom Gradient for badge contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color(0x88000000) /* theme-invariant */
                                )
                            )
                        )
                )

                // Bottom Left Glassmorphism Pill: Spotless Finish / Every Time
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(glassBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = brandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = currentItem.badgeTitle,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = textPrimary
                            )
                            Text(
                                text = currentItem.badgeSubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = textSecondary
                            )
                        }
                    }
                }

                // Bottom Right Controls: Dots Pagination + Next Button
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pagination Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.indices.forEach { idx ->
                            val isSelected = idx == currentIndex
                            val dotWidth by animateDpAsState(
                                targetValue = if (isSelected) 14.dp else 6.dp,
                                animationSpec = tween(300),
                                label = "DotWidth"
                            )
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(dotWidth)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSelected) brandGreen else Color(0xAAFFFFFF) /* theme-invariant */)
                                    .clickable {
                                        isUserInteracting = true
                                        currentIndex = idx
                                        isUserInteracting = false
                                    }
                            )
                        }
                    }

                    // Next Circular Arrow Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(nextBtnBg)
                            .clickable {
                                isUserInteracting = true
                                currentIndex = (currentIndex + 1) % items.size
                                isUserInteracting = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Photo",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // =========================================================
            // 4. FULL-WIDTH SOLID GREEN CTA BUTTON
            // =========================================================
            Button(
                onClick = { onBookServiceClick(currentItem) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = brandGreen,
                    contentColor = Color.White /* theme-invariant */
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Book",
                        tint = Color.White /* theme-invariant */,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Book Service",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color.White /* theme-invariant */
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White /* theme-invariant */,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// FULLSCREEN INTERACTIVE AI REEL STORY PLAYER DIALOG
// ============================================================================
@Composable
private fun AiReelStoryPlayerDialog(
    reel: AiReelItem,
    onDismiss: () -> Unit,
    onBookService: () -> Unit
) {
    val isDark = ServoraTheme.colors.isDark
    val brandGreen = if (isDark) ServoraTheme.colors.primary else BrandGreen

    var activeStoryIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    val storyCount = reel.storyImages.size

    // Story progress timer (advances every 3.5 seconds)
    LaunchedEffect(activeStoryIndex, isPaused) {
        if (!isPaused) {
            delay(3500L)
            if (activeStoryIndex < storyCount - 1) {
                activeStoryIndex += 1
            } else {
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black /* theme-invariant */)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Tap left: previous slide
                                if (activeStoryIndex > 0) activeStoryIndex -= 1
                            } else {
                                // Tap right: next slide
                                if (activeStoryIndex < storyCount - 1) {
                                    activeStoryIndex += 1
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Story Image with Smooth Animation
            Crossfade(
                targetState = reel.storyImages[activeStoryIndex],
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "StoryImageCrossfade"
            ) { imageRes ->
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = reel.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Top Gradient & Bottom Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC000000) /* theme-invariant */,
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xF0000000) /* theme-invariant */
                            )
                        )
                    )
            )

            // Top Header: Progress Bars & Author Profile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 20.dp)
            ) {
                // Segmented Story Progress Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0 until storyCount) {
                        val progress = when {
                            i < activeStoryIndex -> 1f
                            i == activeStoryIndex -> 1f
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x66FFFFFF) /* theme-invariant */)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(Color.White /* theme-invariant */)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Creator / Pro Profile Info + Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(brandGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = reel.author.take(1),
                                color = Color.White /* theme-invariant */,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = reel.author,
                                    color = Color.White /* theme-invariant */,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF55E6A5) /* theme-invariant */,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = "${reel.authorRole} • ${reel.views} views",
                                color = Color(0xFFCCCCCC) /* theme-invariant */,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    // Controls: Sound toggle & Close
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White /* theme-invariant */,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White /* theme-invariant */,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Right Floating Action Bar (Like, Share, Soundwave)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Like Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000) /* theme-invariant */)
                            .clickable { isLiked = !isLiked },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFFFF3366) /* theme-invariant */ else Color.White /* theme-invariant */,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLiked) "Liked!" else reel.likes,
                        color = Color.White /* theme-invariant */,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // AI Soundwave Indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000) /* theme-invariant */),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Sound",
                        tint = Color(0xFF55E6A5) /* theme-invariant */,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Bottom CTA Card: Direct Service Booking
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE112217) /* theme-invariant */),
                border = BorderStroke(1.dp, brandGreen)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reel.serviceName,
                            color = Color.White /* theme-invariant */,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Starting at ${reel.priceText} • Instant Slot",
                            color = Color(0xFF55E6A5) /* theme-invariant */,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onBookService,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandGreen,
                            contentColor = Color.White /* theme-invariant */
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Book Now ➔",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White /* theme-invariant */
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// CATEGORY GRID TILE COMPONENT (Clean, Large 3x3 Card Layout)
// Features: Premium card with large photo container + Title + Green Arrow
// ============================================================================
@Composable
private fun CategoryGridTile(
    item: GridCategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = ServoraTheme.colors.isDark
    val brandGreen = if (isDark) ServoraTheme.colors.primary else BrandGreen
    val brandVeryLightGreen = if (isDark) ServoraTheme.colors.surfaceVariant else BrandVeryLightGreen
    val textPrimary = ServoraTheme.colors.textPrimary
    val borderColor = ServoraTheme.colors.cardBorder
    val imgContainerBg = if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFEBF5F0)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPressed) brandVeryLightGreen else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.1.dp, if (isPressed) brandGreen.copy(alpha = 0.5f) else borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            // Large Top Image Container with rounded corners and mint background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(imgContainerBg),
                contentAlignment = Alignment.Center
            ) {
                if (item.categoryId == null) {
                    // All Services Icon: 4 Solid Green Rounded Squares in 2x2 grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.5.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brandGreen)
                            )
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brandGreen)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.5.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brandGreen)
                            )
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(brandGreen)
                            )
                        }
                    }
                } else if (item.imageDrawableRes != null) {
                    // Full-bleed Image
                    Image(
                        painter = painterResource(id = item.imageDrawableRes),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (item.icon != null) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = brandGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Bottom Label & Green Arrow Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    ),
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = brandGreen,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
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
    val isDark = ServoraTheme.colors.isDark
    val brandGreen = if (isDark) ServoraTheme.colors.primary else BrandGreen
    val brandVeryLightGreen = if (isDark) ServoraTheme.colors.surfaceVariant else BrandVeryLightGreen
    val textPrimary = ServoraTheme.colors.textPrimary
    val textSecondary = ServoraTheme.colors.subtext
    val borderColor = ServoraTheme.colors.cardBorder

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPressed) brandVeryLightGreen else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
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
                    color = textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Rating & Duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = brandGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "%.2f".format(service.rating),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "(${service.reviewsCount})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
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
                        color = brandGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${service.duration}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
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
                        .background(brandVeryLightGreen)
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
                        .background(brandGreen)
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
                            tint = Color.White /* theme-invariant */,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color.White /* theme-invariant */
                        )
                    }
                }
            }
        }
    }
}
