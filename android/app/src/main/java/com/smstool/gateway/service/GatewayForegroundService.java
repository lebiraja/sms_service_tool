package com.smstool.gateway.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smstool.gateway.data.db.SmsJobEntity;
import com.smstool.gateway.data.model.SmsJobStatus;
import com.smstool.gateway.data.prefs.PrefsManager;
import com.smstool.gateway.data.repository.SmsJobRepository;
import com.smstool.gateway.network.MessageParser;
import com.smstool.gateway.network.WebSocketManager;
import com.smstool.gateway.receiver.SmsDeliveredReceiver;
import com.smstool.gateway.receiver.SmsSentReceiver;
import com.smstool.gateway.util.NotificationHelper;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Foreground Service that maintains the WebSocket connection to the backend.
 * Core responsibilities:
 * - Establish and maintain WebSocket connection
 * - Handle reconnection with exponential backoff
 * - Dispatch jobs to backend for SMS sending
 * - Handle incoming messages (sms_job, ping, etc.)
 * - Manage SMS sending via SmsSender
 * - Keep persistent notification updated
 */
public class GatewayForegroundService extends Service {
    private static final String TAG = "GatewayForegroundService";
    private static final int NOTIFICATION_ID = 1001;

    private PrefsManager prefsManager;
    private SmsJobRepository repository;
    private WebSocketManager webSocketManager;
    private SmsSender smsSender;
    private Handler mainHandler;

    private ServiceBinder binder = new ServiceBinder();
    private int reconnectAttempt = 0;
    private static final long INITIAL_RECONNECT_DELAY = 5000;  // 5 seconds
    private static final long MAX_RECONNECT_DELAY = 60000;    // 60 seconds

    private ServiceStateListener stateListener;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");

        prefsManager = new PrefsManager(this);
        repository = new SmsJobRepository(this);
        webSocketManager = new WebSocketManager();
        smsSender = new SmsSender(this, repository);
        mainHandler = new Handler(Looper.getMainLooper());

        // Register broadcast receivers dynamically
        registerBroadcastReceivers();

