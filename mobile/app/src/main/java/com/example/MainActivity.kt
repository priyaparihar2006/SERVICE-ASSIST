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
import com.example.data.model.ServiceItem
import com.example.data.model.ServicePackage
import com.example.data.model.UserRole
import com.example.ui.components.StatusBarIcons
import androidx.compose.foundation.layout.WindowInsets
import com.example.ui.components.AdminBottomNav
import com.example.ui.components.AdminNavTab
import com.example.ui.components.LocationPickerModal
import com.example.ui.components.PartnerBottomNav
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseConfig
import com.example.data.repository.FakeChatRepository
import com.example.data.repository.RemoteChatRepository
import com.example.ui.components.IncomingMessageBanner
import com.example.ui.components.PartnerNavTab
import com.example.ui.components.SearchOverlay
import com.example.ui.components.ServoraBottomNav
import com.example.ui.components.ServoraNavTab
import com.example.ui.components.ServoraTopBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import com.example.ui.screens.PaymentCollectionScreen
import com.example.ui.viewmodel.PaymentViewModel
import com.example.ui.viewmodel.PaymentViewModelFactory
import com.example.data.repository.PaymentRepository

sealed interface AppScreen {
    data object Login : AppScreen
    data object MainTabs : AppScreen
    data object MyBookings : AppScreen
    data object PartnerSettlementHistory : AppScreen
    data class ServiceDetail(val service: ServiceItem) : AppScreen
    data class BookingFlow(val service: ServiceItem, val pkg: ServicePackage?) : AppScreen
    data class BookingTracking(val bookingId: Long) : AppScreen
    data class ChatThread(val conversationId: String, val isAdminView: Boolean = false) : AppScreen
    data class PaymentCollection(val bookingId: Long) : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        setContent {
            MyApplicationTheme(darkTheme = false) {
                ServoraApp()
            }
        }
    }
}

