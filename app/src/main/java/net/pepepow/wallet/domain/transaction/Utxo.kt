package net.pepepow.wallet.domain.transaction

data class Utxo(
    val txid: String,
    val vout: Int,
    val satoshis: Long,
    val scriptPubKey: String
)

data class TransactionInput(
    val txid: String,
    val vout: Int,
    val scriptSig: ByteArray,
    val sequence: Long = 0xffffffffL
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TransactionInput
        if (txid != other.txid) return false
        if (vout != other.vout) return false
        if (!scriptSig.contentEquals(other.scriptSig)) return false
        if (sequence != other.sequence) return false
        return true
    }

    override fun hashCode(): Int {
        var result = txid.hashCode()
        result = 31 * result + vout
        result = 31 * result + scriptSig.contentHashCode()
        result = 31 * result + sequence.hashCode()
        return result
    }
}

data class TransactionOutput(
    val amount: Long,
    val scriptPubKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TransactionOutput
        if (amount != other.amount) return false
        if (!scriptPubKey.contentEquals(other.scriptPubKey)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = amount.hashCode()
        result = 31 * result + scriptPubKey.contentHashCode()
        return result
    }
}
