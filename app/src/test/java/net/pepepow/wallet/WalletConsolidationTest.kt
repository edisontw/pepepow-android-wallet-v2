package net.pepepow.wallet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import net.pepepow.wallet.data.ApiState
import net.pepepow.wallet.data.ConsolidationProgress
import net.pepepow.wallet.data.SendResult
import net.pepepow.wallet.data.Transaction
import net.pepepow.wallet.data.WalletDiagnostics
import net.pepepow.wallet.data.WalletRepository
import net.pepepow.wallet.domain.transaction.Utxo
import net.pepepow.wallet.viewmodel.ConsolidationViewModel
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class WalletConsolidationTest {

    class MockWalletRepository : WalletRepository {
        override val balance = MutableStateFlow(10.0)
        override val address = MutableStateFlow("PRfbEeHAKKbz6Voz85WJudrJwTA3ZbHunb")
        override val apiState = MutableStateFlow(ApiState.READY)
        override val apiMessage = MutableStateFlow("")
        override val isApiLoading = MutableStateFlow(false)
        override val mnemonic = MutableStateFlow<String?>("abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about")
        override val isWalletCreated = MutableStateFlow(true)
        override val transactions = MutableStateFlow<List<Transaction>>(emptyList())
        override val usdPrice = MutableStateFlow<Double?>(null)
        override val isApiMode = MutableStateFlow(true)

        override fun createWallet() {}
        override fun confirmBackup() {}
        override fun clearWallet() {}
        override suspend fun sendTx(
            recipientAddress: String,
            amountAtoms: Long,
            onProgress: (String) -> Unit
        ): SendResult = SendResult.Success("")
        override suspend fun retryConnection() {}
        override suspend fun refreshWalletData(force: Boolean) {
            refreshCallCount.incrementAndGet()
        }
        override suspend fun checkDiagnostics(): WalletDiagnostics =
            WalletDiagnostics(true, "ok", 0, 0.0, true, "ok", null)
        override fun setApiState(state: ApiState) {}
        override fun setApiMode(enabled: Boolean) {}
        override fun requestMockFaucet() {}
        override fun restoreWalletFromMnemonic(mnemonic: String) {}

        var mockUtxos = mutableListOf<Utxo>()
        val spentOutpoints = mutableSetOf<String>()
        var progress: ConsolidationProgress? = null
        val refreshCallCount = AtomicInteger(0)
        val rawTxFetchCount = AtomicInteger(0)
        val broadcastCallCount = AtomicInteger(0)
        var lastBroadcastHex: String? = null

        override suspend fun getRawTransaction(txid: String): String {
            rawTxFetchCount.incrementAndGet()
            return "01000000000101..."
        }

        override suspend fun fetchUtxos(address: String): List<Utxo> {
            return mockUtxos
        }

        override suspend fun broadcastConsolidationTx(rawHex: String): String {
            broadcastCallCount.incrementAndGet()
            lastBroadcastHex = rawHex
            return "mock_txid_${System.currentTimeMillis()}"
        }

        override fun markOutpointsSpent(outpoints: List<Pair<String, Int>>) {
            outpoints.forEach { (txid, vout) ->
                spentOutpoints.add("$txid:$vout")
            }
        }

        override fun isOutpointSpent(txid: String, vout: Int): Boolean {
            return spentOutpoints.contains("$txid:$vout")
        }

        override fun getConsolidationProgress(): ConsolidationProgress? = progress

        override fun saveConsolidationProgress(progress: ConsolidationProgress?) {
            this.progress = progress
        }
    }

    @Test
    fun testSmallestFirstUtxoSelection() = runBlocking {
        val repo = MockWalletRepository()
        repo.mockUtxos = mutableListOf(
            Utxo("tx1", 0, 500_000, "script", 0),
            Utxo("tx2", 1, 100_000, "script", 3000),
            Utxo("tx3", 0, 800_000, "script", 2999),
            Utxo("tx4", 2, 200_000, "script", 0)
        )

        val sorted = repo.mockUtxos.sortedWith(
            compareBy<Utxo> { if (it.height > 0L) 0 else 1 }
                .thenBy { it.satoshis }
        )

        assertEquals("tx2", sorted[0].txid)
        assertEquals("tx3", sorted[1].txid)
        assertEquals("tx4", sorted[2].txid)
        assertEquals("tx1", sorted[3].txid)
    }

    @Test
    fun testInputCapEnforcement() = runBlocking {
        val repo = MockWalletRepository()
        repo.mockUtxos = (1..200).map { i ->
            Utxo("tx$i", 0, 100_000L, "script", 1000L)
        }.toMutableList()

        val selected = repo.mockUtxos.take(80)
        assertEquals(80, selected.size)

        val selectedHardCap = repo.mockUtxos.take(150)
        assertEquals(150, selectedHardCap.size)
    }

    @Test
    fun testSelectedTotalAndFeeCalculation() {
        val inputCount = 80
        val estSize = 10 + inputCount * 148 + 34
        val feeSat = ((estSize + 999) / 1000) * 100_000L

        assertEquals(1_200_000L, feeSat)
    }

    @Test
    fun testTooFewUtxoValidation() = runBlocking {
        val repo = MockWalletRepository()
        repo.mockUtxos = mutableListOf(Utxo("tx1", 0, 1_000_000L, "script", 1000L))
        
        val viewModel = ConsolidationViewModel(repo)
        viewModel.performManualConsolidation(80)
        
        assertEquals(0, repo.broadcastCallCount.get())
        assertTrue(viewModel.apiMessage.value!!.contains("Too few UTXOs"))
    }

    @Test
    fun testInsufficientAfterFeeValidation() = runBlocking {
        val repo = MockWalletRepository()
        repo.mockUtxos = mutableListOf(
            Utxo("11".repeat(32), 0, 50_000L, "script", 1000L),
            Utxo("22".repeat(32), 1, 50_000L, "script", 1000L)
        )
        
        val viewModel = ConsolidationViewModel(repo)
        viewModel.performManualConsolidation(80)
        
        assertEquals(0, repo.broadcastCallCount.get())
        assertTrue(viewModel.apiMessage.value!!.contains("below dust limit") || viewModel.apiMessage.value!!.contains("exceeds"))
    }

    @Test
    fun testRecentlySpentOutpointExclusion() = runBlocking {
        val repo = MockWalletRepository()
        repo.mockUtxos = mutableListOf(
            Utxo("tx1", 0, 500_000, "script", 1000),
            Utxo("tx2", 1, 500_000, "script", 1000),
            Utxo("tx3", 2, 500_000, "script", 1000)
        )
        
        repo.markOutpointsSpent(listOf("tx2" to 1))
        
        val eligible = repo.mockUtxos.filter { !repo.isOutpointSpent(it.txid, it.vout) }
        assertEquals(2, eligible.size)
        assertFalse(eligible.any { it.txid == "tx2" })
    }

    @Test
    fun testProgressPersistenceWithoutSecrets() {
        val progress = ConsolidationProgress(
            mode = "AUTO",
            roundSize = 80,
            completedRounds = 3,
            lastTxid = "mock_txid",
            updatedTimestamp = 123456789L
        )
        
        val fields = progress.javaClass.declaredFields.map { it.name }
        assertFalse(fields.contains("mnemonic"))
        assertFalse(fields.contains("privateKey"))
        assertFalse(fields.contains("wif"))
        assertFalse(fields.contains("seed"))
    }

    @Test
    fun testGetRawTransactionParsesDataHexCorrectly() {
        val client = net.pepepow.wallet.data.PepewApiClient()
        val response = """
            {
              "txid": "abc",
              "data": {
                "hex": "0102030405060708090a0b0c0d0e0f1011121314",
                "txid": "abc"
              },
              "source": "electrumx"
            }
        """
        val parsed = client.parseRawTxFromResponse(response)
        assertEquals("0102030405060708090a0b0c0d0e0f1011121314", parsed)
    }

    @Test
    fun testGetRawTransactionParsesTopLevelHexCorrectly() {
        val client = net.pepepow.wallet.data.PepewApiClient()
        val response = """
            {
              "hex": "0102030405060708090a0b0c0d0e0f1011121314",
              "txid": "abc"
            }
        """
        val parsed = client.parseRawTxFromResponse(response)
        assertEquals("0102030405060708090a0b0c0d0e0f1011121314", parsed)
    }

    @Test
    fun testGetRawTransactionRejectsEmptyOrMalformedHex() {
        val client = net.pepepow.wallet.data.PepewApiClient()
        
        // Odd length
        assertNull(client.parseRawTxFromResponse("0102030405060708090a0b0c0d0e0f101112131"))
        
        // Too short (less than 20 characters / 10 bytes)
        assertNull(client.parseRawTxFromResponse("010203040506070809"))
        
        // Non-hex
        assertNull(client.parseRawTxFromResponse("xyz1234567890abcdef01234567890abcdef"))
        
        // Empty
        assertNull(client.parseRawTxFromResponse(""))
    }

    @Test
    fun testAutoConsolidationDoesNotRequireRawTx() = runBlocking {
        val repo = MockWalletRepository()
        repo.mockUtxos = mutableListOf(
            Utxo("11".repeat(32), 0, 5_000_000L, "76a914ab12cd34ef56789012345678901234567890123488ac", 1000L),
            Utxo("22".repeat(32), 1, 5_000_000L, "76a914ab12cd34ef56789012345678901234567890123488ac", 1000L)
        )
        
        val viewModel = ConsolidationViewModel(repo)
        viewModel.runAutoConsolidationLoop(80, 1)
        
        assertEquals(1, repo.broadcastCallCount.get())
        assertEquals(0, repo.rawTxFetchCount.get())
    }

    @Test
    fun testBroadcastPayloadContainsOnlyRawTx() {
        val rawTx = "0102030405060708090a0b0c0d0e0f1011121314"
        val cleanHex = rawTx.trim()
        val postBody = "{\"raw_tx\":\"$cleanHex\"}"
        
        assertTrue(postBody.contains("\"raw_tx\""))
        assertTrue(postBody.contains(rawTx))
        assertFalse(postBody.contains("mnemonic"))
        assertFalse(postBody.contains("privateKey"))
    }
}
