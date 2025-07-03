package com.example.indri

import android.content.ContentValues
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.permission.LaunchPermissionRequest
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import android.graphics.Paint
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.database.database
import java.io.OutputStream
import java.util.Timer
import java.util.TimerTask


class ActivityTwo : ComponentActivity() {

    // Create a new PdfDocument
    private val pdfDocument = PdfDocument()
    private var pageNumber = 1
    private val paint = Paint()
    private var yPos = 60f
    private lateinit var page: PdfDocument.Page
    private val database = Firebase.database
    private val reference = database.getReference("reading")
    private val started = false

    private fun writeToPdf(what: String){
        // Get the canvas from the page
        val currentDateTime = LocalDateTime.now()
        val canvas = page.canvas
        var color: Int
        var gasSensor: String
        var distance: String

        try{
            val readings = what.split(" ")
            gasSensor = readings[0]
            distance = readings[1]
            color = if(gasSensor.toInt() < 200) 0 else 1;
        }
        catch (e: Exception){
            color = 1
            gasSensor = "ERROR"
            distance = "ERROR"
        }

        if(color == 1) paint.color = android.graphics.Color.RED
        else paint.color = android.graphics.Color.BLACK
        paint.textSize = 6f

        val message = "$currentDateTime        MQ2 Reading: $gasSensor        Distance: $distance cm"
        reference.setValue(message)
        canvas.drawText(message, 40F, yPos, paint)
        yPos += 10f
        if(yPos > 772f){
            yPos = 60f
            // Finish the page
            pdfDocument.finishPage(page)
            createNewPage()
        }
    }

    private fun createNewPage(){
        // Start a new page
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        pageNumber += 1
    }

    private fun savePdf(){
        val currentDateTime = LocalDateTime.now()
        val fileName = "Report:$currentDateTime"

        if(yPos != 60f){
            pdfDocument.finishPage(page)
        }

        // Save the document using MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Indri")
        }

        val contentResolver = this.contentResolver
        var outputStream: OutputStream? = null

        try {
            // Insert the file into MediaStore
            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                // Get the output stream
                outputStream = contentResolver.openOutputStream(uri)

                // Write the PDF data to the output stream
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream)
                }

                // Close the output stream
                outputStream?.close()
                pdfDocument.close()
                Toast.makeText(this, "PDF saved successfully at: Documents/Indri/$fileName", Toast.LENGTH_SHORT).show()
            } else {
                println("Error saving PDF: URI is null")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Ensure the output stream is closed
            try {
                outputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNewPage()

        var btMessage by mutableStateOf("No Message Received yet")
        var test = "";

        val uiHandler = Handler(Looper.getMainLooper()) { msg ->
            // Handle the message received from the BluetoothManager
            when (msg.what) {
                0 -> {
                    val numBytes = msg.arg1
                    val buffer = msg.obj as ByteArray
                    // Process the data (e.g., display in the UI or handle further)
                    val receivedData = String(buffer, 0, numBytes)
                    // Update the UI with the received data
                    test += receivedData
                    // Check if the received data contains a newline character
                    if (test.contains("\n")) {
                        // Split the test string at the newline character
                        // TODO: WORK HERE
                        val completeLine = test.substringBefore("\n")
                        btMessage = completeLine
                        writeToPdf(completeLine)
                        try {
                            val readings = completeLine.split(" ")
                            val gasSensor = readings[0]
                            val distance = readings[1]
                            btMessage = "MQ2 Reading: $gasSensor        Distance: $distance cm"
                        }
                        catch (e: Exception){
                            btMessage = "ERROR"
                        }
                        // Remove the processed line from the test variable
                        test = test.substringAfter("\n")
                    }
                }
            }
            true
        }

        val bluetoothManager = MyBluetoothManager(uiHandler)
        val connectedThread = bluetoothManager.ConnectedThread()
        connectedThread.start()

        val call = StreamVideo.instance().call("livestream", "6264442256")

        // TODO: Uncomment while testing
//        // Change the value of btMessage randomly
//        val randomMessageTimer = Timer()
//        randomMessageTimer.schedule(object : TimerTask() {
//            override fun run() {
//                val completeLine = assignRandomLine()
//                btMessage = completeLine
//                writeToPdf(completeLine)
//            }
//        }, 0, 1000)

        setContent {
            VideoTheme {
                Column {
                    Text(
                        modifier = Modifier.padding(Dp(5F)),
                        text = btMessage)
                    LiveHost(call)
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                // Launch a coroutine on the main thread
                lifecycleScope.launch {
                    call.end()
                }
                finish()
            }
        })

    }

    private fun assignRandomLine(): String {
        val lines = listOf(
            "Lorem ipsum dolor sit amet",
            "Sed do eiusmod tempor incididunt",
            "Ut enim ad minim veniam",
            "Duis aute irure dolor",
            "Excepteur sint occaecat"
        )
        return lines[lines.indices.random()]
    }

    @Composable
    fun LiveHost(call: Call) {
        LaunchPermissionRequest(
            permissions = listOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA
            )
        ) {
            AllPermissionsGranted {
                LaunchedEffect(call) {
                    call.join(create = true)
                }
                LiveHostContent(call = call)
            }
            NoneGranted {
                // Handle permission explanation
            }
        }
    }

    @Composable
    fun LiveHostContent(call: Call) {
        val connection by call.state.connection.collectAsState()
        val totalParticipants by call.state.totalParticipants.collectAsState()
        val backstage by call.state.backstage.collectAsState()
        val localParticipant by call.state.localParticipant.collectAsState()
        val video = localParticipant?.video?.collectAsState()
        val duration by call.state.duration.collectAsState()
        val scope = rememberCoroutineScope()

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(VideoTheme.colors.baseSheetPrimary)
                .padding(6.dp),
            contentColor = VideoTheme.colors.baseSheetPrimary,
            topBar = {
                // Will define the topBar content here
                if (connection == RealtimeConnection.Connected) {
                    if (!backstage) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .background(
                                        color = VideoTheme.colors.brandPrimary,
                                        shape = RoundedCornerShape(6.dp),
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                text = "Live $totalParticipants",
                                color = Color.White,
                            )

                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = "Live for $duration",
                                color = VideoTheme.colors.basePrimary,
                            )
                        }
                    } else {
                        Text(
                            text = "The livestream is not started yet",
                            color = VideoTheme.colors.basePrimary,
                        )
                    }
                } else if (connection is RealtimeConnection.Failed) {
                    Text(
                        text = "Connection failed",
                        color = VideoTheme.colors.basePrimary,
                    )
                }
            },
            bottomBar = {
                // Will define bottomBar content here
                Button(
                    colors = ButtonDefaults.buttonColors(
                        contentColor = VideoTheme.colors.brandPrimary,
                        containerColor = VideoTheme.colors.brandPrimary
                    ),
                    onClick = {
                        scope.launch {
                            if(backstage){
                                call.goLive()
                            }
                            else{
                                savePdf()
                                call.end()
                                finish()
                            }
                        }
                    },
                ) {
                    Text(
                        text = if (backstage) "Start Broadcast" else "Stop Broadcast",
                        color = Color.White,
                    )
                }
            },
        ) {
            // Main content, will be the VideoRenderer (ignore the padding warning)
            VideoRenderer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .clip(RoundedCornerShape(6.dp)),
                call = call,
                video = video?.value,
                videoFallbackContent = {
                    // Content for when the video is not available.
                },
            )
        }
    }
}