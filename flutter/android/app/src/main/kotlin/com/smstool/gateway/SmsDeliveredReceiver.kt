package com.smstool.gateway

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SmsDeliveredReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION = "com.smstool.gateway.SMS_DELIVERED"
        const val TAG = "SmsDeliveredReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val jobId = intent.getStringExtra("jobId") ?: return
        val resultCode = resultCode

        Log.d(TAG, "SMS delivered result: jobId=$jobId resultCode=$resultCode")

        val platformChannel = SmsPlatformChannel(context)

        when (resultCode) {
            Activity.RESULT_OK -> {
                platformChannel.notifySmsResult(jobId, "delivered", null, null)
            }

            else -> {
                Log.w(TAG, "Delivery failed: jobId=$jobId code=$resultCode")
                // Some carriers don't send delivery reports
                // We don't mark as failed, just log it
            }
        }
    }
}
