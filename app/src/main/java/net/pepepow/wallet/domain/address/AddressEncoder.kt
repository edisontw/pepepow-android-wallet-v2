package net.pepepow.wallet.domain.address

import net.pepepow.wallet.domain.keys.Secp256k1
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import java.security.MessageDigest

object AddressEncoder {
    
    fun getAddressFromPrivateKey(privKey: ByteArray): String {
        val pubKey = Secp256k1.getPublicKey(privKey, true)
        val hash160 = hash160(pubKey)
        return Base58Check.encode(PepepowNetworkParams.P2PKH_VERSION, hash160)
    }

    fun getWifFromPrivateKey(privKey: ByteArray): String {
        val payload = ByteArray(33)
        System.arraycopy(privKey, 0, payload, 0, 32)
        payload[32] = 0x01 // compressed public key flag
        return Base58Check.encode(PepepowNetworkParams.WIF_VERSION, payload)
    }

    fun hash160(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val sha256 = md.digest(data)
        val ripemd = RIPEMD160Digest()
        ripemd.update(sha256, 0, sha256.size)
        val out = ByteArray(20)
        ripemd.doFinal(out, 0)
        return out
    }
}
