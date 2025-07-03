package com.example.indri

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.example.indri.ui.theme.IndriTheme
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID


class MainActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>

    override fun getApplicationContext(): Context {
        return super.getApplicationContext() as LiveStreamApplication
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        enableBluetooth()
        setContent {
            IndriTheme {
                MainScreen()
            }
        }
    }

    private fun enableBluetooth(){
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            // Initialize the ActivityResultLauncher
            enableBluetoothLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    // Bluetooth was enabled
                } else {
                    // Bluetooth was not enabled
                    checkAndEnableBluetooth()
                }
            }
            checkAndEnableBluetooth()
        }
    }

    private fun checkAndEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        var overlayVisible by remember { mutableStateOf(false) }
        var enableVideo by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    startActivity(context, intent, null)
                }
            ) {
                Text("Open Bluetooth Settings")
            }
            Button(
                onClick = {
                    overlayVisible = true
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Show Devices", fontSize = 18.sp)
            }
            Button(
                onClick = {
                    context.startActivity(Intent(context, ActivityTwo::class.java))
                },
                enabled = enableVideo,
//                enabled = true,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Start Streaming", fontSize = 18.sp)
            }
        }
        if (overlayVisible) {
            DevicesOverlay(visible = overlayVisible,
                enableVideo = { enableVideo = it}) {
                overlayVisible = false
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        IndriTheme {
            MainScreen()
        }
    }

    @Composable
    fun DevicesOverlay(visible: Boolean,
                       enableVideo: (Boolean) -> Unit,
                       onDismiss: () -> Unit ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (visible) 1f else 0f))
                .clickable(onClick = onDismiss)
        ) {
            // Content of your overlay
            Column {
                CrossButton(onClick = onDismiss)
                PairedDevicesList(enableVideo)
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun PairedDevicesList(enableVideo: (Boolean) -> Unit,) {
        val context = LocalContext.current
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val deviceList = pairedDevices?.toList() ?: emptyList()
        // Display the devices in a LazyColumn
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(deviceList) { device ->
                BluetoothDeviceItem(device,
                    onClick = {
                        val hc05uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val socket = device.createRfcommSocketToServiceRecord(hc05uuid)
                                socket.connect() // Perform connection on a background thread
                                // Handle connection success
                                SocketManager.initializeSocket(socket)
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(context, "Connection successful", Toast.LENGTH_SHORT).show()
                                    enableVideo(true)
                                }
                            } catch (e: Exception) {
                                // Handle connection failure
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(context, "Connection failed. Try again", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun BluetoothDeviceItem(device: BluetoothDevice, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ){
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "${device.name ?: "Unknown"}",
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = "${device.address}",
                    color = Color.White,
                    fontSize = 14.sp,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }

    @Composable
    fun CrossButton(onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }

}





