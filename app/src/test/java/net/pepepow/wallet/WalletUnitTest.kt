package net.pepepow.wallet

import net.pepepow.wallet.domain.address.AddressValidator
import net.pepepow.wallet.domain.address.Base58Check
import net.pepepow.wallet.domain.keys.Bip32
import net.pepepow.wallet.domain.mnemonic.Bip39MnemonicService
import net.pepepow.wallet.domain.mnemonic.MnemonicValidationResult
import org.junit.Assert.*
import org.junit.Test

class WalletUnitTest {

    @Test
    fun testBase58Check() {
        val payload = ByteArray(20) { it.toByte() }
        val encoded = Base58Check.encode(55, payload)
        assertTrue(encoded.startsWith("P") || encoded.startsWith("Q"))
        
        val (version, decoded) = Base58Check.decodeChecked(encoded)
        assertEquals(55.toByte(), version)
        assertArrayEquals(payload, decoded)
    }

    @Test
    fun testBip39Mnemonic() {
        val service = Bip39MnemonicService()
        val mnemonic = service.generateMnemonic()
        val words = mnemonic.split(" ")
        assertEquals(12, words.size)
        
        val validation = service.validateMnemonic(mnemonic)
        assertEquals(MnemonicValidationResult.VALID, validation)

        // Invalid word count
        val shortMnemonic = words.take(11).joinToString(" ")
        assertEquals(MnemonicValidationResult.INVALID_LENGTH, service.validateMnemonic(shortMnemonic))

        // Invalid word
        val badWordMnemonic = mnemonic.replace(words[0], "invalidword")
        assertEquals(MnemonicValidationResult.INVALID_WORD, service.validateMnemonic(badWordMnemonic))

        // Invalid checksum
        var tamperedMnemonic = mnemonic
        for (candidate in listOf("abandon", "about", "academic", "accept")) {
            if (candidate != words[11]) {
                val temp = words.take(11).joinToString(" ") + " " + candidate
                if (service.validateMnemonic(temp) == MnemonicValidationResult.INVALID_CHECKSUM) {
                    tamperedMnemonic = temp
                    break
                }
            }
        }
        val result = service.validateMnemonic(tamperedMnemonic)
        assertEquals(MnemonicValidationResult.INVALID_CHECKSUM, result)
    }

    @Test
    fun testBip32Derivation() {
        val service = Bip39MnemonicService()
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val seed = service.deriveSeed(mnemonic)
        val keyNode = Bip32.derivePath(seed, "m/44'/5'/0'/0/0")
        assertNotNull(keyNode.privateKey)
        assertEquals(32, keyNode.privateKey.size)
        assertEquals(32, keyNode.chainCode.size)
    }

    @Test
    fun testAddressValidator() {
        val validP2pkh = "PRfbEeHAKKbz6Voz85WJudrJwTA3ZbHunb"
        assertTrue(AddressValidator.isValidAddress(validP2pkh))

        // Invalid address: blank
        assertFalse(AddressValidator.isValidAddress(""))

        // Invalid address: wrong prefix
        assertFalse(AddressValidator.isValidAddress("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"))

        // Invalid address: mock address
        assertFalse(AddressValidator.isValidAddress("PMockPepepowAddress123"))
    }

    @Test
    fun testTxMetadataManagerSorting() {
        val mockPrefs = MockSharedPreferences()
        val manager = net.pepepow.wallet.data.TxMetadataManager(mockPrefs)

        val txId1 = "tx_first_5_pepew"
        val txId2 = "tx_second_4_pepew"

        val now = System.currentTimeMillis()
        manager.setFirstSeenTimestamp(txId1, now)
        manager.setInsertionOrder(txId1, manager.getNextSequence())

        val now2 = now + 1000
        manager.setFirstSeenTimestamp(txId2, now2)
        manager.setInsertionOrder(txId2, manager.getNextSequence())

        assertEquals(now, manager.getFirstSeenTimestamp(txId1))
        assertEquals(now2, manager.getFirstSeenTimestamp(txId2))
        assertTrue(manager.getInsertionOrder(txId2) > manager.getInsertionOrder(txId1))

        val apiTx1 = net.pepepow.wallet.data.Transaction(
            txId = txId1,
            address = "addr1",
            amount = 5.0,
            timestamp = 1600000000000L,
            isSend = true,
            isPending = false
        )
        val apiTx2 = net.pepepow.wallet.data.Transaction(
            txId = txId2,
            address = "addr2",
            amount = 4.0,
            timestamp = 1600000000000L,
            isSend = true,
            isPending = false
        )

        val sortTransactions = { txs: List<net.pepepow.wallet.data.Transaction> ->
            txs.sortedWith { t1, t2 ->
                val tsCompare = t2.timestamp.compareTo(t1.timestamp)
                if (tsCompare != 0) {
                    tsCompare
                } else {
                    val ord1 = manager.getInsertionOrder(t1.txId)
                    val ord2 = manager.getInsertionOrder(t2.txId)
                    ord2.compareTo(ord1)
                }
            }
        }

        val list1 = listOf(apiTx1, apiTx2)
        val sorted1 = sortTransactions(list1)
        assertEquals(txId2, sorted1[0].txId)
        assertEquals(txId1, sorted1[1].txId)

        val list2 = listOf(apiTx2, apiTx1)
        val sorted2 = sortTransactions(list2)
        assertEquals(txId2, sorted2[0].txId)
        assertEquals(txId1, sorted2[1].txId)
    }

    class MockSharedPreferences : android.content.SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = map
        override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = (map[key] as? Set<String>) ?: defValues
        override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String): Boolean = map.containsKey(key)
        override fun edit(): android.content.SharedPreferences.Editor = MockEditor()
        override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class MockEditor : android.content.SharedPreferences.Editor {
            private val tempMap = mutableMapOf<String, Any?>()

            override fun putString(key: String, value: String?): android.content.SharedPreferences.Editor { tempMap[key] = value; return this }
            override fun putStringSet(key: String, values: Set<String>?): android.content.SharedPreferences.Editor { tempMap[key] = values; return this }
            override fun putInt(key: String, value: Int): android.content.SharedPreferences.Editor { tempMap[key] = value; return this }
            override fun putLong(key: String, value: Long): android.content.SharedPreferences.Editor { tempMap[key] = value; return this }
            override fun putFloat(key: String, value: Float): android.content.SharedPreferences.Editor { tempMap[key] = value; return this }
            override fun putBoolean(key: String, value: Boolean): android.content.SharedPreferences.Editor { tempMap[key] = value; return this }
            override fun remove(key: String): android.content.SharedPreferences.Editor { tempMap[key] = null; return this }
            override fun clear(): android.content.SharedPreferences.Editor { map.clear(); tempMap.clear(); return this }
            override fun commit(): Boolean {
                for ((k, v) in tempMap) {
                    if (v == null) map.remove(k) else map[k] = v
                }
                return true
            }
            override fun apply() {
                commit()
            }
        }
    }
}
