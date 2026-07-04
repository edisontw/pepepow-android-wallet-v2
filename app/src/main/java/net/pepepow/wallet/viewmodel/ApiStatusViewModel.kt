package net.pepepow.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.pepepow.wallet.data.ApiState
import net.pepepow.wallet.data.WalletRepository

class ApiStatusViewModel(
    private val repository: WalletRepository
) : ViewModel() {

    val apiState: StateFlow<ApiState> = repository.apiState

    fun retryApiConnection() {
        viewModelScope.launch {
            repository.retryConnection()
        }
    }

    fun simulateApiFailure() {
        repository.setApiState(ApiState.FAILED)
    }
}
