package com.example.indri

import android.bluetooth.BluetoothSocket
import android.os.Handler
import java.io.IOException
import java.io.InputStream

object SocketManager {
    private var socket: BluetoothSocket? = null

    fun initializeSocket(bluetoothSocket: BluetoothSocket) {
        socket = bluetoothSocket
    }

    fun getSocket(): BluetoothSocket? {
        return socket
    }
}



class MyBluetoothManager(
    private val handler: Handler
) {
    inner class ConnectedThread() : Thread() {
        override fun run() {
            val mmSocket = SocketManager.getSocket()
            val mmInStream: InputStream? = mmSocket?.inputStream
            val mmBuffer: ByteArray = ByteArray(1024)
            var numBytes: Int // bytes returned from read()

            if (mmInStream == null){
                interrupt()
            }
            else{
                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    // Read from the InputStream.
                    numBytes = try {
                        mmInStream.read(mmBuffer)
                    } catch (e: IOException) {
                        break
                    }

                    // Send the obtained bytes to the UI activity.
                    val readMsg = handler.obtainMessage(
                        0, numBytes, -1,
                        mmBuffer
                    )
                    readMsg.sendToTarget()
                }
            }
        }
    }
}