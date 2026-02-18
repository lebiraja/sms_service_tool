package com.smstool.gateway

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class SmsPlatformChannel(private val context: Context) {
    companion object {
        const val METHOD_CHANNEL = "com.smstool.gateway/sms"
        const val EVENT_CHANNEL = "com.smstool.gateway/smsEvents"
        const val TAG = "SmsPlatformChannel"

        private var eventSink: EventChannel.EventSink? = null
    }

    private val smsBroadcastReceivers = mutableMapOf<String, BroadcastReceiver>()

    fun setupChannels(flutterEngine: FlutterEngine) {
        // Method channel for sending SMS
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "sendSms" -> {
                        val jobId = call.argument<String>("jobId") ?: return@setMethodCallHandler
                        val to = call.argument<String>("to") ?: return@setMethodCallHandler
                        val body = call.argument<String>("body") ?: return@setMethodCallHandler
                        val maxRetries = call.argument<Int>("maxRetries") ?: 3

                        try {
                            sendSms(jobId, to, body, maxRetries)
                            result.success(null)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sending SMS: ${e.message}")
                            result.error("SEND_ERROR", e.message, null)
                        }
                    }

                    else -> result.notImplemented()
                }
            }

        // Event channel for delivery reports
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    eventSink = events
                    Log.d(TAG, "Event channel listener attached")
                }

                override fun onCancel(arguments: Any?) {
                    eventSink = null
                    Log.d(TAG, "Event channel listener detached")
                }
            })
    }

    private fun sendSms(jobId: String, phoneNumber: String, messageBody: String, maxRetries: Int) {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        val parts = smsManager.divideMessage(messageBody) as ArrayList<String>
        val sentIntents = arrayListOf<PendingIntent>()
        val deliveryIntents = arrayListOf<PendingIntent>()

        for ((index, part) in parts.withIndex()) {
            // Create sent intent
            val sentIntent = PendingIntent.getBroadcast(
                context,
                jobId.hashCode() + index,
                Intent(SmsSentReceiver.ACTION).apply {
                    putExtra("jobId", jobId)
                    putExtra("part", index)
                    putExtra("maxRetries", maxRetries)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create delivery intent
            val deliveryIntent = PendingIntent.getBroadcast(
                context,
                (jobId.hashCode() + 10000) + index,
                Intent(SmsDeliveredReceiver.ACTION).apply {
                    putExtra("jobId", jobId)
                    putExtra("part", index)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            sentIntents.add(sentIntent)
            deliveryIntents.add(deliveryIntent)
        }

        try {
            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                parts,
                sentIntents,
                deliveryIntents
            )
            Log.d(TAG, "SMS sent: jobId=$jobId to=$phoneNumber parts=${parts.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}")
            throw e
        }
    }

    fun notifySmsResult(jobId: String, status: String, errorCode: Int?, errorMessage: String?) {
        val data = mapOf(
            "jobId" to jobId,
            "status" to status,
            "errorCode" to errorCode,
            "errorMessage" to errorMessage
        )
        eventSink?.success(data)
        Log.d(TAG, "Notified SMS result: $data")
    }
}
