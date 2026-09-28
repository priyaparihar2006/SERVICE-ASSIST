package com.example

import com.example.data.model.MessageStatus
import com.example.data.repository.FakeChatRepository
import com.example.data.repository.FakeChatStore
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeChatRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        FakeChatStore.resetToDefault()
        fakeRepository = FakeChatRepository("user_priya_1", "CUSTOMER")
        viewModel = ChatViewModel(fakeRepository, enablePolling = false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadConversations_populatesSummariesAndNewestFirst() = runTest {
        advanceUntilIdle()

        val convs = viewModel.conversations.value
        assertTrue("Should have initial seeded conversations", convs.isNotEmpty())
        assertEquals("conv_ac_rajesh_1", convs[0].id)
        assertTrue("Unread total should reflect conversation unread count", viewModel.unreadTotal.value > 0)
    }

    @Test
    fun testOpenThread_loadsMessagesAndClearsUnread() = runTest {
        advanceUntilIdle()

        viewModel.openThread("conv_ac_rajesh_1")
        advanceUntilIdle()

        val active = viewModel.activeThreadMessages.value
        assertTrue("Messages should be loaded", active.size >= 4)
        assertEquals("conv_ac_rajesh_1", viewModel.activeConversationId.value)
    }

    @Test
    fun testSendMessage_optimisticAndSuccessTransition() = runTest {
        advanceUntilIdle()
        viewModel.openThread("conv_ac_rajesh_1")
        advanceUntilIdle()

        val initialCount = viewModel.activeThreadMessages.value.size
        viewModel.sendMessage("conv_ac_rajesh_1", "Please ring the bell twice")

        // Optimistic check
        val optimisticMessages = viewModel.activeThreadMessages.value
        assertEquals(initialCount + 1, optimisticMessages.size)
        assertEquals("Please ring the bell twice", optimisticMessages.last().text)
        assertEquals(MessageStatus.SENDING, optimisticMessages.last().status)

        advanceUntilIdle()

        val sentMessages = viewModel.activeThreadMessages.value
        val lastMsg = sentMessages.last()
        assertEquals("Please ring the bell twice", lastMsg.text)
        assertTrue(lastMsg.status == MessageStatus.SENT || lastMsg.status == MessageStatus.DELIVERED)
    }

    @Test
    fun testSendMessage_contactSharingBlocked() = runTest {
        advanceUntilIdle()
        viewModel.openThread("conv_ac_rajesh_1")
        advanceUntilIdle()

        val initialCount = viewModel.activeThreadMessages.value.size
        viewModel.sendMessage("conv_ac_rajesh_1", "Call me at 9876543210")

        // Should be blocked before being sent or inserted
        val currentMessages = viewModel.activeThreadMessages.value
        assertEquals(initialCount, currentMessages.size)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("safety", ignoreCase = true))
    }

    @Test
    fun testSessionSwitching_updatesConversationsForPartner() = runTest {
        advanceUntilIdle()

        // Switch to Rajesh (Partner)
        viewModel.onSessionChanged("pro_rajesh_1", "PARTNER")
        advanceUntilIdle()

        val partnerConvs = viewModel.conversations.value
        assertTrue("Partner should see assigned conversation", partnerConvs.isNotEmpty())
        assertEquals("conv_ac_rajesh_1", partnerConvs[0].id)
        assertTrue("Counterpart should display client name", partnerConvs[0].counterpartName.contains("Priya"))
    }

    @Test
    fun testOnSessionChanged_clearsAllState() = runTest {
        advanceUntilIdle()
        viewModel.openThread("conv_ac_rajesh_1")
        advanceUntilIdle()
        assertTrue(viewModel.activeThreadMessages.value.isNotEmpty())

        viewModel.onSessionChanged("unknown_user", "CUSTOMER")
        advanceUntilIdle()

        assertEquals(0, viewModel.conversations.value.size)
        assertEquals(0, viewModel.activeThreadMessages.value.size)
        assertEquals(null, viewModel.activeConversationId.value)
        assertEquals(0, viewModel.unreadTotal.value)
    }

    @Test
    fun testSendMessage_deduplicatesOptimisticMessage() = runTest {
        advanceUntilIdle()
        viewModel.openThread("conv_ac_rajesh_1")
        advanceUntilIdle()

        val countBefore = viewModel.activeThreadMessages.value.size
        viewModel.sendMessage("conv_ac_rajesh_1", "Testing deduplication")

        // Should have 1 more message (optimistic)
        assertEquals(countBefore + 1, viewModel.activeThreadMessages.value.size)
        advanceUntilIdle()

        // After server returns, optimistic message is replaced by server msg with matching client UUID, no duplicate items
        val finalMessages = viewModel.activeThreadMessages.value
        assertEquals(countBefore + 1, finalMessages.size)
        val distinctIds = finalMessages.map { it.id }.toSet()
        assertEquals(finalMessages.size, distinctIds.size)
    }

    @Test
    fun testActiveAndRecentConversations_segregatesCorrectly() = runTest {
        advanceUntilIdle()

        val allConvs = viewModel.conversations.value
        val active = allConvs.filter { it.status == "ACTIVE" }
        val recent = allConvs.filter { it.status != "ACTIVE" }

        assertTrue("Should have at least one active conversation", active.isNotEmpty())
        assertTrue("Should have at least one recent/closed conversation", recent.isNotEmpty())
        assertEquals("conv_ac_rajesh_1", active[0].id)
        assertEquals("conv_cleaning_amit_2", recent[0].id)
        assertEquals("READ_ONLY", recent[0].status)
    }

    @Test
    fun test1to1ChatArchitecture_AllScenarios() = runTest {
        advanceUntilIdle()

        // -------------------------------------------------------------
        // SCENARIO 1: Customer A books "AC Repair" with Partner X (Rajesh)
        // -> Chat room #1 is active with history
        // -------------------------------------------------------------
        val initialConvs = viewModel.conversations.value
        val initialRajeshConv = initialConvs.find { it.counterpartName.contains("Rajesh") }
        assertNotNull("Rajesh conversation should exist", initialRajeshConv)
        val rajeshConvId = initialRajeshConv!!.id

        viewModel.openThread(rajeshConvId)
        advanceUntilIdle()
        val messagesBeforeNewBooking = viewModel.activeThreadMessages.value
        val initialMessageCount = messagesBeforeNewBooking.size
        assertTrue("Initial messages should be present", initialMessageCount >= 4)

        // Customer sends a message prior to the second booking
        viewModel.sendMessage(rajeshConvId, "Thank you for the AC repair earlier!")
        advanceUntilIdle()
        val countAfterUserMsg = viewModel.activeThreadMessages.value.size
        assertEquals(initialMessageCount + 1, countAfterUserMsg)

        // -------------------------------------------------------------
        // SCENARIO 2: Customer A books "Plumbing Service" with SAME Partner X (Rajesh)
        // -> NO new chat item in inbox. Existing Chat room is updated and brought to top.
        // -------------------------------------------------------------
        val secondBookingRajesh = com.example.data.model.Booking(
            id = 202L,
            bookingCode = "SRV-55442",
            customerId = "user_priya_1",
            customerName = "Priya Sharma",
            serviceId = "plumbing_drain_cleaning",
            serviceName = "Drain & Pipe Clog Clearance",
            packageName = "Express Plumbing",
            scheduledDate = "Tomorrow",
            scheduledTime = "04:00 PM",
            addressText = "Taj Nagri Phase 2, Agra",
            locality = "Taj Nagri Phase 2",
            city = "Agra",
            totalAmount = 499,
            status = com.example.data.model.BookingStatus.ASSIGNED,
            professionalId = "pro_rajesh_1",
            createdAt = System.currentTimeMillis() + 5000L
        )

        FakeChatStore.syncFromBookings(listOf(secondBookingRajesh), "user_priya_1", "CUSTOMER")
        viewModel.loadConversations()
        advanceUntilIdle()

        val updatedConvs = viewModel.conversations.value
        // Only 1 conversation for Rajesh in inbox!
        val rajeshConvs = updatedConvs.filter { it.counterpartName.contains("Rajesh") }
        assertEquals("Inbox must show only ONE conversation per partner", 1, rajeshConvs.size)

        // Brought to the top of inbox list
        assertEquals("Rajesh conversation should be at the top of inbox", rajeshConvId, updatedConvs[0].id)
        assertEquals("Active booking code should be updated", "SRV-55442", updatedConvs[0].bookingCode)
        assertEquals("Active service should be updated", "Drain & Pipe Clog Clearance", updatedConvs[0].serviceName)

        // -------------------------------------------------------------
        // SCENARIO 3: Messages sent prior to second booking remain intact
        // + new system card "New booking accepted: Drain & Pipe Clog Clearance..."
        // + pro confirmation message for the active chat
        // -------------------------------------------------------------
        viewModel.openThread(rajeshConvId)
        advanceUntilIdle()

        val messagesAfterSecondBooking = viewModel.activeThreadMessages.value
        assertTrue("Message count should include system update and pro confirmation message", messagesAfterSecondBooking.size == countAfterUserMsg + 2)

        // Verify past messages intact
        val pastMsgFound = messagesAfterSecondBooking.any { it.text == "Thank you for the AC repair earlier!" }
        assertTrue("Past messages sent prior to second booking must remain intact", pastMsgFound)

        // Verify system card
        val systemCardMsg = messagesAfterSecondBooking[messagesAfterSecondBooking.size - 2]
        assertEquals("BOOKING_UPDATE", systemCardMsg.kind)
        assertEquals("SYSTEM", systemCardMsg.senderRole)
        assertTrue("System card text should contain new service name", systemCardMsg.text.contains("Drain & Pipe Clog Clearance"))
        assertTrue("System card text should contain new booking ID", systemCardMsg.text.contains("SRV-55442"))

        // Verify pro confirmation message
        val proConfirmationMsg = messagesAfterSecondBooking.last()
        assertEquals("TEXT", proConfirmationMsg.kind)
        assertEquals("PARTNER", proConfirmationMsg.senderRole)
        assertTrue("Pro confirmation text should contain pro intro", proConfirmationMsg.text.contains("Rajesh Sharma"))
        assertTrue("Pro confirmation text should contain service name", proConfirmationMsg.text.contains("Drain & Pipe Clog Clearance"))

        // -------------------------------------------------------------
        // SCENARIO 4: Customer A books with Partner Y (Dinesh)
        // -> Creates a separate chat room for Partner Y in Active Chats
        // -------------------------------------------------------------
        val bookingPartnerY = com.example.data.model.Booking(
            id = 303L,
            bookingCode = "SRV-77883",
            customerId = "user_priya_1",
            customerName = "Priya Sharma",
            serviceId = "electrician_fan_repair",
            serviceName = "Ceiling Fan Repair & Installation",
            packageName = "Standard",
            scheduledDate = "Friday",
            scheduledTime = "11:00 AM",
            addressText = "Taj Nagri Phase 2, Agra",
            locality = "Taj Nagri Phase 2",
            city = "Agra",
            totalAmount = 299,
            status = com.example.data.model.BookingStatus.ASSIGNED,
            professionalId = "pro_dinesh_4",
            createdAt = System.currentTimeMillis() + 10000L
        )

        FakeChatStore.syncFromBookings(listOf(bookingPartnerY), "user_priya_1", "CUSTOMER")
        viewModel.loadConversations()
        advanceUntilIdle()

        val finalConvs = viewModel.conversations.value
        val dineshConv = finalConvs.find { it.counterpartName.contains("Dinesh") }
        assertNotNull("Separate chat room for Partner Y (Dinesh) must be created", dineshConv)
        assertTrue("Dinesh room ID should be distinct from Rajesh room ID", dineshConv!!.id != rajeshConvId)
        assertEquals("conv_user_priya_1_pro_dinesh_4", dineshConv.id)
        assertEquals("ACTIVE", dineshConv.status)

        // -------------------------------------------------------------
        // SCENARIO 5: Completing a booking moves its chat to Recent Chats
        // and only active chats count towards notifications
        // -------------------------------------------------------------
        val completedBooking = bookingPartnerY.copy(status = com.example.data.model.BookingStatus.COMPLETED)
        FakeChatStore.syncFromBookings(listOf(completedBooking), "user_priya_1", "CUSTOMER")
        viewModel.loadConversations()
        advanceUntilIdle()

        val convsAfterComplete = viewModel.conversations.value
        val dineshCompletedConv = convsAfterComplete.find { it.id == "conv_user_priya_1_pro_dinesh_4" }
        assertNotNull(dineshCompletedConv)
        assertEquals("READ_ONLY", dineshCompletedConv!!.status)

        val activeList = convsAfterComplete.filter { it.status == "ACTIVE" }
        val recentList = convsAfterComplete.filter { it.status != "ACTIVE" }

        assertTrue("Dinesh chat should now be in Recent Chats", recentList.any { it.id == "conv_user_priya_1_pro_dinesh_4" })
        assertTrue("Dinesh chat should NOT be in Active Chats", activeList.none { it.id == "conv_user_priya_1_pro_dinesh_4" })
        assertEquals("Unread total should only include active chats", activeList.sumOf { it.unreadCount }, viewModel.unreadTotal.value)
    }
}
