package com.smstool.gateway.network;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smstool.gateway.data.model.SmsJobStatus;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Wraps OkHttp's WebSocket for persistent connection to the backend.
 * Handles connection lifecycle, message sending, and listener callbacks.
 */
public class WebSocketManager extends WebSocketListener {
    private static final String TAG = "WebSocketManager";
    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private WebSocket webSocket;
    private WebSocketListener listener;
    private boolean isConnecting = false;

    private final OkHttpClient httpClient = new OkHttpClient();

    /**
     * Set the listener for WebSocket events.
     */
    public void setListener(WebSocketListener listener) {
        this.listener = listener;
    }

    /**
     * Connect to the WebSocket backend.
     */
    public void connect(String url) {
        if (isConnecting || webSocket != null) {
            Log.w(TAG, "Already connecting or connected");
            return;
        }

        isConnecting = true;
        Log.i(TAG, "Connecting to WebSocket: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = httpClient.newWebSocket(request, this);
        httpClient.dispatcher().executorService().shutdown();
    }

    /**
     * Disconnect from the WebSocket.
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(NORMAL_CLOSURE_STATUS, "User disconnect");
            webSocket = null;
        }
        isConnecting = false;
    }

    /**
     * Send a JSON message to the server.
     */
    public void sendMessage(String jsonMessage) {
        if (webSocket != null) {
            try {
                webSocket.send(jsonMessage);
                Log.d(TAG, "Message sent");
            } catch (Exception e) {
                Log.e(TAG, "Failed to send message", e);
            }
        } else {
            Log.w(TAG, "WebSocket not connected, message dropped");
        }
    }

    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return webSocket != null && !isConnecting;
    }

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
        isConnecting = false;
        Log.i(TAG, "WebSocket opened");
        if (listener != null) {
            listener.onOpen(webSocket, response);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.d(TAG, "Message received");
        if (listener != null) {
            listener.onMessage(webSocket, text);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.d(TAG, "Binary message received");
        if (listener != null) {
            listener.onMessage(webSocket, bytes);
        }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        Log.i(TAG, "WebSocket closing: " + code + " " + reason);
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        if (listener != null) {
            listener.onClosing(webSocket, code, reason);
        }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        isConnecting = false;
        this.webSocket = null;
        Log.i(TAG, "WebSocket closed: " + code + " " + reason);
        if (listener != null) {
            listener.onClosed(webSocket, code, reason);
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
        isConnecting = false;
        this.webSocket = null;
        Log.e(TAG, "WebSocket failure", t);
        if (listener != null) {
            listener.onFailure(webSocket, t, response);
        }
    }
}
