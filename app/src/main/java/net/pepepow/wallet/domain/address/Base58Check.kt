package net.pepepow.wallet.domain.address

import java.security.MessageDigest

object Base58Check {
    private val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray()
    private val ALPHABET_INDEX = IntArray(128) { -1 }.apply {
        for (i in ALPHABET.indices) {
            this[ALPHABET[i].code] = i
        }
    }

    fun encode(version: Byte, payload: ByteArray): String {
        val addressBytes = ByteArray(1 + payload.size + 4)
        addressBytes[0] = version
        System.arraycopy(payload, 0, addressBytes, 1, payload.size)
        
        val checksum = doubleSha256(addressBytes, 0, 1 + payload.size)
        System.arraycopy(checksum, 0, addressBytes, 1 + payload.size, 4)
        
        return encodeBase58(addressBytes)
    }

    /**
     * Decodes a Base58Check string.
     * Returns a Pair of (version byte, payload byte array).
     */
    fun decodeChecked(input: String): Pair<Byte, ByteArray> {
        val decoded = decodeBase58(input)
        if (decoded.size < 5) throw IllegalArgumentException("Address too short")
        val version = decoded[0]
        val payloadSize = decoded.size - 5
        val payload = ByteArray(payloadSize)
        System.arraycopy(decoded, 1, payload, 0, payloadSize)
        val checksum = ByteArray(4)
        System.arraycopy(decoded, decoded.size - 4, checksum, 0, 4)
        
        val doubleHash = doubleSha256(decoded, 0, decoded.size - 4)
        for (i in 0..3) {
            if (doubleHash[i] != checksum[i]) {
                throw IllegalArgumentException("Invalid checksum")
            }
        }
        return Pair(version, payload)
    }

    private fun doubleSha256(data: ByteArray, offset: Int, length: Int): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(data, offset, length)
        val hash1 = md.digest()
        return md.digest(hash1)
    }

    private fun encodeBase58(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var zeros = 0
        while (zeros < input.size && input[zeros].toInt() == 0) {
            zeros++
        }
        val encoded = StringBuilder()
        val temp = input.copyOf()
        var start = zeros
        while (start < temp.size) {
            val mod = divmod(temp, start, 256, 58)
            encoded.append(ALPHABET[mod.toInt()])
            if (temp[start].toInt() == 0) {
                start++
            }
        }
        for (i in 0 until zeros) {
            encoded.append(ALPHABET[0])
        }
        return encoded.reverse().toString()
    }

    private fun decodeBase58(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)
        var zeros = 0
        while (zeros < input.length && input[zeros] == ALPHABET[0]) {
            zeros++
        }
        val bytes = ByteArray(input.length)
        var start = zeros
        var length = 0
        while (start < input.length) {
            val charCode = input[start].code
            if (charCode >= 128 || ALPHABET_INDEX[charCode] == -1) {
                throw IllegalArgumentException("Invalid character: ${input[start]}")
            }
            val digit = ALPHABET_INDEX[charCode]
            var remainder = digit
            for (i in bytes.size - 1 downTo bytes.size - length) {
                val temp = (bytes[i].toInt() and 0xFF) * 58 + remainder
                bytes[i] = temp.toByte()
                remainder = temp ushr 8
            }
            while (remainder > 0) {
                bytes[bytes.size - 1 - length] = remainder.toByte()
                remainder = remainder ushr 8
                length++
            }
            start++
        }
        var firstNonZero = bytes.size - length
        while (firstNonZero < bytes.size && bytes[firstNonZero].toInt() == 0) {
            firstNonZero++
        }
        val result = ByteArray(zeros + bytes.size - firstNonZero)
        System.arraycopy(bytes, firstNonZero, result, zeros, bytes.size - firstNonZero)
        return result
    }

    private fun divmod(number: ByteArray, firstNonZero: Int, base: Int, divisor: Int): Byte {
        var remainder = 0
        for (i in firstNonZero until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder.toByte()
    }
}
