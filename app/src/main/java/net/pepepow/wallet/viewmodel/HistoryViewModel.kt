package net.pepepow.wallet.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import net.pepepow.wallet.data.Transaction
import net.pepepow.wallet.data.WalletRepository

class HistoryViewModel(
    private val repository: WalletRepository
) : ViewModel() {
    val transactions: StateFlow<List<Transaction>> = repository.transactions
}
