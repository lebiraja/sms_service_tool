package com.smstool.gateway.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.smstool.gateway.data.prefs.PrefsManager;
import com.smstool.gateway.service.GatewayForegroundService;

/**
 * BroadcastReceiver for BOOT_COMPLETED and MY_PACKAGE_REPLACED intents.
 * Auto-restarts the gateway service if it was running before reboot.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received action: " + action);

        PrefsManager prefs = new PrefsManager(context);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Device rebooted
            if (prefs.isServiceRunning()) {
                Log.i(TAG, "Boot completed, restarting gateway service");
                startGatewayService(context);
            }
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            // App was updated
            if (prefs.isServiceRunning()) {
                Log.i(TAG, "App updated, restarting gateway service");
                startGatewayService(context);
            }
        }
    }

    private void startGatewayService(Context context) {
        Intent serviceIntent = new Intent(context, GatewayForegroundService.class);
        context.startForegroundService(serviceIntent);
    }
}
