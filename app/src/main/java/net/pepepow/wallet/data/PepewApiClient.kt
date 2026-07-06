package net.pepepow.wallet.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val timeoutMs: Int = 15_000
) {
    suspend fun getHealth(): ApiHealth = withContext(Dispatchers.IO) {
        val body = requestHttp("/api/health")
        val json = JSONObject(body)
        ApiHealth(
            ok = json.optBoolean("ok", true),
            status = json.optString("status", "unknown")
        )
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
        parseAddressSummary(json)
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
        parseHistory(historyArray)
    }

    suspend fun getUtxos(address: String): List<ApiUtxo> = withContext(Dispatchers.IO) {
        val safeAddress = encodePathSegment(address)
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
                        item.optString("script", "")))).trim()

            result.add(ApiUtxo(txid, vout, satoshis, scriptPubKey))
        }
        result
    }

    suspend fun broadcastTransaction(hex: String): String = withContext(Dispatchers.IO) {
        val postBody = JSONObject().put("raw_tx", hex).toString()
        val body = requestHttp("/api/wallet/broadcast", method = "POST", postBody = postBody)
        
        val json = JSONObject(body)
        if (json.has("error") && !json.isNull("error")) {
            val errorObj = json.optJSONObject("error")
            val errMsg = errorObj?.optString("message") 
                ?: json.optString("error") 
                ?: json.optString("message")
            throw PepewApiException(400, errMsg)
        }
        val txid = json.optString("txid", json.optString("tx_hash", json.optString("result", json.optString("data", ""))))
        if (txid.trim().length != 64) {
            throw PepewApiException(500, "Invalid txid returned by broadcast: $txid")
        }
        txid.trim()
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
        return Math.round(valueDouble * 1e8)
    }

    private fun parseAddressSummary(json: JSONObject): ApiAddressSummary {
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

        val history = json.optJSONArray("history")?.let { parseHistory(it) } ?: emptyList()

        return ApiAddressSummary(
            address = json.optString("address"),
            confirmedPepew = confirmedPepew.toSafeDouble(),
            unconfirmedPepew = unconfirmedPepew.toSafeDouble(),
            history = history,
            source = json.optString("source", "unknown")
        )
    }

    private fun parseHistory(array: JSONArray): List<ApiTransaction> {
        val result = mutableListOf<ApiTransaction>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val txid = item.optString("txid", item.optString("tx_hash", item.optString("hash", ""))).trim()
            if (txid.isBlank()) continue

            val amount = firstDouble(
                item,
                listOf("address_delta_pepew", "delta_pepew", "balance_delta_pepew", "amount_pepew", "value_pepew", "delta_pepew", "amount", "value", "balance_delta")
            )

            if (amount == null) continue

            val timestampSeconds = item.optLongOrNull("timestamp") ?: item.optLongOrNull("time")
            val height = item.optLongOrNull("height")
            val pending = item.optBoolean("pending", false) || height == 0L || height == -1L
            val isSend = when {
                item.has("is_send") -> item.optBoolean("is_send")
                item.has("direction") -> item.optString("direction").lowercase(Locale.US).contains("send")
                amount < 0.0 -> true
                else -> false
            }
            result += ApiTransaction(
                txid = txid,
                address = item.optString("address", txid),
                amount = kotlin.math.abs(amount),
                timestampMillis = (timestampSeconds ?: (System.currentTimeMillis() / 1000L)) * 1000L,
                isSend = isSend,
                isPending = pending
            )
        }
        return result
    }

    private fun requestHttp(path: String, method: String = "GET", postBody: String? = null): String {
        val normalizedBase = baseUrl.trimEnd('/')
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val url = URL(normalizedBase + normalizedPath)
        val connection = (url.openConnection() as HttpURLConnection).apply {
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

        try {
            if (postBody != null) {
                connection.outputStream.use { os ->
                    os.write(postBody.toByteArray(Charsets.UTF_8))
                }
            }
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
            if (statusCode !in 200..299) {
                val message = parseApiError(body) ?: "HTTP $statusCode"
                throw PepewApiException(statusCode, message)
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun parseApiError(body: String): String? = try {
        val json = JSONObject(body)
        val error = json.optJSONObject("error")
        error?.optString("message")?.takeIf { it.isNotBlank() }
            ?: error?.optString("code")?.takeIf { it.isNotBlank() }
            ?: json.optString("message").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun encodePathSegment(value: String): String =
        URLEncoder.encode(value.trim(), "UTF-8").replace("+", "%20")

    private fun satoshiLikeToPepew(value: Long): String =
        String.format(Locale.US, "%.8f", value / 100_000_000.0)

    private fun String.toSafeDouble(): Double {
        val parsed = replace(",", "").trim().toDoubleOrNull() ?: 0.0
        return if (parsed.isNaN() || parsed.isInfinite()) 0.0 else parsed
    }

    private fun firstDouble(json: JSONObject, keys: List<String>): Double? {
        for (key in keys) {
            if (!json.has(key) || json.isNull(key)) continue
            val raw = json.optString(key)
            val parsed = raw.toSafeDouble()
            if (parsed != 0.0 || raw.trim() in listOf("0", "0.0", "0.00")) return parsed
        }
        return null
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null
}

data class ApiUtxo(
    val txid: String,
    val vout: Int,
    val satoshis: Long,
    val scriptPubKey: String
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
    val isPending: Boolean
)

class PepewApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)
