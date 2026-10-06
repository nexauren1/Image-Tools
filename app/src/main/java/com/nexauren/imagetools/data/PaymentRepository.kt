package com.nexauren.imagetools.data

import com.nexauren.imagetools.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object PaymentRepository {
    suspend fun createOrder(token: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open("/paypal/create-order", token)
            connection.outputStream.bufferedWriter().use {
                it.write("{\"plan\":\"premium\",\"amount\":\"9.00\"}")
            }
            val body = read(connection)
            val data = JSONObject(body)
            require(data.optBoolean("ok")) { data.optString("error", "Unable to create order") }
            Pair(data.getString("orderId"), data.getString("approveUrl"))
        }
    }

    suspend fun captureOrder(token: String, orderId: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val connection = open("/paypal/capture-order", token)
            connection.outputStream.bufferedWriter().use {
                it.write(JSONObject().put("orderId", orderId).toString())
            }
            JSONObject(read(connection)).optBoolean("ok")
        }.getOrDefault(false)
    }

    private fun open(path: String, token: String): HttpURLConnection {
        require(!BuildConfig.WORKER_URL.contains("YOUR-IMAGE-TOOLS-WORKER"))
        val connection = URL(BuildConfig.WORKER_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.setRequestProperty("Authorization", "Bearer " + token)
        connection.setRequestProperty("Content-Type", "application/json")
        return connection
    }

    private fun read(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader()?.use { it.readText() } ?: "{}"
    }
}