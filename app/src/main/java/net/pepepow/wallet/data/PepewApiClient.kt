package net.pepepow.wallet.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.pepepow.wallet.domain.address.Base58Check
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Real PEPEW Light API client.
 *
 * Security boundary:
 * - This client only performs public read/write requests.
 * - It never receives mnemonic, seed, private key, or WIF.
 * - For broadcast, it only receives the signed raw transaction hex.
 */
class PepewApiClient(
    val baseUrl: String = "https://light.pepepow.net/",
    private val timeoutMs: Int = 30_000
) {
    suspend fun getHealth(): ApiHealth = withContext(Dispatchers.IO) {
        val body = requestHttp("/api/health")
        val json = JSONObject(body)
        ApiHealth(
            ok = json.optBoolean("ok", true),
            status = json.optString("status", "unknown")
        )
    }

    suspend fun getPrice(): ApiPrice = withContext(Dispatchers.IO) {
        val body = requestHttp("/api/price")
        val json = JSONObject(body)
        val ok = json.optBoolean("ok", true)
        val priceUsdtStr = when {
            json.has("price_usdt") && !json.isNull("price_usdt") -> json.optString("price_usdt")
            json.has("price") && !json.isNull("price") -> json.optString("price")
            else -> null
        }
        val priceUsdt = try {
            priceUsdtStr?.replace(",", "")?.trim()?.let {
                java.math.BigDecimal(it).toDouble()
            }
        } catch (_: Exception) {
            priceUsdtStr?.replace(",", "")?.trim()?.toDoubleOrNull()
        }
        ApiPrice(ok = ok, priceUsdt = priceUsdt)
    }

    suspend fun getStatus(): ApiStatusResponse = withContext(Dispatchers.IO) {
        val body = requestHttp("/api/status")
        val json = JSONObject(body)
        ApiStatusResponse(
            ok = !json.optBoolean("error", false),
            status = json.optString("status", json.optString("state", "unknown")),
            height = json.optLongOrNull("height") ?: json.optLongOrNull("block_height")
        )
    }

    suspend fun getAddressSummary(address: String): ApiAddressSummary = withContext(Dispatchers.IO) {
        val safeAddress = encodePathSegment(address)
        val body = requestHttp("/api/wallet/address/$safeAddress")
        val json = JSONObject(body)
        parseAddressSummary(json, address)
    }

    suspend fun getHistory(address: String, limit: Int = 50, offset: Int = 0): List<ApiTransaction> = withContext(Dispatchers.IO) {
        val safeAddress = encodePathSegment(address)
        val body = requestHttp("/api/wallet/history/$safeAddress?limit=$limit&offset=$offset")
        
        val historyArray = if (body.trim().startsWith("[")) {
            JSONArray(body)
        } else {
            val json = JSONObject(body)
            when {
                json.has("history") -> json.optJSONArray("history") ?: JSONArray()
                json.has("transactions") -> json.optJSONArray("transactions") ?: JSONArray()
                json.has("items") -> json.optJSONArray("items") ?: JSONArray()
                else -> JSONArray()
            }
        }
        parseHistory(historyArray, address)
    }

    suspend fun getUtxos(address: String): List<ApiUtxo> = withContext(Dispatchers.IO) {
        val safeAddress = encodePathSegment(address)
        val fallbackScriptPubKey = p2pkhScriptForAddress(address)
        val body = requestHttp("/api/wallet/utxo/$safeAddress?fresh=1&t=${System.currentTimeMillis()}")
        
        val utxoArray = if (body.trim().startsWith("[")) {
            JSONArray(body)
        } else {
            val json = JSONObject(body)
            when {
                json.has("utxos") -> json.optJSONArray("utxos")
                json.has("data") -> json.optJSONArray("data")
                else -> null
            } ?: JSONArray()
        }

        val result = mutableListOf<ApiUtxo>()
        for (i in 0 until utxoArray.length()) {
            val item = utxoArray.optJSONObject(i) ?: continue
            val txid = item.optString("txid", item.optString("tx_hash", item.optString("hash", ""))).trim()
            if (txid.length != 64) continue

            val vout = when {
                item.has("vout") -> item.optInt("vout", -1)
                item.has("tx_pos") -> item.optInt("tx_pos", -1)
                item.has("index") -> item.optInt("index", -1)
                item.has("n") -> item.optInt("n", -1)
                else -> -1
            }
            if (vout < 0) continue

            val satoshis = atomsFromUtxo(item)
            if (satoshis <= 0) continue

            val scriptPubKey = item.optString("scriptPubKey", 
                item.optString("script_pub_key", 
                    item.optString("script_pubkey", 
                        item.optString("script", fallbackScriptPubKey)))).trim()
            if (scriptPubKey.isBlank()) continue

            val height = when {
                item.has("height") -> item.optLong("height", 0L)
                item.has("block_height") -> item.optLong("block_height", 0L)
                item.has("status") -> {
                    val statusObj = item.optJSONObject("status")
                    statusObj?.optLong("block_height", 0L) ?: 0L
                }
                else -> 0L
            }

            result.add(ApiUtxo(txid, vout, satoshis, scriptPubKey, height))
        }
        result
    }

    fun parseRawTxFromResponse(body: String): String? {
        val clean = body.trim()
        
        // 8. if body itself is plain raw hex, accept it
        if (clean.matches(Regex("^[0-9a-fA-F]{20,}$")) && clean.length % 2 == 0) {
            return clean.lowercase(Locale.US)
        }
        
        // 1-7. Use regex to find any key-value pairs matching hex values of length >= 20
        val pattern = Regex("\"(?:hex|raw_tx|rawTx|raw)\"\\s*:\\s*\"([0-9a-fA-F]{20,})\"")
        val match = pattern.find(clean)
        if (match != null) {
            val hexVal = match.groupValues[1]
            if (hexVal.length % 2 == 0) {
                return hexVal.lowercase(Locale.US)
            }
        }
        
        try {
            val json = JSONObject(clean)
            
            // Check top level
            val topHex = listOf("hex", "raw_tx", "rawTx").firstNotNullOfOrNull { key ->
                json.optString(key, "").takeIf { it.isNotBlank() }
            }
            if (topHex != null && topHex.matches(Regex("^[0-9a-fA-F]{20,}$")) && topHex.length % 2 == 0) {
                return topHex.lowercase(Locale.US)
            }
            
            // Check data object
            val dataObj = json.optJSONObject("data")
            if (dataObj != null) {
                val dataHex = listOf("hex", "raw", "raw_tx", "rawTx").firstNotNullOfOrNull { key ->
                    dataObj.optString(key, "").takeIf { it.isNotBlank() }
                }
                if (dataHex != null && dataHex.matches(Regex("^[0-9a-fA-F]{20,}$")) && dataHex.length % 2 == 0) {
                    return dataHex.lowercase(Locale.US)
                }
            }
        } catch (_: Exception) {}
        
        return null
    }

    suspend fun getRawTransaction(txid: String): String = withContext(Dispatchers.IO) {
        val safeTxid = encodePathSegment(txid)
        val body = requestHttp("/api/wallet/tx/$safeTxid?raw=1")
        val hex = parseRawTxFromResponse(body)
        if (hex == null) {
            throw PepewApiException(500, "Failed to parse raw transaction hex from response")
        }
        hex
    }

    suspend fun broadcastTransaction(hex: String): String = withContext(Dispatchers.IO) {
        val cleanHex = hex.trim()
        val postBody = JSONObject().put("raw_tx", cleanHex).toString()
        val body = requestHttp("/api/wallet/broadcast", method = "POST", postBody = postBody)
        val trimmedBody = body.trim()

        if (trimmedBody.matches(Regex("^[0-9a-fA-F]{64}$"))) {
            return@withContext trimmedBody.lowercase(Locale.US)
        }

        val json = JSONObject(trimmedBody)
        val apiError = extractApiError(json)
        if (apiError != null) {
            throw PepewApiException(400, apiError)
        }

        val txid = extractTxid(json)
        if (txid == null) {
            throw PepewApiException(500, "Unexpected broadcast response")
        }
        txid
    }

    suspend fun checkHealth(): Boolean = try {
        getHealth().ok
    } catch (_: Exception) {
        false
    }

    private fun atomsFromUtxo(u: JSONObject): Long {
        val explicitAtoms = when {
            u.has("satoshis") && !u.isNull("satoshis") -> u.optLongOrNull("satoshis")
            u.has("value_atoms") && !u.isNull("value_atoms") -> u.optLongOrNull("value_atoms")
            u.has("amount_atoms") && !u.isNull("amount_atoms") -> u.optLongOrNull("amount_atoms")
            u.has("atoms") && !u.isNull("atoms") -> u.optLongOrNull("atoms")
            u.has("value_sats") && !u.isNull("value_sats") -> u.optLongOrNull("value_sats")
            else -> null
        }
        if (explicitAtoms != null) {
            return explicitAtoms
        }
        val valueStr = when {
            u.has("value") && !u.isNull("value") -> u.optString("value")
            u.has("amount") && !u.isNull("amount") -> u.optString("amount")
            u.has("value_pepew") && !u.isNull("value_pepew") -> u.optString("value_pepew")
            u.has("amount_pepew") && !u.isNull("amount_pepew") -> u.optString("amount_pepew")
            else -> "0"
        }
        val valueDouble = valueStr.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        val isLargeInteger = valueDouble >= 1_000_000.0 && valueDouble == Math.floor(valueDouble)
        return if (isLargeInteger) {
            Math.round(valueDouble)
        } else {
            Math.round(valueDouble * 1e8)
        }
    }

    private fun parseAddressSummary(json: JSONObject, fallbackAddress: String): ApiAddressSummary {
        val balance = json.optJSONObject("balance")
        val confirmedPepew = when {
            balance == null -> json.optString("confirmed_pepew", json.optString("balance", "0"))
            balance.has("confirmed_pepew") -> balance.optString("confirmed_pepew", "0")
            balance.has("total_pepew") -> balance.optString("total_pepew", "0")
            balance.has("confirmed") -> satoshiLikeToPepew(balance.optLong("confirmed", 0L))
            else -> "0"
        }

        val unconfirmedPepew = when {
            balance == null -> json.optString("unconfirmed_pepew", "0")
            balance.has("unconfirmed_pepew") -> balance.optString("unconfirmed_pepew", "0")
            balance.has("unconfirmed") -> satoshiLikeToPepew(balance.optLong("unconfirmed", 0L))
            else -> "0"
        }

        val responseAddress = json.optString("address", fallbackAddress)
        val history = json.optJSONArray("history")?.let { parseHistory(it, responseAddress) } ?: emptyList()

        return ApiAddressSummary(
            address = responseAddress,
            confirmedPepew = confirmedPepew.toSafeDouble(),
            unconfirmedPepew = unconfirmedPepew.toSafeDouble(),
            history = history,
            source = json.optString("source", "unknown")
        )
    }

    private fun parseHistory(array: JSONArray, ownAddress: String): List<ApiTransaction> {
        val result = mutableListOf<ApiTransaction>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val txid = item.optString("txid", item.optString("tx_hash", item.optString("hash", ""))).trim()
            if (txid.isBlank()) continue

            val direction = item.optString("direction").lowercase(Locale.US)
            val txType = item.optString("type").lowercase(Locale.US)
            val fromAddress = item.optString("from_address", item.optString("from", item.optString("sender", item.optString("source", ""))))
            val toAddress = item.optString("to_address", item.optString("to", item.optString("recipient", item.optString("destination", ""))))
            val hasOwnInput = containsAddress(item, ownAddress, listOf("inputs", "vin", "ins"))
            val hasOwnOutput = containsAddress(item, ownAddress, listOf("outputs", "vout", "outs"))

            val itemStr = item.toString()
            val ownAddressCount = if (ownAddress.isNotBlank()) itemStr.split(ownAddress).size - 1 else 0
            val parsedAmount = firstPepewAmount(item) ?: continue

            val isSelfTransfer = item.optBoolean("is_self_transfer", false) ||
                direction.contains("self") ||
                txType.contains("self") ||
                (fromAddress == ownAddress && toAddress == ownAddress) ||
                (parsedAmount < 0.0 && ownAddressCount >= 2) ||
                (hasOwnInput && ownAddressCount >= 2) ||
                (hasOwnInput && hasOwnOutput && !containsExternalOutput(item, ownAddress))

            val amount = if (isSelfTransfer) 0.001 else parsedAmount

            val isSend = when {
                isSelfTransfer -> true
                item.has("is_send") -> item.optBoolean("is_send")
                direction.contains("sent") || direction.contains("send") -> true
                fromAddress == ownAddress -> true
                hasOwnInput -> true
                parsedAmount < 0.0 -> true
                else -> false
            }

            val timestampSeconds = item.optLongOrNull("timestamp") ?: item.optLongOrNull("time")
            val height = item.optLongOrNull("height")
            val pending = item.optBoolean("pending", false) ||
                item.optBoolean("is_mempool", false) ||
                height == 0L ||
                height == -1L

            result += ApiTransaction(
                txid = txid,
                address = item.optString("address", txid),
                amount = kotlin.math.abs(amount),
                timestampMillis = (timestampSeconds ?: (System.currentTimeMillis() / 1000L)) * 1000L,
                isSend = isSend,
                isPending = pending,
                isSelfTransfer = isSelfTransfer
            )
        }
        return result
    }

    private fun requestHttp(path: String, method: String = "GET", postBody: String? = null): String {
        var attempts = 0
        val maxAttempts = if (method == "GET") 2 else 1
        var lastException: Exception? = null

        while (attempts < maxAttempts) {
            attempts++
            val normalizedBase = baseUrl.trimEnd('/')
            val normalizedPath = if (path.startsWith("/")) path else "/$path"
            val url = URL(normalizedBase + normalizedPath)
            var connection: HttpURLConnection? = null
            try {
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "PEPEW-Android-Wallet-V2")
                    if (postBody != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                    }
                }

                if (postBody != null) {
                    connection.outputStream.use { os ->
                        os.write(postBody.toByteArray(Charsets.UTF_8))
                    }
                }
                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) connection.inputStream else (connection.errorStream ?: connection.inputStream)
                val body = if (stream != null) {
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    ""
                }
                if (statusCode !in 200..299) {
                    val message = if (body.isNotBlank()) parseApiError(body) else null
                    throw PepewApiException(statusCode, message ?: "HTTP $statusCode")
                }
                return body
            } catch (e: Exception) {
                lastException = e
                if (attempts < maxAttempts && method == "GET") {
                    try {
                        Thread.sleep(1000)
                    } catch (_: InterruptedException) {}
                    continue
                }
                throw e
            } finally {
                connection?.disconnect()
            }
        }
        throw lastException ?: PepewApiException(500, "Unknown network error")
    }

    private fun parseApiError(body: String): String? = try {
        val json = JSONObject(body)
        extractApiError(json) ?: json.optString("message").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun extractApiError(json: JSONObject): String? {
        if (!json.has("error") || json.isNull("error")) return null
        val raw = json.get("error")
        return when (raw) {
            is Boolean -> if (raw) json.optString("message", "API rejected transaction") else null
            is JSONObject -> raw.optString("message").takeIf { it.isNotBlank() }
                ?: raw.optString("code").takeIf { it.isNotBlank() }
                ?: "API rejected transaction"
            is String -> raw.takeIf { it.isNotBlank() && it.lowercase(Locale.US) != "false" }
            else -> raw.toString().takeIf { it.isNotBlank() && it != "0" }
        }
    }

    private fun extractTxid(json: JSONObject): String? {
        val directKeys = listOf("txid", "tx_hash", "hash", "result", "data")
        for (key in directKeys) {
            if (!json.has(key) || json.isNull(key)) continue
            val value = json.get(key)
            when (value) {
                is String -> if (value.trim().matches(Regex("^[0-9a-fA-F]{64}$"))) return value.trim().lowercase(Locale.US)
                is JSONObject -> extractTxid(value)?.let { return it }
            }
        }
        json.optJSONObject("transaction")?.let { extractTxid(it)?.let { txid -> return txid } }
        return null
    }

    private fun p2pkhScriptForAddress(address: String): String {
        val (_, payload) = Base58Check.decodeChecked(address.trim())
        require(payload.size == 20) { "Invalid address hash size" }
        val script = ByteArray(25)
        script[0] = 0x76.toByte()
        script[1] = 0xa9.toByte()
        script[2] = 0x14.toByte()
        System.arraycopy(payload, 0, script, 3, 20)
        script[23] = 0x88.toByte()
        script[24] = 0xac.toByte()
        return script.joinToString("") { String.format("%02x", it.toInt() and 0xff) }
    }

    private fun firstPepewAmount(json: JSONObject): Double? {
        val pepewKeys = listOf(
            "address_delta_pepew",
            "delta_pepew",
            "balance_delta_pepew",
            "amount_pepew",
            "value_pepew"
        )
        for (key in pepewKeys) {
            if (!json.has(key) || json.isNull(key)) continue
            val raw = json.optString(key)
            val parsed = raw.toSafeDouble()
            if (parsed != 0.0 || raw.trim() in listOf("0", "0.0", "0.00")) return parsed
        }

        val atomKeys = listOf(
            "address_delta_atoms",
            "delta_atoms",
            "balance_delta_atoms",
            "amount_atoms",
            "value_atoms",
            "satoshis"
        )
        for (key in atomKeys) {
            if (!json.has(key) || json.isNull(key)) continue
            val raw = json.optString(key)
            val parsedAtoms = raw.replace(",", "").trim().toLongOrNull() ?: continue
            return parsedAtoms / 100_000_000.0
        }

        val ambiguousKeys = listOf("amount", "value", "balance_delta")
        for (key in ambiguousKeys) {
            if (!json.has(key) || json.isNull(key)) continue
            val raw = json.optString(key)
            val parsed = raw.toSafeDouble()
            if (parsed == 0.0 && raw.trim() !in listOf("0", "0.0", "0.00")) continue
            return if (parsed == Math.floor(parsed) && kotlin.math.abs(parsed) >= 1_000_000.0) {
                parsed / 100_000_000.0
            } else {
                parsed
            }
        }
        return null
    }

    private fun containsAddress(json: JSONObject, ownAddress: String, keys: List<String>): Boolean {
        if (ownAddress.isBlank()) return false
        for (key in keys) {
            val value = json.opt(key) ?: continue
            if (value.toString().contains(ownAddress)) return true
        }
        return false
    }

    private fun containsExternalOutput(json: JSONObject, ownAddress: String): Boolean {
        if (ownAddress.isBlank()) return false
        val outputKeys = listOf("outputs", "vout", "outs")
        for (key in outputKeys) {
            val array = json.optJSONArray(key) ?: continue
            for (i in 0 until array.length()) {
                val output = array.opt(i)?.toString() ?: continue
                if (!output.contains(ownAddress)) return true
            }
        }
        return false
    }

    private fun encodePathSegment(value: String): String =
        URLEncoder.encode(value.trim(), "UTF-8").replace("+", "%20")

    private fun satoshiLikeToPepew(value: Long): String =
        String.format(Locale.US, "%.8f", value / 100_000_000.0)

    private fun String.toSafeDouble(): Double {
        val parsed = replace(",", "").trim().toDoubleOrNull() ?: 0.0
        return if (parsed.isNaN() || parsed.isInfinite()) 0.0 else parsed
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null
}

data class ApiPrice(
    val ok: Boolean,
    val priceUsdt: Double?
)

data class ApiUtxo(
    val txid: String,
    val vout: Int,
    val satoshis: Long,
    val scriptPubKey: String,
    val height: Long = 0L
)

data class ApiHealth(
    val ok: Boolean,
    val status: String
)

data class ApiStatusResponse(
    val ok: Boolean,
    val status: String,
    val height: Long?
)

data class ApiAddressSummary(
    val address: String,
    val confirmedPepew: Double,
    val unconfirmedPepew: Double,
    val history: List<ApiTransaction>,
    val source: String
)

data class ApiTransaction(
    val txid: String,
    val address: String,
    val amount: Double,
    val timestampMillis: Long,
    val isSend: Boolean,
    val isPending: Boolean,
    val isSelfTransfer: Boolean = false
)

class PepewApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)
