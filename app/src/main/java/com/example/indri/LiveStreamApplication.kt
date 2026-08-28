package com.example.indri

import android.app.Application
import android.util.Log
import android.widget.Toast
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import com.google.firebase.Firebase
import com.google.firebase.database.database

class LiveStreamApplication : Application() {

    @Override
    override fun onCreate() {
        super.onCreate()
        initializeSDK()
    }

    private fun initializeSDK() {
        // Initialize StreamVideo. For a production app, we recommend adding the client to your Application class or DI module.
        val client = StreamVideoBuilder(
            context = this,
            user = User(
                id = "",
                name = "",
            ),
            apiKey = "", // API key
            token = "",
            runForegroundServiceForCalls = false
        ).build()
    }

}