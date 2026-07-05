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
 * Read-only PEPEW Light API client for Phase 2.
 *
 * Security boundary:
 * - This client only performs public read requests.
 * - It does not send mnemonic, seed, private key, WIF, or signed transaction data.
 * - Broadcast is intentionally unsupported in the Android Phase 2 prototype.
 */
class PepewApiClient(
    val baseUrl: String = "https://light.pepepow.net/",
    private val timeoutMs: Int = 10_000
) {
    suspend fun getHealth(): ApiHealth = withContext(Dispatchers.IO) {
        val json = getJson("/api/health")
        ApiHealth(
            ok = json.optBoolean("ok", true),
            status = json.optString("status", "unknown")
        )
    }

    suspend fun getStatus(): ApiStatusResponse = withContext(Dispatchers.IO) {
        val json = getJson("/api/status")
        ApiStatusResponse(
            ok = !json.optBoolean("error", false),
            status = json.optString("status", json.optString("state", "unknown")),
            height = json.optLongOrNull("height") ?: json.optLongOrNull("block_height")
        )
    }

    suspend fun getAddressSummary(address: String): ApiAddressSummary = withContext(Dispatchers.IO) {
        val safeAddress = encodePathSegment(address)
        val json = getJson("/api/wallet/address/$safeAddress")
        parseAddressSummary(json)
    }

    suspend fun getHistory(address: String, limit: Int = 50, offset: Int = 0): List<ApiTransaction> = withContext(Dispatchers.IO) {
        val safeAddress = encodePathSegment(address)
        val json = getJson("/api/wallet/history/$safeAddress?limit=$limit&offset=$offset")
        val historyArray = when {
            json.has("history") -> json.optJSONArray("history") ?: JSONArray()
            json.has("transactions") -> json.optJSONArray("transactions") ?: JSONArray()
            json.has("items") -> json.optJSONArray("items") ?: JSONArray()
            else -> JSONArray()
        }
        parseHistory(historyArray)
    }

    suspend fun broadcastTransaction(hex: String): String = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException(
            "Broadcast is disabled in Phase 2. Android wallet must not broadcast until local signing is implemented."
        )
    }

    suspend fun checkHealth(): Boolean = try {
        getHealth().ok
    } catch (_: Exception) {
        false
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
                listOf("amount_pepew", "value_pepew", "delta_pepew", "amount", "value", "balance_delta")
            )

            // Some ElectrumX history entries only include txid/height. They do not contain
            // wallet delta data, so rendering them as received +NaN or fake 0.0 is misleading.
            // Skip those compact entries until the API provides a confirmed amount field.
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

    private fun getJson(path: String): JSONObject {
        val normalizedBase = baseUrl.trimEnd('/')
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val url = URL(normalizedBase + normalizedPath)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "PEPEW-Android-Wallet-Phase2")
        }

        try {
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
            if (statusCode !in 200..299) {
                val message = parseApiError(body) ?: "HTTP $statusCode"
                throw PepewApiException(statusCode, message)
            }
            return JSONObject(body)
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
        val parsed = replace(",", "").trim().toDoubleOrNull() ?: return 0.0
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
