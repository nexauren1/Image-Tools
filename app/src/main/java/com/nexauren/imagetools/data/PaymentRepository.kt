package com.nexauren.imagetools.data

import com.nexauren.imagetools.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SubscriptionStart(
    val subscriptionId: String,
    val approveUrl: String
)

object PaymentRepository {
    suspend fun createSubscription(token: String): Result<SubscriptionStart> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open("/paypal/create-subscription", token, "POST")
            connection.outputStream.bufferedWriter().use {
                it.write("{}")
            }
            val data = JSONObject(read(connection))
            require(data.optBoolean("ok")) {
                data.optString("error", "Unable to start subscription")
            }
            SubscriptionStart(
                subscriptionId = data.getString("subscriptionId"),
                approveUrl = data.getString("approveUrl")
            )
        }
    }

    suspend fun refreshSubscription(token: String, subscriptionId: String? = null): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = if (subscriptionId.isNullOrBlank()) {
                    "/paypal/subscription-status"
                } else {
                    "/paypal/subscription-status?subscriptionId=" +
                        java.net.URLEncoder.encode(subscriptionId, "UTF-8")
                }
                val connection = open(path, token, "GET")
                val data = JSONObject(read(connection))
                require(data.optBoolean("ok")) {
                    data.optString("error", "Unable to verify subscription")
                }
                data.optBoolean("premium")
            }
        }

    suspend fun cancelSubscription(token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open("/paypal/cancel-subscription", token, "POST")
            connection.outputStream.bufferedWriter().use {
                it.write("{}")
            }
            JSONObject(read(connection)).optBoolean("ok")
        }
    }

    private fun open(path: String, token: String, method: String): HttpURLConnection {
        require(!BuildConfig.WORKER_URL.contains("YOUR-IMAGE-TOOLS-WORKER"))
        return (URL(BuildConfig.WORKER_URL.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            doOutput = method == "POST"
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Authorization", "Bearer " + token)
            setRequestProperty("Content-Type", "application/json")
        }
    }

    private fun read(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader()?.use { it.readText() } ?: "{}"
    }
}
