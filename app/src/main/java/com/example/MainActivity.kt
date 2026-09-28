package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.NotificationType
import com.example.data.model.ServiceItem
import com.example.data.model.ServicePackage
import com.example.data.model.UserRole
import com.example.ui.components.StatusBarIcons
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.ui.components.AdminBottomNav
import com.example.ui.components.AdminNavTab
import com.example.ui.components.FloatingCartBar
import com.example.ui.components.IncomingMessageBanner
import com.example.ui.components.LocationPickerModal
import com.example.ui.components.PartnerBottomNav
import com.example.ui.components.PartnerNavTab
import com.example.ui.components.SearchOverlay
import com.example.ui.components.ServoraBottomNav
import com.example.ui.components.ServoraNavTab
import com.example.ui.components.ServoraTopBar
import com.example.ui.components.stableStatusBarsPadding
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseConfig
import com.example.data.repository.FakeChatRepository
import com.example.data.repository.RemoteChatRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.BookingConfirmationScreen
import com.example.ui.screens.BookingFlowScreen
import com.example.ui.screens.BookingsListScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OffersScreen
import com.example.ui.screens.PartnerEarningsScreen
import com.example.ui.screens.PartnerJobsScreen
import com.example.ui.screens.PartnerSettlementHistoryScreen
import com.example.ui.screens.PartnerToolkitScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ServiceDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatViewModelFactory
import com.example.ui.viewmodel.ServoraViewModel

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.example.data.prefs.AppTheme
import com.example.data.prefs.RainbowColor
import com.example.data.prefs.ThemePreferenceRepository
import com.example.ui.viewmodel.ThemeViewModel
import com.example.ui.viewmodel.ThemeViewModelFactory

import com.example.data.model.AppNotification
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PartnerBookingsScreen
import com.example.ui.screens.PartnerJobDetailScreen
import com.example.ui.screens.PaymentCollectionScreen
import com.example.ui.viewmodel.PaymentViewModel
import com.example.ui.viewmodel.PaymentViewModelFactory
import com.example.data.repository.PaymentRepository

sealed interface AppScreen {
    data object Login : AppScreen
    data object MainTabs : AppScreen
    data object MyBookings : AppScreen
    data object PartnerBookings : AppScreen
    data object PartnerSettlementHistory : AppScreen
    data object Notifications : AppScreen
    data class ServiceDetail(val service: ServiceItem) : AppScreen
    data class BookingFlow(val service: ServiceItem, val pkg: ServicePackage?) : AppScreen
    data class BookingTracking(val bookingId: Long) : AppScreen
    data class ChatThread(val conversationId: String, val isAdminView: Boolean = false) : AppScreen
    data class PaymentCollection(val bookingId: Long) : AppScreen
    data class PartnerJobDetail(val bookingId: Long) : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        insetsController.show(WindowInsetsCompat.Type.statusBars())

        val themeRepo = ThemePreferenceRepository(applicationContext)
        val (initialTheme, initialColor) = runBlocking {
            try {
                Pair(themeRepo.themeFlow.first(), themeRepo.colorFlow.first())
            } catch (_: Exception) {
                Pair(AppTheme.LIGHT, RainbowColor.GREEN)
            }
        }
        val initialDark = initialTheme == AppTheme.DARK
        window.setBackgroundDrawable(ColorDrawable(if (initialDark) 0xFF0F1412.toInt() else 0xFFFFFFFF.toInt()))

        setContent {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModelFactory(themeRepo, initialTheme, initialColor)
            )
            val currentAppTheme by themeViewModel.appTheme.collectAsState()
            val selectedColor by themeViewModel.selectedColor.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            val effectiveDark = when (currentAppTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> systemInDark
            }

            MyApplicationTheme(
                darkTheme = effectiveDark,
                rainbowColor = selectedColor
            ) {
                ServoraApp(
                    isDarkTheme = effectiveDark,
                    selectedColor = selectedColor,
                    appTheme = currentAppTheme,
                    onToggleTheme = { themeViewModel.toggle() },
                    onSetTheme = { themeViewModel.setTheme(it) },
                    onSetColor = { themeViewModel.setColor(it) }
                )
            }
        }
    }
}

