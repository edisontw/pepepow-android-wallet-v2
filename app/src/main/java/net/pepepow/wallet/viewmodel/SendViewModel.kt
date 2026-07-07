package net.pepepow.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.pepepow.wallet.data.WalletRepository
import net.pepepow.wallet.data.SendResult
import net.pepepow.wallet.domain.address.AddressValidator
import java.util.concurrent.atomic.AtomicBoolean

class SendViewModel(
    private val repository: WalletRepository
) : ViewModel() {

    private val _sendSuccess = MutableStateFlow<Boolean?>(null)
    val sendSuccess: StateFlow<Boolean?> = _sendSuccess.asStateFlow()

    private val _addressError = MutableStateFlow<String?>(null)
    val addressError: StateFlow<String?> = _addressError.asStateFlow()

    private val _amountError = MutableStateFlow<String?>(null)
    val amountError: StateFlow<String?> = _amountError.asStateFlow()

    private val _sendResult = MutableStateFlow<SendResult?>(null)
    val sendResult: StateFlow<SendResult?> = _sendResult.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _sendProgress = MutableStateFlow<String?>(null)
    val sendProgress: StateFlow<String?> = _sendProgress.asStateFlow()

    private val sendInFlight = AtomicBoolean(false)

    private fun pepesToAtomic(amountStr: String): Long {
        val parts = amountStr.trim().split('.')
        if (parts.size > 2) throw IllegalArgumentException("Invalid decimal format")
        val wholeStr = parts[0]
        val whole = (if (wholeStr.isEmpty()) 0L else wholeStr.toLongOrNull())
            ?: throw IllegalArgumentException("Invalid integer part")
        if (whole < 0) throw IllegalArgumentException("Amount cannot be negative")
        val fractionStr = parts.getOrNull(1) ?: ""
        if (fractionStr.length > 8) {
            throw IllegalArgumentException("Too many decimal places (maximum 8 allowed)")
        }
        val fraction = fractionStr.padEnd(8, '0').toLongOrNull()
            ?: throw IllegalArgumentException("Invalid fractional part")
        return whole * 100_000_000L + fraction
    }

    fun sendPepew(recipientAddress: String, amountStr: String) {
        if (!sendInFlight.compareAndSet(false, true)) {
            return
        }

        _addressError.value = null
        _amountError.value = null
        _sendSuccess.value = null
        _sendResult.value = null
        _sendProgress.value = null

        val addressValidation = AddressValidator.validateAddress(recipientAddress)
        var valid = true

        if (recipientAddress.isBlank()) {
            _addressError.value = "Address is required"
            valid = false
        } else {
            when (addressValidation) {
                AddressValidator.AddressValidationResult.InvalidAddress -> {
                    _addressError.value = "Invalid PEPEW address format"
                    valid = false
                }
                AddressValidator.AddressValidationResult.UnsupportedAddressType -> {
                    _addressError.value = "Unsupported address type (P2SH not supported)"
                    valid = false
                }
                AddressValidator.AddressValidationResult.ValidP2PKH -> {
                    // Valid.
                }
            }
        }

        var amountAtoms = 0L
        if (amountStr.isBlank()) {
            _amountError.value = "Amount is required"
            valid = false
        } else {
            try {
                amountAtoms = pepesToAtomic(amountStr)
                if (amountAtoms <= 0) {
                    _amountError.value = "Please enter a valid amount greater than 0"
                    valid = false
                }
            } catch (e: IllegalArgumentException) {
                _amountError.value = e.message ?: "Invalid amount format"
                valid = false
            }
        }

        if (valid) {
            val balanceAtoms = Math.round(repository.balance.value * 1e8)
            val feeAtoms = 100_000L // 0.001 PEPEW
            val requiredAtoms = amountAtoms + feeAtoms
            if (balanceAtoms < requiredAtoms) {
                _amountError.value = "Insufficient balance (Need amount + 0.001 fee)"
                _sendResult.value = SendResult.InsufficientFunds(
                    availableAtoms = balanceAtoms,
                    requiredAtoms = requiredAtoms,
                    feeAtoms = feeAtoms
                )
                valid = false
            }
        }

        if (!valid) {
            _sendSuccess.value = false
            sendInFlight.set(false)
            return
        }

        _isSending.value = true
        _sendProgress.value = "Preparing transaction..."
        viewModelScope.launch {
            try {
                val result = repository.sendTx(recipientAddress, amountAtoms) { progress ->
                    _sendProgress.value = progress
                }
                _sendResult.value = result
                _sendProgress.value = null

                when (result) {
                    is SendResult.Success -> {
                        _sendSuccess.value = true
                        _sendProgress.value = "Refreshing wallet..."
                        try {
                            repository.refreshWalletData(force = true)
                        } catch (_: Exception) {
                            // Keep send success visible even if the immediate post-broadcast refresh is delayed.
                        } finally {
                            _sendProgress.value = null
                        }

                        // Schedule delayed refreshes after broadcast so mempool/API state can catch up.
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(12_000)
                            try {
                                repository.refreshWalletData(force = true)
                            } catch (_: Exception) {}
                        }
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(45_000)
                            try {
                                repository.refreshWalletData(force = true)
                            } catch (_: Exception) {}
                        }
                    }
                    else -> {
                        _sendSuccess.value = false
                        when (result) {
                            is SendResult.ValidationError -> {
                                _amountError.value = result.message
                            }
                            is SendResult.InsufficientFunds -> {
                                val availableP = result.availableAtoms / 1e8
                                val requiredP = result.requiredAtoms / 1e8
                                val feeP = result.feeAtoms / 1e8
                                _amountError.value = "Insufficient funds: available ${String.format("%.4f", availableP)} PEPEW, required ${String.format("%.4f", requiredP)} PEPEW (fee ${String.format("%.4f", feeP)} PEPEW)"
                            }
                            is SendResult.Blocked -> {
                                _amountError.value = "Send blocked: ${result.reason}"
                            }
                            is SendResult.ApiError -> {
                                _amountError.value = "API Error: ${result.message}"
                            }
                            is SendResult.Failure -> {
                                _amountError.value = "Send failed: ${result.message}"
                            }
                            else -> {
                                _amountError.value = "Send failed: check connection or balance"
                            }
                        }
                    }
                }
            } finally {
                _isSending.value = false
                _sendProgress.value = null
                sendInFlight.set(false)
            }
        }
    }

    fun resetSendState() {
        _sendSuccess.value = null
        _addressError.value = null
        _amountError.value = null
        _sendResult.value = null
        _isSending.value = false
        _sendProgress.value = null
    }
}
