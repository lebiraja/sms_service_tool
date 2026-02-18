package com.smstool.gateway

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log

class SmsSentReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION = "com.smstool.gateway.SMS_SENT"
        const val TAG = "SmsSentReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val jobId = intent.getStringExtra("jobId") ?: return
        val resultCode = resultCode

        Log.d(TAG, "SMS sent result: jobId=$jobId resultCode=$resultCode")

        val platformChannel = SmsPlatformChannel(context)

        when (resultCode) {
            Activity.RESULT_OK -> {
                platformChannel.notifySmsResult(jobId, "sent", null, null)
            }

            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                platformChannel.notifySmsResult(
                    jobId,
                    "failed_retrying",
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE,
                    "Generic failure"
                )
            }

            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                platformChannel.notifySmsResult(
                    jobId,
                    "failed_retrying",
                    SmsManager.RESULT_ERROR_RADIO_OFF,
                    "Radio off"
                )
            }

            SmsManager.RESULT_ERROR_NULL_PDU -> {
                platformChannel.notifySmsResult(
                    jobId,
                    "failed_permanent",
                    SmsManager.RESULT_ERROR_NULL_PDU,
                    "Invalid message"
                )
            }

            SmsManager.RESULT_ERROR_NO_SERVICE -> {
                platformChannel.notifySmsResult(
                    jobId,
                    "failed_retrying",
                    SmsManager.RESULT_ERROR_NO_SERVICE,
                    "No service"
                )
            }

            else -> {
                platformChannel.notifySmsResult(
                    jobId,
                    "failed_retrying",
                    resultCode,
                    "Unknown error"
                )
            }
        }
    }
}
