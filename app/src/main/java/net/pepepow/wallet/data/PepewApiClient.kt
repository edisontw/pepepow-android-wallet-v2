package net.pepepow.wallet.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PepewApiClient placeholder for Phase 2 node communication.
 * Currently returns mock data offline for the Phase 1 implementation.
 */
class PepewApiClient(val baseUrl: String = "https://light.pepepow.net/") {
    
    suspend fun getBalance(address: String): Double = withContext(Dispatchers.IO) {
        // Mock balance placeholder
        12345.6789
    }

    suspend fun broadcastTransaction(hex: String): String = withContext(Dispatchers.IO) {
        // Mock tx broadcast placeholder
        "tx_" + java.util.UUID.randomUUID().toString().take(12)
    }

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        // Mock health check placeholder
        true
    }
}
