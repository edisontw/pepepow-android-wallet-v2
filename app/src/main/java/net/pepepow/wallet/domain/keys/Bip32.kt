package net.pepepow.wallet.domain.keys

import org.bouncycastle.asn1.x9.ECNamedCurveTable
import java.math.BigInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Bip32 {
    private val curve = ECNamedCurveTable.getByName("secp256k1")
    private val N = curve.n

    data class KeyNode(val privateKey: ByteArray, val chainCode: ByteArray)

    fun masterNodeFromSeed(seed: ByteArray): KeyNode {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec("Bitcoin seed".toByteArray(Charsets.UTF_8), "HmacSHA512"))
        val iBytes = mac.doFinal(seed)
        val privKeyBytes = iBytes.copyOfRange(0, 32)
        val chainCode = iBytes.copyOfRange(32, 64)
        return KeyNode(privKeyBytes, chainCode)
    }

    fun deriveChild(parent: KeyNode, index: Int): KeyNode {
        val isHardened = (index and 0x80000000.toInt()) != 0
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(parent.chainCode, "HmacSHA512"))
        
        val data = if (isHardened) {
            val buf = ByteArray(1 + 32 + 4)
            buf[0] = 0x00
            System.arraycopy(parent.privateKey, 0, buf, 1, 32)
            writeUInt32BE(buf, 33, index)
            buf
        } else {
            val pubKey = Secp256k1.getPublicKey(parent.privateKey, true)
            val buf = ByteArray(33 + 4)
            System.arraycopy(pubKey, 0, buf, 0, 33)
            writeUInt32BE(buf, 33, index)
            buf
        }
        
        val iBytes = mac.doFinal(data)
        val tBytes = iBytes.copyOfRange(0, 32)
        val chainCode = iBytes.copyOfRange(32, 64)
        
        val tweak = BigInteger(1, tBytes)
        val parentPriv = BigInteger(1, parent.privateKey)
        val childPriv = tweak.add(parentPriv).mod(N)
        
        val childPrivBytes = ByteArray(32)
        val temp = childPriv.toByteArray()
        if (temp.size <= 32) {
            System.arraycopy(temp, 0, childPrivBytes, 32 - temp.size, temp.size)
        } else {
            System.arraycopy(temp, temp.size - 32, childPrivBytes, 0, 32)
        }
        
        return KeyNode(childPrivBytes, chainCode)
    }

    fun derivePath(seed: ByteArray, path: String): KeyNode {
        val clean = path.trim()
        if (!clean.startsWith("m")) throw IllegalArgumentException("Path must start with m")
        var node = masterNodeFromSeed(seed)
        if (clean == "m") return node
        
        val parts = clean.split("/")
        for (i in 1 until parts.size) {
            val part = parts[i]
            val hardened = part.endsWith("'") || part.endsWith("h")
            val indexStr = if (hardened) part.dropLast(1) else part
            var index = indexStr.toInt()
            if (hardened) {
                index = index or 0x80000000.toInt()
            }
            node = deriveChild(node, index)
        }
        return node
    }

    private fun writeUInt32BE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value ushr 24).toByte()
        buf[offset + 1] = (value ushr 16).toByte()
        buf[offset + 2] = (value ushr 8).toByte()
        buf[offset + 3] = value.toByte()
    }
}
