package net.pepepow.wallet.domain.mnemonic

import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class Bip39MnemonicService : MnemonicService {

    private val wordList = Bip39Wordlist.words
    private val wordIndexMap = wordList.withIndex().associate { it.value to it.index }

    override fun generateMnemonic(): String {
        val entropy = ByteArray(16) // 128 bits
        SecureRandom().nextBytes(entropy)

        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(entropy)
        val checksumByte = hash[0]

        val bits = BooleanArray(132)
        for (i in 0 until 128) {
            val b = entropy[i / 8].toInt()
            val bit = (b ushr (7 - (i % 8))) and 1
            bits[i] = bit == 1
        }
        for (i in 0 until 4) {
            val b = checksumByte.toInt()
            val bit = (b ushr (7 - i)) and 1
            bits[128 + i] = bit == 1
        }

        val words = mutableListOf<String>()
        for (i in 0 until 12) {
            var index = 0
            for (j in 0 until 11) {
                index = index shl 1
                if (bits[i * 11 + j]) {
                    index = index or 1
                }
            }
            words.add(wordList[index])
        }

        return words.joinToString(" ")
    }

    override fun validateMnemonic(mnemonic: String): MnemonicValidationResult {
        val trimmed = mnemonic.normalizeMnemonic()
        if (trimmed.isEmpty()) return MnemonicValidationResult.INVALID_LENGTH
        val words = trimmed.split(" ")
        if (words.size != 12) return MnemonicValidationResult.INVALID_LENGTH

        val wordIndices = IntArray(12)
        for (i in 0 until 12) {
            val index = wordIndexMap[words[i]] ?: return MnemonicValidationResult.INVALID_WORD
            wordIndices[i] = index
        }

        val bits = BooleanArray(132)
        for (i in 0 until 12) {
            val index = wordIndices[i]
            for (j in 0 until 11) {
                val bit = (index ushr (10 - j)) and 1
                bits[i * 11 + j] = bit == 1
            }
        }

        val entropy = ByteArray(16)
        for (i in 0 until 128) {
            if (bits[i]) {
                val bytePos = i / 8
                val bitPos = 7 - (i % 8)
                entropy[bytePos] = (entropy[bytePos].toInt() or (1 shl bitPos)).toByte()
            }
        }

        var checksum = 0
        for (i in 0 until 4) {
            checksum = checksum shl 1
            if (bits[128 + i]) {
                checksum = checksum or 1
            }
        }

        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(entropy)
        val expectedChecksum = (hash[0].toInt() and 0xFF) ushr 4

        return if (checksum == expectedChecksum) {
            MnemonicValidationResult.VALID
        } else {
            MnemonicValidationResult.INVALID_CHECKSUM
        }
    }

    override fun deriveSeed(mnemonic: String, passphrase: String): ByteArray {
        val normalizedMnemonic = mnemonic.normalizeMnemonic()
        val normalizedPassphrase = Normalizer.normalize(passphrase, Normalizer.Form.NFKD)
        val salt = "mnemonic" + normalizedPassphrase
        
        val spec = PBEKeySpec(
            normalizedMnemonic.toCharArray(),
            salt.toByteArray(Charsets.UTF_8),
            2048,
            512
        )
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        return skf.generateSecret(spec).encoded
    }

    private fun String.normalizeMnemonic(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFKD)
            .lowercase()
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
