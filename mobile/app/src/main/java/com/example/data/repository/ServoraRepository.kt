package com.example.data.repository

import com.example.R
import com.example.data.db.AddressDao
import com.example.data.db.BookingDao
import com.example.data.db.ReviewDao
import com.example.data.db.UserDao
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
import com.example.data.remote.supabase.SupabaseSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ServoraRepository(
    private val bookingDao: BookingDao,
    private val addressDao: AddressDao,
    private val reviewDao: ReviewDao,
    private val userDao: UserDao
) {
    val syncManager = SupabaseSyncManager(bookingDao, addressDao, reviewDao, userDao)
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Reactive Room Streams
    val allBookings: Flow<List<Booking>> = bookingDao.getAllBookings()
    val activeBooking: Flow<Booking?> = bookingDao.getActiveBooking()
    val savedAddresses: Flow<List<SavedAddress>> = addressDao.getAllAddresses()
    val allReviews: Flow<List<CustomerReview>> = reviewDao.getAllReviews()
    val currentUser: Flow<UserProfile?> = userDao.getCurrentUser()
    val syncState = syncManager.syncState

    fun getBookingById(id: Long): Flow<Booking?> = bookingDao.getBookingById(id)
    fun getReviewsForService(serviceId: String): Flow<List<CustomerReview>> = reviewDao.getReviewsForService(serviceId)

    suspend fun syncWithSupabase(): Boolean = syncManager.performFullSync()

    suspend fun createBooking(booking: Booking): Long {
        val id = bookingDao.insertBooking(booking)
        val created = booking.copy(id = id)
        syncManager.pushBooking(created)
        return id
    }

    suspend fun updateBookingStatus(id: Long, status: BookingStatus): Boolean {
        val booking = bookingDao.getBookingByIdSync(id)
        if (booking == null) {
            bookingDao.updateStatus(id, status)
            return true
        }

        // Try remote update first (with one immediate retry for minor network flakes)
        var success = syncManager.updateBookingStatus(booking.bookingCode, status)
        if (!success) {
            kotlinx.coroutines.delay(300)
            success = syncManager.updateBookingStatus(booking.bookingCode, status)
        }

        if (success) {
            bookingDao.updateStatus(id, status)
            return true
        } else {
            // Remote failed: leave local Room untouched so UI reflects real server state
            return false
        }
    }

    suspend fun cancelBooking(id: Long, reason: String? = null, feedback: String? = null): Boolean {
        val booking = bookingDao.getBookingByIdSync(id)
        val now = System.currentTimeMillis()
        val success = if (booking != null) {
            syncManager.updateBookingStatus(booking.bookingCode, BookingStatus.CANCELLED, reason, feedback)
        } else true
        bookingDao.updateCancellation(id, BookingStatus.CANCELLED, reason, feedback, now)
        return success
    }

    suspend fun refreshBookingStatus(bookingId: Long): Boolean = syncManager.refreshBookingStatus(bookingId)

    suspend fun addAddress(address: SavedAddress): Long {
        val id = addressDao.insertAddress(address)
        val created = address.copy(id = id)
        syncManager.pushAddressAsync(created, repositoryScope)
        return id
    }

    suspend fun deleteAddress(id: Long) {
        addressDao.deleteAddress(id)
        syncManager.deleteAddressAsync(id, repositoryScope)
    }

    suspend fun setDefaultAddress(id: Long) {
        addressDao.clearDefaults()
        addressDao.setDefaultAddress(id)
    }

    suspend fun addReview(review: CustomerReview): Long {
        val id = reviewDao.insertReview(review)
        val created = review.copy(id = id)
        syncManager.pushReviewAsync(created, repositoryScope)
        return id
    }

    suspend fun updateUserRole(role: UserRole) {
        userDao.updateRole(role)
        val user = userDao.getCurrentUserSync()
        if (user != null) {
            syncManager.pushUserProfileAsync(user, repositoryScope)
        }
    }

    suspend fun updateLocation(city: String, locality: String) {
        userDao.updateLocation(city, locality)
        val user = userDao.getCurrentUserSync()
        if (user != null) {
            syncManager.pushUserProfileAsync(user, repositoryScope)
        }
    }

    suspend fun updateProfile(name: String, phone: String, email: String) {
        val current = userDao.getCurrentUserSync() ?: UserProfile()
        val updated = current.copy(name = name, phone = phone, email = email)
        userDao.insertUser(updated)
        syncManager.pushUserProfileAsync(updated, repositoryScope)
    }

    suspend fun loginUser(profile: UserProfile) {
        userDao.clearUsers()
        userDao.insertUser(profile)
        com.example.data.remote.supabase.SupabaseClient.devProfileId = profile.id
        com.example.data.remote.supabase.SupabaseClient.devUserRole = profile.role.name
        syncManager.pushUserProfileAsync(profile, repositoryScope)
    }

    suspend fun logoutUser() {
        userDao.clearUsers()
        com.example.data.remote.supabase.SupabaseClient.userAccessToken = null
        com.example.data.remote.supabase.SupabaseClient.devProfileId = null
        com.example.data.remote.supabase.SupabaseClient.devUserRole = null
    }

    val demoCustomer = UserProfile(
        id = "user_priya_1",
        name = "Priya Sharma",
        phone = "+91 98765 43210",
        email = "priya.sharma@example.com",
        city = "Agra",
        locality = "Taj Nagri Phase 2",
        role = UserRole.CUSTOMER
    )

    val demoPartner = UserProfile(
        id = "pro_rajesh_1",
        name = "Rajesh Sharma",
        phone = "+91 98234 56789",
        email = "rajesh.pro@serviceassist.in",
        city = "Agra",
        locality = "Fatehabad Road",
        role = UserRole.PROFESSIONAL
    )

    val demoAdmin = UserProfile(
        id = "user_admin_sneha",
        name = "Sneha Patel",
        phone = "+91 94120 11223",
        email = "admin@serviceassist.in",
        city = "Agra",
        locality = "Sanjay Place",
        role = UserRole.ADMIN
    )

    val categories: List<ServiceCategory>
        get() {
            val remote = syncManager.remoteCategories.value
            return if (remote.isNotEmpty()) {
                val map = defaultCategories.associateBy { it.id }.toMutableMap()
                remote.forEach { map[it.id] = it }
                map.values.toList()
            } else {
                defaultCategories
            }
        }

    // In-memory catalog of Categories, Services, Professionals, Offers, and Cities
    val defaultCategories: List<ServiceCategory> = listOf(
        ServiceCategory(
            id = "cat_ac",
            name = "AC Repair",
            description = "Deep jet cleaning, gas refill, repair & installation",
            startingPrice = 499,
            iconName = "ac_unit",
            tag = "Most Booked",
            isFeatured = true
        ),
        ServiceCategory(
            id = "cat_cleaning",
            name = "Home Cleaning",
            description = "Deep cleaning, bathroom, kitchen & sofa scrubbing",
            startingPrice = 999,
            iconName = "cleaning_services",
            tag = "4.9 ★ Rating",
            isFeatured = true
        ),
        ServiceCategory(
            id = "cat_salon_w",
            name = "Salon for Women",
            description = "Facial, waxing, mani-pedi, haircut & spa at home",
            startingPrice = 399,
            iconName = "spa",
            tag = "Top Rated",
            isFeatured = true
        ),
        ServiceCategory(
            id = "cat_salon_m",
            name = "Salon for Men",
            description = "Haircut, beard styling, head massage & tan care",
            startingPrice = 249,
            iconName = "face",
            tag = "Quick Visit"
        ),
        ServiceCategory(
            id = "cat_electrician",
            name = "Electrician",
            description = "Switchboards, fans, MCB fuse, wiring & chandeliers",
            startingPrice = 199,
            iconName = "bolt",
            tag = "Under 30 mins"
        ),
        ServiceCategory(
            id = "cat_plumber",
            name = "Plumber",
            description = "Leakages, taps, flush tanks, washbasins & water pipes",
            startingPrice = 199,
            iconName = "plumbing",
            tag = "Verified"
        ),
        ServiceCategory(
            id = "cat_carpenter",
            name = "Carpenter",
            description = "Furniture repair, hinges, lock installation & drills",
            startingPrice = 249,
            iconName = "handyman"
        ),
        ServiceCategory(
            id = "cat_pest",
            name = "Pest Control",
            description = "Cockroach, termite, bed bug & mosquito control",
            startingPrice = 799,
            iconName = "pest_control"
        ),
        ServiceCategory(
            id = "cat_painting",
            name = "Painting",
            description = "Full home, waterproofing, texture wall & touch-ups",
            startingPrice = 1499,
            iconName = "format_paint"
        ),
        ServiceCategory(
            id = "cat_ro",
            name = "RO Repair",
            description = "Filter replacement, membrane service & water testing",
            startingPrice = 299,
            iconName = "water_drop"
        ),
        ServiceCategory(
            id = "cat_appliances",
            name = "Appliance Repair",
            description = "Washing machine, refrigerator, microwave & geyser",
            startingPrice = 349,
            iconName = "kitchen"
        ),
        ServiceCategory(
            id = "cat_bathroom",
            name = "Bathroom Cleaning",
            description = "Hard water stains, tile scrubbing, mirror polishing",
            startingPrice = 599,
            iconName = "shower"
        ),
        ServiceCategory(
            id = "cat_kitchen",
            name = "Kitchen Cleaning",
            description = "Degreasing chimney, tiles, cabinets & gas stove",
            startingPrice = 799,
            iconName = "countertops"
        ),
        ServiceCategory(
            id = "cat_sofa",
            name = "Sofa Cleaning",
            description = "Fabric extraction, leather polish & cushions",
            startingPrice = 399,
            iconName = "chair"
        )
    )

    val services: List<ServiceItem> = listOf(
        // ================= 1. AC & APPLIANCES (cat_ac) =================
        ServiceItem(
            id = "ac_service_deep",
            categoryId = "cat_ac",
            name = "Intense AC Foam Jet Service",
            subtitle = "2x deeper dust extraction with high-pressure water jet technology",
            startingPrice = 499,
            rating = 4.84f,
            reviewsCount = 3840,
            duration = "45 mins",
            warrantyText = "30-day cooling warranty",
            description = "Service Assist's signature split and window AC deep servicing. Uses specialized indoor foam spray and high-pressure jet pump with spill-guard bag to wash indoor coils, blower fan, drain tray, and outdoor condenser.",
            imageDrawableRes = R.drawable.img_ac_repair,
            isPopular = true,
            isHeroFeatured = true,
            whatIsIncluded = listOf(
                "Complete coil & blower cleaning with high-pressure water jet",
                "Spill-proof indoor jacket protection for walls & sofa",
                "Outdoor condenser coil high-pressure wash",
                "Cooling gas pressure and amp measurement check",
                "Drain pipe clearing to prevent indoor water dripping"
            ),
            whatIsNotIncluded = listOf(
                "Gas refill or leak repair (charged separately if needed)",
                "Spare parts replacement like capacitor or circuit board",
                "Dismounting/reinstalling AC unit from wall"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_ac_1",
                    name = "1 Split AC Jet Wash",
                    description = "Single indoor & outdoor unit comprehensive cleaning",
                    price = 499,
                    originalPrice = 699,
                    durationText = "45 mins",
                    includes = listOf("Foam Jet Indoor Wash", "Outdoor Jet Clean", "Gas Check", "Spill Guard")
                ),
                ServicePackage(
                    id = "pkg_ac_2",
                    name = "2 Split ACs Combo Pack",
                    description = "Service 2 AC units together and save ₹200",
                    price = 799,
                    originalPrice = 1198,
                    durationText = "80 mins",
                    includes = listOf("2x Foam Jet Wash", "2x Outdoor Jet Clean", "Dual Gas Diagnostics")
                ),
                ServicePackage(
                    id = "pkg_ac_3",
                    name = "Full Home Cooling (3 ACs)",
                    description = "Best value for whole apartment or bungalow",
                    price = 1149,
                    originalPrice = 1797,
                    durationText = "120 mins",
                    includes = listOf("3x Deep Wash", "Full Electrical Inspection", "Anti-Rust Spray")
                )
            ),
            faqs = listOf(
                "Will water ruin my painted wall?" to "Not at all. Our verified technicians fit a sealed waterproof protective jacket around the indoor AC that funnels all drained dirty water safely into a bucket.",
                "How often should AC jet service be done in Agra?" to "Due to Agra's seasonal dust and summer heat, servicing every 4 to 6 months maintains maximum cooling efficiency and reduces power bills."
            )
        ),
        ServiceItem(
            id = "ac_gas_leak",
            categoryId = "cat_ac",
            name = "AC Gas Refill & Leak Fix",
            subtitle = "Complete nitrogen leak test, brazing repair & pure refrigerant top-up",
            startingPrice = 1499,
            rating = 4.88f,
            reviewsCount = 1290,
            duration = "60 mins",
            warrantyText = "60-day Gas Leak Warranty",
            description = "Comprehensive refrigerant diagnosis using digital pressure gauges, high-pressure nitrogen testing, copper pipe brazing, vacuumization, and weighing-scale accurate gas charge.",
            imageDrawableRes = R.drawable.img_ac_repair,
            isPopular = false,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Nitrogen pressure leak detection",
                "Copper pipe brazing & valve replacement",
                "Complete compressor vacuum pumping",
                "100% pure R32 / R410A / R22 gas charge by weight"
            ),
            whatIsNotIncluded = listOf(
                "Compressor mechanical overhaul or motor replacement",
                "PCB circuit replacement"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_ac_gas_split",
                    name = "Split AC Gas Top-up & Refill",
                    description = "Full refrigerant refill with leak fix & 60-day warranty",
                    price = 1499,
                    originalPrice = 1999,
                    durationText = "60 mins",
                    includes = listOf("Leak Detection", "Nitrogen Test", "Gas Refill", "Cooling Check")
                )
            )
        ),

        // ================= 2. HOME CLEANING (cat_cleaning) =================
        ServiceItem(
            id = "deep_home_cleaning",
            categoryId = "cat_cleaning",
            name = "Full Home Deep Cleaning",
            subtitle = "Thorough sanitization, machine scrubbing & corner-to-corner detailing",
            startingPrice = 999,
            rating = 4.92f,
            reviewsCount = 2410,
            duration = "4 - 6 hours",
            warrantyText = "100% Satisfaction or Free Reclean",
            description = "Complete revival for your living spaces. A trained crew of 2-3 professionals arrives with industrial grade single-disc floor scrubbers, vacuum extractors, non-hazardous cleaning agents, and microfiber towels.",
            imageDrawableRes = R.drawable.img_cleaning_pro,
            isPopular = true,
            isHeroFeatured = true,
            whatIsIncluded = listOf(
                "All rooms floor scrubbing with rotary machines",
                "Kitchen deep degreasing: chimney, exhaust, tiles & cabinets",
                "Bathrooms descaling, hard water stain removal & sanitization",
                "Balcony wash, ceiling cobweb removal & fan blades dusting",
                "Window glass, railings, switchboards & door frames wiped"
            ),
            whatIsNotIncluded = listOf(
                "Repainting walls or filling cement cracks",
                "Moving heavy solid wood wardrobes or fixed safes",
                "Wet cleaning inside locked private drawers without permission"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_clean_1bhk",
                    name = "1 BHK Full Home",
                    description = "Complete deep clean for 1 bedroom, hall, kitchen & bath",
                    price = 999,
                    originalPrice = 1499,
                    durationText = "3 hours",
                    includes = listOf("1 Bedroom", "1 Bathroom", "1 Kitchen", "Living Room", "Dry & Wet Vacuuming")
                ),
                ServicePackage(
                    id = "pkg_clean_2bhk",
                    name = "2 BHK Full Home",
                    description = "Most popular package in Agra apartments",
                    price = 1899,
                    originalPrice = 2499,
                    durationText = "4.5 hours",
                    includes = listOf("2 Bedrooms", "2 Bathrooms", "Complete Kitchen", "Balcony & Living")
                ),
                ServicePackage(
                    id = "pkg_clean_3bhk",
                    name = "3 BHK / Villa Deep Clean",
                    description = "Comprehensive team of 3 specialists with floor machine",
                    price = 2799,
                    originalPrice = 3699,
                    durationText = "6 hours",
                    includes = listOf("3 Bedrooms", "3 Bathrooms", "Deep Kitchen Scrub", "2 Balconies", "Floor Buffing")
                )
            ),
            faqs = listOf(
                "Do I need to provide buckets or cleaning liquids?" to "No, Service Assist professionals carry all required specialized cleaning agents, rotary scrubber, vacuum, and ladders.",
                "Are the cleaning chemicals safe for pets and children?" to "Yes, we exclusively utilize eco-friendly, non-toxic hospital-grade sanitizers that leave a fresh citrus scent."
            )
        ),
        ServiceItem(
            id = "villa_makeover_cleaning",
            categoryId = "cat_cleaning",
            name = "Move-in / Post-Construction Clean",
            subtitle = "Intensive cement stain removal, paint scraping & industrial machine polish",
            startingPrice = 3499,
            rating = 4.95f,
            reviewsCount = 780,
            duration = "6 - 8 hours",
            warrantyText = "Ready-to-Move Guarantee",
            description = "Specialized heavy-duty cleaning for newly renovated or recently painted houses. Clears dried plaster, paint marks, cement debris, and deep tile buffing.",
            imageDrawableRes = R.drawable.img_cleaning_pro,
            isPopular = false,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Cement & paint splatter removal from floors & window glass",
                "Inside-out deep vacuuming of all fixed cupboards & drawers",
                "High-pressure wash for terraces & driveways",
                "Sanitization of all sanitaryware and bath fittings"
            ),
            whatIsNotIncluded = listOf(
                "Hauling large masonry construction rubble"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_post_con_2bhk",
                    name = "2-3 BHK Move-in Clean",
                    description = "Deep renovation debris clearance and tile scrubbing",
                    price = 3499,
                    originalPrice = 4500,
                    durationText = "6 hours",
                    includes = listOf("Paint Scraper Work", "Deep Buffing", "Glass Detailing", "Cabinet Sanitize")
                )
            )
        ),

        // ================= 3. BATHROOM CLEANING (cat_bathroom) =================
        ServiceItem(
            id = "bathroom_deep_scrub",
            categoryId = "cat_bathroom",
            name = "Intense Bathroom Stain Removal",
            subtitle = "Remove stubborn yellow hard water scales, soap scum & grout mildew",
            startingPrice = 599,
            rating = 4.88f,
            reviewsCount = 2890,
            duration = "60 mins",
            warrantyText = "Spotless Guarantee",
            description = "Make ceramic tiles and chrome fixtures sparkle like new. High-potency acid-free descaling agents break down minerals without harming expensive marble or grouting.",
            imageDrawableRes = R.drawable.img_bathroom_cleaner,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Full tile wall & floor machine scrubbing",
                "Toilet pot internal descaling & rim disinfection",
                "Mirror, glass partition & washbasin gleaming polish",
                "Chrome taps and shower head lime-scale removal"
            ),
            whatIsNotIncluded = listOf(
                "Replacing broken tiles or regrouting missing cement"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_bath_1",
                    name = "1 Bathroom Deep Cleaning",
                    description = "Single bathroom thorough descaling and sanitation",
                    price = 599,
                    originalPrice = 799,
                    durationText = "60 mins",
                    includes = listOf("Tiles Scrubbing", "Sanitary Descaling", "Chrome Polish")
                ),
                ServicePackage(
                    id = "pkg_bath_2",
                    name = "2 Bathrooms Combo",
                    description = "Service master and guest bathrooms together",
                    price = 999,
                    originalPrice = 1499,
                    durationText = "100 mins",
                    includes = listOf("2 Bathrooms Complete", "Exhaust Fan Clean", "Drain Disinfection")
                )
            )
        ),

        // ================= 4. KITCHEN CLEANING (cat_kitchen) =================
        ServiceItem(
            id = "kitchen_deep_clean",
            categoryId = "cat_kitchen",
            name = "Kitchen Deep Degreasing & Chimney Clean",
            subtitle = "Complete removal of sticky oil grease from chimney, tiles & slabs",
            startingPrice = 799,
            rating = 4.89f,
            reviewsCount = 1940,
            duration = "90 mins",
            warrantyText = "Oil-Free Sparkle Guarantee",
            description = "Heavy kitchen grime and sticky oil build-up are dissolved with food-safe bio-degreasing solvents and steam cleaning.",
            imageDrawableRes = R.drawable.img_kitchen_clean_work,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Chimney outer body & baffle filter ultrasonic degreasing",
                "Gas burner, stove & knob carbon removal",
                "Backsplash tile scrubbing and grout oil lift",
                "Modular cabinet exterior wiping & countertop sanitization"
            ),
            whatIsNotIncluded = listOf(
                "Cleaning inside utensil racks without client unpacking"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_kitch_1",
                    name = "Standard Kitchen Degrease",
                    description = "Complete stove, tiles, sink & chimney filter cleaning",
                    price = 799,
                    originalPrice = 1199,
                    durationText = "90 mins",
                    includes = listOf("Baffle Filter Soak", "Wall Tile Scrub", "Sink Polish", "Stove Clean")
                ),
                ServicePackage(
                    id = "pkg_kitch_2",
                    name = "Modular Kitchen + Appliance Detailing",
                    description = "Includes microwave, fridge exterior & cabinet interiors",
                    price = 1299,
                    originalPrice = 1799,
                    durationText = "130 mins",
                    includes = listOf("Full Degreasing", "Microwave Clean", "Drawer Sanitize", "Exhaust Motor Clean")
                )
            )
        ),

        // ================= 5. SOFA & UPHOLSTERY (cat_sofa) =================
        ServiceItem(
            id = "sofa_shampoo_cleaning",
            categoryId = "cat_sofa",
            name = "Sofa & Upholstery Deep Extraction",
            subtitle = "Foam shampoo scrubbing & wet vacuum suction for stain & dust removal",
            startingPrice = 399,
            rating = 4.91f,
            reviewsCount = 2150,
            duration = "60 mins",
            warrantyText = "Fresh Fabric Warranty",
            description = "Restore the original color and fragrance of your fabric and suede sofas. High-power injection extraction vacuums pull out embedded dust mites, spills, and pet odors.",
            imageDrawableRes = R.drawable.img_sofa_clean_work,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Deep dry vacuuming of seams, corners & cushions",
                "Specialized foam shampoo scrubbing for tough food stains",
                "High-power extraction vacuum moisture removal",
                "Fabric deodorizer spray"
            ),
            whatIsNotIncluded = listOf(
                "Repairing torn stitching or replacing foam"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_sofa_3seater",
                    name = "3-Seater Sofa Shampoo",
                    description = "Deep extraction shampoo for 3 seats",
                    price = 599,
                    originalPrice = 899,
                    durationText = "45 mins",
                    includes = listOf("Dry Vacuum", "Foam Scrub", "Moisture Extraction", "Deodorize")
                ),
                ServicePackage(
                    id = "pkg_sofa_5seater",
                    name = "5-Seater / L-Shape Sofa Set",
                    description = "Full living room sofa set revival with cushions",
                    price = 899,
                    originalPrice = 1399,
                    durationText = "75 mins",
                    includes = listOf("5 Seats + Cushions", "Armrest Scrub", "Fabric Conditioner")
                )
            )
        ),

        // ================= 6. PAINTING & WATERPROOFING (cat_painting) =================
        ServiceItem(
            id = "painting_full_home",
            categoryId = "cat_painting",
            name = "Wall Painting & Waterproofing",
            subtitle = "Laser measurement, wall putty, primer & premium Asian Paints finish",
            startingPrice = 1499,
            rating = 4.87f,
            reviewsCount = 1420,
            duration = "1 - 3 Days",
            warrantyText = "1-Year Paint & Seepage Warranty",
            description = "Transform your home with verified master painters. Dustless mechanized sanding, accurate laser site consultation, wall dampness inspection, and premium Asian Paints Royale or Apex finish.",
            imageDrawableRes = R.drawable.img_painter_work,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Free doorstep laser measurement & color consultation",
                "Complete floor and furniture masking protection",
                "Mechanized wall sanding & crack filling",
                "2 coats of primer + 2 coats of premium washable paint"
            ),
            whatIsNotIncluded = listOf(
                "Civil masonry plaster repair"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_paint_1room",
                    name = "Single Room Repaint",
                    description = "Complete 4 walls & ceiling refresh with premium plastic emulsion",
                    price = 1499,
                    originalPrice = 2199,
                    durationText = "1 Day",
                    includes = listOf("Crack Filling", "Primer Coat", "2 Paint Coats", "Masking & Cleanup")
                ),
                ServicePackage(
                    id = "pkg_paint_full",
                    name = "Full Home Consultation & Painting",
                    description = "Complete 2/3 BHK end-to-end painting with color visualization",
                    price = 4999,
                    originalPrice = 7500,
                    durationText = "3-4 Days",
                    includes = listOf("Laser Survey", "Royale Emulsion", "Ceiling White", "Post-paint cleanup")
                )
            )
        ),
        ServiceItem(
            id = "painting_room_makeover",
            categoryId = "cat_painting",
            name = "Accent Wall & Fresh Paint Makeover",
            subtitle = "Designer stencils, geometric textures & waterproof highlight walls",
            startingPrice = 999,
            rating = 4.93f,
            reviewsCount = 890,
            duration = "4 hours",
            warrantyText = "Fade-Resistant Guarantee",
            description = "Add elegance to your living room or bedroom with a stunning accent wall, metallic textures, or aesthetic solid tone highlights.",
            imageDrawableRes = R.drawable.img_home_makeover,
            isPopular = false,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Designer stencil / texture application",
                "Furniture masking & tape boundary lines",
                "High sheen metallic or matte finish",
                "Full floor protection"
            ),
            whatIsNotIncluded = listOf("Exterior exterior wall coating"),
            packages = listOf(
                ServicePackage(
                    id = "pkg_paint_accent",
                    name = "1 Designer Feature Wall",
                    description = "Texture or dual-tone feature wall for living room",
                    price = 999,
                    originalPrice = 1499,
                    durationText = "4 hours",
                    includes = listOf("Feature Design", "Royale Play Texture", "Clean edges")
                )
            )
        ),

        // ================= 7. PLUMBER (cat_plumber) =================
        ServiceItem(
            id = "plumber_leak_fix",
            categoryId = "cat_plumber",
            name = "Plumbing Leakage & Tap Care",
            subtitle = "Fast fix for dripping taps, flush cisterns, basin chokes & pipes",
            startingPrice = 199,
            rating = 4.79f,
            reviewsCount = 1530,
            duration = "30 mins",
            warrantyText = "30-Day Leak-Free Guarantee",
            description = "Tackle frustrating water leaks and drain blockages immediately. Our verified plumbers arrive with pipe wrenches, teflon seals, silicone gaskets, and unblocking snakes.",
            imageDrawableRes = R.drawable.img_plumber_work,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Diagnostic inspection of pipes & valves",
                "Repair/replacement of up to 2 taps or angle valves",
                "Drain unblocking for kitchen sink or washbasin trap",
                "Pressure test to ensure zero residual drip"
            ),
            whatIsNotIncluded = listOf(
                "Cost of branded brass faucets or PVC pipes (customer provides or pro procures at MRP)",
                "Excavating underground sewer lines"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_plumb_1",
                    name = "Tap & Leak Repair",
                    description = "Fix 1-2 dripping faucets or loose pipe joints",
                    price = 199,
                    originalPrice = 299,
                    durationText = "30 mins",
                    includes = listOf("Teflon sealing", "Washer replacement", "Flow check")
                ),
                ServicePackage(
                    id = "pkg_plumb_2",
                    name = "Toilet Flush Tank Overhaul",
                    description = "Fix continuous overflow or broken flush siphon",
                    price = 349,
                    originalPrice = 499,
                    durationText = "45 mins",
                    includes = listOf("Ball valve tuning", "Inlet replacement", "Leak test")
                )
            )
        ),
        ServiceItem(
            id = "plumber_tank_cleaning",
            categoryId = "cat_plumber",
            name = "Overhead Water Tank Deep Clean",
            subtitle = "High-pressure mud extraction, sludge wash & UV germicidal disinfection",
            startingPrice = 499,
            rating = 4.86f,
            reviewsCount = 920,
            duration = "60 mins",
            warrantyText = "100% Pure Water Guarantee",
            description = "Remove algae, silt, and bacterial slime from overhead Sintex or RCC water tanks with specialized rotary jet pumps and antibacterial spray.",
            imageDrawableRes = R.drawable.img_plumber_work,
            isPopular = false,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Complete water dewatering & sludge vacuuming",
                "High-pressure jet scrubbing of tank interior walls",
                "Antibacterial chlorine wash & UV disinfection",
                "Inlet filter pipe cleaning"
            ),
            whatIsNotIncluded = listOf("Repairing cracked concrete tank structure"),
            packages = listOf(
                ServicePackage(
                    id = "pkg_tank_1000l",
                    name = "Overhead Tank (Up to 1000L)",
                    description = "Complete 6-stage hygienic tank cleaning",
                    price = 499,
                    originalPrice = 799,
                    durationText = "60 mins",
                    includes = listOf("Sludge Removal", "High Pressure Wash", "UV Sterilization")
                )
            )
        ),

        // ================= 8. ELECTRICIAN (cat_electrician) =================
        ServiceItem(
            id = "electrician_quick_fix",
            categoryId = "cat_electrician",
            name = "Electrician On-Demand Visit & Wiring",
            subtitle = "Certified wiremen for switches, fuse trips, fans & short circuits",
            startingPrice = 199,
            rating = 4.81f,
            reviewsCount = 1890,
            duration = "30 mins",
            warrantyText = "30-Day Workmanship Guarantee",
            description = "Get an experienced ITI-certified electrician at your doorstep in under 30 minutes in Agra. Equipped with digital multimeters, insulated safety gear, and spare testing lamps.",
            imageDrawableRes = R.drawable.img_electrician_work,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Doorstep diagnostic visit & fault inspection",
                "Fixing up to 2 switches / sockets or ceiling fan regulator",
                "MCB breaker trip diagnosis",
                "Safety voltage & earthing check"
            ),
            whatIsNotIncluded = listOf(
                "Cost of replacement switches, fans or MCBs (billed on actual MRP)",
                "Concealed wall cutting / whole house rewiring"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_elec_1",
                    name = "Quick Repair (Up to 30 mins)",
                    description = "Fix 1-2 faulty switches, sockets, or fan regulator",
                    price = 199,
                    originalPrice = 299,
                    durationText = "30 mins",
                    includes = listOf("Diagnosis", "Switch/Socket Replacement", "Safety Check")
                ),
                ServicePackage(
                    id = "pkg_elec_2",
                    name = "Fan / Chandelier Installation",
                    description = "Mounting and secure wiring for heavy ceiling fixtures",
                    price = 299,
                    originalPrice = 399,
                    durationText = "45 mins",
                    includes = listOf("Ceiling Hook Check", "Wiring Connection", "Balance Tuning")
                )
            )
        ),
        ServiceItem(
            id = "electrician_appliance_install",
            categoryId = "cat_electrician",
            name = "Inverter, Geyser & MCB Setup",
            subtitle = "Heavy load wiring, sub-meter installation & short-circuit prevention",
            startingPrice = 349,
            rating = 4.85f,
            reviewsCount = 1120,
            duration = "45 mins",
            warrantyText = "30-Day Safe Wiring Warranty",
            description = "Safe installation and wiring for heavy appliances including power inverters, water geysers, MCB distribution boards, and heavy AC sockets.",
            imageDrawableRes = R.drawable.img_electrician_work,
            isPopular = false,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Inverter battery connection & bypass testing",
                "Geyser mounting, water inlet earthing & power test",
                "MCB box phase distribution load balance"
            ),
            whatIsNotIncluded = listOf("Internal plumbing pipe laying for geysers"),
            packages = listOf(
                ServicePackage(
                    id = "pkg_inverter_setup",
                    name = "Inverter & Battery Installation",
                    description = "Complete wiring connection and charging diagnostics",
                    price = 349,
                    originalPrice = 499,
                    durationText = "45 mins",
                    includes = listOf("Inverter Mount", "Battery Terminal Greasing", "Load Balance")
                )
            )
        ),

        // ================= 9. CARPENTER (cat_carpenter) =================
        ServiceItem(
            id = "carpenter_furniture_repair",
            categoryId = "cat_carpenter",
            name = "Carpentry, Bed & Wardrobe Assembly",
            subtitle = "Master carpenters for hinge alignment, hydraulic bed fitting & lock repairs",
            startingPrice = 249,
            rating = 4.82f,
            reviewsCount = 1350,
            duration = "45 mins",
            warrantyText = "30-Day Alignment Guarantee",
            description = "Fix squeaky doors, sagging modular wardrobe drawers, loose cabinet hinges, or assemble brand new furniture from Amazon/IKEA with precision power drills and clamps.",
            imageDrawableRes = R.drawable.img_carpenter_work,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Fixing loose hinges, drawer channels & sliding wheels",
                "Door latch, handles and tower bolt tightening",
                "Bed frame leveling and wooden joint bracing"
            ),
            whatIsNotIncluded = listOf(
                "Procurement cost of hardware fittings (hinges, locks)",
                "Full wood polishing or varnish coats"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_carp_1",
                    name = "Minor Repair & Hinge Tuning",
                    description = "Fix up to 2 cabinet hinges, drawer channels or door locks",
                    price = 249,
                    originalPrice = 349,
                    durationText = "45 mins",
                    includes = listOf("Hinge Adjustment", "Screw Tightening", "Smooth Slide Check")
                ),
                ServicePackage(
                    id = "pkg_carp_2",
                    name = "Bed / Wardrobe Assembly",
                    description = "Complete assembly of double bed, sofa-cum-bed or 2-door wardrobe",
                    price = 599,
                    originalPrice = 899,
                    durationText = "90 mins",
                    includes = listOf("Frame Assembly", "Headboard Mounting", "Hydraulic Lift Tuning")
                )
            )
        ),
        ServiceItem(
            id = "carpenter_door_locks",
            categoryId = "cat_carpenter",
            name = "Door Locks, Hinges & Mesh Fitting",
            subtitle = "Godrej mortise locks, stainless steel wire mesh & sliding balcony channels",
            startingPrice = 299,
            rating = 4.84f,
            reviewsCount = 760,
            duration = "45 mins",
            warrantyText = "Perfect Fit Guarantee",
            description = "Professional installation of high-security Godrej mortise locks, mosquito net wooden frames, and heavy stainless steel door closers.",
            imageDrawableRes = R.drawable.img_carpenter_work,
            isPopular = false,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Door mortise cavity chiseled with precision",
                "Deadbolt and cylinder lock alignment",
                "Door closer tension adjustment"
            ),
            whatIsNotIncluded = listOf("Cost of locks/mesh"),
            packages = listOf(
                ServicePackage(
                    id = "pkg_lock_install",
                    name = "Mortise Lock Installation",
                    description = "Install 1 main door Godrej/Europa high security lock",
                    price = 299,
                    originalPrice = 450,
                    durationText = "45 mins",
                    includes = listOf("Chiseling", "Handle Fitting", "Key Alignment")
                )
            )
        ),

        // ================= 10. PEST CONTROL (cat_pest) =================
        ServiceItem(
            id = "pest_control_herbal",
            categoryId = "cat_pest",
            name = "Herbal Pest & Cockroach Control",
            subtitle = "100% odorless gel baiting & spray treatment safe for children & pets",
            startingPrice = 799,
            rating = 4.90f,
            reviewsCount = 1860,
            duration = "45 mins",
            warrantyText = "90-Day Free Retreatment Warranty",
            description = "Eliminate cockroaches, ants, and pests without clearing kitchen utensils or bearing toxic fumes. Uses advanced micro-encapsulated odorless gel dots and perimeter spray.",
            imageDrawableRes = R.drawable.img_pest_control_work,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Odorless herbal gel baiting in all kitchen cabinets & corners",
                "Perimeter spray along baseboards, drain pipes & windows",
                "Sink drain hole anti-cockroach chemical flushing",
                "Free 2nd round visit within 90 days if pests return"
            ),
            whatIsNotIncluded = listOf(
                "Termite drilling into wooden structures (separate package)"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_pest_1bhk",
                    name = "1-2 BHK Pest Control",
                    description = "Full apartment odorless cockroach and ant treatment",
                    price = 799,
                    originalPrice = 1199,
                    durationText = "45 mins",
                    includes = listOf("Kitchen Gel Baiting", "Bathroom Spray", "90-Day Warranty")
                ),
                ServicePackage(
                    id = "pkg_pest_3bhk",
                    name = "3 BHK / Villa Pest Shield",
                    description = "Comprehensive protection for bedrooms, kitchen, balcony & drains",
                    price = 1199,
                    originalPrice = 1699,
                    durationText = "60 mins",
                    includes = listOf("Full Home Gel Dots", "Drain Disinfection", "Dual Visits")
                )
            )
        ),
        ServiceItem(
            id = "pest_control_termite",
            categoryId = "cat_pest",
            name = "Anti-Termite Drilling & Wood Protection",
            subtitle = "Deep chemical injection at 1-foot intervals to protect wooden wardrobes & doors",
            startingPrice = 1499,
            rating = 4.94f,
            reviewsCount = 640,
            duration = "2 - 3 hours",
            warrantyText = "1-Year Anti-Termite Guarantee",
            description = "Protect precious woodwork from silent termite destruction. Technicians drill tiny holes along walls and wooden frames, inject termiticide, and seal with matching wood filler.",
            imageDrawableRes = R.drawable.img_pest_control_work,
            isPopular = false,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Drill-Fill-Seal technology around infested door frames & cupboards",
                "Government-approved non-repellent chemical infusion",
                "Color-matched wax sealing of drill points",
                "1-year warranty with free inspection"
            ),
            whatIsNotIncluded = listOf("Replacing termite-eaten structural wood"),
            packages = listOf(
                ServicePackage(
                    id = "pkg_termite_wardrobe",
                    name = "Wardrobe & Door Frame Treatment",
                    description = "Protect up to 2 large wardrobes and 3 door frames",
                    price = 1499,
                    originalPrice = 2199,
                    durationText = "2 hours",
                    includes = listOf("Drill & Inject", "Chemical Barrier", "1-Year Warranty")
                )
            )
        ),

        // ================= 11. RO WATER PURIFIER (cat_ro) =================
        ServiceItem(
            id = "ro_water_purifier_service",
            categoryId = "cat_ro",
            name = "RO Water Purifier Service & Filter Change",
            subtitle = "TDS digital testing, sediment filter replacement & membrane flushing",
            startingPrice = 299,
            rating = 4.88f,
            reviewsCount = 1980,
            duration = "45 mins",
            warrantyText = "30-Day Leak & Purity Guarantee",
            description = "Ensure clean, sweet drinking water for your family in Agra. Our technician performs a digital water TDS test, checks booster pump pressure, flushes the RO membrane, and sanitizes storage tanks.",
            imageDrawableRes = R.drawable.img_ro_water_work,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Digital TDS and water hardness test before and after",
                "Complete inner water tank sanitization",
                "Booster pump & SMPS power supply check",
                "Filter housing high-pressure flush"
            ),
            whatIsNotIncluded = listOf(
                "Cost of new RO Membrane / Copper filter (charged on actuals if replaced)"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_ro_check",
                    name = "RO Routine Check & Tank Clean",
                    description = "TDS test, pressure check and internal tank wash",
                    price = 299,
                    originalPrice = 450,
                    durationText = "30 mins",
                    includes = listOf("TDS Analysis", "Tank Wash", "Pump Check", "Leak Test")
                ),
                ServicePackage(
                    id = "pkg_ro_full_service",
                    name = "Complete Filter Replacement Combo",
                    description = "Includes new sediment filter, carbon block & post-carbon candle",
                    price = 899,
                    originalPrice = 1350,
                    durationText = "50 mins",
                    includes = listOf("3 Filter Cartridges", "Tank Sanitize", "TDS Calibration")
                )
            )
        ),

        // ================= 12. APPLIANCE REPAIR (cat_appliances) =================
        ServiceItem(
            id = "appliance_washing_machine",
            categoryId = "cat_appliances",
            name = "Washing Machine Repair & Servicing",
            subtitle = "Fix spin vibration, water drain chokes, motor issues & PCB errors",
            startingPrice = 349,
            rating = 4.83f,
            reviewsCount = 1430,
            duration = "45 mins",
            warrantyText = "30-Day Workmanship Warranty",
            description = "Expert diagnosis and repair for Front Load, Top Load, and Semi-Automatic machines of all brands (LG, Samsung, IFB, Whirlpool).",
            imageDrawableRes = R.drawable.img_washing_machine,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Complete doorstep fault diagnosis",
                "Drain filter and inlet valve cleaning",
                "Drum spin balance & belt tensioning",
                "Water level sensor & motor test"
            ),
            whatIsNotIncluded = listOf(
                "Cost of spare parts like drain pump, PCB or drive belt"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_wm_check",
                    name = "Diagnosis & Minor Repair",
                    description = "Drain choke fix, inlet filter clean and electrical diagnosis",
                    price = 349,
                    originalPrice = 499,
                    durationText = "45 mins",
                    includes = listOf("Full Checkup", "Drain Clean", "Balance Calibration")
                ),
                ServicePackage(
                    id = "pkg_wm_deep_clean",
                    name = "Drum Descaling & Deep Service",
                    description = "Removes calcium build-up, detergent residue and bad odor",
                    price = 699,
                    originalPrice = 999,
                    durationText = "60 mins",
                    includes = listOf("Descaling Treatment", "Tub Sanitization", "Pipe Flush")
                )
            )
        ),
        ServiceItem(
            id = "appliance_refrigerator",
            categoryId = "cat_appliances",
            name = "Refrigerator Cooling & Gas Refill",
            subtitle = "Fix no cooling, ice formation, thermostat failure & compressor noise",
            startingPrice = 349,
            rating = 4.86f,
            reviewsCount = 1180,
            duration = "45 mins",
            warrantyText = "60-Day Cooling Guarantee",
            description = "Specialized technicians for Single Door, Double Door, and Side-by-Side Inverter Frost-Free refrigerators.",
            imageDrawableRes = R.drawable.img_fridge_repair,
            isPopular = false,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Comprehensive cooling and compressor inspection",
                "Thermostat and defrost sensor calibration",
                "Condenser coil dust cleaning",
                "Door gasket magnetic seal check"
            ),
            whatIsNotIncluded = listOf(
                "Compressor replacement cost or new gas charge (charged if needed)"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_fridge_check",
                    name = "Doorstep Checkup & Service",
                    description = "Cooling diagnosis, coil cleaning and defrost test",
                    price = 349,
                    originalPrice = 499,
                    durationText = "45 mins",
                    includes = listOf("Cooling Diagnostic", "Coil Cleaning", "Seal Check")
                )
            )
        ),

        // ================= 13. SALON FOR WOMEN (cat_salon_w) =================
        ServiceItem(
            id = "salon_women_glow",
            categoryId = "cat_salon_w",
            name = "Glow Facial & Skin Radiance",
            subtitle = "Luxe skincare treatment with single-use sealed kits in your bedroom",
            startingPrice = 399,
            rating = 4.95f,
            reviewsCount = 4120,
            duration = "60 mins",
            warrantyText = "100% Sealed Monodose Kits",
            description = "Pamper yourself with salon luxury without stepping out into Agra traffic. Our certified beauticians arrive with sanitized bed sheets, portable ring lights, disposable gowns, and premium organic skincare lines.",
            imageDrawableRes = R.drawable.img_salon_wellness,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Deep pore cleansing, steam extraction & walnut exfoliator",
                "Relaxing 20-minute acupressure face, neck & shoulder massage",
                "Skin brightening peel-off mask & hydration toner",
                "Disposable bed roll, towel, headband & spatula"
            ),
            whatIsNotIncluded = listOf(
                "Hair coloring or permanent straightening (book separately)",
                "Medical dermatological procedures"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_salon_w1",
                    name = "Fruit Radiance Facial",
                    description = "Instant glow with antioxidant fruit extracts",
                    price = 399,
                    originalPrice = 599,
                    durationText = "45 mins",
                    includes = listOf("Cleansing", "Steam", "Massage", "Pack", "Lip Balm")
                ),
                ServicePackage(
                    id = "pkg_salon_w2",
                    name = "O3+ Bridal Glow Luxury Facial",
                    description = "Deep tan removal, diamond polish & youth boost",
                    price = 999,
                    originalPrice = 1499,
                    durationText = "75 mins",
                    includes = listOf("O3+ Radiance Kit", "Neck Therapy", "Under-Eye Treatment", "Ice Roller")
                )
            ),
            faqs = listOf(
                "How do you ensure hygiene?" to "All tools are UV-sterilized and every kit is opened directly in front of you from factory-sealed packs."
            )
        ),
        ServiceItem(
            id = "salon_women_waxing",
            categoryId = "cat_salon_w",
            name = "Honey & RICA Waxing at Home",
            subtitle = "Painless cartridged waxing with soothing post-wax aloe lotion",
            startingPrice = 349,
            rating = 4.92f,
            reviewsCount = 2840,
            duration = "45 mins",
            warrantyText = "Painless & Hygienic",
            description = "Gentle, residue-free waxing using Italian RICA wax or soothing Honey wax. Performed by trained female beauticians with disposable strips and pre/post oil care.",
            imageDrawableRes = R.drawable.img_threading,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Pre-wax cleansing and powder application",
                "Disposable wooden spatulas & non-woven strips",
                "Soothing coconut or chamomile oil post-wax massage",
                "Eyebrow & upper lip threading included in combo"
            ),
            whatIsNotIncluded = listOf("Laser hair removal"),
            packages = listOf(
                ServicePackage(
                    id = "pkg_wax_full",
                    name = "Full Arms + Full Legs + Underarms",
                    description = "Complete body smooth waxing with Honey wax",
                    price = 549,
                    originalPrice = 799,
                    durationText = "45 mins",
                    includes = listOf("Full Arms", "Full Legs", "Underarms", "Post-Wax Lotion")
                ),
                ServicePackage(
                    id = "pkg_wax_rica",
                    name = "RICA Italian Waxing (Full Body)",
                    description = "Premium chocolate / white chocolate RICA wax for sensitive skin",
                    price = 999,
                    originalPrice = 1499,
                    durationText = "60 mins",
                    includes = listOf("RICA Full Arms", "RICA Full Legs", "Underarms", "Threading")
                )
            )
        ),
        ServiceItem(
            id = "salon_women_manicure",
            categoryId = "cat_salon_w",
            name = "Luxury Manicure & Pedicure Spa",
            subtitle = "Bubble soak, cuticles cleanup, foot scrubbing & gel nail polish",
            startingPrice = 499,
            rating = 4.94f,
            reviewsCount = 1760,
            duration = "60 mins",
            warrantyText = "Single-Use Sealed Tools",
            description = "Give your hands and feet the pampering they deserve. Heated herbal water soak, dead skin callus filing, aromatic scrub, and shiny nail buffing.",
            imageDrawableRes = R.drawable.img_nail_art,
            isPopular = false,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Aromatherapy salt bubble bath soak",
                "Cuticle pushing, nail shaping and buffering",
                "Callus filing and walnut foot scrub",
                "10-min hydrating hand & leg cream massage",
                "Long-lasting gel nail lacquer finish"
            ),
            whatIsNotIncluded = listOf("Acrylic nail extensions (available on request)"),
            packages = listOf(
                ServicePackage(
                    id = "pkg_mani_pedi_duo",
                    name = "Classic Manicure + Pedicure Duo",
                    description = "Complete hand and foot pampering session",
                    price = 499,
                    originalPrice = 799,
                    durationText = "60 mins",
                    includes = listOf("Bubble Soak", "Cuticle Care", "Scrub", "Massage", "Nail Polish")
                )
            )
        ),
        ServiceItem(
            id = "salon_women_facial",
            categoryId = "cat_salon_w",
            name = "Ayurvedic Cleanup & Detan Pack",
            subtitle = "Quick refresh for sun-exposed skin with pure turmeric & saffron herbs",
            startingPrice = 299,
            rating = 4.87f,
            reviewsCount = 1450,
            duration = "35 mins",
            warrantyText = "100% Herbal Actives",
            description = "Quick and refreshing 35-minute cleanup designed for immediate removal of Agra summer sun tan and pollution impurities.",
            imageDrawableRes = R.drawable.img_facial_cleanup,
            isPopular = false,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Herbal cleansing & blackhead extraction",
                "Kumkumadi saffron detan cream massage",
                "Cooling clay pack & rose water toner"
            ),
            whatIsNotIncluded = listOf("Full body massage"),
            packages = listOf(
                ServicePackage(
                    id = "pkg_ayur_detan",
                    name = "Tan Removal Cleanup",
                    description = "Instant radiance and sun damage recovery",
                    price = 299,
                    originalPrice = 450,
                    durationText = "35 mins",
                    includes = listOf("Cleansing", "Scrub", "Detan Pack", "Rose Mist")
                )
            )
        ),

        // ================= 14. SALON FOR MEN (cat_salon_m) =================
        ServiceItem(
            id = "salon_men_grooming",
            categoryId = "cat_salon_m",
            name = "Men's Haircut, Beard Trim & Massage",
            subtitle = "Sanitized barber cape, precision scissor cut & relaxing head massage",
            startingPrice = 249,
            rating = 4.89f,
            reviewsCount = 2650,
            duration = "40 mins",
            warrantyText = "Sanitized Single-Use Cape",
            description = "Get a stylish haircut and sharp beard shape at home without waiting in crowded salon queues. Certified male stylists use UV-sterilized clippers and fresh disposable neck strips.",
            imageDrawableRes = R.drawable.img_haircut_styling,
            isPopular = true,
            isHeroFeatured = false,
            whatIsIncluded = listOf(
                "Consultation & customized fade/scissor haircut",
                "Beard line edging, shaping & trimmer fade",
                "Hot towel steam and aftershave splash",
                "10-min pressure-point head and shoulder massage"
            ),
            whatIsNotIncluded = listOf("Hair transplantation or permanent coloring"),
            packages = listOf(
                ServicePackage(
                    id = "pkg_men_cut_beard",
                    name = "Haircut + Beard Styling Combo",
                    description = "Trending haircut and sharp precision beard trim",
                    price = 249,
                    originalPrice = 399,
                    durationText = "40 mins",
                    includes = listOf("Haircut", "Beard Trim", "Head Massage", "Aftershave")
                ),
                ServicePackage(
                    id = "pkg_men_royal_groom",
                    name = "Royal Men's Grooming Package",
                    description = "Haircut, beard, detan face cleanup & 15-min head massage",
                    price = 499,
                    originalPrice = 799,
                    durationText = "60 mins",
                    includes = listOf("Haircut", "Beard Styling", "Face Detan Scrub", "Head Therapy")
                )
            )
        )
    )

    val professionals: List<Professional> = listOf(
        Professional(
            id = "pro_rajesh_1",
            name = "Rajesh Sharma",
            phone = "+91 ••••• •••••",
            specialty = "Master AC & Appliance Technician",
            rating = 4.92f,
            reviewsCount = 840,
            completedJobs = 1240,
            experienceYears = 8,
            isVerified = true,
            etaMinutes = 18,
            avatarInitials = "RS"
        ),
        Professional(
            id = "pro_amit_2",
            name = "Amit Kumar & Crew",
            phone = "+91 ••••• •••••",
            specialty = "Deep Home & Floor Care Specialist",
            rating = 4.89f,
            reviewsCount = 1120,
            completedJobs = 1860,
            experienceYears = 6,
            isVerified = true,
            etaMinutes = 25,
            avatarInitials = "AK"
        ),
        Professional(
            id = "pro_meera_3",
            name = "Meera Saxena",
            phone = "+91 ••••• •••••",
            specialty = "Certified Beautician & Skin Aesthetician",
            rating = 4.98f,
            reviewsCount = 650,
            completedJobs = 940,
            experienceYears = 7,
            isVerified = true,
            etaMinutes = 20,
            avatarInitials = "MS"
        ),
        Professional(
            id = "pro_dinesh_4",
            name = "Dinesh Verma",
            phone = "+91 ••••• •••••",
            specialty = "Licensed Residential Electrician",
            rating = 4.85f,
            reviewsCount = 980,
            completedJobs = 1520,
            experienceYears = 10,
            isVerified = true,
            etaMinutes = 14,
            avatarInitials = "DV"
        )
    )

    val offers: List<Offer> = listOf(
        Offer(
            code = "FIRST20",
            title = "20% OFF First Booking",
            discountDescription = "Get 20% instant discount up to ₹200 on any service for new customers in Agra.",
            percentageDiscount = 20,
            minOrderAmount = 399,
            validUntil = "30 Sep 2026"
        ),
        Offer(
            code = "CLEAN100",
            title = "₹100 Flat OFF Deep Cleaning",
            discountDescription = "Special festive discount on full home, kitchen or bathroom deep cleaning services.",
            flatDiscount = 100,
            minOrderAmount = 899,
            validUntil = "15 Oct 2026",
            categoryRestriction = "cat_cleaning"
        ),
        Offer(
            code = "GLOW50",
            title = "Flat ₹150 OFF Salon & Spa",
            discountDescription = "Pamper yourself with premium at-home facial & grooming rituals.",
            flatDiscount = 150,
            minOrderAmount = 699,
            validUntil = "31 Oct 2026",
            categoryRestriction = "cat_salon"
        ),
        Offer(
            code = "ACCOOL150",
            title = "₹150 OFF AC Jet Wash & Repair",
            discountDescription = "Deep foam jet cleaning, gas pressure check & anti-bacterial coil sanitization.",
            flatDiscount = 150,
            minOrderAmount = 499,
            validUntil = "30 Nov 2026",
            categoryRestriction = "cat_ac"
        ),
        Offer(
            code = "ELECT50",
            title = "₹50 OFF Quick Electrical Fix",
            discountDescription = "Instant discount on switchboard, fuse, wiring & fan installation in Agra.",
            flatDiscount = 50,
            minOrderAmount = 249,
            validUntil = "15 Nov 2026",
            categoryRestriction = "cat_electrician"
        ),
        Offer(
            code = "PEST25",
            title = "25% OFF Herbal Pest Control",
            discountDescription = "Odorless herbal treatment for cockroaches, ants & termites with 90-day warranty.",
            percentageDiscount = 25,
            minOrderAmount = 799,
            validUntil = "30 Nov 2026",
            categoryRestriction = "cat_pest"
        ),
        Offer(
            code = "AGRA200",
            title = "Mega Agra Super Voucher",
            discountDescription = "Special city voucher valid on any premium booking across Agra localities.",
            flatDiscount = 200,
            minOrderAmount = 999,
            validUntil = "31 Dec 2026"
        )
    )

    val supportedCities = listOf("Agra", "Mathura", "Delhi NCR", "Noida", "Gurgaon", "Jaipur", "Lucknow")
    val agraLocalities = listOf(
        "Taj Nagri Phase 2",
        "Fatehabad Road",
        "Sanjay Place Commercial Hub",
        "Dayalbagh",
        "Kamla Nagar",
        "Sadar Bazaar & Cantt",
        "Khandari",
        "Sikandra",
        "Trans Yamuna Colony",
        "Shahganj"
    )

    fun getServiceById(id: String): ServiceItem? = services.find { it.id == id }
    fun getServicesByCategory(catId: String): List<ServiceItem> = services.filter { it.categoryId == catId }
    fun getCategoryById(id: String): ServiceCategory? = categories.find { it.id == id }
    fun getProfessionalById(id: String): Professional? = professionals.find { it.id == id }
}
