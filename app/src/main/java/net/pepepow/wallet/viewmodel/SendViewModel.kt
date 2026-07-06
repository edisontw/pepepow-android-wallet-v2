package net.pepepow.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.pepepow.wallet.data.WalletRepository
import net.pepepow.wallet.domain.address.AddressValidator

class SendViewModel(
    private val repository: WalletRepository
) : ViewModel() {

    private val _sendSuccess = MutableStateFlow<Boolean?>(null)
    val sendSuccess: StateFlow<Boolean?> = _sendSuccess.asStateFlow()

    private val _addressError = MutableStateFlow<String?>(null)
    val addressError: StateFlow<String?> = _addressError.asStateFlow()

    private val _amountError = MutableStateFlow<String?>(null)
    val amountError: StateFlow<String?> = _amountError.asStateFlow()

    fun sendPepew(recipientAddress: String, amountStr: String) {
        _addressError.value = null
        _amountError.value = null
        _sendSuccess.value = null

        val amount = amountStr.toDoubleOrNull()
        var valid = true

        if (recipientAddress.isBlank()) {
            _addressError.value = "Address is required"
            valid = false
        } else if (!AddressValidator.isValidAddress(recipientAddress)) {
            _addressError.value = "Invalid PEPEW address format"
            valid = false
        }

        if (amountStr.isBlank()) {
            _amountError.value = "Amount is required"
            valid = false
        } else if (amount == null || amount <= 0) {
            _amountError.value = "Please enter a valid amount greater than 0"
            valid = false
        } else if (amount + 0.001 > repository.balance.value) {
            _amountError.value = "Insufficient balance (Need amount + 0.001 fee)"
            valid = false
        }

        if (valid && amount != null) {
            viewModelScope.launch {
                val success = repository.sendTx(recipientAddress, amount)
                if (!success) {
                    _amountError.value = "Send failed: check connection or balance"
                }
                _sendSuccess.value = success
            }
        } else {
            _sendSuccess.value = false
        }
    }

    fun resetSendState() {
        _sendSuccess.value = null
        _addressError.value = null
        _amountError.value = null
    }
}