@Composable
fun ServoraApp(
    viewModel: ServoraViewModel = viewModel(),
    isDarkTheme: Boolean = false,
    selectedColor: RainbowColor = RainbowColor.GREEN,
    appTheme: AppTheme = AppTheme.LIGHT,
    onToggleTheme: () -> Unit = {},
    onSetTheme: (AppTheme) -> Unit = {},
    onSetColor: (RainbowColor) -> Unit = {}
) {
    val view = LocalView.current
    val activity = LocalContext.current as? Activity
    val insetsController = remember(view, activity) {
        if (activity != null) WindowCompat.getInsetsController(activity.window, view) else null
    }

    val appContext = LocalContext.current.applicationContext
    val appScope = rememberCoroutineScope()
    val servoraDb = remember(appContext) {
        com.example.data.db.ServoraDatabase.getDatabase(appContext, appScope)
    }

    val chatRepository = remember(servoraDb) {
        val remoteApi = SupabaseClient.chatApiService
        if (com.example.BuildConfig.DEBUG && com.example.BuildConfig.CHAT_USE_FAKE) {
            FakeChatRepository()
        } else if (SupabaseConfig.isConfigured && remoteApi != null) {
            RemoteChatRepository(remoteApi, servoraDb.bookingDao())
        } else {
            com.example.data.repository.UnconfiguredChatRepository()
        }
    }
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(chatRepository, servoraDb.chatDao())
    )

    val paymentRepository = remember(servoraDb) {
        PaymentRepository(SupabaseClient.paymentApiService, servoraDb.bookingDao())
    }
    val paymentViewModel: PaymentViewModel = viewModel(
        factory = PaymentViewModelFactory(paymentRepository, servoraDb.bookingDao())
    )

    var currentTab by remember { mutableStateOf(ServoraNavTab.HOME) }
    var currentPartnerTab by remember { mutableStateOf(PartnerNavTab.DUTY_JOBS) }
    var currentAdminTab by remember { mutableStateOf(AdminNavTab.OVERVIEW) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showSearchOverlay by remember { mutableStateOf(false) }
    var isBottomNavVisible by remember { mutableStateOf(true) }

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val impersonatingAdminProfile by viewModel.impersonatingAdminProfile.collectAsState()
    val allBookings by viewModel.allBookings.collectAsState()
    val displayedBookings by viewModel.displayedBookings.collectAsState()
    val activeBooking by viewModel.activeBooking.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    val displayedAddresses by viewModel.displayedAddresses.collectAsState()
    val allReviews by viewModel.allReviews.collectAsState()
    val displayedReviews by viewModel.displayedReviews.collectAsState()
    val filteredServices by viewModel.filteredServices.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val bookingDraft by viewModel.bookingDraft.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val unreadChatsCount by chatViewModel.unreadTotal.collectAsState()
    val unreadByBookingId by chatViewModel.unreadByBookingId.collectAsState()
    val incomingBanner by chatViewModel.incomingBanner.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCustomerNotificationsCount by viewModel.unreadCustomerNotificationsCount.collectAsState()
    val unreadPartnerNotificationsCount by viewModel.unreadPartnerNotificationsCount.collectAsState()

    var isCartVisible by remember { mutableStateOf(true) }
    val cartItems by viewModel.cartItems.collectAsState()
    val cartTotalCount by viewModel.cartTotalCount.collectAsState()
    val cartTotalPrice by viewModel.cartTotalPrice.collectAsState()
    val cartTotalSavings by viewModel.cartTotalSavings.collectAsState()

    LaunchedEffect(cartTotalCount) {
        if (cartTotalCount > 0) {
            isCartVisible = true
        }
    }

    // Observe user / session changes to switch chat repository profile and cursors
    LaunchedEffect(currentUser.id, currentUser.role, impersonatingAdminProfile != null) {
        chatViewModel.onSessionChanged(currentUser.id, currentUser.role.name)
    }

    // Refresh chats whenever booking codes or statuses change (not on every fullSync Room write)
    val bookingStatesKey = remember(allBookings) {
        allBookings.map { it.bookingCode to it.status }
    }
    LaunchedEffect(bookingStatesKey) {
        chatViewModel.loadConversations()
    }

    // Always restore bottom nav visibility and status bar when switching tabs or screens
    LaunchedEffect(currentTab, currentPartnerTab, currentAdminTab, currentScreen) {
        isBottomNavVisible = true
        insetsController?.show(WindowInsetsCompat.Type.statusBars())
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -8f) {
                    // Scrolling down (content moving up) -> hide bottom nav
                    isBottomNavVisible = false
                } else if (available.y > 8f) {
                    // Scrolling up (content moving down) -> show bottom nav
                    isBottomNavVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // Handle back button behavior
    BackHandler(enabled = (currentScreen !is AppScreen.MainTabs && currentScreen !is AppScreen.Login) || showSearchOverlay) {
        if (showSearchOverlay) {
            showSearchOverlay = false
        } else {
            if (currentScreen is AppScreen.PartnerJobDetail) {
                currentPartnerTab = PartnerNavTab.DUTY_JOBS
            }
            currentScreen = AppScreen.MainTabs
        }
    }

    val isMainTabsActive = currentScreen is AppScreen.MainTabs && isLoggedIn

    val isDarkHeader = when {
        showSearchOverlay -> false
        !isLoggedIn || currentScreen is AppScreen.Login -> false
        currentScreen is AppScreen.Notifications -> true
        currentScreen is AppScreen.ServiceDetail -> true
        currentScreen is AppScreen.PartnerSettlementHistory -> true
        currentScreen is AppScreen.PartnerBookings -> true
        currentScreen is AppScreen.PartnerJobDetail -> false
        currentScreen is AppScreen.ChatThread -> false
        currentScreen is AppScreen.MyBookings -> false
        currentScreen is AppScreen.BookingFlow -> false
        currentScreen is AppScreen.BookingTracking -> false
        currentScreen is AppScreen.PaymentCollection -> false
        currentScreen is AppScreen.MainTabs -> {
            when (currentUser.role) {
                UserRole.ADMIN -> true
                UserRole.PROFESSIONAL -> {
                    when (currentPartnerTab) {
                        PartnerNavTab.DUTY_JOBS -> true
                        PartnerNavTab.BOOKINGS -> true
                        PartnerNavTab.EARNINGS -> true
                        PartnerNavTab.PROFILE -> true
                        PartnerNavTab.TOOLKIT -> false
                    }
                }
                UserRole.CUSTOMER -> {
                    when (currentTab) {
                        ServoraNavTab.PROFILE -> true
                        ServoraNavTab.HOME -> true
                        ServoraNavTab.SERVICES -> false
                        ServoraNavTab.SEARCH -> false
                        ServoraNavTab.BOOKINGS -> false
                    }
                }
            }
        }
        else -> false
    }

    StatusBarIcons(darkIcons = !isDarkHeader)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                val shouldShowCustomerTopBar = isMainTabsActive &&
                    currentUser.role == UserRole.CUSTOMER &&
                    currentTab == ServoraNavTab.HOME

                if (shouldShowCustomerTopBar) {
                    ServoraTopBar(
                        selectedCity = currentUser.city,
                        selectedLocality = currentUser.locality,
                        userRole = currentUser.role,
                        onLocationClick = { showLocationPicker = true },
                        onSearchClick = { currentTab = ServoraNavTab.SEARCH },
                        onNotificationsClick = { currentScreen = AppScreen.Notifications },
                        unreadNotificationsCount = unreadCustomerNotificationsCount,
                        isImpersonating = impersonatingAdminProfile != null,
                        onReturnToAdminClick = { viewModel.adminReturnToDashboard() }
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isMainTabsActive && isBottomNavVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 220)
                    ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 220)
                    ) + fadeOut(animationSpec = tween(durationMillis = 180))
                ) {
                    when (currentUser.role) {
                        UserRole.ADMIN -> {
                            val pendingAdminJobs = allBookings.count {
                                it.status != BookingStatus.COMPLETED &&
                                it.status != BookingStatus.CANCELLED
                            }
                            AdminBottomNav(
                                currentTab = currentAdminTab,
                                onTabSelected = { 
                                    currentAdminTab = it 
                                    isBottomNavVisible = true
                                },
                                pendingBookingsCount = pendingAdminJobs
                            )
                        }
                        UserRole.PROFESSIONAL -> {
                            val pendingPartnerJobs = displayedBookings.count {
                                it.status != BookingStatus.COMPLETED &&
                                it.status != BookingStatus.CANCELLED
                            }
                            PartnerBottomNav(
                                currentTab = currentPartnerTab,
                                onTabSelected = { 
                                    currentPartnerTab = it 
                                    isBottomNavVisible = true
                                },
                                activeJobsCount = pendingPartnerJobs
                            )
                        }
                        UserRole.CUSTOMER -> {
                            val activeCount = displayedBookings.count {
                                it.status != BookingStatus.COMPLETED &&
                                it.status != BookingStatus.CANCELLED
                            }
                            ServoraBottomNav(
                                currentTab = currentTab,
                                onTabSelected = { 
                                    currentTab = it 
                                    isBottomNavVisible = true
                                },
                                onCenterActionClick = {
                                    currentTab = ServoraNavTab.SEARCH
                                    isBottomNavVisible = true
                                },
                                activeBookingsCount = activeCount
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        if (isMainTabsActive) {
                            androidx.compose.foundation.layout.PaddingValues(
                                top = innerPadding.calculateTopPadding(),
                                bottom = 0.dp
                            )
                        } else {
                            androidx.compose.foundation.layout.PaddingValues(0.dp)
                        }
                    )
            ) {
                if (!isLoggedIn || currentScreen is AppScreen.Login) {
                    LoginScreen(
                        onLoginWithProfile = { profile ->
                            viewModel.loginWithProfile(profile)
                            currentScreen = AppScreen.MainTabs
                            when (profile.role) {
                                UserRole.CUSTOMER -> currentTab = ServoraNavTab.HOME
                                UserRole.PROFESSIONAL -> currentPartnerTab = PartnerNavTab.DUTY_JOBS
                                UserRole.ADMIN -> currentAdminTab = AdminNavTab.OVERVIEW
                            }
                        },
                        onLoginWithCredentials = { role, id, name ->
                            viewModel.loginWithCredentials(role, id, name)
                            currentScreen = AppScreen.MainTabs
                            when (role) {
                                UserRole.CUSTOMER -> currentTab = ServoraNavTab.HOME
                                UserRole.PROFESSIONAL -> currentPartnerTab = PartnerNavTab.DUTY_JOBS
                                UserRole.ADMIN -> currentAdminTab = AdminNavTab.OVERVIEW
                            }
                        },
                        demoCustomer = viewModel.demoCustomer,
                        demoPartner = viewModel.demoPartner,
                        demoAdmin = viewModel.demoAdmin
                    )
                } else {
                    when (val screen = currentScreen) {
                        is AppScreen.Login -> {
                            // Handled above
                        }
                        is AppScreen.MainTabs -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Persistent Impersonation Banner for Admin
                                if (impersonatingAdminProfile != null) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.adminReturnToDashboard() },
                                        color = Color(0xFF4F46E5)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AdminPanelSettings,
                                                    contentDescription = "Admin",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Viewing as ${currentUser.name} (${currentUser.role.name})",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "Return to Admin ➔",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    when (currentUser.role) {
                                        UserRole.ADMIN -> {
                                            AdminDashboardScreen(
                                                currentAdmin = currentUser,
                                                currentTab = currentAdminTab,
                                                allCustomers = viewModel.allCustomers,
                                                allPartners = viewModel.professionals,
                                                allBookings = allBookings,
                                                syncState = syncState,
                                                onSwitchToUser = { targetUser ->
                                                    viewModel.adminSwitchToUser(targetUser)
                                                },
                                                onAdvanceBookingStatus = { id, st ->
                                                    viewModel.advanceBookingStatus(id, st)
                                                },
                                                onSyncClick = { viewModel.triggerSupabaseSync() },
                                                onLogout = {
                                                    viewModel.logout()
                                                    currentScreen = AppScreen.Login
                                                },
                                                isDarkTheme = isDarkTheme,
                                                onToggleTheme = onToggleTheme
                                            )
                                        }
                                        UserRole.PROFESSIONAL -> {
                                            when (currentPartnerTab) {
                                                PartnerNavTab.DUTY_JOBS -> {
                                                    PartnerJobsScreen(
                                                        partnerProfile = currentUser,
                                                        bookings = displayedBookings,
                                                        onAcceptJob = { booking, cb ->
                                                            viewModel.acceptJob(booking.id, cb)
                                                        },
                                                        onOpenJob = { bookingId ->
                                                            currentScreen = AppScreen.PartnerJobDetail(bookingId)
                                                        },
                                                        onSyncClick = { viewModel.triggerSupabaseSync() },
                                                        onOpenChat = { booking ->
                                                            chatViewModel.openThreadForBooking(
                                                                bookingId = booking.id,
                                                                bookingCode = booking.bookingCode,
                                                                serviceName = booking.serviceName,
                                                                onError = { error ->
                                                                    android.widget.Toast.makeText(appContext, error, android.widget.Toast.LENGTH_SHORT).show()
                                                                }
                                                            ) { convId ->
                                                                currentScreen = AppScreen.ChatThread(convId)
                                                            }
                                                        },
                                                        onViewAllBookings = {
                                                            currentPartnerTab = PartnerNavTab.BOOKINGS
                                                        },
                                                        onNotificationsClick = {
                                                            currentScreen = AppScreen.Notifications
                                                        },
                                                        unreadNotificationsCount = unreadPartnerNotificationsCount,
                                                        unreadByBookingId = unreadByBookingId,
                                                        statusUpdateErrorFlow = viewModel.statusUpdateError
                                                    )
                                                }
                                                PartnerNavTab.BOOKINGS -> {
                                                    PartnerBookingsScreen(
                                                        bookings = displayedBookings,
                                                        onAdvanceStatus = { id, st -> viewModel.advanceBookingStatus(id, st) },
                                                        onAcceptJob = { bk, onRes -> viewModel.acceptJob(bk.id, onRes) },
                                                        onOpenChat = { bk ->
                                                            chatViewModel.openThreadForBooking(
                                                                bookingId = bk.id,
                                                                bookingCode = bk.bookingCode,
                                                                serviceName = bk.serviceName,
                                                                onError = { error ->
                                                                    android.widget.Toast.makeText(appContext, error, android.widget.Toast.LENGTH_SHORT).show()
                                                                }
                                                            ) { convId ->
                                                                currentScreen = AppScreen.ChatThread(convId)
                                                            }
                                                        },
                                                        onCollectPayment = { bookingId ->
                                                            currentScreen = AppScreen.PaymentCollection(bookingId)
                                                        },
                                                        onBackClick = {
                                                            currentPartnerTab = PartnerNavTab.DUTY_JOBS
                                                        },
                                                        unreadByBookingId = unreadByBookingId
                                                    )
                                                }
                                                PartnerNavTab.EARNINGS -> {
                                                    PartnerEarningsScreen(
                                                        bookings = displayedBookings,
                                                        reviews = displayedReviews,
                                                        onBackClick = { currentPartnerTab = PartnerNavTab.DUTY_JOBS },
                                                        onViewAllSettlementsClick = {
                                                            currentScreen = AppScreen.PartnerSettlementHistory
                                                        }
                                                    )
                                                }
                                                PartnerNavTab.TOOLKIT -> {
                                                    PartnerToolkitScreen(
                                                        onBackClick = { currentPartnerTab = PartnerNavTab.DUTY_JOBS }
                                                    )
                                                }
                                                PartnerNavTab.PROFILE -> {
                                                    ProfileScreen(
                                                        userProfile = currentUser,
                                                        savedAddresses = displayedAddresses,
                                                        allBookings = displayedBookings,
                                                        allReviews = displayedReviews,
                                                        syncState = syncState,
                                                        onSyncClick = { viewModel.triggerSupabaseSync() },
                                                        onUpdateProfile = { name, phone, email ->
                                                            viewModel.updateUserProfile(name, phone, email)
                                                        },
                                                        onMyBookingsClick = {
                                                            currentPartnerTab = PartnerNavTab.BOOKINGS
                                                        },
                                                        onSwitchRole = { role -> viewModel.switchRole(role) },
                                                        onLocationClick = { showLocationPicker = true },
                                                        onDeleteAddress = { id -> viewModel.deleteAddress(id) },
                                                        onAddNewAddress = { title, fullAddr, loc, landmark ->
                                                            viewModel.addAddress(title, fullAddr, loc, landmark)
                                                        },
                                                        onLogout = {
                                                            viewModel.logout()
                                                            currentScreen = AppScreen.Login
                                                        },
                                                        isDarkTheme = isDarkTheme,
                                                        selectedColor = selectedColor,
                                                        appTheme = appTheme,
                                                        onToggleTheme = onToggleTheme,
                                                        onSetTheme = onSetTheme,
                                                        onSetColor = onSetColor
                                                    )
                                                }
                                            }
                                        }
                                        UserRole.CUSTOMER -> {
                                            val customerTabs = remember { listOf(ServoraNavTab.HOME, ServoraNavTab.SERVICES, ServoraNavTab.SEARCH, ServoraNavTab.BOOKINGS, ServoraNavTab.PROFILE) }
                                            AnimatedContent(
                                                targetState = currentTab,
                                                transitionSpec = {
                                                    val targetIdx = customerTabs.indexOf(targetState)
                                                    val initialIdx = customerTabs.indexOf(initialState)
                                                    if (targetIdx > initialIdx) {
                                                        (slideInHorizontally(animationSpec = tween(220)) { it } + fadeIn(tween(200)))
                                                            .togetherWith(slideOutHorizontally(animationSpec = tween(220)) { -it } + fadeOut(tween(180)))
                                                    } else {
                                                        (slideInHorizontally(animationSpec = tween(220)) { -it } + fadeIn(tween(200)))
                                                            .togetherWith(slideOutHorizontally(animationSpec = tween(220)) { it } + fadeOut(tween(180)))
                                                    }
                                                },
                                                label = "CustomerTabTransition"
                                            ) { tab ->
                                                when (tab) {
                                                    ServoraNavTab.HOME -> {
                                                        HomeScreen(
                                                            categories = viewModel.categories,
                                                            popularServices = viewModel.services,
                                                            reviews = allReviews,
                                                            activeBooking = activeBooking,
                                                            selectedCity = currentUser.city,
                                                            selectedLocality = currentUser.locality,
                                                            onCategoryClick = { cat ->
                                                                viewModel.selectCategory(cat.id)
                                                                currentTab = ServoraNavTab.SERVICES
                                                            },
                                                            onSeeAllServicesClick = {
                                                                viewModel.selectCategory(null)
                                                                viewModel.setSearchQuery("")
                                                                currentTab = ServoraNavTab.SERVICES
                                                            },
                                                            onServiceClick = { srv ->
                                                                currentScreen = AppScreen.ServiceDetail(srv)
                                                            },
                                                            onBookService = { srv ->
                                                                viewModel.addToCart(srv)
                                                            },
                                                            onTrackBookingClick = { bookingId ->
                                                                currentScreen = AppScreen.BookingTracking(bookingId)
                                                            },
                                                            onSearchClick = { currentTab = ServoraNavTab.SEARCH },
                                                            onLocationClick = { showLocationPicker = true },
                                                            onNotificationsClick = { currentScreen = AppScreen.Notifications },
                                                            onBecomePartnerClick = {
                                                                viewModel.switchRole(UserRole.PROFESSIONAL)
                                                                currentPartnerTab = PartnerNavTab.DUTY_JOBS
                                                            }
                                                        )
                                                    }
                                                    ServoraNavTab.SERVICES -> {
                                                        ExploreScreen(
                                                            categories = viewModel.categories,
                                                            services = filteredServices,
                                                            selectedCategoryId = selectedCategoryId,
                                                            searchQuery = searchQuery,
                                                            initialSearchExpanded = searchQuery.isNotEmpty(),
                                                            autoFocusSearch = false,
                                                            onCategorySelect = { catId -> viewModel.selectCategory(catId) },
                                                            onSearchChange = { q -> viewModel.setSearchQuery(q) },
                                                            onServiceClick = { srv ->
                                                                currentScreen = AppScreen.ServiceDetail(srv)
                                                            },
                                                            onBookService = { srv ->
                                                                viewModel.addToCart(srv)
                                                            },
                                                            onBackClick = {
                                                                currentTab = ServoraNavTab.HOME
                                                            }
                                                        )
                                                    }
                                                    ServoraNavTab.SEARCH -> {
                                                        ExploreScreen(
                                                            categories = viewModel.categories,
                                                            services = filteredServices,
                                                            selectedCategoryId = selectedCategoryId,
                                                            searchQuery = searchQuery,
                                                            initialSearchExpanded = true,
                                                            autoFocusSearch = true,
                                                            onCategorySelect = { catId -> viewModel.selectCategory(catId) },
                                                            onSearchChange = { q -> viewModel.setSearchQuery(q) },
                                                            onServiceClick = { srv ->
                                                                currentScreen = AppScreen.ServiceDetail(srv)
                                                            },
                                                            onBookService = { srv ->
                                                                viewModel.addToCart(srv)
                                                            },
                                                            onBackClick = {
                                                                currentTab = ServoraNavTab.HOME
                                                            }
                                                        )
                                                    }
                                                    ServoraNavTab.BOOKINGS -> {
                                                        BookingsListScreen(
                                                            bookings = displayedBookings,
                                                            onSelectBooking = { booking ->
                                                                currentScreen = AppScreen.BookingTracking(booking.id)
                                                            },
                                                            onBookAgain = { srvId ->
                                                                val srv = viewModel.services.find { it.id == srvId } ?: viewModel.services.first()
                                                                viewModel.startBooking(srv)
                                                                currentScreen = AppScreen.BookingFlow(srv, srv.packages.firstOrNull())
                                                            },
                                                            onExploreClick = {
                                                                currentTab = ServoraNavTab.SERVICES
                                                            },
                                                            onBackClick = null,
                                                            onOpenChat = { booking ->
                                                                chatViewModel.openThreadForBooking(
                                                                    bookingId = booking.id,
                                                                    bookingCode = booking.bookingCode,
                                                                    serviceName = booking.serviceName,
                                                                    onError = { error ->
                                                                        android.widget.Toast.makeText(appContext, error, android.widget.Toast.LENGTH_SHORT).show()
                                                                    }
                                                                ) { convId ->
                                                                    currentScreen = AppScreen.ChatThread(convId)
                                                                }
                                                            },
                                                            unreadByBookingId = unreadByBookingId
                                                        )
                                                    }
                                                    ServoraNavTab.PROFILE -> {
                                                        ProfileScreen(
                                                            userProfile = currentUser,
                                                            savedAddresses = displayedAddresses,
                                                            allBookings = displayedBookings,
                                                            allReviews = allReviews,
                                                            syncState = syncState,
                                                            onSyncClick = { viewModel.triggerSupabaseSync() },
                                                            onUpdateProfile = { name, phone, email ->
                                                                viewModel.updateUserProfile(name, phone, email)
                                                            },
                                                            onMyBookingsClick = {
                                                                currentTab = ServoraNavTab.BOOKINGS
                                                            },
                                                            onSwitchRole = { role -> viewModel.switchRole(role) },
                                                            onLocationClick = { showLocationPicker = true },
                                                            onDeleteAddress = { id -> viewModel.deleteAddress(id) },
                                                            onAddNewAddress = { title, fullAddr, loc, landmark ->
                                                                viewModel.addAddress(title, fullAddr, loc, landmark)
                                                            },
                                                            onLogout = {
                                                                viewModel.logout()
                                                                currentScreen = AppScreen.Login
                                                            },
                                                            isDarkTheme = isDarkTheme,
                                                            selectedColor = selectedColor,
                                                            appTheme = appTheme,
                                                            onToggleTheme = onToggleTheme,
                                                            onSetTheme = onSetTheme,
                                                            onSetColor = onSetColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is AppScreen.MyBookings -> {
                            BookingsListScreen(
                                bookings = displayedBookings,
                                onSelectBooking = { booking ->
                                    if (currentUser.role == UserRole.PROFESSIONAL) {
                                        currentScreen = AppScreen.PartnerBookings
                                    } else {
                                        currentScreen = AppScreen.BookingTracking(booking.id)
                                    }
                                },
                                onBookAgain = { srvId ->
                                    val srv = viewModel.services.find { it.id == srvId } ?: viewModel.services.first()
                                    viewModel.startBooking(srv)
                                    currentScreen = AppScreen.BookingFlow(srv, srv.packages.firstOrNull())
                                },
                                onExploreClick = {
                                    currentScreen = AppScreen.MainTabs
                                    if (currentUser.role == UserRole.PROFESSIONAL) {
                                        currentPartnerTab = PartnerNavTab.DUTY_JOBS
                                    } else {
                                        currentTab = ServoraNavTab.SERVICES
                                    }
                                },
                                onBackClick = {
                                    currentScreen = AppScreen.MainTabs
                                },
                                onOpenChat = { booking ->
                                    chatViewModel.openThreadForBooking(
                                        bookingId = booking.id,
                                        bookingCode = booking.bookingCode,
                                        serviceName = booking.serviceName,
                                        onError = { error ->
                                            android.widget.Toast.makeText(appContext, error, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    ) { convId ->
                                        currentScreen = AppScreen.ChatThread(convId)
                                    }
                                },
                                unreadByBookingId = unreadByBookingId
                            )
                        }
                        is AppScreen.PartnerBookings -> {
                            PartnerBookingsScreen(
                                bookings = displayedBookings,
                                onAdvanceStatus = { id, st -> viewModel.advanceBookingStatus(id, st) },
                                onAcceptJob = { bk, onRes -> viewModel.acceptJob(bk.id, onRes) },
                                onOpenChat = { bk ->
                                    chatViewModel.openThreadForBooking(
                                        bookingId = bk.id,
                                        bookingCode = bk.bookingCode,
                                        serviceName = bk.serviceName,
                                        onError = { error ->
                                            android.widget.Toast.makeText(appContext, error, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    ) { convId ->
                                        currentScreen = AppScreen.ChatThread(convId)
                                    }
                                },
                                onCollectPayment = { bookingId ->
                                    currentScreen = AppScreen.PaymentCollection(bookingId)
                                },
                                onBackClick = {
                                    currentScreen = AppScreen.MainTabs
                                },
                                unreadByBookingId = unreadByBookingId
                            )
                        }
                        is AppScreen.ServiceDetail -> {
                            val pro = viewModel.professionals.find { it.id == screen.service.recommendedProId }
                                ?: viewModel.professionals.firstOrNull()
                            val srvReviews = allReviews.filter { it.serviceId == screen.service.id }

                            ServiceDetailScreen(
                                service = screen.service,
                                assignedPro = pro,
                                reviews = srvReviews.ifEmpty { allReviews.take(3) },
                                onBackClick = { currentScreen = AppScreen.MainTabs },
                                onBookPackage = { srv, pkg ->
                                    viewModel.startBooking(srv, pkg)
                                    currentScreen = AppScreen.BookingFlow(srv, pkg)
                                }
                            )
                        }
                        is AppScreen.BookingFlow -> {
                            BookingFlowScreen(
                                service = screen.service,
                                selectedPackage = screen.pkg ?: bookingDraft.selectedPackage,
                                userProfile = currentUser,
                                savedAddresses = displayedAddresses,
                                availableOffers = viewModel.offers,
                                appliedOffer = bookingDraft.appliedPromo,
                                selectedAddress = bookingDraft.selectedAddress,
                                selectedDate = bookingDraft.selectedDate,
                                selectedTimeSlot = bookingDraft.selectedTimeSlot,
                                paymentMethod = bookingDraft.paymentMethod,
                                onDateChange = { d -> viewModel.updateBookingDraft(date = d) },
                                onTimeSlotChange = { s -> viewModel.updateBookingDraft(timeSlot = s) },
                                onAddressChange = { a -> viewModel.updateBookingDraft(address = a) },
                                onAddNewAddress = { title, addr, loc, landmark ->
                                    viewModel.addAddress(title, addr, loc, landmark)
                                },
                                onApplyPromo = { code -> viewModel.applyPromoCode(code) },
                                onRemovePromo = { viewModel.removePromoCode() },
                                onPaymentMethodChange = { p -> viewModel.updateBookingDraft(paymentMethod = p) },
                                onConfirmBooking = { total, discount, pkgName, notes ->
                                    viewModel.confirmBooking(
                                        customTotal = total,
                                        customDiscount = discount,
                                        customPackageName = pkgName,
                                        customNotes = notes
                                    ) { newId ->
                                        chatViewModel.loadConversations()
                                        currentScreen = AppScreen.BookingTracking(newId)
                                    }
                                },
                                onBackClick = { currentScreen = AppScreen.MainTabs },
                                onAddMoreServices = {
                                    currentScreen = AppScreen.MainTabs
                                    currentTab = ServoraNavTab.SERVICES
                                }
                            )
                        }
                        is AppScreen.BookingTracking -> {
                            val booking = allBookings.find { it.id == screen.bookingId }
                                ?: viewModel.confirmedBooking.value
                                ?: allBookings.firstOrNull()

                            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                            LaunchedEffect(booking?.id) {
                                val id = booking?.id ?: return@LaunchedEffect
                                lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                                    while (isActive) {
                                        val current = allBookings.find { it.id == id }
                                        if (current == null || current.status == BookingStatus.COMPLETED || current.status == BookingStatus.CANCELLED) {
                                            break
                                        }
                                        viewModel.pollBookingStatusOnce(id)
                                        kotlinx.coroutines.delay(12_000)
                                    }
                                }
                            }

                            if (booking != null) {
                                val pro = viewModel.professionals.find { it.id == booking.professionalId }
                                    ?: viewModel.professionals.firstOrNull()

                                BookingConfirmationScreen(
                                    booking = booking,
                                    professional = pro,
                                    onAdvanceStatus = { id, st -> viewModel.advanceBookingStatus(id, st) },
                                    onCancelBooking = { id, reason, feedback -> viewModel.cancelBooking(id, reason, feedback) },
                                    onSubmitReview = { srvId, srvName, proName, rating, comment, tags ->
                                        viewModel.submitReview(srvId, srvName, proName, rating, comment, tags)
                                    },
                                    onBackToHome = {
                                        currentScreen = AppScreen.MainTabs
                                        currentTab = ServoraNavTab.HOME
                                    },
                                    onOpenChat = { bk ->
                                        chatViewModel.openThreadForBooking(
                                            bookingId = bk.id,
                                            bookingCode = bk.bookingCode,
                                            serviceName = bk.serviceName,
                                            onError = { error ->
                                                android.widget.Toast.makeText(appContext, error, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        ) { convId ->
                                            currentScreen = AppScreen.ChatThread(convId)
                                        }
                                    }
                                )
                            } else {
                                currentScreen = AppScreen.MainTabs
                            }
                        }
                        is AppScreen.ChatThread -> {
                            ChatThreadScreen(
                                conversationId = screen.conversationId,
                                chatViewModel = chatViewModel,
                                isAdminView = screen.isAdminView || impersonatingAdminProfile != null || currentUser.role == UserRole.ADMIN,
                                isPartner = currentUser.role == UserRole.PROFESSIONAL,
                                onBack = {
                                    currentScreen = AppScreen.MainTabs
                                }
                            )
                        }
                        is AppScreen.PartnerSettlementHistory -> {
                            val completedBookings = displayedBookings.filter { it.status == BookingStatus.COMPLETED }
                            PartnerSettlementHistoryScreen(
                                completedBookings = completedBookings,
                                onBackClick = {
                                    currentScreen = AppScreen.MainTabs
                                }
                            )
                        }
                        is AppScreen.PartnerJobDetail -> {
                            PartnerJobDetailScreen(
                                bookingId = screen.bookingId,
                                bookings = displayedBookings,
                                onBackClick = {
                                    currentPartnerTab = PartnerNavTab.DUTY_JOBS
                                    currentScreen = AppScreen.MainTabs
                                },
                                onAdvanceStatus = { id, st -> viewModel.advanceBookingStatus(id, st) },
                                onAdvanceStatusWithCallback = { id, st, cb -> viewModel.advanceBookingStatus(id, st, cb) },
                                onAcceptJob = { bk, cb -> viewModel.acceptJob(bk.id, cb) },
                                onPartnerCancelJob = { id, code, note, cb -> viewModel.partnerCancelJob(id, code, note, cb) },
                                onOpenChat = { bk ->
                                    chatViewModel.openThreadForBooking(
                                        bookingId = bk.id,
                                        bookingCode = bk.bookingCode,
                                        serviceName = bk.serviceName,
                                        onError = { error ->
                                            android.widget.Toast.makeText(appContext, error, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    ) { convId ->
                                        currentScreen = AppScreen.ChatThread(convId)
                                    }
                                },
                                onCollectPayment = { bookingId ->
                                    currentScreen = AppScreen.PaymentCollection(bookingId)
                                },
                                unreadByBookingId = unreadByBookingId,
                                statusUpdateErrorFlow = viewModel.statusUpdateError
                            )
                        }
                        is AppScreen.PaymentCollection -> {
                            PaymentCollectionScreen(
                                bookingId = screen.bookingId,
                                viewModel = paymentViewModel,
                                onBackClick = {
                                    if (currentUser.role == UserRole.PROFESSIONAL) {
                                        currentScreen = AppScreen.PartnerJobDetail(screen.bookingId)
                                    } else {
                                        currentScreen = AppScreen.MainTabs
                                    }
                                },
                                onPaymentCompleted = {
                                    if (currentUser.role == UserRole.PROFESSIONAL) {
                                        currentScreen = AppScreen.PartnerJobDetail(screen.bookingId)
                                    } else {
                                        currentScreen = AppScreen.MainTabs
                                    }
                                    viewModel.triggerSupabaseSync()
                                }
                            )
                        }
                        is AppScreen.Notifications -> {
                            NotificationsScreen(
                                notifications = notifications,
                                userRole = currentUser.role,
                                onNotificationClick = { notif ->
                                    viewModel.markNotificationAsRead(notif.id)
                                    if (notif.bookingId != null) {
                                        if (notif.type == NotificationType.NEW_MESSAGE) {
                                            chatViewModel.openThreadForBooking(
                                                bookingId = notif.bookingId,
                                                onError = { err ->
                                                    android.widget.Toast.makeText(appContext, err, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            ) { convId ->
                                                currentScreen = AppScreen.ChatThread(convId)
                                            }
                                        } else if (currentUser.role == UserRole.PROFESSIONAL) {
                                            currentScreen = AppScreen.PartnerJobDetail(notif.bookingId)
                                        } else {
                                            currentScreen = AppScreen.BookingTracking(notif.bookingId)
                                        }
                                    } else {
                                        currentScreen = AppScreen.MainTabs
                                    }
                                },
                                onMarkAllAsRead = {
                                    viewModel.markAllNotificationsAsRead(currentUser.role)
                                },
                                onClearAll = {
                                    viewModel.clearAllNotifications(currentUser.role)
                                },
                                onDeleteNotification = { notifId ->
                                    viewModel.deleteNotification(notifId)
                                },
                                onBackClick = {
                                    currentScreen = AppScreen.MainTabs
                                }
                            )
                        }
                    }
                }
            }
        }

        // Location Modal BottomSheet
        if (showLocationPicker) {
            LocationPickerModal(
                currentCity = currentUser.city,
                currentLocality = currentUser.locality,
                supportedCities = viewModel.supportedCities,
                agraLocalities = viewModel.agraLocalities,
                onDismiss = { showLocationPicker = false },
                onLocationSelected = { city, locality ->
                    viewModel.setLocation(city, locality)
                }
            )
        }

        // Floating Cart Bar (Customer role, in Home/Services/Search tabs when cart has items)
        val shouldShowCart = isMainTabsActive &&
            currentUser.role == UserRole.CUSTOMER &&
            (currentTab == ServoraNavTab.HOME || currentTab == ServoraNavTab.SERVICES || currentTab == ServoraNavTab.SEARCH) &&
            cartTotalCount > 0 &&
            isCartVisible

        AnimatedVisibility(
            visible = shouldShowCart,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(240)) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(240)) + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (isBottomNavVisible) 76.dp else 16.dp)
        ) {
            FloatingCartBar(
                itemCount = cartTotalCount,
                totalPrice = cartTotalPrice,
                totalSavings = cartTotalSavings,
                onViewCartClick = {
                    val firstItem = cartItems.firstOrNull()
                    if (firstItem != null) {
                        viewModel.startBooking(firstItem.service, firstItem.selectedPackage)
                        currentScreen = AppScreen.BookingFlow(firstItem.service, firstItem.selectedPackage)
                    }
                },
                onDismiss = {
                    isCartVisible = false
                }
            )
        }

        // Incoming Message Heads-up Banner (R2)
        AnimatedVisibility(
            visible = incomingBanner != null && (currentScreen !is AppScreen.ChatThread || (currentScreen as AppScreen.ChatThread).conversationId != incomingBanner?.conversationId),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .stableStatusBarsPadding()
                .padding(top = 8.dp)
        ) {
            incomingBanner?.let { banner ->
                IncomingMessageBanner(
                    bannerData = banner,
                    onClick = {
                        chatViewModel.dismissBanner()
                        currentScreen = AppScreen.ChatThread(banner.conversationId)
                    },
                    onDismiss = {
                        chatViewModel.dismissBanner()
                    }
                )
            }
        }
    }
}
