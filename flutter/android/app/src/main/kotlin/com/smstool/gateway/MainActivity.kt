package com.smstool.gateway

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import android.util.Log

class MainActivity : FlutterActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        Log.d(TAG, "Configuring Flutter engine")

        // Set up platform channels
        val smsPlatformChannel = SmsPlatformChannel(this)
        smsPlatformChannel.setupChannels(flutterEngine)

        Log.d(TAG, "Platform channels configured")
    }
}
