package com.conviot.rssiindoorlocalization.manager

import android.util.Log
import com.google.gson.Gson
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

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

const val headerDelimiter = "/"
const val headerSizeMax = 20
const val payloadSplitter = ','

/**
 * Dead Reckoning
 */
fun sendUdpLocalization(message: String, ipAddress: String, port: Int): LocalizationResponse {
    var receivedMessage: LocalizationResponse = LocalizationResponse(false, 0f, 0f, 0f, "None")
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

fun sendUdpUpdateState(x: Float, y: Float, WiFIOnly: Boolean, ipAddress: String, port: Int): UpdateResponse {
    var receivedMessage: UpdateResponse = UpdateResponse(0f, 0f, 0f)
    val identifier = "update"

    try {
        // Send
        val socket = DatagramSocket()
        val sendData = StringBuilder().append(x, payloadSplitter, y, payloadSplitter, WiFIOnly).toString().toByteArray()
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

        Log.d("sendUdpUpdateState", "Packet sent to: $ipAddress:$port")

        // Receive
        val receiveData = ByteArray(1024)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)
        socket.receive(receivePacket)

        val gson = Gson()
        val receivedString = String(receivePacket.data, 0, receivePacket.length)
        Log.d("sendUdpUpdateState", "ReceivedPacket: ${receivedString}")
        receivedMessage = gson.fromJson(receivedString, UpdateResponse::class.java)

        Log.d("sendUdpUpdateState", "ReceivedMessage: $receivedMessage")
        socket.close()
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("sendUdpUpdateState", "Error in UDP communication")
    }

    return receivedMessage
}

fun sendUdpInitialState(x: Float, y: Float, ori: Float, ipAddress: String, port: Int): Boolean {
    var receivedMessage: Boolean = false
    val identifier = "start"

    try {
        // Send
        val socket = DatagramSocket()
        val sendData = StringBuilder().append(x, payloadSplitter, y, payloadSplitter, ori).toString().toByteArray()
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

fun sendUdpEnd(ipAddress: String, port: Int): Boolean {
    var receivedMessage: Boolean = false
    val identifier = "end"

    try {
        // Send
        val socket = DatagramSocket()
        val sendData = identifier.toByteArray()
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

        Log.d("sendUdpEnd", "Packet sent to: $ipAddress:$port")

        // Receive
        val receiveData = ByteArray(1024)
        val receivePacket = DatagramPacket(receiveData, receiveData.size)
        socket.receive(receivePacket)

        val receivedString = String(receivePacket.data, 0, receivePacket.length)
        Log.d("sendUdpEnd", "ReceivedPacket: ${receivedString}")
        receivedMessage = receivedString == "true"

        Log.d("sendUdpEnd", "ReceivedMessage: $receivedMessage")
        socket.close()
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("sendUdpEnd", "Error in UDP communication")
    }
    return receivedMessage
}
