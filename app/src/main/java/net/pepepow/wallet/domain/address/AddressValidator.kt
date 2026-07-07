package net.pepepow.wallet.domain.address

object AddressValidator {
    sealed class AddressValidationResult {
        object ValidP2PKH : AddressValidationResult()
        object UnsupportedAddressType : AddressValidationResult()
        object InvalidAddress : AddressValidationResult()
    }

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

    fun validateAddress(address: String): AddressValidationResult {
        if (address.isBlank()) return AddressValidationResult.InvalidAddress
        return try {
            val (version, payload) = Base58Check.decodeChecked(address.trim())
            if (payload.size != 20) return AddressValidationResult.InvalidAddress
            when (version) {
                PepepowNetworkParams.P2PKH_VERSION -> AddressValidationResult.ValidP2PKH
                PepepowNetworkParams.P2SH_VERSION -> AddressValidationResult.UnsupportedAddressType
                else -> AddressValidationResult.UnsupportedAddressType
            }
        } catch (e: Exception) {
            AddressValidationResult.InvalidAddress
        }
    }
}
