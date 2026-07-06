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
        val tamperedMnemonic = mnemonic.replace(words[11], if (words[11] == "abandon") "about" else "abandon")
        // Tampering might accidentally result in a valid checksum by chance (1 in 16), but most likely not
        val result = service.validateMnemonic(tamperedMnemonic)
        assertTrue(result == MnemonicValidationResult.INVALID_CHECKSUM || result == MnemonicValidationResult.INVALID_WORD)
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
}
