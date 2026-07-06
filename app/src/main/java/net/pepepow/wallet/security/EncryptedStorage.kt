package net.pepepow.wallet.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedStorage(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_wallet_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        // Automatically clear old mock plain shared preferences if they exist
        val oldPrefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        if (oldPrefs.contains("mnemonic") || oldPrefs.contains("address")) {
            oldPrefs.edit().clear().apply()
        }
    }

    fun saveMnemonic(mnemonic: String) {
        securePrefs.edit().putString("secure_mnemonic", mnemonic).apply()
    }

    fun getMnemonic(): String? {
        return securePrefs.getString("secure_mnemonic", null)
    }

    fun saveAddress(address: String) {
        securePrefs.edit().putString("secure_address", address).apply()
    }

    fun getAddress(): String {
        return securePrefs.getString("secure_address", "") ?: ""
    }

    fun saveWalletCreated(isCreated: Boolean) {
        securePrefs.edit().putBoolean("secure_wallet_created", isCreated).apply()
    }

    fun isWalletCreated(): Boolean {
        return securePrefs.getBoolean("secure_wallet_created", false)
    }

    fun clear() {
        securePrefs.edit().clear().apply()
        val oldPrefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        oldPrefs.edit().clear().apply()
    }
}
