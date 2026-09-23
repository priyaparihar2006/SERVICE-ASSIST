package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CustomerReview
import com.example.data.model.SavedAddress
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Booking::class,
        SavedAddress::class,
        CustomerReview::class,
        UserProfile::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ServoraDatabase : RoomDatabase() {
    abstract fun bookingDao(): BookingDao
    abstract fun addressDao(): AddressDao
    abstract fun reviewDao(): ReviewDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: ServoraDatabase? = null

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bookings ADD COLUMN paymentReference TEXT")
                db.execSQL("ALTER TABLE bookings ADD COLUMN paidAt INTEGER")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bookings ADD COLUMN cancellationReason TEXT")
                db.execSQL("ALTER TABLE bookings ADD COLUMN cancellationFeedback TEXT")
                db.execSQL("ALTER TABLE bookings ADD COLUMN cancelledAt INTEGER")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): ServoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ServoraDatabase::class.java,
                    "servora_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: ServoraDatabase) {
            val userDao = database.userDao()
            val addressDao = database.addressDao()
            val bookingDao = database.bookingDao()
            val reviewDao = database.reviewDao()

            // Only seed demo data if database is fresh and unpopulated
            if (userDao.getCurrentUserSync() != null) {
                return
            }

            // Seed User
            userDao.insertUser(
                UserProfile(
                    id = "user_priya_1",
                    name = "Priya Sharma",
                    phone = "+91 98765 43210",
                    email = "priya.sharma@example.com",
                    city = "Agra",
                    locality = "Taj Nagri Phase 2",
                    role = UserRole.CUSTOMER
                )
            )

            // Seed Saved Addresses in Agra
            addressDao.insertAddress(
                SavedAddress(
                    title = "Home",
                    fullAddress = "Flat 402, Royal Residency, Taj Nagri Phase 2",
                    locality = "Taj Nagri",
                    city = "Agra",
                    landmark = "Near Shilpgram, East Gate",
                    isDefault = true
                )
            )
            addressDao.insertAddress(
                SavedAddress(
                    title = "Office",
                    fullAddress = "Suite 305, Corporate Plaza, Sanjay Place",
                    locality = "Sanjay Place",
                    city = "Agra",
                    landmark = "Opposite LIC Building",
                    isDefault = false
                )
            )
            addressDao.insertAddress(
                SavedAddress(
                    title = "Parents",
                    fullAddress = "B-14, Dayalbagh Main Road",
                    locality = "Dayalbagh",
                    city = "Agra",
                    landmark = "Near Radhasoami Temple",
                    isDefault = false
                )
            )

            // Seed an active initial booking so the user can immediately test tracking & status timeline!
            bookingDao.insertBooking(
                Booking(
                    bookingCode = "SRV-84920",
                    serviceId = "ac_service_deep",
                    serviceName = "Intense AC Jet Service",
                    packageName = "1 Split AC Deep Jet Cleaning",
                    scheduledDate = "Today",
                    scheduledTime = "02:30 PM",
                    addressText = "Flat 402, Royal Residency, Taj Nagri Phase 2, Agra",
                    locality = "Taj Nagri",
                    city = "Agra",
                    totalAmount = 499,
                    discountAmount = 100,
                    promoCode = "FIRST20",
                    paymentMethod = "Cash after service",
                    isPaid = false,
                    status = BookingStatus.ON_THE_WAY,
                    professionalId = "pro_rajesh_1",
                    startOtp = "6824",
                    specialNotes = "Ring bell twice, indoor split unit in master bedroom"
                )
            )

            // Seed past completed booking
            bookingDao.insertBooking(
                Booking(
                    bookingCode = "SRV-73105",
                    serviceId = "deep_home_cleaning",
                    serviceName = "Complete Home Deep Cleaning",
                    packageName = "2 BHK Intensive Deep Clean",
                    scheduledDate = "12 Sep 2026",
                    scheduledTime = "10:00 AM",
                    addressText = "Flat 402, Royal Residency, Taj Nagri Phase 2, Agra",
                    locality = "Taj Nagri",
                    city = "Agra",
                    totalAmount = 1899,
                    discountAmount = 200,
                    promoCode = "CLEAN100",
                    paymentMethod = "UPI (Google Pay)",
                    isPaid = true,
                    status = BookingStatus.COMPLETED,
                    professionalId = "pro_amit_2",
                    startOtp = "3194"
                )
            )

            // Seed verified reviews from Agra customers
            reviewDao.insertReview(
                CustomerReview(
                    serviceId = "ac_service_deep",
                    serviceName = "Intense AC Jet Service",
                    professionalName = "Rajesh Sharma",
                    customerName = "Ananya V.",
                    rating = 5.0f,
                    comment = "Brilliant service! Rajesh brought proper foam jet pressure equipment. AC cooling is like brand new now, zero mess left on the wall.",
                    tags = "Punctual, Super Clean",
                    dateText = "2 days ago"
                )
            )
            reviewDao.insertReview(
                CustomerReview(
                    serviceId = "deep_home_cleaning",
                    serviceName = "Complete Home Deep Cleaning",
                    professionalName = "Amit Kumar & Team",
                    customerName = "Vikram Singhania",
                    rating = 4.9f,
                    comment = "Booked for our home in Fatehabad Road Agra before Diwali guests arrived. Every bathroom tile and kitchen chimney was scrubbed mirror-shine.",
                    tags = "Detail Oriented, Professional",
                    dateText = "Last week"
                )
            )
            reviewDao.insertReview(
                CustomerReview(
                    serviceId = "salon_women_glow",
                    serviceName = "Glow Facial & Mani-Pedi",
                    professionalName = "Meera Saxena",
                    customerName = "Pooja Agarwal",
                    rating = 5.0f,
                    comment = "Meera brought all sanitized disposable kits. The facial massage was so relaxing right in my living room. Will book again!",
                    tags = "Hygienic, Gentle",
                    dateText = "3 days ago"
                )
            )
        }
    }
}
