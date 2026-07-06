package net.pepepow.wallet.domain.keys

import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x9.ECNamedCurveTable
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import java.math.BigInteger

object Secp256k1 {
    private val curve = ECNamedCurveTable.getByName("secp256k1")
    val domain = ECDomainParameters(curve.curve, curve.g, curve.n, curve.h)

    fun getPublicKey(privateKey: ByteArray, compressed: Boolean = true): ByteArray {
        val privKeyInt = BigInteger(1, privateKey)
        val q = curve.g.multiply(privKeyInt)
        return q.getEncoded(compressed)
    }

    fun sign(messageHash: ByteArray, privateKey: ByteArray): ByteArray {
        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        val privKeyParams = ECPrivateKeyParameters(BigInteger(1, privateKey), domain)
        signer.init(true, privKeyParams)
        val components = signer.generateSignature(messageHash)
        
        val r = components[0]
        var s = components[1]
        
        // Canonicalize s to be in the lower half of the curve order
        val halfN = curve.n.shiftRight(1)
        if (s > halfN) {
            s = curve.n.subtract(s)
        }
        
        val der = DERSequence(
            arrayOf(
                ASN1Integer(r),
                ASN1Integer(s)
            )
        )
        return der.encoded
    }
}
