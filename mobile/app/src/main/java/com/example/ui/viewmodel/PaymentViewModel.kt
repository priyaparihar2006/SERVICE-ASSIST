package com.example.ui.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.BookingDao
import com.example.data.model.Booking
import com.example.data.repository.PaymentRepository
import com.example.util.UpiQr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PaymentMethodTab {
    CASH, UPI
}

sealed interface PaymentUiState {
    data object Idle : PaymentUiState
    data object Loading : PaymentUiState
    data class Error(
        val bookingId: Long,
        val booking: Booking?,
        val message: String
    ) : PaymentUiState
    data class Ready(
        val booking: Booking,
        val selectedTab: PaymentMethodTab,
        val upiPayeeVpa: String = "9105830551@upi",
        val upiPayeeName: String = "Servora",
        val qrPayload: String = "",
        val qrBitmap: ImageBitmap? = null,
        val utr: String = "",
        val utrError: String? = null,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null
    ) : PaymentUiState
    data class Success(
        val bookingId: Long,
        val method: String,
        val reference: String? = null
    ) : PaymentUiState
}

class PaymentViewModel(
    private val paymentRepository: PaymentRepository,
    private val bookingDao: BookingDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun loadBooking(bookingId: Long) {
        viewModelScope.launch {
            _uiState.value = PaymentUiState.Loading
            val booking = bookingDao.getBookingByIdSync(bookingId)
            if (booking == null) {
                _uiState.value = PaymentUiState.Error(
                    bookingId = bookingId,
                    booking = null,
                    message = "Booking #$bookingId not found locally"
                )
                return@launch
            }

            // Default tab: checkout preference suggestion
            val initialTab = if (booking.paymentMethod.contains("UPI", ignoreCase = true)) {
                PaymentMethodTab.UPI
            } else {
                PaymentMethodTab.CASH
            }

            // Unconditionally initialize payment via backend (reconciles server status & obtains fresh UPI payload)
            val initResult = paymentRepository.initPayment(bookingId)
            if (initResult.isFailure) {
                val errorMsg = initResult.exceptionOrNull()?.message ?: "Couldn't prepare payment — check your connection"
                _uiState.value = PaymentUiState.Error(
                    bookingId = bookingId,
                    booking = booking,
                    message = errorMsg
                )
                return@launch
            }

            val initData = initResult.getOrNull()
            val upiVpa = initData?.upiPayeeVpa ?: "9105830551@upi"
            val upiName = initData?.upiPayeeName ?: "Servora"
            val payload = initData?.qrPayload ?: UpiQr.buildUpiUri(upiVpa, upiName, booking.totalAmount, booking.bookingCode)
            val qrBitmap = UpiQr.generateQrImageBitmap(payload)

            _uiState.value = PaymentUiState.Ready(
                booking = booking,
                selectedTab = initialTab,
                upiPayeeVpa = upiVpa,
                upiPayeeName = upiName,
                qrPayload = payload,
                qrBitmap = qrBitmap
            )
        }
    }

    fun selectTab(tab: PaymentMethodTab) {
        val current = _uiState.value as? PaymentUiState.Ready ?: return
        if (current.selectedTab == tab) return
        _uiState.value = current.copy(selectedTab = tab)
    }

    fun onUtrChanged(newUtr: String) {
        val current = _uiState.value as? PaymentUiState.Ready ?: return
        val filtered = newUtr.filter { it.isDigit() }.take(12)
        val error = if (filtered.isNotEmpty() && filtered.length < 12) {
            "UTR must be 12 digits (${filtered.length}/12)"
        } else null

        _uiState.value = current.copy(
            utr = filtered,
            utrError = error,
            errorMessage = null
        )
    }

    fun collectCash() {
        val current = _uiState.value as? PaymentUiState.Ready ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSubmitting = true, errorMessage = null)
            val result = paymentRepository.collectCash(current.booking.id)
            if (result.isSuccess) {
                _uiState.value = PaymentUiState.Success(
                    bookingId = current.booking.id,
                    method = "CASH"
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to record cash collection"
                _uiState.value = current.copy(isSubmitting = false, errorMessage = errorMsg)
            }
        }
    }

    fun collectUpi() {
        val current = _uiState.value as? PaymentUiState.Ready ?: return
        if (current.utr.length != 12) {
            _uiState.value = current.copy(utrError = "Please enter a valid 12-digit UPI reference number (UTR)")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isSubmitting = true, errorMessage = null)
            val result = paymentRepository.collectUpi(current.booking.id, current.utr)
            if (result.isSuccess) {
                _uiState.value = PaymentUiState.Success(
                    bookingId = current.booking.id,
                    method = "UPI",
                    reference = current.utr
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to confirm UPI payment"
                _uiState.value = current.copy(isSubmitting = false, errorMessage = errorMsg)
            }
        }
    }

    fun clearError() {
        val current = _uiState.value as? PaymentUiState.Ready ?: return
        _uiState.value = current.copy(errorMessage = null)
    }

    fun reset() {
        _uiState.value = PaymentUiState.Idle
    }
}

class PaymentViewModelFactory(
    private val paymentRepository: PaymentRepository,
    private val bookingDao: BookingDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            return PaymentViewModel(paymentRepository, bookingDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
