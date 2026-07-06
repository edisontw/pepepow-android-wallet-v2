package net.pepepow.wallet.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.pepepow.wallet.domain.address.AddressEncoder
import net.pepepow.wallet.domain.address.PepepowNetworkParams
import net.pepepow.wallet.domain.keys.Bip32
import net.pepepow.wallet.domain.mnemonic.Bip39MnemonicService
import net.pepepow.wallet.domain.transaction.TransactionBuilder
import net.pepepow.wallet.domain.transaction.Utxo
import net.pepepow.wallet.security.EncryptedStorage
import org.json.JSONArray
import org.json.JSONObject

enum class ApiState {
    CONNECTED,
    READY,
    FAILED
}

data class Transaction(
    val txId: String,
    val address: String,
    val amount: Double,
    val timestamp: Long,
    val isSend: Boolean,
    val isPending: Boolean = false
)

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
    suspend fun sendTx(recipientAddress: String, amount: Double): Boolean
    suspend fun retryConnection()
    suspend fun refreshWalletData()
    fun setApiState(state: ApiState)
    fun restoreWalletFromMnemonic(mnemonic: String)
    fun requestMockFaucet()
    val isApiMode: StateFlow<Boolean>
    fun setApiMode(enabled: Boolean)
}

/**
 * Real production wallet repository implementing end-to-end Bitcoin/PEPEPOW signing,
 * key derivation, secure storage, and Light API integration.
 */
