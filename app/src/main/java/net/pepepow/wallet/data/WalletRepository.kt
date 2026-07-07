package net.pepepow.wallet.data

import android.content.Context
import android.util.Log
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
    val isPending: Boolean = false,
    val isSelfTransfer: Boolean = false
)

sealed class SendResult {
    data class Success(val txid: String) : SendResult()
    data class ValidationError(val message: String) : SendResult()
    data class InsufficientFunds(
        val availableAtoms: Long,
        val requiredAtoms: Long,
        val feeAtoms: Long
    ) : SendResult()
    data class Blocked(val reason: String) : SendResult()
    data class ApiError(val message: String, val cause: Throwable? = null) : SendResult()
    data class Failure(val message: String, val cause: Throwable? = null) : SendResult()
}

data class WalletDiagnostics(
    val apiConnected: Boolean,
    val utxoEndpointStatus: String, // "ok", "missing", "error"
    val utxoCount: Int,
    val spendableAmountDouble: Double,
    val signingEnabled: Boolean,
    val broadcastEndpointStatus: String, // "ok", "missing", "error"
    val lastSendError: String?
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
    val usdPrice: StateFlow<Double?>

    fun createWallet()
    fun confirmBackup()
    fun clearWallet()
    suspend fun sendTx(recipientAddress: String, amountAtoms: Long, onProgress: (String) -> Unit): SendResult
    suspend fun retryConnection()
    suspend fun refreshWalletData(force: Boolean = false)
    suspend fun checkDiagnostics(): WalletDiagnostics
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
    private var _lastSendError: String? = null

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

    private val _usdPrice = MutableStateFlow<Double?>(null)
    override val usdPrice: StateFlow<Double?> = _usdPrice.asStateFlow()

    private var lastRefreshAt = 0L
    private var consecutiveRefreshFailures = 0
    private var hasLoadedSuccessfully = false

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
        _usdPrice.value = null
        lastRefreshAt = 0L
        consecutiveRefreshFailures = 0
        hasLoadedSuccessfully = false
        _apiState.value = ApiState.CONNECTED
        _apiMessage.value = "Wallet reset."
    }

    private fun logDebug(tag: String, message: String) {
        val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            Log.d(tag, message)
        }
    }

    override suspend fun sendTx(
        recipientAddress: String,
        amountAtoms: Long,
        onProgress: (String) -> Unit
    ): SendResult = withContext(Dispatchers.IO) {
        val words = secureStorage.getMnemonic() ?: return@withContext SendResult.Blocked("Mnemonic is missing")
        val sender = _address.value
        if (sender.isBlank()) return@withContext SendResult.Blocked("Sender address is blank")

        try {
            _lastSendError = null
            _isApiLoading.value = true
            onProgress("Fetching UTXOs...")
            _apiMessage.value = "Fetching spendable UTXOs..."

            // 1. Fetch live UTXOs
            val apiUtxos = apiClient.getUtxos(sender)
            val utxos = apiUtxos.map { Utxo(it.txid, it.vout, it.satoshis, it.scriptPubKey) }

            // Sort UTXOs by value descending
            val sortedUtxos = utxos.sortedByDescending { it.satoshis }

            val feeAtoms = 100_000L // 0.001 PEPEW
            val totalNeededAtoms = amountAtoms + feeAtoms

            // Select UTXOs
            var selectedSatoshis = 0L
            val selectedUtxos = mutableListOf<Utxo>()
            for (utxo in sortedUtxos) {
                selectedUtxos.add(utxo)
                selectedSatoshis += utxo.satoshis
                if (selectedSatoshis >= totalNeededAtoms) {
                    break
                }
            }

            logDebug("WalletSend", "sender: $sender")
            logDebug("WalletSend", "recipient: $recipientAddress")
            logDebug("WalletSend", "amount atomic: $amountAtoms")
            logDebug("WalletSend", "fee atomic: $feeAtoms")
            logDebug("WalletSend", "available atomic: $selectedSatoshis")
            logDebug("WalletSend", "UTXO count: ${sortedUtxos.size}")
            logDebug("WalletSend", "selected UTXO count: ${selectedUtxos.size}")

            if (selectedSatoshis < totalNeededAtoms) {
                return@withContext SendResult.InsufficientFunds(
                    availableAtoms = selectedSatoshis,
                    requiredAtoms = totalNeededAtoms,
                    feeAtoms = feeAtoms
                )
            }

            // Estimate size: size = 10 + 148 * selectedUtxos.size + 34 * (outputs.size)
            val changeSat = selectedSatoshis - totalNeededAtoms
            val outputsCount = if (changeSat >= 546L) 2 else 1
            val estSize = 10 + 148 * selectedUtxos.size + 34 * outputsCount
            logDebug("WalletSend", "unsigned tx size estimate: $estSize bytes")

            onProgress("Building transaction...")
            _apiMessage.value = "Signing transaction locally..."

            onProgress("Signing locally...")
            // 2. Derive private key
            val seed = mnemonicService.deriveSeed(words)
            val keyNode = Bip32.derivePath(seed, PepepowNetworkParams.DEFAULT_PATH)

            // 3. Build and Sign Transaction
            val rawHex = TransactionBuilder.createAndSignTransaction(
                privateKey = keyNode.privateKey,
                utxos = selectedUtxos,
                recipientAddress = recipientAddress,
                amountSat = amountAtoms,
                feeSat = feeAtoms,
                senderAddress = sender
            )

            logDebug("WalletSend", "signed tx hex length: ${rawHex.length}")

            onProgress("Broadcasting...")
            _apiMessage.value = "Broadcasting raw transaction..."

            // 4. Broadcast
            val txid = apiClient.broadcastTransaction(rawHex)

            // 5. Update local state
            val isSelf = recipientAddress.trim() == sender.trim()
            val pendingAmount = if (isSelf) feeAtoms / 100_000_000.0 else amountAtoms / 100_000_000.0
            val pendingTx = Transaction(
                txId = txid,
                address = recipientAddress,
                amount = pendingAmount,
                timestamp = System.currentTimeMillis(),
                isSend = true,
                isPending = true,
                isSelfTransfer = isSelf
            )
            
            _transactions.value = listOf(pendingTx) + _transactions.value
            _lastSendError = null
            _apiMessage.value = "Transaction broadcasted successfully! TXID: $txid"

            SendResult.Success(txid)
        } catch (e: PepewApiException) {
            logDebug("WalletSend", "broadcast HTTP status: ${e.statusCode}")
            logDebug("WalletSend", "broadcast error message: ${e.message}")
            _lastSendError = "Send failed: ${e.message}"
            SendResult.ApiError(e.message, e)
        } catch (e: java.net.SocketTimeoutException) {
            logDebug("WalletSend", "broadcast network timeout")
            _lastSendError = "Send failed: network timeout"
            SendResult.ApiError("Network timeout. Please retry.", e)
        } catch (e: java.net.UnknownHostException) {
            logDebug("WalletSend", "broadcast no network connection")
            _lastSendError = "Send failed: no connection"
            SendResult.ApiError("No network connection or DNS failure.", e)
        } catch (e: Exception) {
            logDebug("WalletSend", "broadcast failure: ${e.message}")
            _lastSendError = "Send failed: ${e.message ?: e.javaClass.simpleName}"
            SendResult.Failure(e.message ?: "Unexpected error", e)
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

    override suspend fun refreshWalletData(force: Boolean) {
        val addr = _address.value
        if (addr.isBlank()) return

        if (_isApiLoading.value) return

        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshAt < 15_000) {
            _apiMessage.value = "Refresh cooling down."
            return
        }

        _isApiLoading.value = true
        _apiMessage.value = "Syncing with blockchain..."

        try {
            val summary = apiClient.getAddressSummary(addr)
            val history = apiClient.getHistory(addr, limit = 50, offset = 0)

            _balance.value = summary.confirmedPepew + summary.unconfirmedPepew
            val apiTransactions = (history.ifEmpty { summary.history }).map { apiTx ->
                Transaction(
                    txId = apiTx.txid,
                    address = apiTx.address,
                    amount = apiTx.amount,
                    timestamp = apiTx.timestampMillis,
                    isSend = apiTx.isSend,
                    isPending = apiTx.isPending,
                    isSelfTransfer = apiTx.isSelfTransfer
                )
            }
            val localPending = _transactions.value.filter { it.isPending }
            val merged = (apiTransactions + localPending)
                .distinctBy { it.txId }
                .sortedByDescending { it.timestamp }
            _transactions.value = merged
            consecutiveRefreshFailures = 0
            hasLoadedSuccessfully = true
            lastRefreshAt = now
            _apiState.value = ApiState.READY
            _apiMessage.value = "Sync complete."
        } catch (e: Exception) {
            val syncErrorMsg = when (e) {
                is PepewApiException -> "API error ${e.statusCode}: ${e.message}"
                is java.net.SocketTimeoutException -> "API timeout. Please retry."
                is java.net.UnknownHostException -> "No network or DNS failure. Please retry."
                else -> e.message ?: "Unable to reach PEPEW Light API."
            }
            consecutiveRefreshFailures++
            if (hasLoadedSuccessfully && consecutiveRefreshFailures <= 2) {
                _apiState.value = ApiState.READY
                _apiMessage.value = "Refresh delayed. Showing last synced data."
            } else {
                _apiState.value = ApiState.FAILED
                _apiMessage.value = syncErrorMsg
            }
        } finally {
            _isApiLoading.value = false
        }

        try {
            val priceRes = apiClient.getPrice()
            if (priceRes.ok && priceRes.priceUsdt != null) {
                _usdPrice.value = priceRes.priceUsdt
            }
        } catch (e: Exception) {
            Log.e("WalletRepository", "Price fetch failed", e)
        }
    }

    override fun setApiState(state: ApiState) {
        _apiState.value = state
    }

    override fun requestMockFaucet() {}

    override suspend fun checkDiagnostics(): WalletDiagnostics = withContext(Dispatchers.IO) {
        val sender = _address.value
        if (sender.isBlank()) {
            return@withContext WalletDiagnostics(
                apiConnected = false,
                utxoEndpointStatus = "missing (no address)",
                utxoCount = 0,
                spendableAmountDouble = 0.0,
                signingEnabled = false,
                broadcastEndpointStatus = "ok",
                lastSendError = _lastSendError
            )
        }
        
        val apiConnected = try {
            val health = apiClient.getHealth()
            val status = apiClient.getStatus()
            health.ok && status.ok
        } catch (e: Exception) {
            false
        }
        var utxoStatus = "ok"
        var utxoCount = 0
        var spendableAmount = 0.0
        var broadcastStatus = "ok"
        
        try {
            val apiUtxos = apiClient.getUtxos(sender)
            utxoCount = apiUtxos.size
            spendableAmount = apiUtxos.sumOf { it.satoshis } / 1e8
        } catch (e: Exception) {
            utxoStatus = "error: ${e.message ?: e.javaClass.simpleName}"
        }
        
        if (!apiConnected) {
            broadcastStatus = "error (API disconnected)"
        }
        
        val signingEnabled = secureStorage.getMnemonic() != null
        
        WalletDiagnostics(
            apiConnected = apiConnected,
            utxoEndpointStatus = utxoStatus,
            utxoCount = utxoCount,
            spendableAmountDouble = spendableAmount,
            signingEnabled = signingEnabled,
            broadcastEndpointStatus = broadcastStatus,
            lastSendError = _lastSendError
        )
    }
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

    private val _usdPrice = MutableStateFlow<Double?>(0.000000799)
    override val usdPrice: StateFlow<Double?> = _usdPrice.asStateFlow()

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
            isSend = false,
            isSelfTransfer = false
        )
        val updated = listOf(faucetTx) + _transactions.value
        _transactions.value = updated
        prefs.edit().putFloat("balance", newBalance.toFloat()).apply()
        saveTransactions(updated)
    }

    override suspend fun sendTx(
        recipientAddress: String,
        amountAtoms: Long,
        onProgress: (String) -> Unit
    ): SendResult {
        onProgress("Preparing transaction...")
        delay(300)
        onProgress("Fetching UTXOs...")
        delay(400)

        val feeAtoms = 100_000L // 0.001 PEPEW
        val totalNeededAtoms = amountAtoms + feeAtoms

        val currentBalanceAtoms = Math.round(_balance.value * 1e8)
        if (amountAtoms <= 0L || totalNeededAtoms > currentBalanceAtoms) {
            return SendResult.InsufficientFunds(
                availableAtoms = currentBalanceAtoms,
                requiredAtoms = totalNeededAtoms,
                feeAtoms = feeAtoms
            )
        }

        onProgress("Building transaction...")
        delay(300)
        onProgress("Signing locally...")
        delay(300)
        onProgress("Broadcasting...")
        delay(500)

        val newBalance = (_balance.value * 1e8 - totalNeededAtoms) / 1e8
        _balance.value = newBalance
        val txid = "mock_pending_${System.currentTimeMillis()}"
        val sender = _address.value
        val isSelf = recipientAddress.trim() == sender.trim()
        val pendingAmount = if (isSelf) feeAtoms / 100_000_000.0 else amountAtoms / 100_000_000.0
        val pendingTx = Transaction(
            txId = txid,
            address = recipientAddress,
            amount = pendingAmount,
            timestamp = System.currentTimeMillis(),
            isSend = true,
            isPending = true,
            isSelfTransfer = isSelf
        )
        val updated = listOf(pendingTx) + _transactions.value
        _transactions.value = updated
        prefs.edit().putFloat("balance", newBalance.toFloat()).apply()
        saveTransactions(updated)
        return SendResult.Success(txid)
    }

    override suspend fun retryConnection() {}
    override suspend fun refreshWalletData(force: Boolean) {}
    override suspend fun checkDiagnostics(): WalletDiagnostics {
        return WalletDiagnostics(
            apiConnected = true,
            utxoEndpointStatus = "ok (mock)",
            utxoCount = 5,
            spendableAmountDouble = _balance.value,
            signingEnabled = true,
            broadcastEndpointStatus = "ok (mock)",
            lastSendError = null
        )
    }
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
            obj.put("isSelfTransfer", tx.isSelfTransfer)
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
                        isPending = obj.optBoolean("isPending", false),
                        isSelfTransfer = obj.optBoolean("isSelfTransfer", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