@Composable
fun ServoraApp(
    viewModel: ServoraViewModel = viewModel()
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
        if (SupabaseConfig.isConfigured && remoteApi != null) {
            RemoteChatRepository(remoteApi, servoraDb.bookingDao())
        } else {
            FakeChatRepository()
        }
    }
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(chatRepository)
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
    val incomingBanner by chatViewModel.incomingBanner.collectAsState()

    // Observe user / session changes to switch chat repository profile and cursors
    LaunchedEffect(currentUser.id, currentUser.role, impersonatingAdminProfile != null) {
        chatViewModel.onSessionChanged(currentUser.id, currentUser.role.name)
    }

    // Always restore bottom nav visibility and status bar when switching tabs or screens
    LaunchedEffect(currentTab, currentPartnerTab, currentAdminTab, currentScreen) {
        isBottomNavVisible = true
        insetsController?.show(WindowInsetsCompat.Type.statusBars())
    }

    val nestedScrollConnection = remember(insetsController) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -8f) {
                    // Scrolling down (content moving up) -> hide bottom nav & status bar
                    isBottomNavVisible = false
                    insetsController?.hide(WindowInsetsCompat.Type.statusBars())
                } else if (available.y > 8f) {
                    // Scrolling up (content moving down) -> show bottom nav & status bar
                    isBottomNavVisible = true
                    insetsController?.show(WindowInsetsCompat.Type.statusBars())
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
            currentScreen = AppScreen.MainTabs
        }
    }

    val isMainTabsActive = currentScreen is AppScreen.MainTabs && isLoggedIn

    val isDarkHeader = when {
        showSearchOverlay -> false
        !isLoggedIn || currentScreen is AppScreen.Login -> false
        currentScreen is AppScreen.ServiceDetail -> true
        currentScreen is AppScreen.PartnerSettlementHistory -> true
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
                        PartnerNavTab.EARNINGS -> true
                        PartnerNavTab.PROFILE -> true
                        PartnerNavTab.CHATS -> false
                        PartnerNavTab.TOOLKIT -> false
                    }
                }
                UserRole.CUSTOMER -> {
                    when (currentTab) {
                        ServoraNavTab.PROFILE -> true
                        ServoraNavTab.HOME -> false
                        ServoraNavTab.SERVICES -> false
                        ServoraNavTab.CHATS -> false
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
                    !showSearchOverlay &&
                    currentUser.role == UserRole.CUSTOMER &&
                    currentTab == ServoraNavTab.HOME

                if (shouldShowCustomerTopBar) {
                    ServoraTopBar(
                        selectedCity = currentUser.city,
                        selectedLocality = currentUser.locality,
                        userRole = currentUser.role,
                        onLocationClick = { showLocationPicker = true },
                        onSearchClick = { showSearchOverlay = true },
                        isImpersonating = impersonatingAdminProfile != null,
                        onReturnToAdminClick = { viewModel.adminReturnToDashboard() }
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isMainTabsActive && !showSearchOverlay && isBottomNavVisible,
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
                                activeJobsCount = pendingPartnerJobs,
                                unreadChatsCount = unreadChatsCount
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
                                    showSearchOverlay = true
                                },
                                activeBookingsCount = activeCount,
                                unreadChatsCount = unreadChatsCount
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isMainTabsActive) innerPadding else androidx.compose.foundation.layout.PaddingValues(0.dp))
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
                                                }
                                            )
                                        }
                                        UserRole.PROFESSIONAL -> {
                                            when (currentPartnerTab) {
                                                PartnerNavTab.DUTY_JOBS -> {
                                                    PartnerJobsScreen(
                                                        partnerProfile = currentUser,
                                                        bookings = displayedBookings,
                                                        onAdvanceStatus = { bookingId, status ->
                                                            viewModel.advanceBookingStatus(bookingId, status)
                                                        },
                                                        onAdvanceStatusWithCallback = { bookingId, status, cb ->
                                                            viewModel.advanceBookingStatus(bookingId, status, cb)
                                                        },
                                                        statusUpdateErrorFlow = viewModel.statusUpdateError,
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
                                                        onCollectPayment = { bookingId ->
                                                            currentScreen = AppScreen.PaymentCollection(bookingId)
                                                        }
                                                    )
                                                }
                                                PartnerNavTab.CHATS -> {
                                                    ChatListScreen(
                                                        chatViewModel = chatViewModel,
                                                        onOpenThread = { convId ->
                                                            currentScreen = AppScreen.ChatThread(convId)
                                                        }
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
                                                        onSwitchRole = { role -> viewModel.switchRole(role) },
                                                        onLocationClick = { showLocationPicker = true },
                                                        onDeleteAddress = { id -> viewModel.deleteAddress(id) },
                                                        onAddNewAddress = { title, fullAddr, loc, landmark ->
                                                            viewModel.addAddress(title, fullAddr, loc, landmark)
                                                        },
                                                        onLogout = {
                                                            viewModel.logout()
                                                            currentScreen = AppScreen.Login
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        UserRole.CUSTOMER -> {
                                            val customerTabs = remember { listOf(ServoraNavTab.HOME, ServoraNavTab.SERVICES, ServoraNavTab.CHATS, ServoraNavTab.PROFILE) }
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
                                                                viewModel.startBooking(srv)
                                                                currentScreen = AppScreen.BookingFlow(srv, srv.packages.firstOrNull())
                                                            },
                                                            onTrackBookingClick = { bookingId ->
                                                                currentScreen = AppScreen.BookingTracking(bookingId)
                                                            },
                                                            onSearchClick = { showSearchOverlay = true },
                                                            onLocationClick = { showLocationPicker = true },
                                                            onNotificationsClick = {},
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
                                                            onCategorySelect = { catId -> viewModel.selectCategory(catId) },
                                                            onSearchChange = { q -> viewModel.setSearchQuery(q) },
                                                            onServiceClick = { srv ->
                                                                currentScreen = AppScreen.ServiceDetail(srv)
                                                            },
                                                            onBookService = { srv ->
                                                                viewModel.startBooking(srv)
                                                                currentScreen = AppScreen.BookingFlow(srv, srv.packages.firstOrNull())
                                                            }
                                                        )
                                                    }
                                                    ServoraNavTab.CHATS -> {
                                                        ChatListScreen(
                                                            chatViewModel = chatViewModel,
                                                            onOpenThread = { convId ->
                                                                currentScreen = AppScreen.ChatThread(convId)
                                                            }
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
                                                                currentScreen = AppScreen.MyBookings
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
                        is AppScreen.MyBookings -> {
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
                                    currentScreen = AppScreen.MainTabs
                                    currentTab = ServoraNavTab.SERVICES
                                },
                                onBackClick = {
                                    currentScreen = AppScreen.MainTabs
                                }
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
                        is AppScreen.PaymentCollection -> {
                            PaymentCollectionScreen(
                                bookingId = screen.bookingId,
                                viewModel = paymentViewModel,
                                onBackClick = {
                                    currentScreen = AppScreen.MainTabs
                                },
                                onPaymentCompleted = {
                                    currentScreen = AppScreen.MainTabs
                                    viewModel.triggerSupabaseSync()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Search Full Overlay
        if (showSearchOverlay) {
            SearchOverlay(
                query = searchQuery,
                onQueryChange = { q -> viewModel.setSearchQuery(q) },
                results = filteredServices,
                onClose = { showSearchOverlay = false },
                onSelectService = { srv ->
                    showSearchOverlay = false
                    currentScreen = AppScreen.ServiceDetail(srv)
                },
                onBookService = { srv ->
                    showSearchOverlay = false
                    viewModel.startBooking(srv)
                    currentScreen = AppScreen.BookingFlow(srv, srv.packages.firstOrNull())
                }
            )
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

        // Incoming Message Heads-up Banner (R2)
        AnimatedVisibility(
            visible = incomingBanner != null && (currentScreen !is AppScreen.ChatThread || (currentScreen as AppScreen.ChatThread).conversationId != incomingBanner?.conversationId),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
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
