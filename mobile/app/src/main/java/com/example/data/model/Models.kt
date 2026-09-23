package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    CUSTOMER,
    PROFESSIONAL,
    ADMIN
}

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String = "user_priya_1",
    val name: String = "Priya Sharma",
    val phone: String = "+91 98765 43210",
    val email: String = "priya.sharma@example.com",
    val city: String = "Agra",
    val locality: String = "Taj Nagri Phase 2",
    val role: UserRole = UserRole.CUSTOMER
)

@Entity(tableName = "saved_addresses")
data class SavedAddress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "user_priya_1",
    val title: String, // Home, Office, Parents
    val fullAddress: String,
    val locality: String,
    val city: String = "Agra",
    val landmark: String = "",
    val isDefault: Boolean = false
)

enum class BookingStatus(val label: String, val stepIndex: Int) {
    PENDING("Booking Requested", 0),
    CONFIRMED("Booking Confirmed", 1),
    ASSIGNED("Professional Assigned", 2),
    ON_THE_WAY("Professional on the Way", 3),
    ARRIVED("Arrived at Doorstep", 4),
    STARTED("Service in Progress", 5),
    AWAITING_PAYMENT("Awaiting Payment", 6),
    COMPLETED("Service Completed", 7),
    CANCELLED("Cancelled", -1)
}

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingCode: String,
    val customerId: String = "user_priya_1",
    val customerName: String = "Priya Sharma",
    val customerPhone: String = "+91 98765 43210",
    val serviceId: String,
    val serviceName: String,
    val packageName: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val addressText: String,
    val locality: String,
    val city: String = "Agra",
    val totalAmount: Int,
    val discountAmount: Int = 0,
    val promoCode: String = "",
    val paymentMethod: String = "Cash after service", // UPI, Card, Cash
    val isPaid: Boolean = false,
    val status: BookingStatus = BookingStatus.ASSIGNED,
    val professionalId: String = "pro_rajesh_1",
    val startOtp: String = "4829",
    val specialNotes: String = "",
    val paymentReference: String? = null,
    val paidAt: Long? = null,
    val cancellationReason: String? = null,
    val cancellationFeedback: String? = null,
    val cancelledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class CustomerReview(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceId: String,
    val serviceName: String,
    val professionalName: String,
    val customerName: String = "Priya S.",
    val rating: Float = 5.0f,
    val comment: String,
    val tags: String = "Punctual, Expert",
    val dateText: String = "Today",
    val createdAt: Long = System.currentTimeMillis()
)

data class ServiceCategory(
    val id: String,
    val name: String,
    val description: String,
    val startingPrice: Int,
    val iconName: String,
    val tag: String = "",
    val isFeatured: Boolean = false
)

data class ServicePackage(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val originalPrice: Int,
    val durationText: String,
    val includes: List<String> = emptyList()
)

data class ServiceItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val subtitle: String,
    val startingPrice: Int,
    val rating: Float,
    val reviewsCount: Int,
    val duration: String,
    val warrantyText: String,
    val description: String,
    val imageDrawableRes: Int, // e.g. R.drawable.img_cleaning_pro
    val isPopular: Boolean = false,
    val isHeroFeatured: Boolean = false,
    val recommendedProId: String = "pro_rajesh_1",
    val whatIsIncluded: List<String>,
    val whatIsNotIncluded: List<String>,
    val packages: List<ServicePackage> = emptyList(),
    val faqs: List<Pair<String, String>> = emptyList()
)

data class Professional(
    val id: String,
    val name: String,
    val phone: String,
    val specialty: String,
    val rating: Float,
    val reviewsCount: Int,
    val completedJobs: Int,
    val experienceYears: Int,
    val isVerified: Boolean = true,
    val etaMinutes: Int = 18,
    val avatarInitials: String = "RS"
)

data class Offer(
    val code: String,
    val title: String,
    val discountDescription: String,
    val description: String = discountDescription,
    val percentageDiscount: Int = 0,
    val flatDiscount: Int = 0,
    val minOrderAmount: Int = 299,
    val minBookingAmount: Int = minOrderAmount,
    val validUntil: String = "30 Sep 2026",
    val categoryRestriction: String? = null
)
