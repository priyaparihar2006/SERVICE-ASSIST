package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.example.ui.components.stableStatusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServiceCategory
import com.example.data.model.ServiceItem
import com.example.ui.components.hideStatusBarOnScroll
import com.example.ui.theme.ServoraTheme
import kotlinx.coroutines.delay

private val EmeraldGreen = Color(0xFF009051)
private val LightMintBg = Color(0xFFEBF5F0)
private val SoftBadgeBg = Color(0xFFD8EFE3)
private val DarkTextPrimary = Color(0xFF14241B)
private val TextMutedSecondary = Color(0xFF68776E)
private val CardBorderColor = Color(0xFFE8EFEA)
private val ScreenBgColor = Color(0xFFF9FAF9)

@Composable
fun ExploreScreen(
    categories: List<ServiceCategory>,
    services: List<ServiceItem>,
    selectedCategoryId: String?,
    searchQuery: String,
    onCategorySelect: (String?) -> Unit,
    onSearchChange: (String) -> Unit,
    onServiceClick: (ServiceItem) -> Unit,
    onBookService: (ServiceItem) -> Unit,
    onBackClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    initialSearchExpanded: Boolean = false,
    autoFocusSearch: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = ServoraTheme.colors.isDark
    val emeraldGreen = if (isDark) ServoraTheme.colors.primary else EmeraldGreen
    val lightMintBg = if (isDark) ServoraTheme.colors.surfaceVariant else LightMintBg
    val softBadgeBg = if (isDark) ServoraTheme.colors.surfaceVariant else SoftBadgeBg
    val darkTextPrimary = ServoraTheme.colors.textPrimary
    val textMutedSecondary = ServoraTheme.colors.subtext
    val cardBorderColor = ServoraTheme.colors.cardBorder

    var isSearchExpanded by remember { mutableStateOf(initialSearchExpanded || searchQuery.isNotEmpty()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(initialSearchExpanded, autoFocusSearch) {
        if (initialSearchExpanded || autoFocusSearch) {
            isSearchExpanded = true
            if (autoFocusSearch) {
                delay(120)
                try {
                    focusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    val currentCategory = categories.find { it.id == selectedCategoryId }
    val categoryTitle = currentCategory?.name ?: if (searchQuery.isNotEmpty()) "Search Results" else "All Services"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .hideStatusBarOnScroll(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ================= 1. TOP HEADER (BACK, TITLE, SEARCH, SHARE) =================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .stableStatusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back Button & Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Circular Back Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(lightMintBg)
                                .clickable {
                                    if (searchQuery.isNotEmpty()) {
                                        onSearchChange("")
                                        isSearchExpanded = false
                                    } else if (selectedCategoryId != null) {
                                        onCategorySelect(null)
                                    } else {
                                        onBackClick?.invoke()
                                    }
                                }
                                .testTag("explore_back_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = darkTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Category / Page Title
                        Text(
                            text = categoryTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 22.sp
                            ),
                            color = darkTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Circular Action Buttons (Search, Share)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Search Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSearchExpanded) softBadgeBg else lightMintBg)
                                .clickable { isSearchExpanded = !isSearchExpanded }
                                .testTag("explore_search_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchExpanded) emeraldGreen else darkTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Circular Share Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(lightMintBg)
                                .clickable { onShareClick?.invoke() }
                                .testTag("explore_share_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = darkTextPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // Expandable Search Bar
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = {
                                Text(
                                    text = "Search services (e.g. pipe, tap, facial)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        color = textMutedSecondary
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = emeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = textMutedSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("explore_search_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldGreen,
                                unfocusedBorderColor = if (isDark) cardBorderColor else Color(0xFFD4E5DC),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedTextColor = darkTextPrimary,
                                unfocusedTextColor = darkTextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // ================= 2. HORIZONTAL CATEGORY FILTER CHIPS =================
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isAllSelected = selectedCategoryId == null
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .clickable { onCategorySelect(null) }
                            .testTag("cat_chip_all"),
                        shape = RoundedCornerShape(50.dp),
                        color = if (isAllSelected) emeraldGreen else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAllSelected) emeraldGreen else cardBorderColor
                        )
                    ) {
                        Text(
                            text = "All Services",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isAllSelected) FontWeight.Medium else FontWeight.Medium,
                                fontSize = 12.5.sp
                            ),
                            color = if (isAllSelected) (if (isDark) ServoraTheme.colors.onPrimary else Color.White /* theme-invariant */) else darkTextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                items(categories) { category ->
                    val isSelected = selectedCategoryId == category.id
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .clickable { onCategorySelect(category.id) }
                            .testTag("cat_chip_${category.id}"),
                        shape = RoundedCornerShape(50.dp),
                        color = if (isSelected) emeraldGreen else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) emeraldGreen else cardBorderColor
                        )
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
                                fontSize = 12.5.sp
                            ),
                            color = if (isSelected) (if (isDark) ServoraTheme.colors.onPrimary else Color.White /* theme-invariant */) else darkTextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // ================= 4. SERVICE CARDS LIST (LARGE IMAGES, LESS TEXT) =================
        if (services.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No services matched your filter.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = darkTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onCategorySelect(null)
                            onSearchChange("")
                            isSearchExpanded = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = emeraldGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset Filters", color = Color.White /* theme-invariant */, fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            items(services) { service ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    VisualServiceExploreCard(
                        service = service,
                        onServiceClick = { onServiceClick(service) },
                        onBookService = { onBookService(service) }
                    )
                }
            }
        }
    }
}

// ============================================================================
// VISUAL SERVICE EXPLORE CARD (EXACT MATCH TO REFERENCE SCREENSHOT)
// Big image, clean hierarchy, bold title, green star rating, pill view details & floating + Add
// ============================================================================
@Composable
fun VisualServiceExploreCard(
    service: ServiceItem,
    onServiceClick: () -> Unit,
    onBookService: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ServoraTheme.colors.isDark
    val emeraldGreen = if (isDark) ServoraTheme.colors.primary else EmeraldGreen
    val lightMintBg = if (isDark) ServoraTheme.colors.surfaceVariant else LightMintBg
    val darkTextPrimary = ServoraTheme.colors.textPrimary
    val textMutedSecondary = ServoraTheme.colors.subtext
    val cardBorderColor = ServoraTheme.colors.cardBorder

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onServiceClick() }
            .testTag("service_card_${service.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Details (Title, Rating, Price/Duration, View Details)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                // Title (Bold, Clean, uncluttered)
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        lineHeight = 22.sp
                    ),
                    color = darkTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Rating Row (Green Star + Score + Reviews)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = emeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%.2f".format(service.rating),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = darkTextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${service.reviewsCount} reviews)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp
                        ),
                        color = textMutedSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Price & Duration Row: ₹89 | 🕒 30 mins
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${service.startingPrice}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        ),
                        color = emeraldGreen
                    )

                    Text(
                        text = " | ",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = if (isDark) cardBorderColor else Color(0xFFD0DDD5),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Duration",
                        tint = textMutedSecondary,
                        modifier = Modifier.size(15.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = service.duration,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp
                        ),
                        color = textMutedSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // View Details Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(lightMintBg)
                        .clickable { onServiceClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("explore_view_details_${service.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View details",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = emeraldGreen
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View details",
                            tint = emeraldGreen,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Right Column: Big Image + Floating "+ Add" Button
            Box(
                modifier = Modifier
                    .size(width = 136.dp, height = 140.dp)
            ) {
                // Background & Image Container with Rounded Corners
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) ServoraTheme.colors.surfaceVariant else Color(0xFFF2F8F4))
                ) {
                    Image(
                        painter = painterResource(id = service.imageDrawableRes),
                        contentDescription = service.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Floating "+ Add" Green Pill Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .shadow(4.dp, RoundedCornerShape(50.dp))
                        .clip(RoundedCornerShape(50.dp))
                        .background(emeraldGreen)
                        .clickable { onBookService() }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("explore_add_${service.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White, /* theme-invariant */
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = Color.White /* theme-invariant */
                        )
                    }
                }
            }
        }
    }
}

// Backwards compatibility alias
@Composable
fun ServiceExploreCard(
    service: ServiceItem,
    onServiceClick: () -> Unit,
    onBookService: () -> Unit
) {
    VisualServiceExploreCard(
        service = service,
        onServiceClick = onServiceClick,
        onBookService = onBookService
    )
}

private fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "plumbing", "build" -> Icons.Default.WaterDrop
        "ac_unit" -> Icons.Default.AcUnit
        "cleaning_services" -> Icons.Default.CleaningServices
        "spa" -> Icons.Default.Spa
        "face" -> Icons.Default.Face
        "bolt" -> Icons.Default.Bolt
        "handyman", "carpenter" -> Icons.Default.Carpenter
        "pest_control", "security" -> Icons.Default.Security
        "format_paint" -> Icons.Default.FormatPaint
        "kitchen" -> Icons.Default.Kitchen
        "water_drop" -> Icons.Default.WaterDrop
        else -> Icons.Default.Handyman
    }
}