        // Start as foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startForeground(NOTIFICATION_ID, NotificationHelper.createForegroundNotification(
                    this, "Connecting..."), Service.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            startForeground(NOTIFICATION_ID, NotificationHelper.createForegroundNotification(
                    this, "Connecting..."));
        }

        prefsManager.setServiceRunning(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");

        // Attempt to connect
        connectToBackend();

        // Resume any pending jobs that didn't complete before the service stopped
        resumePendingJobs();

        return START_STICKY;  // Restart if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed");
        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
        prefsManager.setServiceRunning(false);
        unregisterBroadcastReceivers();
        super.onDestroy();
    }

    /**
     * Register broadcast receivers for SMS sent/delivered.
     */
    private void registerBroadcastReceivers() {
        // Receivers will be dynamically registered by SmsSender
        Log.d(TAG, "Broadcast receivers will be registered by SmsSender");
    }

    private void unregisterBroadcastReceivers() {
        Log.d(TAG, "Unregistering broadcast receivers");
    }

    /**
     * Connect to the backend WebSocket.
     */
    private void connectToBackend() {
        String url = prefsManager.getGatewayUrl();
        if (url == null || url.isEmpty()) {
            Log.w(TAG, "No gateway URL configured");
            updateNotification("Not configured");
            return;
        }

        // Normalize URL if needed
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            url = "ws://" + url;
        }
        if (!url.endsWith("/ws")) {
            url = url + "/ws";
        }

        Log.i(TAG, "Connecting to " + url);
        updateNotification("Connecting...");

        // Set up WebSocket listener
        webSocketManager.setListener(new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.i(TAG, "WebSocket connected");
                reconnectAttempt = 0;
                updateNotification("Connected");
                repository.logEvent("INFO", "✓ Connected to server");

                // Send device info
                String deviceInfo = MessageParser.createDeviceInfoMessage(
                        prefsManager.getDeviceId(),
                        android.os.Build.MODEL,
                        String.valueOf(android.os.Build.VERSION.SDK_INT),
                        "1.0.0"
                );
                webSocketManager.sendMessage(deviceInfo);

                // Flush any pending status reports
                flushPendingReports();

                if (stateListener != null) {
                    stateListener.onConnected();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.i(TAG, "WebSocket closing: " + code);
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.i(TAG, "WebSocket closed: " + code + " " + reason);
                updateNotification("Disconnected");
                repository.logEvent("WARN", "✗ Disconnected from server");
                scheduleReconnect();

                if (stateListener != null) {
                    stateListener.onDisconnected();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e(TAG, "WebSocket failure", t);
                updateNotification("Connection failed");
                repository.logEvent("ERROR", "✗ Connection failed: " + t.getMessage());
                scheduleReconnect();

                if (stateListener != null) {
                    stateListener.onError(t.getMessage());
                }
            }
        });

        webSocketManager.connect(url);
    }

    /**
     * Handle incoming WebSocket message from server.
     */
    private void handleMessage(String jsonText) {
        try {
            String messageType = MessageParser.getMessageType(jsonText);
            Log.d(TAG, "Received message type: " + messageType);

            if ("sms_job".equals(messageType)) {
                handleSmsJob(jsonText);
            } else if ("ping".equals(messageType)) {
                handlePing(jsonText);
            } else if ("error".equals(messageType)) {
                handleError(jsonText);
            } else {
                Log.w(TAG, "Unknown message type: " + messageType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling message", e);
            repository.logEvent("ERROR", "✗ Message parsing error");
        }
    }

    /**
     * Handle an sms_job message from the server.
     */
    private void handleSmsJob(String jsonText) throws Exception {
        JsonObject json = MessageParser.parseMessage(jsonText);
        String jobId = MessageParser.getStringField(json, "job_id", null);
        String to = MessageParser.getStringField(json, "to", null);
        String body = MessageParser.getStringField(json, "body", null);
        Integer maxRetries = MessageParser.getIntField(json, "max_retries");

        if (jobId == null || to == null || body == null) {
            throw new Exception("Missing required fields");
        }

        Log.i(TAG, "Received SMS job: " + jobId);
        repository.logEvent("INFO", "Received job to " + to);

        // Create job in local queue
        repository.createJob(to, body, maxRetries != null ? maxRetries : 3, () -> {
            // Job created, now send it
            repository.getJob(jobId, job -> {
                if (job != null) {
                    sendSmsJob(job);
                }
            });
        });
    }

    /**
     * Send an SMS job using SmsManager.
     */
    private void sendSmsJob(SmsJobEntity job) {
        smsSender.sendSms(job, new SmsSender.SmsSenderCallback() {
            @Override
            public void onSmsSendStarted(SmsJobEntity job) {
                // Update status to SENDING
                repository.updateJobStatus(job.jobId, SmsJobStatus.SENDING, null, null, null);
                // Send status update to server
                String statusMsg = MessageParser.createStatusUpdateMessage(
                        job.jobId,
                        SmsJobStatus.SENDING.getValue(),
                        0
                );
                webSocketManager.sendMessage(statusMsg);
            }

            @Override
            public void onSmsSendFailed(SmsJobEntity job, String errorMessage) {
                // Update status and mark as pending report (will be sent when reconnected)
                repository.updateJobStatus(job.jobId, SmsJobStatus.FAILED_PERMANENT, null,
                        errorMessage, null);
                repository.setPendingReport(job.jobId, true, null);
            }
        });
    }

    /**
     * Handle a ping message from the server.
     */
    private void handlePing(String jsonText) throws Exception {
        JsonObject json = MessageParser.parseMessage(jsonText);
        String messageId = MessageParser.getStringField(json, "message_id", null);

        if (messageId != null) {
            String pongMsg = MessageParser.createPongMessage(messageId);
            webSocketManager.sendMessage(pongMsg);
            Log.d(TAG, "Sent pong response");
        }
    }

    /**
     * Handle an error message from the server.
     */
    private void handleError(String jsonText) throws Exception {
        JsonObject json = MessageParser.parseMessage(jsonText);
        String code = MessageParser.getStringField(json, "code", "UNKNOWN");
        String detail = MessageParser.getStringField(json, "detail", "");

        Log.w(TAG, "Server error: " + code + " - " + detail);
        repository.logEvent("ERROR", "✗ Server error: " + code);
    }

    /**
     * Flush pending status reports to the server.
     */
    private void flushPendingReports() {
        repository.getJobsWithPendingReports(jobs -> {
            for (SmsJobEntity job : jobs) {
                String statusMsg = MessageParser.createStatusUpdateMessage(
                        job.jobId,
                        job.status,
                        job.attempts
                );
                webSocketManager.sendMessage(statusMsg);
            }
            if (!jobs.isEmpty()) {
                Log.i(TAG, "Flushed " + jobs.size() + " pending reports");
            }
        });
    }

    /**
     * Resume jobs that were pending when the service stopped.
     */
    private void resumePendingJobs() {
        repository.getJobsReadyForRetry(jobs -> {
            Log.i(TAG, "Resuming " + jobs.size() + " pending jobs");
            for (SmsJobEntity job : jobs) {
                sendSmsJob(job);
            }
        });
    }

    /**
     * Schedule a reconnect attempt with exponential backoff.
     */
    private void scheduleReconnect() {
        reconnectAttempt++;
        long delay = Math.min(
                INITIAL_RECONNECT_DELAY * (long) Math.pow(2, reconnectAttempt - 1),
                MAX_RECONNECT_DELAY
        );

        Log.i(TAG, "Scheduling reconnect (attempt " + reconnectAttempt + ") in " + delay + "ms");
        updateNotification("Reconnecting... (" + reconnectAttempt + ")");

        mainHandler.postDelayed(this::connectToBackend, delay);
    }

    /**
     * Update the persistent notification.
     */
    private void updateNotification(String statusText) {
        NotificationHelper.updateNotification(this, statusText);
    }

    /**
     * Check if service is currently connected.
     */
    public boolean isConnected() {
        return webSocketManager != null && webSocketManager.isConnected();
    }

    /**
     * Set a listener for service state changes.
     */
    public void setStateListener(ServiceStateListener listener) {
        this.stateListener = listener;
    }

    // Binder for local client binding
    public class ServiceBinder extends Binder {
        GatewayForegroundService getService() {
            return GatewayForegroundService.this;
        }
    }

    // Listener interface for service state changes
    public interface ServiceStateListener {
        void onConnected();
        void onDisconnected();
        void onError(String message);
    }
}
