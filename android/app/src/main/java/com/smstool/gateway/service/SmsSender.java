package com.smstool.gateway.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import com.smstool.gateway.data.db.SmsJobEntity;
import com.smstool.gateway.data.model.SmsJobStatus;
import com.smstool.gateway.data.repository.SmsJobRepository;
import com.smstool.gateway.receiver.SmsSentReceiver;

import java.util.ArrayList;

/**
 * Handles SMS sending via Android's SmsManager.
 * Manages multi-part messages, pending intents, and result callbacks.
 */
public class SmsSender {
    private static final String TAG = "SmsSender";
    private static final String ACTION_SMS_SENT = "com.smstool.gateway.action.SMS_SENT";

    private final Context context;
    private final SmsJobRepository repository;
    private final SmsManager smsManager;

    public SmsSender(Context context, SmsJobRepository repository) {
        this.context = context;
        this.repository = repository;
        this.smsManager = SmsManager.getDefault();
    }

    /**
     * Send an SMS for a job.
     * Handles multi-part messages if body > 160 chars.
     */
    public void sendSms(SmsJobEntity job, SmsSenderCallback callback) {
        try {
            // Check if message needs to be split into multiple parts
            ArrayList<String> parts = smsManager.divideMessage(job.body);

            if (parts.size() == 1) {
                sendSinglePartSms(job, callback);
            } else {
                sendMultiPartSms(job, parts, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error preparing SMS", e);
            if (callback != null) {
                callback.onSmsSendFailed(job, "Error preparing SMS: " + e.getMessage());
            }
        }
    }

    /**
     * Send a single-part SMS.
     */
    private void sendSinglePartSms(SmsJobEntity job, SmsSenderCallback callback) {
        try {
            PendingIntent sentIntent = createSentIntent(job.jobId);
            PendingIntent deliveryIntent = createDeliveryIntent(job.jobId);

            smsManager.sendTextMessage(
                    job.toNumber,
                    null,  // use default sender ID
                    job.body,
                    sentIntent,
                    deliveryIntent
            );

            Log.i(TAG, "SMS sent for job: " + job.jobId + " to " + job.toNumber);
            if (callback != null) {
                callback.onSmsSendStarted(job);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending single-part SMS", e);
            if (callback != null) {
                callback.onSmsSendFailed(job, "Error sending SMS: " + e.getMessage());
            }
        }
    }

    /**
     * Send a multi-part SMS.
     */
    private void sendMultiPartSms(SmsJobEntity job, ArrayList<String> parts,
                                   SmsSenderCallback callback) {
        try {
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            ArrayList<PendingIntent> deliveryIntents = new ArrayList<>();

            for (String part : parts) {
                sentIntents.add(createSentIntent(job.jobId));
                deliveryIntents.add(createDeliveryIntent(job.jobId));
            }

            smsManager.sendMultipartTextMessage(
                    job.toNumber,
                    null,
                    parts,
                    sentIntents,
                    deliveryIntents
            );

            Log.i(TAG, "Multi-part SMS sent for job: " + job.jobId + " (" + parts.size() + " parts)");
            if (callback != null) {
                callback.onSmsSendStarted(job);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending multi-part SMS", e);
            if (callback != null) {
                callback.onSmsSendFailed(job, "Error sending SMS: " + e.getMessage());
            }
        }
    }

    /**
     * Create a PendingIntent for SMS_SENT broadcast.
     */
    private PendingIntent createSentIntent(String jobId) {
        Intent intent = new Intent(ACTION_SMS_SENT);
        intent.putExtra("job_id", jobId);
        return PendingIntent.getBroadcast(
                context,
                jobId.hashCode(),  // use job ID as request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Create a PendingIntent for SMS delivery report.
     */
    private PendingIntent createDeliveryIntent(String jobId) {
        Intent intent = new Intent(SmsSentReceiver.ACTION_SMS_DELIVERED);
        intent.putExtra("job_id", jobId);
        return PendingIntent.getBroadcast(
                context,
                (jobId + "_delivery").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    // Callback interface for SMS sending events
    public interface SmsSenderCallback {
        void onSmsSendStarted(SmsJobEntity job);
        void onSmsSendFailed(SmsJobEntity job, String errorMessage);
    }
}
