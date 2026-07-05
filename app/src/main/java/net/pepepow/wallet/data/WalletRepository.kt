package net.pepepow.wallet.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * API status values used by the wallet UI.
 * Phase 2 maps real read-only API checks into these UI states.
 */
enum class ApiState {
    CONNECTED,
    READY,
    FAILED
}

/**
 * Transaction model for UI/history rendering.
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
 *
 * Phase 2 may read public address data from the PEPEW Light API, but it must not
 * derive keys, sign transactions, select UTXOs, or broadcast transactions.
 */
interface WalletRepository {
    val balance: StateFlow<Double>
    val address: StateFlow<String>
    val apiState: StateFlow<ApiState>
    val apiMessage: StateFlow<String>
    val isApiLoading: StateFlow<Boolean>
    val mnemonic: StateFlow<String?>
    val isWalletCreated: StateFlow<Boolean>
    val transactions: StateFlow<List<Transaction>>

    fun createWallet()
    fun confirmBackup()
    fun clearWallet()
    fun sendTx(recipientAddress: String, amount: Double): Boolean
    suspend fun retryConnection()
    suspend fun refreshWalletData()
    fun setApiState(state: ApiState)
}

/**
 * Offline mock data source retained for previews, fallback testing, and Phase 1 comparison.
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

    private val _apiMessage = MutableStateFlow("Mock wallet mode. No real API request is made.")
    override val apiMessage: StateFlow<String> = _apiMessage.asStateFlow()

    private val _isApiLoading = MutableStateFlow(false)
    override val isApiLoading: StateFlow<Boolean> = _isApiLoading.asStateFlow()

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
        _apiMessage.value = "Mock wallet ready."
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
        _apiMessage.value = "Mock wallet reset."
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
        _apiMessage.value = "Mock connection retry..."
        delay(300)
        _apiState.value = ApiState.READY
        _apiMessage.value = "Mock API ready."
    }

    override suspend fun refreshWalletData() {
        _apiMessage.value = "Mock wallet data already loaded."
    }

    override fun setApiState(state: ApiState) {
        _apiState.value = state
        _apiMessage.value = "Mock API state: $state"
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
            "pepe frog wallet mock phase one seed words demo safe never real"
    }
}

/**
 * Phase 2 read-only API repository.
 *
 * This repository uses a public demo address so the prototype can query balance/history
 * without implementing real mnemonic/private key/address derivation yet.
 */
class ReadOnlyApiWalletRepository(
    private val apiClient: PepewApiClient = PepewApiClient()
) : WalletRepository {
    private val _balance = MutableStateFlow(0.0)
    override val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _address = MutableStateFlow(DEMO_READ_ONLY_ADDRESS)
    override val address: StateFlow<String> = _address.asStateFlow()

    private val _apiState = MutableStateFlow(ApiState.CONNECTED)
    override val apiState: StateFlow<ApiState> = _apiState.asStateFlow()

    private val _apiMessage = MutableStateFlow("PEPEW Light API read-only mode. Tap API Status to refresh.")
    override val apiMessage: StateFlow<String> = _apiMessage.asStateFlow()

    private val _isApiLoading = MutableStateFlow(false)
    override val isApiLoading: StateFlow<Boolean> = _isApiLoading.asStateFlow()

    private val _mnemonic = MutableStateFlow<String?>(null)
    override val mnemonic: StateFlow<String?> = _mnemonic.asStateFlow()

    private val _isWalletCreated = MutableStateFlow(false)
    override val isWalletCreated: StateFlow<Boolean> = _isWalletCreated.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    override val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    override fun createWallet() {
        _mnemonic.value = FAKE_PHASE2_MNEMONIC
        _address.value = DEMO_READ_ONLY_ADDRESS
        _apiMessage.value = "Phase 2 read-only wallet created. Address data comes from PEPEW Light API."
    }

    override fun confirmBackup() {
        _isWalletCreated.value = true
    }

    override fun clearWallet() {
        _mnemonic.value = null
        _isWalletCreated.value = false
        _address.value = DEMO_READ_ONLY_ADDRESS
        _balance.value = 0.0
        _transactions.value = emptyList()
        _apiState.value = ApiState.CONNECTED
        _apiMessage.value = "Wallet reset. PEPEW Light API read-only mode remains enabled."
    }

    override fun sendTx(recipientAddress: String, amount: Double): Boolean {
        _apiMessage.value = "Send is disabled in Phase 2. Real signing/broadcast starts in Phase 3."
        return false
    }

    override suspend fun retryConnection() {
        _isApiLoading.value = true
        _apiState.value = ApiState.CONNECTED
        _apiMessage.value = "Checking PEPEW Light API..."
        try {
            val health = apiClient.getHealth()
            val status = apiClient.getStatus()
            _apiState.value = if (health.ok && status.ok) ApiState.READY else ApiState.CONNECTED
            _apiMessage.value = buildString {
                append("API reachable")
                status.height?.let { append(". Height: $it") }
                if (status.status.isNotBlank()) append(". Status: ${status.status}")
            }
            refreshWalletDataInternal(setLoading = false)
        } catch (e: Exception) {
            _apiState.value = ApiState.FAILED
            _apiMessage.value = friendlyError(e)
        } finally {
            _isApiLoading.value = false
        }
    }

    override suspend fun refreshWalletData() {
        refreshWalletDataInternal(setLoading = true)
    }

    override fun setApiState(state: ApiState) {
        _apiState.value = state
        _apiMessage.value = "Manual API state set to $state."
    }

    private suspend fun refreshWalletDataInternal(setLoading: Boolean) {
        if (setLoading) _isApiLoading.value = true
        try {
            val summary = apiClient.getAddressSummary(_address.value)
            val history = apiClient.getHistory(_address.value, limit = 50, offset = 0)
            _balance.value = summary.confirmedPepew + summary.unconfirmedPepew
            _transactions.value = (history.ifEmpty { summary.history }).map { it.toUiTransaction() }
            _apiState.value = ApiState.READY
            _apiMessage.value = "Read-only address data loaded from ${summary.source.ifBlank { "PEPEW Light API" }}."
        } catch (e: Exception) {
            _apiState.value = ApiState.FAILED
            _apiMessage.value = friendlyError(e)
        } finally {
            if (setLoading) _isApiLoading.value = false
        }
    }

    private fun ApiTransaction.toUiTransaction(): Transaction = Transaction(
        txId = txid,
        address = address.ifBlank { txid },
        amount = amount,
        timestamp = timestampMillis,
        isSend = isSend,
        isPending = isPending
    )

    private fun friendlyError(error: Exception): String = when (error) {
        is PepewApiException -> "API error ${error.statusCode}: ${error.message}"
        is java.net.SocketTimeoutException -> "API timeout. Please retry."
        is java.net.UnknownHostException -> "No network or DNS failure. Please retry."
        is UnsupportedOperationException -> error.message ?: "Unsupported in Phase 2."
        else -> error.message ?: "Unable to reach PEPEW Light API."
    }

    companion object {
        // Public example address from PEPEW Light wallet API documentation.
        private const val DEMO_READ_ONLY_ADDRESS = "PRfbEeHAKKbz6Voz85WJudrJwTA3ZbHunb"
        private const val FAKE_PHASE2_MNEMONIC =
            "pepe frog wallet phase two mock words read only api demo safe"
    }
}
