package com.smstool.gateway.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.smstool.gateway.data.model.SmsJobStatus;
import com.smstool.gateway.data.repository.SmsJobRepository;

/**
 * BroadcastReceiver for SMS delivery reports.
 * Called when the SMS is delivered to the recipient device.
 * Note: Not all carriers provide delivery reports.
 */
public class SmsDeliveredReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsDeliveredReceiver";
    public static final String ACTION_SMS_DELIVERED = "com.smstool.gateway.action.SMS_DELIVERED";

    private SmsJobRepository repository;

    public SmsDeliveredReceiver() {
    }

    public SmsDeliveredReceiver(SmsJobRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (repository == null) {
            repository = new SmsJobRepository(context);
        }

        String jobId = intent.getStringExtra("job_id");
        if (jobId == null) {
            Log.w(TAG, "Received SMS_DELIVERED intent without job_id");
            return;
        }

        int resultCode = getResultCode();

        if (resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "SMS delivered for job: " + jobId);
            repository.updateJobStatus(
                    jobId,
                    SmsJobStatus.DELIVERED,
                    null,
                    null,
                    () -> {
                        repository.logEvent("INFO", "✓ SMS delivered to recipient");
                    }
            );
        } else {
            Log.w(TAG, "SMS delivery report failed for job: " + jobId + ", code: " + resultCode);
            // Still keep status as SENT (delivery report failed, but SMS was likely sent)
            repository.logEvent("WARN", "SMS sent but delivery report failed");
        }
    }
}
