package com.smstool.gateway.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import com.smstool.gateway.data.db.SmsJobEntity;
import com.smstool.gateway.data.model.SmsJobStatus;
import com.smstool.gateway.data.repository.SmsJobRepository;

/**
 * BroadcastReceiver for SMS_SENT callbacks from SmsManager.
 * Called when the SMS message is sent (accepted by the network).
 */
public class SmsSentReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsSentReceiver";
    public static final String ACTION_SMS_DELIVERED = "com.smstool.gateway.action.SMS_DELIVERED";

    private SmsJobRepository repository;

    public SmsSentReceiver() {
    }

    public SmsSentReceiver(SmsJobRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (repository == null) {
            repository = new SmsJobRepository(context);
        }

        String jobId = intent.getStringExtra("job_id");
        if (jobId == null) {
            Log.w(TAG, "Received SMS_SENT intent without job_id");
            return;
        }

        // Get the result code from SmsManager
        int resultCode = getResultCode();

        if (resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "SMS sent successfully for job: " + jobId);
            repository.updateJobStatus(
                    jobId,
                    SmsJobStatus.SENT,
                    null,
                    null,
                    () -> {
                        repository.logEvent("INFO", "✓ SMS sent to recipient (waiting for delivery)");
                    }
            );
        } else {
            // Handle error
            String errorMessage = getErrorMessage(resultCode);
            Log.e(TAG, "SMS send failed for job " + jobId + ": " + errorMessage);

            // Get the job to check retry count
            repository.getJob(jobId, job -> {
                if (job != null) {
                    if (job.attempts < job.maxRetries) {
                        // Schedule retry
                        long backoffDelay = calculateBackoff(job.attempts);
                        repository.scheduleRetry(jobId, job.attempts + 1, backoffDelay, () -> {
                            repository.logEvent("WARN",
                                    "✗ Send failed, retry " + (job.attempts + 1) + "/" + job.maxRetries);
                        });
                    } else {
                        // All retries exhausted
                        repository.updateJobStatus(
                                jobId,
                                SmsJobStatus.FAILED_PERMANENT,
                                resultCode,
                                errorMessage,
                                () -> {
                                    repository.logEvent("ERROR",
                                            "✗ Send failed (final) - " + errorMessage);
                                }
                        );
                    }
                }
            });
        }
    }

    /**
     * Calculate exponential backoff delay in milliseconds.
     * Formula: min(5 * 2^attempt + jitter, 300) seconds
     */
    private long calculateBackoff(int attempt) {
        long baseDelay = 5000;  // 5 seconds
        long maxDelay = 300000;  // 5 minutes
        long delay = baseDelay * (long) Math.pow(2, attempt);
        long jitter = (long) (Math.random() * 5000);  // Random 0-5 seconds
        return Math.min(delay + jitter, maxDelay);
    }

    /**
     * Get a human-readable error message for an SMS send result code.
     */
    private String getErrorMessage(int resultCode) {
        switch (resultCode) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                return "Generic failure";
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                return "Radio is off";
            case SmsManager.RESULT_ERROR_NULL_PDU:
                return "Null PDU";
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                return "No service";
            default:
                return "Unknown error (" + resultCode + ")";
        }
    }
}
