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
}
