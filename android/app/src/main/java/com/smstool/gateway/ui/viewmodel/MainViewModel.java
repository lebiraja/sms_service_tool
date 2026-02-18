package com.smstool.gateway.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.smstool.gateway.data.db.EventLogEntity;
import com.smstool.gateway.data.prefs.PrefsManager;
import com.smstool.gateway.data.repository.SmsJobRepository;

import java.util.List;

/**
 * ViewModel for MainActivity.
 * Manages UI state, LiveData, and interactions with the service layer.
 */
public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";

    public enum ConnectionState {
        IDLE("Not connected"),
        CONNECTING("Connecting..."),
        CONNECTED("Connected"),
        ERROR("Connection failed");

        public final String displayText;

        ConnectionState(String displayText) {
            this.displayText = displayText;
        }
    }

    private final PrefsManager prefsManager;
    private final SmsJobRepository repository;

    private final MutableLiveData<ConnectionState> connectionState = new MutableLiveData<>(ConnectionState.IDLE);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> gatewayUrl = new MutableLiveData<>();
    private final MutableLiveData<String> deviceId = new MutableLiveData<>();
    private final LiveData<List<EventLogEntity>> eventLog;

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.prefsManager = new PrefsManager(application);
        this.repository = new SmsJobRepository(application);

        // Load initial data
        gatewayUrl.setValue(prefsManager.getGatewayUrl() != null ? prefsManager.getGatewayUrl() : "");
        deviceId.setValue(prefsManager.getDeviceId());

        // Get event log (last 50 entries)
        this.eventLog = repository.getRecentEventsLive(50);

        // Set initial state based on whether service was running
        if (prefsManager.isServiceRunning()) {
            connectionState.setValue(ConnectionState.CONNECTING);
        } else {
            connectionState.setValue(ConnectionState.IDLE);
        }
    }

    // ========================
    // Public Observable Data
    // ========================

    public LiveData<ConnectionState> getConnectionState() {
        return connectionState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getGatewayUrl() {
        return gatewayUrl;
    }

    public LiveData<String> getDeviceId() {
        return deviceId;
    }

    public LiveData<List<EventLogEntity>> getEventLog() {
        return eventLog;
    }

    // ========================
    // User Actions
    // ========================

    /**
     * User clicked Connect button.
     * Validates URL and saves it to preferences.
     */
    public void onConnectClicked(String urlInput) {
        if (urlInput == null || urlInput.trim().isEmpty()) {
            errorMessage.setValue("Please enter a server URL");
            return;
        }

        String url = urlInput.trim();

        // Validate URL format
        if (!isValidUrl(url)) {
            errorMessage.setValue("Invalid URL format. Examples: 192.168.1.100:7777 or ws://example.com:7777");
            return;
        }

        // Normalize URL
        url = normalizeUrl(url);

        Log.i(TAG, "Saving gateway URL: " + url);
        prefsManager.setGatewayUrl(url);
        gatewayUrl.setValue(url);

        // Update state to connecting
        connectionState.setValue(ConnectionState.CONNECTING);
        errorMessage.setValue(null);

        // Signal to MainActivity to start the service
        // (MainActivity will call startGatewayService())
    }

    /**
     * User clicked Disconnect button.
     */
    public void onDisconnectClicked() {
        Log.i(TAG, "User requested disconnect");
        connectionState.setValue(ConnectionState.IDLE);
        prefsManager.setServiceRunning(false);
        // Signal to MainActivity to stop the service
    }

    /**
     * Called when the service successfully connects.
     */
    public void onServiceConnected() {
        Log.i(TAG, "Service connected");
        connectionState.setValue(ConnectionState.CONNECTED);
        errorMessage.setValue(null);
    }

    /**
     * Called when the service disconnects or fails.
     */
    public void onServiceDisconnected() {
        Log.i(TAG, "Service disconnected");
        connectionState.setValue(ConnectionState.IDLE);
    }

    /**
     * Called when the service encounters an error.
     */
    public void onServiceError(String message) {
        Log.e(TAG, "Service error: " + message);
        connectionState.setValue(ConnectionState.ERROR);
        errorMessage.setValue(message);
    }

    /**
     * Called when attempting to reconnect.
     */
    public void onServiceReconnecting() {
        connectionState.setValue(ConnectionState.CONNECTING);
    }

    /**
     * Copy device ID to clipboard (called from UI).
     */
    public void copyDeviceIdToClipboard() {
        repository.logEvent("INFO", "Device ID copied to clipboard");
    }

    // ========================
    // Helper Methods
    // ========================

    /**
     * Validate URL format.
     */
    private boolean isValidUrl(String url) {
        // Allow formats like:
        // - 192.168.1.100:7777
        // - ws://192.168.1.100:7777
        // - wss://example.com:7777
        // - example.com:7777
        // - localhost:7777

        url = url.toLowerCase();

        // Remove protocol if present
        if (url.startsWith("ws://") || url.startsWith("wss://") || url.startsWith("http://") || url.startsWith("https://")) {
            url = url.substring(url.indexOf("://") + 3);
        }

        // Check basic format: should have at least one colon for port
        // or should be a valid hostname/IP
        return url.length() > 0 && !url.contains(" ");
    }

    /**
     * Normalize URL to WebSocket format.
     */
    private String normalizeUrl(String url) {
        url = url.trim();

        // Add protocol if missing
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            url = "ws://" + url;
        }

        // Add /ws path if missing
        if (!url.endsWith("/ws")) {
            if (!url.endsWith("/")) {
                url = url + "/";
            }
            url = url + "ws";
        }

        return url;
    }

    /**
     * Check if URL is valid and saved.
     */
    public boolean isConfigured() {
        String url = prefsManager.getGatewayUrl();
        return url != null && !url.isEmpty();
    }

    /**
     * Get current connection state value (non-live).
     */
    public ConnectionState getCurrentConnectionState() {
        ConnectionState state = connectionState.getValue();
        return state != null ? state : ConnectionState.IDLE;
    }

    /**
     * Check if currently trying to connect.
     */
    public boolean isConnecting() {
        return getCurrentConnectionState() == ConnectionState.CONNECTING;
    }

    /**
     * Check if currently connected.
     */
    public boolean isConnected() {
        return getCurrentConnectionState() == ConnectionState.CONNECTED;
    }
}
