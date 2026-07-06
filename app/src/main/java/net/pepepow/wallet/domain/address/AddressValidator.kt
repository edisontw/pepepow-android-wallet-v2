package net.pepepow.wallet.domain.address

object AddressValidator {
    fun isValidAddress(address: String): Boolean {
        if (address.isBlank()) return false
        
        try {
            val (version, payload) = Base58Check.decodeChecked(address.trim())
            if (payload.size != 20) return false
            return version == PepepowNetworkParams.P2PKH_VERSION || version == PepepowNetworkParams.P2SH_VERSION
        } catch (e: Exception) {
            return false
        }
    }
}
