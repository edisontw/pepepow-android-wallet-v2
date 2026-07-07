package net.pepepow.wallet.domain.transaction

import net.pepepow.wallet.domain.address.Base58Check
import net.pepepow.wallet.domain.keys.Secp256k1
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object TransactionBuilder {

    fun createAndSignTransaction(
        privateKey: ByteArray,
        utxos: List<Utxo>,
        recipientAddress: String,
        amountSat: Long,
        feeSat: Long,
        senderAddress: String
    ): String {
        val totalNeededSat = amountSat + feeSat

        // 1. Verify and sum UTXOs
        var selectedSatoshis = 0L
        val selectedUtxos = utxos
        for (utxo in selectedUtxos) {
            selectedSatoshis += utxo.satoshis
        }

        if (selectedSatoshis < totalNeededSat) {
            throw IllegalArgumentException(
                "Insufficient UTXO balance. Selected inputs total ${String.format("%.4f", selectedSatoshis / 1e8)} PEPEW, but need ${String.format("%.4f", totalNeededSat / 1e8)} PEPEW."
            )
        }

        val changeSat = selectedSatoshis - totalNeededSat

        // 2. Decode recipient address & create outputs
        val recipientHash = decodeAddressToHash160(recipientAddress)
        val recipientScript = getP2PKHScript(recipientHash)

        val outputs = mutableListOf<TransactionOutput>()
        outputs.add(TransactionOutput(amountSat, recipientScript))

        // Dust threshold for P2PKH is 546 satoshis
        if (changeSat >= 546L) {
            val senderHash = decodeAddressToHash160(senderAddress)
            val changeScript = getP2PKHScript(senderHash)
            outputs.add(TransactionOutput(changeSat, changeScript))
        }

        // 3. Sign each input
        val signedScriptSigs = mutableListOf<ByteArray>()
        val pubKey = Secp256k1.getPublicKey(privateKey, true)

        for (i in selectedUtxos.indices) {
            val signInputs = selectedUtxos.mapIndexed { idx, utxo ->
                val scriptSig = if (idx == i) hexToBytes(utxo.scriptPubKey) else ByteArray(0)
                TransactionInput(utxo.txid, utxo.vout, scriptSig)
            }

            val bos = ByteArrayOutputStream()
            bos.write(writeUInt32(1)) // version
            bos.write(writeVarInt(signInputs.size.toLong()))
            for (input in signInputs) {
                bos.write(hexToBytes(input.txid).clone().reversedArray())
                bos.write(writeUInt32(input.vout))
                bos.write(writeVarInt(input.scriptSig.size.toLong()))
                bos.write(input.scriptSig)
                bos.write(writeUInt32(0xffffffffL.toInt()))
            }
            bos.write(writeVarInt(outputs.size.toLong()))
            for (output in outputs) {
                bos.write(writeInt64(output.amount))
                bos.write(writeVarInt(output.scriptPubKey.size.toLong()))
                bos.write(output.scriptPubKey)
            }
            bos.write(writeUInt32(0)) // locktime
            bos.write(writeUInt32(1)) // sighash type

            val sighash = doubleSha256(bos.toByteArray())
            val sig = Secp256k1.sign(sighash, privateKey)
            
            val sigWithHashType = ByteArray(sig.size + 1)
            System.arraycopy(sig, 0, sigWithHashType, 0, sig.size)
            sigWithHashType[sig.size] = 1

            val scriptSigBos = ByteArrayOutputStream()
            scriptSigBos.write(writeVarInt(sigWithHashType.size.toLong()))
            scriptSigBos.write(sigWithHashType)
            scriptSigBos.write(writeVarInt(pubKey.size.toLong()))
            scriptSigBos.write(pubKey)

            signedScriptSigs.add(scriptSigBos.toByteArray())
        }

        // 4. Final Transaction Serialization
        val finalBos = ByteArrayOutputStream()
        finalBos.write(writeUInt32(1)) // version
        finalBos.write(writeVarInt(selectedUtxos.size.toLong()))
        for (i in selectedUtxos.indices) {
            val utxo = selectedUtxos[i]
            finalBos.write(hexToBytes(utxo.txid).clone().reversedArray())
            finalBos.write(writeUInt32(utxo.vout))
            val scriptSig = signedScriptSigs[i]
            finalBos.write(writeVarInt(scriptSig.size.toLong()))
            finalBos.write(scriptSig)
            finalBos.write(writeUInt32(0xffffffffL.toInt()))
        }
        finalBos.write(writeVarInt(outputs.size.toLong()))
        for (output in outputs) {
            finalBos.write(writeInt64(output.amount))
            finalBos.write(writeVarInt(output.scriptPubKey.size.toLong()))
            finalBos.write(output.scriptPubKey)
        }
        finalBos.write(writeUInt32(0)) // locktime

        return bytesToHex(finalBos.toByteArray())
    }

    private fun decodeAddressToHash160(address: String): ByteArray {
        val (_, payload) = Base58Check.decodeChecked(address.trim())
        if (payload.size != 20) {
            throw IllegalArgumentException("Invalid address size")
        }
        return payload
    }

    fun getP2PKHScript(hash160: ByteArray): ByteArray {
        val script = ByteArray(25)
        script[0] = 0x76.toByte() // OP_DUP
        script[1] = 0xa9.toByte() // OP_HASH160
        script[2] = 0x14.toByte() // Push 20 bytes
        System.arraycopy(hash160, 0, script, 3, 20)
        script[23] = 0x88.toByte() // OP_EQUALVERIFY
        script[24] = 0xac.toByte() // OP_CHECKSIG
        return script
    }

    fun doubleSha256(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(md.digest(data))
    }

    fun writeUInt32(value: Int): ByteArray {
        val bytes = ByteArray(4)
        bytes[0] = value.toByte()
        bytes[1] = (value ushr 8).toByte()
        bytes[2] = (value ushr 16).toByte()
        bytes[3] = (value ushr 24).toByte()
        return bytes
    }

    fun writeInt64(value: Long): ByteArray {
        val bytes = ByteArray(8)
        for (i in 0..7) {
            bytes[i] = (value ushr (i * 8)).toByte()
        }
        return bytes
    }

    fun writeVarInt(value: Long): ByteArray {
        return when {
            value < 0xfdL -> byteArrayOf(value.toByte())
            value <= 0xffffL -> {
                val bytes = ByteArray(3)
                bytes[0] = 0xfd.toByte()
                bytes[1] = value.toByte()
                bytes[2] = (value ushr 8).toByte()
                bytes
            }
            value <= 0xffffffffL -> {
                val bytes = ByteArray(5)
                bytes[0] = 0xfe.toByte()
                bytes[1] = value.toByte()
                bytes[2] = (value ushr 8).toByte()
                bytes[3] = (value ushr 16).toByte()
                bytes[4] = (value ushr 24).toByte()
                bytes
            }
            else -> {
                val bytes = ByteArray(9)
                bytes[0] = 0xff.toByte()
                for (i in 0..7) {
                    bytes[1 + i] = (value ushr (i * 8)).toByte()
                }
                bytes
            }
        }
    }

    fun hexToBytes(hex: String): ByteArray {
        val clean = if (hex.startsWith("0x")) hex.substring(2) else hex
        require(clean.length % 2 == 0) { "Invalid hex string length" }
        val bytes = ByteArray(clean.length / 2)
        for (i in bytes.indices) {
            bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }

    fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b.toInt() and 0xFF))
        }
        return sb.toString()
    }
}
