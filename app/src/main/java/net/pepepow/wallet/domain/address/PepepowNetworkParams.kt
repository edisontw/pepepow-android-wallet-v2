package net.pepepow.wallet.domain.address

object PepepowNetworkParams {
    const val P2PKH_VERSION: Byte = 55
    const val P2SH_VERSION: Byte = 16
    const val WIF_VERSION: Byte = 204.toByte()
    const val COIN_TYPE = 5
    const val DEFAULT_PATH = "m/44'/5'/0'/0/0"
}
