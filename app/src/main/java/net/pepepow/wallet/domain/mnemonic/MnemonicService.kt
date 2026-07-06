package net.pepepow.wallet.domain.mnemonic

enum class MnemonicValidationResult {
    VALID,
    INVALID_LENGTH,
    INVALID_WORD,
    INVALID_CHECKSUM
}

interface MnemonicService {
    fun generateMnemonic(): String
    fun validateMnemonic(mnemonic: String): MnemonicValidationResult
    fun deriveSeed(mnemonic: String, passphrase: String = ""): ByteArray
}
