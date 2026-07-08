package net.pepepow.wallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import net.pepepow.wallet.data.ConsolidationProgress
import net.pepepow.wallet.data.WalletRepository
import net.pepepow.wallet.domain.address.PepepowNetworkParams
import net.pepepow.wallet.domain.keys.Bip32
import net.pepepow.wallet.domain.mnemonic.Bip39MnemonicService
import net.pepepow.wallet.domain.transaction.TransactionBuilder
import net.pepepow.wallet.domain.transaction.Utxo

class ConsolidationViewModel(
    private val repository: WalletRepository
) : ViewModel() {

    val balance: StateFlow<Double> = repository.balance
    val address: StateFlow<String> = repository.address

    private val _isConsolidating = MutableStateFlow(false)
    val isConsolidating: StateFlow<Boolean> = _isConsolidating.asStateFlow()

    private val _apiMessage = MutableStateFlow<String?>(null)
    val apiMessage: StateFlow<String?> = _apiMessage.asStateFlow()

    private val _lastTxid = MutableStateFlow<String?>(null)
    val lastTxid: StateFlow<String?> = _lastTxid.asStateFlow()

    // Phase 2: Auto Mode States
    private val _isAutoMode = MutableStateFlow(false)
    val isAutoMode: StateFlow<Boolean> = _isAutoMode.asStateFlow()

    private val _autoCompletedRounds = MutableStateFlow(0)
    val autoCompletedRounds: StateFlow<Int> = _autoCompletedRounds.asStateFlow()

    private val _autoState = MutableStateFlow("idle") // idle, loading UTXOs, fetching raw tx, signing, broadcasting, waiting, refreshing, paused, completed, failed
    val autoState: StateFlow<String> = _autoState.asStateFlow()

    private val _autoStatusText = MutableStateFlow("")
    val autoStatusText: StateFlow<String> = _autoStatusText.asStateFlow()

    private val _canResume = MutableStateFlow(false)
    val canResume: StateFlow<Boolean> = _canResume.asStateFlow()

    private var autoJob: Job? = null

    init {
        checkResumeProgress()
    }

    fun checkResumeProgress() {
        val progress = repository.getConsolidationProgress()
        if (progress != null && (progress.mode == "AUTO" || progress.mode == "PAUSED")) {
            val elapsed = System.currentTimeMillis() - progress.updatedTimestamp
            if (elapsed < 30 * 60 * 1000L) { // 30 minutes
                _canResume.value = true
            } else {
                clearProgress()
            }
        } else {
            _canResume.value = false
        }
    }

    fun startManualConsolidation(inputCountCap: Int) {
        viewModelScope.launch {
            performManualConsolidation(inputCountCap)
        }
    }

    suspend fun performManualConsolidation(inputCountCap: Int) {
        if (_isConsolidating.value) return
        _isConsolidating.value = true
        _apiMessage.value = "Starting manual consolidation..."
        _lastTxid.value = null
        
        try {
            val addressStr = repository.address.value
            val mnemonicStr = repository.mnemonic.value
            if (addressStr.isBlank() || mnemonicStr.isNullOrBlank()) {
                _apiMessage.value = "Wallet keys are not initialized locally."
                _isConsolidating.value = false
                return
            }

            // 1. Fetch live UTXOs
            _apiMessage.value = "Fetching UTXOs..."
            val allUtxos = repository.fetchUtxos(addressStr)
            if (allUtxos.isEmpty()) {
                _apiMessage.value = "No UTXOs available."
                _isConsolidating.value = false
                return
            }

            // 2. Filter invalid & recently spent UTXOs
            val eligibleUtxos = allUtxos.filter { utxo ->
                utxo.txid.length == 64 && utxo.vout >= 0 && utxo.satoshis > 0 &&
                        !repository.isOutpointSpent(utxo.txid, utxo.vout)
            }

            if (eligibleUtxos.size < 2) {
                _apiMessage.value = "Too few UTXOs to consolidate (minimum 2 required)."
                _isConsolidating.value = false
                return
            }

            // 3. Sort: confirmed first, then smallest satoshis first
            val sortedUtxos = eligibleUtxos.sortedWith(
                compareBy<Utxo> { if (it.height > 0L) 0 else 1 }
                    .thenBy { it.satoshis }
            )

            // 4. Select up to user-selected cap
            val selectedUtxos = sortedUtxos.take(inputCountCap)
            if (selectedUtxos.size < 2) {
                _apiMessage.value = "Too few UTXOs selected after filtering."
                _isConsolidating.value = false
                return
            }

            // 5. Estimate Fee and Size
            val inputCount = selectedUtxos.size
            val estSize = 10 + inputCount * 148 + 34
            val feeSat = ((estSize + 999) / 1000) * 100_000L
            val totalInputSat = selectedUtxos.sumOf { it.satoshis }

            val outputSat = totalInputSat - feeSat
            if (outputSat <= 546L) {
                _apiMessage.value = "Insufficient funds: consolidation amount is below dust limit after fee."
                _isConsolidating.value = false
                return
            }

            if (feeSat >= totalInputSat) {
                _apiMessage.value = "Insufficient funds: fee exceeds total input amount."
                _isConsolidating.value = false
                return
            }

            // 6. Fetch previous raw transactions (omitted in Option A as unused by TransactionBuilder)

            // 7. Local key derivation & signing
            _apiMessage.value = "Signing locally..."
            val seed = Bip39MnemonicService().deriveSeed(mnemonicStr)
            val keyNode = Bip32.derivePath(seed, PepepowNetworkParams.DEFAULT_PATH)

            val rawTxHex = TransactionBuilder.createAndSignTransaction(
                privateKey = keyNode.privateKey,
                utxos = selectedUtxos,
                recipientAddress = addressStr,
                amountSat = outputSat,
                feeSat = feeSat,
                senderAddress = addressStr
            )

            // 8. Broadcast
            _apiMessage.value = "Broadcasting..."
            val txid = repository.broadcastConsolidationTx(rawTxHex)

            // 9. Mark spent outpoints locally
            repository.markOutpointsSpent(selectedUtxos.map { it.txid to it.vout })

            // 10. Done!
            _lastTxid.value = txid
            _apiMessage.value = "Consolidation transaction broadcasted successfully! TXID: $txid"

            repository.refreshWalletData(force = true)

        } catch (e: Exception) {
            _apiMessage.value = "Consolidation failed: ${e.message ?: e.javaClass.simpleName}"
        } finally {
            _isConsolidating.value = false
        }
    }

    fun startAutoConsolidation(roundSize: Int, maxRounds: Int?) {
        autoJob = viewModelScope.launch {
            runAutoConsolidationLoop(roundSize, maxRounds)
        }
    }

    suspend fun runAutoConsolidationLoop(roundSize: Int, maxRounds: Int?) {
        if (_isConsolidating.value || _isAutoMode.value) return
        _isAutoMode.value = true
        _autoCompletedRounds.value = 0
        _autoState.value = "loading"
        
        repository.saveConsolidationProgress(
            ConsolidationProgress(
                mode = "AUTO",
                roundSize = roundSize,
                completedRounds = 0,
                lastTxid = "",
                updatedTimestamp = System.currentTimeMillis()
            )
        )

        var consecutiveFailures = 0
        val maxFailures = 3

        try {
            while (true) {
                if (maxRounds != null && _autoCompletedRounds.value >= maxRounds) {
                    _autoState.value = "completed"
                    _autoStatusText.value = "Auto consolidation completed: max rounds reached."
                    clearProgress()
                    break
                }

                _autoState.value = "loading UTXOs"
                _autoStatusText.value = "Round ${_autoCompletedRounds.value + 1}: Loading UTXOs..."

                val addressStr = repository.address.value
                val mnemonicStr = repository.mnemonic.value
                if (addressStr.isBlank() || mnemonicStr.isNullOrBlank()) {
                    _autoState.value = "failed"
                    _autoStatusText.value = "Wallet keys are not available locally."
                    clearProgress()
                    break
                }

                val allUtxos = try {
                    repository.fetchUtxos(addressStr)
                } catch (e: Exception) {
                    consecutiveFailures++
                    if (consecutiveFailures >= maxFailures) {
                        _autoState.value = "failed"
                        _autoStatusText.value = "Repeated network failures. Stopped."
                        break
                    }
                    _autoStatusText.value = "Network error fetching UTXOs. Retrying in 10s... ($consecutiveFailures/$maxFailures)"
                    delay(10_000)
                    continue
                }

                val eligibleUtxos = allUtxos.filter { utxo ->
                    utxo.txid.length == 64 && utxo.vout >= 0 && utxo.satoshis > 0 &&
                            !repository.isOutpointSpent(utxo.txid, utxo.vout)
                }

                if (eligibleUtxos.size < 2) {
                    _autoState.value = "completed"
                    _autoStatusText.value = "Completed: No more eligible UTXOs to consolidate."
                    clearProgress()
                    break
                }

                val sortedUtxos = eligibleUtxos.sortedWith(
                    compareBy<Utxo> { if (it.height > 0L) 0 else 1 }
                        .thenBy { it.satoshis }
                )

                val selectedUtxos = sortedUtxos.take(roundSize)
                if (selectedUtxos.size < 2) {
                    _autoState.value = "completed"
                    _autoStatusText.value = "Completed: Less than 2 UTXOs remaining."
                    clearProgress()
                    break
                }

                val inputCount = selectedUtxos.size
                val estSize = 10 + inputCount * 148 + 34
                val feeSat = ((estSize + 999) / 1000) * 100_000L
                val totalInputSat = selectedUtxos.sumOf { it.satoshis }
                val outputSat = totalInputSat - feeSat

                if (outputSat <= 546L || feeSat >= totalInputSat) {
                    _autoState.value = "completed"
                    _autoStatusText.value = "Completed: Remaining UTXOs are too small to cover transaction fee."
                    clearProgress()
                    break
                }

                // Fetching raw transactions (omitted in Option A as unused by TransactionBuilder)

                _autoState.value = "signing"
                _autoStatusText.value = "Round ${_autoCompletedRounds.value + 1}: Signing transaction locally..."
                val rawTxHex = try {
                    val seed = Bip39MnemonicService().deriveSeed(mnemonicStr)
                    val keyNode = Bip32.derivePath(seed, PepepowNetworkParams.DEFAULT_PATH)
                    TransactionBuilder.createAndSignTransaction(
                        privateKey = keyNode.privateKey,
                        utxos = selectedUtxos,
                        recipientAddress = addressStr,
                        amountSat = outputSat,
                        feeSat = feeSat,
                        senderAddress = addressStr
                    )
                } catch (e: Exception) {
                    _autoState.value = "failed"
                    _autoStatusText.value = "Local signing failed: ${e.message}"
                    break
                }

                _autoState.value = "broadcasting"
                _autoStatusText.value = "Round ${_autoCompletedRounds.value + 1}: Broadcasting transaction..."
                val txid = try {
                    repository.broadcastConsolidationTx(rawTxHex)
                } catch (e: Exception) {
                    consecutiveFailures++
                    if (consecutiveFailures >= maxFailures) {
                        _autoState.value = "failed"
                        _autoStatusText.value = "Repeated broadcast rejections. Stopped."
                        break
                    }
                    _autoStatusText.value = "Broadcast failed: ${e.message}. Retrying in 10s... ($consecutiveFailures/$maxFailures)"
                    delay(10_000)
                    continue
                }

                consecutiveFailures = 0
                repository.markOutpointsSpent(selectedUtxos.map { it.txid to it.vout })

                _autoCompletedRounds.value++
                _lastTxid.value = txid
                
                repository.saveConsolidationProgress(
                    ConsolidationProgress(
                        mode = "AUTO",
                        roundSize = roundSize,
                        completedRounds = _autoCompletedRounds.value,
                        lastTxid = txid,
                        updatedTimestamp = System.currentTimeMillis()
                    )
                )

                _autoState.value = "waiting"
                val delayMs = (8000..12000).random().toLong()
                for (sec in (delayMs / 1000) downTo 1) {
                    _autoStatusText.value = "Round ${_autoCompletedRounds.value} success! Next round in ${sec}s..."
                    delay(1000)
                }

                _autoState.value = "refreshing"
                _autoStatusText.value = "Refreshing wallet state..."
                repository.refreshWalletData(force = true)
                delay(3000)
            }
        } finally {
            _isAutoMode.value = false
        }
    }

    fun pauseAutoConsolidation() {
        autoJob?.cancel()
        autoJob = null
        _isAutoMode.value = false
        _autoState.value = "paused"
        _autoStatusText.value = "Auto consolidation paused by user."
        
        val progress = repository.getConsolidationProgress()
        if (progress != null) {
            repository.saveConsolidationProgress(
                progress.copy(
                    mode = "PAUSED",
                    updatedTimestamp = System.currentTimeMillis()
                )
            )
        }
        _canResume.value = true
    }

    fun cancelAutoConsolidation() {
        autoJob?.cancel()
        autoJob = null
        _isAutoMode.value = false
        _autoState.value = "idle"
        _autoStatusText.value = "Auto consolidation cancelled."
        clearProgress()
    }

    fun resumeAutoConsolidation() {
        val progress = repository.getConsolidationProgress()
        if (progress != null && (progress.mode == "AUTO" || progress.mode == "PAUSED")) {
            val elapsed = System.currentTimeMillis() - progress.updatedTimestamp
            if (elapsed < 30 * 60 * 1000L) {
                _autoCompletedRounds.value = progress.completedRounds
                _lastTxid.value = progress.lastTxid
                startAutoConsolidation(progress.roundSize, null)
                return
            }
        }
        clearProgress()
    }

    private fun clearProgress() {
        repository.saveConsolidationProgress(null)
        _canResume.value = false
    }

    override fun onCleared() {
        super.onCleared()
        autoJob?.cancel()
    }
}
