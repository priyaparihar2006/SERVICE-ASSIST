package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ServoraDatabase
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CustomerReview
import com.example.data.model.Offer
import com.example.data.model.Professional
import com.example.data.model.SavedAddress
import com.example.data.model.ServiceCategory
import com.example.data.model.ServiceItem
import com.example.data.model.ServicePackage
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.ServoraRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class BookingDraft(
    val service: ServiceItem? = null,
    val selectedPackage: ServicePackage? = null,
    val selectedDate: String = "Today",
    val selectedTimeSlot: String = "02:00 PM",
    val selectedAddress: SavedAddress? = null,
    val appliedPromo: Offer? = null,
    val paymentMethod: String = "Cash after service", // "UPI (Google Pay / PhonePe)", "Card", "Cash after service"
    val specialNotes: String = ""
)

class ServoraViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ServoraDatabase.getDatabase(application, viewModelScope)
    val repository = ServoraRepository(
        bookingDao = database.bookingDao(),
        addressDao = database.addressDao(),
        reviewDao = database.reviewDao(),
        userDao = database.userDao()
    )

    init {
        // Ensure default data is present if fresh install
        viewModelScope.launch {
            ServoraDatabase.populateInitialData(database)
            repository.syncWithSupabase()
        }
    }

    val syncState = repository.syncState

    private val _statusUpdateError = MutableSharedFlow<String>()
    val statusUpdateError: SharedFlow<String> = _statusUpdateError.asSharedFlow()

    fun triggerSupabaseSync() {
        viewModelScope.launch {
            repository.syncWithSupabase()
        }
    }

    // Observables from Room
    val currentUser: StateFlow<UserProfile> = repository.currentUser
        .map { it ?: repository.demoCustomer }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.demoCustomer
        )

    val isLoggedIn: StateFlow<Boolean> = repository.currentUser
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val demoCustomer = repository.demoCustomer
    val demoPartner = repository.demoPartner
    val demoAdmin = repository.demoAdmin

    fun loginWithProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.loginUser(profile)
        }
    }

    fun loginWithCredentials(role: UserRole, phoneOrEmail: String, name: String? = null) {
        val cleanInput = phoneOrEmail.trim()
        val isEmail = cleanInput.contains("@")
        val generatedName = name?.takeIf { it.isNotBlank() } ?: when (role) {
            UserRole.CUSTOMER -> if (isEmail) cleanInput.substringBefore("@").replaceFirstChar { it.uppercase() } else "Customer ${cleanInput.takeLast(4)}"
            UserRole.PROFESSIONAL -> "Partner ${cleanInput.takeLast(4)}"
            UserRole.ADMIN -> "Operations Admin"
        }
        val targetProfile = if (role == UserRole.PROFESSIONAL && (cleanInput == demoPartner.phone || cleanInput == demoPartner.email || cleanInput.contains("rajesh", ignoreCase = true))) {
            demoPartner
        } else if (role == UserRole.CUSTOMER && (cleanInput == demoCustomer.phone || cleanInput == demoCustomer.email || cleanInput.contains("priya", ignoreCase = true))) {
            demoCustomer
        } else if (role == UserRole.ADMIN && (cleanInput == demoAdmin.phone || cleanInput == demoAdmin.email || cleanInput.contains("admin", ignoreCase = true))) {
            demoAdmin
        } else {
            val idPrefix = if (role == UserRole.PROFESSIONAL) "pro_" else "user_"
            UserProfile(
                id = idPrefix + (if (isEmail) cleanInput.substringBefore("@").lowercase() else cleanInput.filter { it.isDigit() }.ifEmpty { "12345" }),
                name = generatedName,
                phone = if (isEmail) "+91 98765 43210" else if (cleanInput.startsWith("+91")) cleanInput else "+91 $cleanInput",
                email = if (isEmail) cleanInput else "${cleanInput.filter { it.isDigit() }.ifEmpty { "user" }}@serviceassist.in",
                city = "Agra",
                locality = "Taj Nagri Phase 2",
                role = role
            )
        }
        viewModelScope.launch {
            repository.loginUser(targetProfile)
        }
    }

    val allCustomers: List<UserProfile> = listOf(
        UserProfile(
            id = "user_priya_1",
            name = "Priya Sharma",
            phone = "+91 98765 43210",
            email = "priya.sharma@example.com",
            city = "Agra",
            locality = "Taj Nagri Phase 2",
            role = UserRole.CUSTOMER
        ),
        UserProfile(
            id = "user_amit_verma",
            name = "Amit Verma",
            phone = "+91 98111 22334",
            email = "amit.verma@example.com",
            city = "Agra",
            locality = "Dayalbagh",
            role = UserRole.CUSTOMER
        ),
        UserProfile(
            id = "user_pooja_ag",
            name = "Pooja Agarwal",
            phone = "+91 94125 66778",
            email = "pooja.ag@example.com",
            city = "Agra",
            locality = "Kamla Nagar",
            role = UserRole.CUSTOMER
        ),
        UserProfile(
            id = "user_rohan_g",
            name = "Rohan Gupta",
            phone = "+91 97580 99887",
            email = "rohan.gupta@example.com",
            city = "Agra",
            locality = "Sanjay Place",
            role = UserRole.CUSTOMER
        )
    )

    private val _impersonatingAdminProfile = MutableStateFlow<UserProfile?>(null)
    val impersonatingAdminProfile: StateFlow<UserProfile?> = _impersonatingAdminProfile.asStateFlow()

    fun adminSwitchToUser(profile: UserProfile) {
        val current = currentUser.value
        if (current.role == UserRole.ADMIN && _impersonatingAdminProfile.value == null) {
            _impersonatingAdminProfile.value = current
        }
        viewModelScope.launch {
            repository.loginUser(profile)
        }
    }

    fun adminReturnToDashboard() {
        val admin = _impersonatingAdminProfile.value ?: demoAdmin
        _impersonatingAdminProfile.value = null
        viewModelScope.launch {
            repository.loginUser(admin)
        }
    }

    fun logout() {
        _impersonatingAdminProfile.value = null
        viewModelScope.launch {
            repository.logoutUser()
        }
    }

    val allBookings: StateFlow<List<Booking>> = repository.allBookings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedAddresses: StateFlow<List<SavedAddress>> = repository.savedAddresses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allReviews: StateFlow<List<CustomerReview>> = repository.allReviews
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val displayedBookings: StateFlow<List<Booking>> = combine(
        currentUser,
        allBookings
    ) { user, bookings ->
        when (user.role) {
            UserRole.ADMIN -> bookings
            UserRole.CUSTOMER -> bookings.filter {
                it.customerId == user.id ||
                it.customerName.equals(user.name, ignoreCase = true) ||
                (user.phone.isNotBlank() && it.customerPhone.filter { ch -> ch.isDigit() }.endsWith(user.phone.filter { ch -> ch.isDigit() }.takeLast(10)))
            }
            UserRole.PROFESSIONAL -> bookings.filter {
                it.professionalId == user.id ||
                (user.id == "user_rajesh_pro" && it.professionalId == "pro_rajesh_1") ||
                it.professionalId.contains(user.name.take(4), ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeBooking: StateFlow<Booking?> = displayedBookings
        .map { list ->
            list.firstOrNull { it.status != BookingStatus.COMPLETED && it.status != BookingStatus.CANCELLED }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val displayedAddresses: StateFlow<List<SavedAddress>> = combine(
        currentUser,
        savedAddresses
    ) { user, addresses ->
        when (user.role) {
            UserRole.ADMIN -> addresses
            UserRole.CUSTOMER, UserRole.PROFESSIONAL -> {
                val userSpecific = addresses.filter { it.userId == user.id }
                if (userSpecific.isEmpty()) {
                    addresses.filter { it.userId == user.id || it.userId == "user_priya_1" }
                } else {
                    userSpecific
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val displayedReviews: StateFlow<List<CustomerReview>> = combine(
        currentUser,
        allReviews
    ) { user, reviews ->
        when (user.role) {
            UserRole.ADMIN -> reviews
            UserRole.CUSTOMER -> reviews
            UserRole.PROFESSIONAL -> {
                val proId = if (user.id == "user_rajesh_pro") "Rajesh" else user.name
                reviews.filter {
                    it.professionalName.contains(proId, ignoreCase = true) ||
                    it.professionalName.contains(user.name.take(4), ignoreCase = true)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Catalog state
    val categories: List<ServiceCategory> = repository.categories
    val services: List<ServiceItem> = repository.services
    val professionals: List<Professional> = repository.professionals
    val offers: List<Offer> = repository.offers
    val supportedCities: List<String> = repository.supportedCities
    val agraLocalities: List<String> = repository.agraLocalities

    // Search & Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    val filteredServices = combine(
        _searchQuery,
        _selectedCategoryId
    ) { query, categoryId ->
        var list = services
        if (!categoryId.isNullOrBlank()) {
            list = list.filter { it.categoryId == categoryId }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.whatIsIncluded.any { inc -> inc.lowercase().contains(q) }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), services)

    // Booking Flow State
    private val _bookingDraft = MutableStateFlow(BookingDraft())
    val bookingDraft = _bookingDraft.asStateFlow()

    private val _confirmedBooking = MutableStateFlow<Booking?>(null)
    val confirmedBooking = _confirmedBooking.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun startBooking(service: ServiceItem, defaultPackage: ServicePackage? = null) {
        val pkg = defaultPackage ?: service.packages.firstOrNull()
        val defaultAddr = displayedAddresses.value.firstOrNull { it.isDefault } ?: displayedAddresses.value.firstOrNull()
        _bookingDraft.value = BookingDraft(
            service = service,
            selectedPackage = pkg,
            selectedDate = "Today",
            selectedTimeSlot = "02:00 PM",
            selectedAddress = defaultAddr,
            appliedPromo = null,
            paymentMethod = "Cash after service"
        )
    }

    fun updateBookingDraft(
        pkg: ServicePackage? = _bookingDraft.value.selectedPackage,
        date: String = _bookingDraft.value.selectedDate,
        timeSlot: String = _bookingDraft.value.selectedTimeSlot,
        address: SavedAddress? = _bookingDraft.value.selectedAddress,
        promo: Offer? = _bookingDraft.value.appliedPromo,
        paymentMethod: String = _bookingDraft.value.paymentMethod,
        notes: String = _bookingDraft.value.specialNotes
    ) {
        _bookingDraft.value = _bookingDraft.value.copy(
            selectedPackage = pkg,
            selectedDate = date,
            selectedTimeSlot = timeSlot,
            selectedAddress = address,
            appliedPromo = promo,
            paymentMethod = paymentMethod,
            specialNotes = notes
        )
    }

    fun applyPromoCode(code: String): Boolean {
        val found = offers.find { it.code.equals(code.trim(), ignoreCase = true) }
        if (found != null) {
            _bookingDraft.value = _bookingDraft.value.copy(appliedPromo = found)
            return true
        }
        return false
    }

    fun removePromoCode() {
        _bookingDraft.value = _bookingDraft.value.copy(appliedPromo = null)
    }

    fun calculateTotalAmount(): Triple<Int, Int, Int> {
        val draft = _bookingDraft.value
        val basePrice = draft.selectedPackage?.price ?: draft.service?.startingPrice ?: 499
        var discount = 0
        draft.appliedPromo?.let { offer ->
            if (offer.percentageDiscount > 0) {
                discount = (basePrice * offer.percentageDiscount) / 100
            } else if (offer.flatDiscount > 0) {
                discount = offer.flatDiscount
            }
        }
        val finalPrice = (basePrice - discount).coerceAtLeast(49)
        return Triple(basePrice, discount, finalPrice)
    }

    fun confirmBooking(
        customTotal: Int? = null,
        customDiscount: Int? = null,
        customPackageName: String? = null,
        customNotes: String? = null,
        onSuccess: (Long) -> Unit
    ) {
        val draft = _bookingDraft.value
        val service = draft.service ?: return
        val pkg = draft.selectedPackage
        val (_, defaultDiscount, defaultTotal) = calculateTotalAmount()
        val total = customTotal ?: defaultTotal
        val discount = customDiscount ?: defaultDiscount
        val packageName = customPackageName ?: (pkg?.name ?: "Standard Service")
        val combinedNotes = buildString {
            if (draft.specialNotes.isNotBlank()) append(draft.specialNotes)
            if (!customNotes.isNullOrBlank()) {
                if (isNotEmpty()) append(" | ")
                append(customNotes)
            }
        }
        val address = draft.selectedAddress
        val addressText = if (address != null) {
            "${address.fullAddress}, ${address.locality}, ${address.city}"
        } else {
            "Taj Nagri Phase 2, Agra"
        }

        val user = currentUser.value
        val randomCode = "SRV-" + Random.nextInt(10000, 99999)
        val randomOtp = Random.nextInt(1000, 9999).toString()

        val newBooking = Booking(
            bookingCode = randomCode,
            customerId = user.id,
            customerName = user.name,
            customerPhone = user.phone,
            serviceId = service.id,
            serviceName = service.name,
            packageName = packageName,
            scheduledDate = draft.selectedDate,
            scheduledTime = draft.selectedTimeSlot,
            addressText = addressText,
            locality = address?.locality ?: user.locality,
            city = address?.city ?: user.city,
            totalAmount = total,
            discountAmount = discount,
            promoCode = draft.appliedPromo?.code ?: "",
            paymentMethod = draft.paymentMethod,
            isPaid = false,
            status = BookingStatus.ASSIGNED,
            professionalId = service.recommendedProId.ifBlank { "pro_rajesh_1" },
            startOtp = randomOtp,
            specialNotes = combinedNotes
        )

        viewModelScope.launch {
            val id = repository.createBooking(newBooking)
            val created = newBooking.copy(id = id)
            _confirmedBooking.value = created
            onSuccess(id)
        }
    }

    fun advanceBookingStatus(bookingId: Long, currentStatus: BookingStatus, onResult: ((Boolean) -> Unit)? = null) {
        val next = when (currentStatus) {
            BookingStatus.PENDING -> BookingStatus.CONFIRMED
            BookingStatus.CONFIRMED -> BookingStatus.ASSIGNED
            BookingStatus.ASSIGNED -> BookingStatus.ON_THE_WAY
            BookingStatus.ON_THE_WAY -> BookingStatus.ARRIVED
            BookingStatus.ARRIVED -> BookingStatus.STARTED
            BookingStatus.STARTED -> BookingStatus.AWAITING_PAYMENT
            BookingStatus.AWAITING_PAYMENT -> return
            BookingStatus.COMPLETED -> BookingStatus.COMPLETED
            BookingStatus.CANCELLED -> BookingStatus.CANCELLED
        }
        viewModelScope.launch {
            val success = repository.updateBookingStatus(bookingId, next)
            if (!success) {
                _statusUpdateError.emit("Couldn't update job status to ${next.name} — please check connection and try again")
            }
            onResult?.invoke(success)
        }
    }

    suspend fun pollBookingStatusOnce(bookingId: Long): Boolean {
        return repository.refreshBookingStatus(bookingId)
    }

    fun cancelBooking(bookingId: Long, reason: String? = null, feedback: String? = null) {
        viewModelScope.launch {
            val success = repository.cancelBooking(bookingId, reason, feedback)
            if (_confirmedBooking.value?.id == bookingId) {
                _confirmedBooking.value = _confirmedBooking.value?.copy(
                    status = BookingStatus.CANCELLED,
                    cancellationReason = reason,
                    cancellationFeedback = feedback,
                    cancelledAt = System.currentTimeMillis()
                )
            }
            if (!success) {
                _statusUpdateError.emit("Couldn't sync booking cancellation with server")
            }
        }
    }

    fun submitReview(serviceId: String, serviceName: String, proName: String, rating: Float, comment: String, tags: String) {
        viewModelScope.launch {
            repository.addReview(
                CustomerReview(
                    serviceId = serviceId,
                    serviceName = serviceName,
                    professionalName = proName,
                    customerName = currentUser.value.name,
                    rating = rating,
                    comment = comment,
                    tags = tags,
                    dateText = "Just now"
                )
            )
        }
    }

    fun addAddress(title: String, addressText: String, locality: String, landmark: String) {
        val user = currentUser.value
        viewModelScope.launch {
            repository.addAddress(
                SavedAddress(
                    userId = user.id,
                    title = title,
                    fullAddress = addressText,
                    locality = locality,
                    city = user.city,
                    landmark = landmark,
                    isDefault = displayedAddresses.value.isEmpty()
                )
            )
        }
    }

    fun deleteAddress(id: Long) {
        viewModelScope.launch {
            repository.deleteAddress(id)
        }
    }

    fun switchRole(role: UserRole) {
        val targetProfile = when (role) {
            UserRole.CUSTOMER -> demoCustomer
            UserRole.PROFESSIONAL -> demoPartner
            UserRole.ADMIN -> demoAdmin
        }
        viewModelScope.launch {
            repository.loginUser(targetProfile)
        }
    }

    fun setLocation(city: String, locality: String) {
        viewModelScope.launch {
            repository.updateLocation(city, locality)
        }
    }

    fun updateUserProfile(name: String, phone: String, email: String) {
        viewModelScope.launch {
            repository.updateProfile(name, phone, email)
        }
    }
}
