package com.smstool.gateway.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Parses incoming WebSocket messages from the backend.
 * Provides utility methods for constructing outgoing messages.
 */
public class MessageParser {
    private static final String TAG = "MessageParser";
    private static final Gson gson = new Gson();

    /**
     * Parse the type of a JSON message.
     */
    public static String getMessageType(String jsonText) {
        try {
            JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();
            return json.has("type") ? json.get("type").getAsString() : null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse message type", e);
            return null;
        }
    }

    /**
     * Parse a complete JSON message into a JsonObject.
     */
    public static JsonObject parseMessage(String jsonText) throws Exception {
        try {
            return JsonParser.parseString(jsonText).getAsJsonObject();
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse JSON message", e);
            throw new Exception("Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Create a device_info message to send on connection.
     */
    public static String createDeviceInfoMessage(String deviceId, String deviceName,
                                                  String androidVersion, String appVersion) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "device_info");
        msg.addProperty("message_id", java.util.UUID.randomUUID().toString());
        msg.addProperty("device_id", deviceId);
        msg.addProperty("device_name", deviceName);
        msg.addProperty("android_version", androidVersion);
        msg.addProperty("app_version", appVersion);
        msg.addProperty("connected_at", java.time.Instant.now().toString());
        return msg.toString();
    }

    /**
     * Create a status_update message for a completed job.
     */
    public static String createStatusUpdateMessage(String jobId, String status, int attempt) {
        return createStatusUpdateMessage(jobId, status, attempt, null, null);
    }

    /**
     * Create a status_update message with error details.
     */
    public static String createStatusUpdateMessage(String jobId, String status, int attempt,
                                                   Integer errorCode, String errorMessage) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "status_update");
        msg.addProperty("message_id", java.util.UUID.randomUUID().toString());
        msg.addProperty("job_id", jobId);
        msg.addProperty("status", status);
        msg.addProperty("attempt", attempt);
        if (errorCode != null) {
            msg.addProperty("error_code", errorCode);
        } else {
            msg.add("error_code", com.google.gson.JsonNull.INSTANCE);
        }
        if (errorMessage != null) {
            msg.addProperty("error_message", errorMessage);
        } else {
            msg.add("error_message", com.google.gson.JsonNull.INSTANCE);
        }
        msg.addProperty("timestamp", java.time.Instant.now().toString());
        return msg.toString();
    }

    /**
     * Create a pong message in response to a ping.
     */
    public static String createPongMessage(String pingMessageId) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "pong");
        msg.addProperty("message_id", java.util.UUID.randomUUID().toString());
        msg.addProperty("ping_message_id", pingMessageId);
        msg.addProperty("timestamp", java.time.Instant.now().toString());
        return msg.toString();
    }

    /**
     * Extract a field from a JSON message.
     */
    public static String getStringField(JsonObject json, String fieldName, String defaultValue) {
        try {
            if (json.has(fieldName) && !json.get(fieldName).isJsonNull()) {
                return json.get(fieldName).getAsString();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract field: " + fieldName);
        }
        return defaultValue;
    }

    /**
     * Extract an integer field from a JSON message.
     */
    public static Integer getIntField(JsonObject json, String fieldName) {
        try {
            if (json.has(fieldName) && !json.get(fieldName).isJsonNull()) {
                return json.get(fieldName).getAsInt();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract int field: " + fieldName);
        }
        return null;
    }
}
