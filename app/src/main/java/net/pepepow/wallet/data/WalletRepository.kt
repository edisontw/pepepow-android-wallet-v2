package net.pepepow.wallet.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * API status values used by the Phase 1 mock wallet UI.
 * These are simulated states only. No real network request is made in Phase 1.
 */
enum class ApiState {
    CONNECTED,
    READY,
    FAILED
}

/**
 * Mock transaction model for Phase 1 UI/history rendering.
 * This is not a decoded blockchain transaction.
 */
data class Transaction(
    val txId: String,
    val address: String,
    val amount: Double,
    val timestamp: Long,
    val isSend: Boolean,
    val isPending: Boolean = false
)

/**
 * Repository boundary for wallet state.
 * Phase 1 must only use FakeWalletRepository.
 */
interface WalletRepository {
    val balance: StateFlow<Double>
    val address: StateFlow<String>
    val apiState: StateFlow<ApiState>
    val mnemonic: StateFlow<String?>
    val isWalletCreated: StateFlow<Boolean>
    val transactions: StateFlow<List<Transaction>>

    fun createWallet()
    fun confirmBackup()
    fun clearWallet()
    fun sendTx(recipientAddress: String, amount: Double): Boolean
    suspend fun retryConnection()
    fun setApiState(state: ApiState)
}

/**
 * The only active data source for Phase 1.
 *
 * Important boundaries:
 * - No real mnemonic generation.
 * - No private key derivation.
 * - No signing.
 * - No UTXO selection.
 * - No broadcast.
 * - No real API call.
 */
class FakeWalletRepository : WalletRepository {
    private val _balance = MutableStateFlow(12345.6789)
    override val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _address = MutableStateFlow("PExamplePepepowAddress123456789")
    override val address: StateFlow<String> = _address.asStateFlow()

    private val _apiState = MutableStateFlow(ApiState.READY)
    override val apiState: StateFlow<ApiState> = _apiState.asStateFlow()

    private val _mnemonic = MutableStateFlow<String?>(null)
    override val mnemonic: StateFlow<String?> = _mnemonic.asStateFlow()

    private val _isWalletCreated = MutableStateFlow(false)
    override val isWalletCreated: StateFlow<Boolean> = _isWalletCreated.asStateFlow()

    private val _transactions = MutableStateFlow(mockTransactions())
    override val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    override fun createWallet() {
        _mnemonic.value = FAKE_MNEMONIC
        _address.value = "PExamplePepepowAddress123456789"
        _balance.value = 12345.6789
        _apiState.value = ApiState.READY
    }

    override fun confirmBackup() {
        _isWalletCreated.value = true
    }

    override fun clearWallet() {
        _mnemonic.value = null
        _isWalletCreated.value = false
        _address.value = "PExamplePepepowAddress123456789"
        _balance.value = 12345.6789
        _transactions.value = mockTransactions()
        _apiState.value = ApiState.READY
    }

    override fun sendTx(recipientAddress: String, amount: Double): Boolean {
        val fee = 0.001
        val total = amount + fee
        if (amount <= 0.0 || total > _balance.value) return false

        _balance.value -= total
        val pendingTx = Transaction(
            txId = "mock_pending_${System.currentTimeMillis()}",
            address = recipientAddress,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            isSend = true,
            isPending = true
        )
        _transactions.value = listOf(pendingTx) + _transactions.value
        return true
    }

    override suspend fun retryConnection() {
        _apiState.value = ApiState.CONNECTED
        delay(300)
        _apiState.value = ApiState.READY
    }

    override fun setApiState(state: ApiState) {
        _apiState.value = state
    }

    private fun mockTransactions(): List<Transaction> {
        val now = System.currentTimeMillis()
        return listOf(
            Transaction(
                txId = "mock_receive_001",
                address = "PExampleSenderAddress123456789",
                amount = 500.0,
                timestamp = now - 86_400_000L,
                isSend = false
            ),
            Transaction(
                txId = "mock_send_001",
                address = "PExampleRecipientAddress987654321",
                amount = 25.5,
                timestamp = now - 172_800_000L,
                isSend = true
            )
        )
    }

    companion object {
        private const val FAKE_MNEMONIC =
            "pepe frog wallet mock phase one seed words only demo safe never real"
    }
}
