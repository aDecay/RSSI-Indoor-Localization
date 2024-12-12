package com.conviot.rssiindoorlocalization.manager

import android.util.Log
import com.google.gson.Gson
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class LocalizationResponse(
    val isStepped: Boolean,
    val x: Float,
    val y: Float,
    val radian: Float,
    val landmark: String
)

data class UpdateResponse(
    val x: Float,
    val y: Float,
    val radian: Float
)

fun sendHttpLocalization(message: String, serverUrl: String): LocalizationResponse {
    var receivedMessage = LocalizationResponse(false, 0f, 0f, 0f, "None")

    try {
        // HTTP 연결 설정
        val url = URL("$serverUrl/locate")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "text/plain")
        connection.doOutput = true

        // CSV 데이터 전송
        val outputStream = OutputStreamWriter(connection.outputStream)
        outputStream.write(message)
        outputStream.flush()
        outputStream.close()

        // 응답 확인
        val responseCode = connection.responseCode
        Log.d("sendHttpLocalization", "Response Code: $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseStream = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("sendHttpLocalization", "Response: $responseStream")

            receivedMessage = Gson().fromJson(responseStream, LocalizationResponse::class.java)
        } else {
            Log.e("sendHttpLocalization", "Error: Response Code $responseCode")
        }

        connection.disconnect()
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("sendHttpLocalization", "Error in HTTP communication")
    }

    return receivedMessage
}

fun sendHttpUpdateState(x: Float, y: Float, WiFIOnly: Boolean, serverUrl: String): UpdateResponse {
    var receivedMessage = UpdateResponse(0f, 0f, 0f)

    try {
        // HTTP 연결 설정
        val url = URL("$serverUrl/update")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "text/plain")
        connection.doOutput = true

        // CSV 데이터 전송
        val message = "$x,$y,$WiFIOnly"
        val outputStream = OutputStreamWriter(connection.outputStream)
        outputStream.write(message)
        outputStream.flush()
        outputStream.close()

        // 응답 확인
        val responseCode = connection.responseCode
        Log.d("sendHttpUpdateState", "Response Code: $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseStream = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("sendHttpUpdateState", "Response: $responseStream")

            receivedMessage = Gson().fromJson(responseStream, UpdateResponse::class.java)
        } else {
            Log.e("sendHttpUpdateState", "Error: Response Code $responseCode")
        }

        connection.disconnect()
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("sendHttpUpdateState", "Error in HTTP communication")
    }

    return receivedMessage
}

fun sendHttpInitialState(x: Float, y: Float, ori: Float, serverUrl: String): Boolean {
    var receivedMessage = false

    try {
        // HTTP 연결 설정
        val url = URL("$serverUrl/start")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "text/plain")
        connection.doOutput = true

        // CSV 데이터 전송
        val message = "$x,$y,$ori"
        val outputStream = OutputStreamWriter(connection.outputStream)
        outputStream.write(message)
        outputStream.flush()
        outputStream.close()

        // 응답 확인
        val responseCode = connection.responseCode
        Log.d("sendHttpInitialState", "Response Code: $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseStream = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("sendHttpInitialState", "Response: $responseStream")

            receivedMessage = responseStream.toBoolean()
        } else {
            Log.e("sendHttpInitialState", "Error: Response Code $responseCode")
        }

        connection.disconnect()
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("sendHttpInitialState", "Error in HTTP communication")
    }

    return receivedMessage
}

fun sendHttpEnd(serverUrl: String): Boolean {
    var receivedMessage = false

    try {
        // HTTP 연결 설정
        val url = URL("$serverUrl/end")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "text/plain")
        connection.doOutput = true

        // 텍스트 데이터 전송
        val message = "end"
        val outputStream = OutputStreamWriter(connection.outputStream)
        outputStream.write(message)
        outputStream.flush()
        outputStream.close()

        // 응답 확인
        val responseCode = connection.responseCode
        Log.d("sendHttpEnd", "Response Code: $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseStream = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("sendHttpEnd", "Response: $responseStream")

            receivedMessage = responseStream.toBoolean()
        } else {
            Log.e("sendHttpEnd", "Error: Response Code $responseCode")
        }

        connection.disconnect()
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("sendHttpEnd", "Error in HTTP communication")
    }

    return receivedMessage
}