class RealWalletRepository(
    private val context: Context,
    private val apiClient: PepewApiClient = PepewApiClient()
) : WalletRepository {

    private val secureStorage = EncryptedStorage(context)
    private val mnemonicService = Bip39MnemonicService()

    private val _isWalletCreated = MutableStateFlow(secureStorage.isWalletCreated())
    override val isWalletCreated: StateFlow<Boolean> = _isWalletCreated.asStateFlow()

    private val _mnemonic = MutableStateFlow<String?>(secureStorage.getMnemonic())
    override val mnemonic: StateFlow<String?> = _mnemonic.asStateFlow()

    private val _address = MutableStateFlow(secureStorage.getAddress())
    override val address: StateFlow<String> = _address.asStateFlow()

    private val _balance = MutableStateFlow(0.0)
    override val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    override val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _apiState = MutableStateFlow(ApiState.CONNECTED)
    override val apiState: StateFlow<ApiState> = _apiState.asStateFlow()

    private val _apiMessage = MutableStateFlow("Tap API Status to connect.")
    override val apiMessage: StateFlow<String> = _apiMessage.asStateFlow()

    private val _isApiLoading = MutableStateFlow(false)
    override val isApiLoading: StateFlow<Boolean> = _isApiLoading.asStateFlow()

    // Real wallet is always in API mode
    override val isApiMode: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun setApiMode(enabled: Boolean) {}

    override fun createWallet() {
        try {
            val words = mnemonicService.generateMnemonic()
            val seed = mnemonicService.deriveSeed(words)
            val keyNode = Bip32.derivePath(seed, PepepowNetworkParams.DEFAULT_PATH)
            val derivedAddress = AddressEncoder.getAddressFromPrivateKey(keyNode.privateKey)

            secureStorage.saveMnemonic(words)
            secureStorage.saveAddress(derivedAddress)
            secureStorage.saveWalletCreated(false)

            _mnemonic.value = words
            _address.value = derivedAddress
            _isWalletCreated.value = false
            _balance.value = 0.0
            _transactions.value = emptyList()
            _apiMessage.value = "New wallet generated. Please back up your recovery phrase."
        } catch (e: Exception) {
            _apiMessage.value = "Failed to create wallet: ${e.message}"
        }
    }

    override fun restoreWalletFromMnemonic(mnemonic: String) {
        try {
            val trimmed = mnemonic.trim().replace("\\s+".toRegex(), " ")
            val seed = mnemonicService.deriveSeed(trimmed)
            val keyNode = Bip32.derivePath(seed, PepepowNetworkParams.DEFAULT_PATH)
            val derivedAddress = AddressEncoder.getAddressFromPrivateKey(keyNode.privateKey)

            secureStorage.saveMnemonic(trimmed)
            secureStorage.saveAddress(derivedAddress)
            secureStorage.saveWalletCreated(true)

            _mnemonic.value = trimmed
            _address.value = derivedAddress
            _isWalletCreated.value = true
            _balance.value = 0.0
            _transactions.value = emptyList()
            _apiMessage.value = "Wallet restored successfully."
        } catch (e: Exception) {
            _apiMessage.value = "Failed to restore wallet: ${e.message}"
        }
    }

    override fun confirmBackup() {
        secureStorage.saveWalletCreated(true)
        _isWalletCreated.value = true
    }

    override fun clearWallet() {
        secureStorage.clear()
        _mnemonic.value = null
        _address.value = ""
        _isWalletCreated.value = false
        _balance.value = 0.0
        _transactions.value = emptyList()
        _apiState.value = ApiState.CONNECTED
        _apiMessage.value = "Wallet reset."
    }

    override suspend fun sendTx(recipientAddress: String, amount: Double): Boolean = withContext(Dispatchers.IO) {
        val words = secureStorage.getMnemonic() ?: return@withContext false
        val sender = _address.value
        if (sender.isBlank()) return@withContext false

        try {
            _isApiLoading.value = true
            _apiMessage.value = "Fetching spendable UTXOs..."

            // 1. Fetch live UTXOs
            val apiUtxos = apiClient.getUtxos(sender)
            val utxos = apiUtxos.map { Utxo(it.txid, it.vout, it.satoshis, it.scriptPubKey) }

            _apiMessage.value = "Signing transaction locally..."
            // 2. Derive private key
            val seed = mnemonicService.deriveSeed(words)
            val keyNode = Bip32.derivePath(seed, PepepowNetworkParams.DEFAULT_PATH)

            // 3. Build and Sign Transaction
            val fee = 0.001 // PEPEW
            val rawHex = TransactionBuilder.createAndSignTransaction(
                privateKey = keyNode.privateKey,
                utxos = utxos,
                recipientAddress = recipientAddress,
                amountPepew = amount,
                feePepew = fee,
                senderAddress = sender
            )

            _apiMessage.value = "Broadcasting raw transaction..."
            // 4. Broadcast
            val txid = apiClient.broadcastTransaction(rawHex)

            // 5. Update local state
            val pendingTx = Transaction(
                txId = txid,
                address = recipientAddress,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                isSend = true,
                isPending = true
            )
            
            _transactions.value = listOf(pendingTx) + _transactions.value
            _apiState.value = ApiState.READY
            _apiMessage.value = "Transaction broadcasted successfully! TXID: $txid"
            true
        } catch (e: Exception) {
            _apiState.value = ApiState.FAILED
            _apiMessage.value = "Send failed: ${e.message ?: e.javaClass.simpleName}"
            false
        } finally {
            _isApiLoading.value = false
        }
    }

    override suspend fun retryConnection() {
        _isApiLoading.value = true
        _apiState.value = ApiState.CONNECTED
        _apiMessage.value = "Checking API status..."
        try {
            val health = apiClient.getHealth()
            val status = apiClient.getStatus()
            _apiState.value = if (health.ok && status.ok) ApiState.READY else ApiState.CONNECTED
            _apiMessage.value = buildString {
                append("API connected")
                status.height?.let { height -> append(". Height: $height") }
            }
            refreshWalletData()
        } catch (e: Exception) {
            _apiState.value = ApiState.FAILED
            _apiMessage.value = when (e) {
                is PepewApiException -> "API error ${e.statusCode}: ${e.message}"
                is java.net.SocketTimeoutException -> "API timeout. Please retry."
                is java.net.UnknownHostException -> "No network or DNS failure. Please retry."
                else -> e.message ?: "Unable to reach PEPEW Light API."
            }
        } finally {
            _isApiLoading.value = false
        }
    }

    override suspend fun refreshWalletData() {
        val addr = _address.value
        if (addr.isBlank()) return

        _isApiLoading.value = true
        _apiState.value = ApiState.CONNECTED
        _apiMessage.value = "Syncing with blockchain..."

        try {
            val summary = apiClient.getAddressSummary(addr)
            val history = apiClient.getHistory(addr, limit = 50, offset = 0)

            _balance.value = summary.confirmedPepew + summary.unconfirmedPepew
            _transactions.value = (history.ifEmpty { summary.history }).map { apiTx ->
                Transaction(
                    txId = apiTx.txid,
                    address = apiTx.address,
                    amount = apiTx.amount,
                    timestamp = apiTx.timestampMillis,
                    isSend = apiTx.isSend,
                    isPending = apiTx.isPending
                )
            }
            _apiState.value = ApiState.READY
            _apiMessage.value = "Sync complete."
        } catch (e: Exception) {
            _apiState.value = ApiState.FAILED
            _apiMessage.value = when (e) {
                is PepewApiException -> "API error ${e.statusCode}: ${e.message}"
                is java.net.SocketTimeoutException -> "API timeout. Please retry."
                is java.net.UnknownHostException -> "No network or DNS failure. Please retry."
                else -> e.message ?: "Unable to reach PEPEW Light API."
            }
        } finally {
            _isApiLoading.value = false
        }
    }

    override fun setApiState(state: ApiState) {
        _apiState.value = state
    }

    override fun requestMockFaucet() {}
}

/**
 * Offline mock data source retained ONLY for previews/tests.
 */
