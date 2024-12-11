package com.conviot.rssiindoorlocalization.manager

import android.content.Context
import android.util.Log
import androidx.datastore.dataStore
import com.conviot.rssiindoorlocalization.datastore.UserPreferences
import com.conviot.rssiindoorlocalization.userPreferencesStore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class LocalizationResponse(
    val isStepped: Boolean,
    val x: Float,
    val y: Float,
    val radian: Float
)

const val headerDelimiter = "/"
const val headerSizeMax = 20

fun sendUdpLocalization(message: String, ipAddress: String, port: Int): LocalizationResponse {
    var receivedMessage: LocalizationResponse = LocalizationResponse(false, 0f, 0f, 0f)
    val identifier = "locate"

    try {
        // Send
        val socket = DatagramSocket()
        val sendData = message.toByteArray()
        val maxChunkSize = 1024 - headerSizeMax// Adjust based on your network MTU
        val chunks = sendData.toList().chunked(maxChunkSize)

        for (i in chunks.indices) {
            var chunk = chunks[i]
            val header = StringBuilder()
                .append(i + 1).append(headerDelimiter)
                .append(chunks.size).append(headerDelimiter)
                .append(identifier).append(headerDelimiter)
            chunk = header.toString().toByteArray().toList() + chunk.toByteArray().toList()
            val sendPacket = DatagramPacket(chunk.toByteArray(), chunk.size, InetAddress.getByName(ipAddress), port)
            socket.send(sendPacket)
        }

        Log.d("sendUdpLocalization", "Packet sent to: $ipAddress:$port")

        // Receive
        val receiveData = ByteArray(1024)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)
        socket.receive(receivePacket)

        val gson = Gson()
        val receivedString = String(receivePacket.data, 0, receivePacket.length)
        Log.d("sendUdpLocalization", "ReceivedPacket: ${receivedString}")
        receivedMessage = gson.fromJson(receivedString, LocalizationResponse::class.java)

        Log.d("sendUdpLocalization", "ReceivedMessage: $receivedMessage")
        socket.close()
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("sendUdpLocalization", "Error in UDP communication")
    }
    return receivedMessage
}

fun sendUdpInitialState(message: String, ipAddress: String, port: Int): Boolean {
    var receivedMessage: Boolean = false
    val identifier = "start"

    try {
        // Send
        val socket = DatagramSocket()
        val sendData = message.toByteArray()
        val maxChunkSize = 1024 - headerSizeMax// Adjust based on your network MTU
        val chunks = sendData.toList().chunked(maxChunkSize)

        for (i in chunks.indices) {
            var chunk = chunks[i]
            val header = StringBuilder()
                .append(i + 1).append(headerDelimiter)
                .append(chunks.size).append(headerDelimiter)
                .append(identifier).append(headerDelimiter)
            chunk = header.toString().toByteArray().toList() + chunk.toByteArray().toList()
            val sendPacket = DatagramPacket(chunk.toByteArray(), chunk.size, InetAddress.getByName(ipAddress), port)
            socket.send(sendPacket)
        }

        Log.d("sendUdpInitialState", "Packet sent to: $ipAddress:$port")

        // Receive
        val receiveData = ByteArray(1024)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)
        socket.receive(receivePacket)

        val gson = Gson()
        val receivedString = String(receivePacket.data, 0, receivePacket.length)
        Log.d("sendUdpInitialState", "ReceivedPacket: ${receivedString}")
        receivedMessage = receivedString == "true"

        Log.d("sendUdpInitialState", "ReceivedMessage: $receivedMessage")
        socket.close()
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("sendUdpInitialState", "Error in UDP communication")
    }
    return receivedMessage
}
