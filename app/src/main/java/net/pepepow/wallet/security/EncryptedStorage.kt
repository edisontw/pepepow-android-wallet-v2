package net.pepepow.wallet.security

import android.content.Context

/**
 * Placeholder for Phase 2 secure storage.
 * Currently, for the Phase 1 mock wallet, we do not save or store real mnemonic seed phrase data.
 */
class EncryptedStorage(context: Context) {
    
    fun saveMnemonic(mnemonic: String) {
        // Placeholder: No real seed data stored yet in Phase 1 mock wallet
    }

    fun getMnemonic(): String? {
        // Placeholder: No real seed data retrieved yet in Phase 1 mock wallet
        return null
    }

    fun clear() {
        // Placeholder
    }
}
