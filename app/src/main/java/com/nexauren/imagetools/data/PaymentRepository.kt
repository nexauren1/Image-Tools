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

class PaymentException(
    val code: String,
    val stage: String,
    val httpStatus: Int,
    message: String
) : Exception(message)

object PaymentRepository {
    suspend fun createSubscription(token: String): Result<SubscriptionStart> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open("/paypal/create-subscription", token, "POST")
            connection.outputStream.bufferedWriter().use {
                it.write("{}")
            }
            val body = read(connection)
            val data = JSONObject(body)
            requirePaymentOk(connection, data, "subscription-create")
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
                requirePaymentOk(connection, data, "subscription-status")
                data.optBoolean("premium")
            }
        }

    suspend fun cancelSubscription(token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open("/paypal/cancel-subscription", token, "POST")
            connection.outputStream.bufferedWriter().use {
                it.write("{}")
            }
            val data = JSONObject(read(connection))
            requirePaymentOk(connection, data, "subscription-cancel")
            true
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

    private fun requirePaymentOk(connection: HttpURLConnection, data: JSONObject, fallbackStage: String) {
        if (data.optBoolean("ok")) return
        val code = data.optString("code").ifBlank { data.optString("error", "PAYMENT_ERROR") }
        val stage = data.optString("stage").ifBlank { fallbackStage }
        val message = data.optString("error").ifBlank { "Unable to complete payment." }
        throw PaymentException(code, stage, connection.responseCode, message)
    }

    private fun read(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader()?.use { it.readText() } ?: "{}"
    }
}
