package com.example.data.remote.supabase

import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CustomerReview
import com.example.data.model.SavedAddress
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseUserProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "email") val email: String,
    @Json(name = "city") val city: String = "Agra",
    @Json(name = "locality") val locality: String = "Taj Nagri Phase 2",
    @Json(name = "role") val role: String = "CUSTOMER"
) {
    fun toDomainUserProfile(): UserProfile = UserProfile(
        id = id,
        name = name,
        phone = phone,
        email = email,
        city = city,
        locality = locality,
        role = try { UserRole.valueOf(role) } catch (e: Exception) { UserRole.CUSTOMER }
    )
}

fun UserProfile.toSupabaseDto(): SupabaseUserProfileDto = SupabaseUserProfileDto(
    id = id,
    name = name,
    phone = phone,
    email = email,
    city = city,
    locality = locality,
    role = role.name
)

@JsonClass(generateAdapter = true)
data class SupabaseSavedAddressDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "user_id") val userId: String = "user_priya_1",
    @Json(name = "title") val title: String,
    @Json(name = "full_address") val fullAddress: String,
    @Json(name = "locality") val locality: String,
    @Json(name = "city") val city: String = "Agra",
    @Json(name = "landmark") val landmark: String = "",
    @Json(name = "is_default") val isDefault: Boolean = false
) {
    fun toDomainAddress(): SavedAddress = SavedAddress(
        id = id ?: 0,
        userId = userId,
        title = title,
        fullAddress = fullAddress,
        locality = locality,
        city = city,
        landmark = landmark,
        isDefault = isDefault
    )
}

fun SavedAddress.toSupabaseDto(): SupabaseSavedAddressDto = SupabaseSavedAddressDto(
    id = null,
    userId = userId,
    title = title,
    fullAddress = fullAddress,
    locality = locality,
    city = city,
    landmark = landmark,
    isDefault = isDefault
)

@JsonClass(generateAdapter = true)
data class SupabaseBookingDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "booking_code") val bookingCode: String,
    @Json(name = "customer_id") val customerId: String = "user_priya_1",
    @Json(name = "customer_name") val customerName: String = "Priya Sharma",
    @Json(name = "customer_phone") val customerPhone: String = "+91 98765 43210",
    @Json(name = "service_id") val serviceId: String,
    @Json(name = "service_name") val serviceName: String,
    @Json(name = "package_name") val packageName: String,
    @Json(name = "scheduled_date") val scheduledDate: String,
    @Json(name = "scheduled_time") val scheduledTime: String,
    @Json(name = "address_text") val addressText: String,
    @Json(name = "locality") val locality: String,
    @Json(name = "city") val city: String = "Agra",
    @Json(name = "total_amount") val totalAmount: Int,
    @Json(name = "discount_amount") val discountAmount: Int = 0,
    @Json(name = "promo_code") val promoCode: String = "",
    @Json(name = "payment_method") val paymentMethod: String = "Cash after service",
    @Json(name = "is_paid") val isPaid: Boolean = false,
    @Json(name = "status") val status: String = "ASSIGNED",
    @Json(name = "professional_id") val professionalId: String = "pro_rajesh_1",
    @Json(name = "start_otp") val startOtp: String = "4829",
    @Json(name = "special_notes") val specialNotes: String = "",
    @Json(name = "payment_reference") val paymentReference: String? = null,
    @Json(name = "paid_at") val paidAt: Long? = null,
    @Json(name = "cancellation_reason") val cancellationReason: String? = null,
    @Json(name = "cancellation_feedback") val cancellationFeedback: String? = null,
    @Json(name = "cancelled_at") val cancelledAt: Long? = null,
    @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainBooking(): Booking = Booking(
        id = id ?: 0,
        bookingCode = bookingCode,
        customerId = customerId,
        customerName = customerName,
        customerPhone = customerPhone,
        serviceId = serviceId,
        serviceName = serviceName,
        packageName = packageName,
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
        addressText = addressText,
        locality = locality,
        city = city,
        totalAmount = totalAmount,
        discountAmount = discountAmount,
        promoCode = promoCode,
        paymentMethod = paymentMethod,
        isPaid = isPaid,
        status = try { BookingStatus.valueOf(status) } catch (e: Exception) { BookingStatus.ASSIGNED },
        professionalId = professionalId,
        startOtp = startOtp,
        specialNotes = specialNotes,
        paymentReference = paymentReference,
        paidAt = paidAt,
        cancellationReason = cancellationReason,
        cancellationFeedback = cancellationFeedback,
        cancelledAt = cancelledAt,
        createdAt = createdAt
    )
}

fun Booking.toSupabaseDto(): SupabaseBookingDto = SupabaseBookingDto(
    id = null,
    bookingCode = bookingCode,
    customerId = customerId,
    customerName = customerName,
    customerPhone = customerPhone,
    serviceId = serviceId,
    serviceName = serviceName,
    packageName = packageName,
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    addressText = addressText,
    locality = locality,
    city = city,
    totalAmount = totalAmount,
    discountAmount = discountAmount,
    promoCode = promoCode,
    paymentMethod = paymentMethod,
    isPaid = isPaid,
    status = status.name,
    professionalId = professionalId,
    startOtp = startOtp,
    specialNotes = specialNotes,
    paymentReference = paymentReference,
    paidAt = paidAt,
    cancellationReason = cancellationReason,
    cancellationFeedback = cancellationFeedback,
    cancelledAt = cancelledAt,
    createdAt = createdAt
)

@JsonClass(generateAdapter = true)
data class SupabaseReviewDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "service_id") val serviceId: String,
    @Json(name = "service_name") val serviceName: String,
    @Json(name = "professional_name") val professionalName: String,
    @Json(name = "customer_name") val customerName: String = "Priya S.",
    @Json(name = "rating") val rating: Float = 5.0f,
    @Json(name = "comment") val comment: String,
    @Json(name = "tags") val tags: String = "Punctual, Expert",
    @Json(name = "date_text") val dateText: String = "Today",
    @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainReview(): CustomerReview = CustomerReview(
        id = id ?: 0,
        serviceId = serviceId,
        serviceName = serviceName,
        professionalName = professionalName,
        customerName = customerName,
        rating = rating,
        comment = comment,
        tags = tags,
        dateText = dateText,
        createdAt = createdAt
    )
}

fun CustomerReview.toSupabaseDto(): SupabaseReviewDto = SupabaseReviewDto(
    id = null,
    serviceId = serviceId,
    serviceName = serviceName,
    professionalName = professionalName,
    customerName = customerName,
    rating = rating,
    comment = comment,
    tags = tags,
    dateText = dateText,
    createdAt = createdAt
)

@JsonClass(generateAdapter = true)
data class SupabaseServiceCategoryDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "starting_price") val startingPrice: Int,
    @Json(name = "icon_name") val iconName: String,
    @Json(name = "tag") val tag: String = "",
    @Json(name = "is_featured") val isFeatured: Boolean = false
) {
    fun toDomainCategory(): com.example.data.model.ServiceCategory = com.example.data.model.ServiceCategory(
        id = id,
        name = name,
        description = description,
        startingPrice = startingPrice,
        iconName = iconName,
        tag = tag,
        isFeatured = isFeatured
    )
}

fun com.example.data.model.ServiceCategory.toSupabaseDto(): SupabaseServiceCategoryDto = SupabaseServiceCategoryDto(
    id = id,
    name = name,
    description = description,
    startingPrice = startingPrice,
    iconName = iconName,
    tag = tag,
    isFeatured = isFeatured
)
