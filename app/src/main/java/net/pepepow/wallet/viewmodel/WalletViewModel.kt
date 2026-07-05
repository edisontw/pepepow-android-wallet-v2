package net.pepepow.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
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

    fun refreshWalletData() {
        viewModelScope.launch {
            repository.refreshWalletData()
        }
    }
}
