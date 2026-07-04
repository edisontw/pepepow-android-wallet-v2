package net.pepepow.wallet.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import net.pepepow.wallet.data.ApiState
import net.pepepow.wallet.data.WalletRepository

class WalletViewModel(
    private val repository: WalletRepository
) : ViewModel() {

    val balance: StateFlow<Double> = repository.balance
    val address: StateFlow<String> = repository.address
    val apiState: StateFlow<ApiState> = repository.apiState
    val mnemonic: StateFlow<String?> = repository.mnemonic
    val isWalletCreated: StateFlow<Boolean> = repository.isWalletCreated

    fun startCreateWallet() {
        repository.createWallet()
    }

    fun confirmBackup() {
        repository.confirmBackup()
    }

    fun clearWallet() {
        repository.clearWallet()
    }
}
