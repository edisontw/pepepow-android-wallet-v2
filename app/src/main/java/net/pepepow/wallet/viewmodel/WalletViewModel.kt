package net.pepepow.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.pepepow.wallet.data.ApiState
import net.pepepow.wallet.data.WalletRepository

class WalletViewModel(
    private val repository: WalletRepository
) : ViewModel() {

    val balance: StateFlow<Double> = repository.balance
    val address: StateFlow<String> = repository.address
    val apiState: StateFlow<ApiState> = repository.apiState
    val apiMessage: StateFlow<String> = repository.apiMessage
    val isApiLoading: StateFlow<Boolean> = repository.isApiLoading
    val mnemonic: StateFlow<String?> = repository.mnemonic
    val isWalletCreated: StateFlow<Boolean> = repository.isWalletCreated
    val isApiMode: StateFlow<Boolean> = repository.isApiMode

    fun setApiMode(enabled: Boolean) {
        repository.setApiMode(enabled)
        refreshWalletData()
    }

    private val _restoreError = MutableStateFlow<String?>(null)
    val restoreError: StateFlow<String?> = _restoreError.asStateFlow()

    init {
        refreshWalletData()
    }

    fun startCreateWallet() {
        repository.createWallet()
        refreshWalletData()
    }

    fun confirmBackup() {
        repository.confirmBackup()
        refreshWalletData()
    }

    fun clearWallet() {
        repository.clearWallet()
    }

    fun requestMockFaucet() {
        repository.requestMockFaucet()
        refreshWalletData()
    }

    fun refreshWalletData() {
        viewModelScope.launch {
            repository.refreshWalletData()
        }
    }

    fun restoreWallet(mnemonic: String): Boolean {
        val trimmed = mnemonic.trim()
        if (trimmed.isEmpty()) {
            _restoreError.value = "Recovery phrase must contain exactly 12 words."
            return false
        }
        val words = trimmed.split("\\s+".toRegex())
        if (words.size != 12) {
            _restoreError.value = "Recovery phrase must contain exactly 12 words."
            return false
        }
        _restoreError.value = null
        repository.restoreWalletFromMnemonic(trimmed)
        refreshWalletData()
        return true
    }

    fun clearRestoreError() {
        _restoreError.value = null
    }
}
