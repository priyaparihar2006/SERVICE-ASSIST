package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.ServoraDatabase
import com.example.data.repository.ServoraRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Service Assist", appName)
    }

    @Test
    fun `repository has verified services and categories in Agra`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = ServoraDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
        val repository = ServoraRepository(
            bookingDao = database.bookingDao(),
            addressDao = database.addressDao(),
            reviewDao = database.reviewDao(),
            userDao = database.userDao()
        )

        assertTrue(repository.categories.isNotEmpty())
        assertTrue(repository.services.isNotEmpty())
        assertTrue(repository.agraLocalities.contains("Taj Nagri Phase 2"))

        val acService = repository.services.find { it.id == "ac_service_deep" }
        assertNotNull(acService)
        assertEquals("Intense AC Foam Jet Service", acService?.name)
    }
}
