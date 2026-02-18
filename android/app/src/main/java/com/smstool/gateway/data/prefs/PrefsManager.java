package com.smstool.gateway.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.UUID;

/**
 * Manages app preferences and settings stored in SharedPreferences.
 */
public class PrefsManager {
    private static final String TAG = "PrefsManager";
    private static final String PREFS_NAME = "com.smstool.gateway.prefs";

    private static final String KEY_GATEWAY_URL = "gateway_url";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_SERVICE_RUNNING = "service_running";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get or create the device ID (generated once on first launch).
     */
    public String getDeviceId() {
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            setDeviceId(deviceId);
            Log.i(TAG, "Generated new device ID: " + deviceId);
        }
        return deviceId;
    }

    private void setDeviceId(String deviceId) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
    }

    /**
     * Get the configured gateway WebSocket URL.
     */
    public String getGatewayUrl() {
        return prefs.getString(KEY_GATEWAY_URL, null);
    }

    /**
     * Set the gateway WebSocket URL.
     */
    public void setGatewayUrl(String url) {
        if (url != null && !url.isEmpty()) {
            prefs.edit().putString(KEY_GATEWAY_URL, url).apply();
            Log.i(TAG, "Gateway URL set to: " + url);
        }
    }

    /**
     * Clear the gateway URL.
     */
    public void clearGatewayUrl() {
        prefs.edit().remove(KEY_GATEWAY_URL).apply();
        Log.i(TAG, "Gateway URL cleared");
    }

    /**
     * Check if the service was previously running (for auto-restart on boot).
     */
    public boolean isServiceRunning() {
        return prefs.getBoolean(KEY_SERVICE_RUNNING, false);
    }

    /**
     * Set whether the service is running.
     */
    public void setServiceRunning(boolean running) {
        prefs.edit().putBoolean(KEY_SERVICE_RUNNING, running).apply();
        Log.i(TAG, "Service running flag set to: " + running);
    }

    /**
     * Check if all required settings are configured.
     */
    public boolean isConfigured() {
        return getGatewayUrl() != null && !getGatewayUrl().isEmpty();
    }

    /**
     * Clear all preferences (for testing/reset).
     */
    public void clear() {
        prefs.edit().clear().apply();
        // Re-generate device ID so it's not lost
        getDeviceId();
    }
}
