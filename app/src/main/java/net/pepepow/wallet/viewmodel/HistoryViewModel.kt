package net.pepepow.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.pepepow.wallet.data.Transaction
import net.pepepow.wallet.data.WalletRepository

class HistoryViewModel(
    private val repository: WalletRepository
) : ViewModel() {
    val transactions: StateFlow<List<Transaction>> = repository.transactions
    val apiMessage: StateFlow<String> = repository.apiMessage
    val isApiLoading: StateFlow<Boolean> = repository.isApiLoading

    fun refresh() {
        viewModelScope.launch {
            repository.refreshWalletData()
        }
    }
}