class FakeWalletRepository(private val context: Context) : WalletRepository {
    private val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)

    private val _isWalletCreated = MutableStateFlow(prefs.getBoolean("wallet_created", false))
    override val isWalletCreated: StateFlow<Boolean> = _isWalletCreated.asStateFlow()

    private val _mnemonic = MutableStateFlow<String?>(prefs.getString("mnemonic", null))
    override val mnemonic: StateFlow<String?> = _mnemonic.asStateFlow()

    private val _address = MutableStateFlow(prefs.getString("address", "") ?: "")
    override val address: StateFlow<String> = _address.asStateFlow()

    private val _balance = MutableStateFlow(prefs.getFloat("balance", 0f).toDouble())
    override val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(loadTransactions())
    override val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _apiState = MutableStateFlow(ApiState.READY)
    override val apiState: StateFlow<ApiState> = _apiState.asStateFlow()

    private val _apiMessage = MutableStateFlow("Mock wallet mode. No real API request is made.")
    override val apiMessage: StateFlow<String> = _apiMessage.asStateFlow()

    private val _isApiLoading = MutableStateFlow(false)
    override val isApiLoading: StateFlow<Boolean> = _isApiLoading.asStateFlow()

    private val _isApiMode = MutableStateFlow(prefs.getBoolean("use_api_mode", false))
    override val isApiMode: StateFlow<Boolean> = _isApiMode.asStateFlow()

    override fun createWallet() {
        val newMnemonic = "pepe frog wallet mock phase seed words never real green crypto"
        val newAddress = "PMockPepepowAddress123456789"
        
        _mnemonic.value = newMnemonic
        _address.value = newAddress
        _balance.value = 0.0
        _transactions.value = emptyList()
        _isWalletCreated.value = false
        
        prefs.edit().apply {
            putString("mnemonic", newMnemonic)
            putString("address", newAddress)
            putFloat("balance", 0.0f)
            putBoolean("wallet_created", false)
            putString("transactions", JSONArray().toString())
            apply()
        }
    }

    override fun restoreWalletFromMnemonic(mnemonic: String) {
        val trimmed = mnemonic.trim().replace("\\s+".toRegex(), " ")
        val newAddress = "PMockRestoredAddress123456789"
        
        _mnemonic.value = trimmed
        _address.value = newAddress
        _balance.value = 0.0
        _transactions.value = emptyList()
        _isWalletCreated.value = true
        
        prefs.edit().apply {
            putString("mnemonic", trimmed)
            putString("address", newAddress)
            putFloat("balance", 0.0f)
            putBoolean("wallet_created", true)
            putString("transactions", JSONArray().toString())
            apply()
        }
    }

    override fun confirmBackup() {
        _isWalletCreated.value = true
        prefs.edit().putBoolean("wallet_created", true).apply()
    }

    override fun clearWallet() {
        _mnemonic.value = null
        _address.value = ""
        _balance.value = 0.0
        _transactions.value = emptyList()
        _isWalletCreated.value = false
        prefs.edit().clear().apply()
    }

    override fun requestMockFaucet() {
        val newBalance = _balance.value + 100.0
        _balance.value = newBalance
        val faucetTx = Transaction(
            txId = "mock_faucet_${System.currentTimeMillis()}",
            address = "PEPEW Faucet",
            amount = 100.0,
            timestamp = System.currentTimeMillis(),
            isSend = false
        )
        val updated = listOf(faucetTx) + _transactions.value
        _transactions.value = updated
        prefs.edit().putFloat("balance", newBalance.toFloat()).apply()
        saveTransactions(updated)
    }

    override suspend fun sendTx(recipientAddress: String, amount: Double): Boolean {
        val fee = 0.001
        val total = amount + fee
        if (amount <= 0.0 || total > _balance.value) return false
        val newBalance = _balance.value - total
        _balance.value = newBalance
        val pendingTx = Transaction(
            txId = "mock_pending_${System.currentTimeMillis()}",
            address = recipientAddress,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            isSend = true,
            isPending = true
        )
        val updated = listOf(pendingTx) + _transactions.value
        _transactions.value = updated
        prefs.edit().putFloat("balance", newBalance.toFloat()).apply()
        saveTransactions(updated)
        return true
    }

    override suspend fun retryConnection() {}
    override suspend fun refreshWalletData() {}
    override fun setApiState(state: ApiState) {}
    override fun setApiMode(enabled: Boolean) {}

    private fun saveTransactions(transactions: List<Transaction>) {
        val array = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject()
            obj.put("txId", tx.txId)
            obj.put("address", tx.address)
            obj.put("amount", tx.amount)
            obj.put("timestamp", tx.timestamp)
            obj.put("isSend", tx.isSend)
            obj.put("isPending", tx.isPending)
            array.put(obj)
        }
        prefs.edit().putString("transactions", array.toString()).apply()
    }

    private fun loadTransactions(): List<Transaction> {
        val str = prefs.getString("transactions", null) ?: return emptyList()
        return try {
            val array = JSONArray(str)
            val list = mutableListOf<Transaction>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Transaction(
                        txId = obj.getString("txId"),
                        address = obj.getString("address"),
                        amount = obj.getDouble("amount"),
                        timestamp = obj.getLong("timestamp"),
                        isSend = obj.getBoolean("isSend"),
                        isPending = obj.optBoolean("isPending", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
